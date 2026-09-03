package com.taskweave.android.ui.execution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskweave.android.data.model.FocusSession
import com.taskweave.android.data.model.Mode
import com.taskweave.android.data.model.Task
import com.taskweave.android.ui.components.EmptyState
import com.taskweave.android.ui.components.ModeBadge
import com.taskweave.android.ui.focuslock.FocusLockCard
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DeadlineFmt = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")
private val DURATIONS = listOf(15, 25, 50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionScreen(
    onOpenTask: (String) -> Unit,
    onChooseApps: () -> Unit,
    viewModel: ExecutionViewModel = hiltViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val secondsLeft by viewModel.secondsLeft.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Focus") },
                actions = { ModeBadge(content.mode, Modifier.padding(end = 12.dp)) },
            )
        },
    ) { padding ->
        val task = content.panicTask ?: content.currentTask
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (content.mode == Mode.PANIC && content.panicTask != null) {
                Text(
                    "One thing. Right now.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (task == null) {
                EmptyState(
                    title = "Nothing to focus on",
                    subtitle = "Add a task or ask the Coach to plan your day.",
                )
            } else {
                TaskFocusCard(task = task, onOpen = { onOpenTask(task.id) })

                if (session != null) {
                    TimerCard(
                        session = session!!,
                        secondsLeft = secondsLeft,
                        onPause = viewModel::pause,
                        onResume = viewModel::resume,
                        onStop = viewModel::stop,
                    )
                } else {
                    StartCard(onStart = viewModel::startFocus)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::completeCurrent,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Mark done")
                    }
                    OutlinedButton(onClick = viewModel::skipCurrent, modifier = Modifier.weight(1f)) {
                        Text("Skip")
                    }
                }
            }

            FocusLockCard(onChooseApps = onChooseApps)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskFocusCard(task: Task, onOpen: () -> Unit) {
    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Current task", style = MaterialTheme.typography.labelMedium)
            Text(task.title, style = MaterialTheme.typography.headlineSmall)
            task.nextStep?.let {
                Text(
                    "Next micro-step",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                task.dueDate?.let { due ->
                    AssistChip(
                        onClick = {},
                        label = { Text("Due ${DeadlineFmt.format(due.atZone(ZoneId.systemDefault()))}") },
                    )
                }
                task.riskLevel?.let { AssistChip(onClick = {}, label = { Text("Risk: $it") }) }
            }
        }
    }
}

@Composable
private fun TimerCard(
    session: FocusSession,
    secondsLeft: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val total = (session.plannedMinutes * 60).coerceAtLeast(1)
    val fraction = (secondsLeft.toFloat() / total).coerceIn(0f, 1f)
    val overtime = session.plannedMinutes > 0 && secondsLeft == 0L && !session.isPaused

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (overtime) 1f else fraction },
                    modifier = Modifier.size(180.dp),
                    strokeWidth = 10.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatMmSs(secondsLeft),
                        fontSize = 44.sp,
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        when {
                            session.isPaused -> "Paused"
                            overtime -> "Overtime"
                            else -> "${session.plannedMinutes} min block"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (session.isPaused) {
                    FilledTonalButton(onClick = onResume) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("Resume")
                    }
                } else {
                    FilledTonalButton(onClick = onPause) {
                        Icon(Icons.Filled.Pause, contentDescription = null)
                        Text("Pause")
                    }
                }
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Text("End")
                }
            }
        }
    }
}

@Composable
private fun StartCard(onStart: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Start a focus block", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pick a length — the timer keeps running against real time even if you leave.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DURATIONS.forEach { m ->
                    FilledTonalButton(onClick = { onStart(m) }) { Text("$m min") }
                }
            }
        }
    }
}

private fun formatMmSs(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
