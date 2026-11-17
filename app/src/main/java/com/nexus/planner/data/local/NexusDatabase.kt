package com.nexus.planner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nexus.planner.data.local.dao.FrictionDao
import com.nexus.planner.data.local.dao.HabitDao
import com.nexus.planner.data.local.dao.HabitLogDao
import com.nexus.planner.data.local.dao.ScheduleDao
import com.nexus.planner.data.local.dao.SubTaskDao
import com.nexus.planner.data.local.dao.UserProfileDao
import com.nexus.planner.data.local.entity.FrictionLogEntity
import com.nexus.planner.data.local.entity.HabitEntity
import com.nexus.planner.data.local.entity.HabitLogEntity
import com.nexus.planner.data.local.entity.ScheduleBlockEntity
import com.nexus.planner.data.local.entity.SubTaskEntity
import com.nexus.planner.data.local.entity.UserCognitiveProfileEntity

@Database(
    entities = [
        ScheduleBlockEntity::class,
        SubTaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        FrictionLogEntity::class,
        UserCognitiveProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun frictionDao(): FrictionDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: NexusDatabase? = null

        fun getInstance(context: Context): NexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexusDatabase::class.java,
                    "nexus_planner.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
