package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStatisticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameStatistic(statistic: GameStatistics): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statistics: List<GameStatistics>)

    @Query("SELECT * FROM game_statistics ORDER BY timestamp DESC")
    fun getAllGameStatistics(): Flow<List<GameStatistics>>

    @Query("SELECT * FROM game_statistics ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentGameStatisticsFlow(limit: Int = 20): Flow<List<GameStatistics>>

    @Query("SELECT * FROM game_statistics ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentGameStatistics(limit: Int = 20): List<GameStatistics>

    @Query("SELECT * FROM game_statistics WHERE trackId = :trackId ORDER BY timestamp DESC")
    fun getStatisticsForTrack(trackId: String): Flow<List<GameStatistics>>

    @Query("SELECT * FROM game_statistics WHERE bestLapTimeMs > 0 ORDER BY bestLapTimeMs ASC LIMIT :limit")
    fun getTopRaceTimesFlow(limit: Int = 50): Flow<List<GameStatistics>>

    @Query("SELECT * FROM game_statistics WHERE bestLapTimeMs > 0 ORDER BY bestLapTimeMs ASC LIMIT :limit")
    suspend fun getTopRaceTimes(limit: Int = 50): List<GameStatistics>

    @Query("SELECT * FROM game_statistics WHERE trackId = :trackId AND bestLapTimeMs > 0 ORDER BY bestLapTimeMs ASC LIMIT :limit")
    fun getTopRaceTimesForTrackFlow(trackId: String, limit: Int = 50): Flow<List<GameStatistics>>

    @Query("SELECT * FROM game_statistics WHERE trackId = :trackId AND bestLapTimeMs > 0 ORDER BY bestLapTimeMs ASC LIMIT :limit")
    suspend fun getTopRaceTimesForTrack(trackId: String, limit: Int = 50): List<GameStatistics>

    @Query("SELECT MIN(bestLapTimeMs) FROM game_statistics WHERE bestLapTimeMs > 0")
    suspend fun getPersonalBestLapTime(): Long?

    @Query("SELECT MIN(bestLapTimeMs) FROM game_statistics WHERE trackId = :trackId AND bestLapTimeMs > 0")
    suspend fun getPersonalBestLapTimeForTrack(trackId: String): Long?

    @Query("SELECT * FROM game_statistics WHERE bestLapTimeMs > 0 ORDER BY bestLapTimeMs ASC LIMIT 1")
    suspend fun getPersonalBestRecord(): GameStatistics?

    @Query("SELECT * FROM game_statistics WHERE trackId = :trackId AND bestLapTimeMs > 0 ORDER BY bestLapTimeMs ASC LIMIT 1")
    suspend fun getPersonalBestRecordForTrack(trackId: String): GameStatistics?

    @Query("SELECT COUNT(*) FROM game_statistics")
    suspend fun getTotalRacesRecorded(): Int

    @Query("SELECT COUNT(*) FROM game_statistics WHERE isWin = 1")
    suspend fun getTotalWinsRecorded(): Int

    @Query("SELECT COUNT(*) FROM game_statistics WHERE isPodium = 1")
    suspend fun getTotalPodiumsRecorded(): Int

    @Query("SELECT SUM(distanceMeters) FROM game_statistics")
    suspend fun getTotalDistanceSum(): Float?

    @Query("SELECT SUM(driftMeters) FROM game_statistics")
    suspend fun getTotalDriftSum(): Float?

    @Query("SELECT MAX(maxSpeedKmh) FROM game_statistics")
    suspend fun getMaxSpeedRecorded(): Float?

    @Query("DELETE FROM game_statistics WHERE recordId = :recordId")
    suspend fun deleteStatisticById(recordId: Long)

    @Query("DELETE FROM game_statistics")
    suspend fun clearAllGameStatistics()
}
