package com.example.physics.simulation2d

import com.example.model.DriveTrain
import com.example.model.TireCompound
import com.example.model.Vehicle
import com.example.model.WeatherType
import org.jbox2d.collision.shapes.MassData
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-level state and telemetry representation for the 2D rigid-body vehicle.
 */
data class VehicleBody2DState(
    val xM: Float = 0f,
    val yM: Float = 0f,
    val vxMps: Float = 0f,
    val vyMps: Float = 0f,
    val speedMps: Float = 0f,
    val speedKmh: Float = 0f,
    val headingDeg: Float = 0f,
    val headingRad: Float = 0f,
    val angularVelocityRadPerSec: Float = 0f,
    val angularVelocityDegPerSec: Float = 0f,
    val localLongitudinalVelocityMps: Float = 0f,
    val localLateralVelocityMps: Float = 0f,
    val longitudinalAccMs2: Float = 0f,
    val lateralAccMs2: Float = 0f,
    val longitudinalG: Float = 0f,
    val lateralG: Float = 0f,
    val currentSteerAngleDeg: Float = 0f,
    val slipAngleDeg: Float = 0f
) {
    val formattedSpeed: String
        get() = "${speedKmh.toInt()} km/h"

    val formattedHeading: String
        get() = String.format(Locale.US, "%.1f°", headingDeg)

    val formattedGForces: String
        get() = String.format(Locale.US, "Long: %+.2f G | Lat: %+.2f G", longitudinalG, lateralG)
}

/**
 * Encapsulates the JBox2D rigid-body vehicle chassis, steering kinematics,
 * powertrain distribution, aerodynamic integration, and tire contact patches.
 */
