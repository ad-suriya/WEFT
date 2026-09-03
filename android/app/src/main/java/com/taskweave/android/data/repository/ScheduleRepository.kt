package com.taskweave.android.data.repository

import com.taskweave.android.data.remote.TaskWeaveApi
import com.taskweave.android.data.remote.dto.ScheduleRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val api: TaskWeaveApi,
    private val taskRepository: TaskRepository,
) {
    /** Run the AI time-blocker for [date] (null = today), then refresh the cache. */
    suspend fun scheduleDay(date: String? = null): Result<String?> =
        runCatching { api.schedule(ScheduleRequest(date = date)) }
            .onSuccess { taskRepository.refresh() }
            .map { it.message }

    suspend fun reschedule(taskIds: List<String>? = null): Result<String?> =
        runCatching { api.reschedule(ScheduleRequest(taskIds = taskIds)) }
            .onSuccess { taskRepository.refresh() }
            .map { it.message }
}
