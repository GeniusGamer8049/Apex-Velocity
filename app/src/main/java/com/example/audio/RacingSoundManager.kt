package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * RacingSoundManager handles all dynamic real-time in-game audio synthesis:
 * - Multi-harmonic engine revs responsive to RPM, gear, throttle load, and redline limiter bounce.
 * - Dynamic tire screeching triggered by lateral G-force, high slip angles, drifting, or heavy braking lockup.
 * - Collision sound effects for barrier and vehicle impacts with velocity-proportional intensity.
 * - Kerb rumble strip vibrations, nitrous boost surge, gear shift exhaust pops, and rain ambience.
 * - Zero external file dependencies: 100% self-contained procedural PCM synthesis.
 */
class RacingSoundManager {

    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private val isRunning = AtomicBoolean(false)
    private val isAudioActive = AtomicBoolean(true)

    // Master & Channel volume controls (0.0f .. 1.0f)
    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var masterVolume: Float = 1.0f

    @Volatile
    var engineVolume: Float = 0.90f

    @Volatile
    var screechVolume: Float = 0.85f

    @Volatile
    var collisionVolume: Float = 1.0f

    @Volatile
    var effectsVolume: Float = 0.75f

    // Live Telemetry Inputs
    @Volatile
    var targetRpm: Float = 900f

    @Volatile
    var maxRpm: Float = 8500f

    @Volatile
    var currentGear: Int = 1

    @Volatile
    var throttleInput: Float = 0f

    @Volatile
    var speedKmh: Float = 0f

    @Volatile
    var isDrifting: Boolean = false

    @Volatile
    var slipAngleDeg: Float = 0f

    @Volatile
    var lateralG: Float = 0f

    @Volatile
    var isBraking: Boolean = false

    @Volatile
    var isBoosting: Boolean = false

    @Volatile
    var isKerbRumble: Boolean = false

    @Volatile
    var isRaining: Boolean = false

    // One-shot SFX counters (measured in sample counts)
    private val collisionRemainingSamples = AtomicInteger(0)
    private val collisionTotalSamples = AtomicInteger(1)
    @Volatile
    private var collisionIntensity = 0f

    private val shiftPopRemainingSamples = AtomicInteger(0)
    private val shiftPopTotalSamples = AtomicInteger(1)

