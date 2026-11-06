package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AsphaltColor
import com.example.ui.theme.AsphaltWetColor
import com.example.ui.theme.DampGrassColor
import com.example.ui.theme.GrassGreenColor
import com.example.ui.theme.IcePatchColor
import com.example.ui.theme.SnowGroundColor
import com.example.ui.theme.SnowTrackColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.random.Random

/**
 * Weather precipitation visual type.
 */
enum class WeatherPrecipitationType {
    NONE,
    RAIN_STREAKS,
    SNOW_FLURRIES
}

/**
 * Standard weather conditions selectable and randomized at race start.
 * Supports Sunny (CLEAR), Rainy (RAIN), and Snowy (SNOW).
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
        id = "sunny",
        displayName = "Sunny & Dry",
        iconEmoji = "☀️",
        gripMultiplier = 1.0f,
        steeringResponseMultiplier = 1.0f,
        brakingEfficiencyMultiplier = 1.0f,
        accelerationTractionMultiplier = 1.0f,
        topSpeedDragMultiplier = 1.0f,
        targetTrackWetness = 0.0f,
        associatedWeatherType = WeatherType.SUNNY_CLEAR,
        description = "Dry optimal tarmac, peak adhesion, maximum braking power, full traction"
    ),
    RAIN(
        id = "rainy",
        displayName = "Rainy & Wet",
        iconEmoji = "🌧️",
        gripMultiplier = 0.72f,
        steeringResponseMultiplier = 0.82f,
        brakingEfficiencyMultiplier = 0.75f,
        accelerationTractionMultiplier = 0.70f,
        topSpeedDragMultiplier = 0.96f,
        targetTrackWetness = 0.75f,
        associatedWeatherType = WeatherType.LIGHT_RAIN,
        description = "Slick wet surface, reduced tire traction, standing water puddles, aquaplaning hazard"
    ),
    SNOW(
        id = "snowy",
        displayName = "Snowy & Icy",
        iconEmoji = "❄️",
        gripMultiplier = 0.40f,
        steeringResponseMultiplier = 0.52f,
        brakingEfficiencyMultiplier = 0.44f,
        accelerationTractionMultiplier = 0.38f,
        topSpeedDragMultiplier = 0.90f,
        targetTrackWetness = 0.90f,
        associatedWeatherType = WeatherType.FREEZING_ICE,
        description = "Treacherous compacted snow and ice, severe traction loss, 2.5x braking distance, high drift angle"
    );

    val isSunny: Boolean
        get() = this == CLEAR

    val isRainy: Boolean
        get() = this == RAIN

    val isSnowy: Boolean
        get() = this == SNOW

    val brakingDistanceMultiplier: Float
        get() = 1.0f / brakingEfficiencyMultiplier

    companion object {
        // Direct aliases for Sunny, Rainy, Snowy
        val SUNNY = CLEAR
        val RAINY = RAIN
        val SNOWY = SNOW

        fun fromWeatherType(weatherType: WeatherType): WeatherCondition {
            return when (weatherType) {
                WeatherType.SUNNY_CLEAR, WeatherType.OVERCAST -> CLEAR
                WeatherType.LIGHT_RAIN, WeatherType.THUNDERSTORM, WeatherType.FOG_MIST -> RAIN
                WeatherType.FREEZING_ICE -> SNOW
            }
        }

        fun fromId(id: String): WeatherCondition {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
                ?: CLEAR
        }
    }
}

/**
 * Visual styling and atmospheric parameters applied to the race track canvas.
 */
data class VisualTrackConditions(
    val condition: WeatherCondition = WeatherCondition.CLEAR,
    val asphaltColor: Color = AsphaltColor,
    val asphaltWetnessSheen: Float = 0.0f,
    val trackBorderColor: Color = Color(0xFFE0E0E0),
    val grassRunoffColor: Color = GrassGreenColor,
    val ambientOverlayColor: Color = Color.Transparent,
    val ambientOverlayAlpha: Float = 0.0f,
    val trackWetness: Float = 0.0f,
    val precipitationType: WeatherPrecipitationType = WeatherPrecipitationType.NONE,
    val precipitationParticleCount: Int = 0,
    val snowEdgeAccumulation: Float = 0.0f,
    val puddleReflectionAlpha: Float = 0.0f,
    val bannerMessage: String = "☀️ RACE START: Sunny & Dry! Peak tire traction available."
)

/**
 * Detailed physics and vehicle traction parameters modulated by the active weather condition.
 */
data class WeatherHandlingParameters(
    val gripMultiplier: Float = 1.0f,
    val steeringResponseMultiplier: Float = 1.0f,
    val brakingEfficiencyMultiplier: Float = 1.0f,
    val accelerationTractionMultiplier: Float = 1.0f,
    val topSpeedDragMultiplier: Float = 1.0f,
    val tireCoolingRate: Float = 1.2f,
    val lateralSlipThresholdDeg: Float = 12.0f,
    val wheelspinRiskFactor: Float = 0.05f,
    val aquaplaningRiskFactor: Float = 0.0f,
    val brakingDistanceMultiplier: Float = 1.0f
)

