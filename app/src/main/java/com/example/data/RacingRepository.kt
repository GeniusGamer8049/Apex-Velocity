package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.CareerRaceEvent
import com.example.model.CareerRegistry
import com.example.model.GhostFrame
import com.example.model.GhostLap
import com.example.model.TireCompound
import com.example.model.Vehicle
import com.example.model.VehicleRegistry
import com.example.model.VehicleUpgrades
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

import com.example.model.Achievement
import com.example.model.AchievementRegistry
import com.example.model.ChallengeType
import com.example.model.DailyChallenge
import com.example.model.DailyChallengeGenerator

data class PlayerProfile(
    val credits: Int = 2500,
    val reputation: Int = 0,
    val selectedVehicleId: String = "apex_gt86",
    val vehicles: List<Vehicle> = VehicleRegistry.createDefaultVehicles(),
    val bestLapTimes: Map<String, Long> = emptyMap(), // trackId -> timeMs
    val bestLapVehicles: Map<String, String> = emptyMap(), // trackId -> vehicleName
    val completedEventIds: Set<String> = emptySet(),
    val isAudioMuted: Boolean = false,
    val controlScheme: ControlScheme = ControlScheme.BUTTONS,
    val dailyChallenges: List<DailyChallenge> = emptyList(),
    val lastChallengeDateKey: String = "",
    val allChallengesBonusClaimed: Boolean = false,
    val driverName: String = "Apex Pilot",
    val driverCountry: String = "US",
    val totalRaceWins: Int = 0,
    val totalRacesCompleted: Int = 0,
    val totalDriftDistanceMeters: Float = 0f,
    val highestSpeedKmh: Float = 0f,
    val cleanRacesCount: Int = 0,
    val wetRaceWinsCount: Int = 0,
    val equippedBadgeId: String = "badge_rookie",
    val achievements: List<Achievement> = AchievementRegistry.createDefaultAchievements()
)

enum class ControlScheme(val displayName: String) {
    BUTTONS("Touch Buttons"),
    WHEEL("Virtual Steering Wheel")
}

class RacingRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("apex_racing_prefs", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()

    private val ghostLapsCache = mutableMapOf<String, GhostLap>()

    init {
        loadGhostLaps()
    }

    private fun loadProfile(): PlayerProfile {
        val credits = prefs.getInt("credits", 2500)
        val rep = prefs.getInt("reputation", 0)
        val selectedCar = prefs.getString("selected_vehicle_id", "apex_gt86") ?: "apex_gt86"
        val isMuted = prefs.getBoolean("audio_muted", false)
        val controlScheme = try {
            ControlScheme.valueOf(prefs.getString("control_scheme", ControlScheme.BUTTONS.name)!!)
        } catch (e: Exception) {
            ControlScheme.BUTTONS
        }

        // Load vehicles
        val defaultVehicles = VehicleRegistry.createDefaultVehicles()
        val vehiclesJson = prefs.getString("vehicles_json", null)
        val vehicles = if (vehiclesJson != null) {
            parseVehiclesJson(vehiclesJson, defaultVehicles)
        } else {
            defaultVehicles
        }

        // Load best lap times
        val bestTimesJson = prefs.getString("best_laps_json", null)
        val bestTimes = mutableMapOf<String, Long>()
        if (bestTimesJson != null) {
            try {
                val json = JSONObject(bestTimesJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    bestTimes[k] = json.getLong(k)
                }
            } catch (ignored: Exception) {}
        }

        // Load best lap vehicles
        val bestVehiclesJson = prefs.getString("best_lap_vehicles_json", null)
        val bestVehicles = mutableMapOf<String, String>()
        if (bestVehiclesJson != null) {
            try {
                val json = JSONObject(bestVehiclesJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    bestVehicles[k] = json.getString(k)
                }
            } catch (ignored: Exception) {}
        }

        // Driver Profile
        val driverName = prefs.getString("driver_name", "Apex Pilot") ?: "Apex Pilot"
        val driverCountry = prefs.getString("driver_country", "US") ?: "US"

        // Completed events
        val completedSet = prefs.getStringSet("completed_events", emptySet()) ?: emptySet()

        // Daily Challenges
        val todayKey = DailyChallengeGenerator.getTodayKey()
        val savedDateKey = prefs.getString("challenge_date_key", "") ?: ""
        val challengesJson = prefs.getString("challenges_json", null)
        val allBonusClaimed = if (savedDateKey == todayKey) prefs.getBoolean("all_challenges_bonus_claimed", false) else false

        val challenges = if (savedDateKey == todayKey && challengesJson != null) {
            val defaults = DailyChallengeGenerator.generateForDate(todayKey)
            parseChallengesJson(challengesJson, defaults)
        } else {
            DailyChallengeGenerator.generateForDate(todayKey)
        }

        // Milestones & Achievements
        val totalWins = prefs.getInt("total_race_wins", 0)
        val totalRaces = prefs.getInt("total_races_completed", 0)
        val driftMeters = prefs.getFloat("total_drift_meters", 0f)
        val highestSpeed = prefs.getFloat("highest_speed_kmh", 0f)
        val cleanRaces = prefs.getInt("clean_races_count", 0)
        val wetWins = prefs.getInt("wet_race_wins_count", 0)
        val equippedBadge = prefs.getString("equipped_badge_id", "badge_rookie") ?: "badge_rookie"

        val defaultAchievements = AchievementRegistry.createDefaultAchievements()
        val achievementsJson = prefs.getString("achievements_json", null)
        val achievements = if (achievementsJson != null) {
            parseAchievementsJson(achievementsJson, defaultAchievements)
        } else {
            defaultAchievements
        }

        return PlayerProfile(
            credits = credits,
            reputation = rep,
            selectedVehicleId = selectedCar,
            vehicles = vehicles,
            bestLapTimes = bestTimes,
            bestLapVehicles = bestVehicles,
            completedEventIds = completedSet,
            isAudioMuted = isMuted,
            controlScheme = controlScheme,
            dailyChallenges = challenges,
            lastChallengeDateKey = todayKey,
            allChallengesBonusClaimed = allBonusClaimed,
            driverName = driverName,
            driverCountry = driverCountry,
            totalRaceWins = totalWins,
            totalRacesCompleted = totalRaces,
            totalDriftDistanceMeters = driftMeters,
            highestSpeedKmh = highestSpeed,
            cleanRacesCount = cleanRaces,
            wetRaceWinsCount = wetWins,
            equippedBadgeId = equippedBadge,
            achievements = achievements
        )
    }

    fun saveProfile(profile: PlayerProfile) {
        _profile.value = profile
        prefs.edit().apply {
            putInt("credits", profile.credits)
            putInt("reputation", profile.reputation)
            putString("selected_vehicle_id", profile.selectedVehicleId)
            putBoolean("audio_muted", profile.isAudioMuted)
            putString("control_scheme", profile.controlScheme.name)
            putString("vehicles_json", serializeVehiclesJson(profile.vehicles))

            val bestTimesObj = JSONObject()
            profile.bestLapTimes.forEach { (k, v) -> bestTimesObj.put(k, v) }
            putString("best_laps_json", bestTimesObj.toString())

            val bestVehiclesObj = JSONObject()
            profile.bestLapVehicles.forEach { (k, v) -> bestVehiclesObj.put(k, v) }
            putString("best_lap_vehicles_json", bestVehiclesObj.toString())

            putString("driver_name", profile.driverName)
            putString("driver_country", profile.driverCountry)

            putStringSet("completed_events", profile.completedEventIds)

            putString("challenge_date_key", profile.lastChallengeDateKey)
            putString("challenges_json", serializeChallengesJson(profile.dailyChallenges))
            putBoolean("all_challenges_bonus_claimed", profile.allChallengesBonusClaimed)

            putInt("total_race_wins", profile.totalRaceWins)
            putInt("total_races_completed", profile.totalRacesCompleted)
            putFloat("total_drift_meters", profile.totalDriftDistanceMeters)
            putFloat("highest_speed_kmh", profile.highestSpeedKmh)
            putInt("clean_races_count", profile.cleanRacesCount)
            putInt("wet_race_wins_count", profile.wetRaceWinsCount)
            putString("equipped_badge_id", profile.equippedBadgeId)
            putString("achievements_json", serializeAchievementsJson(profile.achievements))
            apply()
        }
    }

    fun updateChallengeProgress(type: ChallengeType, addValue: Int = 0, absoluteValue: Int = 0) {
        val curr = _profile.value
        var changed = false
        val updated = curr.dailyChallenges.map { ch ->
            if (ch.type == type && !ch.isClaimed) {
                val newVal = if (absoluteValue > 0) {
                    maxOf(ch.currentValue, absoluteValue)
                } else {
                    ch.currentValue + addValue
                }
                if (newVal != ch.currentValue) {
                    changed = true
                    ch.copy(currentValue = newVal.coerceAtMost(ch.targetValue))
                } else ch
            } else ch
        }
        if (changed) {
            saveProfile(curr.copy(dailyChallenges = updated))
        }
    }

    fun claimChallengeReward(challengeId: String): Boolean {
        val curr = _profile.value
        val challenge = curr.dailyChallenges.find { it.id == challengeId } ?: return false
        if (!challenge.isCompleted || challenge.isClaimed) return false

        val updated = curr.dailyChallenges.map {
            if (it.id == challengeId) it.copy(isClaimed = true) else it
        }
        saveProfile(
            curr.copy(
                credits = curr.credits + challenge.rewardCredits,
                reputation = curr.reputation + challenge.rewardRep,
                dailyChallenges = updated
            )
        )
        return true
    }

    fun claimAllDailyBonus(): Boolean {
        val curr = _profile.value
        if (curr.allChallengesBonusClaimed) return false
        if (!curr.dailyChallenges.all { it.isCompleted }) return false

        saveProfile(
            curr.copy(
                credits = curr.credits + 10000,
                reputation = curr.reputation + 300,
                allChallengesBonusClaimed = true
            )
        )
        return true
    }

    fun addRewards(earnedCredits: Int, earnedRep: Int) {
        val curr = _profile.value
        val updated = curr.copy(
            credits = curr.credits + earnedCredits,
            reputation = curr.reputation + earnedRep
        )
        saveProfile(updated)
    }

    fun purchaseVehicle(vehicleId: String): Boolean {
        val curr = _profile.value
        val vehicle = curr.vehicles.find { it.id == vehicleId } ?: return false
        if (curr.credits < vehicle.price || curr.reputation < vehicle.repRequired) return false

        val updatedVehicles = curr.vehicles.map {
            if (it.id == vehicleId) it.copy(isUnlocked = true) else it
        }
        val unlockedCount = updatedVehicles.count { it.isUnlocked }.toFloat()
        val updatedAchievements = curr.achievements.map { ach ->
            if (ach.id == "badge_fleet_3") {
                val willUnlock = !ach.isUnlocked && unlockedCount >= ach.targetValue
                ach.copy(
                    currentValue = unlockedCount,
                    isUnlocked = ach.isUnlocked || willUnlock,
                    unlockedTimestamp = if (willUnlock) System.currentTimeMillis() else ach.unlockedTimestamp
                )
            } else ach
        }

        val updated = curr.copy(
            credits = curr.credits - vehicle.price,
            vehicles = updatedVehicles,
            selectedVehicleId = vehicleId,
            achievements = updatedAchievements
        )
        saveProfile(updated)
        return true
    }

    fun selectVehicle(vehicleId: String) {
        val curr = _profile.value
        if (curr.vehicles.any { it.id == vehicleId && it.isUnlocked }) {
            saveProfile(curr.copy(selectedVehicleId = vehicleId))
        }
    }

    fun upgradeVehicle(vehicleId: String, category: String): Boolean {
        val curr = _profile.value
        val v = curr.vehicles.find { it.id == vehicleId } ?: return false
        val u = v.upgrades

        val nextStage = when (category) {
            "engine" -> u.engineStage + 1
            "tires" -> u.tireStage + 1
            "suspension" -> u.suspensionStage + 1
            "transmission" -> u.transmissionStage + 1
            "brakes" -> u.brakesStage + 1
            "aero" -> u.aeroStage + 1
            "weight" -> u.weightStage + 1
            else -> return false
        }
        if (nextStage > 5) return false

        val cost = VehicleUpgrades.costForUpgrade(category, nextStage)
        if (curr.credits < cost) return false

        val newUpgrades = when (category) {
            "engine" -> u.copy(engineStage = nextStage)
            "tires" -> u.copy(tireStage = nextStage)
            "suspension" -> u.copy(suspensionStage = nextStage)
            "transmission" -> u.copy(transmissionStage = nextStage)
            "brakes" -> u.copy(brakesStage = nextStage)
            "aero" -> u.copy(aeroStage = nextStage)
            "weight" -> u.copy(weightStage = nextStage)
            else -> u
        }

        val updatedVehicles = curr.vehicles.map {
            if (it.id == vehicleId) it.copy(upgrades = newUpgrades) else it
        }

        val hasStage5 = nextStage >= 5 || updatedVehicles.any {
            it.upgrades.engineStage >= 5 || it.upgrades.tireStage >= 5 ||
            it.upgrades.transmissionStage >= 5 || it.upgrades.brakesStage >= 5 ||
            it.upgrades.aeroStage >= 5 || it.upgrades.suspensionStage >= 5
        }

        val updatedAchievements = curr.achievements.map { ach ->
            if (ach.id == "badge_tuning_max") {
                val willUnlock = !ach.isUnlocked && hasStage5
                ach.copy(
                    currentValue = if (hasStage5) 1f else 0f,
                    isUnlocked = ach.isUnlocked || willUnlock,
                    unlockedTimestamp = if (willUnlock) System.currentTimeMillis() else ach.unlockedTimestamp
                )
            } else ach
        }

        saveProfile(curr.copy(credits = curr.credits - cost, vehicles = updatedVehicles, achievements = updatedAchievements))
        return true
    }

    fun equipTireCompound(vehicleId: String, compound: TireCompound): Boolean {
        val curr = _profile.value
        val v = curr.vehicles.find { it.id == vehicleId } ?: return false
        val cost = VehicleUpgrades.tireCost(compound)
        if (v.upgrades.tireCompound != compound && cost > 0 && curr.credits < cost) return false

        val newCredits = if (v.upgrades.tireCompound != compound) curr.credits - cost else curr.credits
        val newUpgrades = v.upgrades.copy(tireCompound = compound)
        val updatedVehicles = curr.vehicles.map {
            if (it.id == vehicleId) it.copy(upgrades = newUpgrades) else it
        }
        saveProfile(curr.copy(credits = newCredits, vehicles = updatedVehicles))
        return true
    }

    fun updateVehicleCustomization(vehicleId: String, colorHex: String, underglowHex: String?) {
        val curr = _profile.value
        val updatedVehicles = curr.vehicles.map {
            if (it.id == vehicleId) it.copy(bodyColorHex = colorHex, underglowColorHex = underglowHex) else it
        }
        saveProfile(curr.copy(vehicles = updatedVehicles))
    }

    fun completeRaceEvent(eventId: String, position: Int, prize: Int, repGain: Int) {
        val curr = _profile.value
        val updatedSet = curr.completedEventIds.toMutableSet().apply { add(eventId) }
        saveProfile(
            curr.copy(
                credits = curr.credits + prize,
                reputation = curr.reputation + repGain,
                completedEventIds = updatedSet
            )
        )
    }

    fun recordBestLap(trackId: String, lapTimeMs: Long, ghostLap: GhostLap?, vehicleName: String = "Apex GT-86 Coupe") {
        val curr = _profile.value
        val currentBest = curr.bestLapTimes[trackId] ?: Long.MAX_VALUE
        if (lapTimeMs < currentBest) {
            val updatedTimes = curr.bestLapTimes.toMutableMap().apply { put(trackId, lapTimeMs) }
            val updatedVehicles = curr.bestLapVehicles.toMutableMap().apply { put(trackId, vehicleName) }
            saveProfile(curr.copy(bestLapTimes = updatedTimes, bestLapVehicles = updatedVehicles))
            if (ghostLap != null) {
                ghostLapsCache[trackId] = ghostLap
                saveGhostLap(trackId, ghostLap)
            }
        }
    }

    fun updateDriverProfile(driverName: String, countryCode: String) {
        val curr = _profile.value
        saveProfile(
            curr.copy(
                driverName = driverName.trim().ifEmpty { "Apex Pilot" },
                driverCountry = countryCode.trim().uppercase().ifEmpty { "US" }
            )
        )
    }

    fun getLeaderboard(trackId: String): List<com.example.model.LeaderboardEntry> {
        val curr = _profile.value
        val pb = curr.bestLapTimes[trackId]
        val vehicleName = curr.bestLapVehicles[trackId]
            ?: (curr.vehicles.find { it.id == curr.selectedVehicleId }?.name ?: "Apex GT-86 Coupe")
        val vehicleCategory = curr.vehicles.find { it.name == vehicleName }?.category ?: "Sport Compact"
        return com.example.model.GlobalLeaderboardRegistry.buildTrackLeaderboard(
            trackId = trackId,
            playerPbMs = pb,
            playerName = curr.driverName,
            playerCountry = curr.driverCountry,
            playerVehicleName = vehicleName,
            playerVehicleCategory = vehicleCategory
        )
    }

    fun getGhostLap(trackId: String): GhostLap? {
        return ghostLapsCache[trackId]
    }

    fun toggleAudio() {
        val curr = _profile.value
        saveProfile(curr.copy(isAudioMuted = !curr.isAudioMuted))
    }

    fun setControlScheme(scheme: ControlScheme) {
        val curr = _profile.value
        saveProfile(curr.copy(controlScheme = scheme))
    }

    private fun saveGhostLap(trackId: String, ghostLap: GhostLap) {
        try {
            val root = JSONObject()
            root.put("trackId", ghostLap.trackId)
            root.put("carName", ghostLap.carName)
            root.put("lapTimeMs", ghostLap.lapTimeMs)

            val arr = JSONArray()
            // Sample every 2nd frame to keep storage compact
            for (i in ghostLap.frames.indices step 2) {
                val f = ghostLap.frames[i]
                val item = JSONObject()
                item.put("t", f.timeMs)
                item.put("x", f.x.toInt())
                item.put("y", f.y.toInt())
                item.put("h", f.heading.toInt())
                item.put("s", f.speedKmh.toInt())
                arr.put(item)
            }
            root.put("frames", arr)
            prefs.edit().putString("ghost_$trackId", root.toString()).apply()
        } catch (ignored: Exception) {}
    }

    private fun loadGhostLaps() {
        val trackIds = listOf("monaco_riviera", "apex_speedway", "neon_tokyo", "mount_akina")
        for (tid in trackIds) {
            val raw = prefs.getString("ghost_$tid", null) ?: continue
            try {
                val root = JSONObject(raw)
                val carName = root.getString("carName")
                val lapTimeMs = root.getLong("lapTimeMs")
                val arr = root.getJSONArray("frames")
                val frames = mutableListOf<GhostFrame>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    frames.add(
                        GhostFrame(
                            timeMs = item.getLong("t"),
                            x = item.getInt("x").toFloat(),
                            y = item.getInt("y").toFloat(),
                            heading = item.getInt("h").toFloat(),
                            speedKmh = item.getInt("s").toFloat()
                        )
                    )
                }
                ghostLapsCache[tid] = GhostLap(tid, carName, lapTimeMs, frames)
            } catch (ignored: Exception) {}
        }
    }

    private fun serializeVehiclesJson(vehicles: List<Vehicle>): String {
        val arr = JSONArray()
        for (v in vehicles) {
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("unlocked", v.isUnlocked)
            obj.put("color", v.bodyColorHex)
            obj.put("underglow", v.underglowColorHex ?: "")
            val u = v.upgrades
            obj.put("engineStage", u.engineStage)
            obj.put("tiresCompound", u.tireCompound.name)
            obj.put("tireStage", u.tireStage)
            obj.put("suspensionStage", u.suspensionStage)
            obj.put("transmissionStage", u.transmissionStage)
            obj.put("brakesStage", u.brakesStage)
            obj.put("aeroStage", u.aeroStage)
            obj.put("weightStage", u.weightStage)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseVehiclesJson(jsonStr: String, defaults: List<Vehicle>): List<Vehicle> {
        return try {
            val arr = JSONArray(jsonStr)
            val map = mutableMapOf<String, JSONObject>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                map[o.getString("id")] = o
            }

            defaults.map { def ->
                val o = map[def.id]
                if (o != null) {
                    val unlocked = o.optBoolean("unlocked", def.isUnlocked)
                    val color = o.optString("color", def.bodyColorHex)
                    val ug = o.optString("underglow", "")
                    val underglow = if (ug.isNotEmpty()) ug else null

                    val tireCompoundName = o.optString("tiresCompound", def.upgrades.tireCompound.name)
                    val compound = try { TireCompound.valueOf(tireCompoundName) } catch (e: Exception) { TireCompound.SPORT_RADIAL }

                    val upgrades = VehicleUpgrades(
                        engineStage = o.optInt("engineStage", def.upgrades.engineStage),
                        tireCompound = compound,
                        tireStage = o.optInt("tireStage", def.upgrades.tireStage),
                        suspensionStage = o.optInt("suspensionStage", def.upgrades.suspensionStage),
                        transmissionStage = o.optInt("transmissionStage", def.upgrades.transmissionStage),
                        brakesStage = o.optInt("brakesStage", def.upgrades.brakesStage),
                        aeroStage = o.optInt("aeroStage", def.upgrades.aeroStage),
                        weightStage = o.optInt("weightStage", def.upgrades.weightStage)
                    )

                    def.copy(
                        isUnlocked = unlocked,
                        bodyColorHex = color,
                        underglowColorHex = underglow,
                        upgrades = upgrades
                    )
                } else {
                    def
                }
            }
        } catch (e: Exception) {
            defaults
        }
    }

    private fun serializeChallengesJson(challenges: List<DailyChallenge>): String {
        val arr = JSONArray()
        for (c in challenges) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("val", c.currentValue)
            obj.put("claimed", c.isClaimed)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseChallengesJson(jsonStr: String, defaults: List<DailyChallenge>): List<DailyChallenge> {
        return try {
            val arr = JSONArray(jsonStr)
            val map = mutableMapOf<String, Pair<Int, Boolean>>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                map[o.getString("id")] = Pair(o.getInt("val"), o.getBoolean("claimed"))
            }
            defaults.map { def ->
                val p = map[def.id]
                if (p != null) {
                    def.copy(currentValue = p.first, isClaimed = p.second)
                } else def
            }
        } catch (e: Exception) {
            defaults
        }
    }

    fun recordRaceMilestones(
        isWin: Boolean,
        isClean: Boolean,
        isWetWin: Boolean,
        maxSpeedKmh: Float,
        driftMeters: Float
    ): List<Achievement> {
        val curr = _profile.value
        val newWins = if (isWin) curr.totalRaceWins + 1 else curr.totalRaceWins
        val newRaces = curr.totalRacesCompleted + 1
        val newDrift = curr.totalDriftDistanceMeters + driftMeters
        val newHighestSpeed = maxOf(curr.highestSpeedKmh, maxSpeedKmh)
        val newCleanRaces = if (isClean) curr.cleanRacesCount + 1 else curr.cleanRacesCount
        val newWetWins = if (isWetWin) curr.wetRaceWinsCount + 1 else curr.wetRaceWinsCount

        val newlyUnlocked = mutableListOf<Achievement>()

        val updatedAchievements = curr.achievements.map { ach ->
            val newVal = when (ach.id) {
                "badge_win_1", "badge_win_5", "badge_win_10", "badge_win_25" -> newWins.toFloat()
                "badge_speed_200", "badge_speed_250", "badge_speed_300" -> newHighestSpeed
                "badge_drift_500m", "badge_drift_2500m", "badge_drift_10000m" -> newDrift
                "badge_clean_5" -> newCleanRaces.toFloat()
                "badge_wet_3" -> newWetWins.toFloat()
                "badge_fleet_3" -> curr.vehicles.count { it.isUnlocked }.toFloat()
                "badge_tuning_max" -> if (curr.vehicles.any {
                        it.upgrades.engineStage >= 5 || it.upgrades.tireStage >= 5 ||
                        it.upgrades.transmissionStage >= 5 || it.upgrades.brakesStage >= 5 ||
                        it.upgrades.aeroStage >= 5 || it.upgrades.suspensionStage >= 5
                    }) 1f else 0f
                else -> ach.currentValue
            }

            val willUnlock = !ach.isUnlocked && newVal >= ach.targetValue
            if (willUnlock) {
                val unlockedAch = ach.copy(
                    currentValue = newVal,
                    isUnlocked = true,
                    unlockedTimestamp = System.currentTimeMillis()
                )
                newlyUnlocked.add(unlockedAch)
                unlockedAch
            } else {
                ach.copy(currentValue = maxOf(ach.currentValue, newVal))
            }
        }

        val updatedProfile = curr.copy(
            totalRaceWins = newWins,
            totalRacesCompleted = newRaces,
            totalDriftDistanceMeters = newDrift,
            highestSpeedKmh = newHighestSpeed,
            cleanRacesCount = newCleanRaces,
            wetRaceWinsCount = newWetWins,
            achievements = updatedAchievements
        )
        saveProfile(updatedProfile)
        return newlyUnlocked
    }

    fun claimAchievementReward(achievementId: String): Boolean {
        val curr = _profile.value
        val ach = curr.achievements.find { it.id == achievementId } ?: return false
        if (!ach.isUnlocked || ach.isClaimed) return false

        val updated = curr.achievements.map {
            if (it.id == achievementId) it.copy(isClaimed = true) else it
        }
        val newProfile = curr.copy(
            credits = curr.credits + ach.rewardCredits,
            reputation = curr.reputation + ach.rewardRep,
            achievements = updated
        )
        saveProfile(newProfile)
        return true
    }

    fun equipBadge(badgeId: String): Boolean {
        val curr = _profile.value
        val ach = curr.achievements.find { it.id == badgeId } ?: return false
        if (!ach.isUnlocked) return false

        saveProfile(curr.copy(equippedBadgeId = badgeId))
        return true
    }

    private fun serializeAchievementsJson(achievements: List<Achievement>): String {
        val arr = JSONArray()
        for (a in achievements) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("val", a.currentValue.toDouble())
            obj.put("unlocked", a.isUnlocked)
            obj.put("claimed", a.isClaimed)
            obj.put("time", a.unlockedTimestamp ?: 0L)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseAchievementsJson(jsonStr: String, defaults: List<Achievement>): List<Achievement> {
        return try {
            val arr = JSONArray(jsonStr)
            val jsonMap = mutableMapOf<String, JSONObject>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                jsonMap[o.getString("id")] = o
            }
            defaults.map { def ->
                val o = jsonMap[def.id]
                if (o != null) {
                    val v = o.optDouble("val", def.currentValue.toDouble()).toFloat()
                    val u = o.optBoolean("unlocked", def.isUnlocked)
                    val c = o.optBoolean("claimed", def.isClaimed)
                    val t = o.optLong("time", 0L)
                    def.copy(
                        currentValue = v,
                        isUnlocked = u,
                        isClaimed = c,
                        unlockedTimestamp = if (t > 0L) t else def.unlockedTimestamp
                    )
                } else def
            }
        } catch (e: Exception) {
            defaults
        }
    }
}
