package com.taskweave.android.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskweave.android.auth.TokenStore
import com.taskweave.android.data.repository.StatusRepository
import com.taskweave.android.data.repository.TaskRepository
import com.taskweave.android.notifications.StatusNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic (and pull-to-refresh) sync: re-pull tasks and hit `/api/status` so the
 * server can run its due-workflow / recovery / push piggyback. Surfaced at-risk
 * items are shown as a local notification as a fallback when FCM is delayed.
 */
@HiltWorker
class StatusSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val tokenStore: TokenStore,
    private val taskRepository: TaskRepository,
    private val statusRepository: StatusRepository,
    private val statusNotifier: StatusNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (tokenStore.idToken == null) return Result.success() // signed out — nothing to do

        taskRepository.refresh()
        val status = statusRepository.fetch().getOrElse { return Result.retry() }
        statusNotifier.notifyFromStatus(status)
        return Result.success()
    }
}
