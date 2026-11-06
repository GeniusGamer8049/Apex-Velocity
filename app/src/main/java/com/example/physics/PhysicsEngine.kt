package com.example.physics

import com.example.model.DriveTrain
import com.example.model.SurfaceHazardType
import com.example.model.SurfacePatch
import com.example.model.TireCompound
import com.example.model.Track
import com.example.model.Vehicle
import com.example.model.WeatherManager
import com.example.model.WeatherType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CarPhysicsState(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var headingDeg: Float = 0f,
    var angularVelocity: Float = 0f,
    var speedKmh: Float = 0f,
    var rpm: Float = 900f,
    var currentGear: Int = 1,
    var slipAngleDeg: Float = 0f,
    var lateralG: Float = 0f,
    var longitudinalG: Float = 0f,
    var downforceN: Float = 0f,
    var tireTempFL: Float = 82f,
    var tireTempFR: Float = 82f,
    var tireTempRL: Float = 80f,
    var tireTempRR: Float = 80f,
    var isDrifting: Boolean = false,
    var isAquaplaning: Boolean = false,
    var isBraking: Boolean = false,
    var currentSurfaceHazard: SurfaceHazardType? = null,
    var effectiveTireGrip: Float = 1.0f,
    var boostGauge: Float = 1.0f, // 0..1
    var isBoosting: Boolean = false,
    var lastCollisionIntensity: Float = 0f,
    var currentLap: Int = 1,
    var lapProgressPct: Float = 0f,
    var currentLapTimeMs: Long = 0L,
    var lastLapTimeMs: Long = 0L,
    var bestLapTimeMs: Long = 0L,
    var driftScore: Int = 0,
    var estimatedBrakingDistanceM: Float = 38.5f,
    var effectiveTopSpeedKmh: Float = 240f,
    var raceFinished: Boolean = false,
    // Real-time Physics Integration Module Variables
    var frictionDragForceN: Float = 0f,
    var frictionRollingResistanceN: Float = 0f,
    var frictionTotalOpposingN: Float = 0f,
    var tireSlipRatio: Float = 0f,
    var frontTireGripPct: Float = 1.0f,
    var rearTireGripPct: Float = 1.0f,
    var frictionCircleUtilizationPct: Float = 0f,
    var isWheelSpinning: Boolean = false,
    var isWheelLockup: Boolean = false,
    var normalLoadFrontN: Float = 0f,
    var normalLoadRearN: Float = 0f,
    var corneringForceN: Float = 0f,
    var tractionForceN: Float = 0f,
    var localVLong: Float = 0f,
    var localVLat: Float = 0f
)