/**
 * Singleton manager coordinating live weather conditions, triggering randomized race weather
 * (Sunny, Rainy, Snowy) at race start, and dynamically influencing vehicle traction and visual track conditions.
 */
object WeatherManager {

    private val _currentCondition = MutableStateFlow(WeatherCondition.CLEAR)
    val currentCondition: StateFlow<WeatherCondition> = _currentCondition.asStateFlow()

    private val _visualTrackConditions = MutableStateFlow(buildVisualConditions(WeatherCondition.CLEAR))
    val visualTrackConditions: StateFlow<VisualTrackConditions> = _visualTrackConditions.asStateFlow()

    private val _handlingParameters = MutableStateFlow(buildHandlingParameters(WeatherCondition.CLEAR))
    val handlingParameters: StateFlow<WeatherHandlingParameters> = _handlingParameters.asStateFlow()

    private val _raceStartBanner = MutableStateFlow<String?>("☀️ RACE START: Sunny & Dry! Peak traction available.")
    val raceStartBanner: StateFlow<String?> = _raceStartBanner.asStateFlow()

    /**
     * Randomly triggers a weather state (Sunny, Rainy, Snowy) at the start of a race,
     * immediately updating vehicle traction parameters and visual track conditions.
     */
    fun triggerRandomRaceWeather(random: Random = Random.Default): WeatherCondition {
        val conditions = WeatherCondition.values()
        val chosen = conditions[random.nextInt(conditions.size)]
        setCondition(chosen)

        val banner = when (chosen) {
            WeatherCondition.CLEAR -> "☀️ RACE START: Sunny & Dry! Peak tire adhesion and maximum top speed."
            WeatherCondition.RAIN -> "🌧️ RACE START: Rainy & Wet Track! 30% traction loss, slick tarmac and puddles!"
            WeatherCondition.SNOW -> "❄️ RACE START: Snowy & Compacted Ice! Extreme traction loss, 2.5x braking distance!"
        }
        _raceStartBanner.value = banner
        return chosen
    }

    /**
     * Updates the active weather condition and refreshes traction and visual track conditions.
     */
    fun setCondition(condition: WeatherCondition) {
        _currentCondition.value = condition
        _visualTrackConditions.value = buildVisualConditions(condition)
        _handlingParameters.value = buildHandlingParameters(condition)
    }

    /**
     * Cycles sequentially to the next weather condition (Sunny -> Rain -> Snow -> Sunny).
     */
    fun cycleNextCondition(): WeatherCondition {
        val all = WeatherCondition.values()
        val nextIdx = (all.indexOf(_currentCondition.value) + 1) % all.size
        val nextCondition = all[nextIdx]
        setCondition(nextCondition)
        return nextCondition
    }

    /**
     * Returns dynamic handling and traction parameters applied to the vehicle physics engine.
     */
    fun getHandlingParameters(condition: WeatherCondition = _currentCondition.value): WeatherHandlingParameters {
        return buildHandlingParameters(condition)
    }

    /**
     * Overload resolving handling parameters from [WeatherType].
     */
    fun getHandlingParameters(weatherType: WeatherType): WeatherHandlingParameters {
        return getHandlingParameters(WeatherCondition.fromWeatherType(weatherType))
    }

