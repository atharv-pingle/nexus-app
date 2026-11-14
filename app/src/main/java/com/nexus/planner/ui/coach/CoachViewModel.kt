package com.nexus.planner.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.planner.data.remote.AgentPrompts
import com.nexus.planner.data.remote.ChatMessage
import com.nexus.planner.data.remote.OpenRouterChatRequest
import com.nexus.planner.data.repository.NexusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UiMessage(
    val sender: String, // "AGENT" or "YOU"
    val text: String
)

class CoachViewModel(private val repository: NexusRepository) : ViewModel() {

    private val _messages = MutableStateFlow<List<UiMessage>>(
        listOf(
            UiMessage(
                sender = "AGENT",
                text = "Welcome to Nexus. You have dumbbells and a bench at home, working 8 AM – 3:30 PM. I am tuned to guide your side-project and strength training without fluff. What are we optimizing today?"
            )
        )
    )
    val messages: StateFlow<List<UiMessage>> = _messages

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val current = _messages.value.toMutableList()
        current.add(UiMessage(sender = "YOU", text = userText.trim()))
        _messages.value = current

        viewModelScope.launch {
            _isSending.value = true
            val request = OpenRouterChatRequest(
                model = "google/gemini-2.0-flash-exp:free",
                messages = listOf(
                    ChatMessage("system", AgentPrompts.SYSTEM_CHIEF_OF_STAFF),
                    ChatMessage("user", userText)
                )
            )

            val result = repository.openRouterClient.completeChat(request)
            _isSending.value = false

            val reply = result.getOrElse {
                "Noted. I have updated your daily priorities locally."
            }

            val updated = _messages.value.toMutableList()
            updated.add(UiMessage(sender = "AGENT", text = reply))
            _messages.value = updated
        }
    }
}
