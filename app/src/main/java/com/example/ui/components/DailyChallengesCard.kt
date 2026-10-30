package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.model.DailyChallenge
import com.example.model.DailyChallengeGenerator
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
import kotlinx.coroutines.delay

@Composable
fun DailyChallengesCard(
    challenges: List<DailyChallenge>,
    allBonusClaimed: Boolean,
    onClaimChallenge: (String) -> Unit,
    onClaimAllBonus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var msUntilReset by remember { mutableLongStateOf(DailyChallengeGenerator.getMsUntilMidnight()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            msUntilReset = DailyChallengeGenerator.getMsUntilMidnight()
        }
    }

    val hours = (msUntilReset / (1000 * 60 * 60)) % 24
    val minutes = (msUntilReset / (1000 * 60)) % 60
    val seconds = (msUntilReset / 1000) % 60
    val countdownText = String.format("%02dh %02dm %02ds", hours, minutes, seconds)

    val allCompleted = challenges.isNotEmpty() && challenges.all { it.isCompleted }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, if (allCompleted && !allBonusClaimed) ApexGold else SurfaceBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row: Title & 24h Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DAILY CHALLENGES",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ApexGold.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("24H RESET", color = ApexGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Timer",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = countdownText,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Challenge List (3 items)
            challenges.forEach { challenge ->
                ChallengeItem(challenge = challenge, onClaim = { onClaimChallenge(challenge.id) })
            }

            // All Challenges Completed Grand Bounty
            if (allCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, ApexGold, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ApexGold, modifier = Modifier.size(22.dp))
                            Column {
                                Text("COMPLETIONIST BOUNTY", color = ApexGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("+$10,000 Credits & +300 REP", color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        if (!allBonusClaimed) {
                            Button(
                                onClick = onClaimAllBonus,
                                colors = ButtonDefaults.buttonColors(containerColor = ApexGold, contentColor = CarbonBlack),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("claim_all_daily_bonus_button")
                            ) {
                                Text("CLAIM BOUNTY", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ApexGreen, modifier = Modifier.size(16.dp))
                                Text("COLLECTED", color = ApexGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeItem(
    challenge: DailyChallenge,
    onClaim: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (challenge.isCompleted && !challenge.isClaimed) ApexGreen.copy(alpha = 0.6f) else SurfaceBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Title & Reward Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(challenge.iconEmoji, fontSize = 16.sp)
                    Text(challenge.title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("+$${String.format("%,d", challenge.rewardCredits)}", color = ApexGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("+${challenge.rewardRep} REP", color = ApexGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(challenge.description, color = TextMuted, fontSize = 11.sp)

            // Progress bar & Claim button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${challenge.currentValue} / ${challenge.targetValue}",
                            color = if (challenge.isCompleted) ApexGreen else TextWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(challenge.progressFraction * 100).toInt()}%",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { challenge.progressFraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (challenge.isCompleted) ApexGreen else ApexCyan,
                        trackColor = Color(0xFF222838)
                    )
                }

                if (challenge.isClaimed) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = ApexGreen, modifier = Modifier.size(16.dp))
                        Text("CLAIMED", color = ApexGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (challenge.isCompleted) {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = ApexGreen, contentColor = CarbonBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("claim_challenge_${challenge.id}")
                    ) {
                        Text("CLAIM", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CarbonBlack.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("IN PROGRESS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
