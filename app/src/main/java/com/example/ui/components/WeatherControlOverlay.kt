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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WeatherCondition
import com.example.model.WeatherManager
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
 * In-game UI overlay allowing drivers to toggle live between Clear, Rain, and Snow conditions,
 * showing immediate telemetry impacts on vehicle handling parameters (grip, braking, traction).
 */
@Composable
fun WeatherControlOverlay(
    modifier: Modifier = Modifier,
    activeCondition: WeatherCondition = WeatherManager.currentCondition.collectAsState().value,
    onWeatherSelected: (WeatherCondition) -> Unit = { WeatherManager.setCondition(it) },
    showHandlingMetrics: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(false) }
    val handling = WeatherManager.getHandlingParameters(activeCondition)

    val activeAccentColor = when (activeCondition) {
        WeatherCondition.CLEAR -> ApexGold
        WeatherCondition.RAIN -> ApexCyan
        WeatherCondition.SNOW -> Color(0xFFE0F7FA)
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
                        text = "WEATHER SIM",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // Expand/Collapse Details button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${(handling.gripMultiplier * 100).toInt()}% GRIP",
                        color = activeAccentColor,
                        fontSize = 10.sp,
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

            // Segmented 3-Way Weather Toggle Buttons (Clear, Rain, Snow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WeatherToggleButton(
                    condition = WeatherCondition.CLEAR,
                    isSelected = activeCondition == WeatherCondition.CLEAR,
                    icon = Icons.Default.WbSunny,
                    accentColor = ApexGold,
                    onClick = { onWeatherSelected(WeatherCondition.CLEAR) },
                    testTag = "weather_toggle_clear",
                    modifier = Modifier.weight(1f)
                )

                WeatherToggleButton(
                    condition = WeatherCondition.RAIN,
                    isSelected = activeCondition == WeatherCondition.RAIN,
                    icon = Icons.Default.WaterDrop,
                    accentColor = ApexCyan,
                    onClick = { onWeatherSelected(WeatherCondition.RAIN) },
                    testTag = "weather_toggle_rain",
                    modifier = Modifier.weight(1f)
                )

                WeatherToggleButton(
                    condition = WeatherCondition.SNOW,
                    isSelected = activeCondition == WeatherCondition.SNOW,
                    icon = Icons.Default.AcUnit,
                    accentColor = Color(0xFFE0F7FA),
                    onClick = { onWeatherSelected(WeatherCondition.SNOW) },
                    testTag = "weather_toggle_snow",
                    modifier = Modifier.weight(1f)
                )
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
                        label = "TIRE ADHESION",
                        percent = (handling.gripMultiplier * 100).toInt(),
                        progress = handling.gripMultiplier,
                        color = activeAccentColor,
                        testTag = "weather_stats_grip"
                    )

                    // Metric 2: Braking Efficiency
                    HandlingMetricBar(
                        label = "BRAKE FORCE",
                        percent = (handling.brakingEfficiencyMultiplier * 100).toInt(),
                        progress = handling.brakingEfficiencyMultiplier,
                        color = if (handling.brakingEfficiencyMultiplier < 0.6f) ApexOrange else ApexGreen,
                        testTag = "weather_stats_braking"
                    )

                    // Metric 3: Throttle Traction (Wheelspin risk)
                    HandlingMetricBar(
                        label = "ACCEL TRACTION",
                        percent = (handling.accelerationTractionMultiplier * 100).toInt(),
                        progress = handling.accelerationTractionMultiplier,
                        color = if (handling.accelerationTractionMultiplier < 0.5f) ApexRed else ApexCyan,
                        testTag = "weather_stats_traction"
                    )

                    // Brief description
                    Text(
                        text = activeCondition.description,
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
