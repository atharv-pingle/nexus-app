package com.nexus.planner.data.repository

import com.nexus.planner.data.local.NexusDatabase
import com.nexus.planner.data.local.entity.FrictionLogEntity
import com.nexus.planner.data.local.entity.HabitEntity
import com.nexus.planner.data.local.entity.ScheduleBlockEntity
import com.nexus.planner.data.local.entity.UserCognitiveProfileEntity
import com.nexus.planner.data.remote.AuthManager
import com.nexus.planner.data.remote.NexusApiClient
import com.nexus.planner.data.remote.OpenRouterClient
import com.nexus.planner.data.remote.VoiceIntentPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class NexusRepository(
    private val database: NexusDatabase,
    val openRouterClient: OpenRouterClient,
    val apiClient: NexusApiClient? = null,
    val authManager: AuthManager? = null
) {
    private val scheduleDao = database.scheduleDao()
    private val habitDao = database.habitDao()
    private val frictionDao = database.frictionDao()
    private val userProfileDao = database.userProfileDao()

    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun syncWithServer(dateIso: String) {
        if (authManager?.hasValidToken() != true || apiClient == null) return
        apiClient.fetchTodaySchedule().onSuccess { serverBlocks ->
            if (serverBlocks.isNotEmpty()) {
                val entities = serverBlocks.mapIndexed { idx, b ->
                    ScheduleBlockEntity(
                        id = b.id ?: 0L,
                        dateIso = b.scheduled_date ?: dateIso,
                        startTime = b.start_time,
                        endTime = b.end_time,
                        title = b.title,
                        description = "",
                        category = b.block_type,
                        durationMinutes = b.duration_minutes,
                        status = b.status,
                        delayCount = b.delay_count,
                        microAction = "",
                        sortOrder = idx
                    )
                }
                scheduleDao.clearDate(dateIso)
                scheduleDao.insertBlocks(entities)
            }
        }
        apiClient.fetchHabits().onSuccess { habits ->
            if (habits.isNotEmpty()) {
                habits.forEach { h ->
                    val entity = HabitEntity(
                        id = h.id ?: 0L,
                        name = h.title,
                        category = h.category,
                        targetDays = h.target_days,
                        currentDayIndex = h.current_day,
                        totalCompletions = h.streak,
                        automaticityScore = (h.streak.toFloat() / h.target_days.toFloat()).coerceIn(0f, 1f)
                    )
                    habitDao.insertHabit(entity)
                }
            }
        }
    }

    suspend fun clearLocalData() {
        scheduleDao.clearAll()
        habitDao.clearAll()
        frictionDao.clearAll()
    }

    fun getTodaySchedule(dateIso: String): Flow<List<ScheduleBlockEntity>> {
        return scheduleDao.getScheduleForDate(dateIso)
    }

    fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAllHabits()
    fun getAllFrictions(): Flow<List<FrictionLogEntity>> = frictionDao.getAllFrictions()
    fun getUserProfile(): Flow<UserCognitiveProfileEntity?> = userProfileDao.getProfile()

    suspend fun saveProfile(profile: UserCognitiveProfileEntity) {
        userProfileDao.saveProfile(profile)
    }

    suspend fun updateBlockStatus(id: Long, status: String) {
        scheduleDao.updateStatus(id, status)
    }

    suspend fun addDelayToRemainingDay(dateIso: String, delayMinutes: Int) {
        val currentBlocks = scheduleDao.getScheduleForDate(dateIso).firstOrNull() ?: return
        val updated = currentBlocks.map { block ->
            if (block.status == "SCHEDULED" && block.category != "JOB") {
                try {
                    val start = LocalTime.parse(block.startTime, timeFormatter)
                    val end = LocalTime.parse(block.endTime, timeFormatter)
                    val newStart = start.plusMinutes(delayMinutes.toLong())
                    val newEnd = end.plusMinutes(delayMinutes.toLong())
                    block.copy(
                        startTime = newStart.format(timeFormatter),
                        endTime = newEnd.format(timeFormatter)
                    )
                } catch (e: Exception) {
                    block
                }
            } else {
                block
            }
        }
        scheduleDao.clearDate(dateIso)
        scheduleDao.insertBlocks(updated)
    }

    suspend fun logFrictionAndDebrief(
        dateIso: String,
        energyLevel: String,
        frictionTags: List<String>,
        priorityGoal: String,
        targetDateIso: String
    ): Result<String> {
        // Log frictions
        frictionTags.forEach { tag ->
            frictionDao.insertFriction(
                FrictionLogEntity(dateIso = dateIso, tag = tag, energyLevel = energyLevel)
            )
        }

        val profile = userProfileDao.getProfile().firstOrNull()
            ?: UserCognitiveProfileEntity()

        val result = openRouterClient.generateNextDayPlan(
            currentDate = dateIso,
            targetDate = targetDateIso,
            energyLevel = energyLevel,
            frictionTags = frictionTags,
            priorityGoal = priorityGoal,
            userProfileJson = profile.toString()
        )

        return result.fold(
            onSuccess = { plan ->
                val entities = plan.blocks.mapIndexed { idx, b ->
                    ScheduleBlockEntity(
                        dateIso = targetDateIso,
                        startTime = b.startTime,
                        endTime = b.endTime,
                        title = b.title,
                        description = b.description,
                        category = b.category,
                        durationMinutes = b.durationMinutes,
                        microAction = b.microAction,
                        sortOrder = idx
                    )
                }
                scheduleDao.clearDate(targetDateIso)
                scheduleDao.insertBlocks(entities)
                Result.success(plan.reasoning)
            },
            onFailure = { error ->
                // Offline fallback schedule generation
                generateOfflineDefaultSchedule(targetDateIso, priorityGoal)
                Result.failure(error)
            }
        )
    }

    suspend fun processVoiceIntent(speechText: String): Result<VoiceIntentPayload> {
        val result = openRouterClient.parseVoiceIntent(speechText)
        return result.fold(
            onSuccess = { intent ->
                when (intent.action.uppercase()) {
                    "RESCHEDULE" -> {
                        val shift = if (intent.minutesShift > 0) intent.minutesShift else 30
                        addDelayToRemainingDay(LocalDate.now().toString(), shift)
                    }
                    "POWER_NAP" -> {
                        // handled by timer
                    }
                    "COMPLETE_STEP" -> {
                        val today = LocalDate.now().toString()
                        val blocks = scheduleDao.getScheduleForDate(today).firstOrNull()
                        val active = blocks?.firstOrNull { it.status == "ACTIVE" || it.status == "SCHEDULED" }
                        if (active != null) {
                            scheduleDao.updateStatus(active.id, "COMPLETED")
                        }
                    }
                }
                Result.success(intent)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    suspend fun initializeDefaultDataIfEmpty(dateIso: String) {
        val existing = scheduleDao.getScheduleForDate(dateIso).firstOrNull()
        if (existing.isNullOrEmpty()) {
            val defaults = listOf(
                ScheduleBlockEntity(
                    dateIso = dateIso,
                    startTime = "08:00",
                    endTime = "15:30",
                    title = "WFH Work Block",
                    description = "Core job deliverables & sprint focus",
                    category = "JOB",
                    durationMinutes = 450,
                    microAction = "Review PR #42 & send daily update",
                    status = "ACTIVE",
                    sortOrder = 0
                ),
                ScheduleBlockEntity(
                    dateIso = dateIso,
                    startTime = "15:30",
                    endTime = "16:00",
                    title = "Work-Life Reset",
                    description = "20-minute power nap / NSDR to prevent evening slump",
                    category = "REST",
                    durationMinutes = 30,
                    microAction = "Set timer, lay back with eyes closed for 20m",
                    status = "SCHEDULED",
                    sortOrder = 1
                ),
                ScheduleBlockEntity(
                    dateIso = dateIso,
                    startTime = "16:30",
                    endTime = "17:45",
                    title = "Side Project Sprint",
                    description = "SaaS problem discovery & developer pain points",
                    category = "DEEP_WORK",
                    durationMinutes = 75,
                    microAction = "Write down 3 annoying developer workflows on paper",
                    status = "SCHEDULED",
                    sortOrder = 2
                ),
                ScheduleBlockEntity(
                    dateIso = dateIso,
                    startTime = "18:00",
                    endTime = "18:45",
                    title = "Home Gym: Push Day",
                    description = "DB Bench Press, Incline Press, Lateral Raises",
                    category = "FITNESS",
                    durationMinutes = 45,
                    microAction = "Warm up shoulders with 10 arm circles & light dumbbells",
                    status = "SCHEDULED",
                    sortOrder = 3
                ),
                ScheduleBlockEntity(
                    dateIso = dateIso,
                    startTime = "21:30",
                    endTime = "21:40",
                    title = "Nightly Debrief",
                    description = "2-min retrospective to calibrate tomorrow",
                    category = "DEBRIEF",
                    durationMinutes = 10,
                    microAction = "Open Nexus and tap today's energy rating",
                    status = "SCHEDULED",
                    sortOrder = 4
                )
            )
            scheduleDao.insertBlocks(defaults)
        }

        val habits = habitDao.getAllHabits().firstOrNull()
        if (habits.isNullOrEmpty()) {
            habitDao.insertHabit(
                HabitEntity(
                    name = "Side Project Deep Work",
                    category = "PROJECT",
                    targetDays = 66,
                    currentDayIndex = 14,
                    totalCompletions = 14,
                    automaticityScore = 0.21f
                )
            )
            habitDao.insertHabit(
                HabitEntity(
                    name = "Dumbbell Strength Training",
                    category = "FITNESS",
                    targetDays = 66,
                    currentDayIndex = 19,
                    totalCompletions = 18,
                    automaticityScore = 0.28f
                )
            )
            habitDao.insertHabit(
                HabitEntity(
                    name = "Controlled 3:30 Transition",
                    category = "SLEEP",
                    targetDays = 66,
                    currentDayIndex = 8,
                    totalCompletions = 7,
                    automaticityScore = 0.12f
                )
            )
        }
    }

    private suspend fun generateOfflineDefaultSchedule(targetDateIso: String, priorityGoal: String) {
        val fallback = listOf(
            ScheduleBlockEntity(
                dateIso = targetDateIso,
                startTime = "09:30",
                endTime = "11:30",
                title = priorityGoal.ifBlank { "Deep Work Sprint" },
                description = "Weekend focus block without distractions",
                category = "DEEP_WORK",
                durationMinutes = 120,
                microAction = "Open editor and begin first sub-task",
                sortOrder = 0
            ),
            ScheduleBlockEntity(
                dateIso = targetDateIso,
                startTime = "12:00",
                endTime = "12:45",
                title = "Home Gym: Dumbbell Pull Day",
                description = "Dumbbell RDLs, Single-arm Rows, Hammer Curls",
                category = "FITNESS",
                durationMinutes = 45,
                microAction = "Set bench to flat and pick up warm-up dumbbells",
                sortOrder = 1
            ),
            ScheduleBlockEntity(
                dateIso = targetDateIso,
                startTime = "13:30",
                endTime = "16:30",
                title = "Rest & Leisure Recovery",
                description = "Guilt-free relaxation, meal, and errands",
                category = "REST",
                durationMinutes = 180,
                microAction = "Disconnect from work screens",
                sortOrder = 2
            )
        )
        scheduleDao.clearDate(targetDateIso)
        scheduleDao.insertBlocks(fallback)
    }
}
