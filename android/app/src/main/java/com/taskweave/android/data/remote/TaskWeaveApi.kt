package com.taskweave.android.data.remote

import com.taskweave.android.data.remote.dto.ChatRequest
import com.taskweave.android.data.remote.dto.ChatResponse
import com.taskweave.android.data.remote.dto.DeviceRegisterDto
import com.taskweave.android.data.remote.dto.RecoverRequest
import com.taskweave.android.data.remote.dto.ReminderDto
import com.taskweave.android.data.remote.dto.ReminderWriteDto
import com.taskweave.android.data.remote.dto.ScheduleRequest
import com.taskweave.android.data.remote.dto.ScheduleResponse
import com.taskweave.android.data.remote.dto.SessionCreateDto
import com.taskweave.android.data.remote.dto.SessionDto
import com.taskweave.android.data.remote.dto.SessionPatchDto
import com.taskweave.android.data.remote.dto.StatusResponse
import com.taskweave.android.data.remote.dto.TaskDto
import com.taskweave.android.data.remote.dto.TaskWriteDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * The same REST surface the web dashboard and Chrome extension use.
 * Base URL + `Authorization: Bearer <Google ID token>` are added by OkHttp
 * interceptors (see [NetworkModule]); callers never pass the token.
 */
interface TaskWeaveApi {

    // --- Tasks ---------------------------------------------------------------
    @GET("api/tasks")
    suspend fun getTasks(): List<TaskDto>

    @POST("api/tasks")
    suspend fun createTask(@Body body: TaskWriteDto): TaskDto

    @PATCH("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body body: TaskWriteDto): TaskDto

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String)

    @POST("api/tasks/{id}/skip")
    suspend fun skipTask(@Path("id") id: String)

    @POST("api/tasks/recover")
    suspend fun recoverTasks(@Body body: RecoverRequest): List<TaskDto>

    // --- Focus sessions (pomodoro) ----------------------------------------------
    @GET("api/sessions")
    suspend fun getSessions(): List<SessionDto>

    @POST("api/sessions")
    suspend fun createSession(@Body body: SessionCreateDto): SessionDto

    @PATCH("api/sessions/{id}")
    suspend fun patchSession(@Path("id") id: String, @Body body: SessionPatchDto): SessionDto

    @DELETE("api/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String)

    // --- AI chat ----------------------------------------------------------------
    @POST("api/chat")
    suspend fun chat(@Body body: ChatRequest): ChatResponse

    // --- Scheduling -----------------------------------------------------------
    @POST("api/schedule")
    suspend fun schedule(@Body body: ScheduleRequest): ScheduleResponse

    @POST("api/reschedule")
    suspend fun reschedule(@Body body: ScheduleRequest): ScheduleResponse

    // --- Status -------------------------------------------------------------
    @GET("api/status")
    suspend fun status(): StatusResponse

    // --- Reminders --------------------------------------------------------------
    @GET("api/reminders")
    suspend fun getReminders(): List<ReminderDto>

    @POST("api/reminders")
    suspend fun createReminder(@Body body: ReminderWriteDto): ReminderDto

    @POST("api/reminders/{id}/ack")
    suspend fun ackReminder(@Path("id") id: String)

    @DELETE("api/reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: String)

    // --- Devices (push) -----------------------------------------------------
    @POST("api/devices")
    suspend fun registerDevice(@Body body: DeviceRegisterDto)

    @DELETE("api/devices/{token}")
    suspend fun unregisterDevice(@Path("token") token: String)
}
