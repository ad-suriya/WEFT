package com.taskweave.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.taskweave.android.focuslock.FocusLockController
import com.taskweave.android.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TaskWeaveApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var focusLockController: FocusLockController

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        syncScheduler.ensurePeriodicStatusSync()
        focusLockController.start()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            getString(R.string.fcm_default_channel_id),
            getString(R.string.fcm_default_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "At-risk deadlines, recovery nudges, and due reminders."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
