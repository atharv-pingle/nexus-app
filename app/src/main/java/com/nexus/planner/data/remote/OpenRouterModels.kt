package com.nexus.planner.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenRouterChatRequest(
    val model: String = "google/gemini-2.0-flash-exp:free",
    val messages: List<ChatMessage>,
    val temperature: Float = 0.2f,
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null
)

@Serializable
data class ResponseFormat(
    val type: String = "json_object"
)

@Serializable
data class ChatMessage(
    val role: String, // system, user, assistant
    val content: String
)

@Serializable
data class OpenRouterChatResponse(
    val id: String? = null,
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    val message: ChatMessage
)

// Structured Output Models for Schedule Generation
@Serializable
data class GeneratedPlanPayload(
    val reasoning: String,
    val dateIso: String,
    val blocks: List<GeneratedBlockPayload>
)

@Serializable
data class GeneratedBlockPayload(
    val startTime: String,
    val endTime: String,
    val title: String,
    val description: String,
    val category: String,
    val durationMinutes: Int,
    val microAction: String
)

// Voice Intent Extraction
@Serializable
data class VoiceIntentPayload(
    val action: String, // "RESCHEDULE", "POWER_NAP", "LOG_WORKOUT", "COMPLETE_STEP", "QUERY"
    val minutesShift: Int = 0,
    val details: String = "",
    val spokenConfirmation: String = ""
)
