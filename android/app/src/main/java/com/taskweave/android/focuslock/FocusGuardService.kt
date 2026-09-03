package com.taskweave.android.focuslock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.taskweave.android.R
import com.taskweave.android.focuslock.FocusLockActivity.Companion.launchOver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * While running as a foreground service, polls the foreground app ~1×/s via
 * [UsageStatsManager]; if it's on the blocklist, throws up [FocusLockActivity].
 * Started/stopped by [FocusLockController] — never directly by the UI.
 */
@AndroidEntryPoint
class FocusGuardService : Service() {

    @Inject lateinit var blocklistRepository: BlocklistRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var blocked: Set<String> = emptySet()
    @Volatile private var lastInterruptAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        blocklistRepository.blockedPackages
            .onEach { blocked = it }
            .launchIn(scope)
        scope.launch { pollLoop() }
    }

    private suspend fun pollLoop() {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        while (scope.isActive) {
            val fg = currentForegroundPackage(usm)
            if (fg != null && fg != packageName && fg in blocked) {
                val now = System.currentTimeMillis()
                if (now - lastInterruptAt > 1_200 && !FocusLockActivity.isVisible) {
                    lastInterruptAt = now
                    launchOver(this, fg)
                }
            }
            delay(800)
        }
    }

    @Suppress("DEPRECATION")
    private fun currentForegroundPackage(usm: UsageStatsManager): String? {
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - 10_000, end)
        val event = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                latest = event.packageName
            }
        }
        if (latest != null) return latest
        // Fallback when no transition happened in the window.
        return usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, end - 10_000, end)
            ?.maxByOrNull { it.lastTimeUsed }
            ?.takeIf { it.lastTimeUsed >= end - 10_000 }
            ?.packageName
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Focus Lock", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Shown while distracting apps are blocked." },
            )
        }
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val open = PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus Lock is on")
            .setContentText("Distracting apps are blocked until your session ends.")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL = "focus_lock"
        private const val NOTIF_ID = 4210

        fun start(context: Context) {
            val i = Intent(context, FocusGuardService::class.java)
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FocusGuardService::class.java))
        }
    }
}
