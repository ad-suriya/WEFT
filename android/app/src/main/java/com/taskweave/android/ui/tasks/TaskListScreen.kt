package com.taskweave.android.ui.tasks

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskweave.android.R
import com.taskweave.android.data.model.Task
import com.taskweave.android.ui.components.EmptyState
import com.taskweave.android.ui.components.rowClick
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFmt = DateTimeFormatter.ofPattern("EEE HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    prefillTitle: String?,
    displayName: String,
    onOpenTask: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val refreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pending by viewModel.pendingWriteCount.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var showAdd by remember { mutableStateOf(prefillTitle != null) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_tasks)) },
                actions = {
                    if (pending > 0) {
                        Text(
                            "$pending queued",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sign_out) + "  ·  $displayName") },
                            onClick = {
                                menuOpen = false
                                onSignOut()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_task))
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (tasks.isEmpty() && !refreshing) {
                EmptyState(
                    title = "No tasks yet",
                    subtitle = "Add one with the + button, or ask the Coach to plan your week.",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggleComplete(task) },
                            onClick = { onOpenTask(task.id) },
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddTaskDialog(
            initialTitle = prefillTitle.orEmpty(),
            onDismiss = { showAdd = false },
            onConfirm = { title, notes ->
                viewModel.addTask(title = title, description = notes)
                showAdd = false
            },
        )
    }
}

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                task.title,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            )
        },
        supportingContent = {
            val bits = buildList {
                task.scheduledStart?.let { add(TimeFmt.format(it.atZone(ZoneId.systemDefault()))) }
                if (task.pendingSync) add("syncing…")
                task.project?.let { add(it) }
            }
            if (bits.isNotEmpty()) Text(bits.joinToString("  ·  "))
        },
        leadingContent = { Checkbox(checked = task.isDone, onCheckedChange = { onToggle() }) },
        modifier = Modifier.rowClick(onClick),
    )
}
