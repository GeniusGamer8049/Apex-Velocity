package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [LocalTrackRecord] persistence and queries.
 * Manages player local track records, personal best lap times, and circuit progression.
 */
@Dao
interface LocalTrackRecordDao {

    @Query("SELECT * FROM local_track_records ORDER BY trackName ASC")
    fun getAllTrackRecordsFlow(): Flow<List<LocalTrackRecord>>

    @Query("SELECT * FROM local_track_records ORDER BY trackName ASC")
    suspend fun getAllTrackRecords(): List<LocalTrackRecord>

    @Query("SELECT * FROM local_track_records WHERE trackId = :trackId LIMIT 1")
    fun getTrackRecordFlow(trackId: String): Flow<LocalTrackRecord?>

    @Query("SELECT * FROM local_track_records WHERE trackId = :trackId LIMIT 1")
    suspend fun getTrackRecord(trackId: String): LocalTrackRecord?

    @Query("SELECT * FROM local_track_records WHERE bestLapTimeMs > 0 ORDER BY bestLapTimeMs ASC")
    fun getFastestTracksFlow(): Flow<List<LocalTrackRecord>>

    @Query("SELECT * FROM local_track_records ORDER BY lastDrivenTimestamp DESC")
    fun getRecentlyDrivenTracksFlow(): Flow<List<LocalTrackRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LocalTrackRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<LocalTrackRecord>)

    @Query("DELETE FROM local_track_records WHERE trackId = :trackId")
    suspend fun deleteRecord(trackId: String)

    @Query("DELETE FROM local_track_records")
    suspend fun deleteAllRecords()

    @Query("SELECT COUNT(*) FROM local_track_records")
    suspend fun getRecordCount(): Int
}
