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
import java.util.Locale
import kotlin.math.abs
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
import com.example.ui.components.TelemetryDashboard
import com.example.ui.telemetry.FourWheelThermalMap
import com.example.ui.telemetry.GForceFrictionCircle
import com.example.ui.telemetry.SpeedRpmChart
import com.example.ui.telemetry.ThrottleBrakeChart
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

import com.example.physics.simulation2d.VehicleSimulation2DTelemetry
import com.example.physics.simulation2d.WheelId2D
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun TelemetryScreen(
    vehicle: Vehicle,
    telemetryHistory: List<TelemetryPoint>,
    simulation2DTelemetry: VehicleSimulation2DTelemetry? = null,
    onBack: () -> Unit,
    onOpenPerformanceAnalytics: () -> Unit = {}
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

        // Long-term Performance Trends Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPerformanceAnalytics() }
                    .testTag("open_performance_analytics_button"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexCyan)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(ApexCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Car Performance Evolution Over Time", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextWhite)
                            Text("D3/Recharts curves • Speed & lap trends from GameStatistics", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                    Text("TRENDS >", fontWeight = FontWeight.Black, fontSize = 11.sp, color = ApexCyan)
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

        // Recharts & Custom Canvas Interactive Telemetry Dashboard Component
        item {
            TelemetryDashboard(
                vehicle = vehicle,
                telemetryHistory = telemetryHistory,
                simulation2DTelemetry = simulation2DTelemetry,
                isOverlay = false
            )
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

        // Real-Time Vehicle Physics Integration Module Metrics
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("physics_integration_telemetry_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PHYSICS INTEGRATION & FORCES",
                            color = ApexCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, ApexCyan, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SEMI-IMPLICIT EULER", color = ApexCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 1. Velocity Decomposition
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("REAL-TIME VELOCITY DECOMPOSITION", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhysicsMiniStat(
                                label = "LONGITUDINAL",
                                value = String.format("%.1f m/s", current?.vLongMs ?: 0f),
                                sub = "Along Heading",
                                color = TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "LATERAL",
                                value = String.format("%.1f m/s", current?.vLatMs ?: 0f),
                                sub = "Cornering Drift",
                                color = if (kotlin.math.abs(current?.vLatMs ?: 0f) > 2f) ApexOrange else TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "SLIP ANGLE",
                                value = String.format("%.1f°", current?.slipAngleDeg ?: 0f),
                                sub = "Tire Deflection",
                                color = if (kotlin.math.abs(current?.slipAngleDeg ?: 0f) > 8f) ApexRed else ApexGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 2. Opposing Friction Forces
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("OPPOSING FRICTION BREAKDOWN", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhysicsMiniStat(
                                label = "AERO DRAG",
                                value = "${current?.frictionDragN?.toInt() ?: 0} N",
                                sub = "0.5*ρ*Cd*A*v²",
                                color = ApexGold,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "ROLLING RES.",
                                value = "${current?.frictionRollingN?.toInt() ?: 0} N",
                                sub = "Crr * N",
                                color = ApexCyan,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "TOTAL FRICTION",
                                value = "${current?.totalFrictionN?.toInt() ?: 0} N",
                                sub = "Drag + Rolling",
                                color = ApexOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 3. Dynamic Tire Grip & Adhesion
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DYNAMIC TIRE GRIP & CONTACT PATCH", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhysicsMiniStat(
                                label = "EFFECTIVE GRIP",
                                value = "${((current?.effectiveGripPct ?: 1f) * 100).toInt()}%",
                                sub = "Compound x Surface",
                                color = if ((current?.effectiveGripPct ?: 1f) < 0.6f) ApexRed else ApexGreen,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "SLIP RATIO",
                                value = String.format("%+.1f%%", (current?.slipRatio ?: 0f) * 100f),
                                sub = "Wheel Spin/Lock",
                                color = if (kotlin.math.abs(current?.slipRatio ?: 0f) > 0.15f) ApexOrange else TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "FRICTION CIRCLE",
                                value = "${(current?.frictionCirclePct ?: 0f).toInt()}%",
                                sub = "Combined Adhesion",
                                color = if ((current?.frictionCirclePct ?: 0f) > 90f) ApexRed else ApexCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2D Physics Simulation Structure Card (JBox2D Rigid-Body, Aero Drag, and Pacejka Tire Friction)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulation_2d_telemetry_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2D PHYSICS SIMULATION (JBox2D)",
                            color = ApexCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, ApexGreen, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE 60Hz SOLVER", color = ApexGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    val simAero = simulation2DTelemetry?.aeroTelemetry
                    val simTires = simulation2DTelemetry?.tireTelemetry
                    val simBody = simulation2DTelemetry?.bodyState

                    // Aerodynamic Drag & Downforce Breakdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AERODYNAMIC DRAG & DOWNFORCE (0.5 * ρ * Cd * A * v²)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhysicsMiniStat(
                                label = "AERO DRAG",
                                value = simAero?.formattedDragForce ?: "${current?.frictionDragN?.toInt() ?: 0} N",
                                sub = simAero?.formattedDragPower ?: "Opposing Force",
                                color = ApexGold,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "DOWNFORCE",
                                value = simAero?.formattedDownforce ?: "${downforce} N",
                                sub = "Normal Grip Aid",
                                color = ApexCyan,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "AIR DENSITY",
                                value = String.format(Locale.US, "%.3f", simAero?.airDensityKgM3 ?: 1.225f),
                                sub = "kg/m³ (Weather)",
                                color = TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 4-Wheel Contact Patch Tire Friction & Normal Load
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("4-WHEEL TIRE FRICTION & DYNAMIC LOAD TRANSFER", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhysicsMiniStat(
                                label = "FRONT LOAD",
                                value = String.format(Locale.US, "%.0f N", ((simTires?.wheels?.get(WheelId2D.FRONT_LEFT)?.normalLoadN ?: 3500f) + (simTires?.wheels?.get(WheelId2D.FRONT_RIGHT)?.normalLoadN ?: 3500f))),
                                sub = "Dynamic Axle Load",
                                color = ApexGreen,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "REAR LOAD",
                                value = String.format(Locale.US, "%.0f N", ((simTires?.wheels?.get(WheelId2D.REAR_LEFT)?.normalLoadN ?: 3300f) + (simTires?.wheels?.get(WheelId2D.REAR_RIGHT)?.normalLoadN ?: 3300f))),
                                sub = "Weight Transfer",
                                color = ApexOrange,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "CORNERING",
                                value = simTires?.formattedCorneringForce ?: "0 N",
                                sub = "Pacejka Magic F.",
                                color = ApexCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // JBox2D Rigid-Body Dynamic Kinematics
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("JBox2D CHASSIS DYNAMICS & YAW KINEMATICS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhysicsMiniStat(
                                label = "WORLD VELOCITY",
                                value = String.format(Locale.US, "(%.1f, %.1f)", simBody?.vxMps ?: (current?.speedKmh ?: 0f) / 3.6f, simBody?.vyMps ?: 0f),
                                sub = "m/s (Global)",
                                color = TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "YAW RATE",
                                value = String.format(Locale.US, "%+.1f°/s", simBody?.angularVelocityDegPerSec ?: 0f),
                                sub = "Chassis Rotation",
                                color = if (abs(simBody?.angularVelocityDegPerSec ?: 0f) > 25f) ApexOrange else ApexCyan,
                                modifier = Modifier.weight(1f)
                            )
                            PhysicsMiniStat(
                                label = "DRIFT STATE",
                                value = if (simTires?.isDrifting == true) "DRIFTING" else "TRACTION",
                                sub = if (simTires?.isDrifting == true) "Slip > Peak" else "Adhesion OK",
                                color = if (simTires?.isDrifting == true) ApexRed else ApexGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhysicsMiniStat(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text(sub, color = TextMuted, fontSize = 8.sp)
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
