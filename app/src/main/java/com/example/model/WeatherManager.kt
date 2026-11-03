package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Standard weather conditions selectable during a race.
 */
enum class WeatherCondition(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val gripMultiplier: Float,
    val steeringResponseMultiplier: Float,
    val brakingEfficiencyMultiplier: Float,
    val accelerationTractionMultiplier: Float,
    val topSpeedDragMultiplier: Float,
    val targetTrackWetness: Float,
    val associatedWeatherType: WeatherType,
    val description: String
) {
    CLEAR(
        id = "clear",
        displayName = "Clear",
        iconEmoji = "☀️",
        gripMultiplier = 1.0f,
        steeringResponseMultiplier = 1.0f,
        brakingEfficiencyMultiplier = 1.0f,
        accelerationTractionMultiplier = 1.0f,
        topSpeedDragMultiplier = 1.0f,
        targetTrackWetness = 0.0f,
        associatedWeatherType = WeatherType.SUNNY_CLEAR,
        description = "Dry optimal tarmac, peak adhesion, maximum braking power"
    ),
    RAIN(
        id = "rain",
        displayName = "Rain",
        iconEmoji = "🌧️",
        gripMultiplier = 0.72f,
        steeringResponseMultiplier = 0.82f,
        brakingEfficiencyMultiplier = 0.75f,
        accelerationTractionMultiplier = 0.70f,
        topSpeedDragMultiplier = 0.96f,
        targetTrackWetness = 0.75f,
        associatedWeatherType = WeatherType.LIGHT_RAIN,
        description = "Slick wet surface, reduced tire grip, aquaplaning risk, longer braking distances"
    ),
    SNOW(
        id = "snow",
        displayName = "Snow",
        iconEmoji = "❄️",
        gripMultiplier = 0.40f,
        steeringResponseMultiplier = 0.52f,
        brakingEfficiencyMultiplier = 0.44f,
        accelerationTractionMultiplier = 0.38f,
        topSpeedDragMultiplier = 0.90f,
        targetTrackWetness = 0.90f,
        associatedWeatherType = WeatherType.FREEZING_ICE,
        description = "Treacherous compacted snow and ice, severe traction loss, high drift slip angle"
    );

    companion object {
        fun fromWeatherType(weatherType: WeatherType): WeatherCondition {
            return when (weatherType) {
                WeatherType.SUNNY_CLEAR, WeatherType.OVERCAST -> CLEAR
                WeatherType.LIGHT_RAIN, WeatherType.THUNDERSTORM, WeatherType.FOG_MIST -> RAIN
                WeatherType.FREEZING_ICE -> SNOW
            }
        }
    }
}

/**
 * Detailed physics parameters modulated by the active weather condition.
 */
data class WeatherHandlingParameters(
    val gripMultiplier: Float,
    val steeringResponseMultiplier: Float,
    val brakingEfficiencyMultiplier: Float,
    val accelerationTractionMultiplier: Float,
    val topSpeedDragMultiplier: Float,
    val tireCoolingRate: Float,
    val lateralSlipThresholdDeg: Float
)

/**
 * Singleton manager coordinating live weather conditions and their direct impact
 * on vehicle handling parameters during race simulation.
 */
object WeatherManager {

    private val _currentCondition = MutableStateFlow(WeatherCondition.CLEAR)
    val currentCondition: StateFlow<WeatherCondition> = _currentCondition.asStateFlow()

    /**
     * Updates the current weather condition and applies changes to vehicle physics parameters.
     */
    fun setCondition(condition: WeatherCondition) {
        _currentCondition.value = condition
    }

    /**
     * Cycles to the next weather condition (Clear -> Rain -> Snow -> Clear).
     */
    fun cycleNextCondition(): WeatherCondition {
        val all = WeatherCondition.values()
        val nextIdx = (all.indexOf(_currentCondition.value) + 1) % all.size
        val nextCondition = all[nextIdx]
        setCondition(nextCondition)
        return nextCondition
    }

    /**
     * Returns the dynamic handling parameters applied to the vehicle physics engine.
     */
    fun getHandlingParameters(condition: WeatherCondition = _currentCondition.value): WeatherHandlingParameters {
        return when (condition) {
            WeatherCondition.CLEAR -> WeatherHandlingParameters(
                gripMultiplier = 1.0f,
                steeringResponseMultiplier = 1.0f,
                brakingEfficiencyMultiplier = 1.0f,
                accelerationTractionMultiplier = 1.0f,
                topSpeedDragMultiplier = 1.0f,
                tireCoolingRate = 1.2f,
                lateralSlipThresholdDeg = 12f
            )
            WeatherCondition.RAIN -> WeatherHandlingParameters(
                gripMultiplier = 0.72f,
                steeringResponseMultiplier = 0.82f,
                brakingEfficiencyMultiplier = 0.75f,
                accelerationTractionMultiplier = 0.70f,
                topSpeedDragMultiplier = 0.96f,
                tireCoolingRate = 4.0f,
                lateralSlipThresholdDeg = 8.5f
            )
            WeatherCondition.SNOW -> WeatherHandlingParameters(
                gripMultiplier = 0.40f,
                steeringResponseMultiplier = 0.52f,
                brakingEfficiencyMultiplier = 0.44f,
                accelerationTractionMultiplier = 0.38f,
                topSpeedDragMultiplier = 0.90f,
                tireCoolingRate = 6.5f,
                lateralSlipThresholdDeg = 5.0f
            )
        }
    }

    /**
     * Overload resolving handling parameters from [WeatherType].
     */
    fun getHandlingParameters(weatherType: WeatherType): WeatherHandlingParameters {
        return getHandlingParameters(WeatherCondition.fromWeatherType(weatherType))
    }

    /**
     * Calculates the effective tire grip combining vehicle base grip with active weather impact.
     */
    fun calculateEffectiveGrip(baseGrip: Float, condition: WeatherCondition = _currentCondition.value): Float {
        return baseGrip * condition.gripMultiplier
    }

    /**
     * Calculates effective braking force factor for the current condition.
     */
    fun calculateEffectiveBraking(baseBrakeForce: Float, condition: WeatherCondition = _currentCondition.value): Float {
        return baseBrakeForce * condition.brakingEfficiencyMultiplier
    }

    /**
     * Calculates acceleration traction for the current condition to model wheelspin.
     */
    fun calculateEffectiveTraction(baseDrivingForce: Float, condition: WeatherCondition = _currentCondition.value): Float {
        return baseDrivingForce * condition.accelerationTractionMultiplier
    }
}
