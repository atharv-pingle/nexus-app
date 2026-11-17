package com.nexus.planner.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.planner.data.remote.NexusApiClient
import com.nexus.planner.data.remote.ServerChatMessageDto
import com.nexus.planner.data.remote.ServerHabitDto
import com.nexus.planner.data.remote.ServerScheduleBlockDto
import com.nexus.planner.data.remote.ServerSubTaskDto
import com.nexus.planner.service.NexusAlarmScheduler
import com.nexus.planner.util.NlTaskParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatAssistantUiState(
    val messages: List<ServerChatMessageDto> = emptyList(),
    val todaySchedule: List<ServerScheduleBlockDto> = emptyList(),
    val tomorrowSchedule: List<ServerScheduleBlockDto> = emptyList(),
    val upcomingSchedule: List<ServerScheduleBlockDto> = emptyList(),
    val inboxTasks: List<ServerScheduleBlockDto> = emptyList(),
    val waitingTasks: List<ServerScheduleBlockDto> = emptyList(),
    val somedayTasks: List<ServerScheduleBlockDto> = emptyList(),
    val activeHorizon: String = "TODAY",
    val lastDeletedTask: ServerScheduleBlockDto? = null,
    val undoSnackbarNotice: String? = null,
    val habits: List<ServerHabitDto> = emptyList(),
    val isAutoPlanEnabled: Boolean = true,
    val activeAlarmTask: ServerScheduleBlockDto? = null,
    val showScheduleInChat: Boolean = false,
    val pushupsCount: Int = 20,
    val workoutStreak: Int = 19,
    val isLoading: Boolean = false,
    val toastNotice: String? = null,
    val isVoiceModalOpen: Boolean = false,
    val isAlarmMicListening: Boolean = false,
    val liveSpeechText: String = ""
)

