package com.example.data

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.DriverProfile
import com.example.data.room.DriverProfileDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository class that abstracts DriverProfile database operations from the UI layer,
 * providing a clean API for reading and modifying driver stats, credentials, and identity.
 */
class DriverProfileRepository(
    val driverProfileDao: DriverProfileDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        driverProfileDao = AppDatabase.getDatabase(context).driverProfileDao()
    )

    /**
     * Continuous reactive Flow emitting the current primary driver profile as it changes.
     */
    val driverProfileFlow: Flow<DriverProfile?> = driverProfileDao.getDriverProfileFlow("primary_driver")

    /**
     * Continuous reactive Flow emitting a driver profile for the given ID.
     */
    fun getDriverProfileFlow(id: String = "primary_driver"): Flow<DriverProfile?> {
        return driverProfileDao.getDriverProfileFlow(id)
    }

    /**
     * Retrieves the latest snapshot of the driver profile from the database.
     */
    suspend fun getDriverProfile(id: String = "primary_driver"): DriverProfile? = withContext(ioDispatcher) {
        driverProfileDao.getDriverProfile(id)
    }

    /**
     * Inserts or completely updates a driver profile entity.
     */
    suspend fun saveDriverProfile(profile: DriverProfile) = withContext(ioDispatcher) {
        driverProfileDao.insertOrUpdateProfile(profile)
    }

    /**
     * Updates the username of the driver. If the record does not exist yet, creates it.
     */
    suspend fun updateUsername(username: String, id: String = "primary_driver") = withContext(ioDispatcher) {
        val trimmed = username.trim().ifEmpty { "Apex Pilot" }
        val current = driverProfileDao.getDriverProfile(id)
        if (current != null) {
            driverProfileDao.insertOrUpdateProfile(
                current.copy(
                    username = trimmed,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            driverProfileDao.insertOrUpdateProfile(
                DriverProfile(
                    id = id,
                    username = trimmed,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Updates driver username, country code, and avatar path.
     */
    suspend fun updateDriverIdentity(
        name: String,
        country: String,
        avatarPath: String,
        id: String = "primary_driver"
    ) = withContext(ioDispatcher) {
        val trimmedName = name.trim().ifEmpty { "Apex Pilot" }
        val trimmedCountry = country.trim().uppercase().ifEmpty { "US" }
        val current = driverProfileDao.getDriverProfile(id)
        if (current != null) {
            driverProfileDao.updateDriverIdentity(
                id = id,
                name = trimmedName,
                country = trimmedCountry,
                avatarPath = avatarPath,
                timestamp = System.currentTimeMillis()
            )
        } else {
            driverProfileDao.insertOrUpdateProfile(
                DriverProfile(
                    id = id,
                    username = trimmedName,
                    country = trimmedCountry,
                    avatarPath = avatarPath,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Accumulates distance driven into the persistent record.
     */
    suspend fun addDistanceDriven(distanceMeters: Double, id: String = "primary_driver") = withContext(ioDispatcher) {
        driverProfileDao.addDistanceDriven(id = id, distance = distanceMeters)
    }

    /**
     * Equips an unlocked achievement badge.
     */
    suspend fun updateEquippedBadge(badgeId: String, id: String = "primary_driver") = withContext(ioDispatcher) {
        driverProfileDao.updateEquippedBadge(id = id, badgeId = badgeId)
    }

    /**
     * Deletes a driver profile.
     */
    suspend fun deleteProfile(id: String = "primary_driver") = withContext(ioDispatcher) {
        driverProfileDao.deleteProfileById(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: DriverProfileRepository? = null

        fun getInstance(context: Context): DriverProfileRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DriverProfileRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
