package com.example.data

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.LocalTrackRecord
import com.example.data.room.LocalTrackRecordDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Result data class returned when recording a completed lap.
 */
data class RecordLapResult(
    val record: LocalTrackRecord,
    val isNewPersonalBest: Boolean,
    val previousBestLapTimeMs: Long?,
    val improvementDeltaMs: Long?
)

/**
 * Repository pattern implementation managing [LocalTrackRecord] persistent data,
 * computing personal best lap times, and tracking player circuit progression.
 */
class LocalTrackRecordRepository(
    private val dao: LocalTrackRecordDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        dao = AppDatabase.getDatabase(context).localTrackRecordDao()
    )

    /**
     * Flow of all circuit records ordered by track name.
     */
    val allTrackRecordsFlow: Flow<List<LocalTrackRecord>> = dao.getAllTrackRecordsFlow()

    /**
     * Flow of track records ordered from fastest to slowest best lap.
     */
    val fastestTracksFlow: Flow<List<LocalTrackRecord>> = dao.getFastestTracksFlow()

    /**
     * Flow of recently driven circuits.
     */
    val recentlyDrivenTracksFlow: Flow<List<LocalTrackRecord>> = dao.getRecentlyDrivenTracksFlow()

    /**
     * Observes a specific circuit record by [trackId].
     */
    fun getTrackRecordFlow(trackId: String): Flow<LocalTrackRecord?> {
        return dao.getTrackRecordFlow(trackId)
    }

    /**
     * Direct query for a specific circuit record.
     */
    suspend fun getTrackRecord(trackId: String): LocalTrackRecord? = withContext(ioDispatcher) {
        ensureDefaultRecords()
        dao.getTrackRecord(trackId)
    }

    /**
     * Seeds benchmark circuit records if database table is empty.
     */
    suspend fun ensureDefaultRecords() = withContext(ioDispatcher) {
        val count = dao.getRecordCount()
        if (count == 0) {
            val defaults = LocalTrackRecord.createDefaultRecords()
            dao.insertRecords(defaults)
        }
    }

    /**
     * Records a completed lap time and updates the player's track record and progress.
     * Updates personal best if [lapTimeMs] is faster than existing best time.
     */
    suspend fun recordLapTime(
        trackId: String,
        trackName: String,
        lapTimeMs: Long,
        vehicleId: String,
        vehicleName: String,
        topSpeedKmh: Float,
        isClean: Boolean = true,
        isWin: Boolean = false,
        isPodium: Boolean = false,
        weather: String = "SUNNY_CLEAR"
    ): RecordLapResult = withContext(ioDispatcher) {
        ensureDefaultRecords()
        val existing = dao.getTrackRecord(trackId)
        val now = System.currentTimeMillis()
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

        val isNewPB = existing == null || existing.bestLapTimeMs <= 0L || lapTimeMs < existing.bestLapTimeMs
        val previousPB = existing?.bestLapTimeMs

        val updatedRecord = if (existing == null) {
            LocalTrackRecord(
                trackId = trackId,
                trackName = trackName,
                bestLapTimeMs = lapTimeMs,
                bestLapVehicleId = vehicleId,
                bestLapVehicleName = vehicleName,
                topSpeedKmh = topSpeedKmh,
                totalLapsCompleted = 1,
                totalRacesFinished = if (isWin || isPodium) 1 else 0,
                totalWins = if (isWin) 1 else 0,
                totalPodiums = if (isPodium) 1 else 0,
                isCleanLap = isClean,
                weatherCondition = weather,
                previousBestLapTimeMs = null,
                lastDrivenTimestamp = now,
                dateRecorded = todayDate
            )
        } else {
            val newBestTime = if (isNewPB) lapTimeMs else existing.bestLapTimeMs
            val newVehicleId = if (isNewPB) vehicleId else existing.bestLapVehicleId
            val newVehicleName = if (isNewPB) vehicleName else existing.bestLapVehicleName
            val maxSpeed = maxOf(existing.topSpeedKmh, topSpeedKmh)
            val prevTime = if (isNewPB) existing.bestLapTimeMs else existing.previousBestLapTimeMs

            existing.copy(
                bestLapTimeMs = newBestTime,
                bestLapVehicleId = newVehicleId,
                bestLapVehicleName = newVehicleName,
                topSpeedKmh = maxSpeed,
                totalLapsCompleted = existing.totalLapsCompleted + 1,
                totalRacesFinished = existing.totalRacesFinished + (if (isWin || isPodium) 1 else 0),
                totalWins = existing.totalWins + (if (isWin) 1 else 0),
                totalPodiums = existing.totalPodiums + (if (isPodium) 1 else 0),
                isCleanLap = if (isNewPB) isClean else existing.isCleanLap,
                weatherCondition = if (isNewPB) weather else existing.weatherCondition,
                previousBestLapTimeMs = prevTime,
                lastDrivenTimestamp = now,
                dateRecorded = if (isNewPB) todayDate else existing.dateRecorded
            )
        }

        dao.insertRecord(updatedRecord)

        RecordLapResult(
            record = updatedRecord,
            isNewPersonalBest = isNewPB,
            previousBestLapTimeMs = previousPB,
            improvementDeltaMs = if (isNewPB && previousPB != null) previousPB - lapTimeMs else null
        )
    }

    /**
     * Resets records back to baseline benchmark defaults.
     */
    suspend fun resetToDefaultRecords() = withContext(ioDispatcher) {
        dao.deleteAllRecords()
        dao.insertRecords(LocalTrackRecord.createDefaultRecords())
    }

    companion object {
        @Volatile
        private var INSTANCE: LocalTrackRecordRepository? = null

        fun getInstance(context: Context): LocalTrackRecordRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = LocalTrackRecordRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
