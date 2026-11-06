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
    val downforceN: Float,
    // Real-time Physics Integration Variables
    val frictionDragN: Float = 0f,
    val frictionRollingN: Float = 0f,
    val effectiveGripPct: Float = 1.0f,
    val slipAngleDeg: Float = 0f,
    val slipRatio: Float = 0f,
    val frictionCirclePct: Float = 0f,
    val vLongMs: Float = 0f,
    val vLatMs: Float = 0f
) {
    val avgTireTemp: Float
        get() = (tempFL + tempFR + tempRL + tempRR) / 4f

    val totalFrictionN: Float
        get() = frictionDragN + frictionRollingN
}
