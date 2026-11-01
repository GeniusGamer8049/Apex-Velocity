package com.example.data

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.DriverProfile
import com.example.data.room.DriverProfileDao
import com.example.data.room.GameStatistics
import com.example.data.room.GameStatisticsDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Data Repository class that abstracts Room database operations from the UI layer,
 * providing a clean, thread-safe, reactive API for accessing and persisting
 * [DriverProfile] and [GameStatistics] data.
 */
class DataRepository(
    val driverProfileDao: DriverProfileDao,
    val gameStatisticsDao: GameStatisticsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        driverProfileDao = AppDatabase.getDatabase(context).driverProfileDao(),
        gameStatisticsDao = AppDatabase.getDatabase(context).gameStatisticsDao()
    )

    // ========================================================================
    // Driver Profile APIs
    // ========================================================================

    /**
     * Observable Flow of the primary driver profile, emitting whenever database records update.
     */
    val driverProfileFlow: Flow<DriverProfile?> = driverProfileDao.getDriverProfileFlow("primary_driver")

    /**
     * Observable Flow for any driver profile by ID.
     */
    fun getDriverProfileFlow(id: String = "primary_driver"): Flow<DriverProfile?> {
        return driverProfileDao.getDriverProfileFlow(id)
    }

    /**
     * Retrieves the driver profile snapshot.
     */
    suspend fun getDriverProfile(id: String = "primary_driver"): DriverProfile? = withContext(ioDispatcher) {
        driverProfileDao.getDriverProfile(id)
    }

    /**
     * Inserts or updates the driver profile.
     */
    suspend fun saveDriverProfile(profile: DriverProfile) = withContext(ioDispatcher) {
        driverProfileDao.insertOrUpdateProfile(profile)
    }

    /**
     * Updates driver identity (callsign, country, avatar asset path).
     */
    suspend fun updateDriverIdentity(
        name: String,
        country: String,
        avatarPath: String,
        id: String = "primary_driver"
    ) = withContext(ioDispatcher) {
        driverProfileDao.updateDriverIdentity(
            id = id,
            name = name,
            country = country,
            avatarPath = avatarPath
        )
    }

    /**
     * Adds distance driven in meters to the cumulative profile stats.
     */
    suspend fun addDistanceDriven(
        distanceMeters: Double,
        id: String = "primary_driver"
    ) = withContext(ioDispatcher) {
        driverProfileDao.addDistanceDriven(
            id = id,
            distance = distanceMeters
        )
    }

    /**
     * Equips an unlocked achievement badge.
     */
    suspend fun updateEquippedBadge(
        badgeId: String,
        id: String = "primary_driver"
    ) = withContext(ioDispatcher) {
        driverProfileDao.updateEquippedBadge(
            id = id,
            badgeId = badgeId
        )
    }

    /**
     * Deletes a driver profile by ID.
     */
    suspend fun deleteDriverProfile(id: String = "primary_driver") = withContext(ioDispatcher) {
        driverProfileDao.deleteProfileById(id)
    }

    // ========================================================================
    // Game Statistics APIs
    // ========================================================================

    /**
     * Observable Flow of recent race game statistics (default: latest 20).
     */
    val recentGameStatisticsFlow: Flow<List<GameStatistics>> =
        gameStatisticsDao.getRecentGameStatisticsFlow(20)

    /**
     * Observable Flow of all game statistics.
     */
    val allGameStatisticsFlow: Flow<List<GameStatistics>> =
        gameStatisticsDao.getAllGameStatistics()

    /**
     * Observable Flow of statistics filtered by specific track ID.
     */
    fun getTrackStatisticsFlow(trackId: String): Flow<List<GameStatistics>> {
        return gameStatisticsDao.getStatisticsForTrack(trackId)
    }

    /**
     * Retrieves recent game statistics snapshot.
     */
    suspend fun getRecentGameStatistics(limit: Int = 20): List<GameStatistics> = withContext(ioDispatcher) {
        gameStatisticsDao.getRecentGameStatistics(limit)
    }

    /**
     * Records a new race event result and vehicle telemetry stats.
     */
    suspend fun insertGameStatistic(statistic: GameStatistics): Long = withContext(ioDispatcher) {
        gameStatisticsDao.insertGameStatistic(statistic)
    }

    /**
     * Records multiple game statistics at once.
     */
    suspend fun insertAllGameStatistics(statistics: List<GameStatistics>) = withContext(ioDispatcher) {
        gameStatisticsDao.insertAll(statistics)
    }

    /**
     * Convenient helper to record a full post-race statistic entry.
     */
    suspend fun recordRaceEvent(
        trackId: String,
        vehicleId: String,
        position: Int,
        totalRacers: Int = 4,
        bestLapTimeMs: Long = 0L,
        isWin: Boolean = false,
        isPodium: Boolean = false,
        isClean: Boolean = false,
        maxSpeedKmh: Float = 0f,
        driftMeters: Float = 0f,
        distanceMeters: Float = 0f,
        earningsCredits: Int = 0,
        reputationEarned: Int = 0,
        raceHistory: String = "",
        carPerformanceLevels: String = ""
    ): Long = withContext(ioDispatcher) {
        val entry = GameStatistics(
            trackId = trackId,
            vehicleId = vehicleId,
            position = position,
            totalRacers = totalRacers,
            bestLapTimeMs = bestLapTimeMs,
            isWin = isWin,
            isPodium = isPodium,
            isClean = isClean,
            maxSpeedKmh = maxSpeedKmh,
            driftMeters = driftMeters,
            distanceMeters = distanceMeters,
            earningsCredits = earningsCredits,
            reputationEarned = reputationEarned,
            raceHistory = raceHistory,
            carPerformanceLevels = carPerformanceLevels
        )
        gameStatisticsDao.insertGameStatistic(entry)
    }

    /**
     * Retrieves aggregate total count of races recorded.
     */
    suspend fun getTotalRacesRecorded(): Int = withContext(ioDispatcher) {
        gameStatisticsDao.getTotalRacesRecorded()
    }

    /**
     * Retrieves aggregate total count of race wins.
     */
    suspend fun getTotalWinsRecorded(): Int = withContext(ioDispatcher) {
        gameStatisticsDao.getTotalWinsRecorded()
    }

    /**
     * Retrieves aggregate total count of podium finishes.
     */
    suspend fun getTotalPodiumsRecorded(): Int = withContext(ioDispatcher) {
        gameStatisticsDao.getTotalPodiumsRecorded()
    }

    /**
     * Retrieves aggregate total distance driven in meters across recorded races.
     */
    suspend fun getTotalDistanceSum(): Float = withContext(ioDispatcher) {
        gameStatisticsDao.getTotalDistanceSum() ?: 0f
    }

    /**
     * Retrieves aggregate total drift distance in meters.
     */
    suspend fun getTotalDriftSum(): Float = withContext(ioDispatcher) {
        gameStatisticsDao.getTotalDriftSum() ?: 0f
    }

    /**
     * Retrieves top maximum speed recorded across all races.
     */
    suspend fun getMaxSpeedRecorded(): Float = withContext(ioDispatcher) {
        gameStatisticsDao.getMaxSpeedRecorded() ?: 0f
    }

    /**
     * Deletes a statistic record by ID.
     */
    suspend fun deleteStatistic(recordId: Long) = withContext(ioDispatcher) {
        gameStatisticsDao.deleteStatisticById(recordId)
    }

    /**
     * Clears all recorded game statistics.
     */
    suspend fun clearAllStatistics() = withContext(ioDispatcher) {
        gameStatisticsDao.clearAllGameStatistics()
    }

    companion object {
        @Volatile
        private var INSTANCE: DataRepository? = null

        fun getInstance(context: Context): DataRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DataRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
