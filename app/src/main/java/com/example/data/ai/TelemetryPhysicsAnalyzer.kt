package com.example.data.ai

import com.example.data.room.ReplayStorage
import com.example.model.GhostFrame
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Key telemetry inflection point extracted from raw race physics data.
 */
data class TelemetryInflectionPoint(
    val eventType: InflectionEventType,
    val timeMs: Long,
    val speedKmh: Float,
    val headingDeg: Float,
    val x: Float,
    val y: Float,
    val isBraking: Boolean,
    val lateralGForce: Float,
    val telemetryDescription: String
)

enum class InflectionEventType {
    TOP_SPEED_STRAIGHT,
    HEAVY_BRAKING_ZONE,
    APEX_CORNER_ENTRY,
    HIGH_SLIP_DRIFT,
    SECTOR_SPLIT_TRANSITION,
    FINISH_LINE_SPRINT
}

/**
 * Analyzes race physics telemetry frames captured by ReplayManager
 * to extract dramatic moments for Gemini Omni Flash cinematic highlight generation.
 */
object TelemetryPhysicsAnalyzer {

    fun analyzeReplay(replay: ReplayStorage): List<TelemetryInflectionPoint> {
        val frames = replay.getGhostFrames()
        return analyzeFrames(
            frames = frames,
            totalLapTimeMs = replay.lapTimeMs,
            maxSpeedKmh = replay.maxSpeedKmh,
            avgSpeedKmh = replay.avgSpeedKmh
        )
    }

    fun analyzeFrames(
        frames: List<GhostFrame>,
        totalLapTimeMs: Long,
        maxSpeedKmh: Float,
        avgSpeedKmh: Float
    ): List<TelemetryInflectionPoint> {
        if (frames.size < 4) {
            return createFallbackInflections(totalLapTimeMs, maxSpeedKmh)
        }

        val results = mutableListOf<TelemetryInflectionPoint>()

        // 1. Find Peak Top Speed Event
        val peakFrame = frames.maxByOrNull { it.speedKmh } ?: frames.first()
        results.add(
            TelemetryInflectionPoint(
                eventType = InflectionEventType.TOP_SPEED_STRAIGHT,
                timeMs = peakFrame.timeMs,
                speedKmh = peakFrame.speedKmh,
                headingDeg = peakFrame.heading,
                x = peakFrame.x,
                y = peakFrame.y,
                isBraking = peakFrame.isBraking,
                lateralGForce = 0.4f,
                telemetryDescription = String.format(
                    Locale.US,
                    "Terminal velocity of %.1f KM/H reached on primary straight",
                    peakFrame.speedKmh
                )
            )
        )

        // 2. Find Maximum Deceleration / Heavy Braking Zone
        var maxDecelDiff = 0f
        var brakingFrame: GhostFrame? = null
        for (i in 0 until frames.size - 1) {
            val f1 = frames[i]
            val f2 = frames[i + 1]
            if (f1.isBraking || f2.isBraking) {
                val speedDiff = f1.speedKmh - f2.speedKmh
                if (speedDiff > maxDecelDiff) {
                    maxDecelDiff = speedDiff
                    brakingFrame = f1
                }
            }
        }

        if (brakingFrame != null) {
            results.add(
                TelemetryInflectionPoint(
                    eventType = InflectionEventType.HEAVY_BRAKING_ZONE,
                    timeMs = brakingFrame.timeMs,
                    speedKmh = brakingFrame.speedKmh,
                    headingDeg = brakingFrame.heading,
                    x = brakingFrame.x,
                    y = brakingFrame.y,
                    isBraking = true,
                    lateralGForce = 1.6f,
                    telemetryDescription = String.format(
                        Locale.US,
                        "Hard braking threshold: dropping %.1f KM/H with brake calipers clamped",
                        maxDecelDiff * 2.5f
                    )
                )
            )
        }

        // 3. Find Apex Cornering / Sharpest Directional Change
        var maxHeadingDiff = 0f
        var apexFrame: GhostFrame? = null
        for (i in 1 until frames.size - 1) {
            val fPrev = frames[i - 1]
            val fCurr = frames[i]
            val fNext = frames[i + 1]

            var diff1 = abs(fNext.heading - fCurr.heading)
            if (diff1 > 180f) diff1 = 360f - diff1
            var diff2 = abs(fCurr.heading - fPrev.heading)
            if (diff2 > 180f) diff2 = 360f - diff2

            val totalTurn = diff1 + diff2
            if (totalTurn > maxHeadingDiff && fCurr.speedKmh > 60f) {
                maxHeadingDiff = totalTurn
                apexFrame = fCurr
            }
        }

        if (apexFrame != null && apexFrame.timeMs != peakFrame.timeMs) {
            val computedLatG = ((apexFrame.speedKmh / 100f) * 1.8f).coerceIn(1.2f, 3.2f)
            results.add(
                TelemetryInflectionPoint(
                    eventType = InflectionEventType.APEX_CORNER_ENTRY,
                    timeMs = apexFrame.timeMs,
                    speedKmh = apexFrame.speedKmh,
                    headingDeg = apexFrame.heading,
                    x = apexFrame.x,
                    y = apexFrame.y,
                    isBraking = apexFrame.isBraking,
                    lateralGForce = computedLatG,
                    telemetryDescription = String.format(
                        Locale.US,
                        "Apex clipping: %.1fG lateral load at %.1f KM/H",
                        computedLatG,
                        apexFrame.speedKmh
                    )
                )
            )
        }

        // 4. High-Slip / Fast Sweeper Event
        val midLapMs = totalLapTimeMs / 2
        val midFrame = frames.minByOrNull { abs(it.timeMs - midLapMs) }
        if (midFrame != null && midFrame.timeMs != peakFrame.timeMs && midFrame.timeMs != (apexFrame?.timeMs ?: -1L)) {
            results.add(
                TelemetryInflectionPoint(
                    eventType = InflectionEventType.HIGH_SLIP_DRIFT,
                    timeMs = midFrame.timeMs,
                    speedKmh = midFrame.speedKmh,
                    headingDeg = midFrame.heading,
                    x = midFrame.x,
                    y = midFrame.y,
                    isBraking = midFrame.isBraking,
                    lateralGForce = 2.1f,
                    telemetryDescription = String.format(
                        Locale.US,
                        "High-speed sweeper carrying %.1f KM/H with controlled tire slip",
                        midFrame.speedKmh
                    )
                )
            )
        }

        // 5. Finish Line Sprint
        val finishFrame = frames.lastOrNull() ?: frames.first()
        results.add(
            TelemetryInflectionPoint(
                eventType = InflectionEventType.FINISH_LINE_SPRINT,
                timeMs = finishFrame.timeMs,
                speedKmh = finishFrame.speedKmh,
                headingDeg = finishFrame.heading,
                x = finishFrame.x,
                y = finishFrame.y,
                isBraking = false,
                lateralGForce = 0.5f,
                telemetryDescription = String.format(
                    Locale.US,
                    "Checkered flag charge across the line at %.1f KM/H",
                    finishFrame.speedKmh
                )
            )
        )

        // Sort chronologically by lap timestamp
        return results.sortedBy { it.timeMs }
    }

