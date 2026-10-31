package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DynamicWeatherState
import com.example.model.SurfaceHazardType
import com.example.model.TireCompound
import com.example.model.WeatherType
import com.example.physics.CarPhysicsState
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.ApexRed
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

/**
 * Dynamic Weather Status Overlay
 *
 * Displays real-time atmospheric conditions, surface wetness, grip adhesion levels,
 * surface hazards, tire compatibility, and incoming weather transition radar alerts.
 */
@Composable
fun DynamicWeatherOverlay(
    weatherState: DynamicWeatherState,
    playerState: CarPhysicsState,
    tireCompound: TireCompound? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val currentWeather = weatherState.currentWeather
    val accentColor = getWeatherAccentColor(currentWeather)
    val weatherIcon = getWeatherIcon(currentWeather)

    // Dynamic animation based on active weather type
    val infiniteTransition = rememberInfiniteTransition(label = "weather_anim")
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_spin"
    )
    val rainBounce by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rain_pulse"
    )
    val stormFlash by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "storm_flash"
    )
    val fogPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fog_pulse"
    )

    val iconModifier = when (currentWeather) {
        WeatherType.SUNNY_CLEAR -> Modifier.rotate(sunRotation)
        WeatherType.LIGHT_RAIN -> Modifier.scale(rainBounce)
        WeatherType.THUNDERSTORM -> Modifier.scale(stormFlash)
        WeatherType.FOG_MIST -> Modifier.scale(fogPulse)
        WeatherType.FREEZING_ICE -> Modifier.scale(rainBounce)
        WeatherType.OVERCAST -> Modifier
    }

    val (trackTemp, airTemp) = getEstimatedTemperatures(currentWeather, weatherState.trackWetness)
    val (gripLabel, gripColor, gripDesc) = getGripRating(playerState.effectiveTireGrip)
    val (tireStatus, tireStatusColor) = getTireSuitability(tireCompound, currentWeather, weatherState.trackWetness)

    val wetnessPercent = (weatherState.trackWetness * 100f).toInt().coerceIn(0, 100)
    val wetnessBarColor = when {
        wetnessPercent < 15 -> ApexGreen
        wetnessPercent < 45 -> ApexGold
        wetnessPercent < 75 -> Color(0xFF29B6F6)
        else -> Color(0xFF7C4DFF)
    }

    Column(
        modifier = modifier
            .testTag("weather_status_overlay")
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonBlack.copy(alpha = 0.88f))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Main Weather Pill Header (Clickable to toggle detailed metrics)
        Row(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Animated Weather Condition Icon Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .border(1.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = currentWeather.displayName,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp).then(iconModifier)
                )
            }

            // Weather Name & Track Temperature
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = currentWeather.displayName.uppercase(),
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${currentWeather.iconEmoji}",
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Temperature indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = "Track Temperature",
                            tint = ApexOrange,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${trackTemp}°C",
                            color = ApexOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Surface Grip Badge
                    Text(
                        text = "GRIP ${(playerState.effectiveTireGrip * 100f).toInt()}%",
                        color = gripColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Real-Time Track Wetness Mini-Gauge
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Wetness",
                        tint = wetnessBarColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "$wetnessPercent%",
                        color = wetnessBarColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                LinearProgressIndicator(
                    progress = { weatherState.trackWetness },
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = wetnessBarColor,
                    trackColor = DarkSurfaceElevated
                )
            }

            // Expand/Collapse Chevron Indicator
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse Weather Details" else "Expand Weather Details",
                tint = TextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .testTag("weather_overlay_expand_button")
            )
        }

        // Active Surface Hazard Alert (Puddle, Ice Patch, Oil, Kerb, etc.)
        if (playerState.currentSurfaceHazard != null) {
            val hazard = playerState.currentSurfaceHazard!!
            val hazardColor = when (hazard) {
                SurfaceHazardType.PUDDLE -> Color(0xFF00E5FF)
                SurfaceHazardType.ICE_PATCH -> Color(0xFF81D4FA)
                SurfaceHazardType.OIL_SLICK -> ApexOrange
                SurfaceHazardType.KERB_RUMBLE -> ApexRed
                SurfaceHazardType.OFFROAD_GRASS -> Color(0xFF7CB342)
                SurfaceHazardType.GRAVEL_TRAP -> ApexGold
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(hazardColor.copy(alpha = 0.2f))
                    .border(1.dp, hazardColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Hazard Alert",
                        tint = hazardColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "SURFACE HAZARD: ${hazard.displayName.uppercase()}",
                        color = hazardColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Aquaplaning Critical Alert Banner
        if (playerState.isAquaplaning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(ApexRed.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Aquaplaning",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "HYDROPLANING! LIFT THROTTLE TO REGAIN STEERING",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Incoming Weather Transition Alert Banner
        if (weatherState.secondsToTransition in 1..45 && weatherState.forecastWeather != currentWeather) {
            val forecastWeather = weatherState.forecastWeather
            val forecastIcon = getWeatherIcon(forecastWeather)
            val forecastColor = getWeatherAccentColor(forecastWeather)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(ApexOrange.copy(alpha = 0.22f))
                    .border(1.dp, ApexOrange.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = forecastIcon,
                        contentDescription = "Incoming Weather",
                        tint = forecastColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "RADAR: ${forecastWeather.displayName.uppercase()} in ${weatherState.secondsToTransition}s",
                        color = ApexOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Expandable Detailed Weather & Tire Telemetry Card
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated.copy(alpha = 0.85f))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "METEOROLOGICAL & TRACK TELEMETRY",
                    color = ApexCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Atmospheric stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeatherStatItem("Air Temp", "${airTemp}°C", Icons.Default.Thermostat, TextWhite)
                    WeatherStatItem("Visibility", "${(currentWeather.visibility * 100f).toInt()}%", Icons.Default.Air, if (currentWeather.visibility < 0.7f) ApexOrange else TextWhite)
                    WeatherStatItem("Traction Level", gripLabel, Icons.Default.Speed, gripColor)
                }

                // Tire strategy advice
                if (tireCompound != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Tire Strategy:",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${tireCompound.displayName} — $tireStatus",
                            color = tireStatusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Description text
                Text(
                    text = "${currentWeather.description}. $gripDesc.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun WeatherStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = TextMuted, modifier = Modifier.size(11.dp))
        Column {
            Text(text = label, color = TextMuted, fontSize = 9.sp)
            Text(text = value, color = valueColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

private fun getWeatherIcon(type: WeatherType): ImageVector = when (type) {
    WeatherType.SUNNY_CLEAR -> Icons.Default.WbSunny
    WeatherType.OVERCAST -> Icons.Default.Cloud
    WeatherType.LIGHT_RAIN -> Icons.Default.WaterDrop
    WeatherType.THUNDERSTORM -> Icons.Default.Bolt
    WeatherType.FOG_MIST -> Icons.Default.CloudQueue
    WeatherType.FREEZING_ICE -> Icons.Default.AcUnit
}

private fun getWeatherAccentColor(type: WeatherType): Color = when (type) {
    WeatherType.SUNNY_CLEAR -> ApexGold
    WeatherType.OVERCAST -> Color(0xFFB0BEC5)
    WeatherType.LIGHT_RAIN -> ApexCyan
    WeatherType.THUNDERSTORM -> Color(0xFFB388FF)
    WeatherType.FOG_MIST -> Color(0xFF90A4AE)
    WeatherType.FREEZING_ICE -> Color(0xFF80D8FF)
}

private fun getEstimatedTemperatures(type: WeatherType, wetness: Float): Pair<Int, Int> = when (type) {
    WeatherType.SUNNY_CLEAR -> Pair((34 - wetness * 6).toInt(), 25)
    WeatherType.OVERCAST -> Pair((22 - wetness * 4).toInt(), 18)
    WeatherType.LIGHT_RAIN -> Pair((16 - wetness * 3).toInt(), 14)
    WeatherType.THUNDERSTORM -> Pair((13 - wetness * 2).toInt(), 11)
    WeatherType.FOG_MIST -> Pair((12 - wetness * 2).toInt(), 9)
    WeatherType.FREEZING_ICE -> Pair((-4 + wetness * 2).toInt(), -2)
}

private fun getGripRating(effectiveGrip: Float): Triple<String, Color, String> = when {
    effectiveGrip >= 0.90f -> Triple("OPTIMAL", ApexGreen, "High tire adhesion")
    effectiveGrip >= 0.72f -> Triple("MODERATE", ApexCyan, "Slight braking elongation")
    effectiveGrip >= 0.52f -> Triple("SLIPPERY", ApexOrange, "Significant traction loss")
    else -> Triple("CRITICAL", ApexRed, "Extreme drift hazard on slick surfaces")
}

private fun getTireSuitability(compound: TireCompound?, weather: WeatherType, wetness: Float): Pair<String, Color> {
    if (compound == null) return Pair("STANDARD", ApexCyan)
    return when {
        compound == TireCompound.SOFT_SLICK && wetness > 0.35f ->
            Pair("DANGEROUS (Slicks on Wet)", ApexRed)
        compound == TireCompound.SOFT_SLICK && wetness <= 0.15f ->
            Pair("OPTIMAL (Max Dry Grip)", ApexGreen)
        compound == TireCompound.FULL_WET && (weather == WeatherType.THUNDERSTORM || wetness > 0.60f) ->
            Pair("OPTIMAL (Wet Adhesion)", ApexGreen)
        compound == TireCompound.FULL_WET && wetness < 0.20f ->
            Pair("OVERHEATING (Dry Surface)", ApexOrange)
        compound == TireCompound.WET_INTERMEDIATE && wetness in 0.20f..0.75f ->
            Pair("EXCELLENT (Grooved Traction)", ApexGreen)
        compound == TireCompound.SEMI_SLICK && wetness > 0.40f ->
            Pair("CAUTION (Reduced Channels)", ApexOrange)
        else ->
            Pair("BALANCED TREAD", ApexCyan)
    }
}
