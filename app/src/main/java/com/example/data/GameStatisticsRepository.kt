package com.example.data

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.GameStatistics
import com.example.data.room.GameStatisticsDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository class that abstracts GameStatistics Room database operations from the UI layer,
 * providing clean reactive streams and query methods for race history, outcomes, and telemetry.
 */
class GameStatisticsRepository(
    val gameStatisticsDao: GameStatisticsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        gameStatisticsDao = AppDatabase.getDatabase(context).gameStatisticsDao()
    )

    /**
     * Continuous reactive Flow emitting all race statistics ordered newest to oldest.
     */
    val allGameStatisticsFlow: Flow<List<GameStatistics>> =
        gameStatisticsDao.getAllGameStatistics()

    /**
     * Continuous reactive Flow emitting the most recent race statistics (default: 20).
     */
    val recentGameStatisticsFlow: Flow<List<GameStatistics>> =
        gameStatisticsDao.getRecentGameStatisticsFlow(20)

    fun getRecentGameStatisticsFlow(limit: Int = 20): Flow<List<GameStatistics>> {
        return gameStatisticsDao.getRecentGameStatisticsFlow(limit)
    }

    fun getTrackStatisticsFlow(trackId: String): Flow<List<GameStatistics>> {
        return gameStatisticsDao.getStatisticsForTrack(trackId)
    }

    suspend fun getRecentGameStatistics(limit: Int = 20): List<GameStatistics> = withContext(ioDispatcher) {
        gameStatisticsDao.getRecentGameStatistics(limit)
    }

    suspend fun insertGameStatistic(statistic: GameStatistics): Long = withContext(ioDispatcher) {
        gameStatisticsDao.insertGameStatistic(statistic)
    }

    suspend fun getTotalRacesRecorded(): Int = withContext(ioDispatcher) {
        gameStatisticsDao.getTotalRacesRecorded()
    }

    suspend fun getTotalWinsRecorded(): Int = withContext(ioDispatcher) {
        gameStatisticsDao.getTotalWinsRecorded()
    }

    suspend fun getTotalPodiumsRecorded(): Int = withContext(ioDispatcher) {
        gameStatisticsDao.getTotalPodiumsRecorded()
    }

    suspend fun getTotalDistanceSum(): Float = withContext(ioDispatcher) {
        gameStatisticsDao.getTotalDistanceSum() ?: 0f
    }

    suspend fun getMaxSpeedRecorded(): Float = withContext(ioDispatcher) {
        gameStatisticsDao.getMaxSpeedRecorded() ?: 0f
    }

    suspend fun deleteStatistic(recordId: Long) = withContext(ioDispatcher) {
        gameStatisticsDao.deleteStatisticById(recordId)
    }

    suspend fun clearAllStatistics() = withContext(ioDispatcher) {
        gameStatisticsDao.clearAllGameStatistics()
    }

    companion object {
        @Volatile
        private var INSTANCE: GameStatisticsRepository? = null

        fun getInstance(context: Context): GameStatisticsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = GameStatisticsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
