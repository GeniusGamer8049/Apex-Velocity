package com.example.physics

import com.example.model.TireCompound
import com.example.model.Vehicle
import com.example.model.WeatherCondition
import com.example.model.WeatherType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.random.Random

/**
 * Standardized race weather condition supporting Sunny, Rain, and Snow.
 */
enum class RaceWeatherCondition(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val associatedWeatherType: WeatherType,
    val associatedCondition: WeatherCondition,
    val baseGripMultiplier: Float,
    val topSpeedMultiplier: Float,
    val brakingEfficiencyMultiplier: Float,
    val brakingDistanceMultiplier: Float,
    val accelerationTractionMultiplier: Float,
    val topSpeedDragMultiplier: Float,
    val steeringResponseMultiplier: Float,
    val targetTrackWetness: Float,
    val description: String
) {
    SUNNY(
        id = "sunny",
        displayName = "Sunny & Dry",
        iconEmoji = "☀️",
        associatedWeatherType = WeatherType.SUNNY_CLEAR,
        associatedCondition = WeatherCondition.CLEAR,
        baseGripMultiplier = 1.0f,
        topSpeedMultiplier = 1.0f,
        brakingEfficiencyMultiplier = 1.0f,
        brakingDistanceMultiplier = 1.0f,
        accelerationTractionMultiplier = 1.0f,
        topSpeedDragMultiplier = 1.0f,
        steeringResponseMultiplier = 1.0f,
        targetTrackWetness = 0.0f,
        description = "Optimal dry tarmac: peak tire adhesion, maximum top speed, shortest braking distance."
    ),
    RAIN(
        id = "rain",
        displayName = "Rain & Wet",
        iconEmoji = "🌧️",
        associatedWeatherType = WeatherType.LIGHT_RAIN,
        associatedCondition = WeatherCondition.RAIN,
        baseGripMultiplier = 0.70f,
        topSpeedMultiplier = 0.94f,
        brakingEfficiencyMultiplier = 0.68f,
        brakingDistanceMultiplier = 1.47f,
        accelerationTractionMultiplier = 0.65f,
        topSpeedDragMultiplier = 1.08f,
        steeringResponseMultiplier = 0.82f,
        targetTrackWetness = 0.80f,
        description = "Slick wet surface: 30% grip loss, increased aerodynamic & puddle drag, 47% longer braking distance."
    ),
    SNOW(
        id = "snow",
        displayName = "Snow & Ice",
        iconEmoji = "❄️",
        associatedWeatherType = WeatherType.FREEZING_ICE,
        associatedCondition = WeatherCondition.SNOW,
        baseGripMultiplier = 0.38f,
        topSpeedMultiplier = 0.85f,
        brakingEfficiencyMultiplier = 0.40f,
        brakingDistanceMultiplier = 2.50f,
        accelerationTractionMultiplier = 0.35f,
        topSpeedDragMultiplier = 1.18f,
        steeringResponseMultiplier = 0.52f,
        targetTrackWetness = 0.95f,
        description = "Compacted snow and ice: severe traction loss, 150% longer braking distance (2.5x), high drift angle."
    );

    companion object {
        fun fromWeatherType(weatherType: WeatherType): RaceWeatherCondition {
            return when (weatherType) {
                WeatherType.SUNNY_CLEAR, WeatherType.OVERCAST -> SUNNY
                WeatherType.LIGHT_RAIN, WeatherType.THUNDERSTORM, WeatherType.FOG_MIST -> RAIN
                WeatherType.FREEZING_ICE -> SNOW
            }
        }

        fun fromWeatherCondition(condition: WeatherCondition): RaceWeatherCondition {
            return when (condition) {
                WeatherCondition.CLEAR -> SUNNY
                WeatherCondition.RAIN -> RAIN
                WeatherCondition.SNOW -> SNOW
            }
        }
    }
}

/**
 * Snapshot of dynamically adjusted vehicle physics parameters modulated by the active race weather.
 */
