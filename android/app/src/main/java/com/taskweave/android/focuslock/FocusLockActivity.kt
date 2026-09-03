package com.taskweave.android.focuslock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.taskweave.android.MainActivity
import com.taskweave.android.data.model.TaskStatus
import com.taskweave.android.data.repository.SessionRepository
import com.taskweave.android.data.repository.TaskRepository
import com.taskweave.android.ui.theme.TaskWeaveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Full-screen "get back to it" wall shown over a blocked app. */
@AndroidEntryPoint
class FocusLockActivity : ComponentActivity() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var taskRepository: TaskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goHome()
                    finish()
                }
            },
        )

        setContent {
            TaskWeaveTheme {
                var taskTitle by remember { mutableStateOf<String?>(null) }
                var nextStep by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val tasks = taskRepository.tasks.first()
                    val current = tasks.firstOrNull { it.status == TaskStatus.IN_PROGRESS && !it.isDone }
                        ?: tasks.firstOrNull { !it.isDone }
                    taskTitle = current?.title
                    nextStep = current?.nextStep
                }

                InterruptWall(
                    taskTitle = taskTitle,
                    nextStep = nextStep,
                    onBackToFocus = {
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        )
                        finish()
                    },
                    onEndSession = {
                        lifecycleScope.launch {
                            runCatching { sessionRepository.stop() }
                            goHome()
                            finish()
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isVisible = true
    }

    override fun onPause() {
        super.onPause()
        isVisible = false
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    companion object {
        @Volatile var isVisible = false

        fun launchOver(context: Context, blockedPackage: String) {
            context.startActivity(
                Intent(context, FocusLockActivity::class.java)
                    .putExtra("blocked_package", blockedPackage)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION,
                    ),
            )
        }
    }
}

@Composable
private fun InterruptWall(
    taskTitle: String?,
    nextStep: String?,
    onBackToFocus: () -> Unit,
    onEndSession: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 28.dp)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "You started a focus session.",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                "Finish first.",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )
            if (taskTitle != null) {
                Text(
                    taskTitle,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (nextStep != null) {
                Text(
                    nextStep,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Button(
                onClick = onBackToFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            ) { Text("Back to focus") }
            OutlinedButton(
                onClick = onEndSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) { Text("End session early") }
        }
    }
}
