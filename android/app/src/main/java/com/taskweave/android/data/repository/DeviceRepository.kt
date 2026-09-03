package com.taskweave.android.data.repository

import com.taskweave.android.data.remote.TaskWeaveApi
import com.taskweave.android.data.remote.dto.DeviceRegisterDto
import javax.inject.Inject
import javax.inject.Singleton

/** Registers this device's FCM token so the backend push job can reach it. */
@Singleton
class DeviceRepository @Inject constructor(
    private val api: TaskWeaveApi,
) {
    suspend fun register(token: String): Result<Unit> =
        runCatching { api.registerDevice(DeviceRegisterDto(token = token)) }.map { }

    suspend fun unregister(token: String): Result<Unit> =
        runCatching { api.unregisterDevice(token) }.map { }

    suspend fun ackReminder(reminderId: String): Result<Unit> =
        runCatching { api.ackReminder(reminderId) }.map { }
}
