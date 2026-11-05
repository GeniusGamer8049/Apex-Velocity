package com.example.data

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.DefaultGlobalLeaderboards
import com.example.data.room.GlobalLeaderboards
import com.example.data.room.GlobalLeaderboardsDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository abstracting [GlobalLeaderboards] Room database operations.
 * Manages top circuit lap times, benchmark racers, world record tracking,
 * and dynamic player ranking recalculation.
 */
class GlobalLeaderboardsRepository(
    val globalLeaderboardsDao: GlobalLeaderboardsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        globalLeaderboardsDao = AppDatabase.getDatabase(context).globalLeaderboardsDao()
    )

    /**
     * Seeds default benchmark circuit times into Room if the table is empty.
     */
    suspend fun ensureDefaultLeaderboards() = withContext(ioDispatcher) {
        val count = globalLeaderboardsDao.getTotalEntryCount()
        if (count == 0) {
            val defaults = DefaultGlobalLeaderboards.createDefaultRankings()
            globalLeaderboardsDao.insertDefaultEntries(defaults)
        }
    }

    /**
     * Observable flow of rankings for a specific circuit, sorted by rank and lap time.
     */
    fun getLeaderboardForTrackFlow(trackId: String): Flow<List<GlobalLeaderboards>> {
        return globalLeaderboardsDao.getLeaderboardForTrackFlow(trackId)
    }

    /**
     * Snapshot query of rankings for a circuit.
     */
    suspend fun getLeaderboardForTrack(trackId: String): List<GlobalLeaderboards> = withContext(ioDispatcher) {
        ensureDefaultLeaderboards()
        globalLeaderboardsDao.getLeaderboardForTrack(trackId)
    }

    /**
     * Observable flow of the top rankings (e.g. Top 5 / Top 10) for a circuit preview.
     */
    fun getTopRankingsForTrackFlow(trackId: String, limit: Int = 5): Flow<List<GlobalLeaderboards>> {
        return globalLeaderboardsDao.getTopRankingsForTrackFlow(trackId, limit)
    }

    /**
     * Observable flow of the player's recorded personal best entry on a circuit.
     */
    fun getPlayerEntryForTrackFlow(trackId: String): Flow<GlobalLeaderboards?> {
        return globalLeaderboardsDao.getPlayerEntryForTrackFlow(trackId)
    }

    /**
     * Observable flow of the #1 World Record on a circuit.
     */
    fun getWorldRecordForTrackFlow(trackId: String): Flow<GlobalLeaderboards?> {
        return globalLeaderboardsDao.getWorldRecordForTrackFlow(trackId)
    }

    /**
     * Records or updates a player's personal best lap time on a circuit in Room.
     * Recalculates ranks and gaps for all circuit entries dynamically.
     */
    suspend fun recordPlayerLapTime(
        trackId: String,
        trackName: String,
        lapTimeMs: Long,
        driverName: String,
        countryCode: String,
        vehicleName: String,
        vehicleCategory: String
    ): GlobalLeaderboards = withContext(ioDispatcher) {
        ensureDefaultLeaderboards()

        val existingPlayer = globalLeaderboardsDao.getPlayerEntryForTrack(trackId)
        if (existingPlayer != null && existingPlayer.lapTimeMs <= lapTimeMs) {
            return@withContext existingPlayer
        }

        // Remove old player entry if updating to a faster PB
        if (existingPlayer != null) {
            globalLeaderboardsDao.deletePlayerEntryForTrack(trackId)
        }

        // Fetch remaining benchmark entries for track
        val currentEntries = globalLeaderboardsDao.getLeaderboardForTrack(trackId).toMutableList()

        // Create player entry
        val playerEntry = GlobalLeaderboards(
            id = "${trackId}_player",
            trackId = trackId,
            trackName = trackName,
            rank = 0, // Assigned after sorting
            driverName = driverName.ifBlank { "Apex Pilot" },
            countryCode = countryCode.ifBlank { "US" },
            vehicleName = vehicleName,
            vehicleCategory = vehicleCategory,
            lapTimeMs = lapTimeMs,
            gapMs = 0L,
            isPlayer = true,
            tierName = "GOLD",
            dateRecorded = "Today"
        )
        currentEntries.add(playerEntry)

        // Sort all entries ascending by lap time
        currentEntries.sortBy { it.lapTimeMs }

        val leaderLapTime = currentEntries.first().lapTimeMs

        // Re-assign ranks, gaps, and tiers
        val updatedEntries = currentEntries.mapIndexed { index, entry ->
            val rank = index + 1
            val gap = entry.lapTimeMs - leaderLapTime
            val tier = when (rank) {
                1 -> "WORLD_RECORD"
                2, 3 -> "TOP_1_PERCENT"
                4, 5 -> "DIAMOND"
                else -> "GOLD"
            }
            entry.copy(
                rank = rank,
                gapMs = gap,
                tierName = tier
            )
        }

        globalLeaderboardsDao.insertEntries(updatedEntries)
        updatedEntries.first { it.isPlayer }
    }

    /**
     * Deletes all leaderboards (used in tests or profile resets).
     */
    suspend fun clearAllLeaderboards() = withContext(ioDispatcher) {
        globalLeaderboardsDao.deleteAllLeaderboards()
    }

    companion object {
        @Volatile
        private var INSTANCE: GlobalLeaderboardsRepository? = null

        fun getInstance(context: Context): GlobalLeaderboardsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = GlobalLeaderboardsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
