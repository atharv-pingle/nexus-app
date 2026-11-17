package com.nexus.planner.data.local.dao

import androidx.room.*
import com.nexus.planner.data.local.entity.FrictionLogEntity
import com.nexus.planner.data.local.entity.HabitEntity
import com.nexus.planner.data.local.entity.HabitLogEntity
import com.nexus.planner.data.local.entity.ScheduleBlockEntity
import com.nexus.planner.data.local.entity.SubTaskEntity
import com.nexus.planner.data.local.entity.UserCognitiveProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_blocks WHERE dateIso = :dateIso ORDER BY sortOrder ASC, startTime ASC")
    fun getScheduleForDate(dateIso: String): Flow<List<ScheduleBlockEntity>>

    @Query("SELECT * FROM schedule_blocks WHERE horizon = :horizon ORDER BY sortOrder ASC, startTime ASC")
    fun getScheduleForHorizon(horizon: String): Flow<List<ScheduleBlockEntity>>

    @Query("SELECT * FROM schedule_blocks WHERE id = :id LIMIT 1")
    suspend fun getBlockById(id: Long): ScheduleBlockEntity?

    @Query("DELETE FROM schedule_blocks WHERE id = :id")
    suspend fun deleteBlockById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: ScheduleBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<ScheduleBlockEntity>)

    @Update
    suspend fun updateBlock(block: ScheduleBlockEntity)

    @Query("UPDATE schedule_blocks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM schedule_blocks WHERE dateIso = :dateIso")
    suspend fun clearDate(dateIso: String)

    @Query("DELETE FROM schedule_blocks")
    suspend fun clearAll()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("DELETE FROM habits")
    suspend fun clearAll()
}

@Dao
interface SubTaskDao {
    @Query("SELECT * FROM subtasks WHERE parentBlockId = :parentBlockId ORDER BY sortOrder ASC, id ASC")
    fun getSubTasksForBlock(parentBlockId: Long): Flow<List<SubTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity): Long

    @Update
    suspend fun updateSubTask(subTask: SubTaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubTask(id: Long)

    @Query("DELETE FROM subtasks WHERE parentBlockId = :parentBlockId")
    suspend fun deleteSubTasksForBlock(parentBlockId: Long)
}

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY dateIso DESC")
    fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE dateIso >= :startDateIso ORDER BY dateIso ASC")
    fun getLogsSince(startDateIso: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND dateIso = :dateIso LIMIT 1")
    suspend fun getLogForDate(habitId: Long, dateIso: String): HabitLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateIso = :dateIso")
    suspend fun deleteLog(habitId: Long, dateIso: String)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun clearLogsForHabit(habitId: Long)
}

@Dao
interface FrictionDao {
    @Query("SELECT * FROM friction_logs ORDER BY id DESC")
    fun getAllFrictions(): Flow<List<FrictionLogEntity>>

    @Insert
    suspend fun insertFriction(friction: FrictionLogEntity)

    @Query("DELETE FROM friction_logs")
    suspend fun clearAll()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_cognitive_profile WHERE profileId = 'primary_user'")
    fun getProfile(): Flow<UserCognitiveProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserCognitiveProfileEntity)
}
