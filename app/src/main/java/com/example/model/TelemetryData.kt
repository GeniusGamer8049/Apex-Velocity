package com.example.model

data class TelemetryPoint(
    val timeSec: Float,
    val speedKmh: Float,
    val rpm: Float,
    val throttle: Float,
    val brake: Float,
    val lateralG: Float,
    val longitudinalG: Float,
    val tempFL: Float,
    val tempFR: Float,
    val tempRL: Float,
    val tempRR: Float,
    val gear: Int,
    val downforceN: Float
) {
    val avgTireTemp: Float
        get() = (tempFL + tempFR + tempRL + tempRR) / 4f
}
