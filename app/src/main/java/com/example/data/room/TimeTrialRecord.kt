package com.example.data.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * Room entity storing top time trial records for each track,
 * including player name, vehicle used, completion time, and track ranking.
 */
@Entity(
    tableName = "time_trial_records",
    indices = [
        Index("trackId"),
        Index("completionTimeMs"),
        Index(value = ["trackId", "completionTimeMs"])
    ]
)
data class TimeTrialRecord(
    @PrimaryKey val id: String,
    val trackId: String,
    val trackName: String,
    val playerName: String,
    val vehicleUsed: String,
    val completionTimeMs: Long,
    val rank: Int = 1,
    val isPlayer: Boolean = false,
    val weatherCondition: String = "CLEAR",
    val cleanLap: Boolean = true,
    val dateRecorded: String = "2026-11-07",
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Human-readable formatted completion time (mm:ss.SSS).
     */
    val formattedCompletionTime: String
        @Ignore get() {
            val totalSec = completionTimeMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val ms = completionTimeMs % 1000
            return String.format(Locale.US, "%02d:%02d.%03d", min, sec, ms)
        }

    /**
     * Formats gap to leader or target time.
     */
    fun formattedGap(leaderTimeMs: Long): String {
        val delta = completionTimeMs - leaderTimeMs
        return if (delta <= 0L || rank == 1) "LEADER" else String.format(Locale.US, "+%.3fs", delta / 1000f)
    }

    companion object {
        fun createDefaultRecords(): List<TimeTrialRecord> {
            val list = mutableListOf<TimeTrialRecord>()
            
            // Monaco Riviera defaults
            list.add(TimeTrialRecord("tt_monaco_1", "monaco_riviera", "Monaco Riviera Grand Prix", "Max Verhoeven", "Apex Formula Open-Wheel", 52410L, 1, false, "CLEAR", true))
            list.add(TimeTrialRecord("tt_monaco_2", "monaco_riviera", "Monaco Riviera Grand Prix", "Lewis Stirling", "Veneno Hyper GT", 53120L, 2, false, "CLEAR", true))
            list.add(TimeTrialRecord("tt_monaco_3", "monaco_riviera", "Monaco Riviera Grand Prix", "Charles Leclercq", "Viper GTS V10", 53890L, 3, false, "CLEAR", true))
            list.add(TimeTrialRecord("tt_monaco_4", "monaco_riviera", "Monaco Riviera Grand Prix", "Takumi Fujiwara", "Evolution IX GSR", 54620L, 4, false, "CLEAR", true))
            list.add(TimeTrialRecord("tt_monaco_5", "monaco_riviera", "Monaco Riviera Grand Prix", "Apex Rookie", "Apex GT-86 Coupe", 56200L, 5, false, "CLEAR", true))

            // Apex International Speedway defaults
            list.add(TimeTrialRecord("tt_apex_1", "apex_speedway", "Apex International Speedway", "Sebastian Vettel", "Apex Formula Open-Wheel", 59850L, 1, false, "CLEAR", true))
            list.add(TimeTrialRecord("tt_apex_2", "apex_speedway", "Apex International Speedway", "Lando Norris", "Veneno Hyper GT", 60720L, 2, false, "CLEAR", true))
            list.add(TimeTrialRecord("tt_apex_3", "apex_speedway", "Apex International Speedway", "Fernando Alonso", "Viper GTS V10", 61540L, 3, false, "CLEAR", true))
            list.add(TimeTrialRecord("tt_apex_4", "apex_speedway", "Apex International Speedway", "Colin McRae", "Evolution IX GSR", 62890L, 4, false, "CLEAR", true))
            list.add(TimeTrialRecord("tt_apex_5", "apex_speedway", "Apex International Speedway", "Apex Rookie", "Apex GT-86 Coupe", 65120L, 5, false, "CLEAR", true))

            // Tokyo Midnight Expressway defaults
            list.add(TimeTrialRecord("tt_tokyo_1", "neon_tokyo", "Tokyo Midnight Expressway", "Keisuke Takahashi", "Veneno Hyper GT", 55320L, 1, false, "RAIN", true))
            list.add(TimeTrialRecord("tt_tokyo_2", "neon_tokyo", "Tokyo Midnight Expressway", "Ryosuke Takahashi", "Viper GTS V10", 56180L, 2, false, "RAIN", true))
            list.add(TimeTrialRecord("tt_tokyo_3", "neon_tokyo", "Tokyo Midnight Expressway", "Ken Block", "Evolution IX GSR", 57240L, 3, false, "RAIN", true))
            list.add(TimeTrialRecord("tt_tokyo_4", "neon_tokyo", "Tokyo Midnight Expressway", "Silvia Drift King", "Silvia S15 Spec-R", 58450L, 4, false, "RAIN", true))
            list.add(TimeTrialRecord("tt_tokyo_5", "neon_tokyo", "Tokyo Midnight Expressway", "Apex Rookie", "Apex GT-86 Coupe", 60980L, 5, false, "RAIN", true))

            // Mount Akina Touge Pass defaults
            list.add(TimeTrialRecord("tt_akina_1", "mount_akina", "Mount Akina Touge Pass", "Bunta Fujiwara", "Evolution IX GSR", 49870L, 1, false, "SNOW", true))
            list.add(TimeTrialRecord("tt_akina_2", "mount_akina", "Mount Akina Touge Pass", "Takumi Fujiwara", "Silvia S15 Spec-R", 50640L, 2, false, "SNOW", true))
            list.add(TimeTrialRecord("tt_akina_3", "mount_akina", "Mount Akina Touge Pass", "Keisuke Takahashi", "Viper GTS V10", 51920L, 3, false, "SNOW", true))
            list.add(TimeTrialRecord("tt_akina_4", "mount_akina", "Mount Akina Touge Pass", "Apex Master", "Veneno Hyper GT", 52830L, 4, false, "SNOW", true))
            list.add(TimeTrialRecord("tt_akina_5", "mount_akina", "Mount Akina Touge Pass", "Apex Rookie", "Apex GT-86 Coupe", 55410L, 5, false, "SNOW", true))

            return list
        }
    }
}
