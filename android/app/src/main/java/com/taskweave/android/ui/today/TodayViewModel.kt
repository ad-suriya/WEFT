package com.taskweave.android.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskweave.android.data.model.Task
import com.taskweave.android.data.repository.ScheduleRepository
import com.taskweave.android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayUiState(
    val tasks: List<Task> = emptyList(),
    val nextStepTask: Task? = null,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    val state: StateFlow<TodayUiState> = taskRepository.tasks
        .map { all ->
            val today = LocalDate.now()
            val scheduled = all
                .filter { it.isScheduledOn(today) }
                .sortedBy { it.scheduledStart }
            TodayUiState(
                tasks = scheduled,
                nextStepTask = scheduled.firstOrNull { !it.isDone }
                    ?: all.firstOrNull { !it.isDone && it.nextStep != null },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    val isScheduling = MutableStateFlow(false)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch { taskRepository.refresh() }
    }

    fun runSchedule() {
        if (isScheduling.value) return
        viewModelScope.launch {
            isScheduling.value = true
            scheduleRepository.scheduleDay()
                .onSuccess { _messages.trySend(it ?: "Your day is scheduled") }
                .onFailure { _messages.trySend(it.message ?: "Couldn't schedule right now") }
            isScheduling.value = false
        }
    }
}
