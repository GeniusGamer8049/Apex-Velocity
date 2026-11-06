package com.example.physics

import com.example.model.DriveTrain
import com.example.model.SurfaceHazardType
import com.example.model.TireCompound
import com.example.model.Track
import com.example.model.Vehicle
import com.example.model.WeatherManager
import com.example.model.WeatherType
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Numerical integration method for vehicle velocity and position.
 */
enum class IntegrationMethod {
    SEMI_IMPLICIT_EULER,
    RUNGE_KUTTA_2 // Midpoint integration for enhanced numerical stability
}

/**
 * Real-time vehicle velocity state in both global Cartesian coordinates
 * and local vehicle-relative reference frame.
 */
data class VehicleVelocityState(
    val vx: Float = 0f,                     // World linear velocity X (m/s)
    val vy: Float = 0f,                     // World linear velocity Y (m/s)
    val speedMs: Float = 0f,                // Absolute speed magnitude (m/s)
    val speedKmh: Float = 0f,               // Absolute speed in km/h
    val vLong: Float = 0f,                  // Local longitudinal velocity along car heading (m/s)
    val vLat: Float = 0f,                   // Local lateral velocity perpendicular to car heading (m/s)
    val headingDeg: Float = 0f,             // Car orientation in degrees [0, 360)
    val headingRad: Double = 0.0,           // Car orientation in radians
    val angularVelocityDegPerSec: Float = 0f, // Yaw rate (degrees per second)
    val longitudinalAcc: Float = 0f,        // Forward acceleration (m/s^2)
    val lateralAcc: Float = 0f,             // Centripetal acceleration (m/s^2)
    val longitudinalG: Float = 0f,          // Longitudinal G-force (normalized to 9.81 m/s^2)
    val lateralG: Float = 0f                // Lateral G-force (normalized to 9.81 m/s^2)
) {
    val formattedSpeed: String
        get() = "${speedKmh.toInt()} km/h"

    val formattedLocalVelocities: String
        get() = String.format(Locale.US, "Long: %.1f m/s | Lat: %.1f m/s", vLong, vLat)
}

/**
 * Real-time opposing friction forces acting on the vehicle chassis and tires.
 */
data class FrictionForcesState(
    val aerodynamicDragForceN: Float = 0f,      // F_drag = 0.5 * rho * Cd * A * v^2
    val rollingResistanceForceN: Float = 0f,    // F_rr = Crr * N * sgn(v)
    val engineBrakingForceN: Float = 0f,        // Mechanical drivetrain compression drag off-throttle
    val coulombFrictionForceN: Float = 0f,      // Zero-velocity static/kinetic threshold friction
    val totalOpposingFrictionN: Float = 0f,     // Sum of all resistive forces (N)
    val rollingResistanceCoefficient: Float = 0.015f, // Surface-dependent Crr
    val aerodynamicDragCoefficientProduct: Float = 0.35f // 0.5 * rho * Cd * A
) {
    val formattedTotalFriction: String
        get() = String.format(Locale.US, "%.0f N", totalOpposingFrictionN)

    val formattedDragVsRolling: String
        get() = String.format(Locale.US, "Aero: %.0f N | Rolling: %.0f N", aerodynamicDragForceN, rollingResistanceForceN)
}

/**
 * Real-time tire grip and contact-patch telemetry variables.
 */
