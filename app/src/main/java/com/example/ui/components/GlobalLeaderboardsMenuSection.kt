package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GlobalLeaderboardsRepository
import com.example.data.room.GlobalLeaderboards
import com.example.model.TrackRegistry
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

/**
 * Main menu UI component displaying circuit rankings from the [GlobalLeaderboards] Room table.
 * Allows browsing top lap times and world records across circuits and highlights the player's personal best.
 */
@Composable
fun GlobalLeaderboardsMenuSection(
    onViewFullLeaderboard: (String) -> Unit,
    modifier: Modifier = Modifier,
    repository: GlobalLeaderboardsRepository = GlobalLeaderboardsRepository.getInstance(LocalContext.current)
) {
    val tracks = TrackRegistry.allTracks
    var selectedTrackId by remember { mutableStateOf(tracks.first().id) }
    val currentTrack = tracks.find { it.id == selectedTrackId } ?: tracks.first()

    val topRankings by repository.getTopRankingsForTrackFlow(selectedTrackId, limit = 5)
        .collectAsState(initial = emptyList())
    val playerEntry by repository.getPlayerEntryForTrackFlow(selectedTrackId)
        .collectAsState(initial = null)
    val worldRecord by repository.getWorldRecordForTrackFlow(selectedTrackId)
        .collectAsState(initial = null)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .testTag("main_menu_global_leaderboards"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Icon, Title, and Subtitle
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.dp, ApexCyan.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "Leaderboard",
                            tint = ApexCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "GLOBAL LEADERBOARDS",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Circuit Top Times & World Records",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Circuit count indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${tracks.size} CIRCUITS",
                        color = ApexCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Circuit Selector Scrollable Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tracks.forEach { track ->
                    val isSelected = track.id == selectedTrackId
                    val borderColor = if (isSelected) ApexCyan else SurfaceBorder
                    val bgColor = if (isSelected) DarkSurface else CarbonBlack.copy(alpha = 0.4f)
                    val textColor = if (isSelected) ApexCyan else TextMuted

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .clickable { selectedTrackId = track.id }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("track_tab_${track.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = track.name,
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Circuit World Record Highlight Pill
            worldRecord?.let { wr ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(DarkSurface, ApexGold.copy(alpha = 0.12f))
                            )
                        )
                        .border(1.dp, ApexGold.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
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
                            Text(text = "👑", fontSize = 16.sp)
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "WORLD RECORD",
                                        color = ApexGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "• ${wr.countryFlag} ${wr.driverName}",
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${wr.vehicleName} (${wr.vehicleCategory})",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Text(
                            text = wr.formattedTime,
                            color = ApexGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Player Personal Best Card
            if (playerEntry != null) {
                val player = playerEntry!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, ApexCyan.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .testTag("player_personal_best_card")
                ) {
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ApexCyan.copy(alpha = 0.2f))
                                    .border(1.dp, ApexCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${player.rank}",
                                    color = ApexCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "YOUR PERSONAL BEST",
                                        color = ApexCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ApexCyan)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "YOU",
                                            color = CarbonBlack,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                Text(
                                    text = "${player.vehicleName} • Gap: ${player.formattedGap}",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Text(
                            text = player.formattedTime,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "No personal best set yet on this circuit. Start a Time Trial to rank!",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Top Rankings Preview Rows (Up to 5)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                topRankings.forEach { entry ->
                    GlobalLeaderboardRow(entry = entry)
                }
            }

            // View Full Circuit Rankings CTA Button
            Button(
                onClick = { onViewFullLeaderboard(selectedTrackId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("view_full_rankings_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurface,
                    contentColor = ApexCyan
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ApexCyan
                    )
                    Text(
                        text = "VIEW FULL CIRCUIT LEADERBOARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ApexCyan
                    )
                }
            }
        }
    }
}

/**
 * Individual leaderboard ranking item row.
 */
@Composable
private fun GlobalLeaderboardRow(entry: GlobalLeaderboards) {
    val isPlayer = entry.isPlayer
    val rankColor = when (entry.rank) {
        1 -> ApexGold
        2 -> Color(0xFFCFD8DC) // Silver
        3 -> Color(0xFFBCAAA4) // Bronze
        else -> TextMuted
    }

    val rowBorder = if (isPlayer) ApexCyan.copy(alpha = 0.7f) else SurfaceBorder
    val rowBg = if (isPlayer) ApexCyan.copy(alpha = 0.08f) else DarkSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("leaderboard_row_${entry.rank}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Rank Pill
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (entry.rank <= 3) rankColor.copy(alpha = 0.2f) else DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${entry.rank}",
                        color = rankColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Driver & Country
                Text(
                    text = entry.countryFlag,
                    fontSize = 13.sp
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = entry.driverName,
                            color = if (isPlayer) ApexCyan else TextWhite,
                            fontWeight = if (isPlayer) FontWeight.Black else FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        if (isPlayer) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ApexCyan)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "YOU",
                                    color = CarbonBlack,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Text(
                        text = entry.vehicleName,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // Lap Time & Gap
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.formattedTime,
                    color = if (entry.rank == 1) ApexGold else TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = entry.formattedGap,
                    color = if (entry.rank == 1) ApexGold else TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
