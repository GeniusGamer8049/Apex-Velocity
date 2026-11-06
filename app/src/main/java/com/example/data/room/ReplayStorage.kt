package com.example.data.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.GhostFrame
import com.example.model.GhostLap
import com.example.model.TrackRegistry
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Room entity storing race telemetry recordings, ghost car coordinate paths,
 * and lap telemetry data for replay playback.
 */
@Entity(
    tableName = "replay_storage",
    indices = [
        Index("trackId"),
        Index("lapTimeMs"),
        Index(value = ["trackId", "lapTimeMs"])
    ]
)
data class ReplayStorage(
    @PrimaryKey val id: String,
    val trackId: String,
    val trackName: String,
    val vehicleId: String,
    val vehicleName: String,
    val lapTimeMs: Long,
    val maxSpeedKmh: Float,
    val avgSpeedKmh: Float,
    val cleanLap: Boolean = true,
    val weatherType: String = "SUNNY_CLEAR",
    val dateRecorded: String = "2026-11-06",
    val timestamp: Long = System.currentTimeMillis(),
    val frameCount: Int = 0,
    val telemetryDataJson: String = "[]"
) {
    val formattedLapTime: String
        @Ignore get() {
            val totalSec = lapTimeMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val ms = lapTimeMs % 1000
            return String.format(Locale.US, "%d:%02d.%03d", min, sec, ms)
        }

    /**
     * Parses stored JSON coordinates into a list of [GhostFrame]s.
     */
    fun getGhostFrames(): List<GhostFrame> {
        if (telemetryDataJson.isBlank() || telemetryDataJson == "[]") return emptyList()
        val list = mutableListOf<GhostFrame>()
        try {
            val arr = JSONArray(telemetryDataJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    GhostFrame(
                        timeMs = obj.optLong("t", 0L),
                        x = obj.optDouble("x", 0.0).toFloat(),
                        y = obj.optDouble("y", 0.0).toFloat(),
                        heading = obj.optDouble("h", 0.0).toFloat(),
                        speedKmh = obj.optDouble("s", 0.0).toFloat(),
                        isBraking = obj.optBoolean("b", false)
                    )
                )
            }
        } catch (ignored: Exception) {}
        return list
    }

    /**
     * Converts this stored replay into a playable [GhostLap].
     */
    fun toGhostLap(): GhostLap {
        return GhostLap(
            trackId = trackId,
            carName = vehicleName,
            lapTimeMs = lapTimeMs,
            frames = getGhostFrames()
        )
    }

    companion object {
        /**
         * Serializes a list of [GhostFrame]s into compact JSON coordinates storage.
         */
        fun serializeFrames(frames: List<GhostFrame>): String {
            val arr = JSONArray()
            for (f in frames) {
                val obj = JSONObject()
                obj.put("t", f.timeMs)
                obj.put("x", String.format(Locale.US, "%.1f", f.x).toDouble())
                obj.put("y", String.format(Locale.US, "%.1f", f.y).toDouble())
                obj.put("h", String.format(Locale.US, "%.1f", f.heading).toDouble())
                obj.put("s", String.format(Locale.US, "%.1f", f.speedKmh).toDouble())
                if (f.isBraking) obj.put("b", true)
                arr.put(obj)
            }
            return arr.toString()
        }

        /**
         * Creates a [ReplayStorage] record from a [GhostLap].
         */
        fun fromGhostLap(
            ghostLap: GhostLap,
            trackName: String,
            vehicleId: String = "apex_gt86",
            weatherType: String = "SUNNY_CLEAR",
            cleanLap: Boolean = true
        ): ReplayStorage {
            val maxSpd = ghostLap.frames.maxOfOrNull { it.speedKmh } ?: 180f
            val avgSpd = if (ghostLap.frames.isNotEmpty()) ghostLap.frames.map { it.speedKmh }.average().toFloat() else 140f
            val json = serializeFrames(ghostLap.frames)

            return ReplayStorage(
                id = "replay_${ghostLap.trackId}_${System.currentTimeMillis()}",
                trackId = ghostLap.trackId,
                trackName = trackName,
                vehicleId = vehicleId,
                vehicleName = ghostLap.carName,
                lapTimeMs = ghostLap.lapTimeMs,
                maxSpeedKmh = maxSpd,
                avgSpeedKmh = avgSpd,
                cleanLap = cleanLap,
                weatherType = weatherType,
                dateRecorded = "2026-11-06",
                timestamp = System.currentTimeMillis(),
                frameCount = ghostLap.frames.size,
                telemetryDataJson = json
            )
        }

        /**
         * Generates default benchmark replay telemetry for all 4 tracks using track geometry.
         */
        fun createDefaultReplays(): List<ReplayStorage> {
            val list = mutableListOf<ReplayStorage>()
            val tracks = TrackRegistry.allTracks

            for (track in tracks) {
                val waypoints = track.waypoints
                if (waypoints.isEmpty()) continue

                val targetLapTimeMs = track.targetLapTimeMs
                val frames = mutableListOf<GhostFrame>()
                val numSamples = 160
                val dt = targetLapTimeMs.toFloat() / numSamples.toFloat()

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples.toFloat()
                    val wpFloatIdx = progress * (waypoints.size - 1)
                    val idx1 = wpFloatIdx.toInt()
                    val idx2 = (idx1 + 1).coerceAtMost(waypoints.size - 1)
                    val fract = wpFloatIdx - idx1

                    val wp1 = waypoints[idx1]
                    val wp2 = waypoints[idx2]

                    val interX = wp1.x + (wp2.x - wp1.x) * fract
                    val interY = wp1.y + (wp2.y - wp1.y) * fract

                    val dx = wp2.x - wp1.x
                    val dy = wp2.y - wp1.y
                    val heading = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()

                    val targetSpeed = wp1.targetSpeedKmh + (wp2.targetSpeedKmh - wp1.targetSpeedKmh) * fract
                    val isBraking = wp2.targetSpeedKmh < wp1.targetSpeedKmh - 15f

                    frames.add(
                        GhostFrame(
                            timeMs = (i * dt).toLong(),
                            x = interX,
                            y = interY,
                            heading = heading,
                            speedKmh = targetSpeed,
                            isBraking = isBraking
                        )
                    )
                }

                val ghostLap = GhostLap(
                    trackId = track.id,
                    carName = "Hyperion GT-R",
                    lapTimeMs = targetLapTimeMs,
                    frames = frames
                )

                list.add(
                    fromGhostLap(
                        ghostLap = ghostLap,
                        trackName = track.name,
                        vehicleId = "hyperion_gtr",
                        weatherType = track.defaultWeather.name
                    )
                )
            }
            return list
        }
    }
}
