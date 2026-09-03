package com.taskweave.android.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskweave.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    var composer by remember { mutableStateOf("") }
    val voice = rememberVoiceInput(
        onPartial = { composer = it },
        onFinal = { composer = it },
    )

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    fun submit() {
        if (composer.isBlank()) return
        viewModel.send(composer)
        composer = ""
        if (voice.isListening) voice.stop()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_chat)) }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.padding(8.dp)) {
                    if (state.quickReplies.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.quickReplies.forEach { reply ->
                                AssistChip(onClick = { viewModel.send(reply) }, label = { Text(reply) })
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = composer,
                            onValueChange = { composer = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.chat_hint)) },
                            maxLines = 4,
                        )
                        IconButton(
                            onClick = { if (voice.isListening) voice.stop() else voice.start() },
                            enabled = voice.isAvailable,
                        ) {
                            Icon(
                                if (voice.isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = stringResource(R.string.voice_input),
                            )
                        }
                        IconButton(onClick = ::submit, enabled = composer.isNotBlank() && !state.sending) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages) { message -> MessageBubble(message) }
                state.agenticAction?.let { action ->
                    item {
                        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    action.actionType?.replace('_', ' ')?.lowercase()
                                        ?.replaceFirstChar { it.uppercase() }
                                        ?: "Starter draft",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    action.actionContent.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        clipboard.setText(AnnotatedString(action.actionContent.orEmpty()))
                                    }) {
                                        Icon(
                                            Icons.Filled.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 4.dp),
                                        )
                                        Text(stringResource(R.string.copy_starter))
                                    }
                                    TextButton(onClick = viewModel::dismissAgenticCard) { Text("Dismiss") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val color =
        if (message.isUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    Column(Modifier.fillMaxWidth()) {
        Surface(
            color = color,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(alignment)
                .padding(vertical = 2.dp),
        ) {
            Text(
                message.content,
                modifier = Modifier
                    .background(color)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
