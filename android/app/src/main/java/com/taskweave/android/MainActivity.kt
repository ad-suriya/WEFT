package com.taskweave.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskweave.android.ui.RootViewModel
import com.taskweave.android.ui.TaskWeaveRoot
import com.taskweave.android.ui.theme.TaskWeaveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TaskWeaveTheme {
                val vm: RootViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                TaskWeaveRoot(
                    state = state,
                    onSignIn = vm::signIn,
                    onSignOut = vm::signOut,
                    intentEvents = intentEvents,
                )
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private val intentEvents = kotlinx.coroutines.flow.MutableSharedFlow<ExternalIntent>(
        replay = 1,
        extraBufferCapacity = 4,
    )

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
                if (text.isNotBlank() || subject.isNotBlank()) {
                    intentEvents.tryEmit(ExternalIntent.ShareCapture(title = subject, body = text))
                }
            }
            Intent.ACTION_VIEW -> {
                val id = intent.data?.lastPathSegment
                if (!id.isNullOrBlank()) intentEvents.tryEmit(ExternalIntent.OpenTask(id))
            }
        }
    }
}

sealed interface ExternalIntent {
    data class ShareCapture(val title: String, val body: String) : ExternalIntent
    data class OpenTask(val taskId: String) : ExternalIntent
}
