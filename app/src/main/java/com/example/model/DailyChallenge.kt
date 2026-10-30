package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class ChallengeType {
    TOP_SPEED,
    DRIFT_POINTS,
    CLEAN_RACE,
    WET_RACE_WIN,
    PODIUM_FINISH,
    COMPLETE_LAPS
}

data class DailyChallenge(
    val id: String,
    val type: ChallengeType,
    val title: String,
    val description: String,
    val targetValue: Int,
    var currentValue: Int = 0,
    val rewardCredits: Int,
    val rewardRep: Int,
    var isClaimed: Boolean = false,
    val iconEmoji: String
) {
    val isCompleted: Boolean
        get() = currentValue >= targetValue

    val progressFraction: Float
        get() = (currentValue.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)
}

object DailyChallengeGenerator {

    fun getTodayKey(timeMs: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(timeMs))
    }

    fun getMsUntilMidnight(timeMs: Long = System.currentTimeMillis()): Long {
        val millisInDay = 24 * 60 * 60 * 1000L
        val currentDayMillis = timeMs % millisInDay
        return millisInDay - currentDayMillis
    }

    fun generateForDate(dateKey: String): List<DailyChallenge> {
        val seed = dateKey.hashCode().toLong()
        val rng = Random(seed)

        val challengePool = listOf(
            DailyChallenge(
                id = "${dateKey}_drift",
                type = ChallengeType.DRIFT_POINTS,
                title = "Apex Drift King",
                description = "Score 1,200 drift points in any single racing session",
                targetValue = 1200,
                rewardCredits = 3500,
                rewardRep = 100,
                iconEmoji = "🔥"
            ),
            DailyChallenge(
                id = "${dateKey}_speed",
                type = ChallengeType.TOP_SPEED,
                title = "Velocity Limitbreaker",
                description = "Reach a maximum speed of 230 km/h in any vehicle",
                targetValue = 230,
                rewardCredits = 4000,
                rewardRep = 120,
                iconEmoji = "⚡"
            ),
            DailyChallenge(
                id = "${dateKey}_clean",
                type = ChallengeType.CLEAN_RACE,
                title = "Precision Driver",
                description = "Complete a clean race without hitting any track limits or barriers",
                targetValue = 1,
                rewardCredits = 3000,
                rewardRep = 90,
                iconEmoji = "🛡️"
            ),
            DailyChallenge(
                id = "${dateKey}_wet",
                type = ChallengeType.WET_RACE_WIN,
                title = "Storm Tamer",
                description = "Win a race in dynamic wet or heavy thunderstorm conditions",
                targetValue = 1,
                rewardCredits = 4800,
                rewardRep = 150,
                iconEmoji = "⛈️"
            ),
            DailyChallenge(
                id = "${dateKey}_podium",
                type = ChallengeType.PODIUM_FINISH,
                title = "Podium Contender",
                description = "Finish 1st or 2nd in 2 Career Championship events",
                targetValue = 2,
                rewardCredits = 5000,
                rewardRep = 180,
                iconEmoji = "🏆"
            ),
            DailyChallenge(
                id = "${dateKey}_laps",
                type = ChallengeType.COMPLETE_LAPS,
                title = "Endurance Specialist",
                description = "Complete 5 total racing laps across any circuit",
                targetValue = 5,
                rewardCredits = 3200,
                rewardRep = 80,
                iconEmoji = "🏁"
            )
        )

        // Deterministically pick 3 distinct challenges for the day
        val shuffled = challengePool.shuffled(rng)
        return shuffled.take(3)
    }
}
