package com.example.physics

import com.example.model.DriveTrain
import com.example.model.SurfaceHazardType
import com.example.model.SurfacePatch
import com.example.model.TireCompound
import com.example.model.Track
import com.example.model.Vehicle
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
    var isDrifting: Boolean = false,
    var isAquaplaning: Boolean = false,
    var isBraking: Boolean = false,
    var currentSurfaceHazard: SurfaceHazardType? = null,
    var effectiveTireGrip: Float = 1.0f,
    var boostGauge: Float = 1.0f, // 0..1
    var isBoosting: Boolean = false,
    var currentLap: Int = 1,
    var lapProgressPct: Float = 0f,
    var currentLapTimeMs: Long = 0L,
    var lastLapTimeMs: Long = 0L,
    var bestLapTimeMs: Long = 0L,
    var driftScore: Int = 0,
    var raceFinished: Boolean = false
)

class PhysicsEngine {

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
        dt: Float
    ) {
        if (state.raceFinished) return

        val prevX = state.x
        val prevY = state.y

        // 1. Heading vectors
        val headingRad = Math.toRadians(state.headingDeg.toDouble())
        val forwardX = cos(headingRad).toFloat()
        val forwardY = sin(headingRad).toFloat()
        val rightX = -sin(headingRad).toFloat()
        val rightY = cos(headingRad).toFloat()

        // 2. Velocity decomposition in local car frame
        val vLong = state.vx * forwardX + state.vy * forwardY
        val vLat = state.vx * rightX + state.vy * rightY
        val speedMs = sqrt(state.vx * state.vx + state.vy * state.vy)
        state.speedKmh = speedMs * 3.6f

        // 3. Surface & Weather Condition Sampling
        val nearestWaypointIdx = track.findNearestWaypointIndex(state.x, state.y)
        val nearestWaypoint = track.waypoints[nearestWaypointIdx]
        val distToCenterSq = (state.x - nearestWaypoint.x) * (state.x - nearestWaypoint.x) +
                (state.y - nearestWaypoint.y) * (state.y - nearestWaypoint.y)
        val halfTrackWidth = nearestWaypoint.width * 0.5f
        val isOffTrack = distToCenterSq > (halfTrackWidth * halfTrackWidth)

        // Check hazard patches
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

        // Calculate weather tire grip
        val compound = vehicle.upgrades.tireCompound
        val weatherDryFactor = compound.dryGripMultiplier
        val weatherWetFactor = compound.wetGripMultiplier
        val weatherGrip = weatherDryFactor * (1f - trackWetness) + weatherWetFactor * trackWetness

        // Aquaplaning check: high speed through standing water on dry-oriented tires
        val isPuddle = activeHazard == SurfaceHazardType.PUDDLE
        val aquaplaneRisk = (state.speedKmh > 75f) && (trackWetness > 0.35f || isPuddle) &&
                (compound == TireCompound.SOFT_SLICK || compound == TireCompound.SEMI_SLICK)
        state.isAquaplaning = aquaplaneRisk

        // Hazard multiplier
        var hazardGripFactor = 1.0f
        if (activeHazard != null) {
            hazardGripFactor = activeHazard.gripFactor
            if (state.isAquaplaning) {
                hazardGripFactor = 0.20f
            }
        }

        // Combined tire grip
        val baseTireGrip = vehicle.effectiveGripMultiplier * weatherGrip * hazardGripFactor
        state.effectiveTireGrip = baseTireGrip

        // 4. Boost / Nitrous System
        var boostMultiplier = 1.0f
        if (boostInput && state.boostGauge > 0.05f) {
            state.isBoosting = true
            state.boostGauge = (state.boostGauge - 0.25f * dt).coerceAtLeast(0f)
            boostMultiplier = 1.45f
            particleSystem.spawnWaterSpray(state.x, state.y, state.headingDeg, intensity = 1.5f)
        } else {
            state.isBoosting = false
            state.boostGauge = (state.boostGauge + 0.08f * dt).coerceAtMost(1.0f)
        }

        // 5. Gear Transmission & RPM Simulation
        val effectiveTopSpeedMs = vehicle.effectiveTopSpeedKmh / 3.6f
        val gearIdx = (state.currentGear - 1).coerceIn(0, 5)
        val currentRatio = gearRatios[gearIdx]

        // Engine power to torque
        val maxPowerWatts = vehicle.effectiveBhp * 745.7f * boostMultiplier
        val maxTorqueNm = (maxPowerWatts / (vehicle.baseMaxRpm * 0.1047f)) * 1.35f

        // RPM calculation based on wheel speed
        val wheelRps = (vLong.coerceAtLeast(0f) / (2f * PI.toFloat() * wheelRadiusM))
        val targetEngineRpm = (wheelRps * currentRatio * finalDrive * 60f)
            .coerceIn(900f, vehicle.baseMaxRpm.toFloat() + 400f)

        // Smooth RPM interpolation with throttle revving
        if (throttleInput > 0.1f && vLong < 2f) {
            state.rpm += 4500f * throttleInput * dt
            if (state.rpm > vehicle.baseMaxRpm) state.rpm = vehicle.baseMaxRpm - 200f // Redline bounce
        } else {
            state.rpm += (targetEngineRpm - state.rpm) * (8f * dt)
        }
        state.rpm = state.rpm.coerceIn(850f, vehicle.baseMaxRpm.toFloat() + 300f)

        // Automatic gear shifting with shift delay logic
        if (state.rpm > (vehicle.baseMaxRpm * 0.92f) && state.currentGear < 6) {
            state.currentGear++
            state.rpm = vehicle.baseMaxRpm * 0.65f
        } else if (state.rpm < 2200f && state.currentGear > 1 && vLong > 2f) {
            state.currentGear--
            state.rpm = vehicle.baseMaxRpm * 0.72f
        }

        // 6. Driving and Braking Forces
        var drivingForce = 0f
        if (throttleInput > 0f) {
            val torque = maxTorqueNm * (0.6f + 0.4f * (state.rpm / vehicle.baseMaxRpm))
            val drivetrainFactor = when (vehicle.driveTrain) {
                DriveTrain.AWD -> 1.05f
                DriveTrain.RWD -> 1.0f
                DriveTrain.FWD -> 0.90f
            }
            drivingForce = (torque * currentRatio * finalDrive / wheelRadiusM) * throttleInput * drivetrainFactor
        }

        var brakingForce = 0f
        state.isBraking = brakeInput > 0.05f
        if (state.isBraking) {
            val baseBrakeForce = (vehicle.effectiveWeightKg * 9.8f) * 1.35f * vehicle.effectiveBrakingPower
            brakingForce = baseBrakeForce * brakeInput
        }

        // Reverse gear handling
        if (brakeInput > 0.3f && vLong <= 0.5f) {
            drivingForce = -maxTorqueNm * gearRatios[0] * 1.5f * brakeInput
            brakingForce = 0f
            state.currentGear = 1
        }

        // Aerodynamic Drag & Downforce
        val aeroDownforceFactor = vehicle.effectiveDownforceFactor
        val downforceN = 0.5f * 1.225f * (0.8f + aeroDownforceFactor) * (speedMs * speedMs)
        val dragForce = 0.5f * 1.225f * 0.34f * 2.1f * (vLong * abs(vLong))

        // Rolling resistance & off-track drag
        var rollingResistanceCoeff = 0.015f
        if (activeHazard == SurfaceHazardType.OFFROAD_GRASS) rollingResistanceCoeff = 0.22f
        if (activeHazard == SurfaceHazardType.GRAVEL_TRAP) rollingResistanceCoeff = 0.45f
        val rollingResistance = rollingResistanceCoeff * vehicle.effectiveWeightKg * 9.8f * (if (vLong > 0) 1f else -1f)

        // Net longitudinal force
        val netLongForce = drivingForce - (brakingForce * (if (vLong > 0) 1f else -1f)) - dragForce - rollingResistance
        val longAcc = netLongForce / vehicle.effectiveWeightKg

        // 7. Lateral Cornering & Drift Physics (Pacejka Curve)
        val slipAngleRad = if (abs(vLong) > 0.5f) atan2(vLat.toDouble(), abs(vLong).toDouble()).toFloat() else 0f
        state.slipAngleDeg = Math.toDegrees(slipAngleRad.toDouble()).toFloat()

        // Peak slip angle (~7 to 11 degrees)
        val peakSlipDeg = 8.5f * (1f / baseTireGrip.coerceAtLeast(0.4f))
        val normalizedSlip = (abs(state.slipAngleDeg) / peakSlipDeg)

        // Lateral adhesion limit
        val totalVerticalLoad = (vehicle.effectiveWeightKg * 9.8f) + downforceN
        val maxCorneringForce = totalVerticalLoad * baseTireGrip

        // Handbrake overrides rear lateral grip to provoke drift
        val handbrakeMultiplier = if (handbrakeInput) 0.25f else 1.0f
        val effectiveMaxCorneringForce = maxCorneringForce * handbrakeMultiplier

        // Cornering friction response
        val lateralFrictionForce = if (normalizedSlip < 1.0f) {
            // Linear adhesive region
            -vLat * (effectiveMaxCorneringForce / peakSlipDeg)
        } else {
            // Saturated sliding/drift region
            -(vLat / (abs(vLat).coerceAtLeast(0.1f))) * (effectiveMaxCorneringForce * (0.85f / normalizedSlip.coerceAtMost(3f)))
        }

        val latAcc = lateralFrictionForce / vehicle.effectiveWeightKg
        state.lateralG = abs(latAcc) / 9.81f

        // Drift state & score
        val isSliding = abs(state.slipAngleDeg) > 12f && speedMs > 8f
        state.isDrifting = isSliding
        if (state.isDrifting) {
            val driftAdd = (abs(state.slipAngleDeg) * (state.speedKmh / 50f) * dt * 8f).toInt()
            state.driftScore += driftAdd
            // Spawn tire smoke & skid marks
            particleSystem.spawnSmoke(state.x, state.y, state.headingDeg, intensity = (abs(state.slipAngleDeg) / 30f))
            particleSystem.addSkid(prevX, prevY, state.x, state.y, alpha = (abs(state.slipAngleDeg) / 45f))
        }

        // Wet weather spray
        if (trackWetness > 0.2f && speedMs > 10f) {
            particleSystem.spawnWaterSpray(state.x, state.y, state.headingDeg, intensity = trackWetness * (speedMs / 25f))
        }

        // 8. Steering & Yaw Dynamics
        val steerResponse = (vehicle.upgrades.suspensionStage * 0.1f + 1.0f)
        val speedSteerFactor = (1f / (1f + (speedMs / 28f))).coerceIn(0.28f, 1.0f)
        val targetAngularVel = steerInput * 165f * steerResponse * speedSteerFactor * (if (vLong < 0) -1f else 1f)

        // Drift counter-steering feel
        val oversteerBias = when (vehicle.driveTrain) {
            DriveTrain.RWD -> 1.35f
            DriveTrain.AWD -> 0.75f
            DriveTrain.FWD -> 0.40f
        }
        val driftYawInfluence = if (state.isDrifting) {
            -(state.slipAngleDeg * 1.8f * oversteerBias)
        } else 0f

        state.angularVelocity += (targetAngularVel + driftYawInfluence - state.angularVelocity) * (14f * dt)
        if (state.isAquaplaning) {
            // Aquaplaning: steering wheel loses connection with road!
            state.angularVelocity *= 0.2f
        }

        state.headingDeg += state.angularVelocity * dt
        while (state.headingDeg < 0f) state.headingDeg += 360f
        while (state.headingDeg >= 360f) state.headingDeg -= 360f

        // 9. Update World Velocity
        val newVLong = vLong + longAcc * dt
        val newVLat = vLat + latAcc * dt

        val newHeadingRad = Math.toRadians(state.headingDeg.toDouble())
        val newForwardX = cos(newHeadingRad).toFloat()
        val newForwardY = sin(newHeadingRad).toFloat()
        val newRightX = -sin(newHeadingRad).toFloat()
        val newRightY = cos(newRightRad(newHeadingRad)).toFloat() // Correct right vector

        state.vx = newVLong * newForwardX + newVLat * (-sin(newHeadingRad).toFloat())
        state.vy = newVLong * newForwardY + newVLat * (cos(newHeadingRad).toFloat())

        // 10. Position update
        state.x += state.vx * dt
        state.y += state.vy * dt

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
