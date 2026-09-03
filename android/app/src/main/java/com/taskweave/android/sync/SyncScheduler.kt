package com.taskweave.android.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : WorkScheduler {
    private val workManager get() = WorkManager.getInstance(context)

    override fun ensurePeriodicStatusSync() {
        val request = PeriodicWorkRequestBuilder<StatusSyncWorker>(Duration.ofMinutes(30))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(1))
            .build()
        workManager.enqueueUniquePeriodicWork(
            STATUS_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun syncNow() {
        val request = OneTimeWorkRequestBuilder<StatusSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        workManager.enqueueUniqueWork(STATUS_SYNC_ONESHOT, ExistingWorkPolicy.REPLACE, request)
    }

    override fun requestPendingSync() {
        val request = OneTimeWorkRequestBuilder<PendingOpWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(30))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueueUniqueWork(PENDING_OPS_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val STATUS_SYNC_WORK = "taskweave.status_sync.periodic"
        const val STATUS_SYNC_ONESHOT = "taskweave.status_sync.oneshot"
        const val PENDING_OPS_WORK = "taskweave.pending_ops"
    }
}
