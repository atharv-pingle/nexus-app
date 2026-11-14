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
    val scheduled_date: String? = null
)

@Serializable
data class ServerHabitDto(
    val id: Long? = null,
    val title: String,
    val category: String = "STRENGTH",
    val target_days: Int = 66,
    val current_day: Int = 1,
    val streak: Int = 0,
    val last_completed_date: String? = null
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
    val schedule: List<ServerScheduleBlockDto> = emptyList()
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
