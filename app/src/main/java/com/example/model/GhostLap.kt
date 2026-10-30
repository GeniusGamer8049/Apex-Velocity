package com.example.model

data class GhostFrame(
    val timeMs: Long,
    val x: Float,
    val y: Float,
    val heading: Float,
    val speedKmh: Float,
    val isBraking: Boolean = false
)

data class GhostLap(
    val trackId: String,
    val carName: String,
    val lapTimeMs: Long,
    val frames: List<GhostFrame> = emptyList()
) {
    /**
     * Interpolates the ghost car's position and orientation at a given lap elapsed time.
     */
    fun sampleAt(currentLapTimeMs: Long): GhostFrame? {
        if (frames.isEmpty()) return null
        if (currentLapTimeMs <= frames.first().timeMs) return frames.first()
        if (currentLapTimeMs >= frames.last().timeMs) return frames.last()

        // Binary search for efficiency
        var low = 0
        var high = frames.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val f = frames[mid]
            if (f.timeMs < currentLapTimeMs) {
                low = mid + 1
            } else if (f.timeMs > currentLapTimeMs) {
                high = mid - 1
            } else {
                return f
            }
        }

        val idx1 = (low - 1).coerceAtLeast(0)
        val idx2 = low.coerceAtMost(frames.size - 1)
        if (idx1 == idx2) return frames[idx1]

        val f1 = frames[idx1]
        val f2 = frames[idx2]
        val duration = (f2.timeMs - f1.timeMs).coerceAtLeast(1)
        val t = (currentLapTimeMs - f1.timeMs).toFloat() / duration.toFloat()

        val interX = f1.x + (f2.x - f1.x) * t
        val interY = f1.y + (f2.y - f1.y) * t
        
        // Handle angle wrap-around for heading
        var dAngle = (f2.heading - f1.heading)
        while (dAngle > 180f) dAngle -= 360f
        while (dAngle < -180f) dAngle += 360f
        val interHeading = f1.heading + dAngle * t
        val interSpeed = f1.speedKmh + (f2.speedKmh - f1.speedKmh) * t

        return GhostFrame(
            timeMs = currentLapTimeMs,
            x = interX,
            y = interY,
            heading = interHeading,
            speedKmh = interSpeed,
            isBraking = if (t > 0.5f) f2.isBraking else f1.isBraking
        )
    }
}
