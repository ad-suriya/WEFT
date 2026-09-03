package com.taskweave.android.ui.chat

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Mirrors the web app's mic feature: streams recognized speech (partial + final)
 * into whatever text the caller wants to update — here, the chat composer.
 */
class VoiceInputController(
    val isListening: Boolean,
    val isAvailable: Boolean,
    val start: () -> Unit,
    val stop: () -> Unit,
)

@Composable
fun rememberVoiceInput(
    onPartial: (String) -> Unit,
    onFinal: (String) -> Unit,
): VoiceInputController {
    val context = LocalContext.current
    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    var listening by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(false) }

    val currentPartial by rememberUpdatedState(onPartial)
    val currentFinal by rememberUpdatedState(onFinal)

    val recognizer = remember {
        if (available) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                listening = false
                results?.stringList()?.firstOrNull()?.let(currentFinal)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.stringList()?.firstOrNull()?.let(currentPartial)
            }

            override fun onError(error: Int) {
                listening = false
            }

            override fun onEndOfSpeech() {
                listening = false
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { recognizer?.destroy() }
    }

    fun beginListening() {
        recognizer ?: return
        listening = true
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        if (granted) beginListening()
    }

    return VoiceInputController(
        isListening = listening,
        isAvailable = available,
        start = {
            if (permissionGranted) beginListening()
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        stop = {
            recognizer?.stopListening()
            listening = false
        },
    )
}

private fun Bundle.stringList(): List<String>? =
    getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
