package com.example.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverProfileDao {

    @Query("SELECT * FROM driver_profile WHERE id = :id LIMIT 1")
    fun getDriverProfileFlow(id: String = "primary_driver"): Flow<DriverProfile?>

    @Query("SELECT * FROM driver_profile LIMIT 1")
    fun getPrimaryProfileFlow(): Flow<DriverProfile?>

    @Query("SELECT * FROM driver_profile WHERE id = :id LIMIT 1")
    suspend fun getDriverProfile(id: String = "primary_driver"): DriverProfile?

    @Query("SELECT * FROM driver_profile LIMIT 1")
    suspend fun getPrimaryProfile(): DriverProfile?

    @Query("SELECT * FROM driver_profile")
    suspend fun getAllProfiles(): List<DriverProfile>

    @Query("SELECT * FROM driver_profile")
    fun getAllProfilesFlow(): Flow<List<DriverProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: DriverProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriverProfile(profile: DriverProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: DriverProfile)

    @Update
    suspend fun updateProfile(profile: DriverProfile)

    @Update
    suspend fun update(profile: DriverProfile)

    @Delete
    suspend fun deleteProfile(profile: DriverProfile)

    @Delete
    suspend fun delete(profile: DriverProfile)

    @Query("DELETE FROM driver_profile WHERE id = :id")
    suspend fun deleteProfileById(id: String = "primary_driver")

    @Query("DELETE FROM driver_profile")
    suspend fun deleteAllProfiles()

    @Query("UPDATE driver_profile SET username = :name, country = :country, avatarPath = :avatarPath, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateDriverIdentity(
        id: String = "primary_driver",
        name: String,
        country: String,
        avatarPath: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE driver_profile SET totalDistance = totalDistance + :distance, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun addDistanceDriven(
        id: String = "primary_driver",
        distance: Double,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE driver_profile SET equippedBadgeId = :badgeId, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateEquippedBadge(
        id: String = "primary_driver",
        badgeId: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE driver_profile SET totalRacesCompleted = :racesCompleted, totalPodiums = :podiums, podiumFinishes = :podiums, preferredCar = :preferredCar, selectedVehicleId = :preferredCar, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun updatePlayerStatistics(
        id: String = "primary_driver",
        racesCompleted: Int,
        podiums: Int,
        preferredCar: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE driver_profile SET preferredCar = :preferredCar, selectedVehicleId = :preferredCar, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun updatePreferredCar(
        id: String = "primary_driver",
        preferredCar: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE driver_profile SET totalRacesCompleted = totalRacesCompleted + 1, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun incrementRacesCompleted(
        id: String = "primary_driver",
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE driver_profile SET totalPodiums = totalPodiums + 1, podiumFinishes = podiumFinishes + 1, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun incrementPodiumFinishes(
        id: String = "primary_driver",
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE driver_profile SET credits = credits + :credits, reputation = reputation + :rep, lastUpdatedTimestamp = :timestamp WHERE id = :id")
    suspend fun addCreditsAndRep(
        id: String = "primary_driver",
        credits: Int,
        rep: Int,
        timestamp: Long = System.currentTimeMillis()
    )
}
