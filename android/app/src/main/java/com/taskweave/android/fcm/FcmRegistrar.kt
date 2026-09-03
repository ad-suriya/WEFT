package com.taskweave.android.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.taskweave.android.data.repository.DeviceRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Called on sign-in and sign-out to keep `/api/devices` in sync with this device. */
@Singleton
class FcmRegistrar @Inject constructor(
    private val deviceRepository: DeviceRepository,
) {
    suspend fun registerCurrentToken(): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        deviceRepository.register(token).getOrThrow()
    }

    suspend fun unregisterAndDelete(): Result<Unit> = runCatching {
        val messaging = FirebaseMessaging.getInstance()
        val token = runCatching { messaging.token.await() }.getOrNull()
        if (token != null) deviceRepository.unregister(token)
        messaging.deleteToken().await()
    }
}
