package com.example.data

import android.content.Context
import com.example.data.room.Achievements
import com.example.data.room.AchievementsDao
import com.example.data.room.AppDatabase
import com.example.data.room.DefaultAchievements
import com.example.data.room.DriverProfileDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Event emitted when a milestone achievement is unlocked and rewarded.
 */
data class MilestoneUnlockEvent(
    val achievement: Achievements,
    val rewardCredits: Int,
    val rewardRep: Int,
    val isFirstUnlock: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Result data class when an achievement reward is claimed.
 */
data class RewardClaimResult(
    val achievementId: String,
    val achievementTitle: String,
    val creditsAwarded: Int,
    val repAwarded: Int,
    val success: Boolean
)

/**
 * AchievementManager module using Room database to track and reward player milestones,
 * such as winning their first race or achieving specific top speeds.
 */
class AchievementManager(
    val achievementsDao: AchievementsDao,
    val driverProfileDao: DriverProfileDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        achievementsDao = AppDatabase.getDatabase(context).achievementsDao(),
        driverProfileDao = AppDatabase.getDatabase(context).driverProfileDao()
    )

    /**
     * Flow of all achievements sorted by unlock status.
     */
    val allAchievementsFlow: Flow<List<Achievements>> = achievementsDao.getAllAchievementsFlow()

    /**
     * Flow of only unlocked achievements.
     */
    val unlockedAchievementsFlow: Flow<List<Achievements>> = achievementsDao.getUnlockedAchievementsFlow()

    /**
     * Flow of total number of unlocked achievements.
     */
    val unlockedCountFlow: Flow<Int> = achievementsDao.getUnlockedCountFlow()

    /**
     * Flow of total achievements count.
     */
    val totalCountFlow: Flow<Int> = achievementsDao.getTotalCountFlow()

    /**
     * Ensures initial default milestone achievements are seeded in Room database.
     */
    suspend fun ensureDefaultAchievements() = withContext(ioDispatcher) {
        achievementsDao.insertDefaultAchievements(DefaultAchievements.createDefaultList())
    }

    /**
     * Returns snapshot list of all achievements.
     */
    suspend fun getAllAchievements(): List<Achievements> = withContext(ioDispatcher) {
        ensureDefaultAchievements()
        achievementsDao.getAllAchievements()
    }

    /**
     * Returns snapshot list of unlocked achievements.
     */
    suspend fun getUnlockedAchievements(): List<Achievements> = withContext(ioDispatcher) {
        ensureDefaultAchievements()
        achievementsDao.getUnlockedAchievements()
    }

    /**
     * Retrieves an achievement by its unique ID.
     */
    suspend fun getAchievement(id: String): Achievements? = withContext(ioDispatcher) {
        ensureDefaultAchievements()
        achievementsDao.getAchievementById(id)
    }

    /**
     * Checks if a specific achievement is unlocked.
     */
    suspend fun isUnlocked(id: String): Boolean = withContext(ioDispatcher) {
        val achievement = getAchievement(id)
        achievement?.isUnlocked == true
    }

    /**
     * Tracks and rewards player race milestones based on the outcome of a race event:
     * - First win (`first_win`)
     * - Top speed achieved (`speed_demon` 250 km/h, `speed_supersonic` 300 km/h)
     * - Clean lap (`clean_lap`)
     * - Podium finish (`podium_master`)
     * - Wet race victory (`wet_racer`)
     * - Distance driven (`100_miles_driven`)
     * - Drift distance (`drift_king`)
     *
     * Automatically credits reward credits and reputation to the player profile upon unlock.
     * Returns a list of newly unlocked milestone events.
     */
    suspend fun trackRaceMilestones(
        isWin: Boolean,
        topSpeedKmh: Float,
        isClean: Boolean,
        isPodium: Boolean,
        isWetWin: Boolean,
        distanceMeters: Float = 0f,
        driftMeters: Float = 0f,
        totalWins: Int = 0,
        driverId: String = "primary_driver"
    ): List<MilestoneUnlockEvent> = withContext(ioDispatcher) {
        ensureDefaultAchievements()
        val unlockedEvents = mutableListOf<MilestoneUnlockEvent>()

        // 1. First Race Win Milestone
        if (isWin || totalWins >= 1) {
            val firstWin = achievementsDao.getAchievementById(DefaultAchievements.ID_FIRST_WIN)
            if (firstWin != null && !firstWin.isUnlocked) {
                achievementsDao.unlockAchievement(DefaultAchievements.ID_FIRST_WIN)
                rewardPlayer(firstWin.rewardCredits, firstWin.rewardRep, driverId)
                unlockedEvents.add(MilestoneUnlockEvent(firstWin.copy(isUnlocked = true), firstWin.rewardCredits, firstWin.rewardRep))
            } else if (firstWin != null && !firstWin.isUnlocked) {
                achievementsDao.updateProgress(DefaultAchievements.ID_FIRST_WIN, if (isWin) 1f else 0f)
            }
        }

        // 2. Specific Top Speed Milestones (250 km/h and 300 km/h)
        val speedDemon = achievementsDao.getAchievementById(DefaultAchievements.ID_SPEED_DEMON)
        if (speedDemon != null) {
            val currentProg = speedDemon.currentProgress.coerceAtLeast(topSpeedKmh)
            achievementsDao.updateProgress(DefaultAchievements.ID_SPEED_DEMON, currentProg)
            if (topSpeedKmh >= speedDemon.targetProgress && !speedDemon.isUnlocked) {
                achievementsDao.unlockAchievement(DefaultAchievements.ID_SPEED_DEMON)
                rewardPlayer(speedDemon.rewardCredits, speedDemon.rewardRep, driverId)
                unlockedEvents.add(MilestoneUnlockEvent(speedDemon.copy(isUnlocked = true), speedDemon.rewardCredits, speedDemon.rewardRep))
            }
        }

        val supersonic = achievementsDao.getAchievementById(DefaultAchievements.ID_SPEED_SUPERSONIC)
        if (supersonic != null) {
            val currentProg = supersonic.currentProgress.coerceAtLeast(topSpeedKmh)
            achievementsDao.updateProgress(DefaultAchievements.ID_SPEED_SUPERSONIC, currentProg)
            if (topSpeedKmh >= supersonic.targetProgress && !supersonic.isUnlocked) {
                achievementsDao.unlockAchievement(DefaultAchievements.ID_SPEED_SUPERSONIC)
                rewardPlayer(supersonic.rewardCredits, supersonic.rewardRep, driverId)
                unlockedEvents.add(MilestoneUnlockEvent(supersonic.copy(isUnlocked = true), supersonic.rewardCredits, supersonic.rewardRep))
            }
        }

        // 3. Clean Lap Milestone
        if (isClean) {
            val cleanLap = achievementsDao.getAchievementById(DefaultAchievements.ID_CLEAN_LAP)
            if (cleanLap != null && !cleanLap.isUnlocked) {
                achievementsDao.unlockAchievement(DefaultAchievements.ID_CLEAN_LAP)
                rewardPlayer(cleanLap.rewardCredits, cleanLap.rewardRep, driverId)
                unlockedEvents.add(MilestoneUnlockEvent(cleanLap.copy(isUnlocked = true), cleanLap.rewardCredits, cleanLap.rewardRep))
            }
        }

        // 4. Podium Master Milestone (5 podium finishes)
        if (isPodium) {
            val podiumMaster = achievementsDao.getAchievementById(DefaultAchievements.ID_PODIUM_MASTER)
            if (podiumMaster != null && !podiumMaster.isUnlocked) {
                val nextCount = podiumMaster.currentProgress + 1f
                achievementsDao.updateProgress(DefaultAchievements.ID_PODIUM_MASTER, nextCount)
                if (nextCount >= podiumMaster.targetProgress) {
                    achievementsDao.unlockAchievement(DefaultAchievements.ID_PODIUM_MASTER)
                    rewardPlayer(podiumMaster.rewardCredits, podiumMaster.rewardRep, driverId)
                    unlockedEvents.add(MilestoneUnlockEvent(podiumMaster.copy(isUnlocked = true), podiumMaster.rewardCredits, podiumMaster.rewardRep))
                }
            }
        }

        // 5. Wet Weather Victory Milestone
        if (isWetWin) {
            val wetRacer = achievementsDao.getAchievementById(DefaultAchievements.ID_WET_RACER)
            if (wetRacer != null && !wetRacer.isUnlocked) {
                achievementsDao.unlockAchievement(DefaultAchievements.ID_WET_RACER)
                rewardPlayer(wetRacer.rewardCredits, wetRacer.rewardRep, driverId)
                unlockedEvents.add(MilestoneUnlockEvent(wetRacer.copy(isUnlocked = true), wetRacer.rewardCredits, wetRacer.rewardRep))
            }
        }

        // 6. Cumulative Distance Driven (100 miles = 160,934 meters)
        if (distanceMeters > 0f) {
            val milesDriven = achievementsDao.getAchievementById(DefaultAchievements.ID_100_MILES)
            if (milesDriven != null && !milesDriven.isUnlocked) {
                val newDist = milesDriven.currentProgress + distanceMeters
                achievementsDao.updateProgress(DefaultAchievements.ID_100_MILES, newDist)
                if (newDist >= milesDriven.targetProgress) {
                    achievementsDao.unlockAchievement(DefaultAchievements.ID_100_MILES)
                    rewardPlayer(milesDriven.rewardCredits, milesDriven.rewardRep, driverId)
                    unlockedEvents.add(MilestoneUnlockEvent(milesDriven.copy(isUnlocked = true), milesDriven.rewardCredits, milesDriven.rewardRep))
                }
            }
        }

        // 7. Cumulative Drift Distance (1,000 meters)
        if (driftMeters > 0f) {
            val driftKing = achievementsDao.getAchievementById(DefaultAchievements.ID_DRIFT_KING)
            if (driftKing != null && !driftKing.isUnlocked) {
                val newDrift = driftKing.currentProgress + driftMeters
                achievementsDao.updateProgress(DefaultAchievements.ID_DRIFT_KING, newDrift)
                if (newDrift >= driftKing.targetProgress) {
                    achievementsDao.unlockAchievement(DefaultAchievements.ID_DRIFT_KING)
                    rewardPlayer(driftKing.rewardCredits, driftKing.rewardRep, driverId)
                    unlockedEvents.add(MilestoneUnlockEvent(driftKing.copy(isUnlocked = true), driftKing.rewardCredits, driftKing.rewardRep))
                }
            }
        }

        unlockedEvents
    }

    /**
     * Evaluates vehicle tuning upgrade stage milestone.
     */
    suspend fun trackTuningMilestone(maxStage: Int, driverId: String = "primary_driver"): MilestoneUnlockEvent? = withContext(ioDispatcher) {
        ensureDefaultAchievements()
        val tuningPro = achievementsDao.getAchievementById(DefaultAchievements.ID_TUNING_PRO) ?: return@withContext null
        if (!tuningPro.isUnlocked) {
            achievementsDao.updateProgress(DefaultAchievements.ID_TUNING_PRO, maxStage.toFloat())
            if (maxStage >= tuningPro.targetProgress) {
                achievementsDao.unlockAchievement(DefaultAchievements.ID_TUNING_PRO)
                rewardPlayer(tuningPro.rewardCredits, tuningPro.rewardRep, driverId)
                return@withContext MilestoneUnlockEvent(tuningPro.copy(isUnlocked = true), tuningPro.rewardCredits, tuningPro.rewardRep)
            }
        }
        null
    }

    /**
     * Claims reward for an unlocked achievement if not already claimed.
     */
    suspend fun claimAchievementReward(id: String, driverId: String = "primary_driver"): RewardClaimResult = withContext(ioDispatcher) {
        ensureDefaultAchievements()
        val achievement = achievementsDao.getAchievementById(id)
            ?: return@withContext RewardClaimResult(id, "Unknown", 0, 0, false)

        if (!achievement.isUnlocked) {
            return@withContext RewardClaimResult(id, achievement.title, 0, 0, false)
        }

        if (achievement.isRewardClaimed) {
            return@withContext RewardClaimResult(id, achievement.title, 0, 0, false)
        }

        achievementsDao.markRewardClaimed(id)
        rewardPlayer(achievement.rewardCredits, achievement.rewardRep, driverId)

        RewardClaimResult(
            achievementId = id,
            achievementTitle = achievement.title,
            creditsAwarded = achievement.rewardCredits,
            repAwarded = achievement.rewardRep,
            success = true
        )
    }

    /**
     * Internal helper to credit rewards to player profile.
     */
    private suspend fun rewardPlayer(credits: Int, rep: Int, driverId: String) {
        driverProfileDao?.addCreditsAndRep(
            id = driverId,
            credits = credits,
            rep = rep,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Clears and resets all achievements back to default locked state.
     */
    suspend fun resetAchievements() = withContext(ioDispatcher) {
        achievementsDao.deleteAllAchievements()
        ensureDefaultAchievements()
    }
}
