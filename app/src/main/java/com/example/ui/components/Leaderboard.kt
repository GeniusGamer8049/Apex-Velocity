package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverProfileRepository
import com.example.data.GameStatisticsRepository
import com.example.data.room.DriverProfile
import com.example.data.room.GameStatistics
import com.example.model.TrackRegistry
import com.example.model.VehicleRegistry
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexCyanGlow
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable Leaderboard Composable that fetches top race times from the Room database,
 * displays them in a high-performance scrollable list, and prominently highlights
 * the user's personal best record.
 */
@Composable
fun Leaderboard(
    modifier: Modifier = Modifier,
    initialTrackId: String? = null,
    repository: GameStatisticsRepository = GameStatisticsRepository.getInstance(LocalContext.current),
    driverProfileRepository: DriverProfileRepository = DriverProfileRepository.getInstance(LocalContext.current),
    onSelectTrack: ((String) -> Unit)? = null,
    onRecordClick: ((GameStatistics) -> Unit)? = null,
    onStartRaceClick: (() -> Unit)? = null
) {
    var selectedTrackId by remember { mutableStateOf(initialTrackId) }

    // Collect top race times reactively from Room database
    val topTimes by remember(selectedTrackId) {
        if (selectedTrackId != null && selectedTrackId!!.isNotBlank()) {
            repository.getTopRaceTimesForTrackFlow(selectedTrackId!!)
        } else {
            repository.topRaceTimesFlow
        }
    }.collectAsState(initial = emptyList())

    // Collect current user's driver profile from Room
    val driverProfile by driverProfileRepository.driverProfileFlow.collectAsState(initial = null)

    // Calculate Personal Best: fastest lap time in the collection
    val personalBestRecord = remember(topTimes) {
        topTimes.filter { it.bestLapTimeMs > 0 }.minByOrNull { it.bestLapTimeMs }
    }
    val personalBestTimeMs = personalBestRecord?.bestLapTimeMs ?: 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .testTag("leaderboard_container")
    ) {
        // 1. Header Bar: Title, Room DB Badge, and Track Filter Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(ApexGold, ApexOrange))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Leaderboard Trophy",
                            tint = CarbonBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "CHAMPIONSHIP LEADERBOARD",
                            color = TextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "ROOM DATABASE FASTEST LAPS",
                            color = ApexCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Driver username badge
                if (driverProfile != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("leaderboard_driver_badge")
                    ) {
                        Text(
                            text = driverProfile!!.username.uppercase(),
                            color = ApexCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Track Selector Chips
            TrackFilterRow(
                selectedTrackId = selectedTrackId,
                onSelectTrack = {
                    selectedTrackId = it
                    if (it != null) onSelectTrack?.invoke(it)
                }
            )
        }

        // 2. Personal Best Spotlight Banner
        if (personalBestRecord != null) {
            PersonalBestCard(
                record = personalBestRecord,
                driverName = driverProfile?.username ?: "DRIVER",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("personal_best_card")
            )
        }

        // 3. Scrollable List of Top Race Times
        if (topTimes.isEmpty()) {
            EmptyLeaderboardCard(
                onStartRaceClick = onStartRaceClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .testTag("leaderboard_empty_state")
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("leaderboard_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section summary header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${topTimes.size} RECORDED RACE TIMES",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "ORDERED BY BEST LAP",
                            color = ApexCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                itemsIndexed(
                    items = topTimes,
                    key = { _, item -> item.recordId }
                ) { index, item ->
                    val isPersonalBest = item.bestLapTimeMs == personalBestTimeMs

                    LeaderboardRowItem(
                        rank = index + 1,
                        item = item,
                        isPersonalBest = isPersonalBest,
                        fastestTimeMs = topTimes.first().bestLapTimeMs,
                        driverName = driverProfile?.username ?: "DRIVER",
                        onClick = { onRecordClick?.invoke(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leaderboard_item_$index")
                    )
                }
            }
        }
    }
}

/**
 * Track filter horizontal chips.
 */
@Composable
private fun TrackFilterRow(
    selectedTrackId: String?,
    onSelectTrack: (String?) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All Tracks" chip
        TrackChip(
            label = "ALL TRACKS",
            isSelected = selectedTrackId == null,
            onClick = { onSelectTrack(null) },
            testTag = "track_chip_all"
        )

        TrackRegistry.allTracks.forEach { track ->
            val isSelected = track.id == selectedTrackId
            TrackChip(
                label = track.name.uppercase(),
                isSelected = isSelected,
                onClick = { onSelectTrack(track.id) },
                testTag = "track_chip_${track.id}"
            )
        }
    }
}

@Composable
private fun TrackChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ApexCyan else DarkSurface)
            .border(
                1.dp,
                if (isSelected) ApexCyan else SurfaceBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) CarbonBlack else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Featured Spotlight Card highlighting the user's all-time fastest lap time.
 */
@Composable
fun PersonalBestCard(
    record: GameStatistics,
    driverName: String,
    modifier: Modifier = Modifier
) {
    val track = TrackRegistry.getTrackById(record.trackId)
    val vehicle = VehicleRegistry.getVehicleById(record.vehicleId)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(ApexGold, ApexCyan)),
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Golden Trophy Medallion
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ApexGold, Color(0xFFC79100))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Personal Best Badge",
                        tint = CarbonBlack,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ApexGold.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("personal_best_badge")
                        ) {
                            Text(
                                text = "PERSONAL BEST",
                                color = ApexGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = driverName.uppercase(),
                            color = TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Track & Vehicle info
                    Text(
                        text = "${track.name} • ${vehicle.name}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Top speed & performance
                    if (record.maxSpeedKmh > 0f) {
                        Text(
                            text = "Peak: ${record.maxSpeedKmh.toInt()} km/h ${if (record.isWin) "• VICTORY 🏆" else ""}",
                            color = ApexGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Lap Time Display
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatRaceDuration(record.bestLapTimeMs, includeMillis = true),
                    color = ApexGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.testTag("personal_best_time_text")
                )
                Text(
                    text = "RECORD TIME",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Row representing a recorded race time in the scrollable list.
 * Distinctly styled and highlighted when it matches the user's personal best.
 */
@Composable
fun LeaderboardRowItem(
    rank: Int,
    item: GameStatistics,
    isPersonalBest: Boolean,
    fastestTimeMs: Long,
    driverName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = TrackRegistry.getTrackById(item.trackId)
    val vehicle = VehicleRegistry.getVehicleById(item.vehicleId)

    val rankBadgeColor = when (rank) {
        1 -> ApexGold
        2 -> Color(0xFFCFD8DC) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> TextMuted
    }

    val gapToLeaderMs = item.bestLapTimeMs - fastestTimeMs
    val formattedGap = if (gapToLeaderMs <= 0) "LEADER" else "+${String.format(Locale.US, "%.3fs", gapToLeaderMs / 1000f)}"

    val formattedDate = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(item.timestamp))
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isPersonalBest) 1.5.dp else 1.dp,
                brush = if (isPersonalBest) {
                    Brush.horizontalGradient(listOf(ApexGold, ApexCyan))
                } else {
                    Brush.horizontalGradient(listOf(SurfaceBorder, SurfaceBorder))
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag("leaderboard_row_$rank"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPersonalBest) {
                Color(0xFF1E232B)
            } else {
                DarkSurface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Rank & Driver / Vehicle Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rank Number Pill
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (rank <= 3) rankBadgeColor.copy(alpha = 0.20f) else Color(0xFF1B1F27)
                        )
                        .border(
                            1.dp,
                            if (rank <= 3) rankBadgeColor else Color(0xFF2C3240),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        color = rankBadgeColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = driverName,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Highlight badge for Personal Best
                        if (isPersonalBest) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ApexGold)
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    .testTag("personal_best_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = CarbonBlack,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "BEST",
                                        color = CarbonBlack,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    // Track & Vehicle
                    Text(
                        text = "${track.name} • ${vehicle.name}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    // Date & Win Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedDate,
                            color = Color(0xFF78909C),
                            fontSize = 10.sp
                        )
                        if (item.isWin) {
                            Text(
                                text = "P1 WIN",
                                color = ApexGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Right: Lap Time & Gap
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatRaceDuration(item.bestLapTimeMs, includeMillis = true),
                    color = if (isPersonalBest) ApexGold else TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = formattedGap,
                    color = if (gapToLeaderMs <= 0) ApexGreen else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                if (item.maxSpeedKmh > 0f) {
                    Text(
                        text = "${item.maxSpeedKmh.toInt()} km/h",
                        color = ApexCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Placeholder when no race records are available yet.
 */
@Composable
private fun EmptyLeaderboardCard(
    onStartRaceClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2530)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = ApexCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "NO SAVED RACE TIMES YET",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Text(
                text = "Complete Time Trial hot laps or Career races to set baseline records in the database. Your fastest lap time will be automatically recorded and crowned as your Personal Best!",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (onStartRaceClick != null) {
                Button(
                    onClick = onStartRaceClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexCyan,
                        contentColor = CarbonBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("leaderboard_start_race_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = CarbonBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RACE TO SET RECORD",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
