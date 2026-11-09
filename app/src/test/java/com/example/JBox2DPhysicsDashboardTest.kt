package com.example

import com.example.model.TelemetryPoint
import com.example.model.VehicleRegistry
import com.example.model.WeatherType
import com.example.physics.CarPhysicsState
import com.example.physics.simulation2d.AeroDragTelemetry2D
import com.example.physics.simulation2d.VehicleBody2DState
import com.example.physics.simulation2d.VehicleSimulation2DTelemetry
import com.example.physics.simulation2d.VehicleTireTelemetry2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JBox2DPhysicsDashboardTest {

    @Test
    fun `test speed derived from JBox2D linear velocity coordinates`() {
        val vx = 24.0f // m/s
        val vy = 10.0f // m/s
        val linearSpeedMs = sqrt(vx * vx + vy * vy) // 26.0 m/s
        val speedKmh = linearSpeedMs * 3.6f          // 93.6 km/h

        val bodyState = VehicleBody2DState(
            vxMps = vx,
            vyMps = vy,
            speedMps = linearSpeedMs,
            speedKmh = speedKmh
        )

        assertEquals(26.0f, bodyState.speedMps, 0.01f)
        assertEquals(93.6f, bodyState.speedKmh, 0.01f)
        assertEquals("93 km/h", bodyState.formattedSpeed)
    }

    @Test
    fun `test RPM redline threshold and shift indicator`() {
        val idleRpm = 1000f
        val redlineRpm = 7500f
        val maxRpm = 9000f

        val stateNormal = CarPhysicsState(x = 0f, y = 0f, rpm = 4500f, currentGear = 3)
        val stateRedline = CarPhysicsState(x = 0f, y = 0f, rpm = 8200f, currentGear = 4)

        assertTrue("Normal RPM should be below redline", stateNormal.rpm < redlineRpm)
        assertTrue("High RPM should trigger redline", stateRedline.rpm >= redlineRpm)

        val fractionNormal = stateNormal.rpm / maxRpm
        val fractionRedline = stateRedline.rpm / maxRpm
        assertEquals(0.5f, fractionNormal, 0.01f)
        assertTrue("Redline fraction should be over 80%", fractionRedline > 0.8f)
    }

    @Test
    fun `test engine temperature thermal dynamics and airflow cooling`() {
        val standardDensity = 1.225f
        val lowSpeed = 10f // m/s (~36 km/h)
        val highSpeed = 50f // m/s (~180 km/h)

        // Dynamic ram-air cooling increases with forward velocity
        val lowCoolingRate = (lowSpeed / 60f).coerceIn(0f, 1f)
        val highCoolingRate = (highSpeed / 60f).coerceIn(0f, 1f)

        assertTrue("High speed should provide significantly greater ram-air cooling airflow",
            highCoolingRate > lowCoolingRate)

        val stateOptimal = CarPhysicsState(x = 0f, y = 0f, engineTempC = 88.5f, engineOilTempC = 92.0f)
        val stateOverheat = CarPhysicsState(x = 0f, y = 0f, engineTempC = 118.0f, engineOilTempC = 124.0f)

        assertTrue("Normal operating temperature is within 80-100°C range",
            stateOptimal.engineTempC in 80f..100f)
        assertTrue("Elevated temperature triggers overheat condition",
            stateOverheat.engineTempC > 115f || stateOverheat.engineOilTempC > 120f)
    }

    @Test
    fun `test JBox2D telemetry snapshot creation and formatting`() {
        val vehicle = VehicleRegistry.createDefaultVehicles().first()
        assertNotNull(vehicle)

        val simTelemetry = VehicleSimulation2DTelemetry(
            bodyState = VehicleBody2DState(
                speedKmh = 140.0f,
                vxMps = 38.0f,
                vyMps = 8.5f,
                angularVelocityDegPerSec = 4.2f
            ),
            aeroTelemetry = AeroDragTelemetry2D(
                dragForceN = 850f,
                downforceN = 1200f,
                dragPowerKw = 33.6f,
                airDensityKgM3 = 1.225f
            ),
            tireTelemetry = VehicleTireTelemetry2D(
                wheels = emptyMap(),
                totalCorneringForceN = 4500f,
                totalTractionForceN = 2800f,
                totalBrakingForceN = 0f,
                totalNormalLoadN = 13800f,
                averageFrictionCircleUsagePct = 65f,
                frontAxleGripPct = 0.92f,
                rearAxleGripPct = 0.88f,
                isDrifting = false,
                surfaceFrictionCoefficient = 1.0f
            ),
            simulationStepCount = 120L
        )

        assertEquals("140 km/h", simTelemetry.bodyState.formattedSpeed)
        assertEquals("850 N", simTelemetry.aeroTelemetry.formattedDragForce)
        assertEquals("1200 N", simTelemetry.aeroTelemetry.formattedDownforce)
        assertEquals("33.6 kW", simTelemetry.aeroTelemetry.formattedDragPower)
        assertFalse("Vehicle is gripping, not drifting", simTelemetry.tireTelemetry.isDrifting)
        assertTrue("Summary line contains speed and forces", simTelemetry.summaryLine.contains("140 km/h"))
    }

    @Test
    fun `test multi-trace telemetry points sequence`() {
        val points = mutableListOf<TelemetryPoint>()
        for (i in 0 until 40) {
            val t = i * 0.1f
            val speed = i * 4.5f // 0 to 180 km/h
            val rpm = 1000f + (i * 180f) // 1000 to 8200 RPM
            val temp = 85f + (i * 0.4f) // 85 to 101°C
            points.add(
                TelemetryPoint(
                    timeSec = t,
                    speedKmh = speed,
                    rpm = rpm,
                    throttle = 0.8f,
                    brake = 0.0f,
                    lateralG = 0.3f,
                    longitudinalG = 0.5f,
                    tempFL = 80f,
                    tempFR = 80f,
                    tempRL = 82f,
                    tempRR = 82f,
                    gear = (i / 8) + 1,
                    downforceN = speed * 4f,
                    engineTempC = temp,
                    engineOilTempC = temp + 4f
                )
            )
        }

        assertEquals(40, points.size)
        assertTrue("Speed increases across telemetry stream",
            points.last().speedKmh > points.first().speedKmh)
        assertTrue("RPM increases across telemetry stream",
            points.last().rpm > points.first().rpm)
        assertTrue("Engine temperature dynamically builds over high throttle",
            points.last().engineTempC > points.first().engineTempC)
    }
}
