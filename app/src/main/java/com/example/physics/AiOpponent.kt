package com.example.physics

import com.example.model.Track
import com.example.model.Vehicle
import com.example.model.WeatherType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class AiRacer(
    val id: String,
    val name: String,
    val vehicle: Vehicle,
    val state: CarPhysicsState,
    val skill: Float = 1.0f, // 0.85 to 1.15
    var currentWaypointIdx: Int = 0
)

class AiOpponentManager {

    fun createOpponents(track: Track, playerVehicle: Vehicle, count: Int = 3): List<AiRacer> {
        val names = listOf("Apex AI - Thorne", "Veloce - Sato", "Ghost - Leclerc", "Turbo - Rossi", "Scuderia - Vance")
        val colors = listOf("#FF2A4B", "#FFD600", "#76FF03", "#9C27B0", "#FF6D00")
        val racers = mutableListOf<AiRacer>()

        val startWp = track.startWaypoint
        val headingRad = Math.toRadians(0.0) // initial heading along track

        for (i in 0 until count) {
            val name = names[i % names.size]
            val color = colors[i % colors.size]
            val aiVehicle = playerVehicle.copy(
                id = "ai_car_$i",
                name = "$name Spec",
                bodyColorHex = color
            )

            // Grid staggered starting positions behind player
            val gridOffset = (i + 1) * -45f
            val racer = AiRacer(
                id = "ai_$i",
                name = name,
                vehicle = aiVehicle,
                state = CarPhysicsState(
                    x = startWp.x + gridOffset,
                    y = startWp.y + (if (i % 2 == 0) -22f else 22f),
                    headingDeg = 0f,
                    currentGear = 1
                ),
                skill = 0.90f + Random.nextFloat() * 0.20f,
                currentWaypointIdx = 0
            )
            racers.add(racer)
        }
        return racers
    }

    fun updateAiRacers(
        racers: List<AiRacer>,
        track: Track,
        currentWeather: WeatherType,
        trackWetness: Float,
        playerState: CarPhysicsState,
        particleSystem: ParticleSystem,
        dt: Float
    ) {
        val wpCount = track.waypoints.size

        for (racer in racers) {
            val s = racer.state
            if (s.raceFinished) continue

            // 1. Target waypoint lookahead
            val lookAheadSteps = 4
            val targetIdx = (racer.currentWaypointIdx + lookAheadSteps) % wpCount
            val targetWp = track.waypoints[targetIdx]

            // 2. Heading to target waypoint
            val dx = targetWp.x - s.x
            val dy = targetWp.y - s.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 45f) {
                racer.currentWaypointIdx = (racer.currentWaypointIdx + 1) % wpCount
            }

            val targetHeadingRad = atan2(dy.toDouble(), dx.toDouble())
            var targetHeadingDeg = Math.toDegrees(targetHeadingRad).toFloat()
            while (targetHeadingDeg < 0f) targetHeadingDeg += 360f

            // Steering delta
            var steerDelta = targetHeadingDeg - s.headingDeg
            while (steerDelta > 180f) steerDelta -= 360f
            while (steerDelta < -180f) steerDelta += 360f

            val steerInput = (steerDelta / 45f).coerceIn(-1f, 1f)

            // Speed & Throttle/Brake control
            val currentSpeedKmh = s.speedKmh
            val targetSpeed = targetWp.targetSpeedKmh * racer.skill * (1f - trackWetness * 0.15f)

            var throttleInput = 0f
            var brakeInput = 0f
            if (currentSpeedKmh < targetSpeed) {
                throttleInput = 1.0f
            } else {
                brakeInput = ((currentSpeedKmh - targetSpeed) / 40f).coerceIn(0f, 1f)
            }

            // Player avoidance
            val distToPlayerSq = (s.x - playerState.x) * (s.x - playerState.x) + (s.y - playerState.y) * (s.y - playerState.y)
            if (distToPlayerSq < 50f * 50f) {
                // Steer slightly away
                val avoidDir = if (s.y > playerState.y) 0.35f else -0.35f
                steerDelta += avoidDir * 20f
            }

            // Simplified AI kinematics
            val forwardRad = Math.toRadians(s.headingDeg.toDouble())
            val targetForwardSpeedMs = (currentSpeedKmh + (throttleInput * 28f - brakeInput * 45f) * dt) / 3.6f
            val actualSpeedMs = targetForwardSpeedMs.coerceIn(0f, (racer.vehicle.effectiveTopSpeedKmh / 3.6f) * racer.skill)

            s.speedKmh = actualSpeedMs * 3.6f
            s.headingDeg += (steerInput * 95f * racer.skill * dt)
            while (s.headingDeg < 0f) s.headingDeg += 360f

            s.vx = cos(forwardRad).toFloat() * actualSpeedMs
            s.vy = sin(forwardRad).toFloat() * actualSpeedMs
            s.x += s.vx * dt
            s.y += s.vy * dt

            // Simulated RPM
            s.rpm = (1200f + (s.speedKmh / racer.vehicle.effectiveTopSpeedKmh) * 6000f).coerceIn(900f, 8500f)

            // Spray
            if (trackWetness > 0.3f && s.speedKmh > 60f && Random.nextFloat() < 0.3f) {
                particleSystem.spawnWaterSpray(s.x, s.y, s.headingDeg, intensity = trackWetness * 0.8f)
            }
        }
    }
}