data class TireGripState(
    val normalLoadFrontN: Float = 0f,           // Dynamic vertical normal load on front axle (N)
    val normalLoadRearN: Float = 0f,            // Dynamic vertical normal load on rear axle (N)
    val totalNormalLoadN: Float = 0f,           // Total normal load including downforce (N)
    val slipRatio: Float = 0f,                  // Longitudinal slip ratio kappa (-1.0 to +1.0+)
    val slipAngleDeg: Float = 0f,               // Lateral tire slip angle alpha in degrees
    val peakGripCoeffLongitudinal: Float = 1.0f,// Peak longitudinal friction coefficient mu_x
    val peakGripCoeffLateral: Float = 1.0f,     // Peak lateral friction coefficient mu_y
    val compoundGripMultiplier: Float = 1.0f,   // Tire compound base grip (dry/wet curve)
    val thermalGripMultiplier: Float = 1.0f,    // Thermal efficiency multiplier (optimal 80-95°C)
    val surfaceGripMultiplier: Float = 1.0f,    // Track surface / hazard / wetness grip factor
    val effectiveTireGripPct: Float = 1.0f,     // Composite grip percentage (0.0 .. 1.0+)
    val frontAxleGripPct: Float = 1.0f,         // Front tire grip availability (0..1)
    val rearAxleGripPct: Float = 1.0f,          // Rear tire grip availability (0..1)
    val tractionForceN: Float = 0f,             // Longitudinal tire driving force (N)
    val brakingTireForceN: Float = 0f,          // Longitudinal tire braking force (N)
    val corneringForceN: Float = 0f,            // Lateral tire cornering force (N)
    val frictionCircleUtilizationPct: Float = 0f,// Combined slip circle usage sqrt((Fx/FxMax)^2 + (Fy/FyMax)^2) * 100
    val isDrifting: Boolean = false,            // Lateral slip angle exceeds threshold
    val isWheelSpinning: Boolean = false,       // High positive slip ratio under acceleration
    val isWheelLockup: Boolean = false,         // High negative slip ratio under braking
    val isAquaplaning: Boolean = false          // Hydrodynamic tire separation
) {
    val formattedGripPct: String
        get() = "${(effectiveTireGripPct * 100).toInt()}%"

    val formattedFrictionCircle: String
        get() = "${frictionCircleUtilizationPct.toInt()}%"
}

/**
 * Complete integration result produced on each physics tick.
 */
data class IntegrationResult(
    val velocity: VehicleVelocityState,
    val friction: FrictionForcesState,
    val tireGrip: TireGripState,
    val newX: Float,
    val newY: Float,
    val newHeadingDeg: Float,
    val newAngularVelocity: Float,
    val newRpm: Float,
    val newGear: Int,
    val boostGauge: Float,
    val isBoosting: Boolean
)

/**
 * Core Physics Integration Module responsible for numerical integration of vehicle velocity,
 * calculating aerodynamic and surface friction forces, and evaluating dynamic tire grip
 * using a non-linear Pacejka Magic Formula tire model with combined-slip friction circle clamping.
 */
