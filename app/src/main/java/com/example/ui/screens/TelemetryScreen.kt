package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryPoint
import com.example.model.Vehicle
import com.example.ui.telemetry.FourWheelThermalMap
import com.example.ui.telemetry.GForceFrictionCircle
import com.example.ui.telemetry.SpeedRpmChart
import com.example.ui.telemetry.ThrottleBrakeChart
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun TelemetryScreen(
    vehicle: Vehicle,
    telemetryHistory: List<TelemetryPoint>,
    onBack: () -> Unit
) {
    val current = telemetryHistory.lastOrNull()

    val currentSpeed = current?.speedKmh?.toInt() ?: 0
    val maxSpeed = telemetryHistory.maxOfOrNull { it.speedKmh }?.toInt() ?: 0
    val maxLatG = telemetryHistory.maxOfOrNull { it.lateralG } ?: 0f
    val currentRpm = current?.rpm?.toInt() ?: 0
    val avgTemp = current?.avgTireTemp ?: 80f
    val downforce = current?.downforceN?.toInt() ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("telemetry_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Column {
                        Text(
                            text = "VEHICLE TELEMETRY & METRICS",
                            color = TextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${vehicle.name} • ${vehicle.driveTrain.displayName}",
                            color = ApexCyan,
                            fontSize = 12.sp
                        )
                    }
                }

                // Live status indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, ApexGreen, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).background(ApexGreen, androidx.compose.foundation.shape.CircleShape))
                        Text("LIVE TELEMETRY", color = ApexGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Top Metric KPI Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricKpiCard(
                    title = "SPEED",
                    value = "$currentSpeed",
                    unit = "KM/H",
                    sub = "Max: $maxSpeed",
                    color = ApexCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricKpiCard(
                    title = "RPM",
                    value = "$currentRpm",
                    unit = "RPM",
                    sub = "Gear: ${current?.gear ?: 1}",
                    color = ApexGold,
                    modifier = Modifier.weight(1f)
                )
                MetricKpiCard(
                    title = "TIRE TEMP",
                    value = "${avgTemp.toInt()}°C",
                    unit = "AVG",
                    sub = "4 Corners",
                    color = if (avgTemp > 105f) ApexOrange else ApexGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricKpiCard(
                    title = "PEAK G",
                    value = String.format("%.2f", maxLatG),
                    unit = "G",
                    sub = "${downforce}N Downforce",
                    color = ApexOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Speed & RPM Line and Area Chart
        item {
            SpeedRpmChart(
                points = telemetryHistory,
                maxSpeed = (vehicle.effectiveTopSpeedKmh * 1.05f),
                maxRpm = vehicle.baseMaxRpm.toFloat()
            )
        }

        // Throttle & Brake Pedals Trace
        item {
            ThrottleBrakeChart(points = telemetryHistory)
        }

        // 4-Corner Tire Thermal Heatmap & Status
        item {
            FourWheelThermalMap(point = current)
        }

        // G-G Friction Circle Diagram
        item {
            GForceFrictionCircle(points = telemetryHistory)
        }
    }
}

@Composable
private fun MetricKpiCard(
    title: String,
    value: String,
    unit: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(unit, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(sub, color = TextMuted, fontSize = 9.sp)
        }
    }
}
