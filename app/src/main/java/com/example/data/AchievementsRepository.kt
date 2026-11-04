package com.example.data

import android.content.Context
import com.example.data.room.Achievements
import com.example.data.room.AchievementsDao
import com.example.data.room.AppDatabase
import com.example.data.room.DefaultAchievements
import com.example.data.room.DriverProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository abstracting Achievements Room persistence and milestone tracking.
 * Manages player milestone achievements such as 'First Win', '100 Miles Driven',
 * and 'Clean Lap'.
 */
class AchievementsRepository(
    val achievementsDao: AchievementsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        achievementsDao = AppDatabase.getDatabase(context).achievementsDao()
    )

    val allAchievementsFlow: Flow<List<Achievements>> = achievementsDao.getAllAchievementsFlow()
    val unlockedAchievementsFlow: Flow<List<Achievements>> = achievementsDao.getUnlockedAchievementsFlow()
    val unlockedCountFlow: Flow<Int> = achievementsDao.getUnlockedCountFlow()
    val totalCountFlow: Flow<Int> = achievementsDao.getTotalCountFlow()

    /**
     * Initializes default milestone records into Room if the database is newly created.
     */
    suspend fun initDefaultAchievementsIfNeeded() = withContext(ioDispatcher) {
        achievementsDao.insertDefaultAchievements(DefaultAchievements.createDefaultList())
    }

    suspend fun getAllAchievements(): List<Achievements> = withContext(ioDispatcher) {
        initDefaultAchievementsIfNeeded()
        achievementsDao.getAllAchievements()
    }

    suspend fun getAchievement(id: String): Achievements? = withContext(ioDispatcher) {
        achievementsDao.getAchievementById(id)
    }

    suspend fun insertAchievement(achievement: Achievements) = withContext(ioDispatcher) {
        achievementsDao.insertAchievement(achievement)
    }

    suspend fun unlockAchievement(id: String) = withContext(ioDispatcher) {
        achievementsDao.unlockAchievement(id)
    }

    suspend fun updateProgress(id: String, progress: Float) = withContext(ioDispatcher) {
        achievementsDao.updateProgress(id, progress)
    }

    suspend fun deleteAchievement(id: String) = withContext(ioDispatcher) {
        achievementsDao.deleteAchievementById(id)
    }

    suspend fun deleteAllAchievements() = withContext(ioDispatcher) {
        achievementsDao.deleteAllAchievements()
    }

    /**
     * Evaluates and updates Room achievements based on player statistics.
     */
    suspend fun syncWithDriverProfile(profile: DriverProfile) = withContext(ioDispatcher) {
        initDefaultAchievementsIfNeeded()

        // 1. First Win milestone
        if (profile.totalWins >= 1) {
            achievementsDao.unlockAchievement(DefaultAchievements.ID_FIRST_WIN)
        } else {
            achievementsDao.updateProgress(DefaultAchievements.ID_FIRST_WIN, profile.totalWins.toFloat())
        }

        // 2. 100 Miles Driven milestone (160,934 meters)
        achievementsDao.updateProgress(DefaultAchievements.ID_100_MILES, profile.totalDistance.toFloat())

        // 3. Clean Lap milestone
        if (profile.cleanRacesCount >= 1) {
            achievementsDao.unlockAchievement(DefaultAchievements.ID_CLEAN_LAP)
        } else {
            achievementsDao.updateProgress(DefaultAchievements.ID_CLEAN_LAP, profile.cleanRacesCount.toFloat())
        }

        // 4. Podium Master
        val podiums = if (profile.podiumFinishes > 0) profile.podiumFinishes else profile.totalPodiums
        achievementsDao.updateProgress(DefaultAchievements.ID_PODIUM_MASTER, podiums.toFloat())

        // 5. Speed Demon
        achievementsDao.updateProgress(DefaultAchievements.ID_SPEED_DEMON, profile.highestSpeedKmh)

        // 6. Drift King
        achievementsDao.updateProgress(DefaultAchievements.ID_DRIFT_KING, profile.totalDriftDistance.toFloat())

        // 7. Wet Racer
        achievementsDao.updateProgress(DefaultAchievements.ID_WET_RACER, profile.wetRaceWinsCount.toFloat())
    }

    companion object {
        @Volatile
        private var INSTANCE: AchievementsRepository? = null

        fun getInstance(context: Context): AchievementsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AchievementsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
