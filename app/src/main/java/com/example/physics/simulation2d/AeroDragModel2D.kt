package com.example.physics.simulation2d

import com.example.model.WeatherType
import org.jbox2d.common.Vec2
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Aerodynamic drag and downforce telemetry snapshot.
 */
data class AeroDragTelemetry2D(
    val dragForceN: Float = 0f,
    val dragForceX: Float = 0f,
    val dragForceY: Float = 0f,
    val downforceN: Float = 0f,
    val frontDownforceN: Float = 0f,
    val rearDownforceN: Float = 0f,
    val yawDampingTorqueNm: Float = 0f,
    val dragPowerKw: Float = 0f,
    val airDensityKgM3: Float = 1.225f,
    val dragCoefficient: Float = 0.32f,
    val frontalAreaM2: Float = 2.1f,
    val downforceCoefficient: Float = 0.45f
) {
    val formattedDragForce: String
        get() = String.format(Locale.US, "%.0f N", dragForceN)

    val formattedDownforce: String
        get() = String.format(Locale.US, "%.0f N", downforceN)

    val formattedDragPower: String
        get() = String.format(Locale.US, "%.1f kW", dragPowerKw)
}

/**
 * Aerodynamic simulation model for 2D vehicle dynamics.
 * Models:
 * 1. Aerodynamic Drag: F_drag = 0.5 * rho * Cd * A * v^2 opposing the vehicle velocity vector.
 * 2. Aerodynamic Downforce: F_down = 0.5 * rho * Cl * A * v^2 pressing the vehicle onto the track.
 * 3. Aerodynamic Yaw Damping: Tau_aero = -0.5 * rho * Cyaw * omega * |omega| stabilizing rotation at speed.
 */
class AeroDragModel2D(
    var dragCoefficientCd: Float = 0.32f,       // Drag coefficient (0.28 - 0.45)
    var frontalAreaM2: Float = 2.1f,            // Frontal cross-sectional area in m^2
    var downforceCoefficientCl: Float = 0.45f,   // Downforce / lift coefficient (0.2 - 0.9)
    var aeroBalanceFrontPct: Float = 0.42f,      // Aero balance (42% front, 58% rear)
    var yawDampingCoefficient: Float = 0.15f     // Yaw stabilizing torque factor
) {
    // Standard air density at sea level (15°C) in kg/m^3
    val standardAirDensity = 1.225f

    /**
     * Calculates air density based on weather type and ambient temperature.
     */
    fun calculateAirDensity(weather: WeatherType, ambientTempC: Float = 20f): Float {
        // Ideal gas law approximation: rho = rho0 * (T0 / T) with humidity adjustment
        val tempK = (ambientTempC + 273.15f).coerceAtLeast(250f)
        val tempFactor = 288.15f / tempK
        val humidityFactor = when (weather) {
            WeatherType.SUNNY_CLEAR -> 1.0f
            WeatherType.OVERCAST -> 1.01f
            WeatherType.LIGHT_RAIN -> 1.03f
            WeatherType.THUNDERSTORM -> 1.05f
            WeatherType.FREEZING_ICE -> 1.08f
            WeatherType.FOG_MIST -> 1.04f
        }
        return standardAirDensity * tempFactor * humidityFactor
    }

    /**
     * Calculates the aerodynamic opposing drag force vector, downforce, and stabilizing moments.
     *
     * @param velocity World velocity vector in m/s (from Box2D body)
     * @param angularVelocityRadPerSec Yaw rate in rad/s
     * @param weather Current weather type
     * @return Aerodynamic force vector in Newtons (to apply to Box2D body) and telemetry snapshot
     */
    fun calculateAeroForces(
        velocity: Vec2,
        angularVelocityRadPerSec: Float,
        weather: WeatherType = WeatherType.SUNNY_CLEAR
    ): Pair<Vec2, AeroDragTelemetry2D> {
        val speedSq = velocity.lengthSquared()
        val speed = sqrt(speedSq)
        val airDensity = calculateAirDensity(weather)

        if (speed < 0.05f) {
            return Pair(
                Vec2(0f, 0f),
                AeroDragTelemetry2D(
                    airDensityKgM3 = airDensity,
                    dragCoefficient = dragCoefficientCd,
                    frontalAreaM2 = frontalAreaM2,
                    downforceCoefficient = downforceCoefficientCl
                )
            )
        }

        // F_drag = 0.5 * rho * Cd * A * v^2
        val dynamicPressure = 0.5f * airDensity * speedSq
        val dragMagnitude = dynamicPressure * dragCoefficientCd * frontalAreaM2

        // Drag force vector acts directly opposite to velocity direction: F_drag = -dragMagnitude * (v / |v|)
        val invSpeed = 1f / speed
        val dragForceX = -dragMagnitude * (velocity.x * invSpeed)
        val dragForceY = -dragMagnitude * (velocity.y * invSpeed)
        val dragForceVec = Vec2(dragForceX, dragForceY)

        // F_downforce = 0.5 * rho * Cl * A * v^2
        val totalDownforce = dynamicPressure * downforceCoefficientCl * frontalAreaM2
        val frontDownforce = totalDownforce * aeroBalanceFrontPct
        val rearDownforce = totalDownforce * (1f - aeroBalanceFrontPct)

        // Aerodynamic yaw damping torque: Tau = -0.5 * rho * Cyaw * omega * |omega|
        val yawTorque = -0.5f * airDensity * yawDampingCoefficient * frontalAreaM2 *
                angularVelocityRadPerSec * abs(angularVelocityRadPerSec)

        // Aerodynamic power dissipated by drag: P = F_drag * v (Watts -> kW)
        val dragPowerKw = (dragMagnitude * speed) / 1000f

        val telemetry = AeroDragTelemetry2D(
            dragForceN = dragMagnitude,
            dragForceX = dragForceX,
            dragForceY = dragForceY,
            downforceN = totalDownforce,
            frontDownforceN = frontDownforce,
            rearDownforceN = rearDownforce,
            yawDampingTorqueNm = yawTorque,
            dragPowerKw = dragPowerKw,
            airDensityKgM3 = airDensity,
            dragCoefficient = dragCoefficientCd,
            frontalAreaM2 = frontalAreaM2,
            downforceCoefficient = downforceCoefficientCl
        )

        return Pair(dragForceVec, telemetry)
    }
}
