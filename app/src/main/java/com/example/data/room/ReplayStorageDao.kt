package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for [ReplayStorage] Room entity.
 * Provides query, insertion, and deletion operations for race telemetry and ghost car replay data.
 */
@Dao
interface ReplayStorageDao {

    @Query("SELECT * FROM replay_storage WHERE id = :id LIMIT 1")
    suspend fun getReplayById(id: String): ReplayStorage?

    @Query("SELECT * FROM replay_storage WHERE id = :id LIMIT 1")
    fun getReplayByIdFlow(id: String): Flow<ReplayStorage?>

    @Query("SELECT * FROM replay_storage ORDER BY timestamp DESC")
    fun getAllReplaysFlow(): Flow<List<ReplayStorage>>

    @Query("SELECT * FROM replay_storage ORDER BY timestamp DESC")
    suspend fun getAllReplays(): List<ReplayStorage>

    @Query("SELECT * FROM replay_storage WHERE trackId = :trackId ORDER BY lapTimeMs ASC, timestamp DESC")
    fun getReplaysForTrackFlow(trackId: String): Flow<List<ReplayStorage>>

    @Query("SELECT * FROM replay_storage WHERE trackId = :trackId ORDER BY lapTimeMs ASC, timestamp DESC")
    suspend fun getReplaysForTrack(trackId: String): List<ReplayStorage>

    @Query("SELECT * FROM replay_storage WHERE trackId = :trackId ORDER BY lapTimeMs ASC LIMIT 1")
    fun getBestReplayForTrackFlow(trackId: String): Flow<ReplayStorage?>

    @Query("SELECT * FROM replay_storage WHERE trackId = :trackId ORDER BY lapTimeMs ASC LIMIT 1")
    suspend fun getBestReplayForTrack(trackId: String): ReplayStorage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplay(replay: ReplayStorage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplays(replays: List<ReplayStorage>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultReplays(replays: List<ReplayStorage>)

    @Update
    suspend fun updateReplay(replay: ReplayStorage)

    @Query("DELETE FROM replay_storage WHERE id = :id")
    suspend fun deleteReplayById(id: String)

    @Query("DELETE FROM replay_storage WHERE trackId = :trackId")
    suspend fun deleteReplaysForTrack(trackId: String)

    @Query("DELETE FROM replay_storage")
    suspend fun clearAllReplays()

    @Query("SELECT COUNT(*) FROM replay_storage")
    suspend fun getReplayCount(): Int
}
