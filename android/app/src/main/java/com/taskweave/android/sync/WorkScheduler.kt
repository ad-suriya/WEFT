package com.taskweave.android.sync

/**
 * Schedules the app's background work. Extracted as an interface so repositories
 * that trigger a sync stay unit-testable without WorkManager.
 */
interface WorkScheduler {
    /** Idempotently enqueue the periodic `/api/status` sync. */
    fun ensurePeriodicStatusSync()

    /** Kick a one-off status + task sync now (e.g. right after sign-in). */
    fun syncNow()

    /** Ask for the offline write queue to be replayed as soon as there's network. */
    fun requestPendingSync()
}
