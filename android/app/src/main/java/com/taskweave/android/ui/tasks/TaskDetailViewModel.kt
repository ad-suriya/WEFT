package com.taskweave.android.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskweave.android.data.model.Task
import com.taskweave.android.data.model.TaskStatus
import com.taskweave.android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    private val taskId = MutableStateFlow<String?>(null)

    val task: StateFlow<Task?> = taskId
        .flatMapLatest { id ->
            repository.tasks.map { list -> list.firstOrNull { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun load(id: String) {
        taskId.value = id
    }

    fun save(title: String, notes: String) {
        val current = task.value ?: return
        viewModelScope.launch {
            repository.updateTask(
                current.copy(
                    title = title.trim(),
                    description = notes.trim().ifBlank { null },
                ),
            )
        }
    }

    fun setStatus(status: TaskStatus) {
        val current = task.value ?: return
        viewModelScope.launch { repository.setStatus(current, status) }
    }

    fun delete() {
        val id = taskId.value ?: return
        viewModelScope.launch { repository.deleteTask(id) }
    }
}
