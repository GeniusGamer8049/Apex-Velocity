package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryPoint
import com.example.model.Vehicle
import com.example.physics.CarPhysicsState
import com.example.physics.simulation2d.VehicleSimulation2DTelemetry
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.ApexPurple
import com.example.ui.theme.ApexRed
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Metric categories available in the TelemetryDashboard.
 */
enum class TelemetryMetricCategory(val title: String, val shortLabel: String) {
    JBOX2D_CANVAS("JBox2D Physics & Canvas", "JBox2D Gauges"),
    OVERVIEW("Overview & Synchronized", "Overview"),
    VELOCITY("Velocity & Acceleration", "Velocity"),
    ENGINE_TEMP("Engine Thermal", "Engine Temp"),
    TIRE_WEAR("Tire Wear & Degradation", "Tire Wear")
}

/**
 * TelemetryDashboard: A high-performance real-time telemetry visualizer component
 * inspired by Recharts/D3 charting paradigms and custom Canvas instruments.
 *
 * Visualizes live racing telemetry during active gameplay, including:
 * - JBox2D physics engine state: speedometer, tachometer, thermal gauge, and multi-trace waveform.
 * - Real-time vehicle velocity, acceleration, and lateral/longitudinal drift decomposition.
 * - Engine coolant and oil temperature dynamics with thermal load and overheating alerts.
 * - 4-corner tire wear and tread degradation curves with remaining life percentages.
 */
@Composable
fun TelemetryDashboard(
    vehicle: Vehicle,
    telemetryHistory: List<TelemetryPoint>,
    playerState: CarPhysicsState? = null,
    simulation2DTelemetry: VehicleSimulation2DTelemetry? = null,
    isOverlay: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    onToggleFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(TelemetryMetricCategory.JBOX2D_CANVAS) }
    var isExpanded by remember { mutableStateOf(true) }
    var scrubRatio by remember { mutableFloatStateOf(-1f) } // -1f means live current stream

    val latestPoint = telemetryHistory.lastOrNull()
    val activePoint = if (scrubRatio in 0f..1f && telemetryHistory.isNotEmpty()) {
        val targetIdx = ((telemetryHistory.size - 1) * scrubRatio).toInt().coerceIn(0, telemetryHistory.size - 1)
        telemetryHistory[targetIdx]
    } else {
        latestPoint
    }

    val currentSpeedKmh = playerState?.speedKmh ?: activePoint?.speedKmh ?: 0f
    val currentEngineTempC = playerState?.engineTempC ?: activePoint?.engineTempC ?: 88f
    val currentEngineOilTempC = playerState?.engineOilTempC ?: activePoint?.engineOilTempC ?: 92f

    val flWear = (playerState?.tireWearFL ?: activePoint?.tireWearFL ?: 0.04f).coerceIn(0f, 1f)
    val frWear = (playerState?.tireWearFR ?: activePoint?.tireWearFR ?: 0.04f).coerceIn(0f, 1f)
    val rlWear = (playerState?.tireWearRL ?: activePoint?.tireWearRL ?: 0.04f).coerceIn(0f, 1f)
    val rrWear = (playerState?.tireWearRR ?: activePoint?.tireWearRR ?: 0.04f).coerceIn(0f, 1f)
    val avgWearPct = ((flWear + frWear + rlWear + rrWear) / 4f) * 100f
    val minRemainingLifePct = (1f - maxOf(flWear, frWear, rlWear, rrWear)) * 100f

    val containerModifier = if (isOverlay) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface.copy(alpha = 0.94f))
            .border(1.5.dp, ApexCyan.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("telemetry_dashboard")
    } else {
        modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(14.dp)
            .testTag("telemetry_dashboard")
    }

    Column(
        modifier = containerModifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Dashboard Header & Live Status Bar
        DashboardHeader(
            vehicle = vehicle,
            isOverlay = isOverlay,
            isExpanded = isExpanded,
            isScrubbing = scrubRatio >= 0f,
            onToggleExpand = { isExpanded = !isExpanded },
            onToggleFullscreen = onToggleFullscreen,
            onDismiss = onDismiss,
            onResetScrub = { scrubRatio = -1f }
        )

        // 2. Real-Time KPI Cards Ribbon
        TelemetryKpiRibbon(
            speedKmh = currentSpeedKmh,
            engineTempC = currentEngineTempC,
            avgWearPct = avgWearPct,
            minRemainingLifePct = minRemainingLifePct,
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it }
        )

        if (isExpanded) {
            // 3. Category Selector Tabs
            TelemetryCategoryTabs(
                selectedCategory = selectedCategory,
                onSelectCategory = {
                    selectedCategory = it
                    scrubRatio = -1f
                }
            )

            // 4. Main Recharts Data Visualizer Content
            when (selectedCategory) {
                TelemetryMetricCategory.JBOX2D_CANVAS -> {
                    JBox2DPhysicsDashboardView(
                        vehicle = vehicle,
                        telemetryHistory = telemetryHistory,
                        playerState = playerState,
                        simulation2DTelemetry = simulation2DTelemetry
                    )
                }
                TelemetryMetricCategory.OVERVIEW -> {
                    OverviewRechartsView(
                        telemetryHistory = telemetryHistory,
                        activePoint = activePoint,
                        scrubRatio = scrubRatio,
                        onScrubChange = { scrubRatio = it },
                        vehicle = vehicle
                    )
                }
                TelemetryMetricCategory.VELOCITY -> {
                    VelocityRechartsView(
                        telemetryHistory = telemetryHistory,
                        activePoint = activePoint,
                        scrubRatio = scrubRatio,
                        onScrubChange = { scrubRatio = it },
                        vehicle = vehicle
                    )
                }
                TelemetryMetricCategory.ENGINE_TEMP -> {
                    EngineThermalRechartsView(
                        telemetryHistory = telemetryHistory,
                        activePoint = activePoint,
                        scrubRatio = scrubRatio,
                        onScrubChange = { scrubRatio = it }
                    )
                }
                TelemetryMetricCategory.TIRE_WEAR -> {
                    TireWearRechartsView(
                        telemetryHistory = telemetryHistory,
                        activePoint = activePoint,
                        scrubRatio = scrubRatio,
                        onScrubChange = { scrubRatio = it },
                        flWear = flWear,
                        frWear = frWear,
                        rlWear = rlWear,
                        rrWear = rrWear
                    )
                }
            }
        }
    }
}