class PhysicsIntegrationModule(
    val integrationMethod: IntegrationMethod = IntegrationMethod.SEMI_IMPLICIT_EULER
) {

    // Air density at sea level in kg/m^3
    val airDensityKgM3 = 1.225f

    // Standard gravity in m/s^2
    val gravity = 9.81f

    // Mechanical transmission constants
    val gearRatios = floatArrayOf(3.6f, 2.15f, 1.50f, 1.15f, 0.90f, 0.74f)
    val finalDrive = 3.9f
    val wheelRadiusM = 0.33f

    /**
     * Calculates Pacejka "Magic Formula" tire adhesion:
     * F(s) = D * sin(C * atan(B * s - E * (B * s - atan(B * s))))
     *
     * @param slip Normalized slip parameter (slip angle in radians or slip ratio)
     * @param B Stiffness factor
     * @param C Shape factor
     * @param D Peak friction force factor (mu * Fz)
     * @param E Curvature factor
     */
    fun pacejkaMagicFormula(
        slip: Float,
        B: Float,
        C: Float,
        D: Float,
        E: Float
    ): Float {
        val bSlip = B * slip
        val atanBSlip = atan(bSlip.toDouble()).toFloat()
        val inner = bSlip - E * (bSlip - atanBSlip)
        val sinArg = C * atan(inner.toDouble()).toFloat()
        return D * sin(sinArg.toDouble()).toFloat()
    }

    /**
     * Computes thermal tire grip efficiency curve.
     * Optimal window is between 75°C and 95°C.
     * Below 60°C tires are cold with reduced grip; above 115°C tires overheat and lose adhesion.
     */
    fun calculateThermalGripMultiplier(avgTireTempC: Float): Float {
        return when {
            avgTireTempC < 50f -> 0.75f + (avgTireTempC / 50f) * 0.10f
            avgTireTempC in 50f..75f -> 0.85f + ((avgTireTempC - 50f) / 25f) * 0.15f
            avgTireTempC in 75f..98f -> 1.0f // Peak operating window
            avgTireTempC in 98f..120f -> 1.0f - ((avgTireTempC - 98f) / 22f) * 0.18f
            else -> 0.82f - ((avgTireTempC - 120f) / 40f).coerceAtMost(0.25f) // Overheated blistering
        }
    }

    /**
     * Calculates rolling resistance coefficient based on surface condition.
     */
    fun calculateRollingResistanceCoeff(
        hazard: SurfaceHazardType?,
        isOffTrack: Boolean,
        weather: WeatherType
    ): Float {
        return when {
            hazard == SurfaceHazardType.GRAVEL_TRAP -> 0.45f
            hazard == SurfaceHazardType.OFFROAD_GRASS || isOffTrack -> 0.22f
            hazard == SurfaceHazardType.KERB_RUMBLE -> 0.040f
            hazard == SurfaceHazardType.PUDDLE -> 0.085f
            hazard == SurfaceHazardType.ICE_PATCH || weather == WeatherType.FREEZING_ICE -> 0.010f
            weather == WeatherType.LIGHT_RAIN || weather == WeatherType.THUNDERSTORM -> 0.022f
            else -> 0.015f // Dry asphalt
        }
    }

    /**
     * Calculates aerodynamic drag force opposing vehicle forward velocity:
     * F_drag = 0.5 * rho * Cd * A * v_long * |v_long| * dragMultiplier
     */
    fun calculateAerodynamicDrag(
        vLong: Float,
        dragMultiplier: Float = 1.0f,
        frontalAreaM2: Float = 2.1f,
        dragCoefficientCd: Float = 0.34f
    ): Float {
        val dynamicCoeff = 0.5f * airDensityKgM3 * dragCoefficientCd * frontalAreaM2 * dragMultiplier
        return dynamicCoeff * vLong * abs(vLong)
    }

    /**
     * Calculates rolling resistance force:
     * F_rr = Crr * N * sgn(v_long)
     */
    fun calculateRollingResistance(
        vLong: Float,
        normalLoadN: Float,
        rollingCoeff: Float
    ): Float {
        if (abs(vLong) < 0.05f) return 0f
        val direction = if (vLong > 0f) 1.0f else -1.0f
        return rollingCoeff * normalLoadN * direction
    }

    /**
     * Performs a single numerical physics integration step, calculating vehicle velocity,
     * opposing friction forces, and non-linear tire grip variables.
     */
    fun integrate(
        state: CarPhysicsState,
        vehicle: Vehicle,
        track: Track,
        currentWeather: WeatherType,
        trackWetness: Float,
        throttleInput: Float,
        brakeInput: Float,
        steerInput: Float,
        handbrakeInput: Boolean,
        boostInput: Boolean,
        dt: Float,
        weatherAdjustment: AdjustedVehiclePhysics? = null
    ): IntegrationResult {
        val dtSafe = dt.coerceIn(0.001f, 0.050f)

        // 1. Heading vectors
        val headingRad = Math.toRadians(state.headingDeg.toDouble())
        val forwardX = cos(headingRad).toFloat()
        val forwardY = sin(headingRad).toFloat()
        val rightX = -sin(headingRad).toFloat()
        val rightY = cos(headingRad).toFloat()

        // 2. Velocity decomposition in local vehicle coordinate frame
        val vLong = state.vx * forwardX + state.vy * forwardY
        val vLat = state.vx * rightX + state.vy * rightY
        val speedMs = sqrt(state.vx * state.vx + state.vy * state.vy)
        val speedKmh = speedMs * 3.6f

        // 3. Surface & Hazard Assessment
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

        // 4. Tire Compound & Weather Grip Modulation
        val compound = vehicle.upgrades.tireCompound
        val weatherDryFactor = compound.dryGripMultiplier
        val weatherWetFactor = compound.wetGripMultiplier
        val compoundGrip = weatherDryFactor * (1f - trackWetness) + weatherWetFactor * trackWetness

        // Aquaplaning risk check
        val isPuddle = activeHazard == SurfaceHazardType.PUDDLE
        val aquaplaneRisk = (speedKmh > 75f) && (trackWetness > 0.35f || isPuddle) &&
                (compound == TireCompound.SOFT_SLICK || compound == TireCompound.SEMI_SLICK)

        var surfaceGripFactor = 1.0f
        if (activeHazard != null) {
            surfaceGripFactor = activeHazard.gripFactor
            if (aquaplaneRisk) {
                surfaceGripFactor = 0.20f
            }
        }

        val weatherHandling = WeatherManager.getHandlingParameters(currentWeather)
        val gripMult = weatherAdjustment?.gripMultiplier ?: weatherHandling.gripMultiplier
        val brakeMult = weatherAdjustment?.brakingEfficiencyMultiplier ?: weatherHandling.brakingEfficiencyMultiplier
        val accelMult = weatherAdjustment?.accelerationTractionMultiplier ?: weatherHandling.accelerationTractionMultiplier
        val dragMult = weatherAdjustment?.topSpeedDragMultiplier ?: weatherHandling.topSpeedDragMultiplier
        val steerMult = weatherAdjustment?.steeringResponseMultiplier ?: weatherHandling.steeringResponseMultiplier

        // Thermal tire grip efficiency
        val avgTireTemp = (state.tireTempFL + state.tireTempFR + state.tireTempRL + state.tireTempRR) / 4f
        val thermalGripMult = calculateThermalGripMultiplier(avgTireTemp)

        // Composite tire friction coefficient (mu)
        val baseTireGrip = vehicle.effectiveGripMultiplier * compoundGrip * surfaceGripFactor * gripMult * thermalGripMult

        // 5. Normal Loads & Aerodynamic Downforce
        val vehicleMassKg = vehicle.effectiveWeightKg.toFloat()
        val staticNormalForce = vehicleMassKg * gravity
        val aeroDownforceFactor = vehicle.effectiveDownforceFactor
        val downforceN = 0.5f * airDensityKgM3 * (0.8f + aeroDownforceFactor) * (speedMs * speedMs)
        val totalNormalLoadN = staticNormalForce + downforceN

        // Dynamic longitudinal weight transfer: a_x * h / L
        // Assuming 50/50 static distribution, wheelbase L ~ 2.6m, CG height h ~ 0.5m
        val weightTransferRatio = (state.longitudinalG * gravity * 0.5f / 2.6f) * (vehicleMassKg / staticNormalForce)
        val normalLoadFrontN = (totalNormalLoadN * (0.5f - weightTransferRatio)).coerceAtLeast(totalNormalLoadN * 0.15f)
        val normalLoadRearN = (totalNormalLoadN * (0.5f + weightTransferRatio)).coerceAtLeast(totalNormalLoadN * 0.15f)

        // 6. Boost / Nitrous System
        var boostMultiplier = 1.0f
        var currentBoostGauge = state.boostGauge
        val isBoosting: Boolean
        if (boostInput && currentBoostGauge > 0.05f) {
            isBoosting = true
            currentBoostGauge = (currentBoostGauge - 0.25f * dtSafe).coerceAtLeast(0f)
            boostMultiplier = 1.45f
        } else {
            isBoosting = false
            currentBoostGauge = (currentBoostGauge + 0.08f * dtSafe).coerceAtMost(1.0f)
        }

        // 7. Engine Torque, RPM & Transmission
        val gearIdx = (state.currentGear - 1).coerceIn(0, 5)
        val currentRatio = gearRatios[gearIdx]
        val maxPowerWatts = vehicle.effectiveBhp * 745.7f * boostMultiplier
        val maxTorqueNm = (maxPowerWatts / (vehicle.baseMaxRpm * 0.1047f)) * 1.35f

        val wheelRps = (vLong.coerceAtLeast(0f) / (2f * PI.toFloat() * wheelRadiusM))
        val targetEngineRpm = (wheelRps * currentRatio * finalDrive * 60f)
            .coerceIn(900f, vehicle.baseMaxRpm.toFloat() + 400f)

        var newRpm = state.rpm
        if (throttleInput > 0.1f && vLong < 2f) {
            newRpm += 4500f * throttleInput * dtSafe
            if (newRpm > vehicle.baseMaxRpm) newRpm = vehicle.baseMaxRpm - 200f
        } else {
            newRpm += (targetEngineRpm - newRpm) * (8f * dtSafe)
        }
        newRpm = newRpm.coerceIn(850f, vehicle.baseMaxRpm.toFloat() + 300f)

        var newGear = state.currentGear
        if (newRpm > (vehicle.baseMaxRpm * 0.92f) && newGear < 6) {
            newGear++
            newRpm = vehicle.baseMaxRpm * 0.65f
        } else if (newRpm < 2200f && newGear > 1 && vLong > 2f) {
            newGear--
            newRpm = vehicle.baseMaxRpm * 0.72f
        }

        // 8. Longitudinal Driving & Braking Forces
        var drivingForce = 0f
        if (throttleInput > 0f) {
            val torque = maxTorqueNm * (0.6f + 0.4f * (newRpm / vehicle.baseMaxRpm))
            val drivetrainFactor = when (vehicle.driveTrain) {
                DriveTrain.AWD -> 1.05f
                DriveTrain.RWD -> 1.00f
                DriveTrain.FWD -> 0.90f
            }
            drivingForce = (torque * currentRatio * finalDrive / wheelRadiusM) * throttleInput * drivetrainFactor * accelMult
        }

        var brakingForce = 0f
        val isBraking = brakeInput > 0.05f
        if (isBraking) {
            val baseBrakeForce = staticNormalForce * 1.35f * vehicle.effectiveBrakingPower
            brakingForce = baseBrakeForce * brakeInput * brakeMult
        }

        // Reverse gear handling
        if (brakeInput > 0.3f && vLong <= 0.5f) {
            drivingForce = -maxTorqueNm * gearRatios[0] * 1.5f * brakeInput
            brakingForce = 0f
            newGear = 1
        }

        // 9. Real-Time Friction Forces Calculation
        val dragForce = calculateAerodynamicDrag(vLong, dragMult)
        val rollingCoeff = calculateRollingResistanceCoeff(activeHazard, isOffTrack, currentWeather)
        val rollingResistance = calculateRollingResistance(vLong, totalNormalLoadN, rollingCoeff)

        // Engine compression braking friction when off-throttle
        val engineBraking = if (throttleInput < 0.05f && vLong > 1.5f) {
            (newRpm / vehicle.baseMaxRpm) * 350f * currentRatio
        } else 0f

        // Anti-jitter Coulomb friction threshold when vehicle is at rest
        val coulombFriction = if (abs(vLong) < 0.2f && throttleInput < 0.05f) {
            (drivingForce - brakingForce).coerceIn(-400f, 400f)
        } else 0f

        val totalOpposingFriction = dragForce + rollingResistance + engineBraking

        // 10. Real-Time Tire Slip & Grip Modeling
        // Longitudinal slip ratio kappa
        val drivenWheelLinearSpeed = (newRpm / 60f) * (2f * PI.toFloat() * wheelRadiusM) / (currentRatio * finalDrive)
        val slipDenominator = max(abs(vLong), max(abs(drivenWheelLinearSpeed), 0.5f))
        val rawSlipRatio = if (throttleInput > 0f) {
            (drivenWheelLinearSpeed - vLong) / slipDenominator
        } else if (isBraking) {
            -(brakingForce / (staticNormalForce * baseTireGrip).coerceAtLeast(100f))
        } else 0f
        val slipRatio = rawSlipRatio.coerceIn(-1.0f, 1.5f)

        // Lateral slip angle alpha
        val slipAngleRad = if (abs(vLong) > 0.5f) atan2(vLat.toDouble(), abs(vLong).toDouble()).toFloat() else 0f
        val slipAngleDeg = Math.toDegrees(slipAngleRad.toDouble()).toFloat()

        // Peak slip angle (~7 to 11 degrees)
        val peakSlipDeg = 8.5f * (1f / baseTireGrip.coerceAtLeast(0.4f)) * (1f / steerMult)
        val normalizedLateralSlip = (abs(slipAngleDeg) / peakSlipDeg)

        // Handbrake overrides rear lateral adhesion to provoke drift
        val handbrakeMultiplier = if (handbrakeInput) 0.25f else 1.0f
        val maxAvailableLateralForce = totalNormalLoadN * baseTireGrip * handbrakeMultiplier

        // Pacejka Magic Formula curve parameters: B (stiffness), C (shape ~1.3), D (peak load), E (curvature ~-0.5)
        val pacejkaB = 0.22f * (steerMult / baseTireGrip.coerceAtLeast(0.3f))
        val pacejkaC = 1.35f
        val pacejkaD = maxAvailableLateralForce
        val pacejkaE = -0.45f
        val rawLateralTireForce = -pacejkaMagicFormula(
            slip = slipAngleDeg,
            B = pacejkaB,
            C = pacejkaC,
            D = pacejkaD,
            E = pacejkaE
        )

        // 11. Combined Slip Friction Circle / Ellipse Constraint
        // (Fx / FxMax)^2 + (Fy / FyMax)^2 <= 1.0
        val maxLongTireForce = totalNormalLoadN * baseTireGrip
        val requestedLongForce = drivingForce - (brakingForce * (if (vLong > 0) 1f else -1f))
        val longRatio = (requestedLongForce / maxLongTireForce.coerceAtLeast(10f))
        val latRatio = (rawLateralTireForce / maxAvailableLateralForce.coerceAtLeast(10f))
        val frictionCircleSum = (longRatio * longRatio) + (latRatio * latRatio)
        val frictionCircleUsage = sqrt(frictionCircleSum)

        val clampedLateralForce = if (frictionCircleUsage > 1.0f) {
            rawLateralTireForce / frictionCircleUsage
        } else {
            rawLateralTireForce
        }

        // Net Longitudinal Acceleration & Forces
        val netLongForce = requestedLongForce - totalOpposingFriction - coulombFriction
        val longAcc = netLongForce / vehicleMassKg
        val latAcc = clampedLateralForce / vehicleMassKg

        val longitudinalG = (longAcc / gravity).coerceIn(-2.5f, 2.5f)
        val lateralG = abs(latAcc) / gravity

        // Axle grip percentages
        val frontAxleGripPct = (normalLoadFrontN / staticNormalForce * baseTireGrip).coerceIn(0f, 1.5f)
        val rearAxleGripPct = (normalLoadRearN / staticNormalForce * baseTireGrip * handbrakeMultiplier).coerceIn(0f, 1.5f)

        // Drift, Wheelspin & Lockup States
        val isDrifting = abs(slipAngleDeg) > weatherHandling.lateralSlipThresholdDeg && speedMs > 6f
        val isWheelSpinning = slipRatio > 0.18f && throttleInput > 0.5f
        val isWheelLockup = slipRatio < -0.35f && isBraking

        // 12. Steering & Yaw Rate Integration
        val steerResponse = (vehicle.upgrades.suspensionStage * 0.1f + 1.0f)
        val speedSteerFactor = (1f / (1f + (speedMs / 28f))).coerceIn(0.28f, 1.0f)
        val targetAngularVel = steerInput * 165f * steerResponse * speedSteerFactor * (if (vLong < 0) -1f else 1f)

        val oversteerBias = when (vehicle.driveTrain) {
            DriveTrain.RWD -> 1.35f
            DriveTrain.AWD -> 0.75f
            DriveTrain.FWD -> 0.40f
        }
        val driftYawInfluence = if (isDrifting) -(slipAngleDeg * 1.8f * oversteerBias) else 0f

        var newAngularVel = state.angularVelocity + (targetAngularVel + driftYawInfluence - state.angularVelocity) * (14f * dtSafe)
        if (aquaplaneRisk) {
            newAngularVel *= 0.2f
        }

        var newHeadingDeg = state.headingDeg + newAngularVel * dtSafe
        while (newHeadingDeg < 0f) newHeadingDeg += 360f
        while (newHeadingDeg >= 360f) newHeadingDeg -= 360f

        // 13. Numerical Velocity Integration
        val newVLong: Float
        val newVLat: Float
        val newVx: Float
        val newVy: Float

        when (integrationMethod) {
            IntegrationMethod.SEMI_IMPLICIT_EULER -> {
                // Semi-Implicit Euler
                newVLong = vLong + longAcc * dtSafe
                newVLat = vLat + latAcc * dtSafe
                val newHeadingRad = Math.toRadians(newHeadingDeg.toDouble())
                val newForwardX = cos(newHeadingRad).toFloat()
                val newForwardY = sin(newHeadingRad).toFloat()
                newVx = newVLong * newForwardX + newVLat * (-sin(newHeadingRad).toFloat())
                newVy = newVLong * newForwardY + newVLat * (cos(newHeadingRad).toFloat())
            }
            IntegrationMethod.RUNGE_KUTTA_2 -> {
                // Midpoint predictor-corrector
                val midVLong = vLong + 0.5f * longAcc * dtSafe
                val midVLat = vLat + 0.5f * latAcc * dtSafe
                newVLong = vLong + midVLong * dtSafe
                newVLat = vLat + midVLat * dtSafe
                val newHeadingRad = Math.toRadians(newHeadingDeg.toDouble())
                val newForwardX = cos(newHeadingRad).toFloat()
                val newForwardY = sin(newHeadingRad).toFloat()
                newVx = newVLong * newForwardX + newVLat * (-sin(newHeadingRad).toFloat())
                newVy = newVLong * newForwardY + newVLat * (cos(newHeadingRad).toFloat())
            }
        }

        // Stationary anti-drift threshold
        val finalVx = if (speedMs < 0.08f && throttleInput < 0.05f) 0f else newVx
        val finalVy = if (speedMs < 0.08f && throttleInput < 0.05f) 0f else newVy

        // 14. Numerical Position Integration
        val newX = state.x + finalVx * dtSafe
        val newY = state.y + finalVy * dtSafe

        val newSpeedMs = sqrt(finalVx * finalVx + finalVy * finalVy)
        val newSpeedKmh = newSpeedMs * 3.6f

        // Build result packages
        val velocityState = VehicleVelocityState(
            vx = finalVx,
            vy = finalVy,
            speedMs = newSpeedMs,
            speedKmh = newSpeedKmh,
            vLong = newVLong,
            vLat = newVLat,
            headingDeg = newHeadingDeg,
            headingRad = Math.toRadians(newHeadingDeg.toDouble()),
            angularVelocityDegPerSec = newAngularVel,
            longitudinalAcc = longAcc,
            lateralAcc = latAcc,
            longitudinalG = longitudinalG,
            lateralG = lateralG
        )

        val frictionState = FrictionForcesState(
            aerodynamicDragForceN = dragForce,
            rollingResistanceForceN = rollingResistance,
            engineBrakingForceN = engineBraking,
            coulombFrictionForceN = coulombFriction,
            totalOpposingFrictionN = totalOpposingFriction,
            rollingResistanceCoefficient = rollingCoeff,
            aerodynamicDragCoefficientProduct = 0.5f * airDensityKgM3 * 0.34f * 2.1f
        )

        val gripState = TireGripState(
            normalLoadFrontN = normalLoadFrontN,
            normalLoadRearN = normalLoadRearN,
            totalNormalLoadN = totalNormalLoadN,
            slipRatio = slipRatio,
            slipAngleDeg = slipAngleDeg,
            peakGripCoeffLongitudinal = baseTireGrip,
            peakGripCoeffLateral = baseTireGrip * handbrakeMultiplier,
            compoundGripMultiplier = compoundGrip,
            thermalGripMultiplier = thermalGripMult,
            surfaceGripMultiplier = surfaceGripFactor * gripMult,
            effectiveTireGripPct = baseTireGrip,
            frontAxleGripPct = frontAxleGripPct,
            rearAxleGripPct = rearAxleGripPct,
            tractionForceN = drivingForce,
            brakingTireForceN = brakingForce,
            corneringForceN = abs(clampedLateralForce),
            frictionCircleUtilizationPct = (frictionCircleUsage * 100f).coerceIn(0f, 150f),
            isDrifting = isDrifting,
            isWheelSpinning = isWheelSpinning,
            isWheelLockup = isWheelLockup,
            isAquaplaning = aquaplaneRisk
        )

        return IntegrationResult(
            velocity = velocityState,
            friction = frictionState,
            tireGrip = gripState,
            newX = newX,
            newY = newY,
            newHeadingDeg = newHeadingDeg,
            newAngularVelocity = newAngularVel,
            newRpm = newRpm,
            newGear = newGear,
            boostGauge = currentBoostGauge,
            isBoosting = isBoosting
        )
    }
}
