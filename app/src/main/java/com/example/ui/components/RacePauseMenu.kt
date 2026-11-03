package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.ControlScheme
import com.example.model.TireCompound
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
 * High-performance motorsport pause menu composable surfacing over the active race screen.
 * Provides clear primary options to:
 * - 'Resume' (resume active race gameplay)
 * - 'Restart Race' (reset session, starting grid, and countdown)
 * - 'Quit to Main Menu' (safely terminate race session and return to menu)
 *
 * Surfaced when the user clicks the in-game pause button, timer HUD toggle,
 * or presses the device back button during gameplay.
 */
@Composable
fun RacePauseMenu(
    modifier: Modifier = Modifier,
    onResume: () -> Unit,
    onRestartRace: () -> Unit,
    onQuitToMainMenu: () -> Unit,
    currentCompound: TireCompound? = null,
    trackWetness: Float = 0f,
    isMuted: Boolean = false,
    controlScheme: ControlScheme? = null,
    onPitStopTireSwap: ((TireCompound) -> Unit)? = null,
    onToggleAudio: (() -> Unit)? = null,
    onToggleControls: (() -> Unit)? = null,
    onOpenTelemetry: (() -> Unit)? = null,
    onOpenAudioSettings: (() -> Unit)? = null,
    trackName: String = "",
    raceDurationMs: Long = 0L
) {
    // Intercept hardware/gesture back press while paused to resume race
    BackHandler(onBack = onResume)

    val infiniteTransition = rememberInfiniteTransition(label = "PausePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PauseBadgePulse"
    )

    // Full-screen scrim overlay
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CarbonBlack.copy(alpha = 0.75f),
                        CarbonBlack.copy(alpha = 0.92f)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Consume taps outside the card to prevent ghost touches on track
            )
            .testTag("race_pause_menu_container"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Brush.verticalGradient(listOf(ApexCyan, SurfaceBorder)), RoundedCornerShape(20.dp))
                .testTag("race_pause_menu_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Header with Animated PAUSED Badge & Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(ApexOrange.copy(alpha = pulseAlpha))
                        )
                        Column {
                            Text(
                                text = "RACE PAUSED",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                            if (trackName.isNotBlank() || raceDurationMs > 0L) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (trackName.isNotBlank()) {
                                        Text(
                                            text = trackName.uppercase(),
                                            color = TextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (raceDurationMs > 0L) {
                                        Text(
                                            text = "•  ${formatRaceDuration(raceDurationMs, includeMillis = false)}",
                                            color = ApexCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onResume,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pause_menu_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Resume and close pause menu",
                            tint = TextMuted
                        )
                    }
                }

                // 2. Quick Pit Stop: Tire Swap (if configured)
                if (currentCompound != null && onPitStopTireSwap != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ApexCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsMotorsports,
                                        contentDescription = null,
                                        tint = ApexCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "PIT STOP TIRE COMPOUND",
                                        color = ApexCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Text(
                                    text = "Track Wetness: ${(trackWetness * 100).toInt()}%",
                                    color = if (trackWetness > 0.4f) ApexOrange else Color(0xFF29B6F6),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TireCompound.values().forEach { compound ->
                                    val isSelected = compound == currentCompound
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) ApexCyan else DarkSurface)
                                            .border(
                                                1.dp,
                                                if (isSelected) ApexCyan else SurfaceBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onPitStopTireSwap(compound) }
                                            .testTag("tire_swap_${compound.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (compound) {
                                                TireCompound.SPORT_RADIAL -> "Sport"
                                                TireCompound.SOFT_SLICK -> "Slick"
                                                TireCompound.SEMI_SLICK -> "Semi"
                                                TireCompound.WET_INTERMEDIATE -> "Inter"
                                                TireCompound.FULL_WET -> "Wet"
                                            },
                                            color = if (isSelected) CarbonBlack else TextWhite,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Audio & Controls Toggles (if configured)
                if (onToggleControls != null || onToggleAudio != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (controlScheme != null && onToggleControls != null) {
                            OutlinedButton(
                                onClick = onToggleControls,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_toggle_controls_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                            ) {
                                Text(
                                    text = "Controls: ${controlScheme.displayName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (onToggleAudio != null) {
                            OutlinedButton(
                                onClick = onToggleAudio,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_toggle_audio_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isMuted) ApexOrange else ApexCyan
                                    )
                                    Text(
                                        text = if (isMuted) "Audio Muted" else "Audio On",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Live Telemetry viewer button (if configured)
                if (onOpenTelemetry != null) {
                    OutlinedButton(
                        onClick = onOpenTelemetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pause_open_telemetry_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(ApexCyan.copy(alpha = 0.7f), SurfaceBorder))
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = ApexCyan
                            )
                            Text(
                                text = "VEHICLE TELEMETRY & METRICS",
                                color = ApexCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Audio & SFX Settings button (if configured)
                if (onOpenAudioSettings != null) {
                    OutlinedButton(
                        onClick = onOpenAudioSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pause_open_audio_settings_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(ApexGold.copy(alpha = 0.7f), SurfaceBorder))
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = ApexGold
                            )
                            Text(
                                text = "AUDIO & SOUND EFFECTS",
                                color = ApexGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 5. REQUIRED PRIMARY BUTTON: RESUME
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pause_menu_resume_button")
                        .testTag("resume_race_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexCyan,
                        contentColor = CarbonBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume Race",
                            modifier = Modifier.size(22.dp),
                            tint = CarbonBlack
                        )
                        Text(
                            text = "RESUME",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // 6. REQUIRED PRIMARY BUTTON: RESTART RACE
                OutlinedButton(
                    onClick = onRestartRace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_menu_restart_button")
                        .testTag("restart_race_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextWhite
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(ApexOrange, SurfaceBorder))
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Race",
                            tint = ApexOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "RESTART RACE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // 7. REQUIRED PRIMARY BUTTON: QUIT TO MAIN MENU
                OutlinedButton(
                    onClick = onQuitToMainMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_menu_quit_button")
                        .testTag("quit_to_main_menu_button")
                        .testTag("exit_race_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ApexRed
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(ApexRed.copy(alpha = 0.8f), SurfaceBorder))
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Quit to Main Menu",
                            tint = ApexRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "QUIT TO MAIN MENU",
                            color = ApexRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }
}
