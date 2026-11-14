package com.nexus.planner.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.planner.data.remote.NexusApiClient
import com.nexus.planner.data.remote.ServerChatMessageDto
import com.nexus.planner.data.remote.ServerScheduleBlockDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatAssistantUiState(
    val messages: List<ServerChatMessageDto> = emptyList(),
    val schedule: List<ServerScheduleBlockDto> = emptyList(),
    val showScheduleInChat: Boolean = false,
    val pushupsCount: Int = 20,
    val workoutStreak: Int = 19,
    val uclDay: Int = 20,
    val isLoading: Boolean = false,
    val toastNotice: String? = null,
    val isVoiceModalOpen: Boolean = false,
    val liveSpeechText: String = ""
)

class ChatAssistantViewModel(
    private val apiClient: NexusApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatAssistantUiState())
    val uiState: StateFlow<ChatAssistantUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 1. Fetch Today's Tasks
            apiClient.fetchTodaySchedule().onSuccess { blocks ->
                _uiState.update { it.copy(schedule = blocks) }
            }

            // 2. Fetch Chat History
            apiClient.fetchAgentMessages().onSuccess { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }

            // 3. Proactive Check
            checkProactiveNudge()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun checkProactiveNudge() {
        viewModelScope.launch {
            apiClient.checkProactiveNudge().onSuccess { nudge ->
                if (nudge.has_nudge && nudge.message != null) {
                    val currentMsgs = _uiState.value.messages.toMutableList()
                    if (currentMsgs.none { it.id == nudge.message.id }) {
                        currentMsgs.add(nudge.message)
                        _uiState.update { it.copy(messages = currentMsgs) }
                    }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()

        viewModelScope.launch {
            // Append optimistic user message
            val tempUserMsg = ServerChatMessageDto(
                id = System.currentTimeMillis(),
                sender = "user",
                content = trimmed,
                message_type = "TEXT"
            )
            _uiState.update { it.copy(messages = it.messages + tempUserMsg, isLoading = true) }

            // Send to agent API
            apiClient.sendChatMessage(trimmed).onSuccess { resp ->
                val agentMsg = ServerChatMessageDto(
                    id = System.currentTimeMillis() + 1,
                    sender = "agent",
                    content = resp.reply,
                    message_type = "TEXT"
                )
                val isPushups = trimmed.contains("pushup", ignoreCase = true)
                _uiState.update { 
                    it.copy(
                        messages = it.messages + agentMsg,
                        schedule = if (resp.schedule.isNotEmpty()) resp.schedule else it.schedule,
                        showScheduleInChat = resp.show_schedule_widget,
                        pushupsCount = if (isPushups) it.pushupsCount + 10 else it.pushupsCount,
                        isLoading = false
                    )
                }
            }.onFailure { err ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        toastNotice = "Failed to sync: ${err.message}"
                    )
                }
            }
        }
    }

    fun toggleTask(blockId: Long) {
        viewModelScope.launch {
            // Optimistic update
            val updated = _uiState.value.schedule.map {
                if (it.id == blockId) it.copy(is_completed = !it.is_completed) else it
            }
            _uiState.update { it.copy(schedule = updated) }

            apiClient.completeBlock(blockId).onSuccess {
                // Refresh schedule to stay in sync with server
                apiClient.fetchTodaySchedule().onSuccess { blocks ->
                    _uiState.update { it.copy(schedule = blocks) }
                }
            }
        }
    }

    fun shiftSchedule(mins: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, toastNotice = "Shifted schedule by +${mins}m") }
            apiClient.executeAgentAction(action = "DELAY_SCHEDULE", mins = mins).onSuccess { res ->
                _uiState.update { it.copy(schedule = res.schedule, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun executeQuickAction(action: String, blockId: Long? = null, mins: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            apiClient.executeAgentAction(action, blockId, mins).onSuccess { res ->
                _uiState.update { 
                    it.copy(
                        schedule = res.schedule,
                        isLoading = false,
                        toastNotice = "Action updated with agent"
                    )
                }
                // Also trigger proactive check to get any agent follow-up message
                checkProactiveNudge()
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun openVoiceModal() {
        _uiState.update { it.copy(isVoiceModalOpen = true, liveSpeechText = "") }
    }

    fun closeVoiceModal() {
        _uiState.update { it.copy(isVoiceModalOpen = false) }
    }

    fun updateLiveSpeech(text: String) {
        _uiState.update { it.copy(liveSpeechText = text) }
    }

    fun confirmVoice() {
        val text = _uiState.value.liveSpeechText
        closeVoiceModal()
        if (text.isNotBlank()) {
            sendMessage(text)
        }
    }

    fun recordPushups(count: Int = 10) {
        _uiState.update { it.copy(pushupsCount = it.pushupsCount + count, toastNotice = "Boom! +$count Pushups logged 💪") }
        sendMessage("✓ Completed $count pushups!")
    }

    fun dismissScheduleFromChat() {
        _uiState.update { it.copy(showScheduleInChat = false) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastNotice = null) }
    }
}
