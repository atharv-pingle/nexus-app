package com.nexus.planner.data.local.dao

import androidx.room.*
import com.nexus.planner.data.local.entity.FrictionLogEntity
import com.nexus.planner.data.local.entity.HabitEntity
import com.nexus.planner.data.local.entity.ScheduleBlockEntity
import com.nexus.planner.data.local.entity.UserCognitiveProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_blocks WHERE dateIso = :dateIso ORDER BY sortOrder ASC, startTime ASC")
    fun getScheduleForDate(dateIso: String): Flow<List<ScheduleBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<ScheduleBlockEntity>)

    @Update
    suspend fun updateBlock(block: ScheduleBlockEntity)

    @Query("UPDATE schedule_blocks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM schedule_blocks WHERE dateIso = :dateIso")
    suspend fun clearDate(dateIso: String)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)
}

@Dao
interface FrictionDao {
    @Query("SELECT * FROM friction_logs ORDER BY id DESC")
    fun getAllFrictions(): Flow<List<FrictionLogEntity>>

    @Insert
    suspend fun insertFriction(friction: FrictionLogEntity)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_cognitive_profile WHERE profileId = 'primary_user'")
    fun getProfile(): Flow<UserCognitiveProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserCognitiveProfileEntity)
}
