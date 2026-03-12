package com.example.pezonn.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

class MetronomeEngine(
    private val onBeat: (beatIndex: Int) -> Unit
) {
    private companion object {
        const val SAMPLE_RATE = 44100
        const val CLICK_SAMPLES = 1764 // ~40ms - tight and punchy
        const val SILENCE_CHUNK = 4410
    }

    @Volatile var bpm: Int = 120
    @Volatile var beatsPerMeasure: Int = 4
    @Volatile private var isRunning = false

    private var thread: Thread? = null
    private val tickSamples = generateClick(1000.0, 2800.0, 0.85)
    private val accentSamples = generateClick(1500.0, 3500.0, 1.0)
    private val silenceBuffer = ShortArray(SILENCE_CHUNK)

    private fun generateClick(bodyFreq: Double, clickFreq: Double, gain: Double): ShortArray {
        val rng = Random(42)
        return ShortArray(CLICK_SAMPLES) { i ->
            val t = i.toDouble() / SAMPLE_RATE

            // 1. Noise burst - sharp percussive attack like a wood block hit
            val attackEnv = exp(-t * 800.0)
            val noise = (rng.nextDouble() * 2.0 - 1.0) * attackEnv

            // 2. Body tone - resonance of the click
            val bodyEnv = exp(-t * 70.0)
            val body = sin(2.0 * PI * bodyFreq * t) * bodyEnv

            // 3. High click in the 2-4kHz range where ears are most sensitive
            val clickEnv = exp(-t * 500.0)
            val click = sin(2.0 * PI * clickFreq * t) * clickEnv

            // 4. Sub thump for weight
            val subEnv = exp(-t * 120.0)
            val sub = sin(2.0 * PI * bodyFreq * 0.5 * t) * subEnv * 0.3

            // Mix it
            val mix = noise * 0.7 + body * 0.8 + click * 0.6 + sub

            // Soft saturate to push RMS up without digital clipping
            val saturated = tanh(mix * 2.5) * gain

            (saturated * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        thread = Thread({
            val bufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack.play()
            var beatIndex = 0

            while (isRunning) {
                val samplesPerBeat = SAMPLE_RATE * 60 / bpm
                val click = if (beatIndex == 0) accentSamples else tickSamples
                onBeat(beatIndex)
                audioTrack.write(click, 0, click.size)

                var remaining = samplesPerBeat - click.size
                while (remaining > 0 && isRunning) {
                    val toWrite = minOf(remaining, SILENCE_CHUNK)
                    audioTrack.write(silenceBuffer, 0, toWrite)
                    remaining -= toWrite
                }
                beatIndex = (beatIndex + 1) % beatsPerMeasure
            }

            audioTrack.stop()
            audioTrack.release()
        }, "MetronomeThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning = false
        thread?.join(1000)
        thread = null
    }
}
