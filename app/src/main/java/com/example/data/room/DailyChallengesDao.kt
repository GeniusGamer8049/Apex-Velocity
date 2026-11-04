package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for [DailyChallenges] Room entity.
 * Provides query, insertion, progressive tracking, and claim operations.
 */
@Dao
interface DailyChallengesDao {

    @Query("SELECT * FROM daily_challenges ORDER BY createdAtTimestamp DESC")
    fun getAllChallengesFlow(): Flow<List<DailyChallenges>>

    @Query("SELECT * FROM daily_challenges ORDER BY createdAtTimestamp DESC")
    suspend fun getAllChallenges(): List<DailyChallenges>

    @Query("SELECT * FROM daily_challenges WHERE dateKey = :dateKey ORDER BY id ASC")
    fun getChallengesForDateFlow(dateKey: String): Flow<List<DailyChallenges>>

    @Query("SELECT * FROM daily_challenges WHERE dateKey = :dateKey ORDER BY id ASC")
    suspend fun getChallengesForDate(dateKey: String): List<DailyChallenges>

    @Query("SELECT * FROM daily_challenges WHERE expiresAtTimestamp > :now ORDER BY isClaimed ASC, isCompleted DESC, id ASC")
    fun getActiveChallengesFlow(now: Long = System.currentTimeMillis()): Flow<List<DailyChallenges>>

    @Query("SELECT * FROM daily_challenges WHERE expiresAtTimestamp > :now ORDER BY isClaimed ASC, isCompleted DESC, id ASC")
    suspend fun getActiveChallenges(now: Long = System.currentTimeMillis()): List<DailyChallenges>

    @Query("SELECT * FROM daily_challenges WHERE id = :id LIMIT 1")
    suspend fun getChallengeById(id: String): DailyChallenges?

    @Query("SELECT * FROM daily_challenges WHERE id = :id LIMIT 1")
    fun getChallengeFlow(id: String): Flow<DailyChallenges?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: DailyChallenges)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<DailyChallenges>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultChallenges(challenges: List<DailyChallenges>)

    @Update
    suspend fun updateChallenge(challenge: DailyChallenges)

    @Query("UPDATE daily_challenges SET currentValue = MAX(currentValue, :newValue), isCompleted = CASE WHEN MAX(currentValue, :newValue) >= targetValue THEN 1 ELSE isCompleted END WHERE id = :id")
    suspend fun updateProgress(id: String, newValue: Int)

    @Query("UPDATE daily_challenges SET currentValue = MIN(targetValue, currentValue + :delta), isCompleted = CASE WHEN (currentValue + :delta) >= targetValue THEN 1 ELSE isCompleted END WHERE id = :id")
    suspend fun incrementProgress(id: String, delta: Int)

    @Query("UPDATE daily_challenges SET isClaimed = 1 WHERE id = :id")
    suspend fun claimReward(id: String)

    @Query("DELETE FROM daily_challenges WHERE id = :id")
    suspend fun deleteChallengeById(id: String)

    @Query("DELETE FROM daily_challenges WHERE expiresAtTimestamp < :expiryThreshold")
    suspend fun deleteExpiredChallenges(expiryThreshold: Long)

    @Query("DELETE FROM daily_challenges")
    suspend fun deleteAllChallenges()

    @Query("SELECT COUNT(*) FROM daily_challenges WHERE dateKey = :dateKey AND (isCompleted = 1 OR currentValue >= targetValue)")
    suspend fun getCompletedCountForDate(dateKey: String): Int

    @Query("SELECT COUNT(*) FROM daily_challenges WHERE dateKey = :dateKey")
    suspend fun getTotalCountForDate(dateKey: String): Int
}
