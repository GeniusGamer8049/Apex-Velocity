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

    @Query("SELECT * FROM driver_profile WHERE id = :id LIMIT 1")
    suspend fun getDriverProfile(id: String = "primary_driver"): DriverProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: DriverProfile)

    @Update
    suspend fun updateProfile(profile: DriverProfile)

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

    @Query("DELETE FROM driver_profile WHERE id = :id")
    suspend fun deleteProfileById(id: String = "primary_driver")
}
