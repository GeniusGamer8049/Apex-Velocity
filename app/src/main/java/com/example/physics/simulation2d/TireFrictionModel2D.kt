package com.example.physics.simulation2d

import com.example.model.DriveTrain
import com.example.model.TireCompound
import com.example.model.WeatherType
import org.jbox2d.common.Vec2
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
 * Identifier for each of the 4 vehicle wheels in the 2D simulation.
 */
enum class WheelId2D {
    FRONT_LEFT,
    FRONT_RIGHT,
    REAR_LEFT,
    REAR_RIGHT;

    val isFront: Boolean get() = this == FRONT_LEFT || this == FRONT_RIGHT
    val isRear: Boolean get() = this == REAR_LEFT || this == REAR_RIGHT
    val isLeft: Boolean get() = this == FRONT_LEFT || this == REAR_LEFT
    val isRight: Boolean get() = this == FRONT_RIGHT || this == REAR_RIGHT
}

/**
 * Detailed telemetry snapshot for an individual tire in the 2D physics simulation.
 */
data class Wheel2DTelemetry(
    val wheelId: WheelId2D,
    val localPositionM: Vec2,
    val normalLoadN: Float = 0f,
    val slipAngleDeg: Float = 0f,
    val slipRatio: Float = 0f,
    val corneringForceN: Float = 0f,
    val tractionForceN: Float = 0f,
    val totalForceN: Float = 0f,
    val maxAvailableGripN: Float = 0f,
    val frictionCircleUsagePct: Float = 0f,
    val isSliding: Boolean = false,
    val isWheelSpin: Boolean = false,
    val isWheelLockup: Boolean = false
) {
    val formattedNormalLoad: String
        get() = String.format(Locale.US, "%.0f N", normalLoadN)

    val formattedSlipAngle: String
        get() = String.format(Locale.US, "%.1f°", slipAngleDeg)

    val formattedTireForces: String
        get() = String.format(Locale.US, "Fx: %.0f N | Fy: %.0f N", tractionForceN, corneringForceN)
}

/**
 * Collective telemetry for all 4 tires and contact patch states.
 */
data class VehicleTireTelemetry2D(
    val wheels: Map<WheelId2D, Wheel2DTelemetry>,
    val totalCorneringForceN: Float,
    val totalTractionForceN: Float,
    val totalBrakingForceN: Float,
    val totalNormalLoadN: Float,
    val averageFrictionCircleUsagePct: Float,
    val frontAxleGripPct: Float,
    val rearAxleGripPct: Float,
    val isDrifting: Boolean,
    val surfaceFrictionCoefficient: Float
) {
    val formattedCorneringForce: String
        get() = String.format(Locale.US, "%.0f N", totalCorneringForceN)

    val formattedTractionForce: String
        get() = String.format(Locale.US, "%.0f N", totalTractionForceN)
}

/**
 * Tire Friction Model for 2D Vehicle Simulation using Pacejka Magic Formula
 * and combined-slip friction circle clamping.
 */