// =========================================================================
// 1. DASHBOARD HEADER & KPI RIBBON
// =========================================================================

@Composable
private fun DashboardHeader(
    vehicle: Vehicle,
    isOverlay: Boolean,
    isExpanded: Boolean,
    isScrubbing: Boolean,
    onToggleExpand: () -> Unit,
    onToggleFullscreen: (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    onResetScrub: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isScrubbing) ApexGold else ApexGreen)
            )
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "TELEMETRY DASHBOARD",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ApexCyan.copy(alpha = 0.15f))
                            .border(0.8.dp, ApexCyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "RECHARTS ENGINE",
                            color = ApexCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    text = if (isScrubbing) "INTERACTIVE SCRUBBING • TAP TO RESUME LIVE" else "${vehicle.name} • ACTIVE GAMEPLAY STREAM",
                    color = if (isScrubbing) ApexGold else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isScrubbing) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, ApexGold, RoundedCornerShape(6.dp))
                        .clickable { onResetScrub() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("LIVE STREAM", color = ApexGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }

            if (isOverlay) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp).testTag("telemetry_dashboard_toggle_overlay_button")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Compress else Icons.Default.Expand,
                        contentDescription = "Toggle Expand",
                        tint = ApexCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onToggleFullscreen != null) {
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Fullscreen",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("telemetry_dashboard_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Dashboard",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryKpiRibbon(
    speedKmh: Float,
    engineTempC: Float,
    avgWearPct: Float,
    minRemainingLifePct: Float,
    selectedCategory: TelemetryMetricCategory,
    onSelectCategory: (TelemetryMetricCategory) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // KPI 1: Velocity
        RechartsKpiPill(
            title = "VELOCITY",
            value = "${speedKmh.toInt()}",
            unit = "KM/H",
            sub = String.format("%.1f m/s", speedKmh / 3.6f),
            accentColor = ApexCyan,
            isSelected = selectedCategory == TelemetryMetricCategory.VELOCITY,
            onClick = { onSelectCategory(TelemetryMetricCategory.VELOCITY) },
            modifier = Modifier.weight(1f)
        )

        // KPI 2: Engine Temp
        val tempColor = when {
            engineTempC < 75f -> Color(0xFF42A5F5)
            engineTempC <= 105f -> ApexGreen
            engineTempC <= 118f -> ApexOrange
            else -> ApexRed
        }
        val tempStatus = when {
            engineTempC < 75f -> "Cold"
            engineTempC <= 105f -> "Optimal"
            engineTempC <= 118f -> "Thermal Load"
            else -> "OVERHEAT"
        }
        RechartsKpiPill(
            title = "ENGINE TEMP",
            value = "${engineTempC.toInt()}°C",
            unit = "",
            sub = tempStatus,
            accentColor = tempColor,
            isSelected = selectedCategory == TelemetryMetricCategory.ENGINE_TEMP,
            onClick = { onSelectCategory(TelemetryMetricCategory.ENGINE_TEMP) },
            modifier = Modifier.weight(1f)
        )

        // KPI 3: Tire Wear
        val wearColor = when {
            avgWearPct < 25f -> ApexGreen
            avgWearPct < 55f -> ApexCyan
            avgWearPct < 75f -> ApexOrange
            else -> ApexRed
        }
        RechartsKpiPill(
            title = "TIRE WEAR",
            value = "${avgWearPct.toInt()}%",
            unit = "USED",
            sub = "${minRemainingLifePct.toInt()}% Life",
            accentColor = wearColor,
            isSelected = selectedCategory == TelemetryMetricCategory.TIRE_WEAR,
            onClick = { onSelectCategory(TelemetryMetricCategory.TIRE_WEAR) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RechartsKpiPill(
    title: String,
    value: String,
    unit: String,
    sub: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) DarkSurfaceElevated else DarkSurface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else SurfaceBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(text = title, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    color = accentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                if (unit.isNotEmpty()) {
                    Text(text = unit, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(text = sub, color = if (isSelected) accentColor else TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TelemetryCategoryTabs(
    selectedCategory: TelemetryMetricCategory,
    onSelectCategory: (TelemetryMetricCategory) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedCategory.ordinal,
        containerColor = Color.Transparent,
        contentColor = ApexCyan,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategory.ordinal]),
                color = ApexCyan,
                height = 2.dp
            )
        },
        divider = {}
    ) {
        TelemetryMetricCategory.values().forEach { category ->
            val isSelected = selectedCategory == category
            val testTagStr = when (category) {
                TelemetryMetricCategory.JBOX2D_CANVAS -> "telemetry_jbox2d_tab"
                TelemetryMetricCategory.OVERVIEW -> "telemetry_overview_tab"
                TelemetryMetricCategory.VELOCITY -> "telemetry_velocity_tab"
                TelemetryMetricCategory.ENGINE_TEMP -> "telemetry_engine_temp_tab"
                TelemetryMetricCategory.TIRE_WEAR -> "telemetry_tire_wear_tab"
            }
            Tab(
                selected = isSelected,
                onClick = { onSelectCategory(category) },
                text = {
                    Text(
                        text = category.shortLabel.uppercase(),
                        color = if (isSelected) ApexCyan else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    )
                },
                modifier = Modifier.testTag(testTagStr)
            )
        }
    }
}

// =========================================================================
// 2. OVERVIEW RECHARTS VIEW (Synchronized Multi-Chart)
// =========================================================================

@Composable
private fun OverviewRechartsView(
    telemetryHistory: List<TelemetryPoint>,
    activePoint: TelemetryPoint?,
    scrubRatio: Float,
    onScrubChange: (Float) -> Unit,
    vehicle: Vehicle
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Synchronized Velocity vs Engine Temp vs Tire Wear Chart
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SYNCHRONIZED ACTIVE TELEMETRY STREAM", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendDot(label = "Velocity", color = ApexCyan)
                            LegendDot(label = "Engine °C", color = ApexGold)
                            LegendDot(label = "Tire Wear", color = ApexOrange)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RechartsCanvasMultiStream(
                        points = telemetryHistory,
                        activeRatio = scrubRatio,
                        onScrub = onScrubChange,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    ActiveScrubTooltipReadout(activePoint = activePoint)
                }
            }
        }
    }
}

