package com.example.audio

/**
 * Legacy wrapper delegating directly to RacingSoundManager.
 */
class ProceduralAudio {

    val soundManager = RacingSoundManager()

    var targetRpm: Float
        get() = soundManager.targetRpm
        set(value) { soundManager.targetRpm = value }

    var isDrifting: Boolean
        get() = soundManager.isDrifting
        set(value) { soundManager.isDrifting = value }

    var isRaining: Boolean
        get() = soundManager.isRaining
        set(value) { soundManager.isRaining = value }

    var isMuted: Boolean
        get() = soundManager.isMuted
        set(value) { soundManager.isMuted = value }

    fun start() = soundManager.start()

    fun stop() = soundManager.stop()

    fun playCollision(intensity: Float = 1.0f) = soundManager.playCollision(intensity)

    fun playGearShift() = soundManager.playGearShift()
}