data class AdjustedVehiclePhysics(
    val weather: RaceWeatherCondition = RaceWeatherCondition.SUNNY,
    val gripMultiplier: Float = 1.0f,
    val effectiveGrip: Float = 1.0f,
    val topSpeedMultiplier: Float = 1.0f,
    val effectiveTopSpeedKmh: Float = 240f,
    val brakingEfficiencyMultiplier: Float = 1.0f,
    val brakingDistanceMultiplier: Float = 1.0f,
    val brakingDistanceAt100Kmh: Float = 38.5f,
    val brakingDistanceAtCurrentSpeed: Float = 0f,
    val accelerationTractionMultiplier: Float = 1.0f,
    val topSpeedDragMultiplier: Float = 1.0f,
    val steeringResponseMultiplier: Float = 1.0f,
    val trackWetness: Float = 0.0f,
    val currentSpeedKmh: Float = 0f,
    val isAquaplaning: Boolean = false,
    val alertMessage: String? = null,
    val isRandomWeatherActive: Boolean = true,
    val secondsToNextRandomization: Int = 45,
    val forecastWeather: RaceWeatherCondition? = null,
    val transitionProgress: Float = 1.0f
) {
    val formattedGripPct: String
        get() = "${(gripMultiplier * 100).toInt()}%"

    val formattedTopSpeed: String
        get() = "${effectiveTopSpeedKmh.toInt()} km/h"

    val formattedBrakingDistance100: String
        get() = String.format(Locale.US, "%.1fm", brakingDistanceAt100Kmh)

    val formattedBrakingDistanceCurrent: String
        get() = String.format(Locale.US, "%.1fm", brakingDistanceAtCurrentSpeed)
}

/**
 * Service that dynamically adjusts vehicle physics parameters like grip, top speed,
 * and braking distance based on randomized weather conditions (Sunny, Rain, Snow) during races.
 */