    private val sampleRate = 22050

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)
        isAudioActive.set(true)

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
            // AudioTrack unavailable on host/container
            return
        }

        audioThread = Thread {
            val buffer = ShortArray(512)
            var enginePhase = 0.0
            var smoothedRpm = 900f
            var redlineStutterCounter = 0
            var redlineCut = false

            // Resonant filter state for tire screech
            var bpFilterPrev1 = 0.0
            var bpFilterPrev2 = 0.0

            while (isRunning.get()) {
                if (isMuted || !isAudioActive.get() || audioTrack == null) {
                    try {
                        Thread.sleep(40)
                    } catch (ignored: InterruptedException) {
                        break
                    }
                    continue
                }

                // Smooth RPM towards target with throttle bite
                val rpmTarget = targetRpm.coerceIn(800f, maxRpm + 500f)
                val rpmSmoothingRate = if (throttleInput > 0.5f) 0.22f else 0.12f
                smoothedRpm += (rpmTarget - smoothedRpm) * rpmSmoothingRate

                // Redline Limiter Stutter (spark-cut bounce)
                if (smoothedRpm >= maxRpm * 0.97f) {
                    redlineStutterCounter++
                    if (redlineStutterCounter > 3) {
                        redlineCut = !redlineCut
                        redlineStutterCounter = 0
                    }
                } else {
                    redlineCut = false
                    redlineStutterCounter = 0
                }

                // Engine fundamental frequency: 4-stroke 6-cylinder pulse
                val fundamentalFreq = (smoothedRpm / 60.0) * 3.0 // Hz
                val enginePhaseInc = (2.0 * PI * fundamentalFreq) / sampleRate

                // Calculate tire skid intensity
                val driftFactor = if (isDrifting) (abs(slipAngleDeg) / 25f).coerceIn(0.15f, 1.0f) else 0f
                val latGFactor = if (lateralG > 0.82f) ((lateralG - 0.82f) / 0.55f).coerceIn(0f, 1.0f) else 0f
                val brakeLockFactor = if (isBraking && speedKmh > 20f && throttleInput < 0.2f) 0.55f else 0f
                val combinedScreechIntensity = maxOf(driftFactor, latGFactor, brakeLockFactor) * screechVolume

                // Active effects flags
                val activeBoosting = isBoosting
                val activeKerb = isKerbRumble
                val activeRain = isRaining
                val masterVol = masterVolume.coerceIn(0f, 1f)

                // Populate PCM audio buffer
                for (i in buffer.indices) {
                    enginePhase += enginePhaseInc
                    if (enginePhase > 2.0 * PI) enginePhase -= 2.0 * PI

                    // 1. Multi-Harmonic Engine Synthesis
                    val engineSample = if (redlineCut) {
                        0.0
                    } else {
                        val wave1 = sin(enginePhase)
                        val wave2 = sin(enginePhase * 2.0) * 0.45
                        val wave3 = sin(enginePhase * 3.0) * 0.28
                        val wave4 = sin(enginePhase * 4.0) * 0.14
                        val subBass = (if (sin(enginePhase * 0.5) > 0.0) 0.22 else -0.22)

                        // Throttle load saturation
                        val rawTone = (wave1 + wave2 + wave3 + wave4 + subBass) * (0.28 + throttleInput * 0.18)
                        // Soft saturation curve: x / (1 + |x|)
                        (rawTone / (1.0 + abs(rawTone))) * engineVolume
                    }

                    // 2. Dynamic Tire Screech (Bandpass Filtered White Noise)
                    var screechSample = 0.0
                    if (combinedScreechIntensity > 0.02f) {
                        val rawNoise = Random.nextDouble() * 2.0 - 1.0
                        // 2-pole resonant bandpass filter centered ~1500Hz
                        val filteredNoise = 0.25 * rawNoise + 1.25 * bpFilterPrev1 - 0.65 * bpFilterPrev2
                        bpFilterPrev2 = bpFilterPrev1
                        bpFilterPrev1 = filteredNoise.coerceIn(-1.5, 1.5)

                        screechSample = filteredNoise * combinedScreechIntensity * 0.42
                    }

                    // 3. Collision Impact Punch & Shatter
                    var collisionSample = 0.0
                    val remCollision = collisionRemainingSamples.get()
                    if (remCollision > 0) {
                        val totalCollision = collisionTotalSamples.get().coerceAtLeast(1)
                        val progress = 1.0 - (remCollision.toDouble() / totalCollision.toDouble())
                        collisionRemainingSamples.decrementAndGet()

                        // Sub-bass heavy transient punch + metal resonance
                        val decay = exp(-progress * 9.0)
                        val subPunch = sin(progress * 2.0 * PI * 65.0) * decay
                        val crunchNoise = (Random.nextDouble() * 2.0 - 1.0) * exp(-progress * 14.0)
                        val metallicRes = sin(progress * 2.0 * PI * 820.0) * exp(-progress * 7.0)

                        collisionSample = (subPunch * 0.75 + crunchNoise * 0.55 + metallicRes * 0.35) *
                                collisionIntensity * collisionVolume
                    }

                    // 4. Gear Shift Exhaust Pop / Turbo Blow-off
                    var shiftPopSample = 0.0
                    val remShift = shiftPopRemainingSamples.get()
                    if (remShift > 0) {
                        val totalShift = shiftPopTotalSamples.get().coerceAtLeast(1)
                        val shiftProgress = 1.0 - (remShift.toDouble() / totalShift.toDouble())
                        shiftPopRemainingSamples.decrementAndGet()

                        val popDecay = exp(-shiftProgress * 12.0)
                        val pop = (Random.nextDouble() * 2.0 - 1.0) * popDecay * 0.40
                        val whoosh = sin(shiftProgress * 2.0 * PI * 350.0) * exp(-shiftProgress * 6.0) * 0.25
                        shiftPopSample = (pop + whoosh) * effectsVolume
                    }

                    // 5. Kerb Rumble Strip Tone
                    var kerbRumbleSample = 0.0
                    if (activeKerb && speedKmh > 15f) {
                        val kerbFreq = 85.0 + (speedKmh * 0.4)
                        val kerbPhase = (i.toDouble() * 2.0 * PI * kerbFreq) / sampleRate
                        kerbRumbleSample = (if (sin(kerbPhase) > 0.0) 0.22 else -0.22) * effectsVolume
                    }

                    // 6. Nitrous / Turbo Boost Hiss
                    var boostSample = 0.0
                    if (activeBoosting) {
                        val boostNoise = (Random.nextDouble() * 2.0 - 1.0) * 0.16
                        boostSample = boostNoise * effectsVolume
                    }

                    // 7. Rain Ambience
                    var rainSample = 0.0
                    if (activeRain) {
                        val droplet = (Random.nextDouble() * 2.0 - 1.0) * 0.07
                        rainSample = droplet * effectsVolume
                    }

                    // Sum all audio layers with master attenuation
                    val finalSample = (engineSample + screechSample + collisionSample +
                            shiftPopSample + kerbRumbleSample + boostSample + rainSample) * masterVol

                    // Convert to 16-bit PCM short
                    val pcmShort = (finalSample.coerceIn(-1.0, 1.0) * 16384.0).toInt().toShort()
                    buffer[i] = pcmShort
                }

                audioTrack?.write(buffer, 0, buffer.size)
            }
        }.apply {
            priority = Thread.MIN_PRIORITY
            isDaemon = true
            name = "ApexAudioEngine"
            start()
        }
    }

    /**
     * Trigger a collision impact sound effect with intensity proportional to impact velocity.
     * @param intensity 0.0f (soft scrape) to 1.0f (high-speed barrier smash).
     */
    fun playCollision(intensity: Float = 1.0f) {
        val clampedIntensity = intensity.coerceIn(0.15f, 1.0f)
        collisionIntensity = clampedIntensity
        val durationSamples = (sampleRate * 0.32).toInt() // ~320ms duration
        collisionTotalSamples.set(durationSamples)
        collisionRemainingSamples.set(durationSamples)
    }

    /**
     * Trigger a gear change exhaust pop and transmission whoosh.
     */
    fun playGearShift() {
        val durationSamples = (sampleRate * 0.18).toInt() // ~180ms
        shiftPopTotalSamples.set(durationSamples)
        shiftPopRemainingSamples.set(durationSamples)
    }

    /**
     * Update real-time racing physics telemetry into sound generator.
     */
    fun updateTelemetry(
        rpm: Float,
        maxRpm: Float = 8500f,
        currentGear: Int = 1,
        throttle: Float = 0f,
        speedKmh: Float = 0f,
        isDrifting: Boolean = false,
        slipAngleDeg: Float = 0f,
        lateralG: Float = 0f,
        isBraking: Boolean = false,
        isBoosting: Boolean = false,
        isKerbRumble: Boolean = false,
        isRaining: Boolean = false
    ) {
        if (currentGear != this.currentGear) {
            playGearShift()
        }
        this.targetRpm = rpm
        this.maxRpm = maxRpm
        this.currentGear = currentGear
        this.throttleInput = throttle
        this.speedKmh = speedKmh
        this.isDrifting = isDrifting
        this.slipAngleDeg = slipAngleDeg
        this.lateralG = lateralG
        this.isBraking = isBraking
        this.isBoosting = isBoosting
        this.isKerbRumble = isKerbRumble
        this.isRaining = isRaining
    }

    fun pause() {
        isAudioActive.set(false)
    }

    fun resume() {
        isAudioActive.set(true)
    }

    fun stop() {
        isRunning.set(false)
        isAudioActive.set(false)
        try {
            audioThread?.interrupt()
            audioThread = null
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (ignored: Exception) {}
    }

    fun release() {
        stop()
    }
}
