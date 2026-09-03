package com.taskweave.android.data.repository

import com.taskweave.android.data.Iso8601
import com.taskweave.android.data.model.FocusSession
import com.taskweave.android.data.remote.TaskWeaveApi
import com.taskweave.android.data.remote.dto.SessionCreateDto
import com.taskweave.android.data.remote.dto.SessionPatchDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Focus/pomodoro sessions. The backend has no "active session" endpoint, so the
 * active one is derived (no `end_time`). The countdown itself runs client-side in
 * the ViewModel, anchored to [FocusSession.startedAt] so reopening recomputes.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val api: TaskWeaveApi,
) {
    private val _active = MutableStateFlow<FocusSession?>(null)
    val active: StateFlow<FocusSession?> = _active.asStateFlow()

    /** Local marker for when the current pause began (for break-minute accounting). */
    private var pausedSince: Instant? = null

    private fun publish(session: FocusSession?) {
        _active.value = session?.copy(pausedSince = if (session.isPaused) pausedSince else null)
    }

    suspend fun refresh(): Result<Unit> = runCatching {
        publish(
            api.getSessions()
                .map { it.toDomain() }
                .filter { it.isActive }
                .maxByOrNull { it.startedAt },
        )
    }

    suspend fun history(limit: Int = 30): Result<List<FocusSession>> = runCatching {
        api.getSessions().map { it.toDomain() }
            .sortedByDescending { it.startedAt }
            .take(limit)
    }

    suspend fun start(label: String?, minutes: Int): Result<FocusSession> = runCatching {
        val created = api.createSession(
            SessionCreateDto(
                description = label?.let { "Focus: $it" } ?: "Pomodoro focus session",
                durationMinutes = minutes,
            ),
        ).toDomain()
        pausedSince = null
        publish(created)
        created
    }

    suspend fun pause(): Result<Unit> = patchActive {
        pausedSince = Instant.now()
        SessionPatchDto(isPaused = true)
    }

    suspend fun resume(): Result<Unit> = patchActive { session ->
        val breakMin = pausedSince?.let {
            Duration.between(it, Instant.now()).toMinutes().toInt()
        } ?: 0
        pausedSince = null
        SessionPatchDto(
            isPaused = false,
            breaksTaken = session.breaksTaken + 1,
            totalBreakMinutes = session.totalBreakMinutes + breakMin,
        )
    }

    suspend fun stop(): Result<Unit> {
        val current = _active.value ?: return Result.success(Unit)
        return runCatching {
            api.patchSession(current.id, SessionPatchDto(endTime = Iso8601.format(Instant.now())))
            pausedSince = null
            _active.value = null
        }
    }

    private suspend inline fun patchActive(
        body: (FocusSession) -> SessionPatchDto,
    ): Result<Unit> {
        val current = _active.value ?: return Result.success(Unit)
        return runCatching {
            publish(api.patchSession(current.id, body(current)).toDomain())
        }
    }
}
