package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.AudioSettings
import com.example.ui.SettingsViewModel
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
 * UI overlay providing interactive toggles and volume sliders for Master Volume
 * and Sound Effects, backed by DataStore persistence via [SettingsViewModel].
 */
@Composable
fun SettingsOverlay(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = { viewModel.hideOverlay() }
) {
    val settings by viewModel.audioSettings.collectAsState()

    SettingsOverlayContent(
        settings = settings,
        onMasterMuteToggle = { viewModel.toggleMasterMute() },
        onMasterVolumeChange = { viewModel.setMasterVolume(it) },
        onSoundEffectsToggle = { viewModel.toggleSoundEffects() },
        onSoundEffectsVolumeChange = { viewModel.setSoundEffectsVolume(it) },
        onEngineSoundToggle = { viewModel.setEngineSoundEnabled(!settings.engineSoundEnabled) },
        onTestAudio = { viewModel.testAudio() },
        onResetDefaults = { viewModel.resetToDefaults() },
        onDismiss = onDismiss,
        modifier = modifier
    )
}

/**
 * Pure stateless Composable rendering the settings overlay dialog.
 */
@Composable
fun SettingsOverlayContent(
    settings: AudioSettings,
    onMasterMuteToggle: () -> Unit,
    onMasterVolumeChange: (Float) -> Unit,
    onSoundEffectsToggle: () -> Unit,
    onSoundEffectsVolumeChange: (Float) -> Unit,
    onEngineSoundToggle: () -> Unit,
    onTestAudio: () -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CarbonBlack.copy(alpha = 0.78f),
                        CarbonBlack.copy(alpha = 0.94f)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Consume outside taps
            )
            .testTag("settings_overlay_container"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    2.dp,
                    Brush.verticalGradient(listOf(ApexCyan, SurfaceBorder)),
                    RoundedCornerShape(20.dp)
                )
                .testTag("settings_overlay_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header Bar: Title and Close button
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ApexCyan.copy(alpha = 0.15f))
                                .border(1.dp, ApexCyan, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = ApexCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "AUDIO SETTINGS",
                                color = TextWhite,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "PERSISTED TO DATASTORE",
                                color = ApexCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close settings overlay",
                            tint = TextMuted
                        )
                    }
                }

                // 2. Master Volume Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (settings.isMasterMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Master Volume Icon",
                                    tint = if (settings.isMasterMuted) ApexOrange else ApexCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "MASTER VOLUME",
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (settings.isMasterMuted) "AUDIO MUTED" else "ACTIVE",
                                        color = if (settings.isMasterMuted) ApexOrange else ApexGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Master Mute Switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (settings.isMasterMuted) "Muted" else "Enabled",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Switch(
                                    checked = !settings.isMasterMuted,
                                    onCheckedChange = { onMasterMuteToggle() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CarbonBlack,
                                        checkedTrackColor = ApexCyan,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkSurface
                                    ),
                                    modifier = Modifier
                                        .testTag("master_mute_switch")
                                        .testTag("master_volume_toggle")
                                )
                            }
                        }

                        // Master Volume Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Output Level",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (settings.isMasterMuted) "0%" else "${(settings.masterVolume * 100).toInt()}%",
                                    color = if (settings.isMasterMuted) TextMuted else ApexCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("master_volume_label")
                                )
                            }

                            Slider(
                                value = if (settings.isMasterMuted) 0f else settings.masterVolume,
                                onValueChange = onMasterVolumeChange,
                                enabled = !settings.isMasterMuted,
                                colors = SliderDefaults.colors(
                                    thumbColor = ApexCyan,
                                    activeTrackColor = ApexCyan,
                                    inactiveTrackColor = Color(0xFF222834)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("master_volume_slider")
                            )
                        }
                    }
                }

                // 3. Sound Effects (SFX) Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Sound Effects Icon",
                                    tint = if (settings.soundEffectsEnabled) ApexGold else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "SOUND EFFECTS (SFX)",
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tire screeches, collisions, shifts",
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            // SFX Toggle Switch
                            Switch(
                                checked = settings.soundEffectsEnabled,
                                onCheckedChange = { onSoundEffectsToggle() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CarbonBlack,
                                    checkedTrackColor = ApexGold,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkSurface
                                ),
                                modifier = Modifier
                                    .testTag("sfx_toggle_switch")
                                    .testTag("sound_effects_toggle")
                            )
                        }

                        // SFX Volume Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Effects Volume",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (!settings.soundEffectsEnabled) "OFF" else "${(settings.soundEffectsVolume * 100).toInt()}%",
                                    color = if (!settings.soundEffectsEnabled) TextMuted else ApexGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("sfx_volume_label")
                                )
                            }

                            Slider(
                                value = if (!settings.soundEffectsEnabled) 0f else settings.soundEffectsVolume,
                                onValueChange = onSoundEffectsVolumeChange,
                                enabled = settings.soundEffectsEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = ApexGold,
                                    activeTrackColor = ApexGold,
                                    inactiveTrackColor = Color(0xFF222834)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sfx_volume_slider")
                            )
                        }
                    }
                }

                // 4. Action Buttons: Preview Sound & Reset to Defaults
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTestAudio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexCyan,
                            contentColor = CarbonBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("test_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CarbonBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TEST AUDIO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onResetDefaults,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("reset_audio_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RESET DEFAULTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
