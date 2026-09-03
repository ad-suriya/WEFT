package com.taskweave.android.data.repository

import com.taskweave.android.data.remote.TaskWeaveApi
import com.taskweave.android.data.remote.dto.StatusResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepository @Inject constructor(
    private val api: TaskWeaveApi,
    private val modeRepository: ModeRepository,
) {
    suspend fun fetch(): Result<StatusResponse> =
        runCatching { api.status() }.onSuccess { modeRepository.onStatus(it) }
}
