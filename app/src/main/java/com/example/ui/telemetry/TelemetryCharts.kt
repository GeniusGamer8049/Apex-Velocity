package com.example.ui.telemetry

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryPoint
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
import kotlin.math.abs

/**
 * Real-time Line & Area Chart for Speed (km/h) and Engine RPM over rolling time window.
 */
@Composable
fun SpeedRpmChart(
    points: List<TelemetryPoint>,
    modifier: Modifier = Modifier,
    maxSpeed: Float = 350f,
    maxRpm: Float = 9000f
) {
    Card(
        modifier = modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPEED & ENGINE RPM TRACE",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(ApexCyan, CircleShape))
                        val currentSpeed = points.lastOrNull()?.speedKmh?.toInt() ?: 0
                        Text("Speed: $currentSpeed km/h", color = ApexCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(ApexGold, CircleShape))
                        val currentRpm = points.lastOrNull()?.rpm?.toInt() ?: 0
                        Text("RPM: $currentRpm", color = ApexGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Line Chart
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val w = size.width
                val h = size.height

                // Draw background grid lines (horizontal 25%, 50%, 75%)
                val gridColor = Color(0xFF222838)
                for (i in 1..3) {
                    val y = h * (i / 4f)
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }

                if (points.size < 2) return@Canvas

                val count = points.size
                val speedPath = Path()
                val speedAreaPath = Path()
                val rpmPath = Path()

                points.forEachIndexed { i, p ->
                    val x = (i.toFloat() / (count - 1)) * w
                    val normalizedSpeed = (p.speedKmh / maxSpeed).coerceIn(0f, 1f)
                    val ySpeed = h - (normalizedSpeed * (h - 10f))

                    val normalizedRpm = (p.rpm / maxRpm).coerceIn(0f, 1f)
                    val yRpm = h - (normalizedRpm * (h - 10f))

                    if (i == 0) {
                        speedPath.moveTo(x, ySpeed)
                        speedAreaPath.moveTo(x, h)
                        speedAreaPath.lineTo(x, ySpeed)
                        rpmPath.moveTo(x, yRpm)
                    } else {
                        speedPath.lineTo(x, ySpeed)
                        speedAreaPath.lineTo(x, ySpeed)
                        rpmPath.lineTo(x, yRpm)
                    }
                }

                speedAreaPath.lineTo(w, h)
                speedAreaPath.close()

                // Draw Area fill for Speed
                drawPath(
                    path = speedAreaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(ApexCyan.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )

                // Draw Speed Line
                drawPath(path = speedPath, color = ApexCyan, style = Stroke(width = 2.5f))

                // Draw RPM Line
                drawPath(path = rpmPath, color = ApexGold, style = Stroke(width = 1.8f))
            }
        }
    }
}

/**
 * Throttle & Brake Pedals Telemetry Trace (0-100% inputs over time)
 */
@Composable
fun ThrottleBrakeChart(
    points: List<TelemetryPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PEDAL INPUTS (THROTTLE / BRAKE)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(ApexGreen, CircleShape))
                        val th = ((points.lastOrNull()?.throttle ?: 0f) * 100).toInt()
                        Text("Throttle: $th%", color = ApexGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(ApexRed, CircleShape))
                        val br = ((points.lastOrNull()?.brake ?: 0f) * 100).toInt()
                        Text("Brake: $br%", color = ApexRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                val w = size.width
                val h = size.height

                // Center/Zero line
                drawLine(Color(0xFF222838), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)

                if (points.size < 2) return@Canvas
                val count = points.size

                val throttlePath = Path()
                val brakePath = Path()

                points.forEachIndexed { i, p ->
                    val x = (i.toFloat() / (count - 1)) * w
                    val yThrottle = h - (p.throttle.coerceIn(0f, 1f) * (h - 8f))
                    val yBrake = h - (p.brake.coerceIn(0f, 1f) * (h - 8f))

                    if (i == 0) {
                        throttlePath.moveTo(x, yThrottle)
                        brakePath.moveTo(x, yBrake)
                    } else {
                        throttlePath.lineTo(x, yThrottle)
                        brakePath.lineTo(x, yBrake)
                    }
                }

                drawPath(path = throttlePath, color = ApexGreen, style = Stroke(width = 2.2f))
                drawPath(path = brakePath, color = ApexRed, style = Stroke(width = 2.2f))
            }
        }
    }
}

/**
 * 4-Wheel Tire Thermal Heatmap & Status (FL, FR, RL, RR)
 */
