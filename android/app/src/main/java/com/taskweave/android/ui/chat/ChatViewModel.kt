package com.taskweave.android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskweave.android.data.remote.dto.AgenticAction
import com.taskweave.android.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val role: String, val content: String) {
    val isUser: Boolean get() = role == "user"
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val quickReplies: List<String> = emptyList(),
    val mode: String? = null,
    val agenticAction: AgenticAction? = null,
    val sending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _state.value.sending) return

        _state.update {
            it.copy(
                messages = it.messages + ChatMessage("user", message),
                quickReplies = emptyList(),
                sending = true,
                error = null,
            )
        }

        viewModelScope.launch {
            repository.send(message)
                .onSuccess { response ->
                    val reply = response.chatUi.agentMessage.ifBlank { "…" }
                    _state.update {
                        it.copy(
                            messages = it.messages + ChatMessage("assistant", reply),
                            quickReplies = response.chatUi.suggestedQuickReplies,
                            mode = response.currentMode ?: it.mode,
                            agenticAction = response.agenticAction ?: it.agenticAction,
                            sending = false,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(sending = false, error = e.message ?: "Message failed")
                    }
                }
        }
    }

    fun dismissAgenticCard() = _state.update { it.copy(agenticAction = null) }

    fun consumeError() = _state.update { it.copy(error = null) }
}
