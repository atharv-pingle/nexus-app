package com.nexus.planner.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenRouterClient(
    private val context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // Retrieve API key from secure preferences or fallback
    fun getApiKey(): String {
        val prefs = context.getSharedPreferences("nexus_secure_prefs", Context.MODE_PRIVATE)
        return prefs.getString("openrouter_api_key", "") ?: ""
    }

    fun saveApiKey(key: String) {
        val prefs = context.getSharedPreferences("nexus_secure_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("openrouter_api_key", key.trim()).apply()
    }

    suspend fun completeChat(
        request: OpenRouterChatRequest,
        customKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = customKey ?: getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("OpenRouter API key is not configured."))
        }

        try {
            val requestBodyString = json.encodeToString(request)
            val httpRequest = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://nexus.planner.app")
                .addHeader("X-Title", "Nexus Day Planner")
                .post(requestBodyString.toRequestBody(mediaType))
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("API error ${response.code}: $body"))
                }

                val chatResponse = json.decodeFromString<OpenRouterChatResponse>(body)
                val text = chatResponse.choices.firstOrNull()?.message?.content
                    ?: return@withContext Result.failure(IOException("Empty response from AI"))

                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateNextDayPlan(
        currentDate: String,
        targetDate: String,
        energyLevel: String,
        frictionTags: List<String>,
        priorityGoal: String,
        userProfileJson: String,
        model: String = "google/gemini-2.0-flash-exp:free"
    ): Result<GeneratedPlanPayload> = withContext(Dispatchers.IO) {
        val prompt = AgentPrompts.buildNextDayPrompt(
            currentDate, targetDate, energyLevel, frictionTags, priorityGoal, userProfileJson
        )

        val request = OpenRouterChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", AgentPrompts.SYSTEM_CHIEF_OF_STAFF),
                ChatMessage("user", prompt)
            ),
            responseFormat = ResponseFormat("json_object")
        )

        val rawResult = completeChat(request)
        rawResult.fold(
            onSuccess = { rawJson ->
                try {
                    // Extract json cleanly even if wrapped in markdown
                    val cleanJson = cleanMarkdownJson(rawJson)
                    val parsed = json.decodeFromString<GeneratedPlanPayload>(cleanJson)
                    Result.success(parsed)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun parseVoiceIntent(
        speechText: String,
        model: String = "google/gemini-2.0-flash-exp:free"
    ): Result<VoiceIntentPayload> = withContext(Dispatchers.IO) {
        val prompt = AgentPrompts.buildVoiceIntentPrompt(speechText)
        val request = OpenRouterChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", AgentPrompts.SYSTEM_CHIEF_OF_STAFF),
                ChatMessage("user", prompt)
            ),
            responseFormat = ResponseFormat("json_object")
        )

        val rawResult = completeChat(request)
        rawResult.fold(
            onSuccess = { rawJson ->
                try {
                    val cleanJson = cleanMarkdownJson(rawJson)
                    val parsed = json.decodeFromString<VoiceIntentPayload>(cleanJson)
                    Result.success(parsed)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    private fun cleanMarkdownJson(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        return str.trim()
    }
}
