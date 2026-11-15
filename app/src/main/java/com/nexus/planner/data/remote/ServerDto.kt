package com.nexus.planner.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ServerUserDto(
    val id: Int,
    val username: String = "",
    val email: String = "",
    val role: String = "user"
)

@Serializable
data class ServerTokenResponseDto(
    val access_token: String,
    val token_type: String = "bearer",
    val user: ServerUserDto
)

@Serializable
data class ServerSubTaskDto(
    val id: Long? = null,
    val title: String,
    val is_completed: Boolean = false
)

@Serializable
data class ServerScheduleBlockDto(
    val id: Long? = null,
    val title: String,
    val block_type: String = "FOCUS",
    val start_time: String,
    val end_time: String,
    val duration_minutes: Int = 90,
    val is_completed: Boolean = false,
    val status: String = "UPCOMING",
    val delay_count: Int = 0,
    val scheduled_date: String? = null,
    val reminder_time: String? = null,
    val subtasks: List<ServerSubTaskDto> = emptyList()
)

@Serializable
data class ServerHabitDto(
    val id: Long? = null,
    val title: String,
    val category: String = "FOCUS",
    val habit_type: String = "YES_NO", // YES_NO or MEASURABLE
    val target_value: Double = 0.0,
    val target_unit: String = "",
    val active_days: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
    val reminder_time: String? = null,
    val today_logged_value: Double = 0.0,
    val is_completed_today: Boolean = false,
    val target_days: Int = 66,
    val current_day: Int = 1,
    val streak: Int = 0,
    val last_completed_date: String? = null,
    val week_history: List<Boolean> = emptyList() // Mon-Sun completion for current week
)

@Serializable
data class ServerVoiceResponseDto(
    val action_taken: String = "NONE",
    val voice_response: String = "",
    val updated_schedule: List<ServerScheduleBlockDto> = emptyList()
)

@Serializable
data class ServerChatMessageDto(
    val id: Long,
    val sender: String,
    val content: String,
    val message_type: String = "TEXT",
    val actions_json: String? = null,
    val created_at: String? = null
)

@Serializable
data class ServerChatSendDto(
    val message: String
)

@Serializable
data class ServerChatResponseDto(
    val reply: String,
    val action_taken: String = "NONE",
    val show_schedule_widget: Boolean = false,
    val schedule: List<ServerScheduleBlockDto> = emptyList(),
    val tomorrow_schedule: List<ServerScheduleBlockDto> = emptyList()
)

@Serializable
data class ServerAutoPlanResponseDto(
    val success: Boolean = true,
    val summary: String? = null,
    val today_schedule: List<ServerScheduleBlockDto> = emptyList(),
    val tomorrow_schedule: List<ServerScheduleBlockDto> = emptyList()
)

@Serializable
data class ServerProactiveNudgeDto(
    val has_nudge: Boolean = false,
    val message: ServerChatMessageDto? = null
)

@Serializable
data class ServerActionExecuteDto(
    val action: String,
    val block_id: Long? = null,
    val mins: Int? = null
)

@Serializable
data class ServerActionExecuteResponseDto(
    val status: String = "ok",
    val schedule: List<ServerScheduleBlockDto> = emptyList()
)

@Serializable
data class ServerQuickTaskCreateDto(
    val title: String,
    val start_time: String = "10:00",
    val duration_minutes: Int = 45,
    val scheduled_date: String? = null,
    val block_type: String = "FOCUS",
    val reminder_time: String? = null,
    val subtasks: List<String> = emptyList()
)

@Serializable
data class ServerTaskUpdateDto(
    val title: String? = null,
    val start_time: String? = null,
    val duration_minutes: Int? = null,
    val scheduled_date: String? = null,
    val block_type: String? = null,
    val is_completed: Boolean? = null,
    val reminder_time: String? = null,
    val subtasks: List<ServerSubTaskDto>? = null
)

@Serializable
data class ServerHabitCreateDto(
    val title: String,
    val category: String = "FOCUS",
    val habit_type: String = "YES_NO",
    val target_value: Double = 0.0,
    val target_unit: String = "",
    val active_days: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
    val reminder_time: String? = null
)

@Serializable
data class ServerHabitLogDto(
    val habit_id: Long,
    val value: Double = 1.0,
    val date: String? = null
)
