package com.example.model

enum class AchievementCategory(val displayName: String) {
    ALL("All Badges"),
    SPEED("Velocity"),
    DRIFT("Drifting"),
    WINS("Race Wins"),
    MASTERY("Precision"),
    GARAGE("Garage")
}

enum class BadgeRarity(val label: String) {
    BRONZE("Bronze"),
    SILVER("Silver"),
    GOLD("Gold"),
    PLATINUM("Platinum"),
    TITANIUM("Titanium")
}

data class Achievement(
    val id: String,
    val category: AchievementCategory,
    val title: String,
    val description: String,
    val badgeTitle: String,
    val badgeRarity: BadgeRarity,
    val targetValue: Float,
    val currentValue: Float = 0f,
    val unit: String,
    val isUnlocked: Boolean = false,
    val isClaimed: Boolean = false,
    val rewardCredits: Int,
    val rewardRep: Int,
    val unlockedTimestamp: Long? = null
) {
    val progressFraction: Float
        get() = (currentValue / targetValue.coerceAtLeast(1f)).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt()
}

object AchievementRegistry {

    fun createDefaultAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "badge_rookie",
                category = AchievementCategory.MASTERY,
                title = "Apex License",
                description = "Obtain rookie racing license and enter the championship",
                badgeTitle = "Rookie Pilot",
                badgeRarity = BadgeRarity.BRONZE,
                targetValue = 1f,
                currentValue = 1f,
                unit = "license",
                isUnlocked = true,
                isClaimed = true,
                rewardCredits = 1000,
                rewardRep = 50,
                unlockedTimestamp = System.currentTimeMillis()
            ),
            Achievement(
                id = "badge_win_1",
                category = AchievementCategory.WINS,
                title = "Checkered Debut",
                description = "Win your very first official career or circuit race",
                badgeTitle = "First Victory",
                badgeRarity = BadgeRarity.BRONZE,
                targetValue = 1f,
                currentValue = 0f,
                unit = "win",
                rewardCredits = 3000,
                rewardRep = 100
            ),
            Achievement(
                id = "badge_win_5",
                category = AchievementCategory.WINS,
                title = "Podium Veteran",
                description = "Win 5 race events across any championship circuit",
                badgeTitle = "Race Veteran",
                badgeRarity = BadgeRarity.SILVER,
                targetValue = 5f,
                currentValue = 0f,
                unit = "wins",
                rewardCredits = 7500,
                rewardRep = 250
            ),
            Achievement(
                id = "badge_win_10",
                category = AchievementCategory.WINS,
                title = "Decathlon Champion",
                description = "Win 10 official career race events against rival AI",
                badgeTitle = "Grand Champion",
                badgeRarity = BadgeRarity.GOLD,
                targetValue = 10f,
                currentValue = 0f,
                unit = "wins",
                rewardCredits = 15000,
                rewardRep = 500
            ),
            Achievement(
                id = "badge_win_25",
                category = AchievementCategory.WINS,
                title = "Apex Dominator",
                description = "Conquer 25 race victories to cement your legend",
                badgeTitle = "Apex Titan",
                badgeRarity = BadgeRarity.TITANIUM,
                targetValue = 25f,
                currentValue = 0f,
                unit = "wins",
                rewardCredits = 35000,
                rewardRep = 1200
            ),
            Achievement(
                id = "badge_speed_200",
                category = AchievementCategory.SPEED,
                title = "Velocity Cadet",
                description = "Break the 200 km/h speed threshold in any session",
                badgeTitle = "Speed 200",
                badgeRarity = BadgeRarity.BRONZE,
                targetValue = 200f,
                currentValue = 0f,
                unit = "km/h",
                rewardCredits = 2500,
                rewardRep = 80
            ),
            Achievement(
                id = "badge_speed_250",
                category = AchievementCategory.SPEED,
                title = "Sonic Velocity",
                description = "Reach a maximum speed of 250 km/h on track",
                badgeTitle = "Speed Demon",
                badgeRarity = BadgeRarity.GOLD,
                targetValue = 250f,
                currentValue = 0f,
                unit = "km/h",
                rewardCredits = 8000,
                rewardRep = 300
            ),
            Achievement(
                id = "badge_speed_300",
                category = AchievementCategory.SPEED,
                title = "Hypersonic Breaker",
                description = "Hit an astonishing 300 km/h down the main straight",
                badgeTitle = "Apex Lightning",
                badgeRarity = BadgeRarity.TITANIUM,
                targetValue = 300f,
                currentValue = 0f,
                unit = "km/h",
                rewardCredits = 25000,
                rewardRep = 800
            ),
            Achievement(
                id = "badge_drift_500m",
                category = AchievementCategory.DRIFT,
                title = "Sideway Strides",
                description = "Accumulate 500 meters of controlled power-sliding",
                badgeTitle = "Slide Apprentice",
                badgeRarity = BadgeRarity.BRONZE,
                targetValue = 500f,
                currentValue = 0f,
                unit = "meters",
                rewardCredits = 3000,
                rewardRep = 100
            ),
            Achievement(
                id = "badge_drift_2500m",
                category = AchievementCategory.DRIFT,
                title = "Tarmac Smoker",
                description = "Sustain 2,500 meters of cumulative drift distance",
                badgeTitle = "Drift Maestro",
                badgeRarity = BadgeRarity.SILVER,
                targetValue = 2500f,
                currentValue = 0f,
                unit = "meters",
                rewardCredits = 9000,
                rewardRep = 350
            ),
            Achievement(
                id = "badge_drift_10000m",
                category = AchievementCategory.DRIFT,
                title = "Tokyo Touge Legend",
                description = "Complete 10,000 meters of high-angle drifting",
                badgeTitle = "Touge King",
                badgeRarity = BadgeRarity.GOLD,
                targetValue = 10000f,
                currentValue = 0f,
                unit = "meters",
                rewardCredits = 22000,
                rewardRep = 750
            ),
            Achievement(
                id = "badge_clean_5",
                category = AchievementCategory.MASTERY,
                title = "Pristine Chassis",
                description = "Finish 5 full races with zero barrier or wall collisions",
                badgeTitle = "Precision Driver",
                badgeRarity = BadgeRarity.SILVER,
                targetValue = 5f,
                currentValue = 0f,
                unit = "races",
                rewardCredits = 7000,
                rewardRep = 250
            ),
            Achievement(
                id = "badge_wet_3",
                category = AchievementCategory.MASTERY,
                title = "Storm Sovereign",
                description = "Win 3 race events in wet rain or thunderstorm conditions",
                badgeTitle = "Rain Master",
                badgeRarity = BadgeRarity.GOLD,
                targetValue = 3f,
                currentValue = 0f,
                unit = "wet wins",
                rewardCredits = 10000,
                rewardRep = 400
            ),
            Achievement(
                id = "badge_tuning_max",
                category = AchievementCategory.GARAGE,
                title = "Apex Engine Meister",
                description = "Upgrade any vehicle component to Stage 5 maximum tune",
                badgeTitle = "Tuning Wizard",
                badgeRarity = BadgeRarity.GOLD,
                targetValue = 1f,
                currentValue = 0f,
                unit = "stage 5",
                rewardCredits = 12000,
                rewardRep = 450
            ),
            Achievement(
                id = "badge_fleet_3",
                category = AchievementCategory.GARAGE,
                title = "Fleet Collector",
                description = "Unlock and own at least 3 high-performance race vehicles",
                badgeTitle = "Fleet Collector",
                badgeRarity = BadgeRarity.PLATINUM,
                targetValue = 3f,
                currentValue = 1f,
                unit = "vehicles",
                rewardCredits = 16000,
                rewardRep = 600
            )
        )
    }

    fun getBadgeById(id: String): Achievement? {
        return createDefaultAchievements().find { it.id == id }
    }
}