// =========================================================================
// 3. VELOCITY & ACCELERATION RECHARTS VIEW
// =========================================================================

@Composable
private fun VelocityRechartsView(
    telemetryHistory: List<TelemetryPoint>,
    activePoint: TelemetryPoint?,
    scrubRatio: Float,
    onScrubChange: (Float) -> Unit,
    vehicle: Vehicle
) {
    val speedKmh = activePoint?.speedKmh ?: 0f
    val vLong = activePoint?.vLongMs ?: (speedKmh / 3.6f)
    val vLat = activePoint?.vLatMs ?: 0f
    val latG = activePoint?.lateralG ?: 0f
    val throttle = ((activePoint?.throttle ?: 0f) * 100).toInt()
    val brake = ((activePoint?.brake ?: 0f) * 100).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(290.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("VELOCITY PROFILE & MONOTONE SPLINE", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Speed (km/h) with reference thresholds", color = TextMuted, fontSize = 9.sp)
                        }
                        Text(
                            text = "${speedKmh.toInt()} KM/H",
                            color = ApexCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Recharts Area Chart for Velocity
                    RechartsAreaCurveCanvas(
                        points = telemetryHistory.map { it.speedKmh },
                        maxScale = max(260f, vehicle.effectiveTopSpeedKmh.toFloat()),
                        primaryColor = ApexCyan,
                        referenceLineVal = 200f,
                        referenceLineLabel = "200 km/h Ref",
                        activeRatio = scrubRatio,
                        onScrub = onScrubChange,
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Velocity Details 4-Card Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DetailMiniCard(title = "LONGITUDINAL", value = String.format("%.1f m/s", vLong), color = TextWhite, modifier = Modifier.weight(1f))
                        DetailMiniCard(title = "LATERAL DRIFT", value = String.format("%.1f m/s", vLat), color = if (abs(vLat) > 2f) ApexOrange else TextWhite, modifier = Modifier.weight(1f))
                        DetailMiniCard(title = "LATERAL G", value = String.format("%.2f G", latG), color = if (latG > 1.2f) ApexRed else ApexCyan, modifier = Modifier.weight(1f))
                        DetailMiniCard(title = "PEDALS", value = "T:${throttle}% B:${brake}%", color = if (brake > 0) ApexRed else ApexGreen, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. ENGINE TEMPERATURE RECHARTS VIEW
// =========================================================================

@Composable
private fun EngineThermalRechartsView(
    telemetryHistory: List<TelemetryPoint>,
    activePoint: TelemetryPoint?,
    scrubRatio: Float,
    onScrubChange: (Float) -> Unit
) {
    val coolantTemp = activePoint?.engineTempC ?: 88.5f
    val oilTemp = activePoint?.engineOilTempC ?: 92.0f
    val isOverheating = coolantTemp > 118f || oilTemp > 122f

    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(290.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isOverheating) ApexRed else SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ENGINE THERMAL REGIME (COOLANT & OIL)", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Recharts Monotone curve • Caution 105°C • Danger 120°C", color = TextMuted, fontSize = 9.sp)
                        }
                        if (isOverheating) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = "Overheat", tint = ApexRed, modifier = Modifier.size(14.dp))
                                Text("OVERHEAT WARNING", color = ApexRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        } else {
                            Text(
                                text = "${coolantTemp.toInt()}°C",
                                color = if (coolantTemp > 105f) ApexOrange else ApexGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Recharts Curve for Engine Temp
                    RechartsAreaCurveCanvas(
                        points = telemetryHistory.map { it.engineTempC },
                        maxScale = 135f,
                        minScale = 65f,
                        primaryColor = if (coolantTemp > 105f) ApexOrange else ApexGreen,
                        referenceLineVal = 105f,
                        referenceLineLabel = "105°C Thermal Load",
                        secondReferenceLineVal = 120f,
                        secondReferenceLineLabel = "120°C Limit",
                        activeRatio = scrubRatio,
                        onScrub = onScrubChange,
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Engine Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DetailMiniCard(
                            title = "COOLANT TEMP",
                            value = "${coolantTemp.toInt()}°C",
                            color = if (coolantTemp > 105f) ApexOrange else ApexGreen,
                            modifier = Modifier.weight(1f)
                        )
                        DetailMiniCard(
                            title = "OIL TEMP",
                            value = "${oilTemp.toInt()}°C",
                            color = if (oilTemp > 115f) ApexOrange else ApexGold,
                            modifier = Modifier.weight(1f)
                        )
                        DetailMiniCard(
                            title = "THERMAL BAND",
                            value = if (coolantTemp < 75f) "Cold" else if (coolantTemp <= 105f) "Optimal" else "Overheat Risk",
                            color = if (coolantTemp < 75f) Color(0xFF42A5F5) else if (coolantTemp <= 105f) ApexGreen else ApexRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 5. TIRE WEAR & DEGRADATION RECHARTS VIEW
// =========================================================================

@Composable
private fun TireWearRechartsView(
    telemetryHistory: List<TelemetryPoint>,
    activePoint: TelemetryPoint?,
    scrubRatio: Float,
    onScrubChange: (Float) -> Unit,
    flWear: Float,
    frWear: Float,
    rlWear: Float,
    rrWear: Float
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(290.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TIRE WEAR PROGRESSION & DEGRADATION", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Cumulative wear percentage over active laps", color = TextMuted, fontSize = 9.sp)
                        }
                        Text(
                            text = "AVG WEAR ${(((flWear + frWear + rlWear + rrWear) / 4f) * 100).toInt()}%",
                            color = ApexOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Recharts Curve for Tire Wear
                    RechartsAreaCurveCanvas(
                        points = telemetryHistory.map { ((it.tireWearFL + it.tireWearFR + it.tireWearRL + it.tireWearRR) / 4f) * 100f },
                        maxScale = 100f,
                        minScale = 0f,
                        primaryColor = ApexOrange,
                        referenceLineVal = 50f,
                        referenceLineLabel = "50% Pit Threshold",
                        secondReferenceLineVal = 75f,
                        secondReferenceLineLabel = "75% Grip Penalty",
                        activeRatio = scrubRatio,
                        onScrub = onScrubChange,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4-Corner Chassis Wear Map
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CornerWearPill(position = "FL", wear = flWear)
                        CornerWearPill(position = "FR", wear = frWear)
                        CornerWearPill(position = "RL", wear = rlWear)
                        CornerWearPill(position = "RR", wear = rrWear)
                    }
                }
            }
        }
    }
}

@Composable
private fun CornerWearPill(position: String, wear: Float) {
    val lifePct = ((1f - wear) * 100).toInt().coerceIn(0, 100)
    val color = when {
        lifePct > 70 -> ApexGreen
        lifePct > 45 -> ApexCyan
        lifePct > 25 -> ApexOrange
        else -> ApexRed
    }

    Box(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, color.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(position, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Box(modifier = Modifier.size(5.dp).background(color, CircleShape))
            }
            Text(
                text = "$lifePct%",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            LinearProgressIndicator(
                progress = { lifePct / 100f },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = Color(0xFF222838),
            )
            Text(
                text = "${(wear * 100).toInt()}% worn",
                color = TextMuted,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// =========================================================================
// 6. RECHARTS CANVAS ENGINE (Monotone Cubic Bezier Splines & Grid)
// =========================================================================

/**
 * Recharts-style Area Curve with Monotone cubic bezier spline smoothing,
 * gradient fill, Cartesian dashed grid lines, and interactive crosshair scrubbing.
 */
@Composable
private fun RechartsAreaCurveCanvas(
    points: List<Float>,
    maxScale: Float,
    minScale: Float = 0f,
    primaryColor: Color,
    referenceLineVal: Float? = null,
    referenceLineLabel: String? = null,
    secondReferenceLineVal: Float? = null,
    secondReferenceLineLabel: String? = null,
    activeRatio: Float = -1f,
    onScrub: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onScrub?.invoke((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onScrub?.invoke((offset.x / size.width).coerceIn(0f, 1f)) },
                    onDrag = { change, _ ->
                        change.consume()
                        onScrub?.invoke((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val range = (maxScale - minScale).coerceAtLeast(1f)

        // 1. Cartesian Grid Lines (Horizontal & Vertical Dashed Lines)
        val gridColor = Color(0xFF1E2330)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        for (i in 1..3) {
            val y = h * (i / 4f)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f, pathEffect = dashEffect)
        }
        for (i in 1..4) {
            val x = w * (i / 5f)
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f, pathEffect = dashEffect)
        }

        // 2. Reference Threshold Lines (Recharts ReferenceLine)
        referenceLineVal?.let { refVal ->
            val refNorm = ((refVal - minScale) / range).coerceIn(0f, 1f)
            val yRef = h - (refNorm * (h - 10f))
            drawLine(
                color = ApexGold.copy(alpha = 0.85f),
                start = Offset(0f, yRef),
                end = Offset(w, yRef),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
            )
        }

        secondReferenceLineVal?.let { refVal ->
            val refNorm = ((refVal - minScale) / range).coerceIn(0f, 1f)
            val yRef = h - (refNorm * (h - 10f))
            drawLine(
                color = ApexRed.copy(alpha = 0.85f),
                start = Offset(0f, yRef),
                end = Offset(w, yRef),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
            )
        }

        if (points.size < 2) return@Canvas

        // 3. Compute Coordinates
        val count = points.size
        val coords = points.mapIndexed { idx, valItem ->
            val x = (idx.toFloat() / (count - 1)) * w
            val normalized = ((valItem - minScale) / range).coerceIn(0f, 1f)
            val y = h - (normalized * (h - 12f))
            Offset(x, y)
        }

        // 4. Build Monotone Cubic Spline (Recharts monotone Bezier)
        val curvePath = Path()
        val areaPath = Path()

        coords.forEachIndexed { i, pt ->
            if (i == 0) {
                curvePath.moveTo(pt.x, pt.y)
                areaPath.moveTo(pt.x, h)
                areaPath.lineTo(pt.x, pt.y)
            } else {
                val prev = coords[i - 1]
                val midX = (prev.x + pt.x) * 0.5f
                curvePath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                areaPath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
            }
        }

        areaPath.lineTo(w, h)
        areaPath.close()

        // 5. Draw Glowing Gradient Fill Area
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.32f), primaryColor.copy(alpha = 0.02f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        // 6. Draw Stroke Line
        drawPath(
            path = curvePath,
            color = primaryColor,
            style = Stroke(width = 2.4f, cap = StrokeCap.Round)
        )

        // 7. Interactive Crosshair Scrubber
        if (activeRatio in 0f..1f) {
            val scrubX = activeRatio * w
            drawLine(
                color = Color.White.copy(alpha = 0.75f),
                start = Offset(scrubX, 0f),
                end = Offset(scrubX, h),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )

            val activeIdx = (activeRatio * (count - 1)).toInt().coerceIn(0, count - 1)
            val targetPt = coords[activeIdx]
            drawCircle(color = primaryColor, radius = 6f, center = targetPt)
            drawCircle(color = Color.White, radius = 2.5f, center = targetPt)
        }
    }
}

/**
 * Recharts Multi-Stream synchronized canvas for Velocity, Engine Temp, and Tire Wear.
 */
@Composable
private fun RechartsCanvasMultiStream(
    points: List<TelemetryPoint>,
    activeRatio: Float,
    onScrub: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onScrub?.invoke((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onScrub?.invoke((offset.x / size.width).coerceIn(0f, 1f)) },
                    onDrag = { change, _ ->
                        change.consume()
                        onScrub?.invoke((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height

        // Grid
        val gridColor = Color(0xFF1E2330)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        for (i in 1..3) {
            val y = h * (i / 4f)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f, pathEffect = dashEffect)
        }

        if (points.size < 2) return@Canvas
        val count = points.size

        val speedPath = Path()
        val tempPath = Path()
        val wearPath = Path()

        points.forEachIndexed { i, p ->
            val x = (i.toFloat() / (count - 1)) * w
            val ySpeed = h - ((p.speedKmh / 280f).coerceIn(0f, 1f) * (h - 10f))
            val yTemp = h - (((p.engineTempC - 65f) / 65f).coerceIn(0f, 1f) * (h - 10f))
            val avgWear = (p.tireWearFL + p.tireWearFR + p.tireWearRL + p.tireWearRR) / 4f
            val yWear = h - (avgWear.coerceIn(0f, 1f) * (h - 10f))

            if (i == 0) {
                speedPath.moveTo(x, ySpeed)
                tempPath.moveTo(x, yTemp)
                wearPath.moveTo(x, yWear)
            } else {
                val prevX = ((i - 1).toFloat() / (count - 1)) * w
                val midX = (prevX + x) * 0.5f
                val prevP = points[i - 1]
                val prevSpeedY = h - ((prevP.speedKmh / 280f).coerceIn(0f, 1f) * (h - 10f))
                val prevTempY = h - (((prevP.engineTempC - 65f) / 65f).coerceIn(0f, 1f) * (h - 10f))
                val prevWear = (prevP.tireWearFL + prevP.tireWearFR + prevP.tireWearRL + prevP.tireWearRR) / 4f
                val prevWearY = h - (prevWear.coerceIn(0f, 1f) * (h - 10f))

                speedPath.cubicTo(midX, prevSpeedY, midX, ySpeed, x, ySpeed)
                tempPath.cubicTo(midX, prevTempY, midX, yTemp, x, yTemp)
                wearPath.cubicTo(midX, prevWearY, midX, yWear, x, yWear)
            }
        }

        drawPath(speedPath, ApexCyan, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
        drawPath(tempPath, ApexGold, style = Stroke(width = 2.0f, cap = StrokeCap.Round))
        drawPath(wearPath, ApexOrange, style = Stroke(width = 1.8f, cap = StrokeCap.Round))

        if (activeRatio in 0f..1f) {
            val scrubX = activeRatio * w
            drawLine(Color.White.copy(alpha = 0.7f), Offset(scrubX, 0f), Offset(scrubX, h), strokeWidth = 1.2f, pathEffect = dashEffect)
        }
    }
}

@Composable
private fun ActiveScrubTooltipReadout(activePoint: TelemetryPoint?) {
    if (activePoint == null) return
    val speed = activePoint.speedKmh.toInt()
    val temp = activePoint.engineTempC.toInt()
    val wear = activePoint.avgTireWearPct.toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("t=%.1fs | Gear %d", activePoint.timeSec, activePoint.gear),
                color = TextWhite,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Speed: $speed km/h", color = ApexCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("Engine: $temp°C", color = ApexGold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("Wear: $wear%", color = ApexOrange, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Text(text = label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceElevated)
            .border(0.8.dp, SurfaceBorder, RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Column {
            Text(title, color = TextMuted, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}
