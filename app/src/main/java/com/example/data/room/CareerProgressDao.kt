package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [CareerProgress] persistence and updates.
 */
@Dao
interface CareerProgressDao {

    @Query("SELECT * FROM career_progression WHERE driverId = :driverId LIMIT 1")
    fun getCareerProgressFlow(driverId: String = "primary_driver"): Flow<CareerProgress?>

    @Query("SELECT * FROM career_progression WHERE driverId = :driverId LIMIT 1")
    suspend fun getCareerProgress(driverId: String = "primary_driver"): CareerProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(careerProgress: CareerProgress)

    @Query("""
        UPDATE career_progression 
        SET experiencePoints = :xp, driverLevel = :level, lastUpdatedTimestamp = :timestamp 
        WHERE driverId = :driverId
    """)
    suspend fun updateExperienceAndLevel(
        driverId: String = "primary_driver",
        xp: Int,
        level: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE career_progression 
        SET unlockedTrackIds = :trackIds, lastUpdatedTimestamp = :timestamp 
        WHERE driverId = :driverId
    """)
    suspend fun updateUnlockedTracks(
        driverId: String = "primary_driver",
        trackIds: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE career_progression 
        SET unlockedTierIds = :tierIds, currentTierId = :currentTierId, lastUpdatedTimestamp = :timestamp 
        WHERE driverId = :driverId
    """)
    suspend fun updateUnlockedTiers(
        driverId: String = "primary_driver",
        tierIds: String,
        currentTierId: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE career_progression 
        SET unlockedVehicleIds = :vehicleIds, lastUpdatedTimestamp = :timestamp 
        WHERE driverId = :driverId
    """)
    suspend fun updateUnlockedVehicles(
        driverId: String = "primary_driver",
        vehicleIds: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM career_progression WHERE driverId = :driverId")
    suspend fun deleteCareerProgress(driverId: String = "primary_driver")
}
