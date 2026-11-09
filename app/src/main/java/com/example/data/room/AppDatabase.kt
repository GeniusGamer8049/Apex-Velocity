package com.example.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DriverProfile::class,
        GameStatistics::class,
        Achievements::class,
        DailyChallenges::class,
        GlobalLeaderboards::class,
        ReplayStorage::class,
        CareerProgress::class,
        TimeTrialRecord::class,
        LocalTrackRecord::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun driverProfileDao(): DriverProfileDao
    abstract fun gameStatisticsDao(): GameStatisticsDao
    abstract fun achievementsDao(): AchievementsDao
    fun achievementDao(): AchievementsDao = achievementsDao()
    abstract fun dailyChallengesDao(): DailyChallengesDao
    fun dailyChallengeDao(): DailyChallengesDao = dailyChallengesDao()
    abstract fun globalLeaderboardsDao(): GlobalLeaderboardsDao
    fun globalLeaderboardDao(): GlobalLeaderboardsDao = globalLeaderboardsDao()
    abstract fun replayStorageDao(): ReplayStorageDao
    fun replayDao(): ReplayStorageDao = replayStorageDao()
    abstract fun careerProgressDao(): CareerProgressDao
    fun careerDao(): CareerProgressDao = careerProgressDao()
    abstract fun timeTrialRecordDao(): TimeTrialRecordDao
    fun leaderboardRecordDao(): TimeTrialRecordDao = timeTrialRecordDao()
    abstract fun localTrackRecordDao(): LocalTrackRecordDao
    fun trackRecordDao(): LocalTrackRecordDao = localTrackRecordDao()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apex_velocity_app_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
