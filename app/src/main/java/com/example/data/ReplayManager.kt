package com.example.data

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.ReplayStorage
import com.example.data.room.ReplayStorageDao
import com.example.model.GhostFrame
import com.example.model.GhostLap
import com.example.model.TrackRegistry
import com.example.physics.CarPhysicsState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Result data class produced when a completed lap is processed and stored in Room.
 */
data class ReplaySaveResult(
    val replay: ReplayStorage,
    val ghostLap: GhostLap,
    val isNewPersonalBest: Boolean,
    val previousBestLapTimeMs: Long?,
    val isNewTrackRecord: Boolean,
    val totalFrames: Int
)

/**
 * Real-time comparison delta between player and ghost car.
 */
data class GhostDeltaComparison(
    val playerLapTimeMs: Long,
    val ghostLapTimeMs: Long,
    val deltaSeconds: Float,
    val isAheadOfGhost: Boolean,
    val playerSpeedKmh: Float,
    val ghostSpeedKmh: Float,
    val distanceSeparationM: Float = 0f
)

/**
 * ReplayManager captures and stores race physics data to enable the 'ghost car'
 * time trial replay feature for completed laps using Room database persistence.
 */
class ReplayManager(
    val replayStorageDao: ReplayStorageDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        replayStorageDao = AppDatabase.getDatabase(context).replayStorageDao()
    )

    // =========================================================================
    // Reactive Streams & Caching
    // =========================================================================

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _activeGhostLap = MutableStateFlow<GhostLap?>(null)
    val activeGhostLap: StateFlow<GhostLap?> = _activeGhostLap.asStateFlow()

    private val _recordedFrameCount = MutableStateFlow(0)
    val recordedFrameCount: StateFlow<Int> = _recordedFrameCount.asStateFlow()

    // In-memory frame buffer for active lap
    private val frameBuffer = mutableListOf<GhostFrame>()
    private var lastRecordedTimeMs: Long = -1L

    // Active session metadata
    var activeTrackId: String = ""
        private set
    var activeTrackName: String = ""
        private set
    var activeVehicleId: String = "apex_gt86"
        private set
    var activeVehicleName: String = "Apex GT-86 Coupe"
        private set
    var activeWeatherType: String = "SUNNY_CLEAR"
        private set

    /**
     * Flow of all saved replays in Room, ordered by timestamp descending.
     */
    val allReplaysFlow: Flow<List<ReplayStorage>> = replayStorageDao.getAllReplaysFlow()

    /**
     * Flow of all saved replays for a specific circuit, ordered by fastest lap.
     */
    fun getReplaysForTrackFlow(trackId: String): Flow<List<ReplayStorage>> {
        return replayStorageDao.getReplaysForTrackFlow(trackId)
    }

    /**
     * Flow of the fastest replay record for a specific circuit.
     */
    fun getBestReplayForTrackFlow(trackId: String): Flow<ReplayStorage?> {
        return replayStorageDao.getBestReplayForTrackFlow(trackId)
    }

    // =========================================================================
    // Capture & Session Lifecycle
    // =========================================================================

    /**
     * Starts a new telemetry capture session for a circuit and vehicle.
     */
    fun startRecordingSession(
        trackId: String,
        trackName: String,
        vehicleId: String,
        vehicleName: String,
        weatherType: String = "SUNNY_CLEAR"
    ) {
        activeTrackId = trackId
        activeTrackName = trackName
        activeVehicleId = vehicleId
        activeVehicleName = vehicleName
        activeWeatherType = weatherType

        synchronized(frameBuffer) {
            frameBuffer.clear()
        }
        lastRecordedTimeMs = -1L
        _recordedFrameCount.value = 0
        _isRecording.value = true
    }

    /**
     * Captures a real-time race physics frame from [CarPhysicsState].
     * Downsamples at [sampleIntervalMs] (default 32ms ~= 30 FPS sampling)
     * while prioritizing critical telemetry events like braking or sharp heading change.
     */
    fun recordPhysicsState(state: CarPhysicsState, sampleIntervalMs: Long = 32L): Boolean {
        if (!_isRecording.value) return false

        val currentLapMs = state.currentLapTimeMs
        if (currentLapMs < 0) return false

        val shouldSample = lastRecordedTimeMs < 0 ||
                (currentLapMs - lastRecordedTimeMs) >= sampleIntervalMs ||
                state.isBraking ||
                abs(state.slipAngleDeg) > 12f

        if (shouldSample) {
            val frame = GhostFrame(
                timeMs = currentLapMs,
                x = state.x,
                y = state.y,
                heading = state.headingDeg,
                speedKmh = state.speedKmh,
                isBraking = state.isBraking
            )
            return recordFrame(frame)
        }
        return false
    }

    /**
     * Manually records an explicit [GhostFrame].
     */
    fun recordFrame(frame: GhostFrame): Boolean {
        if (!_isRecording.value) return false
        synchronized(frameBuffer) {
            frameBuffer.add(frame)
            lastRecordedTimeMs = frame.timeMs
            _recordedFrameCount.value = frameBuffer.size
        }
        return true
    }

    /**
     * Returns a snapshot of currently captured frames in the active buffer.
     */
    fun getRecordedFrames(): List<GhostFrame> {
        return synchronized(frameBuffer) {
            frameBuffer.toList()
        }
    }

    /**
     * Resets the active frame buffer for subsequent laps without stopping the session.
     */
    fun resetRecordingBuffer() {
        synchronized(frameBuffer) {
            frameBuffer.clear()
        }
        lastRecordedTimeMs = -1L
        _recordedFrameCount.value = 0
    }

    /**
     * Cancels the current capture session and releases recorded telemetry.
     */
    fun cancelRecordingSession() {
        _isRecording.value = false
        resetRecordingBuffer()
    }

    // =========================================================================
    // Completed Lap Storage & Room Persistence
    // =========================================================================

    /**
     * Processes a completed lap:
     * 1. Constructs a [GhostLap] from the captured race physics frames.
     * 2. Checks against existing circuit records in Room to determine if it is a new Personal Best.
     * 3. Stores the race physics data as a [ReplayStorage] Room record.
     * 4. Updates [activeGhostLap] to immediately enable ghost car replay for following laps.
     * 5. Cleans and resets the active frame buffer for the next lap.
     */
    suspend fun saveCompletedLap(
        lapTimeMs: Long,
        cleanLap: Boolean = true,
        customVehicleName: String? = null
    ): ReplaySaveResult? = withContext(ioDispatcher) {
        if (lapTimeMs <= 0L || activeTrackId.isBlank()) return@withContext null

        val framesSnapshot = getRecordedFrames()
        if (framesSnapshot.isEmpty()) return@withContext null

        ensureDefaultReplays()

        val vName = customVehicleName ?: activeVehicleName
        val ghostLap = GhostLap(
            trackId = activeTrackId,
            carName = vName,
            lapTimeMs = lapTimeMs,
            frames = framesSnapshot
        )

        // Query existing best replay from Room
        val existingBest = replayStorageDao.getBestReplayForTrack(activeTrackId)
        val prevBestTime = existingBest?.lapTimeMs
        val isNewPB = prevBestTime == null || lapTimeMs < prevBestTime
        val isTrackRecord = isNewPB && (existingBest == null || lapTimeMs < existingBest.lapTimeMs)

        // Create Room entity
        val tName = if (activeTrackName.isNotBlank()) activeTrackName else TrackRegistry.getTrackById(activeTrackId).name
        val replayEntity = ReplayStorage.fromGhostLap(
            ghostLap = ghostLap,
            trackName = tName,
            vehicleId = activeVehicleId,
            weatherType = activeWeatherType,
            cleanLap = cleanLap
        )

        // Persist to Room database
        replayStorageDao.insertReplay(replayEntity)

        // Update in-memory active ghost car if it represents a personal best
        if (isNewPB) {
            _activeGhostLap.value = ghostLap
        }

        // Prepare frame buffer for next lap
        resetRecordingBuffer()

        ReplaySaveResult(
            replay = replayEntity,
            ghostLap = ghostLap,
            isNewPersonalBest = isNewPB,
            previousBestLapTimeMs = prevBestTime,
            isNewTrackRecord = isTrackRecord,
            totalFrames = framesSnapshot.size
        )
    }

    // =========================================================================
    // Ghost Car Time Trial Replay Feature
    // =========================================================================

    /**
     * Seeds initial default benchmark circuit ghost replays if Room is empty.
     */
    suspend fun ensureDefaultReplays() = withContext(ioDispatcher) {
        val count = replayStorageDao.getReplayCount()
        if (count == 0) {
            val defaults = ReplayStorage.createDefaultReplays()
            replayStorageDao.insertDefaultReplays(defaults)
        }
    }

    /**
     * Loads the fastest stored ghost lap for [trackId] from Room and updates [activeGhostLap].
     */
    suspend fun loadGhostLapForTrack(trackId: String): GhostLap? = withContext(ioDispatcher) {
        ensureDefaultReplays()
        val bestReplay = replayStorageDao.getBestReplayForTrack(trackId)
        val ghost = bestReplay?.toGhostLap()
        _activeGhostLap.value = ghost
        ghost
    }

    /**
     * Loads a specific ghost lap by replay ID from Room.
     */
    suspend fun loadGhostLapByReplayId(replayId: String): GhostLap? = withContext(ioDispatcher) {
        val replay = replayStorageDao.getReplayById(replayId)
        replay?.toGhostLap()
    }

    /**
     * Samples the ghost car's coordinates at [currentLapTimeMs] using spline interpolation.
     */
    fun sampleActiveGhost(currentLapTimeMs: Long): GhostFrame? {
        val ghost = _activeGhostLap.value ?: return null
        return ghost.sampleAt(currentLapTimeMs)
    }

    /**
     * Computes the real-time split delta between player and ghost car.
     */
    fun compareWithGhost(
        playerLapTimeMs: Long,
        playerSpeedKmh: Float,
        playerX: Float,
        playerY: Float
    ): GhostDeltaComparison? {
        val ghost = _activeGhostLap.value ?: return null
        val ghostFrame = ghost.sampleAt(playerLapTimeMs) ?: return null

        val deltaMs = playerLapTimeMs - ghostFrame.timeMs
        val deltaSec = deltaMs / 1000f
        val isAhead = deltaSec <= 0f

        val dx = playerX - ghostFrame.x
        val dy = playerY - ghostFrame.y
        val distMeters = kotlin.math.sqrt(dx * dx + dy * dy)

        return GhostDeltaComparison(
            playerLapTimeMs = playerLapTimeMs,
            ghostLapTimeMs = ghostFrame.timeMs,
            deltaSeconds = deltaSec,
            isAheadOfGhost = isAhead,
            playerSpeedKmh = playerSpeedKmh,
            ghostSpeedKmh = ghostFrame.speedKmh,
            distanceSeparationM = distMeters
        )
    }

    /**
     * Explicitly sets or overrides the active ghost lap for time trial playback.
     */
    fun setActiveGhostLap(ghostLap: GhostLap?) {
        _activeGhostLap.value = ghostLap
    }

    /**
     * Fetches the fastest replay record for a circuit.
     */
    suspend fun getBestReplayForTrack(trackId: String): ReplayStorage? = withContext(ioDispatcher) {
        ensureDefaultReplays()
        replayStorageDao.getBestReplayForTrack(trackId)
    }

    /**
     * Returns total count of saved replays in Room.
     */
    suspend fun getReplayCount(): Int = withContext(ioDispatcher) {
        replayStorageDao.getReplayCount()
    }

    /**
     * Deletes a replay record from Room by ID.
     */
    suspend fun deleteReplay(id: String) = withContext(ioDispatcher) {
        replayStorageDao.deleteReplayById(id)
    }

    /**
     * Clears all replays from Room.
     */
    suspend fun clearAllReplays() = withContext(ioDispatcher) {
        replayStorageDao.clearAllReplays()
    }
}
