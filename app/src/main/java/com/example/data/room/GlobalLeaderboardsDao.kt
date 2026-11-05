package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for [GlobalLeaderboards] Room entity.
 * Provides query, insertion, and update operations for circuit rankings.
 */
@Dao
interface GlobalLeaderboardsDao {

    @Query("SELECT * FROM global_leaderboards WHERE trackId = :trackId ORDER BY rank ASC, lapTimeMs ASC")
    fun getLeaderboardForTrackFlow(trackId: String): Flow<List<GlobalLeaderboards>>

    @Query("SELECT * FROM global_leaderboards WHERE trackId = :trackId ORDER BY rank ASC, lapTimeMs ASC")
    suspend fun getLeaderboardForTrack(trackId: String): List<GlobalLeaderboards>

    @Query("SELECT * FROM global_leaderboards WHERE trackId = :trackId ORDER BY rank ASC, lapTimeMs ASC LIMIT :limit")
    fun getTopRankingsForTrackFlow(trackId: String, limit: Int = 10): Flow<List<GlobalLeaderboards>>

    @Query("SELECT * FROM global_leaderboards WHERE trackId = :trackId ORDER BY rank ASC, lapTimeMs ASC LIMIT :limit")
    suspend fun getTopRankingsForTrack(trackId: String, limit: Int = 10): List<GlobalLeaderboards>

    @Query("SELECT * FROM global_leaderboards ORDER BY trackId ASC, rank ASC")
    fun getAllLeaderboardsFlow(): Flow<List<GlobalLeaderboards>>

    @Query("SELECT * FROM global_leaderboards ORDER BY trackId ASC, rank ASC")
    suspend fun getAllLeaderboards(): List<GlobalLeaderboards>

    @Query("SELECT * FROM global_leaderboards WHERE trackId = :trackId AND isPlayer = 1 LIMIT 1")
    fun getPlayerEntryForTrackFlow(trackId: String): Flow<GlobalLeaderboards?>

    @Query("SELECT * FROM global_leaderboards WHERE trackId = :trackId AND isPlayer = 1 LIMIT 1")
    suspend fun getPlayerEntryForTrack(trackId: String): GlobalLeaderboards?

    @Query("SELECT * FROM global_leaderboards WHERE trackId = :trackId AND rank = 1 LIMIT 1")
    fun getWorldRecordForTrackFlow(trackId: String): Flow<GlobalLeaderboards?>

    @Query("SELECT * FROM global_leaderboards WHERE trackId = :trackId AND rank = 1 LIMIT 1")
    suspend fun getWorldRecordForTrack(trackId: String): GlobalLeaderboards?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: GlobalLeaderboards)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<GlobalLeaderboards>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultEntries(entries: List<GlobalLeaderboards>)

    @Update
    suspend fun updateEntry(entry: GlobalLeaderboards)

    @Query("DELETE FROM global_leaderboards WHERE trackId = :trackId")
    suspend fun deleteTrackLeaderboard(trackId: String)

    @Query("DELETE FROM global_leaderboards WHERE trackId = :trackId AND isPlayer = 1")
    suspend fun deletePlayerEntryForTrack(trackId: String)

    @Query("DELETE FROM global_leaderboards")
    suspend fun deleteAllLeaderboards()

    @Query("SELECT COUNT(*) FROM global_leaderboards WHERE trackId = :trackId")
    suspend fun getEntryCountForTrack(trackId: String): Int

    @Query("SELECT COUNT(*) FROM global_leaderboards")
    suspend fun getTotalEntryCount(): Int
}
