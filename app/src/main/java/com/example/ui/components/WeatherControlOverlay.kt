package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WeatherCondition
import com.example.model.WeatherManager
import com.example.physics.DynamicWeatherPhysicsService
import com.example.physics.RaceWeatherCondition
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
 * In-game UI overlay allowing drivers to toggle live between Clear, Rain, and Snow conditions
 * or trigger randomized weather changes during races, displaying real-time vehicle physics adjustments:
 * dynamic grip, top speed limits, and calculated braking distance.
 */
@Composable
fun WeatherControlOverlay(
    modifier: Modifier = Modifier,
    activeCondition: WeatherCondition = WeatherManager.currentCondition.collectAsState().value,
    onWeatherSelected: (WeatherCondition) -> Unit = { WeatherManager.setCondition(it) },
    onRandomizeWeather: (() -> Unit)? = null,
    showHandlingMetrics: Boolean = true,
    physicsService: DynamicWeatherPhysicsService = DynamicWeatherPhysicsService.getInstance()
) {
    var isExpanded by remember { mutableStateOf(false) }
    val dynamicPhysics by physicsService.physicsState.collectAsState()

    val activeAccentColor = when (dynamicPhysics.weather) {
        RaceWeatherCondition.SUNNY -> ApexGold
        RaceWeatherCondition.RAIN -> ApexCyan
        RaceWeatherCondition.SNOW -> Color(0xFFE0F7FA)
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, activeAccentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .testTag("weather_control_overlay"),
        colors = CardDefaults.cardColors(containerColor = CarbonBlack.copy(alpha = 0.88f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Weather Toggle Selector Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Weather Controls",
                        tint = activeAccentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "WEATHER PHYSICS",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    if (dynamicPhysics.isRandomWeatherActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ApexGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "RANDOM ON",
                                color = ApexGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Expand/Collapse Details button with live grip and braking summary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${dynamicPhysics.formattedGripPct} GRIP • STOP ${dynamicPhysics.formattedBrakingDistance100}",
                        color = activeAccentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle weather details",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Segmented 3-Way Weather Toggle Buttons (Clear, Rain, Snow) + Randomize Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeatherToggleButton(
                    condition = WeatherCondition.CLEAR,
                    isSelected = dynamicPhysics.weather == RaceWeatherCondition.SUNNY,
                    icon = Icons.Default.WbSunny,
                    accentColor = ApexGold,
                    onClick = {
                        onWeatherSelected(WeatherCondition.CLEAR)
                        physicsService.forceWeather(RaceWeatherCondition.SUNNY)
                    },
                    testTag = "weather_toggle_clear",
                    modifier = Modifier.weight(1f)
                )

                WeatherToggleButton(
                    condition = WeatherCondition.RAIN,
                    isSelected = dynamicPhysics.weather == RaceWeatherCondition.RAIN,
                    icon = Icons.Default.WaterDrop,
                    accentColor = ApexCyan,
                    onClick = {
                        onWeatherSelected(WeatherCondition.RAIN)
                        physicsService.forceWeather(RaceWeatherCondition.RAIN)
                    },
                    testTag = "weather_toggle_rain",
                    modifier = Modifier.weight(1f)
                )

                WeatherToggleButton(
                    condition = WeatherCondition.SNOW,
                    isSelected = dynamicPhysics.weather == RaceWeatherCondition.SNOW,
                    icon = Icons.Default.AcUnit,
                    accentColor = Color(0xFFE0F7FA),
                    onClick = {
                        onWeatherSelected(WeatherCondition.SNOW)
                        physicsService.forceWeather(RaceWeatherCondition.SNOW)
                    },
                    testTag = "weather_toggle_snow",
                    modifier = Modifier.weight(1f)
                )

                // Dedicated Randomize Weather Button
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            if (onRandomizeWeather != null) {
                                onRandomizeWeather()
                            } else {
                                physicsService.triggerRandomWeatherChange()
                            }
                        }
                        .padding(horizontal = 8.dp)
                        .testTag("randomize_weather_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Randomize weather",
                            tint = ApexGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "RANDOM",
                            color = ApexGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Expandable Telemetry Handling Parameter Metrics
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(150)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                        .testTag("weather_handling_status"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Metric 1: Tire Grip Adhesion
                    HandlingMetricBar(
                        label = "DYNAMIC TIRE GRIP",
                        percent = (dynamicPhysics.gripMultiplier * 100).toInt(),
                        progress = dynamicPhysics.gripMultiplier,
                        color = activeAccentColor,
                        testTag = "weather_stats_grip"
                    )

                    // Metric 2: Braking Distance at 100 km/h (Highlights extended stopping distance)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("weather_stats_braking_distance"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BRAKING DISTANCE (@100 km/h)",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val distanceDelta = when (dynamicPhysics.weather) {
                                RaceWeatherCondition.SUNNY -> "Baseline reference distance"
                                RaceWeatherCondition.RAIN -> "+47% longer stopping distance!"
                                RaceWeatherCondition.SNOW -> "+150% longer stopping distance (2.5x)!"
                            }
                            Text(
                                text = distanceDelta,
                                color = if (dynamicPhysics.weather == RaceWeatherCondition.SUNNY) ApexGreen else ApexOrange,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = dynamicPhysics.formattedBrakingDistance100,
                            color = if (dynamicPhysics.weather == RaceWeatherCondition.SUNNY) ApexGreen else ApexOrange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Metric 3: Top Speed Restriction / Drag
                    HandlingMetricBar(
                        label = "EFFECTIVE TOP SPEED (${dynamicPhysics.formattedTopSpeed})",
                        percent = (dynamicPhysics.topSpeedMultiplier * 100).toInt(),
                        progress = dynamicPhysics.topSpeedMultiplier,
                        color = if (dynamicPhysics.topSpeedMultiplier < 0.90f) ApexOrange else ApexCyan,
                        testTag = "weather_stats_top_speed"
                    )

                    // Metric 4: Throttle Traction (Wheelspin risk)
                    HandlingMetricBar(
                        label = "ACCEL TRACTION",
                        percent = (dynamicPhysics.accelerationTractionMultiplier * 100).toInt(),
                        progress = dynamicPhysics.accelerationTractionMultiplier,
                        color = if (dynamicPhysics.accelerationTractionMultiplier < 0.5f) ApexRed else ApexCyan,
                        testTag = "weather_stats_traction"
                    )

                    // Alert or Description
                    if (dynamicPhysics.alertMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ApexGold, modifier = Modifier.size(12.dp))
                            Text(
                                text = dynamicPhysics.alertMessage ?: "",
                                color = ApexGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = dynamicPhysics.weather.description,
                            color = TextMuted,
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherToggleButton(
    condition: WeatherCondition,
    isSelected: Boolean,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.20f) else DarkSurface,
        animationSpec = tween(150),
        label = "WeatherBtnBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) accentColor else SurfaceBorder,
        animationSpec = tween(150),
        label = "WeatherBtnBorder"
    )

    Box(
        modifier = modifier
            .height(48.dp) // Touch target >= 48dp
            .clip(RoundedCornerShape(8.dp))
            .background(animatedBg)
            .border(1.dp, animatedBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = condition.displayName,
                tint = if (isSelected) accentColor else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = condition.displayName.uppercase(),
                color = if (isSelected) TextWhite else TextMuted,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun HandlingMetricBar(
    label: String,
    percent: Int,
    progress: Float,
    color: Color,
    testTag: String
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$percent%",
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = color,
            trackColor = Color(0xFF1E222D),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}
