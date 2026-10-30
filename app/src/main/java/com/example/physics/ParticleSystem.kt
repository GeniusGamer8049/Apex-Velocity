package com.example.physics

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float,
    var color: Long, // ARGB long
    var maxLife: Float,
    var life: Float = 0f
) {
    val isDead: Boolean get() = life >= maxLife
}

data class SkidMark(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val alpha: Float,
    val width: Float = 6f
)

class ParticleSystem {
    val particles = mutableListOf<Particle>()
    val skidMarks = mutableListOf<SkidMark>()
    private val maxSkidMarks = 400

    fun update(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life += dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vx *= 0.94f
            p.vy *= 0.94f
            p.alpha = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
            p.size += 4f * dt
            if (p.isDead) {
                iter.remove()
            }
        }
    }

    fun spawnSmoke(x: Float, y: Float, headingDeg: Float, intensity: Float = 1f) {
        val count = (Random.nextInt(1, 3) * intensity).toInt().coerceAtMost(4)
        val rad = Math.toRadians(headingDeg.toDouble() + 180.0)
        for (i in 0 until count) {
            val spread = (Random.nextFloat() - 0.5f) * 0.8f
            val speed = Random.nextFloat() * 30f + 10f
            val vx = (cos(rad + spread) * speed).toFloat()
            val vy = (sin(rad + spread) * speed).toFloat()
            particles.add(
                Particle(
                    x = x + (Random.nextFloat() - 0.5f) * 12f,
                    y = y + (Random.nextFloat() - 0.5f) * 12f,
                    vx = vx,
                    vy = vy,
                    size = Random.nextFloat() * 8f + 6f,
                    alpha = 0.5f * intensity.coerceAtMost(1f),
                    color = 0x88DDDDDD,
                    maxLife = Random.nextFloat() * 0.5f + 0.3f
                )
            )
        }
    }

    fun spawnWaterSpray(x: Float, y: Float, headingDeg: Float, intensity: Float = 1f) {
        val count = (2 * intensity).toInt().coerceAtMost(5)
        val rad = Math.toRadians(headingDeg.toDouble() + 180.0)
        for (i in 0 until count) {
            val spread = (Random.nextFloat() - 0.5f) * 0.9f
            val speed = Random.nextFloat() * 45f + 15f
            val vx = (cos(rad + spread) * speed).toFloat()
            val vy = (sin(rad + spread) * speed).toFloat()
            particles.add(
                Particle(
                    x = x + (Random.nextFloat() - 0.5f) * 10f,
                    y = y + (Random.nextFloat() - 0.5f) * 10f,
                    vx = vx,
                    vy = vy,
                    size = Random.nextFloat() * 6f + 4f,
                    alpha = 0.6f * intensity.coerceAtMost(1f),
                    color = 0xAA64B5F6,
                    maxLife = Random.nextFloat() * 0.4f + 0.2f
                )
            )
        }
    }

    fun spawnSparks(x: Float, y: Float) {
        for (i in 0 until 8) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 120f + 40f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    size = Random.nextFloat() * 4f + 2f,
                    alpha = 1.0f,
                    color = 0xFFFFD700,
                    maxLife = Random.nextFloat() * 0.25f + 0.1f
                )
            )
        }
    }

    fun addSkid(x1: Float, y1: Float, x2: Float, y2: Float, alpha: Float) {
        if (skidMarks.size >= maxSkidMarks) {
            skidMarks.removeAt(0)
        }
        skidMarks.add(SkidMark(x1, y1, x2, y2, alpha.coerceIn(0.1f, 0.7f)))
    }

    fun clear() {
        particles.clear()
        skidMarks.clear()
    }
}