class PhysicsEngine(
    val integrationModule: PhysicsIntegrationModule = PhysicsIntegrationModule()
) {

    val gearRatios = floatArrayOf(3.6f, 2.15f, 1.50f, 1.15f, 0.90f, 0.74f)
    val finalDrive = 3.9f
    val wheelRadiusM = 0.33f

    fun updatePlayer(
        state: CarPhysicsState,
        vehicle: Vehicle,
        track: Track,
        currentWeather: WeatherType,
        trackWetness: Float,
        throttleInput: Float,   // 0..1
        brakeInput: Float,      // 0..1
        steerInput: Float,      // -1..1
        handbrakeInput: Boolean,
        boostInput: Boolean,
        particleSystem: ParticleSystem,
        dt: Float,
        onCollision: ((intensity: Float) -> Unit)? = null,
        weatherAdjustment: AdjustedVehiclePhysics? = null
    ) {
        if (state.raceFinished) return

        val prevX = state.x
        val prevY = state.y

        // Execute real-time vehicle velocity, friction, and tire grip integration step
        val result = integrationModule.integrate(
            state = state,
            vehicle = vehicle,
            track = track,
            currentWeather = currentWeather,
            trackWetness = trackWetness,
            throttleInput = throttleInput,
            brakeInput = brakeInput,
            steerInput = steerInput,
            handbrakeInput = handbrakeInput,
            boostInput = boostInput,
            dt = dt,
            weatherAdjustment = weatherAdjustment
        )

        // 1. Update Real-Time Velocity Variables
        state.vx = result.velocity.vx
        state.vy = result.velocity.vy
        state.speedKmh = result.velocity.speedKmh
        state.headingDeg = result.velocity.headingDeg
        state.angularVelocity = result.velocity.angularVelocityDegPerSec
        state.longitudinalG = result.velocity.longitudinalG
        state.lateralG = result.velocity.lateralG
        state.localVLong = result.velocity.vLong
        state.localVLat = result.velocity.vLat

        // 2. Update Real-Time Friction Variables
        state.frictionDragForceN = result.friction.aerodynamicDragForceN
        state.frictionRollingResistanceN = result.friction.rollingResistanceForceN
        state.frictionTotalOpposingN = result.friction.totalOpposingFrictionN

        // 3. Update Real-Time Tire Grip & Adhesion Variables
        state.effectiveTireGrip = result.tireGrip.effectiveTireGripPct
        state.slipAngleDeg = result.tireGrip.slipAngleDeg
        state.tireSlipRatio = result.tireGrip.slipRatio
        state.frontTireGripPct = result.tireGrip.frontAxleGripPct
        state.rearTireGripPct = result.tireGrip.rearAxleGripPct
        state.frictionCircleUtilizationPct = result.tireGrip.frictionCircleUtilizationPct
        state.isWheelSpinning = result.tireGrip.isWheelSpinning
        state.isWheelLockup = result.tireGrip.isWheelLockup
        state.normalLoadFrontN = result.tireGrip.normalLoadFrontN
        state.normalLoadRearN = result.tireGrip.normalLoadRearN
        state.corneringForceN = result.tireGrip.corneringForceN
        state.tractionForceN = result.tireGrip.tractionForceN
        state.downforceN = result.tireGrip.totalNormalLoadN - (vehicle.effectiveWeightKg * 9.81f)
        state.isDrifting = result.tireGrip.isDrifting
        state.isAquaplaning = result.tireGrip.isAquaplaning
        state.isBraking = brakeInput > 0.05f

        // 4. Update Engine & Boost Transmission
        state.rpm = result.newRpm
        state.currentGear = result.newGear
        state.boostGauge = result.boostGauge
        state.isBoosting = result.isBoosting
        state.x = result.newX
        state.y = result.newY

        // 5. Environmental Surface & Hazard
        val nearestWaypointIdx = track.findNearestWaypointIndex(state.x, state.y)
        val nearestWaypoint = track.waypoints[nearestWaypointIdx]
        val distToCenterSq = (state.x - nearestWaypoint.x) * (state.x - nearestWaypoint.x) +
                (state.y - nearestWaypoint.y) * (state.y - nearestWaypoint.y)
        val halfTrackWidth = nearestWaypoint.width * 0.5f
        val isOffTrack = distToCenterSq > (halfTrackWidth * halfTrackWidth)

        var activeHazard: SurfaceHazardType? = null
        for (patch in track.surfacePatches) {
            if (patch.contains(state.x, state.y)) {
                activeHazard = patch.type
                break
            }
        }
        if (isOffTrack && activeHazard == null) {
            activeHazard = SurfaceHazardType.OFFROAD_GRASS
        } else if (distToCenterSq > (halfTrackWidth * 0.75f * halfTrackWidth * 0.75f) && nearestWaypoint.isApexKerb) {
            activeHazard = SurfaceHazardType.KERB_RUMBLE
        }
        state.currentSurfaceHazard = activeHazard

        // Estimated Braking Distance & Dynamic Top Speed
        val speedMs = result.velocity.speedMs
        val weatherHandling = WeatherManager.getHandlingParameters(currentWeather)
        val brakeMult = weatherAdjustment?.brakingEfficiencyMultiplier ?: weatherHandling.brakingEfficiencyMultiplier
        state.effectiveTopSpeedKmh = weatherAdjustment?.effectiveTopSpeedKmh ?: vehicle.effectiveTopSpeedKmh.toFloat()
        state.estimatedBrakingDistanceM = weatherAdjustment?.brakingDistanceAtCurrentSpeed
            ?: if (speedMs > 0.5f) (speedMs * speedMs) / (2f * 9.81f * 1.35f * vehicle.effectiveBrakingPower * brakeMult).coerceAtLeast(0.5f) else 0f

        // Boost particles
        if (state.isBoosting) {
            particleSystem.spawnWaterSpray(state.x, state.y, state.headingDeg, intensity = 1.5f)
        }

        // Tire Thermal Dynamics (FL, FR, RL, RR)
        val ambientTemp = if (trackWetness > 0.5f) 65f else 78f
        val coolingRate = if (trackWetness > 0.3f) weatherHandling.tireCoolingRate else 1.2f
        val frontBrakeHeating = if (state.isBraking) brakeInput * 24f else 0f
        val lateralHeatingFL = if (state.localVLat < 0) abs(state.lateralG) * 16f else abs(state.lateralG) * 7f
        val lateralHeatingFR = if (state.localVLat > 0) abs(state.lateralG) * 16f else abs(state.lateralG) * 7f
        val rearPowerHeating = if (throttleInput > 0.4f) throttleInput * 14f else 0f
        val driftHeating = if (state.isDrifting) (abs(state.slipAngleDeg) / 10f) * 25f else 0f

        state.tireTempFL += (frontBrakeHeating + lateralHeatingFL - (state.tireTempFL - ambientTemp) * coolingRate) * dt
        state.tireTempFR += (frontBrakeHeating + lateralHeatingFR - (state.tireTempFR - ambientTemp) * coolingRate) * dt
        state.tireTempRL += (rearPowerHeating + driftHeating + (if (state.localVLat < 0) abs(state.lateralG) * 10f else 4f) - (state.tireTempRL - ambientTemp) * coolingRate) * dt
        state.tireTempRR += (rearPowerHeating + driftHeating + (if (state.localVLat > 0) abs(state.lateralG) * 10f else 4f) - (state.tireTempRR - ambientTemp) * coolingRate) * dt

        state.tireTempFL = state.tireTempFL.coerceIn(45f, 145f)
        state.tireTempFR = state.tireTempFR.coerceIn(45f, 145f)
        state.tireTempRL = state.tireTempRL.coerceIn(45f, 145f)
        state.tireTempRR = state.tireTempRR.coerceIn(45f, 145f)

        // Drift state & scoring
        if (state.isDrifting) {
            val driftAdd = (abs(state.slipAngleDeg) * (state.speedKmh / 50f) * dt * 8f).toInt()
            state.driftScore += driftAdd
            particleSystem.spawnSmoke(state.x, state.y, state.headingDeg, intensity = (abs(state.slipAngleDeg) / 30f))
            particleSystem.addSkid(prevX, prevY, state.x, state.y, alpha = (abs(state.slipAngleDeg) / 45f))
        }

        // Wet weather spray
        if (trackWetness > 0.2f && speedMs > 10f) {
            particleSystem.spawnWaterSpray(state.x, state.y, state.headingDeg, intensity = trackWetness * (speedMs / 25f))
        }

        // 11. Barrier & Track Limits Collision
        // If car goes too far off-track beyond gravel run-off, softly deflect back
        val maxAllowableOfftrack = halfTrackWidth * 2.2f
        if (distToCenterSq > (maxAllowableOfftrack * maxAllowableOfftrack)) {
            // Wall impact! Bounce and scrub speed
            val dist = sqrt(distToCenterSq)
            val nx = (nearestWaypoint.x - state.x) / dist
            val ny = (nearestWaypoint.y - state.y) / dist
            state.x = nearestWaypoint.x - nx * maxAllowableOfftrack
            state.y = nearestWaypoint.y - ny * maxAllowableOfftrack
            val impactSpeed = speedMs
            val impactIntensity = (impactSpeed / 25f).coerceIn(0.25f, 1.0f)
            state.lastCollisionIntensity = impactIntensity
            onCollision?.invoke(impactIntensity)
            state.vx *= -0.35f
            state.vy *= -0.35f
            particleSystem.spawnSparks(state.x, state.y)
        }

        // 12. Lap Timing & Checkpoints
        updateLapTiming(state, track, dt)
    }

    private fun newRightRad(headingRad: Double): Double = headingRad + PI / 2

    private fun updateLapTiming(state: CarPhysicsState, track: Track, dt: Float) {
        state.currentLapTimeMs += (dt * 1000f).toLong()

        val nearestIdx = track.findNearestWaypointIndex(state.x, state.y)
        state.lapProgressPct = (nearestIdx.toFloat() / track.waypoints.size.toFloat()).coerceIn(0f, 1f)

        val wp = track.waypoints[nearestIdx]
        if (wp.isStartFinish && nearestIdx < 5 && state.lapProgressPct < 0.15f && state.currentLapTimeMs > 15_000L) {
            // Completed a lap!
            state.lastLapTimeMs = state.currentLapTimeMs
            if (state.bestLapTimeMs == 0L || state.lastLapTimeMs < state.bestLapTimeMs) {
                state.bestLapTimeMs = state.lastLapTimeMs
            }
            state.currentLap++
            state.currentLapTimeMs = 0L
        }
    }
}
