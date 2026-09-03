package com.taskweave.android.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskweave.android.data.model.Task
import com.taskweave.android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingWriteCount: StateFlow<Int> = repository.pendingWriteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isRefreshing = MutableStateFlow(false)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.refresh().onFailure { _messages.trySend("Couldn't sync — showing cached tasks") }
            isRefreshing.value = false
        }
    }

    fun addTask(title: String, description: String? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addTask(title, description)
                .onFailure { _messages.trySend(it.message ?: "Couldn't add task") }
        }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            repository.toggleComplete(task)
                .onFailure { _messages.trySend(it.message ?: "Update failed") }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.deleteTask(id)
                .onFailure { _messages.trySend(it.message ?: "Delete failed") }
        }
    }
}
