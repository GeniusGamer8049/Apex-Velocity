package com.example.model

enum class WeatherType(
    val displayName: String,
    val baseGrip: Float,
    val visibility: Float,
    val description: String,
    val iconEmoji: String
) {
    SUNNY_CLEAR("Sunny & Dry", 1.0f, 1.0f, "Optimal dry tarmac, peak tire adhesion", "☀️"),
    OVERCAST("Overcast", 0.95f, 0.95f, "Cool asphalt, moderate grip", "☁️"),
    LIGHT_RAIN("Light Rain", 0.78f, 0.85f, "Damp track, reduced braking efficiency", "🌦️"),
    THUNDERSTORM("Heavy Storm", 0.60f, 0.65f, "Standing water, aquaplaning hazard, low grip", "⛈️"),
    FOG_MIST("Mountain Mist", 0.86f, 0.55f, "Dense mist, low forward visibility", "🌫️"),
    FREEZING_ICE("Freezing Touge", 0.45f, 0.80f, "Treacherous ice patches, extreme oversteer", "❄️")
}

enum class TireCompound(
    val displayName: String,
    val dryGripMultiplier: Float,
    val wetGripMultiplier: Float,
    val wearRate: Float,
    val description: String
) {
    SPORT_RADIAL("Sport Radial", 1.0f, 0.78f, 1.0f, "All-round street & track tire, decent in light rain"),
    SOFT_SLICK("Soft Racing Slick", 1.18f, 0.40f, 1.4f, "Maximum grip in dry conditions, dangerous on wet roads"),
    SEMI_SLICK("Semi-Slick R", 1.10f, 0.58f, 1.2f, "High-performance dry track compound with minimal wet grooves"),
    WET_INTERMEDIATE("Intermediate Grooved", 0.94f, 0.92f, 1.1f, "Specialized channels clear water and prevent aquaplaning"),
    FULL_WET("Full Wet Racing", 0.86f, 1.08f, 1.3f, "Deep directional treads for extreme rain and standing water")
}

data class DynamicWeatherState(
    val currentWeather: WeatherType = WeatherType.SUNNY_CLEAR,
    val trackWetness: Float = 0.0f, // 0.0 = bone dry, 1.0 = fully soaked
    val forecastWeather: WeatherType = WeatherType.SUNNY_CLEAR,
    val secondsToTransition: Int = 0,
    val lightningFlashAlpha: Float = 0.0f,
    val alertMessage: String? = null
)
