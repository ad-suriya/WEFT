package com.taskweave.android.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskweave.android.auth.TokenStore
import com.taskweave.android.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Replays queued offline writes; retries with backoff until the queue drains. */
@HiltWorker
class PendingOpWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val tokenStore: TokenStore,
    private val taskRepository: TaskRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (tokenStore.idToken == null) return Result.success()
        val drained = runCatching { taskRepository.replayPending() }.getOrDefault(false)
        return if (drained) {
            taskRepository.refresh()
            Result.success()
        } else {
            Result.retry()
        }
    }
}
