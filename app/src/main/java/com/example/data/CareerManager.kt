package com.example.data

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.CareerProgress
import com.example.data.room.CareerProgressDao
import com.example.model.CareerRegistry
import com.example.model.TrackRegistry
import com.example.model.VehicleRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Breakdown of XP earned from a single race event.
 */
data class CareerXpBreakdown(
    val baseXp: Int,
    val cleanRaceBonus: Int,
    val driftBonus: Int,
    val podiumBonus: Int,
    val totalXp: Int
)

/**
 * Encapsulates driver level status, progress bar values, and title.
 */
data class LevelProgress(
    val currentLevel: Int,
    val currentLevelTitle: String,
    val currentLevelXp: Int,
    val xpRequiredForNextLevel: Int,
    val progressPercentage: Float,
    val isMaxLevel: Boolean
)

/**
 * Result returned after processing a completed race event.
 */
data class CareerUpdateResult(
    val xpGained: Int,
    val xpBreakdown: CareerXpBreakdown,
    val previousXp: Int,
    val newTotalXp: Int,
    val previousLevel: Int,
    val newLevel: Int,
    val didLevelUp: Boolean,
    val newlyUnlockedTracks: List<String>,
    val newlyUnlockedTiers: List<String>,
    val newlyUnlockedVehicles: List<String>,
    val newTrophyEarned: String? = null,
    val updatedProgress: CareerProgress
)

/**
 * CareerManager manages driver experience points (XP), driver levels,
 * unlocked tracks, and vehicle progression tiers stored in Room database.
 */
