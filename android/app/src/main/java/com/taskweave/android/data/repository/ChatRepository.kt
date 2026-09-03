package com.taskweave.android.data.repository

import com.taskweave.android.data.remote.TaskWeaveApi
import com.taskweave.android.data.remote.dto.ChatRequest
import com.taskweave.android.data.remote.dto.ChatResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: TaskWeaveApi,
    private val taskRepository: TaskRepository,
    private val modeRepository: ModeRepository,
) {
    suspend fun send(message: String): Result<ChatResponse> =
        runCatching { api.chat(ChatRequest(message = message.trim())) }
            .onSuccess { response ->
                modeRepository.onChatMode(response.currentMode)
                // The chat engine hands back the updated task list — refresh the cache.
                if (response.tasks.any { it.id != null }) {
                    taskRepository.refresh()
                }
            }
}
