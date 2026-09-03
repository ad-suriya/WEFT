package com.taskweave.android.ui.execution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskweave.android.data.model.FocusSession
import com.taskweave.android.data.model.Mode
import com.taskweave.android.data.model.Task
import com.taskweave.android.data.model.TaskStatus
import com.taskweave.android.data.remote.dto.StatusResponse
import com.taskweave.android.data.repository.ModeRepository
import com.taskweave.android.data.repository.SessionRepository
import com.taskweave.android.data.repository.StatusRepository
import com.taskweave.android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ExecutionContent(
    val mode: Mode = Mode.PLANNING,
    val currentTask: Task? = null,
    /** In Panic mode: the single most-at-risk task to surface alone. */
    val panicTask: Task? = null,
)

@HiltViewModel
class ExecutionViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val statusRepository: StatusRepository,
    modeRepository: ModeRepository,
) : ViewModel() {

    private val status = MutableStateFlow<StatusResponse?>(null)
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(1_000)
        }
    }

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val session: StateFlow<FocusSession?> = sessionRepository.active

    val secondsLeft: StateFlow<Long> = combine(sessionRepository.active, ticker) { s, _ ->
        s?.remainingSeconds() ?: 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), 0L)

    val content: StateFlow<ExecutionContent> =
        combine(taskRepository.tasks, modeRepository.mode, status) { tasks, mode, st ->
            ExecutionContent(
                mode = mode,
                currentTask = pickCurrent(tasks),
                panicTask = pickPanic(tasks, st),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExecutionContent())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { taskRepository.refresh() }
        viewModelScope.launch { sessionRepository.refresh() }
        viewModelScope.launch { statusRepository.fetch().onSuccess { status.value = it } }
    }

    fun startFocus(minutes: Int) {
        viewModelScope.launch {
            val label = content.value.let { it.panicTask ?: it.currentTask }?.title
            sessionRepository.start(label, minutes)
                .onFailure { _messages.trySend(it.message ?: "Couldn't start the session") }
            // Move the task into progress so "current" is unambiguous.
            (content.value.panicTask ?: content.value.currentTask)?.let { task ->
                if (task.status != TaskStatus.IN_PROGRESS) {
                    taskRepository.setStatus(task, TaskStatus.IN_PROGRESS)
                }
            }
        }
    }

    fun pause() = viewModelScope.launch { sessionRepository.pause() }
    fun resume() = viewModelScope.launch { sessionRepository.resume() }

    fun stop() = viewModelScope.launch {
        sessionRepository.stop().onFailure { _messages.trySend("Couldn't end the session") }
    }

    fun completeCurrent() {
        val task = content.value.panicTask ?: content.value.currentTask ?: return
        viewModelScope.launch {
            taskRepository.setStatus(task, TaskStatus.COMPLETED)
            if (session.value != null) sessionRepository.stop()
        }
    }

    fun skipCurrent() {
        val id = (content.value.panicTask ?: content.value.currentTask)?.id ?: return
        viewModelScope.launch {
            taskRepository.skip(id)
                .onSuccess { _messages.trySend("Skipped — recover it later from Today") }
                .onFailure { _messages.trySend(it.message ?: "Couldn't skip") }
        }
    }

    private fun pickCurrent(tasks: List<Task>): Task? {
        tasks.firstOrNull { it.status == TaskStatus.IN_PROGRESS }?.let { return it }
        val today = LocalDate.now()
        return tasks
            .filter { !it.isDone && it.scheduledStart != null }
            .sortedBy { it.scheduledStart }
            .firstOrNull { it.isScheduledOn(today) || it.scheduledStart!!.isAfter(java.time.Instant.now()) }
            ?: tasks.firstOrNull { !it.isDone }
    }

    private fun pickPanic(tasks: List<Task>, status: StatusResponse?): Task? {
        val byId = tasks.associateBy { it.id }
        status?.risks
            ?.sortedByDescending { it.riskScore ?: 0.0 }
            ?.firstNotNullOfOrNull { byId[it.taskId?.toString()] }
            ?.let { return it }
        return tasks.filter { !it.isDone && it.dueDate != null }
            .minByOrNull { it.dueDate!! }
    }
}
