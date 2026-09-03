package com.taskweave.android.data.repository

import com.taskweave.android.data.model.Mode
import com.taskweave.android.data.remote.dto.StatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the engine's current behavioural [Mode]. Authoritative source is the
 * `current_mode` field on each `/api/chat` reply; between chats we fall back to a
 * guess derived from `/api/status` (overdue/at-risk work ⇒ Panic).
 */
@Singleton
class ModeRepository @Inject constructor() {

    private val fromChat = MutableStateFlow<Mode?>(null)
    private val fromStatus = MutableStateFlow<Mode?>(null)

    private val _mode = MutableStateFlow(Mode.PLANNING)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    fun onChatMode(wire: String?) {
        Mode.fromWire(wire)?.let {
            fromChat.value = it
            recompute()
        }
    }

    fun onStatus(status: StatusResponse) {
        fromStatus.value = when {
            status.overdue.isNotEmpty() || status.atRisk.isNotEmpty() -> Mode.PANIC
            status.recommendReschedule -> Mode.PLANNING
            else -> null
        }
        recompute()
    }

    private fun recompute() {
        _mode.value = fromChat.value ?: fromStatus.value ?: Mode.PLANNING
    }
}
