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
    val todaySchedule: List<ServerScheduleBlockDto> = emptyList(),
    val tomorrowSchedule: List<ServerScheduleBlockDto> = emptyList(),
    val isAutoPlanEnabled: Boolean = true,
    val activeAlarmTask: ServerScheduleBlockDto? = null,
    val showScheduleInChat: Boolean = false,
    val pushupsCount: Int = 20,
    val workoutStreak: Int = 19,
    val isLoading: Boolean = false,
    val toastNotice: String? = null,
    val isVoiceModalOpen: Boolean = false,
    val isAlarmMicListening: Boolean = false,
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
                _uiState.update { it.copy(todaySchedule = blocks) }
            }

            // 2. Fetch Tomorrow's Tasks (triggers core brain carry-over)
            apiClient.fetchTomorrowSchedule().onSuccess { blocks ->
                _uiState.update { it.copy(tomorrowSchedule = blocks) }
            }

            // 3. Fetch Chat History (clean any raw asterisks)
            apiClient.fetchAgentMessages().onSuccess { msgs ->
                val cleaned = msgs.map { it.copy(content = it.content.replace("**", "")) }
                _uiState.update { it.copy(messages = cleaned) }
            }

            // 4. Proactive Check
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
                        val cleanedMsg = nudge.message.copy(content = nudge.message.content.replace("**", ""))
                        currentMsgs.add(cleanedMsg)
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
            val tempUserMsg = ServerChatMessageDto(
                id = System.currentTimeMillis(),
                sender = "user",
                content = trimmed,
                message_type = "TEXT"
            )
            _uiState.update { it.copy(messages = it.messages + tempUserMsg, isLoading = true) }

            apiClient.sendChatMessage(trimmed).onSuccess { resp ->
                val cleanReply = resp.reply.replace("**", "")
                val agentMsg = ServerChatMessageDto(
                    id = System.currentTimeMillis() + 1,
                    sender = "agent",
                    content = cleanReply,
                    message_type = "TEXT"
                )
                val isPushups = trimmed.contains("pushup", ignoreCase = true)
                _uiState.update { 
                    it.copy(
                        messages = it.messages + agentMsg,
                        todaySchedule = if (resp.schedule.isNotEmpty()) resp.schedule else it.todaySchedule,
                        showScheduleInChat = resp.show_schedule_widget,
                        pushupsCount = if (isPushups) it.pushupsCount + 10 else it.pushupsCount,
                        isLoading = false
                    )
                }
                // Refresh tomorrow's schedule as well if user referenced tomorrow
                if (trimmed.contains("tomorrow", ignoreCase = true)) {
                    apiClient.fetchTomorrowSchedule().onSuccess { tomorrowBlocks ->
                        _uiState.update { it.copy(tomorrowSchedule = tomorrowBlocks) }
                    }
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

    fun toggleTask(blockId: Long, isTomorrow: Boolean = false) {
        viewModelScope.launch {
            if (isTomorrow) {
                val updated = _uiState.value.tomorrowSchedule.map {
                    if (it.id == blockId) it.copy(is_completed = !it.is_completed) else it
                }
                _uiState.update { it.copy(tomorrowSchedule = updated) }
            } else {
                val updated = _uiState.value.todaySchedule.map {
                    if (it.id == blockId) it.copy(is_completed = !it.is_completed) else it
                }
                _uiState.update { it.copy(todaySchedule = updated) }
            }

            apiClient.completeBlock(blockId).onSuccess {
                loadData()
            }
        }
    }

    fun toggleAutoPlan() {
        val next = !_uiState.value.isAutoPlanEnabled
        _uiState.update { 
            it.copy(
                isAutoPlanEnabled = next,
                toastNotice = if (next) "Dynamic AI schedule active" else "Manual schedule active"
            ) 
        }
        loadData()
    }

    fun updateTask(
        blockId: Long,
        title: String,
        startTime: String,
        durationMinutes: Int,
        isTomorrow: Boolean
    ) {
        viewModelScope.launch {
            val targetDate = if (isTomorrow) {
                java.time.LocalDate.now().plusDays(1).toString()
            } else {
                java.time.LocalDate.now().toString()
            }
            apiClient.updateBlock(
                blockId = blockId,
                title = title,
                startTime = startTime,
                durationMinutes = durationMinutes,
                scheduledDate = targetDate
            ).onSuccess {
                _uiState.update { it.copy(toastNotice = "Updated '$title'") }
                loadData()
            }.onFailure { err ->
                _uiState.update { it.copy(toastNotice = "Failed to update: ${err.message}") }
            }
        }
    }

    fun triggerAlarmForTask(task: ServerScheduleBlockDto) {
        _uiState.update { it.copy(activeAlarmTask = task) }
    }

    fun dismissAlarm() {
        _uiState.update { it.copy(activeAlarmTask = null, isAlarmMicListening = false) }
    }

    fun snoozeAlarm(mins: Int = 15) {
        val active = _uiState.value.activeAlarmTask
        dismissAlarm()
        viewModelScope.launch {
            _uiState.update { it.copy(toastNotice = "⏱️ Snoozed +${mins}m. Schedule auto-rebalanced.") }
            apiClient.executeAgentAction(action = "DELAY_SCHEDULE", mins = mins).onSuccess { res ->
                _uiState.update { it.copy(todaySchedule = res.schedule) }
            }
        }
    }

    fun shiftSchedule(mins: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, toastNotice = "Shifted schedule by +${mins}m") }
            apiClient.executeAgentAction(action = "DELAY_SCHEDULE", mins = mins).onSuccess { res ->
                _uiState.update { it.copy(todaySchedule = res.schedule, isLoading = false) }
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
                        todaySchedule = res.schedule,
                        isLoading = false,
                        toastNotice = "Action updated with agent"
                    )
                }
                checkProactiveNudge()
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun handleTaskVoiceInstruction(blockId: Long?, command: String) {
        val active = _uiState.value.activeAlarmTask
        dismissAlarm()
        val lower = command.lowercase()
        viewModelScope.launch {
            if (lower.contains("delay") || lower.contains("postpone") || lower.contains("snooze")) {
                snoozeAlarm(20)
                _uiState.update { it.copy(toastNotice = "Delayed task by 20m via voice") }
            } else if (blockId != null && (lower.contains("done") || lower.contains("complete") || lower.contains("finish"))) {
                toggleTask(blockId)
                _uiState.update { it.copy(toastNotice = "Task marked complete via voice") }
            } else {
                val contextualMessage = if (active != null) {
                    "Regarding task '${active.title}' (ID: ${active.id}): $command"
                } else {
                    command
                }
                sendMessage(contextualMessage)
            }
        }
    }

    fun createQuickTask(title: String, startTime: String = "10:00", isTomorrow: Boolean = false) {
        if (title.isBlank()) return
        viewModelScope.launch {
            apiClient.createQuickTask(title, startTime, isTomorrow).onSuccess { newBlock ->
                if (isTomorrow) {
                    _uiState.update { it.copy(tomorrowSchedule = listOf(newBlock) + it.tomorrowSchedule) }
                } else {
                    _uiState.update { it.copy(todaySchedule = listOf(newBlock) + it.todaySchedule) }
                }
                _uiState.update { it.copy(toastNotice = "Added '$title'") }
            }
        }
    }

    fun deleteTask(blockId: Long) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    todaySchedule = it.todaySchedule.filter { b -> b.id != blockId },
                    tomorrowSchedule = it.tomorrowSchedule.filter { b -> b.id != blockId },
                    toastNotice = "Task deleted"
                )
            }
            apiClient.deleteBlock(blockId)
        }
    }

    fun recordPushups(count: Int = 10) {
        _uiState.update { it.copy(pushupsCount = it.pushupsCount + count, toastNotice = "Boom! +$count Pushups logged 💪") }
        sendMessage("✓ Completed $count pushups!")
    }

    fun dismissScheduleFromChat() {
        _uiState.update { it.copy(showScheduleInChat = false) }
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

    fun clearToast() {
        _uiState.update { it.copy(toastNotice = null) }
    }
}
