package com.example.data.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Room entity storing driver experience points, driver level, unlocked tracks,
 * vehicle progression tiers, and career statistics based on race results.
 */
@Entity(tableName = "career_progression")
data class CareerProgress(
    @PrimaryKey val driverId: String = "primary_driver",
    val experiencePoints: Int = 0,
    val driverLevel: Int = 1,
    val unlockedTrackIds: String = "monaco_riviera,apex_speedway",
    val unlockedTierIds: String = "tier_1",
    val currentTierId: String = "tier_1",
    val unlockedVehicleIds: String = "apex_gt86",
    val completedEventIds: String = "",
    val trophyCount: Int = 0,
    val totalRacesFinished: Int = 0,
    val totalWins: Int = 0,
    val totalPodiums: Int = 0,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Splits comma-separated track IDs into a clean list.
     */
    fun getTrackIdList(): List<String> {
        return if (unlockedTrackIds.isBlank()) emptyList()
        else unlockedTrackIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Splits comma-separated tier IDs into a clean list.
     */
    fun getTierIdList(): List<String> {
        return if (unlockedTierIds.isBlank()) emptyList()
        else unlockedTierIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Splits comma-separated vehicle IDs into a clean list.
     */
    fun getVehicleIdList(): List<String> {
        return if (unlockedVehicleIds.isBlank()) emptyList()
        else unlockedVehicleIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Splits comma-separated completed event IDs into a clean list.
     */
    fun getCompletedEventIdList(): List<String> {
        return if (completedEventIds.isBlank()) emptyList()
        else completedEventIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @Ignore
    val isLevelMax: Boolean = driverLevel >= 10

    companion object {
        fun defaultProgression(): CareerProgress {
            return CareerProgress(
                driverId = "primary_driver",
                experiencePoints = 0,
                driverLevel = 1,
                unlockedTrackIds = "monaco_riviera,apex_speedway",
                unlockedTierIds = "tier_1",
                currentTierId = "tier_1",
                unlockedVehicleIds = "apex_gt86",
                completedEventIds = "",
                trophyCount = 0,
                totalRacesFinished = 0,
                totalWins = 0,
                totalPodiums = 0,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        }
    }
}
