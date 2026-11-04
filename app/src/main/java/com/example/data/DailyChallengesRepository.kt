package com.example.data

import android.content.Context
import com.example.data.room.AppDatabase
import com.example.data.room.DailyChallenges
import com.example.data.room.DailyChallengesDao
import com.example.data.room.DailyChallengesGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository abstracting [DailyChallenges] Room entity persistence and task progression.
 * Generates and tracks limited-time daily objectives, progression, and reward claims.
 */
class DailyChallengesRepository(
    val dailyChallengesDao: DailyChallengesDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        dailyChallengesDao = AppDatabase.getDatabase(context).dailyChallengesDao()
    )

    fun getTodayKey(): String = DailyChallengesGenerator.getTodayKey()

    val todayChallengesFlow: Flow<List<DailyChallenges>> =
        dailyChallengesDao.getChallengesForDateFlow(DailyChallengesGenerator.getTodayKey())

    val activeChallengesFlow: Flow<List<DailyChallenges>> =
        dailyChallengesDao.getActiveChallengesFlow()

    val allChallengesFlow: Flow<List<DailyChallenges>> =
        dailyChallengesDao.getAllChallengesFlow()

    /**
     * Ensures challenges exist for the given or current date. If none exist,
     * generates and inserts 3 fresh deterministic tasks.
     */
    suspend fun ensureTodayChallenges(dateKey: String = getTodayKey()): List<DailyChallenges> = withContext(ioDispatcher) {
        val existing = dailyChallengesDao.getChallengesForDate(dateKey)
        if (existing.isNotEmpty()) {
            existing
        } else {
            val generated = DailyChallengesGenerator.generateDefaultChallengesForDate(dateKey)
            dailyChallengesDao.insertDefaultChallenges(generated)
            dailyChallengesDao.getChallengesForDate(dateKey)
        }
    }

    /**
     * Retrieves active challenges, initializing today's tasks first if necessary.
     */
    suspend fun getActiveChallenges(): List<DailyChallenges> = withContext(ioDispatcher) {
        ensureTodayChallenges()
        dailyChallengesDao.getActiveChallenges()
    }

    /**
     * Updates absolute progress for challenges of a specific [type].
     */
    suspend fun updateProgress(type: String, value: Int, dateKey: String = getTodayKey()) = withContext(ioDispatcher) {
        ensureTodayChallenges(dateKey)
        val todayList = dailyChallengesDao.getChallengesForDate(dateKey)
        todayList.filter { it.type == type && !it.isClaimed }.forEach { challenge ->
            dailyChallengesDao.updateProgress(challenge.id, value)
        }
    }

    /**
     * Increments relative progress (e.g. +1 lap, +1 clean race) for challenges of a specific [type].
     */
    suspend fun incrementProgress(type: String, delta: Int = 1, dateKey: String = getTodayKey()) = withContext(ioDispatcher) {
        ensureTodayChallenges(dateKey)
        val todayList = dailyChallengesDao.getChallengesForDate(dateKey)
        todayList.filter { it.type == type && !it.isClaimed }.forEach { challenge ->
            dailyChallengesDao.incrementProgress(challenge.id, delta)
        }
    }

    /**
     * Claims a completed challenge reward, persisting claim state to Room.
     * Returns the claimed challenge for awarding in-game currency.
     */
    suspend fun claimChallenge(id: String): DailyChallenges? = withContext(ioDispatcher) {
        val challenge = dailyChallengesDao.getChallengeById(id)
        if (challenge != null && challenge.isTaskFinished && !challenge.isClaimed) {
            dailyChallengesDao.claimReward(id)
            dailyChallengesDao.getChallengeById(id)
        } else {
            null
        }
    }

    /**
     * Inserts or overwrites challenges directly.
     */
    suspend fun insertChallenges(challenges: List<DailyChallenges>) = withContext(ioDispatcher) {
        dailyChallengesDao.insertChallenges(challenges)
    }

    /**
     * Cleans up expired challenges older than 7 days.
     */
    suspend fun cleanupExpiredChallenges() = withContext(ioDispatcher) {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        dailyChallengesDao.deleteExpiredChallenges(sevenDaysAgo)
    }

    companion object {
        @Volatile
        private var INSTANCE: DailyChallengesRepository? = null

        fun getInstance(context: Context): DailyChallengesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DailyChallengesRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