class TireFrictionModel2D(
    var massKg: Float = 1350f,
    var wheelbaseM: Float = 2.65f,
    var trackWidthM: Float = 1.78f,
    var cogHeightM: Float = 0.48f,
    var weightDistributionFrontPct: Float = 0.52f, // 52% front, 48% rear static
    var driveTrain: DriveTrain = DriveTrain.AWD,
    var tireCompound: TireCompound = TireCompound.SPORT_RADIAL
) {
    val gravity = 9.81f

    // Pacejka Magic Formula coefficients for lateral cornering force:
    // F_y = D * sin(C * atan(B * alpha - E * (B * alpha - atan(B * alpha)))) * F_z
    var pacejkaB: Float = 10.0f  // Stiffness factor
    var pacejkaC: Float = 1.30f  // Shape factor
    var pacejkaD: Float = 1.05f  // Peak friction factor (dry reference)
    var pacejkaE: Float = -0.97f // Curvature factor

    // Rolling resistance coefficient Crr (surface dependent)
    var rollingResistanceCrr: Float = 0.015f

    /**
     * Local offsets from center of gravity for each wheel contact point (in meters).
     * Coordinate system: X is forward (+), Y is left (+) in local vehicle frame.
     */
    fun getWheelLocalPositions(): Map<WheelId2D, Vec2> {
        val distFront = wheelbaseM * (1f - weightDistributionFrontPct) // Distance from CoG to front axle
        val distRear = -wheelbaseM * weightDistributionFrontPct        // Distance from CoG to rear axle
        val halfTrack = trackWidthM * 0.5f

        return mapOf(
            WheelId2D.FRONT_LEFT to Vec2(distFront, halfTrack),
            WheelId2D.FRONT_RIGHT to Vec2(distFront, -halfTrack),
            WheelId2D.REAR_LEFT to Vec2(distRear, halfTrack),
            WheelId2D.REAR_RIGHT to Vec2(distRear, -halfTrack)
        )
    }

    /**
     * Calculates the surface friction multiplier based on weather conditions and track wetness.
     */
    fun getSurfaceFrictionCoefficient(weather: WeatherType, trackWetness: Float = 0f): Float {
        val weatherBase = when (weather) {
            WeatherType.SUNNY_CLEAR -> 1.0f
            WeatherType.OVERCAST -> 0.95f
            WeatherType.FOG_MIST -> 0.88f
            WeatherType.LIGHT_RAIN -> 0.76f
            WeatherType.THUNDERSTORM -> 0.58f
            WeatherType.FREEZING_ICE -> 0.38f
        }
        val wetnessReduction = (1.0f - trackWetness * 0.45f).coerceIn(0.25f, 1.0f)
        val compoundMultiplier = when (tireCompound) {
            TireCompound.SPORT_RADIAL -> if (trackWetness > 0.4f) 0.88f else 1.0f
            TireCompound.SOFT_SLICK -> if (trackWetness > 0.4f) 0.45f else 1.18f
            TireCompound.SEMI_SLICK -> if (trackWetness > 0.4f) 0.60f else 1.10f
            TireCompound.WET_INTERMEDIATE -> if (trackWetness > 0.4f) 1.08f else 0.94f
            TireCompound.FULL_WET -> if (trackWetness > 0.4f) 1.22f else 0.86f
        }
        return (weatherBase * wetnessReduction * compoundMultiplier).coerceIn(0.2f, 1.35f)
    }

    /**
     * Calculates dynamic vertical normal loads on each wheel (in Newtons), accounting for:
     * - Static weight distribution
     * - Dynamic longitudinal load transfer (acceleration / braking)
     * - Dynamic lateral load transfer (cornering G-forces)
     * - Aerodynamic downforce
     */
    fun calculateNormalLoads(
        longitudinalAccMs2: Float,
        lateralAccMs2: Float,
        frontDownforceN: Float,
        rearDownforceN: Float
    ): Map<WheelId2D, Float> {
        val totalWeight = massKg * gravity
        val staticFront = totalWeight * weightDistributionFrontPct
        val staticRear = totalWeight * (1f - weightDistributionFrontPct)

        // Longitudinal load transfer: dFz_long = (m * ax * h) / L
        // Positive acceleration transfers load to rear; braking transfers load to front
        val longTransfer = (massKg * longitudinalAccMs2 * cogHeightM) / wheelbaseM

        // Total axle vertical loads (including downforce)
        val axleLoadFront = max(200f, staticFront - longTransfer + frontDownforceN)
        val axleLoadRear = max(200f, staticRear + longTransfer + rearDownforceN)

        // Lateral load transfer: dFz_lat = (m * ay * h) / W
        // Cornering transfers load to outside wheels
        val totalLatTransfer = (massKg * abs(lateralAccMs2) * cogHeightM) / trackWidthM
        val latTransferFront = totalLatTransfer * weightDistributionFrontPct
        val latTransferRear = totalLatTransfer * (1f - weightDistributionFrontPct)

        val isTurningLeft = lateralAccMs2 > 0f // positive lateral acceleration = turning left (weight moves to right)

        val (fl, fr) = if (isTurningLeft) {
            Pair(
                max(100f, axleLoadFront * 0.5f - latTransferFront),
                max(100f, axleLoadFront * 0.5f + latTransferFront)
            )
        } else {
            Pair(
                max(100f, axleLoadFront * 0.5f + latTransferFront),
                max(100f, axleLoadFront * 0.5f - latTransferFront)
            )
        }

        val (rl, rr) = if (isTurningLeft) {
            Pair(
                max(100f, axleLoadRear * 0.5f - latTransferRear),
                max(100f, axleLoadRear * 0.5f + latTransferRear)
            )
        } else {
            Pair(
                max(100f, axleLoadRear * 0.5f + latTransferRear),
                max(100f, axleLoadRear * 0.5f - latTransferRear)
            )
        }

        return mapOf(
            WheelId2D.FRONT_LEFT to fl,
            WheelId2D.FRONT_RIGHT to fr,
            WheelId2D.REAR_LEFT to rl,
            WheelId2D.REAR_RIGHT to rr
        )
    }

    /**
     * Evaluates tire friction forces for all 4 wheels and returns the local reaction forces
     * and telemetry snapshot.
     */
    fun evaluateTireForces(
        wheelLocalVelocities: Map<WheelId2D, Vec2>, // Local forward (x) and lateral (y) velocity at each wheel
        steerAngleRad: Float,                      // Front steering angle in radians
        throttleInput: Float,                      // 0..1
        brakeInput: Float,                         // 0..1
        handbrake: Boolean,
        boost: Boolean,
        engineDriveForceN: Float,
        normalLoads: Map<WheelId2D, Float>,
        surfaceMu: Float
    ): Pair<Map<WheelId2D, Vec2>, VehicleTireTelemetry2D> {
        val wheelForces = mutableMapOf<WheelId2D, Vec2>()
        val wheelTelemetry = mutableMapOf<WheelId2D, Wheel2DTelemetry>()

        var totalCornering = 0f
        var totalTraction = 0f
        var totalBraking = 0f
        var totalNormal = 0f
        var sumFrictionUsage = 0f

        val localWheelPositions = getWheelLocalPositions()

        for (wheel in WheelId2D.entries) {
            val normalLoad = normalLoads[wheel] ?: (massKg * gravity * 0.25f)
            totalNormal += normalLoad
            val localPos = localWheelPositions[wheel] ?: Vec2(0f, 0f)

            // Local velocity at contact patch
            val vLocal = wheelLocalVelocities[wheel] ?: Vec2(0f, 0f)

            // For front wheels, transform local velocity by steering angle
            val (vTireLong, vTireLat) = if (wheel.isFront) {
                val cosS = cos(-steerAngleRad)
                val sinS = sin(-steerAngleRad)
                Pair(
                    vLocal.x * cosS - vLocal.y * sinS,
                    vLocal.x * sinS + vLocal.y * cosS
                )
            } else {
                Pair(vLocal.x, vLocal.y)
            }

            // 1. Calculate Tire Slip Angle (alpha)
            val speedLong = abs(vTireLong)
            val slipAngleRad = atan2(vTireLat, speedLong.coerceAtLeast(0.5f))
            val slipAngleDeg = (slipAngleRad * 180f / PI).toFloat()

            // 2. Drive Torque / Traction Force Allocation
            val driveShare = when (driveTrain) {
                DriveTrain.FWD -> if (wheel.isFront) 0.5f else 0.0f
                DriveTrain.RWD -> if (wheel.isRear) 0.5f else 0.0f
                DriveTrain.AWD -> if (wheel.isFront) 0.22f else 0.28f
            }
            val boostMult = if (boost) 1.35f else 1.0f
            var desiredLongForce = throttleInput * engineDriveForceN * driveShare * boostMult

            // 3. Braking Force Allocation (62% front bias, 38% rear bias)
            val brakeBias = if (wheel.isFront) 0.31f else 0.19f
            val maxBrakeAxleForce = massKg * gravity * 1.2f
            val brakingForce = brakeInput * maxBrakeAxleForce * brakeBias

            if (brakingForce > 0f) {
                // Braking opposes forward rolling velocity
                val signLong = if (vTireLong > 0.05f) 1f else if (vTireLong < -0.05f) -1f else 0f
                desiredLongForce -= (brakingForce * signLong)
                totalBraking += brakingForce
            }

            // Handbrake applies to rear wheels only, causing full wheel lockup
            if (handbrake && wheel.isRear) {
                desiredLongForce = -normalLoad * surfaceMu * 0.85f * (if (vTireLong > 0.05f) 1f else -1f)
            }

            // Rolling resistance opposing tire motion
            val rollingResistance = rollingResistanceCrr * normalLoad *
                    (if (vTireLong > 0.05f) 1f else if (vTireLong < -0.05f) -1f else 0f)
            desiredLongForce -= rollingResistance

            // 4. Maximum available friction according to Coulomb / Pacejka peak: F_max = mu * F_z
            val maxTireFriction = normalLoad * surfaceMu * pacejkaD

            // 5. Lateral Cornering Force via Pacejka Magic Formula:
            // alpha in radians, cornering stiffness B, shape C, curvature E
            val bAlpha = pacejkaB * slipAngleRad
            val corneringGripCoeff = surfaceMu * pacejkaD * sin(
                pacejkaC * atan(bAlpha - pacejkaE * (bAlpha - atan(bAlpha)))
            )
            // Lateral force opposes lateral tire velocity
            val desiredLateralForce = -corneringGripCoeff * normalLoad

            // 6. Friction Circle / Ellipse Clamping:
            // (Fx / Fx_max)^2 + (Fy / Fy_max)^2 <= 1.0
            val fxFraction = abs(desiredLongForce) / maxTireFriction.coerceAtLeast(1f)
            val fyFraction = abs(desiredLateralForce) / maxTireFriction.coerceAtLeast(1f)
            val combinedSlip = sqrt(fxFraction * fxFraction + fyFraction * fyFraction)
            val frictionCircleUsagePct = min(150f, combinedSlip * 100f)

            val (actualLongForce, actualLatForce) = if (combinedSlip > 1.0f) {
                // Grip saturation: scale both forces to sit on the friction circle
                val scale = 1.0f / combinedSlip
                Pair(desiredLongForce * scale, desiredLateralForce * scale)
            } else {
                Pair(desiredLongForce, desiredLateralForce)
            }

            // Transform wheel tire forces back to vehicle chassis frame (for front wheels, undo steer angle)
            val (bodyFx, bodyFy) = if (wheel.isFront) {
                val cosS = cos(steerAngleRad)
                val sinS = sin(steerAngleRad)
                Pair(
                    actualLongForce * cosS - actualLatForce * sinS,
                    actualLongForce * sinS + actualLatForce * cosS
                )
            } else {
                Pair(actualLongForce, actualLatForce)
            }

            wheelForces[wheel] = Vec2(bodyFx, bodyFy)

            val isWheelSpin = throttleInput > 0.6f && fxFraction > 0.95f
            val isWheelLockup = (brakeInput > 0.7f && fxFraction > 0.95f) || (handbrake && wheel.isRear)
            val isSliding = combinedSlip >= 0.95f || abs(slipAngleDeg) > 8.5f

            wheelTelemetry[wheel] = Wheel2DTelemetry(
                wheelId = wheel,
                localPositionM = localPos,
                normalLoadN = normalLoad,
                slipAngleDeg = slipAngleDeg,
                slipRatio = (fxFraction * if (desiredLongForce > 0) 1f else -1f).coerceIn(-1.5f, 1.5f),
                corneringForceN = actualLatForce,
                tractionForceN = actualLongForce,
                totalForceN = sqrt(bodyFx * bodyFx + bodyFy * bodyFy),
                maxAvailableGripN = maxTireFriction,
                frictionCircleUsagePct = frictionCircleUsagePct,
                isSliding = isSliding,
                isWheelSpin = isWheelSpin,
                isWheelLockup = isWheelLockup
            )

            totalCornering += abs(actualLatForce)
            totalTraction += max(0f, actualLongForce)
            sumFrictionUsage += frictionCircleUsagePct
        }

        val avgFrictionUsage = sumFrictionUsage / 4f
        val frontGrip = 1f - min(1f, (wheelTelemetry[WheelId2D.FRONT_LEFT]?.frictionCircleUsagePct ?: 0f) / 120f * 0.5f)
        val rearGrip = 1f - min(1f, (wheelTelemetry[WheelId2D.REAR_LEFT]?.frictionCircleUsagePct ?: 0f) / 120f * 0.5f)
        val isDrifting = (wheelTelemetry[WheelId2D.REAR_LEFT]?.isSliding == true ||
                wheelTelemetry[WheelId2D.REAR_RIGHT]?.isSliding == true) &&
                abs((wheelTelemetry[WheelId2D.REAR_LEFT]?.slipAngleDeg ?: 0f)) > 10.0f

        val telemetry = VehicleTireTelemetry2D(
            wheels = wheelTelemetry,
            totalCorneringForceN = totalCornering,
            totalTractionForceN = totalTraction,
            totalBrakingForceN = totalBraking,
            totalNormalLoadN = totalNormal,
            averageFrictionCircleUsagePct = avgFrictionUsage,
            frontAxleGripPct = frontGrip,
            rearAxleGripPct = rearGrip,
            isDrifting = isDrifting,
            surfaceFrictionCoefficient = surfaceMu
        )

        return Pair(wheelForces, telemetry)
    }
}
