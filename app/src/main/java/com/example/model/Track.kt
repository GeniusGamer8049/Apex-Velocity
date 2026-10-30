package com.example.model

data class TrackWaypoint(
    val x: Float,
    val y: Float,
    val width: Float = 140f,
    val isApexKerb: Boolean = false,
    val kerbSide: Int = 0, // -1 = inner left, 1 = inner right
    val targetSpeedKmh: Float = 120f, // For AI racing line
    val isCheckpoint: Boolean = false,
    val isStartFinish: Boolean = false
)

data class Track(
    val id: String,
    val name: String,
    val location: String,
    val lengthMeters: Int,
    val turnsCount: Int,
    val defaultWeather: WeatherType,
    val dynamicWeatherProgression: List<Pair<Int, WeatherType>>, // time in seconds -> new weather
    val surfacePatches: List<SurfacePatch>,
    val waypoints: List<TrackWaypoint>,
    val targetLapTimeMs: Long,
    val description: String,
    val unlockReputation: Int = 0
) {
    // Computes bounding box for camera clipping and mini-map
    val minX: Float = waypoints.minOfOrNull { it.x } ?: 0f
    val maxX: Float = waypoints.maxOfOrNull { it.x } ?: 1000f
    val minY: Float = waypoints.minOfOrNull { it.y } ?: 0f
    val maxY: Float = waypoints.maxOfOrNull { it.y } ?: 1000f

    val startWaypoint: TrackWaypoint
        get() = waypoints.firstOrNull { it.isStartFinish } ?: waypoints.first()

    /**
     * Finds nearest waypoint index for distance / progress calculation.
     */
    fun findNearestWaypointIndex(px: Float, py: Float): Int {
        var bestIdx = 0
        var bestDistSq = Float.MAX_VALUE
        for (i in waypoints.indices) {
            val wp = waypoints[i]
            val dx = wp.x - px
            val dy = wp.y - py
            val d = dx * dx + dy * dy
            if (d < bestDistSq) {
                bestDistSq = d
                bestIdx = i
            }
        }
        return bestIdx
    }
}
