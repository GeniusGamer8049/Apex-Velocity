package com.example.model

data class CareerObjectiveResult(
    val title: String,
    val description: String,
    val isAchieved: Boolean,
    val rewardText: String
)

data class RivalFinishResult(
    val position: Int,
    val name: String,
    val vehicleName: String,
    val totalTimeMs: Long,
    val deltaFormatted: String,
    val isPlayer: Boolean
)

data class PostRaceResult(
    val position: Int,
    val totalRacers: Int,
    val totalRaceTimeMs: Long,
    val bestLapTimeMs: Long,
    val isNewBest: Boolean,
    val lapTimesMs: List<Long>,
    val topSpeedKmh: Float,
    val driftScore: Int,
    val isCleanRace: Boolean,
    val basePrize: Int,
    val cleanBonus: Int,
    val driftBonus: Int,
    val repEarned: Int,
    val totalEarnings: Int,
    val creditsBefore: Int,
    val creditsAfter: Int,
    val repBefore: Int,
    val repAfter: Int,
    val isTimeTrial: Boolean,
    val trackId: String,
    val trackName: String,
    val vehicleName: String,
    val careerEvent: CareerRaceEvent?,
    val tierName: String,
    val tierSubtitle: String,
    val tierTrophyName: String,
    val tierCompletedEvents: Int,
    val tierTotalEvents: Int,
    val isTierTrophyUnlocked: Boolean,
    val nextTierName: String?,
    val nextTierRequiredRep: Int?,
    val repProgressToNextTier: Float,
    val nextCareerEvent: CareerRaceEvent?,
    val careerObjectives: List<CareerObjectiveResult>,
    val rivalResults: List<RivalFinishResult>,
    val dailyChallengesUpdated: List<DailyChallenge>,
    val unlockedBadges: List<Achievement> = emptyList()
)
