package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerProfile
import com.example.data.room.TimeTrialRecord
import com.example.data.room.LocalTrackRecord
import com.example.model.GhostLap
import com.example.model.Track
import com.example.model.TrackRegistry
import com.example.model.WeatherType
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun TimeTrialScreen(
    profile: PlayerProfile,
    ghostLapProvider: (String) -> GhostLap?,
    onStartTimeTrial: (track: Track, weather: WeatherType, enableGhost: Boolean) -> Unit,
    onViewLeaderboard: ((trackId: String) -> Unit)? = null,
    onOpenReplayViewer: (trackId: String) -> Unit = {},
    timeTrialRecordsProvider: ((String) -> Flow<List<TimeTrialRecord>>)? = null,
    trackRecordProvider: ((String) -> Flow<LocalTrackRecord?>)? = null
) {
    var selectedTrackId by remember { mutableStateOf("monaco_riviera") }
    var selectedWeather by remember { mutableStateOf(WeatherType.SUNNY_CLEAR) }
    var enableGhost by remember { mutableStateOf(true) }

    val timeTrialRecordsFlow = remember(selectedTrackId, timeTrialRecordsProvider) {
        timeTrialRecordsProvider?.invoke(selectedTrackId) ?: flowOf(emptyList())
    }
    val timeTrialRecords by timeTrialRecordsFlow.collectAsState(initial = emptyList())

    val trackRecordFlow = remember(selectedTrackId, trackRecordProvider) {
        trackRecordProvider?.invoke(selectedTrackId) ?: flowOf(null)
    }
    val localTrackRecord by trackRecordFlow.collectAsState(initial = null)

    val track = TrackRegistry.getTrackById(selectedTrackId)
    val bestLapTime = profile.bestLapTimes[selectedTrackId]
    val ghostLap = ghostLapProvider(selectedTrackId)
    val currentVehicle = profile.vehicles.find { it.id == profile.selectedVehicleId } ?: profile.vehicles.first()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        item {
            Column {
                Text(
                    text = "TIME TRIAL & GHOST RACER",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Push your vehicle to the absolute limit against your personal best ghost",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Track Selector Cards
        item {
            Text(
                text = "SELECT CIRCUIT",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(TrackRegistry.allTracks) { t ->
                    val isSelected = t.id == selectedTrackId
                    val pb = profile.bestLapTimes[t.id]

                    Card(
                        modifier = Modifier
                            .size(width = 200.dp, height = 135.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ApexCyan else SurfaceBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTrackId = t.id },
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) DarkSurfaceElevated else DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = t.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            Text(text = t.location, color = ApexCyan, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "${t.lengthMeters}m • ${t.turnsCount} Turns", color = TextMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "PB:", color = TextMuted, fontSize = 10.sp)
                                Text(
                                    text = if (pb != null) formatLapTime(pb) else "--:--.---",
                                    color = if (pb != null) ApexGold else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Track & Ghost Replay Status Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = track.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = track.description, color = TextMuted, fontSize = 12.sp)
                        }
                    }

                    // Ghost Car Toggle & Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(16.dp))
                                Text("GHOST CAR REPLAY", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            if (ghostLap != null) {
                                Text(
                                    text = "Active: ${ghostLap.carName} (${formatLapTime(ghostLap.lapTimeMs)})",
                                    color = ApexGreen,
                                    fontSize = 11.sp
                                )
                            } else {
                                Text(
                                    text = "No ghost recorded yet. Set a clean lap to record!",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = enableGhost && ghostLap != null,
                            onCheckedChange = { enableGhost = it },
                            enabled = ghostLap != null,
                            colors = SwitchDefaults.colors(checkedThumbColor = CarbonBlack, checkedTrackColor = ApexCyan)
                        )
                    }

                    // Button to launch the 2D ReplayViewer Visualizer
                    Button(
                        onClick = { onOpenReplayViewer(selectedTrackId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_replay_viewer_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = ApexCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VISUALIZE REPLAY & GHOST PATH", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Selected Vehicle Info Pill
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(24.dp))
                        Column {
                            Text(text = currentVehicle.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "${currentVehicle.effectiveBhp} BHP • ${currentVehicle.effectiveTopSpeedKmh} km/h • ${currentVehicle.upgrades.tireCompound.displayName}", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Player Local Track Record & Best Lap Times Progression Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ApexCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .testTag("local_track_record_card"),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(18.dp))
                            Text(
                                text = "LOCAL TRACK RECORD & PROGRESS",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (localTrackRecord != null && localTrackRecord!!.isCleanLap) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ApexGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("CLEAN RECORD", color = ApexGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (localTrackRecord != null) {
                        val record = localTrackRecord!!
                        // Best Lap and Improvement Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CIRCUIT BEST LAP", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = record.formattedBestLapTime,
                                    color = ApexGold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Driven in: ${record.bestLapVehicleName}",
                                    color = TextWhite,
                                    fontSize = 11.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("PROGRESSION DELTA", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = record.formattedImprovement,
                                    color = if (record.improvementDeltaMs != null && record.improvementDeltaMs!! > 0) ApexGreen else ApexCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (record.previousBestLapTimeMs != null) {
                                    Text(
                                        text = "Prev: ${record.formattedPreviousBestLapTime}",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Grid of Metrics: Top Speed, Laps Completed, Win Rate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurface)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("TOP SPEED", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${record.topSpeedKmh.toInt()} KM/H",
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurface)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("LAPS COMPLETED", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${record.totalLapsCompleted} LAPS",
                                        color = ApexCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurface)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("TRACK PODIUMS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${record.totalPodiums} (${record.winRatePercent.toInt()}%)",
                                        color = ApexGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No local track record yet for this circuit. Complete a lap to track your progress!",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Top Time Trial Records Card (LeaderboardManager Room records)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .testTag("time_trial_leaderboard_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ApexGold, modifier = Modifier.size(18.dp))
                            Text(
                                text = "TOP TIME TRIAL RECORDS",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = track.name,
                            color = ApexCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (timeTrialRecords.isEmpty()) {
                        Text(
                            text = "No recorded lap times yet for this circuit. Be the first to set a benchmark record!",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        val leaderTime = timeTrialRecords.first().completionTimeMs
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            timeTrialRecords.take(5).forEachIndexed { index, record ->
                                val isLeader = index == 0
                                val rankColor = when (index) {
                                    0 -> ApexGold
                                    1 -> Color(0xFFCFD8DC)
                                    2 -> Color(0xFFBCAAA4)
                                    else -> TextMuted
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (record.isPlayer) ApexCyan.copy(alpha = 0.12f) else DarkSurface)
                                        .border(
                                            1.dp,
                                            if (record.isPlayer) ApexCyan.copy(alpha = 0.5f) else SurfaceBorder.copy(alpha = 0.4f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#${record.rank}",
                                            color = rankColor,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.width(26.dp)
                                        )
                                        Column {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = record.playerName,
                                                    color = if (record.isPlayer) ApexCyan else TextWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                if (record.isPlayer) {
                                                    Text(
                                                        text = "(YOU)",
                                                        color = ApexGreen,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                            Text(
                                                text = record.vehicleUsed,
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = record.formattedCompletionTime,
                                            color = if (isLeader) ApexGold else TextWhite,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = record.formattedGap(leaderTime),
                                            color = if (isLeader) ApexGreen else TextMuted,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Weather Condition Override (Test any weather & track condition!)
        item {
            Text(
                text = "TRACK WEATHER CONDITION",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(WeatherType.values()) { weather ->
                    val isSelected = weather == selectedWeather
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ApexCyan else DarkSurface)
                            .border(1.dp, if (isSelected) ApexCyan else SurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedWeather = weather }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${weather.iconEmoji} ${weather.displayName}",
                            color = if (isSelected) CarbonBlack else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Launch Time Trial & Leaderboard Buttons
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onViewLeaderboard != null) {
                    OutlinedButton(
                        onClick = { onViewLeaderboard(selectedTrackId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("time_trial_view_leaderboard_button"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ApexCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(18.dp))
                            Text("VIEW GLOBAL LEADERBOARD", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }

                Button(
                    onClick = { onStartTimeTrial(track, selectedWeather, enableGhost && ghostLap != null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_time_trial_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ApexCyan, contentColor = CarbonBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text("START TIME TRIAL", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
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
