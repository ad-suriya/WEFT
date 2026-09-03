package com.taskweave.android.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.taskweave.android.R
import com.taskweave.android.data.remote.dto.StatusResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

const val DEEP_LINK_SCHEME = "taskweave"

fun taskDeepLink(taskId: String): Uri = Uri.parse("$DEEP_LINK_SCHEME://task/$taskId")

/** Builds a "tap opens the task" content intent. */
fun taskContentIntent(context: Context, taskId: String?): PendingIntent {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setPackage(context.packageName)
        data = if (taskId.isNullOrBlank()) Uri.parse("$DEEP_LINK_SCHEME://task/") else taskDeepLink(taskId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    return PendingIntent.getActivity(
        context,
        (taskId ?: "home").hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission") // guarded by canPostNotifications()
fun Context.showTaskWeaveNotification(
    id: Int,
    title: String,
    body: String,
    taskId: String?,
) {
    if (!canPostNotifications()) return
    val notification = NotificationCompat.Builder(this, getString(R.string.fcm_default_channel_id))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(taskContentIntent(this, taskId))
        .build()
    NotificationManagerCompat.from(this).notify(id, notification)
}

/** Fallback local notifications when `/api/status` surfaces something and FCM lags. */
@Singleton
class StatusNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun notifyFromStatus(status: StatusResponse) {
        status.risks
            .filter { it.riskLevel.equals("high", ignoreCase = true) }
            .forEach { risk ->
                val taskId = risk.taskId?.toString()
                context.showTaskWeaveNotification(
                    id = ("atrisk:$taskId").hashCode(),
                    title = "Deadline at risk",
                    body = risk.reason ?: "A task is likely to miss its deadline.",
                    taskId = taskId,
                )
            }
        if (status.recommendReschedule && status.overdue.isNotEmpty()) {
            context.showTaskWeaveNotification(
                id = "reschedule".hashCode(),
                title = "Time to recover your day",
                body = "${status.overdue.size} task(s) slipped — reschedule to catch up.",
                taskId = null,
            )
        }
    }
}
