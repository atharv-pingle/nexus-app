package com.nexus.planner.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class NexusApiClient(
    private val authManager: AuthManager,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun baseUrl(): String = authManager.getServerUrl()
    private fun token(): String? = authManager.getToken()

    suspend fun login(serverUrl: String, userOrEmail: String, pass: String): Result<ServerTokenResponseDto> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverUrl.trim().trimEnd('/')
            val formBody = FormBody.Builder()
                .add("username", userOrEmail.trim())
                .add("password", pass)
                .build()

            val request = Request.Builder()
                .url("$cleanUrl/api/v1/auth/login")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val tokenResp = json.decodeFromString<ServerTokenResponseDto>(bodyStr)
                authManager.saveSession(tokenResp.access_token, tokenResp.user.username, cleanUrl)
                Result.success(tokenResp)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTodaySchedule(): Result<List<ServerScheduleBlockDto>> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val request = Request.Builder()
                .url("${baseUrl()}/api/v1/schedule/today")
                .header("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val blocks = json.decodeFromString<List<ServerScheduleBlockDto>>(bodyStr)
                Result.success(blocks)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTomorrowSchedule(): Result<List<ServerScheduleBlockDto>> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val request = Request.Builder()
                .url("${baseUrl()}/api/v1/schedule/tomorrow")
                .header("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val blocks = json.decodeFromString<List<ServerScheduleBlockDto>>(bodyStr)
                Result.success(blocks)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createQuickTask(title: String, startTime: String, isTomorrow: Boolean): Result<ServerScheduleBlockDto> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val tomorrowStr = java.time.LocalDate.now().plusDays(1).toString()
            val todayStr = java.time.LocalDate.now().toString()
            val targetDate = if (isTomorrow) tomorrowStr else todayStr
            val blockType = when {
                title.contains("gym", ignoreCase = true) || title.contains("workout", ignoreCase = true) -> "WORKOUT"
                title.contains("nap", ignoreCase = true) || title.contains("rest", ignoreCase = true) -> "TRANSITION"
                title.contains("sleep", ignoreCase = true) -> "SLEEP"
                else -> "FOCUS"
            }
            val payload = json.encodeToString(ServerQuickTaskCreateDto(
                title = title,
                start_time = startTime,
                duration_minutes = 45,
                scheduled_date = targetDate,
                block_type = blockType
            ))
            val reqBody = payload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("${baseUrl()}/api/v1/schedule/blocks")
                .header("Authorization", "Bearer $authToken")
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val block = json.decodeFromString<ServerScheduleBlockDto>(bodyStr)
                Result.success(block)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBlock(
        blockId: Long,
        title: String? = null,
        startTime: String? = null,
        durationMinutes: Int? = null,
        scheduledDate: String? = null,
        isCompleted: Boolean? = null
    ): Result<ServerScheduleBlockDto> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val payload = json.encodeToString(ServerTaskUpdateDto(
                title = title,
                start_time = startTime,
                duration_minutes = durationMinutes,
                scheduled_date = scheduledDate,
                is_completed = isCompleted
            ))
            val reqBody = payload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("${baseUrl()}/api/v1/schedule/blocks/$blockId")
                .header("Authorization", "Bearer $authToken")
                .put(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val block = json.decodeFromString<ServerScheduleBlockDto>(bodyStr)
                Result.success(block)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBlock(blockId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val request = Request.Builder()
                .url("${baseUrl()}/api/v1/schedule/blocks/$blockId")
                .header("Authorization", "Bearer $authToken")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeBlock(blockId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val url = "${baseUrl()}/api/v1/schedule/blocks/$blockId/complete"
            val emptyBody = "".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $authToken")
                .put(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rebalanceSchedule(blockId: Long, action: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val url = "${baseUrl()}/api/v1/schedule/rebalance?block_id=$blockId&action=$action"
            val emptyBody = "".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $authToken")
                .post(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchHabits(): Result<List<ServerHabitDto>> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val request = Request.Builder()
                .url("${baseUrl()}/api/v1/habits")
                .header("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val habits = json.decodeFromString<List<ServerHabitDto>>(bodyStr)
                Result.success(habits)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleHabit(habitId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val url = "${baseUrl()}/api/v1/habits/$habitId/toggle"
            val emptyBody = "".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $authToken")
                .post(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------- AGENT API CALLS -----------------

    suspend fun fetchAgentMessages(): Result<List<ServerChatMessageDto>> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val request = Request.Builder()
                .url("${baseUrl()}/api/v1/agent/messages")
                .header("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val msgs = json.decodeFromString<List<ServerChatMessageDto>>(bodyStr)
                Result.success(msgs)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(message: String): Result<ServerChatResponseDto> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val payload = json.encodeToString(ServerChatSendDto(message = message))
            val reqBody = payload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("${baseUrl()}/api/v1/agent/chat")
                .header("Authorization", "Bearer $authToken")
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val resp = json.decodeFromString<ServerChatResponseDto>(bodyStr)
                Result.success(resp)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkProactiveNudge(nowTime: String? = null): Result<ServerProactiveNudgeDto> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val url = if (nowTime != null) "${baseUrl()}/api/v1/agent/proactive-check?now_time=$nowTime" else "${baseUrl()}/api/v1/agent/proactive-check"
            val emptyBody = "".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $authToken")
                .post(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val nudge = json.decodeFromString<ServerProactiveNudgeDto>(bodyStr)
                Result.success(nudge)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeAgentAction(action: String, blockId: Long? = null, mins: Int? = null): Result<ServerActionExecuteResponseDto> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val payload = json.encodeToString(ServerActionExecuteDto(action = action, block_id = blockId, mins = mins))
            val reqBody = payload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("${baseUrl()}/api/v1/agent/actions/execute")
                .header("Authorization", "Bearer $authToken")
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val res = json.decodeFromString<ServerActionExecuteResponseDto>(bodyStr)
                Result.success(res)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendVoiceCommand(transcript: String): Result<ServerVoiceResponseDto> = withContext(Dispatchers.IO) {
        try {
            val authToken = token() ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val payload = """{"transcript": ${json.encodeToString(transcript)}}"""
            val reqBody = payload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("${baseUrl()}/api/v1/agent/voice")
                .header("Authorization", "Bearer $authToken")
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyStr"))
                }
                val res = json.decodeFromString<ServerVoiceResponseDto>(bodyStr)
                Result.success(res)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
