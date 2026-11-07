package com.example.data

import android.content.Context
import com.example.data.room.Achievements
import com.example.data.room.AchievementsDao
import com.example.data.room.AppDatabase
import com.example.data.room.DriverProfile
import com.example.data.room.DriverProfileDao
import com.example.data.room.GameStatistics
import com.example.data.room.GameStatisticsDao
import com.example.data.room.DailyChallenges
import com.example.data.room.DailyChallengesDao
import com.example.data.room.GlobalLeaderboards
import com.example.data.room.GlobalLeaderboardsDao
import com.example.data.room.ReplayStorage
import com.example.data.room.ReplayStorageDao
import com.example.data.room.CareerProgress
import com.example.data.room.CareerProgressDao
import com.example.data.room.TimeTrialRecord
import com.example.data.room.TimeTrialRecordDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Data Repository class that abstracts Room database operations from the UI layer,
 * providing a clean, thread-safe, reactive API for accessing and persisting
 * [DriverProfile], [GameStatistics], [Achievements], [DailyChallenges], [GlobalLeaderboards], [CareerProgress], and [TimeTrialRecord] data.
 */
class DataRepository(
    val driverProfileDao: DriverProfileDao,
    val gameStatisticsDao: GameStatisticsDao,
    val achievementsDao: AchievementsDao? = null,
    val dailyChallengesDao: DailyChallengesDao? = null,
    val globalLeaderboardsDao: GlobalLeaderboardsDao? = null,
    val replayStorageDao: ReplayStorageDao? = null,
    val careerProgressDao: CareerProgressDao? = null,
    val timeTrialRecordDao: TimeTrialRecordDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        driverProfileDao = AppDatabase.getDatabase(context).driverProfileDao(),
        gameStatisticsDao = AppDatabase.getDatabase(context).gameStatisticsDao(),
        achievementsDao = AppDatabase.getDatabase(context).achievementsDao(),
        dailyChallengesDao = AppDatabase.getDatabase(context).dailyChallengesDao(),
        globalLeaderboardsDao = AppDatabase.getDatabase(context).globalLeaderboardsDao(),
        replayStorageDao = AppDatabase.getDatabase(context).replayStorageDao(),
        careerProgressDao = AppDatabase.getDatabase(context).careerProgressDao(),
        timeTrialRecordDao = AppDatabase.getDatabase(context).timeTrialRecordDao()
    )

    val driverProfileRepository = DriverProfileRepository(driverProfileDao, ioDispatcher)
    val gameStatisticsRepository = GameStatisticsRepository(gameStatisticsDao, ioDispatcher)
    val achievementsRepository = achievementsDao?.let { AchievementsRepository(it, ioDispatcher) }
    val dailyChallengesRepository = dailyChallengesDao?.let { DailyChallengesRepository(it, ioDispatcher) }
    val globalLeaderboardsRepository = globalLeaderboardsDao?.let { GlobalLeaderboardsRepository(it, ioDispatcher) }
    val replayStorageRepository = replayStorageDao?.let { ReplayStorageRepository(it, ioDispatcher) }
    val careerManager = careerProgressDao?.let { CareerManager(it, ioDispatcher) }
    val leaderboardManager = timeTrialRecordDao?.let { LeaderboardManager(it, ioDispatcher) }
    val achievementManager = achievementsDao?.let { AchievementManager(it, driverProfileDao, ioDispatcher) }

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
     * Updates player racing statistics: total races completed, podium finishes, and preferred car.
     */
    suspend fun updatePlayerStatistics(
        racesCompleted: Int,
        podiumFinishes: Int,
        preferredCar: String,
        id: String = "primary_driver"
    ) = withContext(ioDispatcher) {
        driverProfileRepository.updatePlayerStatistics(
            racesCompleted = racesCompleted,
            podiumFinishes = podiumFinishes,
            preferredCar = preferredCar,
            id = id
        )
    }

    /**
     * Updates preferred vehicle for the driver.
     */
    suspend fun updatePreferredCar(
        preferredCar: String,
        id: String = "primary_driver"
    ) = withContext(ioDispatcher) {
        driverProfileRepository.updatePreferredCar(preferredCar = preferredCar, id = id)
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
     * Observable Flow of top race times sorted ascending by fastest lap time.
     */
    val topRaceTimesFlow: Flow<List<GameStatistics>> =
        gameStatisticsDao.getTopRaceTimesFlow(50)

    fun getTopRaceTimesFlow(limit: Int = 50): Flow<List<GameStatistics>> {
        return gameStatisticsDao.getTopRaceTimesFlow(limit)
    }

    fun getTopRaceTimesForTrackFlow(trackId: String, limit: Int = 50): Flow<List<GameStatistics>> {
        return gameStatisticsDao.getTopRaceTimesForTrackFlow(trackId, limit)
    }

    suspend fun getTopRaceTimes(limit: Int = 50): List<GameStatistics> = withContext(ioDispatcher) {
        gameStatisticsDao.getTopRaceTimes(limit)
    }

    suspend fun getPersonalBestLapTime(trackId: String? = null): Long? = withContext(ioDispatcher) {
        if (trackId != null && trackId.isNotBlank()) {
            gameStatisticsDao.getPersonalBestLapTimeForTrack(trackId)
        } else {
            gameStatisticsDao.getPersonalBestLapTime()
        }
    }

    suspend fun getPersonalBestRecord(trackId: String? = null): GameStatistics? = withContext(ioDispatcher) {
        if (trackId != null && trackId.isNotBlank()) {
            gameStatisticsDao.getPersonalBestRecordForTrack(trackId)
        } else {
            gameStatisticsDao.getPersonalBestRecord()
        }
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
