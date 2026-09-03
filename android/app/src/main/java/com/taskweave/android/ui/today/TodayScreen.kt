package com.taskweave.android.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskweave.android.R
import com.taskweave.android.data.model.Task
import com.taskweave.android.ui.components.BrutalButton
import com.taskweave.android.ui.components.BrutalCard
import com.taskweave.android.ui.components.EmptyState
import com.taskweave.android.ui.components.MetaLabel
import com.taskweave.android.ui.components.SectionHeader
import com.taskweave.android.ui.components.rowClick
import com.taskweave.android.ui.theme.Brand
import com.taskweave.android.ui.theme.color
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HourFmt = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenTask: (String) -> Unit,
    onOpenChat: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scheduling by viewModel.isScheduling.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        containerColor = Brand.Cream,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_today), style = MaterialTheme.typography.displaySmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Cream),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            state.nextStepTask?.let { task ->
                BrutalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    accent = Brand.Orange,
                    shadowColor = Brand.Orange,
                    onClick = { onOpenTask(task.id) },
                ) {
                    MetaLabel(stringResource(R.string.next_step), color = Brand.Orange)
                    Text(
                        task.nextStep ?: task.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (task.nextStep != null) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            BrutalButton(
                text = if (scheduling) "Scheduling…" else stringResource(R.string.run_schedule),
                onClick = viewModel::runSchedule,
                enabled = !scheduling,
                filled = true,
                color = Brand.Green,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )

            Spacer(Modifier.height(20.dp))
            SectionHeader("Today’s blocks")
            Spacer(Modifier.height(8.dp))

            if (state.tasks.isEmpty()) {
                EmptyState(
                    title = "Nothing scheduled today",
                    subtitle = "Tap “${stringResource(R.string.run_schedule)}” or ask the Coach.",
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.tasks, key = { it.id }) { task ->
                        TodayRow(task = task, onClick = { onOpenTask(task.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayRow(task: Task, onClick: () -> Unit) {
    val time = task.scheduledStart?.atZone(ZoneId.systemDefault())?.let(HourFmt::format)
    BrutalCard(
        modifier = Modifier.fillMaxWidth(),
        accent = task.priority.color(),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (time != null) {
                Text(
                    time,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(Brand.Ink)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    color = Brand.White,
                )
                Spacer(Modifier.width(10.dp))
            }
            Column {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                )
                task.nextStep?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
