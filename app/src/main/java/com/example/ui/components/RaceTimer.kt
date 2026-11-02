package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * State holder for [RaceTimer] managing start, stop/pause, resume, split lap recording,
 * and duration tracking with high-precision timestamps.
 */
@Stable
class RaceTimerState(
    initialDurationMs: Long = 0L,
    initialIsRunning: Boolean = false
) {
    var elapsedMillis by mutableLongStateOf(initialDurationMs)
        private set

    var isRunning by mutableStateOf(initialIsRunning)
        private set

    val lapTimes = mutableStateListOf<Long>()

    private var lastTickTimestamp: Long = System.currentTimeMillis()

    fun start() {
        if (!isRunning) {
            lastTickTimestamp = System.currentTimeMillis()
            isRunning = true
        }
    }

    fun stop() {
        if (isRunning) {
            val now = System.currentTimeMillis()
            elapsedMillis += (now - lastTickTimestamp)
            isRunning = false
        }
    }

    fun toggle() {
        if (isRunning) stop() else start()
    }

    fun reset() {
        stop()
        elapsedMillis = 0L
        lapTimes.clear()
        lastTickTimestamp = System.currentTimeMillis()
    }

    fun setTime(millis: Long) {
        elapsedMillis = millis
        lastTickTimestamp = System.currentTimeMillis()
    }

    fun recordLap(): Long {
        val currentLapDuration = if (lapTimes.isEmpty()) {
            elapsedMillis
        } else {
            val previousTotal = lapTimes.sum()
            elapsedMillis - previousTotal
        }
        lapTimes.add(currentLapDuration)
        return currentLapDuration
    }

    fun tick() {
        if (isRunning) {
            val now = System.currentTimeMillis()
            val delta = now - lastTickTimestamp
            if (delta > 0) {
                elapsedMillis += delta
                lastTickTimestamp = now
            }
        }
    }

    val formattedTime: String
        get() = formatRaceDuration(elapsedMillis, includeMillis = true)
}

/**
 * Creates and remembers a [RaceTimerState] across recompositions.
 */
@Composable
fun rememberRaceTimerState(
    initialDurationMs: Long = 0L,
    autoStart: Boolean = false
): RaceTimerState {
    val state = remember {
        RaceTimerState(
            initialDurationMs = initialDurationMs,
            initialIsRunning = autoStart
        )
    }

    LaunchedEffect(state.isRunning) {
        if (state.isRunning) {
            while (isActive && state.isRunning) {
                delay(16) // ~60 FPS precision ticker
                state.tick()
            }
        }
    }

    return state
}

/**
 * Reusable Timer Composable that tracks race duration during active gameplay,
 * featuring digital HUD aesthetics and complete start/stop/reset/lap controls.
 */
@Composable
fun RaceTimer(
    modifier: Modifier = Modifier,
    timerState: RaceTimerState = rememberRaceTimerState(autoStart = true),
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onReset: () -> Unit = {},
    showLapControls: Boolean = true,
    title: String = "RACE DURATION"
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.5.dp,
                if (timerState.isRunning) ApexCyan else SurfaceBorder,
                RoundedCornerShape(16.dp)
            )
            .testTag("race_timer_container"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Live / Stopped status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (timerState.isRunning) ApexCyan else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title.uppercase(),
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                TimerStatusBadge(isRunning = timerState.isRunning)
            }

            // Digital Stopwatch Display Readout
            DigitalTimerDisplay(
                durationMs = timerState.elapsedMillis,
                isRunning = timerState.isRunning,
                modifier = Modifier.fillMaxWidth()
            )

            // Primary Control Buttons (Start / Stop, Reset, Lap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Start / Stop Toggle Button
                Button(
                    onClick = {
                        if (timerState.isRunning) {
                            timerState.stop()
                            onStop()
                        } else {
                            timerState.start()
                            onStart()
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("timer_start_stop_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timerState.isRunning) ApexOrange else ApexCyan,
                        contentColor = CarbonBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (timerState.isRunning) "Stop Timer" else "Start Timer",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (timerState.isRunning) "STOP" else "START",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Lap / Split Button
                if (showLapControls) {
                    Button(
                        onClick = { timerState.recordLap() },
                        enabled = timerState.isRunning,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("timer_lap_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurface,
                            contentColor = ApexGold,
                            disabledContainerColor = DarkSurface.copy(alpha = 0.5f),
                            disabledContentColor = TextMuted
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (timerState.isRunning) ApexGold else SurfaceBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Lap Split",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LAP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Reset Button
                IconButton(
                    onClick = {
                        timerState.reset()
                        onReset()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .testTag("timer_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Recorded Lap Splits (if any)
            if (timerState.lapTimes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "RECORDED LAPS (${timerState.lapTimes.size})",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    timerState.lapTimes.takeLast(3).forEachIndexed { index, lapMs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lap ${timerState.lapTimes.size - 2 + index}",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = formatRaceDuration(lapMs, includeMillis = true),
                                color = ApexCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact HUD Pill Composable specifically designed to sit unobtrusively inside [RaceHud]
 * during active racing gameplay with an interactive start/stop button.
 */
@Composable
fun RaceTimerHud(
    elapsedMillis: Long,
    isRunning: Boolean,
    onToggleRunning: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "RACE TIME"
) {
    val borderColor by animateColorAsState(
        targetValue = if (isRunning) ApexCyan else SurfaceBorder,
        animationSpec = tween(300),
        label = "hud_timer_border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CarbonBlack.copy(alpha = 0.88f))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("race_timer_hud")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pulsing status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) ApexGreen else ApexOrange)
            )

            // Duration Display
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = formatRaceDuration(elapsedMillis, includeMillis = true),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("race_timer_display")
                )
            }

            // Compact Start / Stop Control
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) ApexOrange.copy(alpha = 0.2f) else ApexCyan.copy(alpha = 0.2f))
                    .border(
                        1.dp,
                        if (isRunning) ApexOrange else ApexCyan,
                        CircleShape
                    )
                    .clickable { onToggleRunning() }
                    .testTag("timer_hud_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause Timer" else "Resume Timer",
                    tint = if (isRunning) ApexOrange else ApexCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Large glowing digital readout component for the race timer.
 */
@Composable
fun DigitalTimerDisplay(
    durationMs: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = durationMs % 1000

    val minStr = String.format(Locale.US, "%02d", minutes)
    val secStr = String.format(Locale.US, "%02d", seconds)
    val msStr = String.format(Locale.US, "%03d", millis)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            // Minutes : Seconds
            Text(
                text = "$minStr:$secStr",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = TextWhite,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            // Milliseconds decimal
            Text(
                text = ".$msStr",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = ApexCyan,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun TimerStatusBadge(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isRunning) ApexGreen.copy(alpha = 0.15f) else ApexOrange.copy(alpha = 0.15f)
            )
            .border(
                1.dp,
                if (isRunning) ApexGreen.copy(alpha = 0.4f) else ApexOrange.copy(alpha = 0.4f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    if (isRunning) ApexGreen.copy(alpha = alpha) else ApexOrange
                )
        )
        Text(
            text = if (isRunning) "ACTIVE" else "STOPPED",
            color = if (isRunning) ApexGreen else ApexOrange,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Formats a duration in milliseconds into MM:SS.mmm or HH:MM:SS.mmm format.
 */
fun formatRaceDuration(ms: Long, includeMillis: Boolean = true): String {
    if (ms <= 0L) return if (includeMillis) "00:00.000" else "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val millis = ms % 1000

    return if (hours > 0) {
        if (includeMillis) {
            String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
        } else {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
    } else {
        if (includeMillis) {
            String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
