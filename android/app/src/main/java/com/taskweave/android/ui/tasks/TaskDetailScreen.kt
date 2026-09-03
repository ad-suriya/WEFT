package com.taskweave.android.ui.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskweave.android.data.model.TaskStatus
import com.taskweave.android.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(taskId) { viewModel.load(taskId) }
    val task by viewModel.task.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.delete(); onBack() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        val current = task
        if (current == null) {
            EmptyState(title = "Task not found", subtitle = "It may have been completed or removed.")
            return@Scaffold
        }

        var title by remember(current.id) { mutableStateOf(current.title) }
        var notes by remember(current.id) { mutableStateOf(current.description.orEmpty()) }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            Row(modifier = Modifier.padding(top = 12.dp)) {
                TaskStatus.entries.filter { it != TaskStatus.SKIPPED }.forEach { status ->
                    FilterChip(
                        selected = current.status == status,
                        onClick = { viewModel.setStatus(status) },
                        label = { Text(status.name.lowercase().replace('_', ' ')) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            current.nextStep?.let {
                Text(
                    "Next micro-step: $it",
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Button(
                onClick = { viewModel.save(title, notes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                enabled = title.isNotBlank() &&
                    (title != current.title || notes != current.description.orEmpty()),
            ) { Text("Save") }
        }
    }
}
