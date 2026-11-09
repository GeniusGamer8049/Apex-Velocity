package com.example

import com.example.model.DriveTrain
import com.example.model.TireCompound
import com.example.model.VehicleRegistry
import com.example.model.WeatherType
import com.example.physics.simulation2d.AeroDragModel2D
import com.example.physics.simulation2d.TireFrictionModel2D
import com.example.physics.simulation2d.VehicleSimulation2D
import com.example.physics.simulation2d.WheelId2D
import org.jbox2d.common.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VehiclePhysicsSimulation2DTest {

    @Test
    fun `test aerodynamic drag scales quadratically with speed`() {
        val aeroModel = AeroDragModel2D(
            dragCoefficientCd = 0.32f,
            frontalAreaM2 = 2.0f
        )

        // At 10 m/s (~36 km/h)
        val (force10, telem10) = aeroModel.calculateAeroForces(
            velocity = Vec2(10f, 0f),
            angularVelocityRadPerSec = 0f,
            weather = WeatherType.SUNNY_CLEAR
        )

        // At 20 m/s (~72 km/h) -> velocity doubled
        val (force20, telem20) = aeroModel.calculateAeroForces(
            velocity = Vec2(20f, 0f),
            angularVelocityRadPerSec = 0f,
            weather = WeatherType.SUNNY_CLEAR
        )

        assertTrue("Drag at 10 m/s must be positive", telem10.dragForceN > 0f)
        assertTrue("Drag at 20 m/s must exceed drag at 10 m/s", telem20.dragForceN > telem10.dragForceN)

        // Quadratic scaling ratio: (20^2) / (10^2) = 4.0
        val ratio = telem20.dragForceN / telem10.dragForceN
        assertTrue("Drag must scale quadratically (ratio ~4.0, actual: $ratio)", abs(ratio - 4.0f) < 0.1f)

        // Drag opposes velocity direction: v = (20, 0) -> F_drag.x < 0
        assertTrue("Drag force must oppose velocity direction", force20.x < 0f)
        assertEquals(0f, force20.y, 0.001f)
    }

    @Test
    fun `test aerodynamic downforce and weather density effect`() {
        val aeroModel = AeroDragModel2D()

        val (sunnyForces, sunnyTelem) = aeroModel.calculateAeroForces(
            velocity = Vec2(30f, 0f),
            angularVelocityRadPerSec = 0f,
            weather = WeatherType.SUNNY_CLEAR
        )

        val (freezingForces, freezingTelem) = aeroModel.calculateAeroForces(
            velocity = Vec2(30f, 0f),
            angularVelocityRadPerSec = 0f,
            weather = WeatherType.FREEZING_ICE
        )

        assertTrue("Downforce must be generated at speed", sunnyTelem.downforceN > 100f)
        assertTrue("Front downforce allocated properly", sunnyTelem.frontDownforceN > 0f)
        assertTrue("Rear downforce allocated properly", sunnyTelem.rearDownforceN > 0f)

        // Freezing air is denser than sunny warm air
        assertTrue("Freezing air density must exceed sunny air density",
            freezingTelem.airDensityKgM3 > sunnyTelem.airDensityKgM3)
        assertTrue("Cold dense air increases aero drag",
            freezingTelem.dragForceN > sunnyTelem.dragForceN)
    }

    @Test
    fun `test dynamic normal load transfer under acceleration and braking`() {
        val tireModel = TireFrictionModel2D(
            massKg = 1400f,
            wheelbaseM = 2.6f,
            cogHeightM = 0.5f,
            weightDistributionFrontPct = 0.5f // 50/50 static
        )

        // 1. Static load (zero acceleration)
        val staticLoads = tireModel.calculateNormalLoads(0f, 0f, 0f, 0f)
        val staticFront = staticLoads[WheelId2D.FRONT_LEFT]!! + staticLoads[WheelId2D.FRONT_RIGHT]!!
        val staticRear = staticLoads[WheelId2D.REAR_LEFT]!! + staticLoads[WheelId2D.REAR_RIGHT]!!
        assertEquals(staticFront, staticRear, 10f)

        // 2. Forward acceleration (+5 m/s^2) -> Transfers load to rear axle
        val accelLoads = tireModel.calculateNormalLoads(
            longitudinalAccMs2 = 5f,
            lateralAccMs2 = 0f,
            frontDownforceN = 0f,
            rearDownforceN = 0f
        )
        val accelFront = accelLoads[WheelId2D.FRONT_LEFT]!! + accelLoads[WheelId2D.FRONT_RIGHT]!!
        val accelRear = accelLoads[WheelId2D.REAR_LEFT]!! + accelLoads[WheelId2D.REAR_RIGHT]!!
        assertTrue("Under acceleration, rear load must exceed front load", accelRear > accelFront)

        // 3. Heavy braking (-8 m/s^2) -> Transfers load to front axle
        val brakeLoads = tireModel.calculateNormalLoads(
            longitudinalAccMs2 = -8f,
            lateralAccMs2 = 0f,
            frontDownforceN = 0f,
            rearDownforceN = 0f
        )
        val brakeFront = brakeLoads[WheelId2D.FRONT_LEFT]!! + brakeLoads[WheelId2D.FRONT_RIGHT]!!
        val brakeRear = brakeLoads[WheelId2D.REAR_LEFT]!! + brakeLoads[WheelId2D.REAR_RIGHT]!!
        assertTrue("Under braking, front load must exceed rear load", brakeFront > brakeRear)
    }

    @Test
    fun `test tire friction circle clamping and drift detection`() {
        val tireModel = TireFrictionModel2D(
            massKg = 1300f,
            driveTrain = DriveTrain.RWD,
            tireCompound = TireCompound.SPORT_RADIAL
        )

        val normalLoads = tireModel.calculateNormalLoads(0f, 0f, 0f, 0f)

        // Test normal cornering (mild slip angle, low throttle)
        val normalVelocities = mapOf(
            WheelId2D.FRONT_LEFT to Vec2(15f, 1.2f),
            WheelId2D.FRONT_RIGHT to Vec2(15f, 1.2f),
            WheelId2D.REAR_LEFT to Vec2(15f, 0.4f),
            WheelId2D.REAR_RIGHT to Vec2(15f, 0.4f)
        )

        val (forcesNormal, telemNormal) = tireModel.evaluateTireForces(
            wheelLocalVelocities = normalVelocities,
            steerAngleRad = 0.08f,
            throttleInput = 0.4f,
            brakeInput = 0f,
            handbrake = false,
            boost = false,
            engineDriveForceN = 2500f,
            normalLoads = normalLoads,
            surfaceMu = 1.0f
        )

        assertTrue("Should generate significant cornering force", telemNormal.totalCorneringForceN > 1000f)
        assertFalse("Should not be drifting under normal grip", telemNormal.isDrifting)

        // Test aggressive drift (handbrake pulled with rear lateral slip)
        val driftVelocities = mapOf(
            WheelId2D.FRONT_LEFT to Vec2(18f, 2.0f),
            WheelId2D.FRONT_RIGHT to Vec2(18f, 2.0f),
            WheelId2D.REAR_LEFT to Vec2(18f, 6.5f),
            WheelId2D.REAR_RIGHT to Vec2(18f, 6.5f)
        )

        val (forcesDrift, telemDrift) = tireModel.evaluateTireForces(
            wheelLocalVelocities = driftVelocities,
            steerAngleRad = -0.15f, // Counter-steering
            throttleInput = 0.8f,
            brakeInput = 0f,
            handbrake = true,
            boost = false,
            engineDriveForceN = 4000f,
            normalLoads = normalLoads,
            surfaceMu = 1.0f
        )

        assertTrue("Handbrake + high lateral slip must trigger drift state", telemDrift.isDrifting)
        assertTrue("Rear friction circle usage must be elevated",
            telemDrift.wheels[WheelId2D.REAR_LEFT]!!.frictionCircleUsagePct > 90f)
    }

    @Test
    fun `test 2D vehicle simulation step and rigid-body integration`() {
        val sim = VehicleSimulation2D(fixedTimeStepHz = 60)
        val car = VehicleRegistry.createDefaultVehicles().first()
        sim.configureVehicle(car)

        sim.reset(0f, 0f, 0f)

        // Initially stationary
        assertEquals(0f, sim.latestTelemetry.bodyState.speedKmh, 0.01f)

        // Step for 1 second with full throttle
        for (i in 0 until 60) {
            sim.step(
                dt = 1f / 60f,
                throttleInput = 1.0f,
                brakeInput = 0.0f,
                steerInput = 0.0f,
                handbrake = false,
                boost = false,
                surfaceGrip = 1.0f,
                weather = WeatherType.SUNNY_CLEAR
            )
        }

        val telemAfterThrottle = sim.latestTelemetry
        assertTrue("Vehicle must accelerate forward (X > 0)", telemAfterThrottle.bodyState.xM > 0f)
        assertTrue("Vehicle speed must increase", telemAfterThrottle.bodyState.speedKmh > 10f)
        assertTrue("Aerodynamic drag must oppose motion", telemAfterThrottle.aeroTelemetry.dragForceN > 10f)
        assertTrue("Traction force must be generated", telemAfterThrottle.tireTelemetry.totalTractionForceN > 0f)

        // Step for 0.5s with heavy braking
        val speedBeforeBrake = telemAfterThrottle.bodyState.speedKmh
        for (i in 0 until 30) {
            sim.step(
                dt = 1f / 60f,
                throttleInput = 0.0f,
                brakeInput = 1.0f,
                steerInput = 0.0f,
                handbrake = false,
                boost = false,
                surfaceGrip = 1.0f,
                weather = WeatherType.SUNNY_CLEAR
            )
        }

        val telemAfterBrake = sim.latestTelemetry
        assertTrue("Speed must decrease after braking", telemAfterBrake.bodyState.speedKmh < speedBeforeBrake)
        assertTrue("Braking force must be logged", telemAfterBrake.tireTelemetry.totalBrakingForceN > 0f)
    }

    @Test
    fun `test 2D vehicle steering and angular velocity dynamics`() {
        val sim = VehicleSimulation2D(fixedTimeStepHz = 60)
        sim.reset(0f, 0f, 0f)

        // Accelerate with steer input to left
        for (i in 0 until 40) {
            sim.step(
                dt = 1f / 60f,
                throttleInput = 0.8f,
                brakeInput = 0.0f,
                steerInput = 0.6f, // Turn left
                handbrake = false,
                boost = false,
                surfaceGrip = 1.0f,
                weather = WeatherType.SUNNY_CLEAR
            )
        }

        val telem = sim.latestTelemetry
        assertTrue("Steering angle must be non-zero", abs(telem.bodyState.currentSteerAngleDeg) > 1f)
        assertTrue("Angular velocity should reflect cornering", abs(telem.bodyState.angularVelocityRadPerSec) > 0.05f)
        assertTrue("Cornering forces generated across axles", telem.tireTelemetry.totalCorneringForceN > 100f)
    }
}