class VehicleBody2D(
    val world: World,
    var vehicleWidthM: Float = 1.9f,
    var vehicleLengthM: Float = 4.4f,
    var massKg: Float = 1350f,
    val aeroModel: AeroDragModel2D = AeroDragModel2D(),
    val tireModel: TireFrictionModel2D = TireFrictionModel2D(massKg = massKg)
) {
    // Reference to JBox2D rigid body
    val body: Body

    // Steering parameters
    var maxSteerAngleRad: Float = (32f * PI / 180f).toFloat() // 32 degrees max lock
    var currentSteerAngleRad: Float = 0f
    var steerResponseSpeedRadPerSec: Float = (180f * PI / 180f).toFloat()

    // Engine and transmission characteristics
    var enginePeakPowerWatts: Float = 350_000f // ~475 HP
    var currentRpm: Float = 1000f
    var currentGear: Int = 1

    // Previous velocity and acceleration tracking for G-force calculations
    private var prevLocalVLong = 0f
    private var prevLocalVLat = 0f
    private var smoothedLongAcc = 0f
    private var smoothedLatAcc = 0f

    init {
        // 1. Define JBox2D dynamic body
        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(0f, 0f)
            angle = 0f
            linearDamping = 0.05f  // Minimal background damping; aerodynamic drag handles air resistance
            angularDamping = 0.8f  // Vehicle yaw damping
            allowSleep = false
        }
        body = world.createBody(bodyDef)

        // 2. Define Box fixture matching vehicle dimensions
        val boxShape = PolygonShape().apply {
            setAsBox(vehicleLengthM * 0.5f, vehicleWidthM * 0.5f)
        }

        val fixtureDef = FixtureDef().apply {
            shape = boxShape
            density = massKg / (vehicleLengthM * vehicleWidthM)
            friction = 0.2f
            restitution = 0.1f
        }
        body.createFixture(fixtureDef)

        // Ensure body mass matches exactly
        val massData = MassData().apply {
            mass = massKg
            I = (1f / 12f) * massKg * (vehicleLengthM * vehicleLengthM + vehicleWidthM * vehicleWidthM) * 1.25f
        }
        body.setMassData(massData)
    }

    /**
     * Reconfigures body parameters based on game vehicle definitions.
     */
    fun configureFromVehicle(vehicle: Vehicle) {
        massKg = vehicle.effectiveWeightKg.toFloat()
        tireModel.massKg = massKg
        tireModel.driveTrain = vehicle.driveTrain
        tireModel.tireCompound = vehicle.upgrades.tireCompound

        aeroModel.dragCoefficientCd = 0.32f + (vehicle.upgrades.aeroStage * 0.02f)
        aeroModel.downforceCoefficientCl = 0.35f + (vehicle.effectiveDownforceFactor * 0.45f)
        enginePeakPowerWatts = (vehicle.effectiveBhp * 745.7f).coerceAtLeast(100_000f)

        val massData = MassData().apply {
            mass = massKg
            I = (1f / 12f) * massKg * (vehicleLengthM * vehicleLengthM + vehicleWidthM * vehicleWidthM) * 1.25f
        }
        body.setMassData(massData)
    }

    /**
     * Resets position, heading, and velocities.
     */
    fun reset(xM: Float, yM: Float, headingDeg: Float) {
        val headingRad = (headingDeg * PI / 180f).toFloat()
        body.setTransform(Vec2(xM, yM), headingRad)
        body.setLinearVelocity(Vec2(0f, 0f))
        body.setAngularVelocity(0f)
        currentSteerAngleRad = 0f
        prevLocalVLong = 0f
        prevLocalVLat = 0f
        smoothedLongAcc = 0f
        smoothedLatAcc = 0f
    }

    /**
     * Steps vehicle behavior, applying aerodynamic drag and tire friction forces
     * for this physics simulation tick.
     */
    fun update(
        throttleInput: Float,
        brakeInput: Float,
        steerInput: Float,
        handbrake: Boolean,
        boost: Boolean,
        surfaceGrip: Float,
        weather: WeatherType,
        dt: Float
    ): Triple<VehicleBody2DState, AeroDragTelemetry2D, VehicleTireTelemetry2D> {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // 1. Steering Kinematics: smooth steering towards target angle
        // At higher speeds, reduce maximum steering lock to simulate speed-sensitive steering
        val currentSpeedMps = body.getLinearVelocity().length()
        val speedReduction = (1.0f - (currentSpeedMps / 80f) * 0.55f).coerceIn(0.35f, 1.0f)
        val targetSteer = steerInput * maxSteerAngleRad * speedReduction

        val steerDiff = targetSteer - currentSteerAngleRad
        val maxSteerDelta = steerResponseSpeedRadPerSec * clampedDt
        currentSteerAngleRad += steerDiff.coerceIn(-maxSteerDelta, maxSteerDelta)

        // 2. Chassis heading and local reference frame
        val headingRad = body.getAngle()
        val cosH = cos(headingRad)
        val sinH = sin(headingRad)

        // Local longitudinal unit vector (forward) & lateral unit vector (left) in world space
        val forwardUnit = Vec2(cosH, sinH)
        val leftUnit = Vec2(-sinH, cosH)

        val linearVelocity = body.getLinearVelocity()
        val angularVel = body.getAngularVelocity()

        // Velocity in local vehicle frame:
        // vLong = velocity . forwardUnit
        // vLat = velocity . leftUnit
        val vLong = linearVelocity.x * forwardUnit.x + linearVelocity.y * forwardUnit.y
        val vLat = linearVelocity.x * leftUnit.x + linearVelocity.y * leftUnit.y

        // Calculate dynamic accelerations and G-forces
        val rawLongAcc = (vLong - prevLocalVLong) / clampedDt
        val rawLatAcc = (vLat - prevLocalVLat) / clampedDt + (vLong * angularVel) // centripetal term
        prevLocalVLong = vLong
        prevLocalVLat = vLat

        smoothedLongAcc = smoothedLongAcc * 0.8f + rawLongAcc * 0.2f
        smoothedLatAcc = smoothedLatAcc * 0.8f + rawLatAcc * 0.2f

        val longG = smoothedLongAcc / 9.81f
        val latG = smoothedLatAcc / 9.81f

        // 3. Compute Aerodynamic Drag and Downforce Forces
        val (aeroForceVec, aeroTelemetry) = aeroModel.calculateAeroForces(
            velocity = linearVelocity,
            angularVelocityRadPerSec = angularVel,
            weather = weather
        )
        // Apply aero drag force opposite to velocity at center of mass
        body.applyForce(aeroForceVec, body.getWorldCenter())
        // Apply aerodynamic yaw stabilizing damping torque
        if (abs(aeroTelemetry.yawDampingTorqueNm) > 0.01f) {
            body.applyTorque(aeroTelemetry.yawDampingTorqueNm)
        }

        // 4. Calculate Dynamic Vertical Normal Loads on 4 tires
        val normalLoads = tireModel.calculateNormalLoads(
            longitudinalAccMs2 = smoothedLongAcc,
            lateralAccMs2 = smoothedLatAcc,
            frontDownforceN = aeroTelemetry.frontDownforceN,
            rearDownforceN = aeroTelemetry.rearDownforceN
        )

        // 5. Calculate Local Velocities at each wheel's contact patch
        val wheelPositions = tireModel.getWheelLocalPositions()
        val wheelVelocities = mutableMapOf<WheelId2D, Vec2>()

        for ((wheelId, localPos) in wheelPositions) {
            // v_wheel_local = (vLong, vLat) + omega x localPos
            // In 2D: omega x (rx, ry) = (-omega * ry, omega * rx)
            val wheelVLong = vLong - angularVel * localPos.y
            val wheelVLat = vLat + angularVel * localPos.x
            wheelVelocities[wheelId] = Vec2(wheelVLong, wheelVLat)
        }

        // 6. Powertrain engine drive force: F_drive = Power / speed (with low-speed traction clamp)
        val speedKmh = currentSpeedMps * 3.6f
        val effectiveSpeedForPower = max(4.0f, abs(vLong))
        val engineDriveForceN = (enginePeakPowerWatts / effectiveSpeedForPower).coerceIn(0f, massKg * 9.81f * 1.5f)

        // 7. Evaluate 4-Wheel Tire Friction Forces via Pacejka and Friction Circle
        val effectiveSurfaceMu = surfaceGrip * tireModel.getSurfaceFrictionCoefficient(weather)
        val (tireForcesInBodyFrame, tireTelemetry) = tireModel.evaluateTireForces(
            wheelLocalVelocities = wheelVelocities,
            steerAngleRad = currentSteerAngleRad,
            throttleInput = throttleInput,
            brakeInput = brakeInput,
            handbrake = handbrake,
            boost = boost,
            engineDriveForceN = engineDriveForceN,
            normalLoads = normalLoads,
            surfaceMu = effectiveSurfaceMu
        )

        // 8. Apply Tire Reaction Forces to JBox2D Body at each wheel world location
        for ((wheelId, localForce) in tireForcesInBodyFrame) {
            val localOffset = wheelPositions[wheelId] ?: Vec2(0f, 0f)

            // Transform local wheel force (x = forward, y = left) to world coordinates:
            // F_world = F_x * forwardUnit + F_y * leftUnit
            val worldForce = Vec2(
                localForce.x * forwardUnit.x + localForce.y * leftUnit.x,
                localForce.x * forwardUnit.y + localForce.y * leftUnit.y
            )

            // World contact patch location:
            // P_world = body.worldCenter + rx * forwardUnit + ry * leftUnit
            val worldPos = body.getWorldPoint(localOffset)

            body.applyForce(worldForce, worldPos)
        }

        // Calculate body slip angle (angle between heading and velocity)
        val bodySlipAngleRad = if (currentSpeedMps > 1.0f) {
            atan2(vLat, abs(vLong))
        } else 0f
        val bodySlipAngleDeg = (bodySlipAngleRad * 180f / PI).toFloat()

        // Construct updated vehicle state snapshot
        var normHeadingDeg = (headingRad * 180f / PI).toFloat() % 360f
        if (normHeadingDeg < 0f) normHeadingDeg += 360f

        val stateSnapshot = VehicleBody2DState(
            xM = body.getPosition().x,
            yM = body.getPosition().y,
            vxMps = linearVelocity.x,
            vyMps = linearVelocity.y,
            speedMps = currentSpeedMps,
            speedKmh = speedKmh,
            headingDeg = normHeadingDeg,
            headingRad = headingRad,
            angularVelocityRadPerSec = angularVel,
            angularVelocityDegPerSec = (angularVel * 180f / PI).toFloat(),
            localLongitudinalVelocityMps = vLong,
            localLateralVelocityMps = vLat,
            longitudinalAccMs2 = smoothedLongAcc,
            lateralAccMs2 = smoothedLatAcc,
            longitudinalG = longG,
            lateralG = latG,
            currentSteerAngleDeg = (currentSteerAngleRad * 180f / PI).toFloat(),
            slipAngleDeg = bodySlipAngleDeg
        )

        return Triple(stateSnapshot, aeroTelemetry, tireTelemetry)
    }
}
