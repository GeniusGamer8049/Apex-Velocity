package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ControlScheme
import com.example.model.DynamicWeatherState
import com.example.model.GhostFrame
import com.example.model.SurfaceHazardType
import com.example.physics.CarPhysicsState
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.ApexRed
import com.example.model.TireCompound
import com.example.ui.components.DynamicWeatherOverlay
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.abs

@Composable
fun RaceHud(
    modifier: Modifier = Modifier,
    playerState: CarPhysicsState,
    weatherState: DynamicWeatherState,
    totalLaps: Int,
    playerPosition: Int,
    totalRacers: Int,
    isTimeTrial: Boolean,
    ghostFrame: GhostFrame?,
    controlScheme: ControlScheme,
    tireCompound: TireCompound? = null,
    isMuted: Boolean = false,
    onToggleAudio: () -> Unit = {},
    onThrottleChange: (Float) -> Unit,
    onBrakeChange: (Float) -> Unit,
    onSteerChange: (Float) -> Unit,
    onHandbrakeChange: (Boolean) -> Unit,
    onBoostChange: (Boolean) -> Unit,
    onPauseClick: () -> Unit
) {
    var showTelemetry by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().padding(12.dp)) {

        // Top Header Row: Position, Laps, Lap Times, Weather Badge, Pause
        Column(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position badge or Time Trial badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isTimeTrial) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CarbonBlack.copy(alpha = 0.85f))
                                .border(1.5.dp, ApexCyan, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "P $playerPosition / $totalRacers",
                                color = ApexCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CarbonBlack.copy(alpha = 0.85f))
                                .border(1.5.dp, ApexGold, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "TIME TRIAL",
                                color = ApexGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Lap Counter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonBlack.copy(alpha = 0.85f))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isTimeTrial) "LAP ${playerState.currentLap}" else "LAP ${playerState.currentLap}/$totalLaps",
                            color = TextWhite,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Telemetry toggle
                    IconButton(
                        onClick = { showTelemetry = !showTelemetry },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CarbonBlack.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Toggle Telemetry",
                            tint = if (showTelemetry) ApexCyan else TextMuted
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio toggle button
                    IconButton(
                        onClick = onToggleAudio,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CarbonBlack.copy(alpha = 0.85f))
                            .border(1.dp, SurfaceBorder, CircleShape)
                            .testTag("hud_audio_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute Audio" else "Mute Audio",
                            tint = if (isMuted) ApexOrange else ApexCyan
                        )
                    }

                    // Pause button
                    IconButton(
                        onClick = onPauseClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CarbonBlack.copy(alpha = 0.85f))
                            .border(1.dp, SurfaceBorder, CircleShape)
                            .testTag("pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = TextWhite
                        )
                    }
                }
            }

            // Lap Times & Ghost Delta Bar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CarbonBlack.copy(alpha = 0.8f))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "CURRENT", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = formatLapTime(playerState.currentLapTimeMs),
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (playerState.bestLapTimeMs > 0L) {
                    Column {
                        Text(text = "BEST", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatLapTime(playerState.bestLapTimeMs),
                            color = ApexGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Ghost split delta
                if (isTimeTrial && ghostFrame != null) {
                    val deltaMs = playerState.currentLapTimeMs - ghostFrame.timeMs
                    val deltaSec = deltaMs / 1000f
                    val isAhead = deltaSec <= 0f
                    Column {
                        Text(text = "GHOST DELTA", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isAhead) String.format("-%.2fs", abs(deltaSec)) else String.format("+%.2fs", deltaSec),
                            color = if (isAhead) ApexGreen else ApexRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Real-Time Dynamic Weather Status Overlay
            DynamicWeatherOverlay(
                weatherState = weatherState,
                playerState = playerState,
                tireCompound = tireCompound
            )
        }

        // Telemetry Overlay Box
        AnimatedVisibility(
            visible = showTelemetry,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(top = 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonBlack.copy(alpha = 0.90f))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("TELEMETRY", color = ApexCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Speed: ${playerState.speedKmh.toInt()} km/h", color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("RPM: ${playerState.rpm.toInt()}", color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(String.format("Lateral G: %.2f G", playerState.lateralG), color = if (playerState.lateralG > 1.2f) ApexRed else TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(String.format("Slip Angle: %.1f°", playerState.slipAngleDeg), color = if (playerState.isDrifting) ApexOrange else TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(String.format("Tire Grip: %.0f%%", playerState.effectiveTireGrip * 100f), color = if (playerState.effectiveTireGrip < 0.6f) ApexRed else ApexGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "Tires: FL:${playerState.tireTempFL.toInt()}° FR:${playerState.tireTempFR.toInt()}° RL:${playerState.tireTempRL.toInt()}° RR:${playerState.tireTempRR.toInt()}°C",
                        color = if (playerState.tireTempFL > 110f || playerState.tireTempFR > 110f) ApexOrange else ApexGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("Downforce: ${playerState.downforceN.toInt()} N", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    if (playerState.driftScore > 0) {
                        Text("Drift Points: ${playerState.driftScore}", color = ApexGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Center Drift Badge
        if (playerState.isDrifting) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ApexOrange.copy(alpha = 0.85f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "DRIFT +${playerState.driftScore}",
                    color = CarbonBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Bottom Dashboard: Speedometer, Tachometer, Pedals, Steering
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Speedometer & Tachometer Cluster
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CarbonBlack.copy(alpha = 0.88f))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Speed & Gear
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${playerState.speedKmh.toInt()}",
                        color = TextWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = "KM/H", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Current Gear Indicator
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(1.5.dp, if (playerState.rpm > 7000f) ApexRed else ApexCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (playerState.currentGear == -1) "R" else "${playerState.currentGear}",
                        color = if (playerState.rpm > 7000f) ApexRed else ApexCyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // RPM Gauge Bar
                Column(modifier = Modifier.width(130.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "RPM", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${playerState.rpm.toInt()}",
                            color = if (playerState.rpm > 7200f) ApexRed else TextWhite,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    val rpmRatio = (playerState.rpm / 8500f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { rpmRatio },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (rpmRatio > 0.88f) ApexRed else if (rpmRatio > 0.7f) ApexOrange else ApexCyan,
                        trackColor = Color(0xFF202533)
                    )
                }

                // Nitrous / Boost Meter & Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (playerState.isBoosting) ApexCyan else DarkSurface)
                            .border(1.dp, ApexCyan, CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onBoostChange(true)
                                        tryAwaitRelease()
                                        onBoostChange(false)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NOS",
                            color = if (playerState.isBoosting) CarbonBlack else ApexCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    LinearProgressIndicator(
                        progress = { playerState.boostGauge },
                        modifier = Modifier.width(42.dp).height(4.dp).padding(top = 3.dp).clip(RoundedCornerShape(2.dp)),
                        color = ApexCyan,
                        trackColor = Color(0xFF202533)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Driver Controls: Steering Left/Right, Handbrake, Brake, Gas
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left side: Steering controls
                if (controlScheme == ControlScheme.BUTTONS) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Steer Left Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DarkSurface.copy(alpha = 0.85f))
                                .border(1.5.dp, SurfaceBorder, CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            onSteerChange(-1.0f)
                                            tryAwaitRelease()
                                            onSteerChange(0f)
                                        }
                                    )
                                }
                                .testTag("steer_left_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("◀", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }

                        // Steer Right Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DarkSurface.copy(alpha = 0.85f))
                                .border(1.5.dp, SurfaceBorder, CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            onSteerChange(1.0f)
                                            tryAwaitRelease()
                                            onSteerChange(0f)
                                        }
                                    )
                                }
                                .testTag("steer_right_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Virtual Steering Wheel
                    VirtualSteeringWheel(onSteerChange = onSteerChange)
                }

                // Middle: Handbrake (Drift initiation)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (playerState.isDrifting) ApexOrange else DarkSurface.copy(alpha = 0.85f))
                        .border(1.5.dp, ApexOrange, CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onHandbrakeChange(true)
                                    tryAwaitRelease()
                                    onHandbrakeChange(false)
                                }
                            )
                        }
                        .testTag("handbrake_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DRIFT",
                        color = if (playerState.isDrifting) CarbonBlack else ApexOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Right side: Brake and Gas Pedals
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Brake Pedal
                    Box(
                        modifier = Modifier
                            .width(62.dp)
                            .height(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (playerState.isBraking) ApexRed else DarkSurface.copy(alpha = 0.85f))
                            .border(1.5.dp, ApexRed, RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onBrakeChange(1.0f)
                                        tryAwaitRelease()
                                        onBrakeChange(0f)
                                    }
                                )
                            }
                            .testTag("brake_pedal"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BRAKE", color = if (playerState.isBraking) CarbonBlack else ApexRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text("REVERSE", color = if (playerState.isBraking) CarbonBlack else TextMuted, fontSize = 9.sp)
                        }
                    }

                    // Gas / Throttle Pedal
                    Box(
                        modifier = Modifier
                            .width(68.dp)
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF00E676), Color(0xFF00A854))))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onThrottleChange(1.0f)
                                        tryAwaitRelease()
                                        onThrottleChange(0f)
                                    }
                                )
                            }
                            .testTag("gas_pedal"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("GAS", color = CarbonBlack, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun VirtualSteeringWheel(onSteerChange: (Float) -> Unit) {
    var wheelAngle by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(DarkSurface.copy(alpha = 0.85f))
            .border(2.dp, ApexCyan, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        wheelAngle = 0f
                        onSteerChange(0f)
                    },
                    onDragCancel = {
                        wheelAngle = 0f
                        onSteerChange(0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        wheelAngle = (wheelAngle + dragAmount.x * 0.8f).coerceIn(-90f, 90f)
                        onSteerChange(wheelAngle / 90f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(wheelAngle)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.width(70.dp).height(8.dp).background(ApexCyan.copy(alpha = 0.7f), RoundedCornerShape(4.dp)))
            Box(modifier = Modifier.size(24.dp).background(CarbonBlack, CircleShape).border(1.dp, ApexCyan, CircleShape))
        }
    }
}

private fun formatLapTime(ms: Long): String {
    if (ms <= 0L) return "--:--.---"
    val minutes = ms / 60000
    val seconds = (ms % 60000) / 1000
    val millis = ms % 1000
    return String.format("%02d:%02d.%03d", minutes, seconds, millis)
}
