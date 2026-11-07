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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import com.example.data.CareerManager
import com.example.data.room.CareerProgress
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
import com.example.ui.components.DailyChallengesCard
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

import androidx.compose.material.icons.filled.MilitaryTech
import com.example.ui.components.AvatarSize
import com.example.ui.components.DriverAvatarView
import com.example.ui.dialogs.BadgePill

@Composable
fun CareerScreen(
    profile: PlayerProfile,
    careerProgress: CareerProgress? = null,
    careerManager: CareerManager? = null,
    onStartCareerRace: (CareerRaceEvent) -> Unit,
    onClaimChallenge: (String) -> Unit,
    onClaimAllBonus: () -> Unit,
    onOpenAchievements: () -> Unit = {},
    onOpenDriverProfile: () -> Unit = {}
) {
    val activeProgress = careerProgress ?: CareerProgress.defaultProgression()
    val levelProgress = remember(activeProgress.experiencePoints) {
        val level = run {
            var lvl = 1
            for (i in CareerManager.LEVEL_THRESHOLDS.indices.reversed()) {
                if (activeProgress.experiencePoints >= CareerManager.LEVEL_THRESHOLDS[i]) {
                    lvl = i + 1
                    break
                }
            }
            lvl
        }
        val currentThresh = CareerManager.LEVEL_THRESHOLDS[(level - 1).coerceIn(0, CareerManager.LEVEL_THRESHOLDS.size - 1)]
        val nextThresh = if (level < CareerManager.LEVEL_THRESHOLDS.size) CareerManager.LEVEL_THRESHOLDS[level] else currentThresh
        val span = (nextThresh - currentThresh).coerceAtLeast(1)
        val curXp = (activeProgress.experiencePoints - currentThresh).coerceIn(0, span)
        val pct = if (level >= CareerManager.LEVEL_THRESHOLDS.size) 1.0f else (curXp.toFloat() / span.toFloat()).coerceIn(0f, 1f)
        val title = CareerManager.LEVEL_TITLES[(level - 1).coerceIn(0, CareerManager.LEVEL_TITLES.size - 1)]
        com.example.data.LevelProgress(
            currentLevel = level,
            currentLevelTitle = title,
            currentLevelXp = curXp,
            xpRequiredForNextLevel = span,
            progressPercentage = pct,
            isMaxLevel = level >= CareerManager.LEVEL_THRESHOLDS.size
        )
    }

    val tiers = remember { CareerRegistry.createDefaultTiers() }
    var selectedTierIndex by remember { mutableIntStateOf(0) }
    val currentTier = tiers[selectedTierIndex]
    val equippedBadge = profile.achievements.find { it.id == profile.equippedBadgeId }
        ?: profile.achievements.firstOrNull { it.isUnlocked }

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
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CarbonBlack.copy(alpha = 0.85f))
                                .border(1.dp, ApexCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { onOpenDriverProfile() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("career_open_profile_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DriverAvatarView(
                                    avatarId = profile.avatarId,
                                    size = AvatarSize.SMALL
                                )
                                Text(
                                    text = profile.driverName,
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.0f%%", profile.winPercentage)} Win",
                                    color = ApexGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (equippedBadge != null) {
                            BadgePill(badge = equippedBadge)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CarbonBlack.copy(alpha = 0.85f))
                                .border(1.dp, ApexGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { onOpenAchievements() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("career_open_achievements_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = ApexGold, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "${profile.achievements.count { it.isUnlocked }}/${profile.achievements.size} BADGES",
                                    color = ApexGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // Driver Level & Career Progression Room Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ApexGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("career_progression_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header with Driver Level & Total XP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ApexGold.copy(alpha = 0.2f))
                                    .border(1.dp, ApexGold, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "LVL ${levelProgress.currentLevel}",
                                    color = ApexGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Column {
                                Text(
                                    text = levelProgress.currentLevelTitle,
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (levelProgress.isMaxLevel) "MAX DRIVER RANK REACHED"
                                    else "${levelProgress.currentLevelXp} / ${levelProgress.xpRequiredForNextLevel} XP to Level ${levelProgress.currentLevel + 1}",
                                    color = ApexCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CarbonBlack.copy(alpha = 0.8f))
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "${activeProgress.experiencePoints} XP",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Level XP Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { levelProgress.progressPercentage },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ApexGold,
                            trackColor = CarbonBlack
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progression: ${(levelProgress.progressPercentage * 100).toInt()}%",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (levelProgress.isMaxLevel) "APEX LEGEND" else "Next: Level ${levelProgress.currentLevel + 1}",
                                color = ApexGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 4 Career Quick Statistics
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TROPHIES", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("🏆 ${activeProgress.trophyCount}", color = ApexGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RACES", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("🏁 ${activeProgress.totalRacesFinished}", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WINS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("🥇 ${activeProgress.totalWins}", color = ApexGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PODIUMS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("🥈 ${activeProgress.totalPodiums}", color = ApexCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Track & Vehicle Progression Tiers Status Badges
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CIRCUITS & VEHICLE TIERS UNLOCKED",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val unlockedTracks = activeProgress.getTrackIdList()
                            val tracks = listOf(
                                "monaco_riviera" to "Monaco",
                                "apex_speedway" to "Apex GP",
                                "neon_tokyo" to "Tokyo",
                                "mount_akina" to "Akina"
                            )
                            for ((trackId, label) in tracks) {
                                val isUnlocked = unlockedTracks.contains(trackId)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isUnlocked) ApexGreen.copy(alpha = 0.15f) else CarbonBlack)
                                        .border(
                                            1.dp,
                                            if (isUnlocked) ApexGreen.copy(alpha = 0.7f) else SurfaceBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isUnlocked) "✓ $label" else "🔒 $label",
                                        color = if (isUnlocked) ApexGreen else TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Vehicle Progression Tiers Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val unlockedTiers = activeProgress.getTierIdList()
                            val tierBadges = listOf(
                                "tier_1" to "T1 Clubman",
                                "tier_2" to "T2 Pro-Am",
                                "tier_3" to "T3 Supercar",
                                "tier_4" to "T4 World GP"
                            )
                            for ((tierId, label) in tierBadges) {
                                val isUnlocked = unlockedTiers.contains(tierId)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isUnlocked) ApexCyan.copy(alpha = 0.15f) else CarbonBlack)
                                        .border(
                                            1.dp,
                                            if (isUnlocked) ApexCyan.copy(alpha = 0.7f) else SurfaceBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isUnlocked) "★ $label" else "🔒 $label",
                                        color = if (isUnlocked) ApexCyan else TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 24H Daily Objectives Card
        item {
            DailyChallengesCard(
                challenges = profile.dailyChallenges,
                allBonusClaimed = profile.allChallengesBonusClaimed,
                onClaimChallenge = onClaimChallenge,
                onClaimAllBonus = onClaimAllBonus
            )
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
