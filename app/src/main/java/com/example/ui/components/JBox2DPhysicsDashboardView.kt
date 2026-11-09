package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.physics.simulation2d.WheelId2D
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
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Real-time data visualization dashboard utilizing custom Compose Canvas to display:
 * 1. Speedometer gauge with JBox2D rigid-body linear velocity decomposition
 * 2. Tachometer arc gauge with dynamic shift lights & gear indicator
 * 3. Engine coolant and oil thermal gauge with aerodynamics cooling rate
 * 4. Multi-trace synchronized Canvas waveform chart for Speed, RPM, and Engine Temp over time
 * 5. JBox2D physics engine telemetry status bar (aerodynamic drag, downforce, weight transfer)
 */
@Composable
fun JBox2DPhysicsDashboardView(
    vehicle: Vehicle,
    telemetryHistory: List<TelemetryPoint>,
    playerState: CarPhysicsState? = null,
    simulation2DTelemetry: VehicleSimulation2DTelemetry? = null,
    modifier: Modifier = Modifier
) {
    var scrubRatio by remember { mutableFloatStateOf(-1f) }

    val latestPoint = telemetryHistory.lastOrNull()
    val activePoint = if (scrubRatio in 0f..1f && telemetryHistory.isNotEmpty()) {
        val targetIdx = ((telemetryHistory.size - 1) * scrubRatio).toInt().coerceIn(0, telemetryHistory.size - 1)
        telemetryHistory[targetIdx]
    } else {
        latestPoint
    }

    // Extract dynamic values prioritizing active JBox2D simulation state
    val simBody = simulation2DTelemetry?.bodyState
    val simAero = simulation2DTelemetry?.aeroTelemetry
    val simTires = simulation2DTelemetry?.tireTelemetry

    val currentSpeedKmh = simBody?.speedKmh
        ?: playerState?.speedKmh
        ?: activePoint?.speedKmh
        ?: 0f

    val currentRpm = playerState?.rpm
        ?: activePoint?.rpm
        ?: 1000f

    val currentGear = playerState?.currentGear
        ?: activePoint?.gear
        ?: 1

    val currentEngineTempC = playerState?.engineTempC
        ?: activePoint?.engineTempC
        ?: 88.5f

    val currentOilTempC = playerState?.engineOilTempC
        ?: activePoint?.engineOilTempC
        ?: 92.0f

    val vxMps = simBody?.vxMps ?: ((playerState?.vx ?: 0f))
    val vyMps = simBody?.vyMps ?: ((playerState?.vy ?: 0f))
    val yawRate = simBody?.angularVelocityDegPerSec ?: (playerState?.angularVelocity ?: 0f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("jbox2d_physics_dashboard_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Dual Canvas Instrument Gauges: Speedometer, Tachometer & Thermal Meter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Speedometer Gauge
            SpeedometerDialCanvas(
                speedKmh = currentSpeedKmh,
                vxMps = vxMps,
                vyMps = vyMps,
                maxSpeed = 320f,
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )

            // Tachometer (RPM) & Shift Light Gauge
            TachometerDialCanvas(
                rpm = currentRpm,
                gear = currentGear,
                maxRpm = 9000f,
                redlineRpm = 7500f,
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )
        }

        // 2. Engine Temperature & Thermal Cooling Canvas Bar
        EngineThermalGaugeCanvas(
            coolantTempC = currentEngineTempC,
            oilTempC = currentOilTempC,
            airDensityKgM3 = simAero?.airDensityKgM3 ?: 1.225f,
            speedMps = currentSpeedKmh / 3.6f,
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        )

        // 3. Multi-Trace Synchronized Real-Time Canvas Waveform Chart (Speed, RPM, Temp)
        MultiTraceWaveformCanvas(
            telemetryHistory = telemetryHistory,
            activePoint = activePoint,
            scrubRatio = scrubRatio,
            onScrubChange = { scrubRatio = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        )

        // 4. JBox2D Rigid-Body Physics Telemetry Readout
        JBox2DPhysicsStatusCard(
            simBody = simBody,
            simAero = simAero,
            simTires = simTires,
            speedKmh = currentSpeedKmh,
            yawRate = yawRate
        )
    }
}

/**
 * Custom Canvas Speedometer Dial: Swept arc gauge with dynamic needle, tick marks,
 * and JBox2D velocity vector decomposition.
 */
@Composable
fun SpeedometerDialCanvas(
    speedKmh: Float,
    vxMps: Float,
    vyMps: Float,
    maxSpeed: Float = 320f,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speedKmh.coerceIn(0f, maxSpeed),
        animationSpec = tween(durationMillis = 80),
        label = "SpeedDialAnim"
    )

    Card(
        modifier = modifier
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .testTag("speedometer_dial_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.52f)
                val radius = min(size.width, size.height) * 0.40f
                val strokeWidth = 10.dp.toPx()

                // Sweep parameters: 140° to 400° (260° arc)
                val startAngle = 140f
                val sweepAngle = 260f

                // 1. Background Track Arc
                drawArc(
                    color = Color(0xFF1E2435),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 2. Active Speed Arc with Gradient
                val speedFraction = (animatedSpeed / maxSpeed).coerceIn(0f, 1f)
                val activeSweep = sweepAngle * speedFraction

                if (activeSweep > 1f) {
                    val gradient = Brush.sweepGradient(
                        0.0f to ApexCyan,
                        0.6f to ApexGreen,
                        0.85f to ApexGold,
                        1.0f to ApexOrange,
                        center = center
                    )
                    drawArc(
                        brush = gradient,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 3. Tick Marks
                val totalTicks = 16
                for (i in 0..totalTicks) {
                    val tickAngle = startAngle + (sweepAngle * (i.toFloat() / totalTicks))
                    val tickRad = (tickAngle * PI / 180f).toFloat()
                    val isMajor = i % 2 == 0
                    val tickLen = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                    val innerR = radius - strokeWidth * 0.6f - tickLen
                    val outerR = radius - strokeWidth * 0.6f

                    val pStart = Offset(center.x + innerR * cos(tickRad), center.y + innerR * sin(tickRad))
                    val pEnd = Offset(center.x + outerR * cos(tickRad), center.y + outerR * sin(tickRad))

                    drawLine(
                        color = if (isMajor) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                    )
                }

                // 4. Needle
                val needleAngle = startAngle + activeSweep
                val needleRad = (needleAngle * PI / 180f).toFloat()
                val needleLength = radius * 0.85f
                val needleTip = Offset(
                    center.x + needleLength * cos(needleRad),
                    center.y + needleLength * sin(needleRad)
                )

                // Needle shadow/glow line
                drawLine(
                    color = ApexCyan.copy(alpha = 0.35f),
                    start = center,
                    end = needleTip,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Main needle line
                drawLine(
                    color = ApexCyan,
                    start = center,
                    end = needleTip,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Needle Center Hub
                drawCircle(color = ApexCyan, radius = 6.dp.toPx(), center = center)
                drawCircle(color = DarkSurface, radius = 3.dp.toPx(), center = center)
            }

            // Digital Speed Readout & JBox2D velocity
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${animatedSpeed.toInt()}",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "KM/H",
                    color = ApexCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = String.format(Locale.US, "v(%.1f, %.1f) m/s", vxMps, vyMps),
                    color = TextMuted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Custom Canvas Tachometer (RPM) Dial: Radial arc with color-coded safety, warning,
 * and redline zones, real-time gear badge, and sequential LED shift lights.
 */
@Composable
fun TachometerDialCanvas(
    rpm: Float,
    gear: Int,
    maxRpm: Float = 9000f,
    redlineRpm: Float = 7500f,
    modifier: Modifier = Modifier
) {
    val animatedRpm by animateFloatAsState(
        targetValue = rpm.coerceIn(0f, maxRpm),
        animationSpec = tween(durationMillis = 80),
        label = "RpmDialAnim"
    )

    val rpmFraction = (animatedRpm / maxRpm).coerceIn(0f, 1f)
    val isRedlining = animatedRpm >= redlineRpm

    Card(
        modifier = modifier
            .border(
                1.dp,
                if (isRedlining) ApexRed else SurfaceBorder,
                RoundedCornerShape(14.dp)
            )
            .testTag("tachometer_dial_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.52f)
                val radius = min(size.width, size.height) * 0.40f
                val strokeWidth = 10.dp.toPx()

                val startAngle = 140f
                val sweepAngle = 260f

                // 1. Background Arc
                drawArc(
                    color = Color(0xFF1E2435),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 2. Redline Zone Highlight
                val redlineFraction = redlineRpm / maxRpm
                val redlineStart = startAngle + sweepAngle * redlineFraction
                val redlineSweep = sweepAngle * (1f - redlineFraction)
                drawArc(
                    color = ApexRed.copy(alpha = 0.35f),
                    startAngle = redlineStart,
                    sweepAngle = redlineSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 3. Active RPM Arc
                val activeSweep = sweepAngle * rpmFraction
                if (activeSweep > 1f) {
                    val activeColor = when {
                        animatedRpm >= redlineRpm -> ApexRed
                        animatedRpm >= 5500f -> ApexGold
                        else -> ApexGreen
                    }
                    drawArc(
                        color = activeColor,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 4. Tick Marks
                val totalTicks = 18
                for (i in 0..totalTicks) {
                    val tickAngle = startAngle + (sweepAngle * (i.toFloat() / totalTicks))
                    val tickRad = (tickAngle * PI / 180f).toFloat()
                    val isMajor = i % 2 == 0
                    val isRedlineTick = (i.toFloat() / totalTicks) >= redlineFraction
                    val tickLen = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                    val innerR = radius - strokeWidth * 0.6f - tickLen
                    val outerR = radius - strokeWidth * 0.6f

                    val pStart = Offset(center.x + innerR * cos(tickRad), center.y + innerR * sin(tickRad))
                    val pEnd = Offset(center.x + outerR * cos(tickRad), center.y + outerR * sin(tickRad))

                    drawLine(
                        color = if (isRedlineTick) ApexRed else if (isMajor) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                    )
                }

                // 5. Needle
                val needleAngle = startAngle + activeSweep
                val needleRad = (needleAngle * PI / 180f).toFloat()
                val needleLength = radius * 0.85f
                val needleTip = Offset(
                    center.x + needleLength * cos(needleRad),
                    center.y + needleLength * sin(needleRad)
                )

                val needleColor = if (isRedlining) ApexRed else ApexGold
                drawLine(
                    color = needleColor.copy(alpha = 0.35f),
                    start = center,
                    end = needleTip,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = needleColor,
                    start = center,
                    end = needleTip,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Hub
                drawCircle(color = needleColor, radius = 6.dp.toPx(), center = center)
                drawCircle(color = DarkSurface, radius = 3.dp.toPx(), center = center)
            }

            // Shift Light LED bar at top
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val ledCount = 5
                for (led in 1..ledCount) {
                    val threshold = (led.toFloat() / (ledCount + 1))
                    val isOn = rpmFraction >= threshold
                    val ledColor = when {
                        led >= 4 -> ApexRed
                        led == 3 -> ApexGold
                        else -> ApexGreen
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isOn) ledColor else Color(0xFF202636))
                    )
                }
            }

            // Digital RPM & Gear Readout
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${animatedRpm.toInt()}",
                    color = if (isRedlining) ApexRed else TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "GEAR $gear",
                    color = ApexGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Custom Canvas Engine Temperature Gauge: Coolant & Oil thermal bars with optimal,
 * elevated, and critical overheat zones plus aerodynamics ram-air cooling efficiency.
 */
@Composable
fun EngineThermalGaugeCanvas(
    coolantTempC: Float,
    oilTempC: Float,
    airDensityKgM3: Float,
    speedMps: Float,
    modifier: Modifier = Modifier
) {
    val isOverheating = coolantTempC > 115f || oilTempC > 120f
    val isElevated = coolantTempC > 102f || oilTempC > 108f

    Card(
        modifier = modifier
            .border(
                1.dp,
                if (isOverheating) ApexRed else SurfaceBorder,
                RoundedCornerShape(12.dp)
            )
            .testTag("engine_thermal_gauge_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ENGINE THERMAL STATE",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isOverheating) ApexRed else if (isElevated) ApexOrange else ApexGreen)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isOverheating) "OVERHEAT" else if (isElevated) "WARM" else "OPTIMAL",
                            color = CarbonBlack,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    text = String.format(Locale.US, "Airflow Cooling: %.0f%% (ρ=%.2f)", (speedMps / 60f).coerceIn(0f, 1f) * 100f, airDensityKgM3),
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Dual Thermal Canvas Bars: Coolant & Oil
            Canvas(modifier = Modifier.fillMaxWidth().height(38.dp)) {
                val barWidth = size.width
                val barHeight = 12.dp.toPx()
                val radius = barHeight / 2f

                // Scale: 60°C to 130°C (range 70°C)
                val minT = 60f
                val maxT = 130f
                val coolantFrac = ((coolantTempC - minT) / (maxT - minT)).coerceIn(0f, 1f)
                val oilFrac = ((oilTempC - minT) / (maxT - minT)).coerceIn(0f, 1f)

                // 1. Coolant Bar (Top)
                val yCoolant = 2.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF1E2435),
                    topLeft = Offset(0f, yCoolant),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
                // Optimal Zone Indicator (80°C - 100°C)
                val optStart = barWidth * ((80f - minT) / (maxT - minT))
                val optEnd = barWidth * ((100f - minT) / (maxT - minT))
                drawRect(
                    color = ApexGreen.copy(alpha = 0.2f),
                    topLeft = Offset(optStart, yCoolant),
                    size = Size(optEnd - optStart, barHeight)
                )
                // Active Coolant Fill
                val coolantColor = when {
                    coolantTempC > 115f -> ApexRed
                    coolantTempC > 102f -> ApexOrange
                    else -> ApexCyan
                }
                if (coolantFrac > 0.02f) {
                    drawRoundRect(
                        color = coolantColor,
                        topLeft = Offset(0f, yCoolant),
                        size = Size(barWidth * coolantFrac, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                    )
                }

                // 2. Oil Bar (Bottom)
                val yOil = 22.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF1E2435),
                    topLeft = Offset(0f, yOil),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
                val oilColor = when {
                    oilTempC > 120f -> ApexRed
                    oilTempC > 108f -> ApexOrange
                    else -> ApexGold
                }
                if (oilFrac > 0.02f) {
                    drawRoundRect(
                        color = oilColor,
                        topLeft = Offset(0f, yOil),
                        size = Size(barWidth * oilFrac, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                    )
                }
            }

            // Value Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "Coolant: %.1f°C", coolantTempC),
                    color = if (coolantTempC > 115f) ApexRed else ApexCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format(Locale.US, "Oil: %.1f°C", oilTempC),
                    color = if (oilTempC > 120f) ApexRed else ApexGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Multi-Trace Synchronized Waveform Chart using Jetpack Compose Canvas:
 * Renders simultaneous, real-time lines for Speed (km/h), Engine RPM, and Engine Temp (°C)
 * with an interactive drag/touch scrubbing crosshair.
 */
@Composable
fun MultiTraceWaveformCanvas(
    telemetryHistory: List<TelemetryPoint>,
    activePoint: TelemetryPoint?,
    scrubRatio: Float,
    onScrubChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .testTag("multi_trace_waveform_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Header with legend and current values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME MULTI-TRACE WAVEFORM",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TracePill(label = "Speed", value = "${activePoint?.speedKmh?.toInt() ?: 0}", unit = "km/h", color = ApexCyan)
                    TracePill(label = "RPM", value = "${activePoint?.rpm?.toInt() ?: 0}", unit = "", color = ApexGold)
                    TracePill(label = "Temp", value = "${activePoint?.engineTempC?.toInt() ?: 88}", unit = "°C", color = ApexRed)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Canvas plotting traces
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(telemetryHistory) {
                        detectTapGestures(
                            onTap = { offset ->
                                val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                                onScrubChange(ratio)
                            }
                        )
                    }
                    .pointerInput(telemetryHistory) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                                onScrubChange(ratio)
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val n = telemetryHistory.size

                    // 1. Grid lines (25%, 50%, 75%)
                    val gridColor = Color(0xFF1E2436)
                    for (i in 1..3) {
                        val y = h * (i / 4f)
                        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                    }

                    if (n >= 2) {
                        val stepX = w / (n - 1).toFloat()

                        // 2. Speed Trace (Max 320 km/h)
                        val maxSpeed = 320f
                        val speedPath = Path()
                        val speedArea = Path()
                        speedArea.moveTo(0f, h)

                        for (i in 0 until n) {
                            val x = i * stepX
                            val speedVal = telemetryHistory[i].speedKmh.coerceIn(0f, maxSpeed)
                            val y = h - (speedVal / maxSpeed) * (h * 0.90f)

                            if (i == 0) {
                                speedPath.moveTo(x, y)
                                speedArea.lineTo(x, y)
                            } else {
                                speedPath.lineTo(x, y)
                                speedArea.lineTo(x, y)
                            }
                        }
                        speedArea.lineTo(w, h)
                        speedArea.close()

                        // Shaded area under speed trace
                        drawPath(
                            path = speedArea,
                            brush = Brush.verticalGradient(
                                colors = listOf(ApexCyan.copy(alpha = 0.25f), Color.Transparent),
                                startY = 0f,
                                endY = h
                            )
                        )
                        // Speed stroke
                        drawPath(path = speedPath, color = ApexCyan, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                        // 3. RPM Trace (Max 9000 RPM)
                        val maxRpm = 9000f
                        val rpmPath = Path()
                        for (i in 0 until n) {
                            val x = i * stepX
                            val rpmVal = telemetryHistory[i].rpm.coerceIn(0f, maxRpm)
                            val y = h - (rpmVal / maxRpm) * (h * 0.90f)
                            if (i == 0) rpmPath.moveTo(x, y) else rpmPath.lineTo(x, y)
                        }
                        drawPath(path = rpmPath, color = ApexGold, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))

                        // 4. Engine Temp Trace (60°C to 130°C)
                        val tempPath = Path()
                        for (i in 0 until n) {
                            val x = i * stepX
                            val tempVal = telemetryHistory[i].engineTempC.coerceIn(60f, 130f)
                            val y = h - ((tempVal - 60f) / 70f) * (h * 0.90f)
                            if (i == 0) tempPath.moveTo(x, y) else tempPath.lineTo(x, y)
                        }
                        drawPath(
                            path = tempPath,
                            color = ApexRed,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        )

                        // 5. Crosshair Cursor if scrubbing
                        if (scrubRatio in 0f..1f) {
                            val crossX = w * scrubRatio
                            drawLine(
                                color = TextWhite,
                                start = Offset(crossX, 0f),
                                end = Offset(crossX, h),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawCircle(color = ApexCyan, radius = 4.dp.toPx(), center = Offset(crossX, h * 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TracePill(label: String, value: String, unit: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            text = "$label: $value$unit",
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Status card summarizing real-time JBox2D physics engine state variables.
 */
@Composable
private fun JBox2DPhysicsStatusCard(
    simBody: com.example.physics.simulation2d.VehicleBody2DState?,
    simAero: com.example.physics.simulation2d.AeroDragTelemetry2D?,
    simTires: com.example.physics.simulation2d.VehicleTireTelemetry2D?,
    speedKmh: Float,
    yawRate: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .testTag("jbox2d_status_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JBOX2D RIGID-BODY KINEMATICS & FORCES",
                    color = ApexCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "60Hz SOLVER ACTIVE",
                    color = ApexGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PhysicsStatusPill(
                    label = "AERO DRAG",
                    value = simAero?.formattedDragForce ?: "${(0.5f * 1.225f * 0.32f * 2.1f * (speedKmh / 3.6f) * (speedKmh / 3.6f)).toInt()} N",
                    color = ApexGold,
                    modifier = Modifier.weight(1f)
                )
                PhysicsStatusPill(
                    label = "DOWNFORCE",
                    value = simAero?.formattedDownforce ?: "${(0.5f * 1.225f * 0.45f * 2.1f * (speedKmh / 3.6f) * (speedKmh / 3.6f)).toInt()} N",
                    color = ApexCyan,
                    modifier = Modifier.weight(1f)
                )
                PhysicsStatusPill(
                    label = "YAW RATE",
                    value = String.format(Locale.US, "%+.1f°/s", yawRate),
                    color = if (abs(yawRate) > 20f) ApexOrange else TextWhite,
                    modifier = Modifier.weight(1f)
                )
                PhysicsStatusPill(
                    label = "DRIVE TRACTION",
                    value = simTires?.formattedTractionForce ?: "1850 N",
                    color = ApexGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PhysicsStatusPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurface)
            .border(0.5.dp, SurfaceBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column {
            Text(label, color = TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}
