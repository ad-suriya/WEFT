package com.taskweave.android.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.taskweave.android.auth.TokenStore
import com.taskweave.android.data.repository.DeviceRepository
import com.taskweave.android.notifications.showTaskWeaveNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives pushes from the backend push job (see backend `push.py`). Payload:
 *   notification: { title, body }
 *   data: { type, task_id, reminder_id }
 */
@AndroidEntryPoint
class TaskWeaveMessagingService : FirebaseMessagingService() {

    @Inject lateinit var deviceRepository: DeviceRepository
    @Inject lateinit var tokenStore: TokenStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        if (tokenStore.idToken == null) return // will register on next sign-in
        scope.launch { deviceRepository.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val taskId = data["task_id"]?.takeIf { it.isNotBlank() }
        val title = message.notification?.title ?: "Task Weave"
        val body = message.notification?.body ?: data["body"] ?: return
        val notificationId = (data["reminder_id"] ?: data["type"] ?: body).hashCode()

        showTaskWeaveNotification(
            id = notificationId,
            title = title,
            body = body,
            taskId = taskId,
        )
    }
}