class CareerManager(
    val careerProgressDao: CareerProgressDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        careerProgressDao = AppDatabase.getDatabase(context).careerProgressDao()
    )

    /**
     * Flow of active career progression from Room database.
     */
    val careerProgressFlow: Flow<CareerProgress?> = careerProgressDao.getCareerProgressFlow("primary_driver")

    // ========================================================================
    // Level Progression Configuration
    // ========================================================================

    companion object {
        // Cumulative XP required to reach each level (Levels 1 to 10)
        val LEVEL_THRESHOLDS = listOf(
            0,     // Level 1: Rookie Driver
            300,   // Level 2: Clubman Racer
            700,   // Level 3: Pro-Am Competitor
            1200,  // Level 4: Supercar Contender
            1800,  // Level 5: Grand Prix Master
            2500,  // Level 6: Apex Veteran
            3300,  // Level 7: Speedway Legend
            4200,  // Level 8: Championship Titan
            5200,  // Level 9: World Class Elite
            6500   // Level 10: Apex Motorsport Legend (Max)
        )

        val LEVEL_TITLES = listOf(
            "Rookie Driver",
            "Clubman Racer",
            "Pro-Am Competitor",
            "Supercar Contender",
            "Grand Prix Master",
            "Apex Veteran",
            "Speedway Legend",
            "Championship Titan",
            "World Class Elite",
            "Apex Motorsport Legend"
        )

        // Track unlock level requirements
        val TRACK_UNLOCK_LEVELS = mapOf(
            "monaco_riviera" to 1,
            "apex_speedway" to 1,
            "neon_tokyo" to 2,
            "mount_akina" to 3
        )

        // Tier unlock level requirements
        val TIER_UNLOCK_LEVELS = mapOf(
            "tier_1" to 1,
            "tier_2" to 2,
            "tier_3" to 3,
            "tier_4" to 4
        )

        // Vehicle unlock level requirements
        val VEHICLE_UNLOCK_LEVELS = mapOf(
            "apex_gt86" to 1,
            "silvia_s15" to 2,
            "evo_rally" to 2,
            "viper_gt" to 3,
            "veneno_hyper" to 4,
            "apex_f1" to 5
        )
    }

    /**
     * Calculates the driver level (1..10) corresponding to a total XP amount.
     */
    fun getLevelForXp(xp: Int): Int {
        for (i in LEVEL_THRESHOLDS.indices.reversed()) {
            if (xp >= LEVEL_THRESHOLDS[i]) {
                return i + 1
            }
        }
        return 1
    }

    /**
     * Returns cumulative XP threshold needed to reach [level].
     */
    fun getXpThresholdForLevel(level: Int): Int {
        val index = (level - 1).coerceIn(0, LEVEL_THRESHOLDS.size - 1)
        return LEVEL_THRESHOLDS[index]
    }

    /**
     * Returns human-readable driver title for [level].
     */
    fun getTitleForLevel(level: Int): String {
        val index = (level - 1).coerceIn(0, LEVEL_TITLES.size - 1)
        return LEVEL_TITLES[index]
    }

    /**
     * Computes the level progress bar details for UI display.
     */
    fun getLevelProgress(xp: Int): LevelProgress {
        val level = getLevelForXp(xp)
        val isMax = level >= LEVEL_THRESHOLDS.size
        val currentLevelThreshold = getXpThresholdForLevel(level)

        if (isMax) {
            return LevelProgress(
                currentLevel = level,
                currentLevelTitle = getTitleForLevel(level),
                currentLevelXp = xp - currentLevelThreshold,
                xpRequiredForNextLevel = 0,
                progressPercentage = 1.0f,
                isMaxLevel = true
            )
        }

        val nextLevelThreshold = getXpThresholdForLevel(level + 1)
        val levelRange = (nextLevelThreshold - currentLevelThreshold).coerceAtLeast(1)
        val currentXpInLevel = (xp - currentLevelThreshold).coerceIn(0, levelRange)
        val progress = (currentXpInLevel.toFloat() / levelRange.toFloat()).coerceIn(0f, 1f)

        return LevelProgress(
            currentLevel = level,
            currentLevelTitle = getTitleForLevel(level),
            currentLevelXp = currentXpInLevel,
            xpRequiredForNextLevel = levelRange,
            progressPercentage = progress,
            isMaxLevel = false
        )
    }

    /**
     * Calculates XP gain breakdown from a race outcome.
     */
    fun calculateXpGain(position: Int, isClean: Boolean, driftScore: Int): CareerXpBreakdown {
        val baseXp = when (position) {
            1 -> 250
            2 -> 175
            3 -> 120
            else -> 60
        }
        val cleanBonus = if (isClean) 50 else 0
        val driftBonus = if (driftScore >= 500) 30 else 0
        val podiumBonus = if (position in 1..3) 40 else 0
        val totalXp = baseXp + cleanBonus + driftBonus + podiumBonus

        return CareerXpBreakdown(
            baseXp = baseXp,
            cleanRaceBonus = cleanBonus,
            driftBonus = driftBonus,
            podiumBonus = podiumBonus,
            totalXp = totalXp
        )
    }

    // ========================================================================
    // Persistence & Race Processing
    // ========================================================================

    /**
     * Loads current career progress from Room, or seeds default if none exists.
     */
    suspend fun getCareerProgress(driverId: String = "primary_driver"): CareerProgress = withContext(ioDispatcher) {
        val existing = careerProgressDao.getCareerProgress(driverId)
        if (existing != null) {
            existing
        } else {
            val defaultProgress = CareerProgress.defaultProgression()
            careerProgressDao.insertOrUpdate(defaultProgress)
            defaultProgress
        }
    }

    /**
     * Processes race results, evaluates XP gain, detects level-up,
     * unlocks eligible tracks and vehicle progression tiers, and updates Room.
     */
    suspend fun processRaceResult(
        position: Int,
        totalRacers: Int = 4,
        trackId: String,
        isClean: Boolean,
        driftScore: Int,
        careerEventId: String? = null,
        driverId: String = "primary_driver"
    ): CareerUpdateResult = withContext(ioDispatcher) {
        val current = getCareerProgress(driverId)
        val xpBreakdown = calculateXpGain(position, isClean, driftScore)
        val newXp = current.experiencePoints + xpBreakdown.totalXp
        val previousLevel = current.driverLevel
        val newLevel = getLevelForXp(newXp)
        val didLevelUp = newLevel > previousLevel

        // Determine track unlocks
        val currentTracks = current.getTrackIdList().toMutableSet()
        val newlyUnlockedTracks = mutableListOf<String>()
        for ((track, reqLevel) in TRACK_UNLOCK_LEVELS) {
            if (newLevel >= reqLevel && !currentTracks.contains(track)) {
                currentTracks.add(track)
                newlyUnlockedTracks.add(track)
            }
        }

        // Determine tier unlocks
        val currentTiers = current.getTierIdList().toMutableSet()
        val newlyUnlockedTiers = mutableListOf<String>()
        for ((tier, reqLevel) in TIER_UNLOCK_LEVELS) {
            if (newLevel >= reqLevel && !currentTiers.contains(tier)) {
                currentTiers.add(tier)
                newlyUnlockedTiers.add(tier)
            }
        }

        // Determine vehicle unlocks
        val currentVehicles = current.getVehicleIdList().toMutableSet()
        val newlyUnlockedVehicles = mutableListOf<String>()
        for ((vehicle, reqLevel) in VEHICLE_UNLOCK_LEVELS) {
            if (newLevel >= reqLevel && !currentVehicles.contains(vehicle)) {
                currentVehicles.add(vehicle)
                newlyUnlockedVehicles.add(vehicle)
            }
        }

        // Determine career event completion and trophies
        val completedEvents = current.getCompletedEventIdList().toMutableSet()
        var newTrophyName: String? = null
        var updatedTrophies = current.trophyCount

        if (careerEventId != null) {
            completedEvents.add(careerEventId)

            // Check if this completion unlocked a tier championship trophy
            val allTiers = CareerRegistry.createDefaultTiers()
            for (tier in allTiers) {
                val tierEvents = tier.events.map { it.id }
                if (tierEvents.isNotEmpty() && tierEvents.all { completedEvents.contains(it) }) {
                    // Check if newly completed
                    val previousTierCompleted = tierEvents.all { current.getCompletedEventIdList().contains(it) }
                    if (!previousTierCompleted) {
                        newTrophyName = tier.trophyName
                        updatedTrophies++
                    }
                }
            }
        }

        // Determine current tier (highest unlocked tier)
        val highestTier = "tier_$newLevel".let { tierCandidate ->
            if (currentTiers.contains(tierCandidate)) tierCandidate else current.currentTierId
        }

        val updatedProgress = current.copy(
            experiencePoints = newXp,
            driverLevel = newLevel,
            unlockedTrackIds = currentTracks.joinToString(","),
            unlockedTierIds = currentTiers.joinToString(","),
            currentTierId = highestTier,
            unlockedVehicleIds = currentVehicles.joinToString(","),
            completedEventIds = completedEvents.joinToString(","),
            trophyCount = updatedTrophies,
            totalRacesFinished = current.totalRacesFinished + 1,
            totalWins = current.totalWins + (if (position == 1) 1 else 0),
            totalPodiums = current.totalPodiums + (if (position in 1..3) 1 else 0),
            lastUpdatedTimestamp = System.currentTimeMillis()
        )

        careerProgressDao.insertOrUpdate(updatedProgress)

        CareerUpdateResult(
            xpGained = xpBreakdown.totalXp,
            xpBreakdown = xpBreakdown,
            previousXp = current.experiencePoints,
            newTotalXp = newXp,
            previousLevel = previousLevel,
            newLevel = newLevel,
            didLevelUp = didLevelUp,
            newlyUnlockedTracks = newlyUnlockedTracks,
            newlyUnlockedTiers = newlyUnlockedTiers,
            newlyUnlockedVehicles = newlyUnlockedVehicles,
            newTrophyEarned = newTrophyName,
            updatedProgress = updatedProgress
        )
    }

    /**
     * Checks if a track is unlocked in Room persistence.
     */
    suspend fun isTrackUnlocked(trackId: String, driverId: String = "primary_driver"): Boolean = withContext(ioDispatcher) {
        val progress = getCareerProgress(driverId)
        progress.getTrackIdList().contains(trackId)
    }

    /**
     * Checks if a career tier is unlocked.
     */
    suspend fun isTierUnlocked(tierId: String, driverId: String = "primary_driver"): Boolean = withContext(ioDispatcher) {
        val progress = getCareerProgress(driverId)
        progress.getTierIdList().contains(tierId)
    }

    /**
     * Checks if a vehicle is unlocked in career progression.
     */
    suspend fun isVehicleUnlocked(vehicleId: String, driverId: String = "primary_driver"): Boolean = withContext(ioDispatcher) {
        val progress = getCareerProgress(driverId)
        progress.getVehicleIdList().contains(vehicleId)
    }

    /**
     * Manually unlocks a track (e.g. from special rewards or events).
     */
    suspend fun unlockTrackManually(trackId: String, driverId: String = "primary_driver") = withContext(ioDispatcher) {
        val current = getCareerProgress(driverId)
        val tracks = current.getTrackIdList().toMutableSet()
        if (tracks.add(trackId)) {
            val updated = current.copy(
                unlockedTrackIds = tracks.joinToString(","),
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            careerProgressDao.insertOrUpdate(updated)
        }
    }

    /**
     * Manually unlocks a tier.
     */
    suspend fun unlockTierManually(tierId: String, driverId: String = "primary_driver") = withContext(ioDispatcher) {
        val current = getCareerProgress(driverId)
        val tiers = current.getTierIdList().toMutableSet()
        if (tiers.add(tierId)) {
            val updated = current.copy(
                unlockedTierIds = tiers.joinToString(","),
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            careerProgressDao.insertOrUpdate(updated)
        }
    }

    /**
     * Manually unlocks a vehicle progression tier.
     */
    suspend fun unlockVehicleManually(vehicleId: String, driverId: String = "primary_driver") = withContext(ioDispatcher) {
        val current = getCareerProgress(driverId)
        val vehicles = current.getVehicleIdList().toMutableSet()
        if (vehicles.add(vehicleId)) {
            val updated = current.copy(
                unlockedVehicleIds = vehicles.joinToString(","),
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            careerProgressDao.insertOrUpdate(updated)
        }
    }

    /**
     * Resets career progression back to default Level 1 status.
     */
    suspend fun resetCareerProgress(driverId: String = "primary_driver") = withContext(ioDispatcher) {
        val default = CareerProgress.defaultProgression()
        careerProgressDao.insertOrUpdate(default)
    }
}
