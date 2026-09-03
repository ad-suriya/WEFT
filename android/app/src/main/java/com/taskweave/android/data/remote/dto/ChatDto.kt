package com.taskweave.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for `POST /api/chat`. The backend keeps conversational state server-side. */
@Serializable
data class ChatRequest(
    val message: String,
)

/**
 * `POST /api/chat` response (verified against the live API):
 * `chat_ui` (what to render) + `current_mode` + optional `agentic_action`
 * (generated starter content) + `system_trigger` + the updated `tasks` list.
 */
@Serializable
data class ChatResponse(
    @SerialName("chat_ui") val chatUi: ChatUi = ChatUi(),
    @SerialName("current_mode") val currentMode: String? = null,
    @SerialName("agentic_action") val agenticAction: AgenticAction? = null,
    @SerialName("system_trigger") val systemTrigger: String? = null,
    val tasks: List<TaskDto> = emptyList(),
)

@Serializable
data class ChatUi(
    @SerialName("agent_message") val agentMessage: String = "",
    @SerialName("suggested_quick_replies") val suggestedQuickReplies: List<String> = emptyList(),
)

@Serializable
data class AgenticAction(
    @SerialName("action_type") val actionType: String? = null,   // e.g. CREATE_OUTLINE, DRAFT_EMAIL
    @SerialName("action_content") val actionContent: String? = null,
)
