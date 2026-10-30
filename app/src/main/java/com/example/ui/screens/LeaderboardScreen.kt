package com.example.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.PlayerProfile
import com.example.model.LeaderboardEntry
import com.example.model.LeaderboardTier
import com.example.model.Track
import com.example.model.TrackRegistry
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
import kotlinx.coroutines.launch

enum class LeaderboardFilter(val title: String) {
    GLOBAL("All Drivers"),
    MY_CLASS("My Car Class"),
    RIVALS("Rival Group")
}

@Composable
fun LeaderboardScreen(
    profile: PlayerProfile,
    selectedTrackId: String,
    onSelectTrack: (String) -> Unit,
    getLeaderboard: (String) -> List<LeaderboardEntry>,
    onStartTimeTrial: (Track) -> Unit,
    onUpdateDriverProfile: (name: String, country: String) -> Unit
) {
    var activeFilter by remember { mutableStateOf(LeaderboardFilter.GLOBAL) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val currentTrack = TrackRegistry.getTrackById(selectedTrackId)
    val entries = remember(selectedTrackId, profile.bestLapTimes, profile.driverName, profile.driverCountry) {
        getLeaderboard(selectedTrackId)
    }

    val currentVehicle = profile.vehicles.find { it.id == profile.selectedVehicleId } ?: profile.vehicles.first()
    val playerEntry = entries.find { it.isPlayer }

    // Filtered entries
    val filteredEntries = remember(entries, activeFilter, currentVehicle.category, playerEntry) {
        when (activeFilter) {
            LeaderboardFilter.GLOBAL -> entries
            LeaderboardFilter.MY_CLASS -> {
                val targetCat = playerEntry?.vehicleCategory ?: currentVehicle.category
                entries.filter { it.vehicleCategory.equals(targetCat, ignoreCase = true) }
            }
            LeaderboardFilter.RIVALS -> {
                if (playerEntry != null) {
                    val pIndex = entries.indexOf(playerEntry)
                    val start = maxOf(0, pIndex - 3)
                    val end = minOf(entries.size, pIndex + 4)
                    entries.subList(start, end)
                } else {
                    entries.take(8)
                }
            }
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header with Title & Driver Tag
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = ApexGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "GLOBAL LEADERBOARDS",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "World record lap times & competitive rankings",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                // Driver profile pill with edit button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                        .clickable { showEditProfileDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("edit_driver_tag_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = countryFlagEmoji(profile.driverCountry),
                            fontSize = 14.sp
                        )
                        Text(
                            text = profile.driverName,
                            color = ApexCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // 2. Track Selector Chips (Horizontal Scroll)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SELECT CIRCUIT",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(TrackRegistry.allTracks) { track ->
                        val isSelected = track.id == selectedTrackId
                        val trackPb = profile.bestLapTimes[track.id]
                        Card(
                            modifier = Modifier
                                .width(155.dp)
                                .clickable { onSelectTrack(track.id) }
                                .testTag("track_chip_${track.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DarkSurfaceElevated else DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ApexCyan else SurfaceBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = countryFlagEmoji(track.getCountryCode()),
                                        fontSize = 13.sp
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(ApexCyan.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("ACTIVE", color = ApexCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }

                                Text(
                                    text = track.name,
                                    color = if (isSelected) TextWhite else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${track.getLengthKmString()} km · ${track.turnsCount} turns",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = if (trackPb != null) ApexGreen else TextMuted,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = if (trackPb != null) formatTimeMillis(trackPb) else "--:--.---",
                                        color = if (trackPb != null) ApexGreen else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Player Status / Rank Banner
        item {
            PlayerRankBanner(
                playerEntry = playerEntry,
                currentTrack = currentTrack,
                onStartTimeTrial = { onStartTimeTrial(currentTrack) },
                onScrollToMe = {
                    if (playerEntry != null) {
                        val idx = filteredEntries.indexOf(playerEntry)
                        if (idx >= 0) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(idx + 4)
                            }
                        }
                    }
                }
            )
        }

        // 4. Filter Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LeaderboardFilter.values().forEach { filter ->
                    val selected = activeFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) ApexCyan else Color.Transparent)
                            .clickable { activeFilter = filter }
                            .padding(vertical = 8.dp)
                            .testTag("filter_${filter.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.title,
                            color = if (selected) CarbonBlack else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 5. Top 3 Podium (Only in Global view and when at least 3 entries exist)
        if (activeFilter == LeaderboardFilter.GLOBAL && filteredEntries.size >= 3) {
            item {
                PodiumSection(
                    first = filteredEntries[0],
                    second = filteredEntries[1],
                    third = filteredEntries[2]
                )
            }
        }

        // 6. Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STANDINGS (${filteredEntries.size} DRIVERS)",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "TIME / GAP",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // 7. Rankings List
        items(filteredEntries, key = { it.id }) { entry ->
            LeaderboardRow(entry = entry)
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showEditProfileDialog) {
        EditDriverProfileDialog(
            currentName = profile.driverName,
            currentCountry = profile.driverCountry,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName, newCountry ->
                onUpdateDriverProfile(newName, newCountry)
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
private fun PlayerRankBanner(
    playerEntry: LeaderboardEntry?,
    currentTrack: Track,
    onStartTimeTrial: () -> Unit,
    onScrollToMe: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_leaderboard_banner"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = if (playerEntry != null) ApexCyan else SurfaceBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (playerEntry != null) {
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ApexCyan.copy(alpha = 0.2f))
                                .border(1.dp, ApexCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${playerEntry.rank}",
                                color = ApexCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "YOUR GLOBAL RANK",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(playerEntry.tier.containerColor)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = playerEntry.tier.displayName,
                                        color = playerEntry.tier.primaryColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "${playerEntry.vehicleName} · ${currentTrack.name}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = playerEntry.formattedTime,
                            color = ApexGreen,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (playerEntry.rank == 1) "WORLD RECORD HOLDER" else playerEntry.formattedGap + " to WR",
                            color = if (playerEntry.rank == 1) ApexGold else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onScrollToMe,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Text("FIND MY SPOT", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onStartTimeTrial,
                        modifier = Modifier.weight(1f).height(42.dp).testTag("time_trial_from_leaderboard_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ApexCyan, contentColor = CarbonBlack),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("BEAT MY TIME", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                // Unranked state
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                                .border(1.dp, SurfaceBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "--",
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text(
                                text = "NOT RANKED YET",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Set a clean lap on ${currentTrack.name} to enter the leaderboard",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = onStartTimeTrial,
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("start_hot_lap_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ApexCyan, contentColor = CarbonBlack),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("START HOT LAP NOW", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSection(
    first: LeaderboardEntry,
    second: LeaderboardEntry,
    third: LeaderboardEntry
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = ApexGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "WORLD PODIUM BENCHMARKS",
                    color = ApexGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Star, contentDescription = null, tint = ApexGold, modifier = Modifier.size(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd Place
                PodiumColumn(
                    entry = second,
                    rank = 2,
                    badgeColor = Color(0xFFCFD8DC),
                    height = 110.dp,
                    modifier = Modifier.weight(1f)
                )

                // 1st Place (Center, tallest)
                PodiumColumn(
                    entry = first,
                    rank = 1,
                    badgeColor = ApexGold,
                    height = 135.dp,
                    modifier = Modifier.weight(1.1f)
                )

                // 3rd Place
                PodiumColumn(
                    entry = third,
                    rank = 3,
                    badgeColor = Color(0xFFBCAAA4),
                    height = 95.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    entry: LeaderboardEntry,
    rank: Int,
    badgeColor: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Driver badge
        Box(
            modifier = Modifier
                .size(if (rank == 1) 36.dp else 30.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.2f))
                .border(1.5.dp, badgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                color = badgeColor,
                fontWeight = FontWeight.Black,
                fontSize = if (rank == 1) 16.sp else 13.sp
            )
        }

        Text(
            text = entry.driverName,
            color = if (entry.isPlayer) ApexCyan else TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            text = entry.formattedTime,
            color = if (rank == 1) ApexGold else ApexGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )

        // Pedestal Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            badgeColor.copy(alpha = 0.25f),
                            DarkSurfaceElevated
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = badgeColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = countryFlagEmoji(entry.countryCode),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.vehicleName,
                    color = TextMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (entry.isPlayer) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ApexCyan)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("YOU", color = CarbonBlack, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val isPlayer = entry.isPlayer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${entry.rank}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayer) ApexCyan.copy(alpha = 0.08f) else DarkSurface
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPlayer) 1.5.dp else 1.dp,
            color = if (isPlayer) ApexCyan else SurfaceBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Rank + Country + Driver & Vehicle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Rank Pill
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (entry.rank) {
                                1 -> ApexGold.copy(alpha = 0.2f)
                                2 -> Color(0xFFCFD8DC).copy(alpha = 0.2f)
                                3 -> Color(0xFFBCAAA4).copy(alpha = 0.2f)
                                else -> DarkSurfaceElevated
                            }
                        )
                        .border(
                            1.dp,
                            when (entry.rank) {
                                1 -> ApexGold
                                2 -> Color(0xFFCFD8DC)
                                3 -> Color(0xFFBCAAA4)
                                else -> if (isPlayer) ApexCyan else SurfaceBorder
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${entry.rank}",
                        color = when (entry.rank) {
                            1 -> ApexGold
                            2 -> Color(0xFFCFD8DC)
                            3 -> Color(0xFFBCAAA4)
                            else -> if (isPlayer) ApexCyan else TextWhite
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Driver details
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = countryFlagEmoji(entry.countryCode),
                            fontSize = 12.sp
                        )
                        Text(
                            text = entry.driverName,
                            color = if (isPlayer) ApexCyan else TextWhite,
                            fontWeight = if (isPlayer) FontWeight.Black else FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isPlayer) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ApexCyan)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("YOU", color = CarbonBlack, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.vehicleName,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "·",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = entry.vehicleCategory,
                            color = entry.tier.primaryColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Right: Time & Gap
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.formattedTime,
                    color = if (entry.rank == 1) ApexGold else TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = entry.formattedGap,
                    color = if (entry.gapMs <= 0L) ApexGold else TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EditDriverProfileDialog(
    currentName: String,
    currentCountry: String,
    onDismiss: () -> Unit,
    onSave: (name: String, country: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var selectedCountry by remember { mutableStateOf(currentCountry) }

    val countries = listOf("US", "GB", "JP", "DE", "FR", "IT", "MC", "ES", "BR", "AU", "CA", "NL", "SE")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, ApexCyan, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ApexCyan)
                    Text(
                        text = "EDIT DRIVER PROFILE",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = "Your driver name and nationality flag will appear on global leaderboards and race telemetry.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 16) name = it },
                    label = { Text("Driver Name / Handle") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = ApexCyan,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedLabelColor = ApexCyan,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("driver_name_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SELECT NATIONALITY",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(countries) { cCode ->
                            val isSelected = cCode == selectedCountry
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ApexCyan.copy(alpha = 0.25f) else DarkSurfaceElevated)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) ApexCyan else SurfaceBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCountry = cCode }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(countryFlagEmoji(cCode), fontSize = 14.sp)
                                    Text(
                                        text = cCode,
                                        color = if (isSelected) ApexCyan else TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Text("CANCEL", color = TextMuted, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSave(name, selectedCountry) },
                        modifier = Modifier.weight(1f).height(46.dp).testTag("save_driver_profile_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ApexCyan, contentColor = CarbonBlack),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SAVE TAG", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

private fun formatTimeMillis(ms: Long): String {
    if (ms <= 0L) return "--:--.---"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val millis = ms % 1000
    return "%d:%02d.%03d".format(min, sec, millis)
}

private fun countryFlagEmoji(countryCode: String): String {
    return when (countryCode.uppercase()) {
        "US" -> "🇺🇸"
        "GB" -> "🇬🇧"
        "JP" -> "🇯🇵"
        "DE" -> "🇩🇪"
        "FR" -> "🇫🇷"
        "IT" -> "🇮🇹"
        "MC" -> "🇲🇨"
        "ES" -> "🇪🇸"
        "BR" -> "🇧🇷"
        "AU" -> "🇦🇺"
        "CA" -> "🇨🇦"
        "NL" -> "🇳🇱"
        "SE" -> "🇸🇪"
        "NO" -> "🇳🇴"
        "FI" -> "🇫🇮"
        else -> "🏁"
    }
}

private fun Track.getCountryCode(): String {
    return when (id) {
        "monaco_riviera" -> "MC"
        "apex_speedway" -> "GB"
        "neon_tokyo" -> "JP"
        "mount_akina" -> "JP"
        else -> "US"
    }
}

private fun Track.getLengthKmString(): String {
    return "%.2f".format(lengthMeters / 1000f)
}
