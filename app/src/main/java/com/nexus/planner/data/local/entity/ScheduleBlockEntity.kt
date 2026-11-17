package com.nexus.planner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "schedule_blocks")
data class ScheduleBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateIso: String,            // e.g. "2026-11-14"
    val startTime: String,          // e.g. "08:00"
    val endTime: String,            // e.g. "15:30"
    val title: String,              // e.g. "WFH Core Sprint"
    val description: String,        // e.g. "Core job deliverables"
    val category: String,           // JOB, DEEP_WORK, FITNESS, REST, BUFFER, DEBRIEF
    val durationMinutes: Int,       // e.g. 75
    val microAction: String,        // 2-minute starter step
    val status: String = "SCHEDULED", // SCHEDULED, ACTIVE, COMPLETED, SKIPPED, DELAYED
    val reminderTime: String? = null, // e.g. "09:30" or ISO
    val subtasksJson: String = "[]", // Serialized subtasks list
    val sortOrder: Int = 0,
    val horizon: String = "TODAY", // TODAY, TOMORROW, UPCOMING, INBOX, WAITING, SOMEDAY
    val waitingOn: String? = null, // e.g. "@Alex", "Finance Team"
    val waitingSinceIso: String? = null, // e.g. "2026-11-15"
    val isSequential: Boolean = false, // Sequential subtask focus mode
    val activeSubtaskIndex: Int = 0, // Current active step in sequential mode
    val priority: String = "NONE" // HIGH, MEDIUM, LOW, NONE
)
