package com.example.model

data class CareerRaceEvent(
    val id: String,
    val tierId: String,
    val title: String,
    val trackId: String,
    val laps: Int,
    val entryFee: Int,
    val firstPrize: Int,
    val secondPrize: Int,
    val thirdPrize: Int,
    val repReward: Int,
    val dynamicWeather: Boolean,
    val requiredRep: Int,
    val isCompleted: Boolean = false,
    val bestFinish: Int? = null // 1, 2, 3 or null
)

data class CareerTier(
    val id: String,
    val name: String,
    val subtitle: String,
    val requiredRep: Int,
    val events: List<CareerRaceEvent>,
    val trophyName: String
)

object CareerRegistry {
    fun createDefaultTiers(): List<CareerTier> {
        return listOf(
            CareerTier(
                id = "tier_1",
                name = "Tier 1: Clubman Cup",
                subtitle = "Entry-level circuit racing for rookie drivers",
                requiredRep = 0,
                trophyName = "Clubman Bronze Cup",
                events = listOf(
                    CareerRaceEvent(
                        id = "event_1_1",
                        tierId = "tier_1",
                        title = "Riviera Sprint",
                        trackId = "monaco_riviera",
                        laps = 2,
                        entryFee = 0,
                        firstPrize = 3200,
                        secondPrize = 1800,
                        thirdPrize = 900,
                        repReward = 80,
                        dynamicWeather = false,
                        requiredRep = 0
                    ),
                    CareerRaceEvent(
                        id = "event_1_2",
                        tierId = "tier_1",
                        title = "Apex National Challenge",
                        trackId = "apex_speedway",
                        laps = 2,
                        entryFee = 250,
                        firstPrize = 4500,
                        secondPrize = 2500,
                        thirdPrize = 1200,
                        repReward = 120,
                        dynamicWeather = true,
                        requiredRep = 60
                    )
                )
            ),
            CareerTier(
                id = "tier_2",
                name = "Tier 2: Pro-Am Championship",
                subtitle = "Higher power cars and sudden weather shifts",
                requiredRep = 200,
                trophyName = "Pro-Am Silver Shield",
                events = listOf(
                    CareerRaceEvent(
                        id = "event_2_1",
                        tierId = "tier_2",
                        title = "Tokyo Midnight Dash",
                        trackId = "neon_tokyo",
                        laps = 3,
                        entryFee = 600,
                        firstPrize = 7800,
                        secondPrize = 4200,
                        thirdPrize = 2100,
                        repReward = 180,
                        dynamicWeather = true,
                        requiredRep = 200
                    ),
                    CareerRaceEvent(
                        id = "event_2_2",
                        tierId = "tier_2",
                        title = "Coastal Storm GP",
                        trackId = "monaco_riviera",
                        laps = 3,
                        entryFee = 900,
                        firstPrize = 9500,
                        secondPrize = 5000,
                        thirdPrize = 2600,
                        repReward = 220,
                        dynamicWeather = true,
                        requiredRep = 350
                    )
                )
            ),
            CareerTier(
                id = "tier_3",
                name = "Tier 3: Supercar Masters",
                subtitle = "Extreme aerodynamics, high downforce, and aquaplaning puddles",
                requiredRep = 550,
                trophyName = "Supercar Gold Plate",
                events = listOf(
                    CareerRaceEvent(
                        id = "event_3_1",
                        tierId = "tier_3",
                        title = "Apex Grand Tempest",
                        trackId = "apex_speedway",
                        laps = 3,
                        entryFee = 1500,
                        firstPrize = 15000,
                        secondPrize = 8500,
                        thirdPrize = 4000,
                        repReward = 320,
                        dynamicWeather = true,
                        requiredRep = 550
                    ),
                    CareerRaceEvent(
                        id = "event_3_2",
                        tierId = "tier_3",
                        title = "Akina Touge Drift Battle",
                        trackId = "mount_akina",
                        laps = 2,
                        entryFee = 2000,
                        firstPrize = 18500,
                        secondPrize = 10000,
                        thirdPrize = 5000,
                        repReward = 400,
                        dynamicWeather = true,
                        requiredRep = 750
                    )
                )
            ),
            CareerTier(
                id = "tier_4",
                name = "Tier 4: Apex World Apex GP",
                subtitle = "The pinnacle of motorsport against champion AI drivers",
                requiredRep = 1000,
                trophyName = "Apex World Champion Trophy",
                events = listOf(
                    CareerRaceEvent(
                        id = "event_4_1",
                        tierId = "tier_4",
                        title = "Frozen Touge Endurance",
                        trackId = "mount_akina",
                        laps = 4,
                        entryFee = 3500,
                        firstPrize = 32000,
                        secondPrize = 18000,
                        thirdPrize = 9000,
                        repReward = 600,
                        dynamicWeather = true,
                        requiredRep = 1000
                    ),
                    CareerRaceEvent(
                        id = "event_4_2",
                        tierId = "tier_4",
                        title = "Monaco World Final GP",
                        trackId = "monaco_riviera",
                        laps = 4,
                        entryFee = 5000,
                        firstPrize = 50000,
                        secondPrize = 25000,
                        thirdPrize = 12000,
                        repReward = 1000,
                        dynamicWeather = true,
                        requiredRep = 1400
                    )
                )
            )
        )
    }
}
