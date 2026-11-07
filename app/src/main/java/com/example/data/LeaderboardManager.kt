package com.example.data

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.TimeTrialRecord
import com.example.data.room.TimeTrialRecordDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Result data class when a new time trial completion is recorded.
 */
data class RecordResult(
    val record: TimeTrialRecord,
    val isNewPersonalBest: Boolean,
    val isNewTrackRecord: Boolean,
    val previousRank: Int?,
    val newRank: Int
)

/**
 * LeaderboardManager manages top time trial records for each track using Room database,
 * storing player name, vehicle used, completion time, clean lap status, and ranks.
 */
class LeaderboardManager(
    val timeTrialRecordDao: TimeTrialRecordDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        timeTrialRecordDao = AppDatabase.getDatabase(context).timeTrialRecordDao()
    )

    /**
     * Seeds initial benchmark time trial records for all circuits if empty.
     */
    suspend fun ensureDefaultRecords() = withContext(ioDispatcher) {
        val count = timeTrialRecordDao.getTotalRecordCount()
        if (count == 0) {
            val defaults = TimeTrialRecord.createDefaultRecords()
            timeTrialRecordDao.insertRecords(defaults)
        }
    }

    /**
     * Returns an observable Flow of top time trial records for [trackId],
     * ordered ascending by completion time (fastest first).
     */
    fun getTopRecordsForTrackFlow(trackId: String, limit: Int = 10): Flow<List<TimeTrialRecord>> {
        return timeTrialRecordDao.getTopRecordsForTrackFlow(trackId, limit)
    }

    /**
     * Snapshot query of top records for a track.
     */
    suspend fun getTopRecordsForTrack(trackId: String, limit: Int = 10): List<TimeTrialRecord> = withContext(ioDispatcher) {
        ensureDefaultRecords()
        timeTrialRecordDao.getTopRecordsForTrack(trackId, limit)
    }

    /**
     * Observable flow of the all-time #1 completion time record on a track.
     */
    fun getTrackRecordFlow(trackId: String): Flow<TimeTrialRecord?> {
        return timeTrialRecordDao.getTrackRecordFlow(trackId)
    }

    /**
     * Snapshot of the all-time #1 completion time record on a track.
     */
    suspend fun getTrackRecord(trackId: String): TimeTrialRecord? = withContext(ioDispatcher) {
        ensureDefaultRecords()
        timeTrialRecordDao.getTrackRecord(trackId)
    }

    /**
     * Observable flow of a player's personal best record on a track.
     */
    fun getPersonalBestFlow(trackId: String, playerName: String = "Apex Pilot"): Flow<TimeTrialRecord?> {
        return timeTrialRecordDao.getPlayerBestRecordFlow(trackId, playerName)
    }

    /**
     * Snapshot of a player's personal best record on a track.
     */
    suspend fun getPersonalBest(trackId: String, playerName: String = "Apex Pilot"): TimeTrialRecord? = withContext(ioDispatcher) {
        ensureDefaultRecords()
        timeTrialRecordDao.getPlayerBestRecord(trackId, playerName)
    }

    /**
     * Checks if a completion time would establish a new all-time circuit track record.
     */
    suspend fun isNewTrackRecord(trackId: String, completionTimeMs: Long): Boolean = withContext(ioDispatcher) {
        val currentRecord = getTrackRecord(trackId)
        currentRecord == null || completionTimeMs < currentRecord.completionTimeMs
    }

    /**
     * Checks if a completion time would establish a new personal best for [playerName] on [trackId].
     */
    suspend fun isPersonalBest(trackId: String, playerName: String, completionTimeMs: Long): Boolean = withContext(ioDispatcher) {
        val pb = getPersonalBest(trackId, playerName)
        pb == null || completionTimeMs < pb.completionTimeMs
    }

    /**
     * Records a new time trial completion for [trackId], storing player name,
     * vehicle used, and completion time, dynamically recalculating circuit rankings.
     */
    suspend fun recordTimeTrialCompletion(
        trackId: String,
        trackName: String,
        playerName: String,
        vehicleUsed: String,
        completionTimeMs: Long,
        cleanLap: Boolean = true,
        weatherCondition: String = "CLEAR"
    ): RecordResult = withContext(ioDispatcher) {
        ensureDefaultRecords()

        val previousPb = timeTrialRecordDao.getPlayerBestRecord(trackId, playerName)
        val isPb = previousPb == null || completionTimeMs < previousPb.completionTimeMs
        val currentTrackRecord = timeTrialRecordDao.getTrackRecord(trackId)
        val isTrackRecord = currentTrackRecord == null || completionTimeMs < currentTrackRecord.completionTimeMs

        val previousRank = previousPb?.rank

        // Create new record
        val newRecordId = "tt_${trackId}_${System.currentTimeMillis()}"
        val candidate = TimeTrialRecord(
            id = newRecordId,
            trackId = trackId,
            trackName = trackName,
            playerName = playerName,
            vehicleUsed = vehicleUsed,
            completionTimeMs = completionTimeMs,
            rank = 1,
            isPlayer = true,
            weatherCondition = weatherCondition,
            cleanLap = cleanLap,
            dateRecorded = "2026-11-07",
            timestamp = System.currentTimeMillis()
        )

        // If player already had a record on this track and this new run is better,
        // remove old player entry so player only has their best record on the leaderboard
        if (previousPb != null && isPb) {
            timeTrialRecordDao.deletePlayerRecordsForTrack(trackId, playerName)
        } else if (previousPb != null && !isPb) {
            return@withContext RecordResult(
                record = previousPb,
                isNewPersonalBest = false,
                isNewTrackRecord = false,
                previousRank = previousRank,
                newRank = previousRank ?: 1
            )
        }

        // Fetch remaining records for track
        val allOtherRecords = timeTrialRecordDao.getAllRecordsForTrack(trackId).filter { it.playerName != playerName }
        val combined = (allOtherRecords + candidate).sortedBy { it.completionTimeMs }

        // Recalculate ranks
        val reranked = combined.mapIndexed { index, record ->
            record.copy(rank = index + 1)
        }

        // Save reranked records into Room
        timeTrialRecordDao.deleteRecordsForTrack(trackId)
        timeTrialRecordDao.insertRecords(reranked)

        val savedPlayerRecord = reranked.find { it.id == newRecordId } ?: candidate.copy(rank = combined.indexOf(candidate) + 1)

        RecordResult(
            record = savedPlayerRecord,
            isNewPersonalBest = isPb,
            isNewTrackRecord = isTrackRecord,
            previousRank = previousRank,
            newRank = savedPlayerRecord.rank
        )
    }

    /**
     * Formats completion time in milliseconds to human-readable string (mm:ss.SSS).
     */
    fun formatCompletionTime(ms: Long): String {
        if (ms <= 0L) return "--:--.---"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d.%03d", min, sec, millis)
    }

    /**
     * Clears all records for a specific track.
     */
    suspend fun clearRecordsForTrack(trackId: String) = withContext(ioDispatcher) {
        timeTrialRecordDao.deleteRecordsForTrack(trackId)
    }

    /**
     * Clears all time trial records across all circuits.
     */
    suspend fun clearAllRecords() = withContext(ioDispatcher) {
        timeTrialRecordDao.deleteAllRecords()
    }
}
