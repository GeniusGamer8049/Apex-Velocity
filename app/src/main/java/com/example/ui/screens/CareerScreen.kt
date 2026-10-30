package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayerProfile
import com.example.model.CareerRaceEvent
import com.example.model.CareerRegistry
import com.example.model.CareerTier
import com.example.model.TrackRegistry
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

@Composable
fun CareerScreen(
    profile: PlayerProfile,
    onStartCareerRace: (CareerRaceEvent) -> Unit
) {
    val tiers = remember { CareerRegistry.createDefaultTiers() }
    var selectedTierIndex by remember { mutableIntStateOf(0) }
    val currentTier = tiers[selectedTierIndex]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Career Championship Header with Track Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_track_hero_1793368532865),
                    contentDescription = "Championship Circuit",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, CarbonBlack.copy(alpha = 0.92f))
                            )
                        )
                )

                // Top Balance Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CarbonBlack.copy(alpha = 0.85f))
                            .border(1.dp, ApexGreen, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$ ${String.format("%,d", profile.credits)}",
                            color = ApexGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CarbonBlack.copy(alpha = 0.85f))
                            .border(1.dp, ApexGold, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = ApexGold, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${profile.reputation} REP",
                                color = ApexGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Title
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "APEX MOTORSPORT CAREER",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Climb the ranks from rookie clubman to world champion",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Tier Tabs (Tier 1..4)
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTierIndex,
                containerColor = DarkSurface,
                contentColor = ApexCyan,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTierIndex]),
                        color = ApexCyan
                    )
                }
            ) {
                tiers.forEachIndexed { index, tier ->
                    val isUnlocked = profile.reputation >= tier.requiredRep
                    Tab(
                        selected = selectedTierIndex == index,
                        onClick = { selectedTierIndex = index },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isUnlocked) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ApexGold, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = tier.name.substringBefore(":"),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) TextWhite else TextMuted
                                )
                            }
                        }
                    )
                }
            }
        }

        // Selected Tier Header Info
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
                    Column {
                        Text(text = currentTier.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = currentTier.subtitle, color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ApexGold, modifier = Modifier.size(16.dp))
                            Text(text = "Trophy: ${currentTier.trophyName}", color = ApexGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (profile.reputation < currentTier.requiredRep) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ApexRed.copy(alpha = 0.2f))
                                .border(1.dp, ApexRed, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "LOCKED (${currentTier.requiredRep} REP REQ)",
                                color = ApexRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Race Event Cards in Tier
        items(currentTier.events) { event ->
            val track = TrackRegistry.getTrackById(event.trackId)
            val isCompleted = profile.completedEventIds.contains(event.id)
            val isTierUnlocked = profile.reputation >= currentTier.requiredRep
            val isEventUnlocked = isTierUnlocked && (profile.reputation >= event.requiredRep)
            val canAffordEntry = profile.credits >= event.entryFee

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isCompleted) 1.5.dp else 1.dp,
                        color = if (isCompleted) ApexGreen else SurfaceBorder,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = event.title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "${track.name} • ${track.location}", color = ApexCyan, fontSize = 12.sp)
                        }

                        if (isCompleted) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ApexGreen, modifier = Modifier.size(18.dp))
                                Text("COMPLETED", color = ApexGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Race details row: Laps, Length, Weather
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("LAPS", color = TextMuted, fontSize = 10.sp)
                            Text("${event.laps} Laps (${event.laps * track.lengthMeters / 1000} km)", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("CONDITIONS", color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = if (event.dynamicWeather) "🌦️ Dynamic Weather" else "☀️ Clear & Dry",
                                color = if (event.dynamicWeather) ApexOrange else ApexCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("ENTRY FEE", color = TextMuted, fontSize = 10.sp)
                            Text(if (event.entryFee > 0) "$${event.entryFee}" else "FREE", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prize Purse
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("1st: $${String.format("%,d", event.firstPrize)}", color = ApexGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("2nd: $${String.format("%,d", event.secondPrize)}", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                            Text("3rd: $${String.format("%,d", event.thirdPrize)}", color = Color(0xFFBCAAA4), fontSize = 12.sp)
                        }
                        Text("+${event.repReward} REP", color = ApexGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Enter Race Action Button
                    Button(
                        onClick = { onStartCareerRace(event) },
                        enabled = isEventUnlocked && canAffordEntry,
                        modifier = Modifier.fillMaxWidth().testTag("enter_race_button_${event.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexCyan,
                            contentColor = CarbonBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isEventUnlocked) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("LOCKED (${event.requiredRep} REP NEEDED)", fontWeight = FontWeight.Bold)
                            } else if (!canAffordEntry) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("INSUFFICIENT FUNDS ($${event.entryFee})", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(if (isCompleted) "RACE AGAIN" else "ENTER RACE", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
