package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.material.icons.filled.MilitaryTech
import com.example.model.Achievement
import com.example.model.CareerObjectiveResult
import com.example.model.CareerRaceEvent
import com.example.model.PostRaceResult
import com.example.model.RivalFinishResult
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
fun PostRaceSummaryScreen(
    result: PostRaceResult,
    onContinueToCareer: () -> Unit,
    onRetryRace: () -> Unit,
    onStartNextEvent: ((CareerRaceEvent) -> Unit)? = null,
    onGoToGarage: () -> Unit,
    onViewLeaderboard: ((String) -> Unit)? = null,
    onOpenAchievements: (() -> Unit)? = null
) {
    BackHandler {
        onContinueToCareer()
    }

    var animateProgress by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateProgress = true
    }

    val repProgressAnim by animateFloatAsState(
        targetValue = if (animateProgress) result.repProgressToNextTier else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "repProgress"
    )

    val tierProgressAnim by animateFloatAsState(
        targetValue = if (animateProgress && result.tierTotalEvents > 0) {
            (result.tierCompletedEvents.toFloat() / result.tierTotalEvents.toFloat()).coerceIn(0f, 1f)
        } else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "tierProgress"
    )

    val podiumColor = when (result.position) {
        1 -> ApexGold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> ApexCyan
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. Podium Celebration & Final Position Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, podiumColor, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(podiumColor.copy(alpha = 0.18f), DarkSurface)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Trophy Badge
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(podiumColor.copy(alpha = 0.22f))
                                .border(2.dp, podiumColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = podiumColor,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        // Placement Headline
                        Text(
                            text = when (result.position) {
                                1 -> "VICTORY! 1ST PLACE"
                                2 -> "RUNNER UP - 2ND PLACE"
                                3 -> "PODIUM FINISH - 3RD PLACE"
                                else -> "FINISHED - ${result.position}TH PLACE"
                            },
                            color = podiumColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "${result.trackName.uppercase()} • ${result.vehicleName}",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Position Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, podiumColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "POSITION P${result.position} OF ${result.totalRacers}",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 2. Race Timing Card (Total Race Time, Best Lap, and Laps Breakdown)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(Icons.Default.Timer, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(20.dp))
                            Text(
                                text = "RACE TIMING",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        if (result.isNewBest) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ApexGreen.copy(alpha = 0.2f))
                                    .border(1.dp, ApexGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "★ NEW LAP RECORD",
                                    color = ApexGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Total Race Time Highlight Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL RACE TIME",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatTime(result.totalRaceTimeMs),
                                    color = ApexCyan,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "BEST LAP",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatTime(result.bestLapTimeMs),
                                    color = if (result.isNewBest) ApexGreen else TextWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Lap by Lap Times Row
                    if (result.lapTimesMs.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "LAP BREAKDOWN",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                result.lapTimesMs.forEachIndexed { index, lapTime ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurface)
                                            .border(
                                                1.dp,
                                                if (lapTime == result.bestLapTimeMs) ApexCyan else SurfaceBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(vertical = 6.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "LAP ${index + 1}",
                                                color = TextMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = formatTime(lapTime),
                                                color = if (lapTime == result.bestLapTimeMs) ApexCyan else TextWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Telemetry Quick Stats
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = ApexOrange, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("TOP SPEED", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${result.topSpeedKmh.toInt()} KM/H",
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = ApexGreen, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("DRIFT PTS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${result.driftScore} PTS",
                                        color = TextWhite,
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
        }

        // 3. Earnings & Financial Rewards Breakdown Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = ApexGreen, modifier = Modifier.size(20.dp))
                            Text(
                                text = "RACE EARNINGS BREAKDOWN",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = ApexGold, modifier = Modifier.size(14.dp))
                            Text(
                                text = "+${result.repEarned} REP",
                                color = ApexGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Itemized Financial Rows
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EarningsRow(
                            label = "Base Prize Purse (P${result.position})",
                            amount = "+$${String.format("%,d", result.basePrize)}",
                            amountColor = TextWhite
                        )

                        EarningsRow(
                            label = "Clean Driving Bonus",
                            amount = if (result.cleanBonus > 0) "+$${result.cleanBonus}" else "$0 (Collisions)",
                            amountColor = if (result.cleanBonus > 0) ApexGreen else TextMuted
                        )

                        if (result.driftBonus > 0) {
                            EarningsRow(
                                label = "Drift Mastery Bonus",
                                amount = "+$${result.driftBonus}",
                                amountColor = ApexOrange
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SurfaceBorder)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL PURSE WON",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "+$${String.format("%,d", result.totalEarnings)}",
                                color = ApexGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Updated Bank Account Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ApexGreen.copy(alpha = 0.12f))
                            .border(1.dp, ApexGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UPDATED DRIVER BALANCE",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$ ${String.format("%,d", result.creditsAfter)}",
                            color = ApexGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 4. Progress Towards Career Goals Card (Core User Requirement)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ApexCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ApexGold, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "CAREER GOALS PROGRESS",
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = result.tierName,
                                    color = ApexCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (result.isTierTrophyUnlocked) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ApexGold.copy(alpha = 0.2f))
                                    .border(1.dp, ApexGold, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "🏆 TROPHY WON!",
                                    color = ApexGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Goal 1: Tier Trophy Completion
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ApexGold, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Championship Trophy: ${result.tierTrophyName}",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${result.tierCompletedEvents} / ${result.tierTotalEvents} Events",
                                color = if (result.isTierTrophyUnlocked) ApexGreen else ApexCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        LinearProgressIndicator(
                            progress = { tierProgressAnim },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (result.isTierTrophyUnlocked) ApexGreen else ApexCyan,
                            trackColor = DarkSurfaceElevated
                        )
                    }

                    // Goal 2: Next Championship Tier Unlock Progress
                    if (result.nextTierName != null && result.nextTierRequiredRep != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurface)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (result.repAfter >= result.nextTierRequiredRep) Icons.Default.CheckCircle else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (result.repAfter >= result.nextTierRequiredRep) ApexGreen else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Unlock: ${result.nextTierName}",
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${result.repAfter} / ${result.nextTierRequiredRep} REP",
                                    color = if (result.repAfter >= result.nextTierRequiredRep) ApexGreen else ApexGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            LinearProgressIndicator(
                                progress = { repProgressAnim },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (result.repAfter >= result.nextTierRequiredRep) ApexGreen else ApexGold,
                                trackColor = DarkSurfaceElevated
                            )

                            if (result.repAfter >= result.nextTierRequiredRep) {
                                Text(
                                    text = "★ NEW TIER UNLOCKED! Ready to compete in higher class events.",
                                    color = ApexGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                val repNeeded = result.nextTierRequiredRep - result.repAfter
                                Text(
                                    text = "${repNeeded} REP needed to unlock this championship tier.",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Goal 3: Event Specific Career Objectives Checklist
                    if (result.careerObjectives.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "EVENT OBJECTIVES",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            result.careerObjectives.forEach { objective ->
                                ObjectiveRow(objective)
                            }
                        }
                    }

                    // Goal 4: Daily Challenges Contribution
                    if (result.dailyChallengesUpdated.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "DAILY OBJECTIVES PROGRESS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            result.dailyChallengesUpdated.take(2).forEach { challenge ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurface)
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(challenge.title, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${challenge.currentValue} / ${challenge.targetValue} • ${challenge.description}",
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }

                                        if (challenge.isCompleted) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(ApexGreen.copy(alpha = 0.2f))
                                                    .border(1.dp, ApexGreen, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("COMPLETED", color = ApexGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4b. Newly Unlocked Badges Celebration Card
        if (result.unlockedBadges.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, ApexGold, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = ApexGold, modifier = Modifier.size(22.dp))
                            Text(
                                text = "★ SPECIAL BADGES UNLOCKED!",
                                color = ApexGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        result.unlockedBadges.forEach { badge ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, ApexGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(badge.title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("\"${badge.badgeTitle}\" • ${badge.description}", color = TextMuted, fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("+$${String.format("%,d", badge.rewardCredits)}", color = ApexGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text("+${badge.rewardRep} REP", color = ApexGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        if (onOpenAchievements != null) {
                            Button(
                                onClick = onOpenAchievements,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("post_race_achievements_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = ApexGold),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ApexGold),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("VIEW ALL TROPHIES & BADGES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Finishing Grid Rivals Comparison
        if (result.rivalResults.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(18.dp))
                            Text(
                                text = "OFFICIAL RACE RESULTS",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        result.rivalResults.forEach { racer ->
                            RivalRow(racer)
                        }
                    }
                }
            }
        }

        // 6. Action Navigation Buttons
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action: Next Event (if available)
                if (result.nextCareerEvent != null && onStartNextEvent != null) {
                    Button(
                        onClick = { onStartNextEvent(result.nextCareerEvent) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("next_event_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ApexCyan, contentColor = CarbonBlack),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "NEXT EVENT: ${result.nextCareerEvent.title.uppercase()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Secondary Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetryRace,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("retry_race_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                            Text("RETRY", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onContinueToCareer,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("career_hub_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (result.nextCareerEvent == null) ApexCyan else DarkSurfaceElevated,
                            contentColor = if (result.nextCareerEvent == null) CarbonBlack else TextWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (result.nextCareerEvent != null) androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder) else null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("CAREER HUB", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Tertiary Actions Row: Garage, Badges & Leaderboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onGoToGarage,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("garage_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(15.dp))
                            Text("GARAGE", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    if (onOpenAchievements != null) {
                        OutlinedButton(
                            onClick = onOpenAchievements,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("badges_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = ApexGold, modifier = Modifier.size(15.dp))
                                Text("BADGES", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    if (onViewLeaderboard != null) {
                        OutlinedButton(
                            onClick = { onViewLeaderboard(result.trackId) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("leaderboard_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Leaderboard, contentDescription = null, tint = ApexGold, modifier = Modifier.size(15.dp))
                                Text("RANKINGS", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningsRow(label: String, amount: String, amountColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(
            text = amount,
            color = amountColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ObjectiveRow(objective: CareerObjectiveResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (objective.isAchieved) ApexGreen.copy(alpha = 0.4f) else SurfaceBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (objective.isAchieved) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = null,
                tint = if (objective.isAchieved) ApexGreen else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = objective.title,
                    color = if (objective.isAchieved) TextWhite else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = objective.description,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = objective.rewardText,
            color = if (objective.isAchieved) ApexGreen else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RivalRow(racer: RivalFinishResult) {
    val isPodium = racer.position <= 3
    val posColor = when (racer.position) {
        1 -> ApexGold
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> TextMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (racer.isPlayer) ApexCyan.copy(alpha = 0.15f) else DarkSurface)
            .border(
                1.dp,
                if (racer.isPlayer) ApexCyan else SurfaceBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "P${racer.position}",
                color = posColor,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )

            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = racer.name,
                        color = if (racer.isPlayer) ApexCyan else TextWhite,
                        fontWeight = if (racer.isPlayer) FontWeight.Black else FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (racer.isPlayer) {
                        Text(
                            text = "(YOU)",
                            color = ApexCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Text(
                    text = racer.vehicleName,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTime(racer.totalTimeMs),
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = racer.deltaFormatted,
                color = if (racer.deltaFormatted == "LEADER") ApexGold else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "--:--.---"
    val minutes = ms / 60000
    val seconds = (ms % 60000) / 1000
    val millis = ms % 1000
    return String.format("%02d:%02d.%03d", minutes, seconds, millis)
}
