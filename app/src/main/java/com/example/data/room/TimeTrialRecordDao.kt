package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [TimeTrialRecord] persistence, querying top records per track,
 * and managing time trial leaderboards.
 */
@Dao
interface TimeTrialRecordDao {

    @Query("SELECT * FROM time_trial_records WHERE trackId = :trackId ORDER BY completionTimeMs ASC LIMIT :limit")
    fun getTopRecordsForTrackFlow(trackId: String, limit: Int = 10): Flow<List<TimeTrialRecord>>

    @Query("SELECT * FROM time_trial_records WHERE trackId = :trackId ORDER BY completionTimeMs ASC LIMIT :limit")
    suspend fun getTopRecordsForTrack(trackId: String, limit: Int = 10): List<TimeTrialRecord>

    @Query("SELECT * FROM time_trial_records WHERE trackId = :trackId ORDER BY completionTimeMs ASC")
    suspend fun getAllRecordsForTrack(trackId: String): List<TimeTrialRecord>

    @Query("SELECT * FROM time_trial_records ORDER BY trackId, completionTimeMs ASC")
    fun getAllRecordsFlow(): Flow<List<TimeTrialRecord>>

    @Query("SELECT * FROM time_trial_records ORDER BY trackId, completionTimeMs ASC")
    suspend fun getAllRecords(): List<TimeTrialRecord>

    @Query("SELECT * FROM time_trial_records WHERE trackId = :trackId ORDER BY completionTimeMs ASC LIMIT 1")
    fun getTrackRecordFlow(trackId: String): Flow<TimeTrialRecord?>

    @Query("SELECT * FROM time_trial_records WHERE trackId = :trackId ORDER BY completionTimeMs ASC LIMIT 1")
    suspend fun getTrackRecord(trackId: String): TimeTrialRecord?

    @Query("SELECT * FROM time_trial_records WHERE trackId = :trackId AND playerName = :playerName ORDER BY completionTimeMs ASC LIMIT 1")
    fun getPlayerBestRecordFlow(trackId: String, playerName: String): Flow<TimeTrialRecord?>

    @Query("SELECT * FROM time_trial_records WHERE trackId = :trackId AND playerName = :playerName ORDER BY completionTimeMs ASC LIMIT 1")
    suspend fun getPlayerBestRecord(trackId: String, playerName: String): TimeTrialRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TimeTrialRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<TimeTrialRecord>)

    @Query("DELETE FROM time_trial_records WHERE trackId = :trackId")
    suspend fun deleteRecordsForTrack(trackId: String)

    @Query("DELETE FROM time_trial_records WHERE trackId = :trackId AND playerName = :playerName")
    suspend fun deletePlayerRecordsForTrack(trackId: String, playerName: String)

    @Query("DELETE FROM time_trial_records")
    suspend fun deleteAllRecords()

    @Query("SELECT COUNT(*) FROM time_trial_records")
    suspend fun getTotalRecordCount(): Int
}
