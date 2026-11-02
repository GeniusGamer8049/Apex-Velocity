package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DataRepository
import com.example.data.GameStatisticsRepository
import com.example.data.room.GameStatistics
import com.example.model.TrackRegistry
import com.example.ui.MainViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter(val label: String) {
    ALL("All Races"),
    WINS("Wins (1st)"),
    PODIUMS("Podiums (Top 3)"),
    CLEAN("Clean Races")
}

/**
 * Jetpack Compose screen that retrieves and displays the user's race history from the
 * [GameStatistics] entity using the repository, showing race dates and outcomes in a LazyColumn.
 */
@Composable
fun RaceHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {},
    onGoToRace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    RaceHistoryScreen(
        repository = viewModel.gameStatisticsRepository,
        onBack = onBack,
        onGoToRace = onGoToRace,
        modifier = modifier
    )
}

@Composable
fun RaceHistoryScreen(
    dataRepository: DataRepository,
    onBack: () -> Unit = {},
    onGoToRace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    RaceHistoryScreen(
        repository = dataRepository.gameStatisticsRepository,
        onBack = onBack,
        onGoToRace = onGoToRace,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceHistoryScreen(
    onBack: () -> Unit = {},
    onGoToRace: () -> Unit = {},
    modifier: Modifier = Modifier,
    repository: GameStatisticsRepository = GameStatisticsRepository.getInstance(LocalContext.current)
) {
    BackHandler { onBack() }

    val allStats by repository.allGameStatisticsFlow.collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }

    val filteredStats = remember(allStats, selectedFilter) {
        when (selectedFilter) {
            HistoryFilter.ALL -> allStats
            HistoryFilter.WINS -> allStats.filter { it.isWin || it.position == 1 }
            HistoryFilter.PODIUMS -> allStats.filter { it.isPodium || it.position in 1..3 }
            HistoryFilter.CLEAN -> allStats.filter { it.isClean }
        }
    }

    val totalRaces = allStats.size
    val totalWins = allStats.count { it.isWin || it.position == 1 }
    val totalPodiums = allStats.count { it.isPodium || it.position in 1..3 }
    val winRate = if (totalRaces > 0) (totalWins.toFloat() / totalRaces.toFloat()) * 100f else 0f

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("race_history_screen"),
        containerColor = CarbonBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "RACE HISTORY",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp,
                            color = TextWhite,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "Telemetry Records & Outcomes ($totalRaces total)",
                            fontSize = 11.sp,
                            color = ApexCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("race_history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextWhite
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("race_history_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Summary Header Card
            item {
                RaceHistorySummaryHeader(
                    totalRaces = totalRaces,
                    totalWins = totalWins,
                    totalPodiums = totalPodiums,
                    winRate = winRate
                )
            }

            // Filter Chips
            item {
                FilterChipsRow(
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it },
                    totalCount = totalRaces,
                    winsCount = totalWins,
                    podiumsCount = totalPodiums,
                    cleanCount = allStats.count { it.isClean }
                )
            }

            // Empty state if no records exist matching filter
            if (filteredStats.isEmpty()) {
                item {
                    EmptyHistoryCard(
                        filter = selectedFilter,
                        onGoToRace = onGoToRace
                    )
                }
            } else {
                // Section Title with Count
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedFilter.label.uppercase()} (${filteredStats.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Most Recent First",
                            fontSize = 11.sp,
                            color = ApexCyan
                        )
                    }
                }

                // Race History Items
                items(
                    items = filteredStats,
                    key = { it.recordId }
                ) { stat ->
                    RaceHistoryItemCard(
                        stat = stat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("race_history_item_${stat.recordId}")
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RaceHistorySummaryHeader(
    totalRaces: Int,
    totalWins: Int,
    totalPodiums: Int,
    winRate: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("race_history_summary_header"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ApexCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = null,
                        tint = ApexCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Recorded Race Archive",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextWhite
                    )
                    Text(
                        text = "Historical telemetry and event outcomes stored in database",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeaderMetricPill(
                    label = "Recorded",
                    value = "$totalRaces",
                    icon = Icons.Default.Flag,
                    tint = ApexCyan,
                    modifier = Modifier.weight(1f)
                )
                HeaderMetricPill(
                    label = "Victories",
                    value = "$totalWins",
                    icon = Icons.Default.EmojiEvents,
                    tint = ApexGold,
                    modifier = Modifier.weight(1f)
                )
                HeaderMetricPill(
                    label = "Podiums",
                    value = "$totalPodiums",
                    icon = Icons.Default.MilitaryTech,
                    tint = ApexOrange,
                    modifier = Modifier.weight(1f)
                )
                HeaderMetricPill(
                    label = "Win Rate",
                    value = "${winRate.toInt()}%",
                    icon = Icons.Default.Speed,
                    tint = ApexGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeaderMetricPill(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = TextWhite,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: HistoryFilter,
    onSelectFilter: (HistoryFilter) -> Unit,
    totalCount: Int,
    winsCount: Int,
    podiumsCount: Int,
    cleanCount: Int
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(HistoryFilter.values()) { filter ->
            val count = when (filter) {
                HistoryFilter.ALL -> totalCount
                HistoryFilter.WINS -> winsCount
                HistoryFilter.PODIUMS -> podiumsCount
                HistoryFilter.CLEAN -> cleanCount
            }
            val isSelected = selectedFilter == filter

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) ApexCyan else DarkSurfaceElevated)
                    .border(
                        1.dp,
                        if (isSelected) ApexCyan else SurfaceBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelectFilter(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("filter_${filter.name.lowercase()}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = filter.label,
                        color = if (isSelected) CarbonBlack else TextWhite,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) CarbonBlack.copy(alpha = 0.2f) else DarkSurface
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$count",
                            color = if (isSelected) CarbonBlack else ApexCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RaceHistoryItemCard(
    stat: GameStatistics,
    modifier: Modifier = Modifier
) {
    val isWin = stat.isWin || stat.position == 1
    val isPodium = (stat.isPodium || stat.position in 1..3) && !isWin
    val isClean = stat.isClean

    val accentBorderColor = when {
        isWin -> ApexGold
        isPodium -> ApexOrange
        isClean -> ApexGreen
        else -> SurfaceBorder
    }

    val outcomeBadgeText = when (stat.position) {
        1 -> "VICTORY • 1ST PLACE"
        2 -> "PODIUM • 2ND PLACE"
        3 -> "PODIUM • 3RD PLACE"
        else -> "FINISHED P${stat.position} / ${stat.totalRacers}"
    }

    val outcomeBadgeColor = when (stat.position) {
        1 -> ApexGold
        2, 3 -> ApexOrange
        else -> TextMuted
    }

    val trackDisplayName = if (stat.trackId.isNotEmpty()) {
        try {
            TrackRegistry.getTrackById(stat.trackId).name
        } catch (e: Exception) {
            stat.trackId.replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    } else {
        "Career Championship Race"
    }

    Card(
        modifier = modifier.testTag("race_history_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(
            if (isWin) 1.5.dp else 1.dp,
            accentBorderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Outcome Badge and Formatted Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Outcome Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(outcomeBadgeColor.copy(alpha = 0.15f))
                        .border(1.dp, outcomeBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isWin) Icons.Default.EmojiEvents else Icons.Default.Flag,
                        contentDescription = null,
                        tint = outcomeBadgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = outcomeBadgeText,
                        color = outcomeBadgeColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Race Date & Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = formatRaceDate(stat.timestamp),
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Track & Vehicle Information
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = trackDisplayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextWhite
                )

                if (stat.raceHistory.isNotEmpty() && stat.raceHistory != trackDisplayName) {
                    Text(
                        text = stat.raceHistory,
                        fontSize = 12.sp,
                        color = ApexCyan
                    )
                }

                if (stat.carPerformanceLevels.isNotEmpty()) {
                    Text(
                        text = "Vehicle: ${stat.carPerformanceLevels}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Telemetry & Outcomes Metric Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Best Lap
                Column(horizontalAlignment = Alignment.Start) {
                    Text("BEST LAP", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatLapTime(stat.bestLapTimeMs),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = ApexCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Max Speed
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOP SPEED", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${stat.maxSpeedKmh.toInt()} km/h",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (stat.maxSpeedKmh >= 250f) ApexRed else TextWhite,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                // Distance
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DISTANCE", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (stat.distanceMeters >= 1000f) {
                            String.format(Locale.US, "%.1f km", stat.distanceMeters / 1000f)
                        } else {
                            "${stat.distanceMeters.toInt()} m"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                // Drift Distance
                if (stat.driftMeters > 0f) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("DRIFT", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${stat.driftMeters.toInt()} m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexPurple
                        )
                    }
                }
            }

            // Tags row: Clean race, Rewards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stat.isClean) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ApexGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ApexGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "CLEAN RACE",
                                color = ApexGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Rewards
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stat.earningsCredits > 0) {
                        Text(
                            text = "+$ ${String.format(Locale.US, "%,d", stat.earningsCredits)}",
                            color = ApexGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (stat.reputationEarned > 0) {
                        Text(
                            text = "+${stat.reputationEarned} REP",
                            color = ApexGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(
    filter: HistoryFilter,
    onGoToRace: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_race_history_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ApexCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = ApexCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (filter == HistoryFilter.ALL) "No Races Recorded Yet" else "No Matching Races",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextWhite
                )
                Text(
                    text = if (filter == HistoryFilter.ALL) {
                        "Complete career races, quick events, or time trials to log your race history and outcomes."
                    } else {
                        "No events found matching '${filter.label}'. Try selecting a different filter."
                    },
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Button(
                onClick = onGoToRace,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("enter_race_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ApexCyan,
                    contentColor = CarbonBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ENTER A RACE",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

private fun formatRaceDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Recent Event"
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US)
    return sdf.format(date)
}

private fun formatLapTime(timeMs: Long): String {
    if (timeMs <= 0L) return "--:--.---"
    val minutes = timeMs / 60000
    val seconds = (timeMs % 60000) / 1000
    val millis = timeMs % 1000
    return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
}