    /**
     * Calculates the effective tire traction combining vehicle base grip with active weather impact,
     * surface wetness, and optional tire compound characteristics.
     */
    fun calculateEffectiveGrip(
        baseGrip: Float,
        condition: WeatherCondition = _currentCondition.value,
        compound: TireCompound? = null,
        trackWetness: Float = condition.targetTrackWetness
    ): Float {
        val compoundFactor = if (compound != null) {
            compound.dryGripMultiplier * (1f - trackWetness) + compound.wetGripMultiplier * trackWetness
        } else {
            1.0f
        }
        return baseGrip * compoundFactor * condition.gripMultiplier
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

    /**
     * Computes the vehicle stopping distance in meters under the active weather traction.
     * Formula: d = v^2 / (2 * a * brakingEfficiency)
     */
    fun calculateStoppingDistanceMeters(
        speedKmh: Float,
        baseBrakePower: Float = 1.0f,
        condition: WeatherCondition = _currentCondition.value
    ): Float {
        if (speedKmh <= 0.5f) return 0f
        val speedMs = speedKmh / 3.6f
        val nominalDecel = 10.0f * baseBrakePower.coerceAtLeast(0.5f)
        val effectiveDecel = nominalDecel * condition.brakingEfficiencyMultiplier.coerceIn(0.15f, 1.5f)
        return (speedMs * speedMs) / (2f * effectiveDecel)
    }

    /**
     * Returns visual track conditions associated with the given weather.
     */
    fun getVisualTrackConditions(condition: WeatherCondition = _currentCondition.value): VisualTrackConditions {
        return buildVisualConditions(condition)
    }

    private fun buildHandlingParameters(condition: WeatherCondition): WeatherHandlingParameters {
        return when (condition) {
            WeatherCondition.CLEAR -> WeatherHandlingParameters(
                gripMultiplier = 1.0f,
                steeringResponseMultiplier = 1.0f,
                brakingEfficiencyMultiplier = 1.0f,
                accelerationTractionMultiplier = 1.0f,
                topSpeedDragMultiplier = 1.0f,
                tireCoolingRate = 1.2f,
                lateralSlipThresholdDeg = 12.0f,
                wheelspinRiskFactor = 0.05f,
                aquaplaningRiskFactor = 0.0f,
                brakingDistanceMultiplier = 1.0f
            )
            WeatherCondition.RAIN -> WeatherHandlingParameters(
                gripMultiplier = 0.72f,
                steeringResponseMultiplier = 0.82f,
                brakingEfficiencyMultiplier = 0.75f,
                accelerationTractionMultiplier = 0.70f,
                topSpeedDragMultiplier = 0.96f,
                tireCoolingRate = 4.0f,
                lateralSlipThresholdDeg = 8.5f,
                wheelspinRiskFactor = 0.35f,
                aquaplaningRiskFactor = 0.45f,
                brakingDistanceMultiplier = 1.33f
            )
            WeatherCondition.SNOW -> WeatherHandlingParameters(
                gripMultiplier = 0.40f,
                steeringResponseMultiplier = 0.52f,
                brakingEfficiencyMultiplier = 0.44f,
                accelerationTractionMultiplier = 0.38f,
                topSpeedDragMultiplier = 0.90f,
                tireCoolingRate = 6.5f,
                lateralSlipThresholdDeg = 5.0f,
                wheelspinRiskFactor = 0.75f,
                aquaplaningRiskFactor = 0.85f,
                brakingDistanceMultiplier = 2.27f
            )
        }
    }

    private fun buildVisualConditions(condition: WeatherCondition): VisualTrackConditions {
        return when (condition) {
            WeatherCondition.CLEAR -> VisualTrackConditions(
                condition = WeatherCondition.CLEAR,
                asphaltColor = AsphaltColor,
                asphaltWetnessSheen = 0.0f,
                trackBorderColor = Color(0xFFE0E0E0),
                grassRunoffColor = GrassGreenColor,
                ambientOverlayColor = Color(0x0FFFEE58), // Subtle warm sun glow
                ambientOverlayAlpha = 0.06f,
                trackWetness = 0.0f,
                precipitationType = WeatherPrecipitationType.NONE,
                precipitationParticleCount = 0,
                snowEdgeAccumulation = 0.0f,
                puddleReflectionAlpha = 0.0f,
                bannerMessage = "☀️ RACE START: Sunny & Dry! Peak tire adhesion and maximum top speed."
            )
            WeatherCondition.RAIN -> VisualTrackConditions(
                condition = WeatherCondition.RAIN,
                asphaltColor = AsphaltWetColor,
                asphaltWetnessSheen = 0.85f,
                trackBorderColor = Color(0xFF9E9E9E),
                grassRunoffColor = DampGrassColor,
                ambientOverlayColor = Color(0x33102027), // Overcast dark storm tint
                ambientOverlayAlpha = 0.20f,
                trackWetness = 0.75f,
                precipitationType = WeatherPrecipitationType.RAIN_STREAKS,
                precipitationParticleCount = 55,
                snowEdgeAccumulation = 0.0f,
                puddleReflectionAlpha = 0.60f,
                bannerMessage = "🌧️ RACE START: Rainy & Wet Track! 30% traction loss, slick tarmac and puddles!"
            )
            WeatherCondition.SNOW -> VisualTrackConditions(
                condition = WeatherCondition.SNOW,
                asphaltColor = SnowTrackColor,
                asphaltWetnessSheen = 0.65f,
                trackBorderColor = Color(0xFFE0F7FA),
                grassRunoffColor = SnowGroundColor,
                ambientOverlayColor = Color(0x2E80DEEA), // Frosted icy cyan haze
                ambientOverlayAlpha = 0.18f,
                trackWetness = 0.90f,
                precipitationType = WeatherPrecipitationType.SNOW_FLURRIES,
                precipitationParticleCount = 65,
                snowEdgeAccumulation = 0.80f,
                puddleReflectionAlpha = 0.80f,
                bannerMessage = "❄️ RACE START: Snowy & Compacted Ice! Extreme traction loss, 2.5x braking distance!"
            )
        }
    }
}
