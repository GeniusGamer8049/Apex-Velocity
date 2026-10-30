package com.example.model

object GlobalLeaderboardRegistry {

    data class BenchmarkRacer(
        val driverName: String,
        val countryCode: String,
        val vehicleName: String,
        val vehicleCategory: String,
        val timeDeltaMs: Long // delta from track WR base
    )

    private val benchmarkRacers = listOf(
        BenchmarkRacer("Max \"Apex\" V.", "NL", "Hyperion GT-R", "Apex Prototype", 0L),
        BenchmarkRacer("Takumi Fujiwara", "JP", "Silvia S15 Spec-R", "Drift Tuner", 280L),
        BenchmarkRacer("Lewis \"Hammer\" H.", "GB", "Viper GTS V10", "Grand Tourer", 590L),
        BenchmarkRacer("Charles \"Riviera\" L.", "MC", "Hyperion GT-R", "Apex Prototype", 820L),
        BenchmarkRacer("Ken Block Tribute", "US", "Evolution IX GSR", "AWD Track & Wet", 1150L),
        BenchmarkRacer("Ayrton S.", "BR", "Apex GT-86 Coupe", "Sport Compact", 1460L),
        BenchmarkRacer("Sebastian \"Seb\" V.", "DE", "Viper GTS V10", "Grand Tourer", 1880L),
        BenchmarkRacer("Fernando \"ElNano\" A.", "ES", "Apex GT-86 Coupe", "Sport Compact", 2250L),
        BenchmarkRacer("Oscar \"Pastry\" P.", "AU", "Evolution IX GSR", "AWD Track & Wet", 2690L),
        BenchmarkRacer("Keichi \"DriftKing\" T.", "JP", "Silvia S15 Spec-R", "Drift Tuner", 3140L),
        BenchmarkRacer("Lando \"Twitch\" N.", "GB", "Hyperion GT-R", "Apex Prototype", 3580L),
        BenchmarkRacer("Mika \"FlyingFinn\" H.", "FI", "Viper GTS V10", "Grand Tourer", 4120L),
        BenchmarkRacer("Carlos \"Smooth\" S.", "ES", "Hyperion GT-R", "Apex Prototype", 4650L),
        BenchmarkRacer("George \"Torpedo\" R.", "GB", "Evolution IX GSR", "AWD Track & Wet", 5210L),
        BenchmarkRacer("Yuki \"Rocket\" T.", "JP", "Apex GT-86 Coupe", "Sport Compact", 5830L),
        BenchmarkRacer("Colin \"Rally\" M.", "GB", "Evolution IX GSR", "AWD Track & Wet", 6450L),
        BenchmarkRacer("ApexGhostX", "US", "Hyperion GT-R", "Apex Prototype", 7120L),
        BenchmarkRacer("Pierre \"Gasly\" P.", "FR", "Apex GT-86 Coupe", "Sport Compact", 7840L),
        BenchmarkRacer("Walter \"Quattro\" R.", "DE", "Evolution IX GSR", "AWD Track & Wet", 8600L),
        BenchmarkRacer("Mario \"Monza\" A.", "IT", "Viper GTS V10", "Grand Tourer", 9410L),
        BenchmarkRacer("DriftNinja_99", "JP", "Silvia S15 Spec-R", "Drift Tuner", 10250L),
        BenchmarkRacer("RedlineRider", "SE", "Viper GTS V10", "Grand Tourer", 11150L),
        BenchmarkRacer("SpeedDemon_US", "US", "Apex GT-86 Coupe", "Sport Compact", 12100L),
        BenchmarkRacer("NordicStig", "NO", "Evolution IX GSR", "AWD Track & Wet", 13200L)
    )

    // Base World Record times in ms for each track
    private val trackBaseWrTimeMs = mapOf(
        "monaco_riviera" to 74420L, // 1:14.420
        "apex_speedway" to 68210L,  // 1:08.210
        "neon_tokyo" to 78890L,     // 1:18.890
        "mount_akina" to 89450L     // 1:29.450
    )

    fun getBaseWrTimeMs(trackId: String): Long {
        return trackBaseWrTimeMs[trackId] ?: 74420L
    }

    fun buildTrackLeaderboard(
        trackId: String,
        playerPbMs: Long? = null,
        playerName: String = "Apex Pilot",
        playerCountry: String = "US",
        playerVehicleName: String = "Apex GT-86 Coupe",
        playerVehicleCategory: String = "Sport Compact"
    ): List<LeaderboardEntry> {
        val baseWr = getBaseWrTimeMs(trackId)

        // Generate seeded entries
        val list = mutableListOf<LeaderboardEntry>()
        benchmarkRacers.forEachIndexed { index, racer ->
            val lapTime = baseWr + racer.timeDeltaMs
            list.add(
                LeaderboardEntry(
                    id = "bot_${trackId}_$index",
                    trackId = trackId,
                    rank = 0, // Assigned after sorting
                    driverName = racer.driverName,
                    countryCode = racer.countryCode,
                    vehicleName = racer.vehicleName,
                    vehicleCategory = racer.vehicleCategory,
                    lapTimeMs = lapTime,
                    isPlayer = false,
                    tier = LeaderboardTier.GOLD,
                    dateRecorded = "2026-10-30"
                )
            )
        }

        // Insert player if they have set a valid PB
        if (playerPbMs != null && playerPbMs > 0L) {
            list.add(
                LeaderboardEntry(
                    id = "player_${trackId}",
                    trackId = trackId,
                    rank = 0,
                    driverName = playerName.ifBlank { "Apex Pilot" },
                    countryCode = playerCountry.ifBlank { "US" },
                    vehicleName = playerVehicleName,
                    vehicleCategory = playerVehicleCategory,
                    lapTimeMs = playerPbMs,
                    isPlayer = true,
                    tier = LeaderboardTier.GOLD,
                    dateRecorded = "Today"
                )
            )
        }

        // Sort ascending by lap time
        val sorted = list.sortedBy { it.lapTimeMs }
        val leaderTime = sorted.firstOrNull()?.lapTimeMs ?: baseWr

        return sorted.mapIndexed { index, entry ->
            val rank = index + 1
            val gap = entry.lapTimeMs - leaderTime
            val tier = when {
                rank == 1 -> LeaderboardTier.WORLD_RECORD
                rank <= 3 -> LeaderboardTier.TOP_1_PERCENT
                rank <= 7 -> LeaderboardTier.DIAMOND
                rank <= 14 -> LeaderboardTier.GOLD
                rank <= 20 -> LeaderboardTier.SILVER
                else -> LeaderboardTier.BRONZE
            }
            entry.copy(rank = rank, gapMs = gap, tier = tier)
        }
    }
}