class DynamicWeatherPhysicsService(
    private val random: Random = Random.Default
) {
    private val _physicsState = MutableStateFlow(AdjustedVehiclePhysics())
    val physicsState: StateFlow<AdjustedVehiclePhysics> = _physicsState.asStateFlow()

    // Configurable state
    var isRandomWeatherEnabled: Boolean = true
        private set

    var minRandomIntervalSec: Float = 30f
    var maxRandomIntervalSec: Float = 60f

    private var activeCondition: RaceWeatherCondition = RaceWeatherCondition.SUNNY
    private var targetCondition: RaceWeatherCondition = RaceWeatherCondition.SUNNY
    private var nextRandomCondition: RaceWeatherCondition? = null
    private var nextWeatherChangeTimestampSec: Float = 45f
    private var currentWetness: Float = 0f
    private var currentGripMultiplier: Float = 1.0f
    private var currentTopSpeedMultiplier: Float = 1.0f
    private var currentBrakingEfficiency: Float = 1.0f
    private var currentTractionMultiplier: Float = 1.0f
    private var currentDragMultiplier: Float = 1.0f
    private var currentSteeringMultiplier: Float = 1.0f

    init {
        scheduleNextRandomWeather(0f)
    }

    companion object {
        @Volatile
        private var INSTANCE: DynamicWeatherPhysicsService? = null

        fun getInstance(): DynamicWeatherPhysicsService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DynamicWeatherPhysicsService().also { INSTANCE = it }
            }
        }
    }

    /**
     * Enables or disables automatic in-race weather randomization.
     */
    fun setRandomWeatherEnabled(enabled: Boolean) {
        isRandomWeatherEnabled = enabled
        _physicsState.value = _physicsState.value.copy(isRandomWeatherActive = enabled)
    }

    /**
     * Resets service state for the beginning of a race.
     */
    fun reset(
        initialCondition: RaceWeatherCondition = RaceWeatherCondition.SUNNY,
        enableRandomization: Boolean = true
    ) {
        activeCondition = initialCondition
        targetCondition = initialCondition
        isRandomWeatherEnabled = enableRandomization
        currentWetness = initialCondition.targetTrackWetness
        currentGripMultiplier = initialCondition.baseGripMultiplier
        currentTopSpeedMultiplier = initialCondition.topSpeedMultiplier
        currentBrakingEfficiency = initialCondition.brakingEfficiencyMultiplier
        currentTractionMultiplier = initialCondition.accelerationTractionMultiplier
        currentDragMultiplier = initialCondition.topSpeedDragMultiplier
        currentSteeringMultiplier = initialCondition.steeringResponseMultiplier

        scheduleNextRandomWeather(0f)

        _physicsState.value = AdjustedVehiclePhysics(
            weather = initialCondition,
            gripMultiplier = initialCondition.baseGripMultiplier,
            topSpeedMultiplier = initialCondition.topSpeedMultiplier,
            brakingEfficiencyMultiplier = initialCondition.brakingEfficiencyMultiplier,
            brakingDistanceMultiplier = initialCondition.brakingDistanceMultiplier,
            accelerationTractionMultiplier = initialCondition.accelerationTractionMultiplier,
            topSpeedDragMultiplier = initialCondition.topSpeedDragMultiplier,
            steeringResponseMultiplier = initialCondition.steeringResponseMultiplier,
            trackWetness = currentWetness,
            isRandomWeatherActive = enableRandomization,
            secondsToNextRandomization = (nextWeatherChangeTimestampSec).toInt()
        )
    }

    /**
     * Calculates the stopping / braking distance in meters given speed and braking efficiency.
     * Formula: d = v^2 / (2 * a), where v = speed (m/s), a = deceleration (m/s^2).
     */
    fun calculateBrakingDistanceMeters(
        speedKmh: Float,
        brakingEfficiency: Float,
        baseBrakePower: Float = 1.0f
    ): Float {
        if (speedKmh <= 0.5f) return 0f
        val speedMs = speedKmh / 3.6f
        // Standard dry sports car deceleration ~ 1.35 * 9.81 m/s^2 (~13.2 m/s^2)
        val nominalDecel = 9.81f * 1.35f * baseBrakePower.coerceAtLeast(0.5f)
        val effectiveDecel = (nominalDecel * brakingEfficiency.coerceIn(0.15f, 1.5f))
        return (speedMs * speedMs) / (2f * effectiveDecel)
    }

    /**
     * Calculates effective top speed given base top speed and weather condition.
     */
    fun calculateEffectiveTopSpeed(
        baseTopSpeedKmh: Float,
        condition: RaceWeatherCondition = activeCondition
    ): Float {
        return baseTopSpeedKmh * condition.topSpeedMultiplier
    }

    /**
     * Calculates effective tire grip taking into account vehicle base grip,
     * weather condition, surface wetness, and equipped tire compound.
     */
    fun calculateEffectiveGrip(
        baseGrip: Float,
        condition: RaceWeatherCondition = activeCondition,
        tireCompound: TireCompound? = null,
        trackWetness: Float = condition.targetTrackWetness
    ): Float {
        val compoundDry = tireCompound?.dryGripMultiplier ?: 1.0f
        val compoundWet = tireCompound?.wetGripMultiplier ?: 0.78f
        val tireGripFactor = compoundDry * (1f - trackWetness) + compoundWet * trackWetness
        return baseGrip * tireGripFactor * condition.baseGripMultiplier
    }

    /**
     * Immediately forces a specific weather condition (Sunny, Rain, Snow)
     * and triggers physics parameter adjustments.
     */
    fun forceWeather(condition: RaceWeatherCondition) {
        targetCondition = condition
        activeCondition = condition
        currentWetness = condition.targetTrackWetness
        currentGripMultiplier = condition.baseGripMultiplier
        currentTopSpeedMultiplier = condition.topSpeedMultiplier
        currentBrakingEfficiency = condition.brakingEfficiencyMultiplier
        currentTractionMultiplier = condition.accelerationTractionMultiplier
        currentDragMultiplier = condition.topSpeedDragMultiplier
        currentSteeringMultiplier = condition.steeringResponseMultiplier

        val alert = when (condition) {
            RaceWeatherCondition.SUNNY -> "☀️ WEATHER: Track dry. Full grip and peak top speed restored!"
            RaceWeatherCondition.RAIN -> "🌧️ SUDDEN RAIN: Track slick! Braking distance +47%, reduced grip!"
            RaceWeatherCondition.SNOW -> "❄️ BLIZZARD WARNING: Compacted ice! Braking distance +150%, extreme slip!"
        }

        _physicsState.value = _physicsState.value.copy(
            weather = condition,
            gripMultiplier = condition.baseGripMultiplier,
            topSpeedMultiplier = condition.topSpeedMultiplier,
            brakingEfficiencyMultiplier = condition.brakingEfficiencyMultiplier,
            brakingDistanceMultiplier = condition.brakingDistanceMultiplier,
            accelerationTractionMultiplier = condition.accelerationTractionMultiplier,
            topSpeedDragMultiplier = condition.topSpeedDragMultiplier,
            steeringResponseMultiplier = condition.steeringResponseMultiplier,
            trackWetness = currentWetness,
            alertMessage = alert
        )
    }

    /**
     * Randomizes the race weather condition among Sunny, Rain, and Snow.
     */
    fun randomizeWeather(excludeCurrent: Boolean = true): RaceWeatherCondition {
        val all = RaceWeatherCondition.values()
        val candidates = if (excludeCurrent && all.size > 1) {
            all.filter { it != activeCondition }
        } else {
            all.toList()
        }
        return candidates[random.nextInt(candidates.size)]
    }

    /**
     * Manually triggers a random weather change event during the race.
     */
    fun triggerRandomWeatherChange(): RaceWeatherCondition {
        val newCondition = randomizeWeather(excludeCurrent = true)
        targetCondition = newCondition
        forceWeather(newCondition)
        return newCondition
    }

    private fun scheduleNextRandomWeather(currentElapsedSec: Float) {
        val interval = minRandomIntervalSec + random.nextFloat() * (maxRandomIntervalSec - minRandomIntervalSec)
        nextWeatherChangeTimestampSec = currentElapsedSec + interval
        nextRandomCondition = randomizeWeather(excludeCurrent = true)
    }

    /**
     * Per-frame dynamic physics update called from the race simulation loop.
     * Adjusts vehicle grip, top speed, braking distance, and handles randomized transitions.
     */
    fun update(
        raceElapsedSec: Float,
        dt: Float,
        vehicle: Vehicle,
        currentSpeedKmh: Float,
        trackWetnessOverride: Float? = null
    ): AdjustedVehiclePhysics {
        // 1. Check for randomized weather condition change
        var alertMsg = _physicsState.value.alertMessage
        if (isRandomWeatherEnabled && raceElapsedSec >= nextWeatherChangeTimestampSec) {
            val chosen = nextRandomCondition ?: randomizeWeather(excludeCurrent = true)
            targetCondition = chosen
            scheduleNextRandomWeather(raceElapsedSec)
            alertMsg = when (chosen) {
                RaceWeatherCondition.SUNNY -> "☀️ WEATHER SHIFT: Sun breaking through. Track drying up!"
                RaceWeatherCondition.RAIN -> "🌧️ WEATHER SHIFT: Sudden downpour! Braking distance +47%!"
                RaceWeatherCondition.SNOW -> "❄️ WEATHER SHIFT: Snow squall! Braking distance +150%, severe ice!"
            }
        }

        // 2. Smoothly interpolate physics parameters towards target weather condition
        val transitionRate = 0.40f * dt // Smooth ~2.5s transition
        activeCondition = targetCondition

        val targetWet = trackWetnessOverride ?: targetCondition.targetTrackWetness
        currentWetness += (targetWet - currentWetness) * (0.15f * dt)
        currentWetness = currentWetness.coerceIn(0f, 1f)

        currentGripMultiplier += (targetCondition.baseGripMultiplier - currentGripMultiplier) * transitionRate
        currentTopSpeedMultiplier += (targetCondition.topSpeedMultiplier - currentTopSpeedMultiplier) * transitionRate
        currentBrakingEfficiency += (targetCondition.brakingEfficiencyMultiplier - currentBrakingEfficiency) * transitionRate
        currentTractionMultiplier += (targetCondition.accelerationTractionMultiplier - currentTractionMultiplier) * transitionRate
        currentDragMultiplier += (targetCondition.topSpeedDragMultiplier - currentDragMultiplier) * transitionRate
        currentSteeringMultiplier += (targetCondition.steeringResponseMultiplier - currentSteeringMultiplier) * transitionRate

        // 3. Compute dynamic vehicle physics values
        val baseTopSpeedKmh = vehicle.effectiveTopSpeedKmh
        val dynamicTopSpeedKmh = baseTopSpeedKmh * currentTopSpeedMultiplier
        val effectiveGrip = calculateEffectiveGrip(
            baseGrip = vehicle.effectiveGripMultiplier,
            condition = targetCondition,
            tireCompound = vehicle.upgrades.tireCompound,
            trackWetness = currentWetness
        )

        val brakeDistance100 = calculateBrakingDistanceMeters(
            speedKmh = 100f,
            brakingEfficiency = currentBrakingEfficiency,
            baseBrakePower = vehicle.effectiveBrakingPower
        )
        val brakeDistanceCurrent = calculateBrakingDistanceMeters(
            speedKmh = currentSpeedKmh,
            brakingEfficiency = currentBrakingEfficiency,
            baseBrakePower = vehicle.effectiveBrakingPower
        )

        // Aquaplaning risk check: high speed in heavy rain/snow on non-wet compounds
        val isPuddleOrHeavyWet = currentWetness > 0.60f
        val isSlickTire = vehicle.upgrades.tireCompound == TireCompound.SOFT_SLICK ||
                vehicle.upgrades.tireCompound == TireCompound.SEMI_SLICK
        val isAquaplaning = (currentSpeedKmh > 75f) && isPuddleOrHeavyWet && isSlickTire

        val secondsRemaining = (nextWeatherChangeTimestampSec - raceElapsedSec).toInt().coerceAtLeast(0)

        // Inverse braking distance multiplier relative to dry
        val dynamicBrakingDistanceMult = 1.0f / currentBrakingEfficiency.coerceAtLeast(0.1f)

        val updated = AdjustedVehiclePhysics(
            weather = targetCondition,
            gripMultiplier = currentGripMultiplier,
            effectiveGrip = effectiveGrip,
            topSpeedMultiplier = currentTopSpeedMultiplier,
            effectiveTopSpeedKmh = dynamicTopSpeedKmh,
            brakingEfficiencyMultiplier = currentBrakingEfficiency,
            brakingDistanceMultiplier = dynamicBrakingDistanceMult,
            brakingDistanceAt100Kmh = brakeDistance100,
            brakingDistanceAtCurrentSpeed = brakeDistanceCurrent,
            accelerationTractionMultiplier = currentTractionMultiplier,
            topSpeedDragMultiplier = currentDragMultiplier,
            steeringResponseMultiplier = currentSteeringMultiplier,
            trackWetness = currentWetness,
            currentSpeedKmh = currentSpeedKmh,
            isAquaplaning = isAquaplaning,
            alertMessage = alertMsg,
            isRandomWeatherActive = isRandomWeatherEnabled,
            secondsToNextRandomization = secondsRemaining,
            forecastWeather = nextRandomCondition
        )

        _physicsState.value = updated
        return updated
    }
}
