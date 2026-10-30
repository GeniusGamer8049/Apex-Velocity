package com.example.model

import androidx.compose.ui.graphics.Color

enum class LeaderboardTier(
    val displayName: String,
    val badgeColorHex: String,
    val primaryColor: Color,
    val containerColor: Color
) {
    WORLD_RECORD("World Record", "#FFD700", Color(0xFFFFD700), Color(0x33FFD700)),
    TOP_1_PERCENT("Top 1% Elite", "#00E5FF", Color(0xFF00E5FF), Color(0x3300E5FF)),
    DIAMOND("Diamond", "#B388FF", Color(0xFFB388FF), Color(0x33B388FF)),
    GOLD("Gold", "#FFC107", Color(0xFFFFC107), Color(0x33FFC107)),
    SILVER("Silver", "#CFD8DC", Color(0xFFCFD8DC), Color(0x33CFD8DC)),
    BRONZE("Bronze", "#BCAAA4", Color(0xFFBCAAA4), Color(0x33BCAAA4))
}

data class LeaderboardEntry(
    val id: String,
    val trackId: String,
    val rank: Int,
    val driverName: String,
    val countryCode: String,
    val vehicleName: String,
    val vehicleCategory: String,
    val lapTimeMs: Long,
    val gapMs: Long = 0L,
    val isPlayer: Boolean = false,
    val tier: LeaderboardTier = LeaderboardTier.GOLD,
    val dateRecorded: String = "2026-10-30"
) {
    val formattedTime: String
        get() {
            val totalSec = lapTimeMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val ms = lapTimeMs % 1000
            return "%d:%02d.%03d".format(min, sec, ms)
        }

    val formattedGap: String
        get() {
            return if (gapMs <= 0L) "LEADER" else "+%.3fs".format(gapMs / 1000f)
        }
}
