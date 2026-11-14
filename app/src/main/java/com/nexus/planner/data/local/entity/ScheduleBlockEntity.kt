package com.nexus.planner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "schedule_blocks")
data class ScheduleBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateIso: String,            // e.g. "2026-11-14"
    val startTime: String,          // e.g. "08:00"
    val endTime: String,            // e.g. "15:30"
    val title: String,              // e.g. "WFH Core Sprint"
    val description: String,        // e.g. "Core job deliverables"
    val category: String,           // JOB, DEEP_WORK, FITNESS, REST, BUFFER, DEBRIEF
    val durationMinutes: Int,       // e.g. 75
    val microAction: String,        // 2-minute starter step
    val status: String = "SCHEDULED", // SCHEDULED, ACTIVE, COMPLETED, SKIPPED, DELAYED
    val sortOrder: Int = 0
)
