package com.example.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Achievements] entity, providing reactive
 * streams and update mechanisms for player milestones.
 */
@Dao
interface AchievementsDao {

    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, targetProgress ASC")
    fun getAllAchievementsFlow(): Flow<List<Achievements>>

    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, targetProgress ASC")
    suspend fun getAllAchievements(): List<Achievements>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedTimestamp DESC")
    fun getUnlockedAchievementsFlow(): Flow<List<Achievements>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedTimestamp DESC")
    suspend fun getUnlockedAchievements(): List<Achievements>

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    fun getAchievementFlow(id: String): Flow<Achievements?>

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getAchievementById(id: String): Achievements?

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    fun getUnlockedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM achievements")
    fun getTotalCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievements)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievements>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultAchievements(achievements: List<Achievements>)

    @Update
    suspend fun updateAchievement(achievement: Achievements)

    @Delete
    suspend fun deleteAchievement(achievement: Achievements)

    @Query("DELETE FROM achievements WHERE id = :id")
    suspend fun deleteAchievementById(id: String)

    @Query("DELETE FROM achievements")
    suspend fun deleteAllAchievements()

    @Query("UPDATE achievements SET isUnlocked = 1, currentProgress = targetProgress, unlockedTimestamp = :timestamp WHERE id = :id")
    suspend fun unlockAchievement(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE achievements SET currentProgress = :progress, isUnlocked = CASE WHEN :progress >= targetProgress THEN 1 ELSE isUnlocked END, unlockedTimestamp = CASE WHEN :progress >= targetProgress AND unlockedTimestamp IS NULL THEN :timestamp ELSE unlockedTimestamp END WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE achievements SET isRewardClaimed = 1 WHERE id = :id")
    suspend fun markRewardClaimed(id: String)
}
