package com.example.data.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * DriverProfile Room entity storing player identity, avatar path,
 * total distance driven, total career wins, and racing progression.
 */
@Entity(tableName = "driver_profile")
data class DriverProfile(
    @PrimaryKey val id: String = "primary_driver",
    val username: String = "Apex Pilot",
    val avatarPath: String = "avatar_cyber_neon",
    val totalDistance: Double = 0.0,
    val totalWins: Int = 0,
    val country: String = "US",
    val credits: Int = 2500,
    val reputation: Int = 0,
    val selectedVehicleId: String = "apex_gt86",
    val totalRacesCompleted: Int = 0,
    val totalPodiums: Int = 0,
    val totalDriftDistance: Double = 0.0,
    val highestSpeedKmh: Float = 0f,
    val cleanRacesCount: Int = 0,
    val wetRaceWinsCount: Int = 0,
    val equippedBadgeId: String = "badge_rookie",
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    val driverName: String @Ignore get() = username
    val avatarId: String @Ignore get() = avatarPath
    val driverCountry: String @Ignore get() = country
    val totalRaceWins: Int @Ignore get() = totalWins
    val totalDistanceDrivenMeters: Float @Ignore get() = totalDistance.toFloat()
}

typealias DriverProfileEntity = DriverProfile
