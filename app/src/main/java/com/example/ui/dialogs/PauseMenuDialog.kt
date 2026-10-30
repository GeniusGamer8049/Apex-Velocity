package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ControlScheme
import com.example.model.TireCompound
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun PauseMenuDialog(
    currentCompound: TireCompound,
    trackWetness: Float,
    isMuted: Boolean,
    controlScheme: ControlScheme,
    onResume: () -> Unit,
    onPitStopTireSwap: (TireCompound) -> Unit,
    onToggleAudio: () -> Unit,
    onToggleControls: () -> Unit,
    onOpenTelemetry: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RACE PAUSED",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )

                    IconButton(onClick = onResume) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                    }
                }

                // Quick Pit Stop: Tire Compound Swap
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, ApexCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PIT STOP: TIRE COMPOUND", color = ApexCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Wetness: ${(trackWetness * 100).toInt()}%",
                                color = if (trackWetness > 0.4f) ApexOrange else Color(0xFF29B6F6),
                                fontSize = 11.sp,
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
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) ApexCyan else DarkSurface)
                                        .border(1.dp, if (isSelected) ApexCyan else SurfaceBorder, RoundedCornerShape(6.dp))
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { onPitStopTireSwap(compound) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = if (isSelected) CarbonBlack else TextWhite
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = when (compound) {
                                                TireCompound.SPORT_RADIAL -> "Sport"
                                                TireCompound.SOFT_SLICK -> "Slick"
                                                TireCompound.SEMI_SLICK -> "Semi"
                                                TireCompound.WET_INTERMEDIATE -> "Inter"
                                                TireCompound.FULL_WET -> "Wet"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Controls and Audio Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleControls,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(controlScheme.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onToggleAudio,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(if (isMuted) "Muted" else "Audio On", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Resume, Restart, Exit
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_race_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ApexCyan, contentColor = CarbonBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("RESUME RACE", fontWeight = FontWeight.Black)
                    }
                }

                OutlinedButton(
                    onClick = onOpenTelemetry,
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("pause_open_telemetry_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = ApexCyan)
                        Text("VEHICLE TELEMETRY & METRICS", color = ApexCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("RESTART", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.weight(1f).testTag("exit_race_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("EXIT TO MENU", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
