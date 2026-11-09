package com.example.physics.simulation2d

import com.example.model.Track
import com.example.model.Vehicle
import com.example.model.WeatherType
import com.example.physics.CarPhysicsState
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Unified telemetry snapshot emitted by the 2D Physics Simulation.
 */
data class VehicleSimulation2DTelemetry(
    val bodyState: VehicleBody2DState,
    val aeroTelemetry: AeroDragTelemetry2D,
    val tireTelemetry: VehicleTireTelemetry2D,
    val simulationStepCount: Long = 0L,
    val fixedDtSec: Float = 1f / 60f
) {
    val summaryLine: String
        get() = String.format(
            Locale.US,
            "Speed: %.0f km/h | Aero Drag: %.0f N | Downforce: %.0f N | Cornering: %.0f N | Grip: %.0f%%",
            bodyState.speedKmh,
            aeroTelemetry.dragForceN,
            aeroTelemetry.downforceN,
            tireTelemetry.totalCorneringForceN,
            (1f - tireTelemetry.averageFrictionCircleUsagePct / 150f).coerceIn(0f, 1f) * 100f
        )
}

/**
 * Master 2D Physics Simulation coordinator using JBox2D to model vehicle rigid-body
 * behavior, 4-wheel tire friction dynamics, and aerodynamic drag/downforce for racing gameplay.
 */
class VehicleSimulation2D(
    val fixedTimeStepHz: Int = 60,
    val velocityIterations: Int = 8,
    val positionIterations: Int = 3
) {
    // Top-down 2D physics world with zero gravity in the horizontal plane
    val world: World = World(Vec2(0f, 0f))

    val aeroModel: AeroDragModel2D = AeroDragModel2D()
    val tireModel: TireFrictionModel2D = TireFrictionModel2D()
    val vehicleBody: VehicleBody2D = VehicleBody2D(
        world = world,
        aeroModel = aeroModel,
        tireModel = tireModel
    )

    private var stepCount: Long = 0L
    private val fixedDt: Float = 1f / fixedTimeStepHz.toFloat()
    private var accumulator: Float = 0f

    // Cached latest telemetry
    var latestTelemetry: VehicleSimulation2DTelemetry = VehicleSimulation2DTelemetry(
        bodyState = VehicleBody2DState(),
        aeroTelemetry = AeroDragTelemetry2D(),
        tireTelemetry = VehicleTireTelemetry2D(
            wheels = emptyMap(),
            totalCorneringForceN = 0f,
            totalTractionForceN = 0f,
            totalBrakingForceN = 0f,
            totalNormalLoadN = 0f,
            averageFrictionCircleUsagePct = 0f,
            frontAxleGripPct = 1f,
            rearAxleGripPct = 1f,
            isDrifting = false,
            surfaceFrictionCoefficient = 1f
        )
    )
        private set

    /**
     * Reconfigures vehicle parameters according to selected game vehicle specs.
     */
    fun configureVehicle(vehicle: Vehicle) {
        vehicleBody.configureFromVehicle(vehicle)
    }

    /**
     * Resets the 2D simulation to initial coordinates and heading.
     */
    fun reset(xM: Float, yM: Float, headingDeg: Float) {
        vehicleBody.reset(xM, yM, headingDeg)
        accumulator = 0f
        stepCount = 0L
    }

    /**
     * Advances the 2D physics simulation by dt seconds with sub-stepping for numerical stability.
     */
    fun step(
        dt: Float,
        throttleInput: Float,
        brakeInput: Float,
        steerInput: Float,
        handbrake: Boolean = false,
        boost: Boolean = false,
        surfaceGrip: Float = 1.0f,
        weather: WeatherType = WeatherType.SUNNY_CLEAR
    ): VehicleSimulation2DTelemetry {
        accumulator += dt.coerceIn(0.001f, 0.1f)

        // Sub-step loop with fixed time step
        var currentBodyState = latestTelemetry.bodyState
        var currentAero = latestTelemetry.aeroTelemetry
        var currentTire = latestTelemetry.tireTelemetry

        while (accumulator >= fixedDt) {
            // 1. Update vehicle forces (aero drag + tire friction) and apply them to Box2D body
            val (bState, aTelem, tTelem) = vehicleBody.update(
                throttleInput = throttleInput,
                brakeInput = brakeInput,
                steerInput = steerInput,
                handbrake = handbrake,
                boost = boost,
                surfaceGrip = surfaceGrip,
                weather = weather,
                dt = fixedDt
            )

            // 2. Step the JBox2D solver for rigid body constraints, velocity, and position
            world.step(fixedDt, velocityIterations, positionIterations)

            currentBodyState = bState
            currentAero = aTelem
            currentTire = tTelem
            accumulator -= fixedDt
            stepCount++
        }

        latestTelemetry = VehicleSimulation2DTelemetry(
            bodyState = currentBodyState,
            aeroTelemetry = currentAero,
            tireTelemetry = currentTire,
            simulationStepCount = stepCount,
            fixedDtSec = fixedDt
        )

        return latestTelemetry
    }

    /**
     * Syncs the 2D physics simulation state into the game's CarPhysicsState.
     */
    fun syncToCarPhysicsState(state: CarPhysicsState) {
        val body = latestTelemetry.bodyState
        val aero = latestTelemetry.aeroTelemetry
        val tire = latestTelemetry.tireTelemetry

        state.x = body.xM
        state.y = body.yM
        state.vx = body.vxMps
        state.vy = body.vyMps
        state.speedKmh = body.speedKmh
        state.headingDeg = body.headingDeg
        state.angularVelocity = body.angularVelocityDegPerSec
        state.longitudinalG = body.longitudinalG
        state.lateralG = body.lateralG
        state.slipAngleDeg = body.slipAngleDeg

        // Aerodynamic Drag & Downforce
        state.frictionDragForceN = aero.dragForceN
        state.downforceN = aero.downforceN

        // Tire Friction & Grip
        state.corneringForceN = tire.totalCorneringForceN
        state.tractionForceN = tire.totalTractionForceN
        state.frictionRollingResistanceN = tire.totalBrakingForceN
        state.frictionTotalOpposingN = aero.dragForceN + tire.totalBrakingForceN
        state.frictionCircleUtilizationPct = tire.averageFrictionCircleUsagePct
        state.frontTireGripPct = tire.frontAxleGripPct
        state.rearTireGripPct = tire.rearAxleGripPct
        state.effectiveTireGrip = (1f - tire.averageFrictionCircleUsagePct / 150f).coerceIn(0.1f, 1.0f)
        state.isDrifting = tire.isDrifting
    }

    /**
     * Initializes simulation state from an existing CarPhysicsState.
     */
    fun syncFromCarPhysicsState(state: CarPhysicsState) {
        reset(state.x, state.y, state.headingDeg)
        val headingRad = (state.headingDeg * PI / 180f).toFloat()
        val speedMps = state.speedKmh / 3.6f
        val vx = speedMps * cos(headingRad)
        val vy = speedMps * sin(headingRad)
        vehicleBody.body.setLinearVelocity(Vec2(vx, vy))
        vehicleBody.body.setAngularVelocity((state.angularVelocity * PI / 180f).toFloat())
    }
}