class ChatAssistantViewModel(
    private val apiClient: NexusApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatAssistantUiState())
    val uiState: StateFlow<ChatAssistantUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 1. Fetch Today's Tasks
            apiClient.fetchTodaySchedule().onSuccess { blocks ->
                _uiState.update { it.copy(todaySchedule = blocks) }
            }

            // 2. Fetch Tomorrow's Tasks (triggers core brain carry-over)
            apiClient.fetchTomorrowSchedule().onSuccess { blocks ->
                _uiState.update { it.copy(tomorrowSchedule = blocks) }
            }

            // 3. Fetch Chat History (clean any raw asterisks)
            apiClient.fetchAgentMessages().onSuccess { msgs ->
                val cleaned = msgs.map { it.copy(content = it.content.replace("**", "")) }
                _uiState.update { it.copy(messages = cleaned) }
            }

            // 4. Fetch Habits
            apiClient.fetchHabits().onSuccess { habitList ->
                _uiState.update { it.copy(habits = habitList) }
            }

            // 5. Proactive Check
            checkProactiveNudge()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun checkProactiveNudge() {
        viewModelScope.launch {
            apiClient.checkProactiveNudge().onSuccess { nudge ->
                if (nudge.has_nudge && nudge.message != null) {
                    val currentMsgs = _uiState.value.messages.toMutableList()
                    if (currentMsgs.none { it.id == nudge.message.id }) {
                        val cleanedMsg = nudge.message.copy(content = nudge.message.content.replace("**", ""))
                        currentMsgs.add(cleanedMsg)
                        _uiState.update { it.copy(messages = currentMsgs) }
                    }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()

        viewModelScope.launch {
            val tempUserMsg = ServerChatMessageDto(
                id = System.currentTimeMillis(),
                sender = "user",
                content = trimmed,
                message_type = "TEXT"
            )
            _uiState.update { it.copy(messages = it.messages + tempUserMsg, isLoading = true) }

            apiClient.sendChatMessage(trimmed).onSuccess { resp ->
                val cleanReply = resp.reply.replace("**", "")
                val agentMsg = ServerChatMessageDto(
                    id = System.currentTimeMillis() + 1,
                    sender = "agent",
                    content = cleanReply,
                    message_type = "TEXT"
                )
                val isPushups = trimmed.contains("pushup", ignoreCase = true)
                _uiState.update { 
                    it.copy(
                        messages = it.messages + agentMsg,
                        todaySchedule = if (resp.schedule.isNotEmpty()) resp.schedule else it.todaySchedule,
                        tomorrowSchedule = if (resp.tomorrow_schedule.isNotEmpty()) resp.tomorrow_schedule else it.tomorrowSchedule,
                        showScheduleInChat = resp.show_schedule_widget,
                        pushupsCount = if (isPushups) it.pushupsCount + 10 else it.pushupsCount,
                        isLoading = false
                    )
                }
                // Refetch both today and tomorrow to ensure 100% full sync
                loadData()
            }.onFailure { err ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        toastNotice = "Failed to sync: ${err.message}"
                    )
                }
            }
        }
    }

    fun toggleTask(blockId: Long, isTomorrow: Boolean = false) {
        viewModelScope.launch {
            if (isTomorrow) {
                val updated = _uiState.value.tomorrowSchedule.map {
                    if (it.id == blockId) it.copy(is_completed = !it.is_completed) else it
                }
                _uiState.update { it.copy(tomorrowSchedule = updated) }
            } else {
                val updated = _uiState.value.todaySchedule.map {
                    if (it.id == blockId) it.copy(is_completed = !it.is_completed) else it
                }
                _uiState.update { it.copy(todaySchedule = updated) }
            }

            apiClient.completeBlock(blockId).onSuccess {
                loadData()
            }
        }
    }

    fun toggleAutoPlan() {
        val next = !_uiState.value.isAutoPlanEnabled
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            if (next) {
                apiClient.triggerAutoPlan().onSuccess { res ->
                    _uiState.update {
                        it.copy(
                            isAutoPlanEnabled = true,
                            todaySchedule = res.today_schedule,
                            tomorrowSchedule = res.tomorrow_schedule,
                            isLoading = false,
                            toastNotice = res.summary ?: "Dynamic AI schedule rebalanced"
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            toastNotice = "Auto-Plan sync failed: ${err.message}"
                        )
                    }
                }
            } else {
                apiClient.revertAutoPlan().onSuccess { res ->
                    _uiState.update {
                        it.copy(
                            isAutoPlanEnabled = false,
                            todaySchedule = res.today_schedule,
                            tomorrowSchedule = res.tomorrow_schedule,
                            isLoading = false,
                            toastNotice = "Manual schedule active"
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            toastNotice = "Revert failed: ${err.message}"
                        )
                    }
                }
            }
        }
    }

    fun updateTask(
        blockId: Long,
        title: String,
        startTime: String,
        durationMinutes: Int,
        isTomorrow: Boolean
    ) {
        viewModelScope.launch {
            val targetDate = if (isTomorrow) {
                java.time.LocalDate.now().plusDays(1).toString()
            } else {
                java.time.LocalDate.now().toString()
            }
            apiClient.updateBlock(
                blockId = blockId,
                title = title,
                startTime = startTime,
                durationMinutes = durationMinutes,
                scheduledDate = targetDate
            ).onSuccess {
                _uiState.update { it.copy(toastNotice = "Updated '$title'") }
                loadData()
            }.onFailure { err ->
                _uiState.update { it.copy(toastNotice = "Failed to update: ${err.message}") }
            }
        }
    }

    fun triggerAlarmForTask(task: ServerScheduleBlockDto) {
        _uiState.update { it.copy(activeAlarmTask = task) }
    }

    fun dismissAlarm() {
        _uiState.update { it.copy(activeAlarmTask = null, isAlarmMicListening = false) }
    }

    fun snoozeAlarm(mins: Int = 15) {
        val active = _uiState.value.activeAlarmTask
        dismissAlarm()
        viewModelScope.launch {
            _uiState.update { it.copy(toastNotice = "⏱️ Snoozed +${mins}m. Schedule auto-rebalanced.") }
            apiClient.executeAgentAction(action = "DELAY_SCHEDULE", mins = mins).onSuccess { res ->
                _uiState.update { it.copy(todaySchedule = res.schedule) }
            }
        }
    }

    fun shiftSchedule(mins: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, toastNotice = "Shifted schedule by +${mins}m") }
            apiClient.executeAgentAction(action = "DELAY_SCHEDULE", mins = mins).onSuccess { res ->
                _uiState.update { it.copy(todaySchedule = res.schedule, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun executeQuickAction(action: String, blockId: Long? = null, mins: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            apiClient.executeAgentAction(action, blockId, mins).onSuccess { res ->
                _uiState.update { 
                    it.copy(
                        todaySchedule = res.schedule,
                        isLoading = false,
                        toastNotice = "Action updated with agent"
                    )
                }
                checkProactiveNudge()
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun handleTaskVoiceInstruction(blockId: Long?, command: String) {
        val active = _uiState.value.activeAlarmTask
        dismissAlarm()
        val lower = command.lowercase()
        viewModelScope.launch {
            if (lower.contains("delay") || lower.contains("postpone") || lower.contains("snooze")) {
                snoozeAlarm(20)
                _uiState.update { it.copy(toastNotice = "Delayed task by 20m via voice") }
            } else if (blockId != null && (lower.contains("done") || lower.contains("complete") || lower.contains("finish"))) {
                toggleTask(blockId)
                _uiState.update { it.copy(toastNotice = "Task marked complete via voice") }
            } else {
                val contextualMessage = if (active != null) {
                    "Regarding task '${active.title}' (ID: ${active.id}): $command"
                } else {
                    command
                }
                sendMessage(contextualMessage)
            }
        }
    }

    fun createQuickTask(title: String, startTime: String = "10:00", isTomorrow: Boolean = false) {
        if (title.isBlank()) return
        viewModelScope.launch {
            apiClient.createQuickTask(title, startTime, isTomorrow).onSuccess { newBlock ->
                if (isTomorrow) {
                    _uiState.update { it.copy(tomorrowSchedule = listOf(newBlock) + it.tomorrowSchedule) }
                } else {
                    _uiState.update { it.copy(todaySchedule = listOf(newBlock) + it.todaySchedule) }
                }
                _uiState.update { it.copy(toastNotice = "Added '$title'") }
            }
        }
    }

    fun setHorizon(horizon: String) {
        _uiState.update { it.copy(activeHorizon = horizon) }
    }

    fun dismissUndoSnackbar() {
        _uiState.update { it.copy(undoSnackbarNotice = null, lastDeletedTask = null) }
    }

    fun deleteTaskWithUndo(task: ServerScheduleBlockDto, context: android.content.Context? = null) {
        val blockId = task.id ?: return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    todaySchedule = state.todaySchedule.filter { it.id != blockId },
                    tomorrowSchedule = state.tomorrowSchedule.filter { it.id != blockId },
                    upcomingSchedule = state.upcomingSchedule.filter { it.id != blockId },
                    inboxTasks = state.inboxTasks.filter { it.id != blockId },
                    waitingTasks = state.waitingTasks.filter { it.id != blockId },
                    somedayTasks = state.somedayTasks.filter { it.id != blockId },
                    lastDeletedTask = task,
                    undoSnackbarNotice = "\"${task.title}\" moved to trash"
                )
            }
            if (context != null) {
                NexusAlarmScheduler.cancelTaskAlarm(context, blockId)
            }
            apiClient.deleteBlock(blockId)
        }
    }

    fun undoDeleteTask(context: android.content.Context? = null) {
        val task = _uiState.value.lastDeletedTask ?: return
        viewModelScope.launch {
            apiClient.createQuickTask(
                title = task.title,
                startTime = task.start_time,
                isTomorrow = task.horizon == "TOMORROW",
                category = task.block_type,
                durationMinutes = task.duration_minutes,
                reminderTime = task.reminder_time,
                subtasks = task.subtasks.map { it.title },
                horizon = task.horizon,
                waitingOn = task.waiting_on,
                isSequential = task.is_sequential,
                priority = task.priority
            ).onSuccess { restored ->
                when (restored.horizon.uppercase()) {
                    "TOMORROW" -> _uiState.update { it.copy(tomorrowSchedule = listOf(restored) + it.tomorrowSchedule) }
                    "UPCOMING" -> _uiState.update { it.copy(upcomingSchedule = listOf(restored) + it.upcomingSchedule) }
                    "INBOX" -> _uiState.update { it.copy(inboxTasks = listOf(restored) + it.inboxTasks) }
                    "WAITING" -> _uiState.update { it.copy(waitingTasks = listOf(restored) + it.waitingTasks) }
                    "SOMEDAY" -> _uiState.update { it.copy(somedayTasks = listOf(restored) + it.somedayTasks) }
                    else -> _uiState.update { it.copy(todaySchedule = listOf(restored) + it.todaySchedule) }
                }
                _uiState.update { it.copy(undoSnackbarNotice = null, lastDeletedTask = null, toastNotice = "Restored \"${task.title}\"") }
            }
        }
    }

    fun moveTaskToHorizon(blockId: Long, targetHorizon: String) {
        viewModelScope.launch {
            val all = _uiState.value.todaySchedule + _uiState.value.tomorrowSchedule + _uiState.value.upcomingSchedule + _uiState.value.inboxTasks + _uiState.value.waitingTasks + _uiState.value.somedayTasks
            val target = all.find { it.id == blockId } ?: return@launch
            val updated = target.copy(horizon = targetHorizon)

            _uiState.update { state ->
                state.copy(
                    todaySchedule = state.todaySchedule.filter { it.id != blockId }.let { if (targetHorizon == "TODAY") listOf(updated) + it else it },
                    tomorrowSchedule = state.tomorrowSchedule.filter { it.id != blockId }.let { if (targetHorizon == "TOMORROW") listOf(updated) + it else it },
                    upcomingSchedule = state.upcomingSchedule.filter { it.id != blockId }.let { if (targetHorizon == "UPCOMING") listOf(updated) + it else it },
                    inboxTasks = state.inboxTasks.filter { it.id != blockId }.let { if (targetHorizon == "INBOX") listOf(updated) + it else it },
                    waitingTasks = state.waitingTasks.filter { it.id != blockId }.let { if (targetHorizon == "WAITING") listOf(updated) + it else it },
                    somedayTasks = state.somedayTasks.filter { it.id != blockId }.let { if (targetHorizon == "SOMEDAY") listOf(updated) + it else it },
                    toastNotice = "Moved to $targetHorizon"
                )
            }
            apiClient.updateBlock(blockId = blockId, horizon = targetHorizon)
        }
    }

    fun advanceSequentialSubtask(blockId: Long) {
        viewModelScope.launch {
            val all = _uiState.value.todaySchedule + _uiState.value.tomorrowSchedule + _uiState.value.upcomingSchedule + _uiState.value.inboxTasks + _uiState.value.waitingTasks + _uiState.value.somedayTasks
            val target = all.find { it.id == blockId } ?: return@launch
            val nextIdx = target.active_subtask_index + 1
            val updatedSubtasks = target.subtasks.mapIndexed { idx, sub ->
                if (idx <= target.active_subtask_index) sub.copy(is_completed = true) else sub
            }
            val isAllDone = nextIdx >= target.subtasks.size
            val updated = target.copy(
                active_subtask_index = nextIdx,
                subtasks = updatedSubtasks,
                is_completed = isAllDone
            )

            _uiState.update { state ->
                fun updateList(list: List<ServerScheduleBlockDto>) = list.map { if (it.id == blockId) updated else it }
                state.copy(
                    todaySchedule = updateList(state.todaySchedule),
                    tomorrowSchedule = updateList(state.tomorrowSchedule),
                    upcomingSchedule = updateList(state.upcomingSchedule),
                    inboxTasks = updateList(state.inboxTasks),
                    waitingTasks = updateList(state.waitingTasks),
                    somedayTasks = updateList(state.somedayTasks),
                    toastNotice = if (isAllDone) "All subtasks completed! 🎉" else "Advanced to step ${nextIdx + 1}"
                )
            }
            apiClient.updateBlock(blockId = blockId, activeSubtaskIndex = nextIdx, isCompleted = isAllDone, subtasks = updatedSubtasks)
        }
    }

    fun createTaskWithNl(inputText: String, context: android.content.Context? = null) {
        if (inputText.isBlank()) return
        val currentHorizon = _uiState.value.activeHorizon
        val parsed = NlTaskParser.parse(inputText, defaultHorizon = currentHorizon)

        viewModelScope.launch {
            apiClient.createQuickTask(
                title = parsed.title,
                startTime = parsed.startTime,
                isTomorrow = parsed.isTomorrow,
                category = parsed.category,
                durationMinutes = parsed.durationMinutes,
                horizon = parsed.horizon,
                waitingOn = parsed.waitingOn,
                isSequential = parsed.isSequential,
                priority = parsed.priority
            ).onSuccess { newBlock ->
                when (newBlock.horizon.uppercase()) {
                    "TOMORROW" -> _uiState.update { it.copy(tomorrowSchedule = listOf(newBlock) + it.tomorrowSchedule) }
                    "UPCOMING" -> _uiState.update { it.copy(upcomingSchedule = listOf(newBlock) + it.upcomingSchedule) }
                    "INBOX" -> _uiState.update { it.copy(inboxTasks = listOf(newBlock) + it.inboxTasks) }
                    "WAITING" -> _uiState.update { it.copy(waitingTasks = listOf(newBlock) + it.waitingTasks) }
                    "SOMEDAY" -> _uiState.update { it.copy(somedayTasks = listOf(newBlock) + it.somedayTasks) }
                    else -> _uiState.update { it.copy(todaySchedule = listOf(newBlock) + it.todaySchedule) }
                }
                _uiState.update { it.copy(toastNotice = "Added \"${parsed.title}\" to ${newBlock.horizon}") }
            }
        }
    }

    fun deleteTask(blockId: Long, context: android.content.Context? = null) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    todaySchedule = it.todaySchedule.filter { b -> b.id != blockId },
                    tomorrowSchedule = it.tomorrowSchedule.filter { b -> b.id != blockId },
                    toastNotice = "Task deleted"
                )
            }
            if (context != null) {
                NexusAlarmScheduler.cancelTaskAlarm(context, blockId)
            }
            apiClient.deleteBlock(blockId)
        }
    }

    fun createFullTask(
        title: String,
        startTime: String = "10:00",
        durationMinutes: Int = 45,
        isTomorrow: Boolean = false,
        category: String = "FOCUS",
        reminderTime: String? = null,
        subtasks: List<String> = emptyList(),
        context: android.content.Context? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            apiClient.createQuickTask(
                title = title,
                startTime = startTime,
                isTomorrow = isTomorrow,
                category = category,
                durationMinutes = durationMinutes,
                reminderTime = reminderTime,
                subtasks = subtasks
            ).onSuccess { newBlock ->
                val subtaskDtos = subtasks.mapIndexed { idx, s ->
                    ServerSubTaskDto(id = (idx + 1).toLong(), title = s, is_completed = false)
                }
                val enrichedBlock = newBlock.copy(
                    reminder_time = reminderTime,
                    subtasks = if (newBlock.subtasks.isNotEmpty()) newBlock.subtasks else subtaskDtos
                )
                val targetDate = if (isTomorrow) {
                    java.time.LocalDate.now().plusDays(1).toString()
                } else {
                    java.time.LocalDate.now().toString()
                }

                if (isTomorrow) {
                    _uiState.update { it.copy(tomorrowSchedule = listOf(enrichedBlock) + it.tomorrowSchedule) }
                } else {
                    _uiState.update { it.copy(todaySchedule = listOf(enrichedBlock) + it.todaySchedule) }
                }

                if (reminderTime != null && context != null && newBlock.id != null) {
                    NexusAlarmScheduler.scheduleTaskAlarm(
                        context = context,
                        taskId = newBlock.id,
                        title = title,
                        scheduledDate = targetDate,
                        reminderTime = reminderTime
                    )
                }

                _uiState.update { it.copy(toastNotice = "Created task '$title' (${subtasks.size} subtasks)") }
            }
        }
    }

    fun toggleSubTask(blockId: Long, subtaskIndex: Int) {
        val todayMatches = _uiState.value.todaySchedule.any { it.id == blockId }
        val tomorrowMatches = _uiState.value.tomorrowSchedule.any { it.id == blockId }

        if (todayMatches) {
            val updated = _uiState.value.todaySchedule.map { block ->
                if (block.id == blockId) {
                    val updatedSubtasks = block.subtasks.mapIndexed { idx, sub ->
                        if (idx == subtaskIndex) sub.copy(is_completed = !sub.is_completed) else sub
                    }
                    block.copy(subtasks = updatedSubtasks)
                } else block
            }
            _uiState.update { it.copy(todaySchedule = updated) }
        } else if (tomorrowMatches) {
            val updated = _uiState.value.tomorrowSchedule.map { block ->
                if (block.id == blockId) {
                    val updatedSubtasks = block.subtasks.mapIndexed { idx, sub ->
                        if (idx == subtaskIndex) sub.copy(is_completed = !sub.is_completed) else sub
                    }
                    block.copy(subtasks = updatedSubtasks)
                } else block
            }
            _uiState.update { it.copy(tomorrowSchedule = updated) }
        }
    }

    fun createHabit(
        title: String,
        category: String,
        habitType: String,
        targetValue: Double,
        targetUnit: String,
        activeDays: String,
        reminderTime: String?,
        context: android.content.Context? = null
    ) {
        viewModelScope.launch {
            apiClient.createHabit(
                title = title,
                category = category,
                habitType = habitType,
                targetValue = targetValue,
                targetUnit = targetUnit,
                activeDays = activeDays,
                reminderTime = reminderTime
            ).onSuccess { newHabit ->
                _uiState.update { it.copy(habits = it.habits + newHabit, toastNotice = "Habit '$title' active! 🔥") }
                if (reminderTime != null && context != null && newHabit.id != null) {
                    NexusAlarmScheduler.scheduleHabitAlarm(
                        context = context,
                        habitId = newHabit.id,
                        title = title,
                        timeStr = reminderTime,
                        activeDaysStr = activeDays
                    )
                }
            }.onFailure {
                val fallback = ServerHabitDto(
                    id = System.currentTimeMillis(),
                    title = title,
                    category = category,
                    habit_type = habitType,
                    target_value = targetValue,
                    target_unit = targetUnit,
                    active_days = activeDays,
                    reminder_time = reminderTime,
                    streak = 1,
                    is_completed_today = false
                )
                _uiState.update { it.copy(habits = it.habits + fallback, toastNotice = "Habit '$title' created! 🔥") }
            }
        }
    }

    fun deleteHabit(habitId: Long, context: android.content.Context? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(habits = it.habits.filter { h -> h.id != habitId }, toastNotice = "Habit removed") }
            if (context != null) {
                NexusAlarmScheduler.cancelHabitAlarm(context, habitId)
            }
            apiClient.deleteHabit(habitId)
        }
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            val updated = _uiState.value.habits.map { h ->
                if (h.id == habitId) {
                    val nextDone = !h.is_completed_today
                    val nextStreak = if (nextDone) h.streak + 1 else maxOf(0, h.streak - 1)
                    h.copy(is_completed_today = nextDone, streak = nextStreak)
                } else h
            }
            _uiState.update { it.copy(habits = updated) }
            apiClient.toggleHabit(habitId)
        }
    }

    fun logHabitProgress(habitId: Long, value: Double) {
        viewModelScope.launch {
            val updated = _uiState.value.habits.map { h ->
                if (h.id == habitId) {
                    val nextVal = h.today_logged_value + value
                    val isDone = nextVal >= h.target_value && h.target_value > 0
                    val nextStreak = if (isDone && !h.is_completed_today) h.streak + 1 else h.streak
                    h.copy(
                        today_logged_value = nextVal,
                        is_completed_today = isDone || h.is_completed_today,
                        streak = nextStreak
                    )
                } else h
            }
            _uiState.update { it.copy(habits = updated, toastNotice = "+${value.toInt()} logged! Keep going 💪") }
            apiClient.logHabitProgress(habitId, value)
        }
    }

    fun recordPushups(count: Int = 10) {
        _uiState.update { it.copy(pushupsCount = it.pushupsCount + count, toastNotice = "Boom! +$count Pushups logged 💪") }
        sendMessage("✓ Completed $count pushups!")
    }

    fun dismissScheduleFromChat() {
        _uiState.update { it.copy(showScheduleInChat = false) }
    }

    fun openVoiceModal() {
        _uiState.update { it.copy(isVoiceModalOpen = true, liveSpeechText = "") }
    }

    fun closeVoiceModal() {
        _uiState.update { it.copy(isVoiceModalOpen = false) }
    }

    fun updateLiveSpeech(text: String) {
        _uiState.update { it.copy(liveSpeechText = text) }
    }

    fun confirmVoice() {
        val text = _uiState.value.liveSpeechText
        closeVoiceModal()
        if (text.isNotBlank()) {
            sendMessage(text)
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastNotice = null) }
    }
}
