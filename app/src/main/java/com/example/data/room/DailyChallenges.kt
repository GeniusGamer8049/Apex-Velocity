package com.example.data.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * DailyChallenges Room entity representing limited-time tasks generated daily for the player.
 * Tracks task parameters, targets, current progression, expiration timestamps, and claim status.
 */
@Entity(tableName = "daily_challenges")
data class DailyChallenges(
    @PrimaryKey val id: String,
    val dateKey: String,
    val type: String,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentValue: Int = 0,
    val rewardCredits: Int,
    val rewardRep: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val iconEmoji: String = "🔥",
    val expiresAtTimestamp: Long,
    val createdAtTimestamp: Long = System.currentTimeMillis()
) {
    val isTaskFinished: Boolean
        @Ignore get() = currentValue >= targetValue || isCompleted

    val progressFraction: Float
        @Ignore get() = (currentValue.toFloat() / targetValue.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    val progressPercent: Int
        @Ignore get() = (progressFraction * 100).toInt()

    val isReadyToClaim: Boolean
        @Ignore get() = isTaskFinished && !isClaimed

    val isExpired: Boolean
        @Ignore get() = System.currentTimeMillis() > expiresAtTimestamp

    val timeRemainingMs: Long
        @Ignore get() = (expiresAtTimestamp - System.currentTimeMillis()).coerceAtLeast(0L)

    fun formattedCountdown(): String {
        val remaining = timeRemainingMs
        val hours = (remaining / (1000 * 60 * 60)) % 24
        val minutes = (remaining / (1000 * 60)) % 60
        val seconds = (remaining / 1000) % 60
        return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
    }
}

typealias DailyChallengesEntity = DailyChallenges
typealias DailyChallengeEntity = DailyChallenges

/**
 * Utility generator for constructing deterministic daily challenges per date key.
 */
object DailyChallengesGenerator {

    const val TYPE_DRIFT_POINTS = "DRIFT_POINTS"
    const val TYPE_TOP_SPEED = "TOP_SPEED"
    const val TYPE_CLEAN_RACE = "CLEAN_RACE"
    const val TYPE_WET_RACE_WIN = "WET_RACE_WIN"
    const val TYPE_PODIUM_FINISH = "PODIUM_FINISH"
    const val TYPE_COMPLETE_LAPS = "COMPLETE_LAPS"

    fun getTodayKey(timeMs: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(timeMs))
    }

    fun getExpiryForDate(timeMs: Long = System.currentTimeMillis()): Long {
        val millisInDay = 24 * 60 * 60 * 1000L
        val currentDayOffset = timeMs % millisInDay
        return timeMs + (millisInDay - currentDayOffset)
    }

    fun generateDefaultChallengesForDate(
        dateKey: String = getTodayKey(),
        nowMs: Long = System.currentTimeMillis()
    ): List<DailyChallenges> {
        val expiry = getExpiryForDate(nowMs)
        val seed = dateKey.hashCode().toLong()
        val rng = Random(seed)

        val challengePool = listOf(
            DailyChallenges(
                id = "${dateKey}_drift",
                dateKey = dateKey,
                type = TYPE_DRIFT_POINTS,
                title = "Apex Drift King",
                description = "Score 1,200 drift points in any single circuit session",
                targetValue = 1200,
                rewardCredits = 3500,
                rewardRep = 100,
                iconEmoji = "🔥",
                expiresAtTimestamp = expiry,
                createdAtTimestamp = nowMs
            ),
            DailyChallenges(
                id = "${dateKey}_speed",
                dateKey = dateKey,
                type = TYPE_TOP_SPEED,
                title = "Velocity Limitbreaker",
                description = "Reach a top speed of 230 km/h in any vehicle",
                targetValue = 230,
                rewardCredits = 4000,
                rewardRep = 120,
                iconEmoji = "⚡",
                expiresAtTimestamp = expiry,
                createdAtTimestamp = nowMs
            ),
            DailyChallenges(
                id = "${dateKey}_clean",
                dateKey = dateKey,
                type = TYPE_CLEAN_RACE,
                title = "Precision Driver",
                description = "Complete a clean race lap without hitting barriers",
                targetValue = 1,
                rewardCredits = 3000,
                rewardRep = 90,
                iconEmoji = "🛡️",
                expiresAtTimestamp = expiry,
                createdAtTimestamp = nowMs
            ),
            DailyChallenges(
                id = "${dateKey}_wet",
                dateKey = dateKey,
                type = TYPE_WET_RACE_WIN,
                title = "Storm Tamer",
                description = "Win a race in dynamic wet or thunderstorm conditions",
                targetValue = 1,
                rewardCredits = 4800,
                rewardRep = 150,
                iconEmoji = "⛈️",
                expiresAtTimestamp = expiry,
                createdAtTimestamp = nowMs
            ),
            DailyChallenges(
                id = "${dateKey}_podium",
                dateKey = dateKey,
                type = TYPE_PODIUM_FINISH,
                title = "Podium Contender",
                description = "Finish on the podium in 2 competitive events",
                targetValue = 2,
                rewardCredits = 5000,
                rewardRep = 180,
                iconEmoji = "🏆",
                expiresAtTimestamp = expiry,
                createdAtTimestamp = nowMs
            ),
            DailyChallenges(
                id = "${dateKey}_laps",
                dateKey = dateKey,
                type = TYPE_COMPLETE_LAPS,
                title = "Endurance Specialist",
                description = "Complete 5 total racing laps across any circuit",
                targetValue = 5,
                rewardCredits = 3200,
                rewardRep = 80,
                iconEmoji = "🏁",
                expiresAtTimestamp = expiry,
                createdAtTimestamp = nowMs
            )
        )

        // Select 3 deterministically shuffled tasks for the day
        return challengePool.shuffled(rng).take(3)
    }
}
