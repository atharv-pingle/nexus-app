package com.nexus.planner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,               // e.g. "Side Project Deep Work"
    val category: String,           // PROJECT, FITNESS, SLEEP, MINDFULNESS
    val targetDays: Int = 66,       // Dr. Lally 66-day protocol
    val currentDayIndex: Int = 1,   // Current day count
    val totalCompletions: Int = 0,  // Raw completions
    val reboundCount: Int = 0,      // Number of times bounced back after miss
    val automaticityScore: Float = 0.0f, // 0.0 to 1.0 (Lally automaticity curve)
    val lastCompletedDate: String? = null
)

@Serializable
@Entity(tableName = "friction_logs")
data class FrictionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateIso: String,
    val tag: String,                // PHONE_SCROLLING, AFTERNOON_SLEEP, WORK_RUNOVER, VAGUE_TASK
    val contextNotes: String = "",
    val energyLevel: String = "MEDIUM" // HIGH, MEDIUM, LOW
)

@Serializable
@Entity(tableName = "user_cognitive_profile")
data class UserCognitiveProfileEntity(
    @PrimaryKey
    val profileId: String = "primary_user",
    val chronotype: String = "MORNING_TO_AFTERNOON",
    val workStartHour: String = "08:00",
    val workEndHour: String = "15:30",
    val afternoonResetPreference: String = "NAP_20M", // NAP_20M, WALK, MOBILITY
    val sideProjectGoal: String = "Build Micro-SaaS & Side Income",
    val fitnessProfile: String = "Home gym dumbbells & bench",
    val preferredChunkMinutes: Int = 75,
    val aiLearnedPatterns: String = "Frequent phone distraction between 15:30-17:00 when unguided."
)
