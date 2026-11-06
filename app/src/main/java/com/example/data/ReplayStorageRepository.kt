package com.example.data

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.ReplayStorage
import com.example.data.room.ReplayStorageDao
import com.example.model.GhostFrame
import com.example.model.GhostLap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository abstracting [ReplayStorage] Room operations from UI and game systems.
 * Provides caching, automatic default ghost benchmark seeding, and telemetry queries.
 */
class ReplayStorageRepository(
    val replayStorageDao: ReplayStorageDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        replayStorageDao = AppDatabase.getDatabase(context).replayStorageDao()
    )

    companion object {
        @Volatile
        private var INSTANCE: ReplayStorageRepository? = null

        fun getInstance(context: Context): ReplayStorageRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReplayStorageRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Seeds default benchmark circuit replays if database table is empty.
     */
    suspend fun ensureDefaultReplays() = withContext(ioDispatcher) {
        val count = replayStorageDao.getReplayCount()
        if (count == 0) {
            val defaults = ReplayStorage.createDefaultReplays()
            replayStorageDao.insertDefaultReplays(defaults)
        }
    }

    /**
     * Observable flow of all stored replays, ordered by recency.
     */
    val allReplaysFlow: Flow<List<ReplayStorage>> = replayStorageDao.getAllReplaysFlow()

    /**
     * Observable flow of replays for a specific circuit, ordered by fastest lap time.
     */
    fun getReplaysForTrackFlow(trackId: String): Flow<List<ReplayStorage>> {
        return replayStorageDao.getReplaysForTrackFlow(trackId)
    }

    /**
     * Observable flow of the best (fastest) replay for a given circuit.
     */
    fun getBestReplayForTrackFlow(trackId: String): Flow<ReplayStorage?> {
        return replayStorageDao.getBestReplayForTrackFlow(trackId)
    }

    /**
     * Fetches the fastest replay for a circuit, ensuring default benchmarks exist.
     */
    suspend fun getBestReplayForTrack(trackId: String): ReplayStorage? = withContext(ioDispatcher) {
        ensureDefaultReplays()
        replayStorageDao.getBestReplayForTrack(trackId)
    }

    /**
     * Saves a recorded ghost lap into Room database storage.
     */
    suspend fun saveGhostLapReplay(
        ghostLap: GhostLap,
        trackName: String,
        vehicleId: String = "apex_gt86",
        weatherType: String = "SUNNY_CLEAR",
        cleanLap: Boolean = true
    ): ReplayStorage = withContext(ioDispatcher) {
        val replay = ReplayStorage.fromGhostLap(
            ghostLap = ghostLap,
            trackName = trackName,
            vehicleId = vehicleId,
            weatherType = weatherType,
            cleanLap = cleanLap
        )
        replayStorageDao.insertReplay(replay)
        replay
    }

    /**
     * Inserts an explicit [ReplayStorage] record.
     */
    suspend fun insertReplay(replay: ReplayStorage) = withContext(ioDispatcher) {
        replayStorageDao.insertReplay(replay)
    }

    /**
     * Deletes a replay by ID.
     */
    suspend fun deleteReplay(id: String) = withContext(ioDispatcher) {
        replayStorageDao.deleteReplayById(id)
    }

    /**
     * Clears all stored replays.
     */
    suspend fun clearAllReplays() = withContext(ioDispatcher) {
        replayStorageDao.clearAllReplays()
    }

    /**
     * Returns total count of saved replays.
     */
    suspend fun getReplayCount(): Int = withContext(ioDispatcher) {
        replayStorageDao.getReplayCount()
    }
}
