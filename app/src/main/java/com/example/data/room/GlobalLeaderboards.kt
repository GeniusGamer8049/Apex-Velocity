package com.example.data.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * GlobalLeaderboards Room entity storing top lap times and rankings for each circuit.
 * Supports querying top circuit rankings, benchmark drivers, and highlighting player personal bests.
 */
@Entity(
    tableName = "global_leaderboards",
    indices = [
        Index("trackId"),
        Index("rank"),
        Index(value = ["trackId", "rank"])
    ]
)
data class GlobalLeaderboards(
    @PrimaryKey val id: String,
    val trackId: String,
    val trackName: String,
    val rank: Int,
    val driverName: String,
    val countryCode: String,
    val vehicleName: String,
    val vehicleCategory: String,
    val lapTimeMs: Long,
    val gapMs: Long = 0L,
    val isPlayer: Boolean = false,
    val tierName: String = "GOLD",
    val dateRecorded: String = "2026-11-05",
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        @Ignore get() {
            val totalSec = lapTimeMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val ms = lapTimeMs % 1000
            return String.format(Locale.US, "%d:%02d.%03d", min, sec, ms)
        }

    val formattedGap: String
        @Ignore get() {
            return if (gapMs <= 0L || rank == 1) "LEADER" else String.format(Locale.US, "+%.3fs", gapMs / 1000f)
        }

    val countryFlag: String
        @Ignore get() = getFlagEmoji(countryCode)

    companion object {
        fun getFlagEmoji(countryCode: String): String {
            return when (countryCode.uppercase().trim()) {
                "US" -> "🇺🇸"
                "NL" -> "🇳🇱"
                "GB" -> "🇬🇧"
                "JP" -> "🇯🇵"
                "MC" -> "🇲🇨"
                "DE" -> "🇩🇪"
                "BR" -> "🇧🇷"
                "ES" -> "🇪🇸"
                "AU" -> "🇦🇺"
                "FI" -> "🇫🇮"
                "FR" -> "🇫🇷"
                "IT" -> "🇮🇹"
                "SE" -> "🇸🇪"
                "NO" -> "🇳🇴"
                "CA" -> "🇨🇦"
                else -> "🏁"
            }
        }
    }
}

typealias GlobalLeaderboardsEntity = GlobalLeaderboards
typealias GlobalLeaderboardEntity = GlobalLeaderboards

/**
 * Pre-configured default benchmark circuit rankings for each racing circuit.
 */
object DefaultGlobalLeaderboards {

    data class TrackBenchmarkSpec(
        val trackId: String,
        val trackName: String,
        val baseWrTimeMs: Long
    )

    val trackSpecs = listOf(
        TrackBenchmarkSpec("monaco_riviera", "Monaco Riviera", 74420L),
        TrackBenchmarkSpec("apex_speedway", "Apex Speedway", 68210L),
        TrackBenchmarkSpec("neon_tokyo", "Neon Tokyo", 78890L),
        TrackBenchmarkSpec("mount_akina", "Mount Akina", 89450L)
    )

    private val benchmarkRoster = listOf(
        Triple("Max \"Apex\" V.", "NL", Pair("Hyperion GT-R", "Apex Prototype")),
        Triple("Takumi Fujiwara", "JP", Pair("Silvia S15 Spec-R", "Drift Tuner")),
        Triple("Lewis \"Hammer\" H.", "GB", Pair("Viper GTS V10", "Grand Tourer")),
        Triple("Charles \"Riviera\" L.", "MC", Pair("Hyperion GT-R", "Apex Prototype")),
        Triple("Ken Block Tribute", "US", Pair("Evolution IX GSR", "AWD Track & Wet")),
        Triple("Ayrton S.", "BR", Pair("Apex GT-86 Coupe", "Sport Compact")),
        Triple("Sebastian \"Seb\" V.", "DE", Pair("Viper GTS V10", "Grand Tourer")),
        Triple("Fernando \"ElNano\" A.", "ES", Pair("Apex GT-86 Coupe", "Sport Compact")),
        Triple("Keichi \"DriftKing\" T.", "JP", Pair("Silvia S15 Spec-R", "Drift Tuner")),
        Triple("Lando \"Twitch\" N.", "GB", Pair("Hyperion GT-R", "Apex Prototype"))
    )

    private val benchmarkDeltas = listOf(
        0L, 280L, 590L, 820L, 1150L, 1460L, 1880L, 2250L, 3140L, 3580L
    )

    fun createDefaultRankings(): List<GlobalLeaderboards> {
        val list = mutableListOf<GlobalLeaderboards>()

        for (spec in trackSpecs) {
            val baseWr = spec.baseWrTimeMs
            benchmarkRoster.forEachIndexed { index, racer ->
                val delta = benchmarkDeltas.getOrElse(index) { index * 400L }
                val lapTime = baseWr + delta
                val tier = when (index) {
                    0 -> "WORLD_RECORD"
                    1, 2 -> "TOP_1_PERCENT"
                    3, 4 -> "DIAMOND"
                    else -> "GOLD"
                }

                list.add(
                    GlobalLeaderboards(
                        id = "${spec.trackId}_bench_${index + 1}",
                        trackId = spec.trackId,
                        trackName = spec.trackName,
                        rank = index + 1,
                        driverName = racer.first,
                        countryCode = racer.second,
                        vehicleName = racer.third.first,
                        vehicleCategory = racer.third.second,
                        lapTimeMs = lapTime,
                        gapMs = delta,
                        isPlayer = false,
                        tierName = tier,
                        dateRecorded = "2026-11-05"
                    )
                )
            }
        }

        return list
    }
}
