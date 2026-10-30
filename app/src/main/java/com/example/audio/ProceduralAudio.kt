package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class ProceduralAudio {

    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    @Volatile
    var targetRpm: Float = 1000f

    @Volatile
    var isDrifting: Boolean = false

    @Volatile
    var isRaining: Boolean = false

    @Volatile
    var isMuted: Boolean = false

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)

        val sampleRate = 22050
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(1024)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            // AudioTrack unavailable or muted
            return
        }

        audioThread = Thread {
            val buffer = ShortArray(512)
            var phase = 0.0
            var driftPhase = 0.0
            var currentSmoothedRpm = 1000f

            while (isRunning.get()) {
                if (isMuted || audioTrack == null) {
                    try {
                        Thread.sleep(50)
                    } catch (ignored: InterruptedException) {
                        break
                    }
                    continue
                }

                // Smooth RPM towards target
                currentSmoothedRpm += (targetRpm - currentSmoothedRpm) * 0.15f

                // Base engine fundamental frequency: 4-cylinder / 8-cylinder engine pulses
                val fundamentalFreq = (currentSmoothedRpm / 60f) * 2.5 // Hz
                val phaseIncrement = (2.0 * PI * fundamentalFreq) / sampleRate

                val driftActive = isDrifting
                val rainActive = isRaining

                for (i in buffer.indices) {
                    phase += phaseIncrement
                    if (phase > 2.0 * PI) phase -= 2.0 * PI

                    // Harmonic synthesis: Fundamental + 2nd harmonic + 3rd harmonic
                    val wave1 = sin(phase)
                    val wave2 = sin(phase * 2.0) * 0.45
                    val wave3 = sin(phase * 3.0) * 0.25
                    val wave4 = (if (sin(phase * 0.5) > 0) 0.2 else -0.2) // Sub-octave throb
                    var sampleVal = (wave1 + wave2 + wave3 + wave4) * 0.35

                    // Tire screech noise
                    if (driftActive) {
                        val screechNoise = (Random.nextDouble() * 2.0 - 1.0) * 0.35
                        sampleVal += screechNoise
                    }

                    // Rain ambient patter
                    if (rainActive) {
                        val rainNoise = (Random.nextDouble() * 2.0 - 1.0) * 0.08
                        sampleVal += rainNoise
                    }

                    // Convert to 16-bit PCM
                    val pcm = (sampleVal.coerceIn(-1.0, 1.0) * 16000.0).toInt().toShort()
                    buffer[i] = pcm
                }

                audioTrack?.write(buffer, 0, buffer.size)
            }
        }.apply {
            priority = Thread.MIN_PRIORITY
            isDaemon = true
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            audioThread?.interrupt()
            audioThread = null
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (ignored: Exception) {}
    }
}
