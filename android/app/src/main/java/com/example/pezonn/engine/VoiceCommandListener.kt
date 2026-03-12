package com.example.pezonn.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

sealed class VoiceCommand {
    data object Start : VoiceCommand()
    data object Stop : VoiceCommand()
    data class SetBpm(val bpm: Int) : VoiceCommand()
}

class VoiceCommandListener(
    private val context: Context,
    private val onCommand: (VoiceCommand) -> Unit,
    private val onDebugLog: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())

    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            log("[MIC] Listening...")
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            log("[MIC] Processing...")
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout"
                SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "No permission"
                else -> "Error $error"
            }
            log("[ERR] $msg")
            restartListening()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches != null) {
                for (match in matches) {
                    log("[RES] $match")
                }
                parseCommand(matches)
            }
            restartListening()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                log("[...] ${matches[0]}")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun log(message: String) {
        Log.d("VoiceCmd", message)
        onDebugLog(message)
    }

    private fun parseCommand(matches: List<String>) {
        for (text in matches) {
            val lower = text.lowercase().trim()
            val nippleIndex = lower.indexOf("nipple")
            if (nippleIndex == -1) continue

            val afterNipple = lower.substring(nippleIndex + 6).trim()

            when {
                afterNipple.startsWith("stop") -> {
                    log("[CMD] >> STOP")
                    onCommand(VoiceCommand.Stop)
                    return
                }
                afterNipple.startsWith("start") -> {
                    log("[CMD] >> START")
                    onCommand(VoiceCommand.Start)
                    return
                }
                else -> {
                    val number = afterNipple.split("\\s+".toRegex())
                        .firstOrNull()
                        ?.toIntOrNull()
                    if (number != null) {
                        log("[CMD] >> BPM $number")
                        onCommand(VoiceCommand.SetBpm(number))
                        return
                    }
                }
            }
        }
    }

    private fun restartListening() {
        if (!isListening) return
        handler.postDelayed({
            if (isListening) {
                try {
                    speechRecognizer?.startListening(recognizerIntent)
                } catch (e: Exception) {
                    log("[ERR] Restart failed: ${e.message}")
                }
            }
        }, 300)
    }

    fun startListening() {
        if (isListening) return
        isListening = true
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            it.startListening(recognizerIntent)
        }
        log("[MIC] Voice commands ON")
    }

    fun stopListening() {
        isListening = false
        speechRecognizer?.apply {
            cancel()
            destroy()
        }
        speechRecognizer = null
        log("[MIC] Voice commands OFF")
    }

    fun destroy() {
        stopListening()
    }
}
