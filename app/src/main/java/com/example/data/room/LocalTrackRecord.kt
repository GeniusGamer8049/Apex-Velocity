package com.example.data.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * Room entity storing local track records, personal best lap times, vehicle used,
 * and player driving progression metrics for each racing circuit.
 */
@Entity(
    tableName = "local_track_records",
    indices = [
        Index("bestLapTimeMs"),
        Index("lastDrivenTimestamp")
    ]
)
data class LocalTrackRecord(
    @PrimaryKey val trackId: String,
    val trackName: String,
    val bestLapTimeMs: Long,
    val bestLapVehicleId: String,
    val bestLapVehicleName: String,
    val topSpeedKmh: Float,
    val totalLapsCompleted: Int = 1,
    val totalRacesFinished: Int = 0,
    val totalWins: Int = 0,
    val totalPodiums: Int = 0,
    val isCleanLap: Boolean = true,
    val weatherCondition: String = "SUNNY_CLEAR",
    val previousBestLapTimeMs: Long? = null,
    val lastDrivenTimestamp: Long = System.currentTimeMillis(),
    val dateRecorded: String = "2026-11-09"
) {

    /**
     * Human-readable formatted best lap time (mm:ss.SSS).
     */
    val formattedBestLapTime: String
        @Ignore get() = formatLapTime(bestLapTimeMs)

    /**
     * Human-readable formatted previous best lap time.
     */
    val formattedPreviousBestLapTime: String?
        @Ignore get() = previousBestLapTimeMs?.let { formatLapTime(it) }

    /**
     * Lap time improvement delta in milliseconds (positive value represents time shaved off).
     */
    val improvementDeltaMs: Long?
        @Ignore get() = previousBestLapTimeMs?.let { it - bestLapTimeMs }

    /**
     * Formatted personal best improvement (e.g., "-1.240s" or "BENCHMARK RECORD").
     */
    val formattedImprovement: String
        @Ignore get() {
            val delta = improvementDeltaMs ?: return "PERSONAL BEST"
            return if (delta > 0L) {
                String.format(Locale.US, "-%.3fs", delta / 1000f)
            } else if (delta < 0L) {
                String.format(Locale.US, "+%.3fs", -delta / 1000f)
            } else {
                "TIED BEST"
            }
        }

    /**
     * Win rate percentage on this track (0% .. 100%).
     */
    val winRatePercent: Float
        @Ignore get() = if (totalRacesFinished > 0) {
            (totalWins.toFloat() / totalRacesFinished.toFloat()) * 100f
        } else 0f

    /**
     * Podium rate percentage on this track (0% .. 100%).
     */
    val podiumRatePercent: Float
        @Ignore get() = if (totalRacesFinished > 0) {
            (totalPodiums.toFloat() / totalRacesFinished.toFloat()) * 100f
        } else 0f

    companion object {
        fun formatLapTime(timeMs: Long): String {
            if (timeMs <= 0L) return "--:--.---"
            val totalSec = timeMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val ms = timeMs % 1000
            return String.format(Locale.US, "%02d:%02d.%03d", min, sec, ms)
        }

        /**
         * Benchmark baseline records seeded for each circuit when database is initialized.
         */
        fun createDefaultRecords(): List<LocalTrackRecord> {
            return listOf(
                LocalTrackRecord(
                    trackId = "monaco_riviera",
                    trackName = "Monaco Riviera Grand Prix",
                    bestLapTimeMs = 54820L,
                    bestLapVehicleId = "apex_gt86",
                    bestLapVehicleName = "Apex GT-86 Coupe",
                    topSpeedKmh = 194.5f,
                    totalLapsCompleted = 5,
                    totalRacesFinished = 2,
                    totalWins = 1,
                    totalPodiums = 2,
                    isCleanLap = true,
                    weatherCondition = "SUNNY_CLEAR",
                    previousBestLapTimeMs = 56420L,
                    lastDrivenTimestamp = System.currentTimeMillis() - 86400000L
                ),
                LocalTrackRecord(
                    trackId = "apex_speedway",
                    trackName = "Apex International Speedway",
                    bestLapTimeMs = 62450L,
                    bestLapVehicleId = "apex_gt86",
                    bestLapVehicleName = "Apex GT-86 Coupe",
                    topSpeedKmh = 212.0f,
                    totalLapsCompleted = 4,
                    totalRacesFinished = 1,
                    totalWins = 1,
                    totalPodiums = 1,
                    isCleanLap = true,
                    weatherCondition = "SUNNY_CLEAR",
                    previousBestLapTimeMs = 64100L,
                    lastDrivenTimestamp = System.currentTimeMillis() - 172800000L
                ),
                LocalTrackRecord(
                    trackId = "neon_tokyo",
                    trackName = "Tokyo Midnight Expressway",
                    bestLapTimeMs = 58920L,
                    bestLapVehicleId = "silvia_s15",
                    bestLapVehicleName = "Silvia S15 Spec-R",
                    topSpeedKmh = 228.4f,
                    totalLapsCompleted = 6,
                    totalRacesFinished = 3,
                    totalWins = 2,
                    totalPodiums = 3,
                    isCleanLap = true,
                    weatherCondition = "RAIN_SLICK",
                    previousBestLapTimeMs = 61250L,
                    lastDrivenTimestamp = System.currentTimeMillis() - 43200000L
                ),
                LocalTrackRecord(
                    trackId = "mount_akina",
                    trackName = "Mount Akina Touge Pass",
                    bestLapTimeMs = 52140L,
                    bestLapVehicleId = "silvia_s15",
                    bestLapVehicleName = "Silvia S15 Spec-R",
                    topSpeedKmh = 186.2f,
                    totalLapsCompleted = 8,
                    totalRacesFinished = 4,
                    totalWins = 3,
                    totalPodiums = 4,
                    isCleanLap = true,
                    weatherCondition = "SNOW_BLIZZARD",
                    previousBestLapTimeMs = 53890L,
                    lastDrivenTimestamp = System.currentTimeMillis() - 21600000L
                )
            )
        }
    }
}