@Composable
fun FourWheelThermalMap(
    point: TelemetryPoint?,
    modifier: Modifier = Modifier
) {
    val fl = point?.tempFL ?: 80f
    val fr = point?.tempFR ?: 80f
    val rl = point?.tempRL ?: 80f
    val rr = point?.tempRR ?: 80f

    Card(
        modifier = modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("4-CORNER TIRE THERMAL MAP", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Optimal: 75°C - 100°C", color = ApexGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Front Left & Rear Left
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TireBadge("FL", fl)
                    TireBadge("RL", rl)
                }

                // Center Chassis Diagram
                Canvas(modifier = Modifier.size(width = 70.dp, height = 120.dp)) {
                    val cw = size.width
                    val ch = size.height
                    // Chassis outline
                    drawRoundRect(
                        color = Color(0xFF222838),
                        topLeft = Offset(cw * 0.15f, ch * 0.08f),
                        size = Size(cw * 0.70f, ch * 0.84f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
                    )
                    drawRoundRect(
                        color = ApexCyan.copy(alpha = 0.5f),
                        topLeft = Offset(cw * 0.15f, ch * 0.08f),
                        size = Size(cw * 0.70f, ch * 0.84f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f),
                        style = Stroke(width = 1.5f)
                    )
                    // Cockpit glass
                    drawCircle(Color(0xFF151922), radius = 12f, center = Offset(cw * 0.5f, ch * 0.45f))
                    // Front chevron
                    drawLine(ApexCyan, Offset(cw * 0.5f, ch * 0.12f), Offset(cw * 0.5f, ch * 0.22f), strokeWidth = 2f)
                }

                // Front Right & Rear Right
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TireBadge("FR", fr)
                    TireBadge("RR", rr)
                }
            }
        }
    }
}

@Composable
private fun TireBadge(position: String, temp: Float) {
    val tempColor = when {
        temp < 65f -> Color(0xFF42A5F5)   // Cold Blue
        temp < 80f -> Color(0xFF26C6DA)   // Warm Cyan
        temp <= 100f -> ApexGreen         // Peak Optimal Green
        temp <= 112f -> ApexOrange        // Hot Amber
        else -> ApexRed                   // Overheating Crimson
    }

    val status = when {
        temp < 65f -> "Cold"
        temp < 80f -> "Warming"
        temp <= 100f -> "Optimal"
        temp <= 112f -> "Hot"
        else -> "Blistering"
    }

    Box(
        modifier = Modifier
            .width(105.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.5.dp, tempColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(position, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(modifier = Modifier.size(6.dp).background(tempColor, CircleShape))
            }
            Text(
                text = "${temp.toInt()}°C",
                color = tempColor,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(status, color = TextMuted, fontSize = 10.sp)
        }
    }
}

/**
 * G-G Diagram (Friction Circle for Lateral G vs Longitudinal G)
 */
@Composable
fun GForceFrictionCircle(
    points: List<TelemetryPoint>,
    modifier: Modifier = Modifier
) {
    val current = points.lastOrNull()
    val latG = current?.lateralG ?: 0f
    val longG = current?.longitudinalG ?: 0f

    Card(
        modifier = modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("G-G FRICTION CIRCLE", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = String.format("Lat: %.2fG | Long: %.2fG", latG, longG),
                    color = ApexCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(modifier = Modifier.size(160.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxRadius = size.width / 2f - 10f

                // Concentric circles at 0.5G, 1.0G, 1.5G
                val gridColor = Color(0xFF222838)
                drawCircle(gridColor, radius = maxRadius * 0.33f, center = Offset(cx, cy), style = Stroke(width = 1f))
                drawCircle(gridColor, radius = maxRadius * 0.66f, center = Offset(cx, cy), style = Stroke(width = 1f))
                drawCircle(gridColor, radius = maxRadius, center = Offset(cx, cy), style = Stroke(width = 1.5f))

                // Crosshairs
                drawLine(gridColor, Offset(cx, 10f), Offset(cx, size.height - 10f), strokeWidth = 1f)
                drawLine(gridColor, Offset(10f, cy), Offset(size.width - 10f, cy), strokeWidth = 1f)

                // Plot historic G-force trail points
                val trailCount = points.size.coerceAtMost(25)
                val startIdx = points.size - trailCount
                for (i in startIdx until points.size) {
                    val p = points[i]
                    val px = cx + (p.lateralG / 1.8f) * maxRadius
                    val py = cy - (p.longitudinalG / 1.8f) * maxRadius
                    val alpha = (i - startIdx + 1).toFloat() / trailCount.toFloat()
                    drawCircle(ApexOrange.copy(alpha = alpha * 0.5f), radius = 2.5f, center = Offset(px, py))
                }

                // Current G-force marker
                val curX = cx + (latG / 1.8f).coerceIn(-1f, 1f) * maxRadius
                val curY = cy - (longG / 1.8f).coerceIn(-1f, 1f) * maxRadius
                drawCircle(ApexCyan, radius = 6f, center = Offset(curX, curY))
                drawCircle(Color.White, radius = 2.5f, center = Offset(curX, curY))
            }
        }
    }
}