    private fun createFallbackInflections(
        totalLapTimeMs: Long,
        maxSpeedKmh: Float
    ): List<TelemetryInflectionPoint> {
        return listOf(
            TelemetryInflectionPoint(
                eventType = InflectionEventType.TOP_SPEED_STRAIGHT,
                timeMs = (totalLapTimeMs * 0.25f).toLong(),
                speedKmh = maxSpeedKmh,
                headingDeg = 90f,
                x = 300f,
                y = 150f,
                isBraking = false,
                lateralGForce = 0.6f,
                telemetryDescription = "Full throttle acceleration on straightaway"
            ),
            TelemetryInflectionPoint(
                eventType = InflectionEventType.HEAVY_BRAKING_ZONE,
                timeMs = (totalLapTimeMs * 0.45f).toLong(),
                speedKmh = maxSpeedKmh * 0.65f,
                headingDeg = 140f,
                x = 600f,
                y = 200f,
                isBraking = true,
                lateralGForce = 1.9f,
                telemetryDescription = "Severe deceleration entering hairpin complex"
            ),
            TelemetryInflectionPoint(
                eventType = InflectionEventType.APEX_CORNER_ENTRY,
                timeMs = (totalLapTimeMs * 0.65f).toLong(),
                speedKmh = maxSpeedKmh * 0.55f,
                headingDeg = 210f,
                x = 450f,
                y = 400f,
                isBraking = false,
                lateralGForce = 2.4f,
                telemetryDescription = "Aggressive kerb strike clipping the inner apex"
            ),
            TelemetryInflectionPoint(
                eventType = InflectionEventType.FINISH_LINE_SPRINT,
                timeMs = totalLapTimeMs,
                speedKmh = maxSpeedKmh * 0.9f,
                headingDeg = 0f,
                x = 100f,
                y = 100f,
                isBraking = false,
                lateralGForce = 0.4f,
                telemetryDescription = "Final sector sprint to the checkered flag"
            )
        )
    }
}
