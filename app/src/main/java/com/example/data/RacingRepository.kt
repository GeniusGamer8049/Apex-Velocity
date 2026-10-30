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

data class PlayerProfile(
    val credits: Int = 2500,
    val reputation: Int = 0,
    val selectedVehicleId: String = "apex_gt86",
    val vehicles: List<Vehicle> = VehicleRegistry.createDefaultVehicles(),
    val bestLapTimes: Map<String, Long> = emptyMap(), // trackId -> timeMs
    val completedEventIds: Set<String> = emptySet(),
    val isAudioMuted: Boolean = false,
    val controlScheme: ControlScheme = ControlScheme.BUTTONS
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

        // Completed events
        val completedSet = prefs.getStringSet("completed_events", emptySet()) ?: emptySet()

        return PlayerProfile(
            credits = credits,
            reputation = rep,
            selectedVehicleId = selectedCar,
            vehicles = vehicles,
            bestLapTimes = bestTimes,
            completedEventIds = completedSet,
            isAudioMuted = isMuted,
            controlScheme = controlScheme
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

            putStringSet("completed_events", profile.completedEventIds)
            apply()
        }
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
        val updated = curr.copy(
            credits = curr.credits - vehicle.price,
            vehicles = updatedVehicles,
            selectedVehicleId = vehicleId
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

        saveProfile(curr.copy(credits = curr.credits - cost, vehicles = updatedVehicles))
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

    fun recordBestLap(trackId: String, lapTimeMs: Long, ghostLap: GhostLap?) {
        val curr = _profile.value
        val currentBest = curr.bestLapTimes[trackId] ?: Long.MAX_VALUE
        if (lapTimeMs < currentBest) {
            val updatedTimes = curr.bestLapTimes.toMutableMap().apply { put(trackId, lapTimeMs) }
            saveProfile(curr.copy(bestLapTimes = updatedTimes))
            if (ghostLap != null) {
                ghostLapsCache[trackId] = ghostLap
                saveGhostLap(trackId, ghostLap)
            }
        }
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
}
