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
        GlobalLeaderboards::class
    ],
    version = 5,
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
