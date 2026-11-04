package com.example.data.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Room entity representing player milestone achievements (e.g. 'First Win',
 * '100 Miles Driven', 'Clean Lap').
 * Persisted in local Room database with reactive query access.
 */
@Entity(tableName = "achievements")
data class Achievements(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String = "Milestone",
    val targetProgress: Float = 1f,
    val currentProgress: Float = 0f,
    val unit: String = "",
    val isUnlocked: Boolean = false,
    val rewardCredits: Int = 1000,
    val rewardRep: Int = 100,
    val iconName: String = "trophy",
    val unlockedTimestamp: Long? = null
) {
    val progressFraction: Float
        @Ignore get() = (currentProgress / targetProgress.coerceAtLeast(1f)).coerceIn(0f, 1f)

    val progressPercent: Int
        @Ignore get() = (progressFraction * 100f).toInt()
}

typealias AchievementEntity = Achievements

/**
 * Built-in default milestone achievement definitions.
 */
object DefaultAchievements {
    const val ID_FIRST_WIN = "first_win"
    const val ID_100_MILES = "100_miles_driven"
    const val ID_CLEAN_LAP = "clean_lap"
    const val ID_PODIUM_MASTER = "podium_master"
    const val ID_SPEED_DEMON = "speed_demon"
    const val ID_DRIFT_KING = "drift_king"
    const val ID_WET_RACER = "wet_racer"
    const val ID_TUNING_PRO = "tuning_pro"

    fun createDefaultList(): List<Achievements> = listOf(
        Achievements(
            id = ID_FIRST_WIN,
            title = "First Win",
            description = "Conquer your very first career or circuit race victory.",
            category = "Wins",
            targetProgress = 1f,
            currentProgress = 0f,
            unit = "win",
            rewardCredits = 3000,
            rewardRep = 100,
            iconName = "emoji_events"
        ),
        Achievements(
            id = ID_100_MILES,
            title = "100 Miles Driven",
            description = "Drive at least 100 miles (160,934 meters) on competitive tracks.",
            category = "Distance",
            targetProgress = 160934f, // 100 miles in meters
            currentProgress = 0f,
            unit = "m",
            rewardCredits = 15000,
            rewardRep = 500,
            iconName = "drive_eta"
        ),
        Achievements(
            id = ID_CLEAN_LAP,
            title = "Clean Lap",
            description = "Complete a full racing circuit lap with zero collision damage.",
            category = "Mastery",
            targetProgress = 1f,
            currentProgress = 0f,
            unit = "lap",
            rewardCredits = 4000,
            rewardRep = 150,
            iconName = "shield"
        ),
        Achievements(
            id = ID_PODIUM_MASTER,
            title = "Podium Finisher",
            description = "Finish on the podium in 5 competitive championship races.",
            category = "Wins",
            targetProgress = 5f,
            currentProgress = 0f,
            unit = "podiums",
            rewardCredits = 7500,
            rewardRep = 250,
            iconName = "military_tech"
        ),
        Achievements(
            id = ID_SPEED_DEMON,
            title = "Speed Demon",
            description = "Surpass the 250 km/h speed barrier in a tuned race car.",
            category = "Speed",
            targetProgress = 250f,
            currentProgress = 0f,
            unit = "km/h",
            rewardCredits = 8000,
            rewardRep = 300,
            iconName = "speed"
        ),
        Achievements(
            id = ID_DRIFT_KING,
            title = "Drift Master",
            description = "Sustain 1,000 meters of cumulative high-angle drifting.",
            category = "Drift",
            targetProgress = 1000f,
            currentProgress = 0f,
            unit = "m",
            rewardCredits = 6000,
            rewardRep = 200,
            iconName = "star"
        ),
        Achievements(
            id = ID_WET_RACER,
            title = "Storm Navigator",
            description = "Win an official race in wet rain or thunderstorm weather conditions.",
            category = "Weather",
            targetProgress = 1f,
            currentProgress = 0f,
            unit = "win",
            rewardCredits = 5000,
            rewardRep = 200,
            iconName = "water_drop"
        ),
        Achievements(
            id = ID_TUNING_PRO,
            title = "Paddock Engineer",
            description = "Upgrade vehicle performance parts to Stage 3 or higher.",
            category = "Garage",
            targetProgress = 3f,
            currentProgress = 0f,
            unit = "stage",
            rewardCredits = 5000,
            rewardRep = 150,
            iconName = "build"
        )
    )
}
