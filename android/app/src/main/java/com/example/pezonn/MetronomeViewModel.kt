package com.example.pezonn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pezonn.engine.MetronomeEngine
import com.example.pezonn.engine.VoiceCommand
import com.example.pezonn.engine.VoiceCommandListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class MetronomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _bpm = MutableStateFlow(120)
    val bpm = _bpm.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentBeat = MutableStateFlow(-1)
    val currentBeat = _currentBeat.asStateFlow()

    private val _beatsPerMeasure = MutableStateFlow(4)
    val beatsPerMeasure = _beatsPerMeasure.asStateFlow()

    private val _debugLog = MutableStateFlow<List<String>>(emptyList())
    val debugLog = _debugLog.asStateFlow()

    private val _isVoiceActive = MutableStateFlow(false)
    val isVoiceActive = _isVoiceActive.asStateFlow()

    private val engine = MetronomeEngine { beatIndex ->
        _currentBeat.value = beatIndex
    }

    private val voiceListener = VoiceCommandListener(
        context = application,
        onCommand = ::handleVoiceCommand,
        onDebugLog = ::addDebugLog
    )

    private val tapTimes = mutableListOf<Long>()

    fun setBpm(newBpm: Int) {
        val clamped = newBpm.coerceIn(20, 300)
        _bpm.value = clamped
        engine.bpm = clamped
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            engine.stop()
            _isPlaying.value = false
            _currentBeat.value = -1
        } else {
            engine.bpm = _bpm.value
            engine.beatsPerMeasure = _beatsPerMeasure.value
            engine.start()
            _isPlaying.value = true
        }
    }

    fun tapTempo() {
        val now = System.currentTimeMillis()
        if (tapTimes.isNotEmpty() && now - tapTimes.last() > 2000) {
            tapTimes.clear()
        }
        tapTimes.add(now)
        if (tapTimes.size > 5) tapTimes.removeFirst()
        if (tapTimes.size >= 2) {
            val avg = tapTimes.zipWithNext { a, b -> b - a }.average()
            setBpm((60000.0 / avg).roundToInt())
        }
    }

    fun toggleVoiceListening() {
        if (_isVoiceActive.value) {
            voiceListener.stopListening()
            _isVoiceActive.value = false
        } else {
            voiceListener.startListening()
            _isVoiceActive.value = true
        }
    }

    private fun handleVoiceCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.Start -> {
                if (!_isPlaying.value) togglePlayback()
            }
            VoiceCommand.Stop -> {
                if (_isPlaying.value) togglePlayback()
            }
            is VoiceCommand.SetBpm -> setBpm(command.bpm)
        }
    }

    private fun addDebugLog(message: String) {
        _debugLog.value = (_debugLog.value + message).takeLast(30)
    }

    override fun onCleared() {
        super.onCleared()
        engine.stop()
        voiceListener.destroy()
    }
}
