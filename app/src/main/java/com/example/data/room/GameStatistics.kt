package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * GameStatistics Room entity storing race records, race history summaries,
 * vehicle performance levels, positions, telemetry, and rewards.
 */
@Entity(tableName = "game_statistics")
data class GameStatistics(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0L,
    val raceHistory: String = "",
    val carPerformanceLevels: String = "",
    val trackId: String = "",
    val vehicleId: String = "",
    val position: Int = 1,
    val totalRacers: Int = 4,
    val bestLapTimeMs: Long = 0L,
    val isWin: Boolean = false,
    val isPodium: Boolean = false,
    val isClean: Boolean = false,
    val maxSpeedKmh: Float = 0f,
    val driftMeters: Float = 0f,
    val distanceMeters: Float = 0f,
    val earningsCredits: Int = 0,
    val reputationEarned: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

typealias GameStatisticsEntity = GameStatistics
