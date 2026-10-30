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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

data class RaceResults(
    val position: Int,
    val totalRacers: Int,
    val bestLapTimeMs: Long,
    val isNewBest: Boolean,
    val basePrize: Int,
    val cleanBonus: Int,
    val driftBonus: Int,
    val repEarned: Int,
    val isTimeTrial: Boolean
)

@Composable
fun RaceSummaryDialog(
    results: RaceResults,
    onContinue: () -> Unit,
    onRetry: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, if (results.position == 1) ApexGold else ApexCyan, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Trophy or Position Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (results.position == 1) ApexGold.copy(alpha = 0.2f) else DarkSurfaceElevated)
                        .border(1.5.dp, if (results.position == 1) ApexGold else ApexCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (results.position == 1) ApexGold else if (results.position <= 3) ApexCyan else TextMuted,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (results.isTimeTrial) "TIME TRIAL FINISHED" else if (results.position == 1) "VICTORY! 1ST PLACE" else "${results.position}TH PLACE FINISH",
                        color = if (results.position == 1) ApexGold else TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (results.isNewBest) {
                        Text(
                            text = "★ NEW ALL-TIME RECORD SET!",
                            color = ApexGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Best Lap Time Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BEST LAP TIME", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatLapTime(results.bestLapTimeMs),
                            color = ApexCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Earnings & REP breakdown
                if (!results.isTimeTrial) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Base Prize Purse", color = TextMuted, fontSize = 12.sp)
                            Text("+$${results.basePrize}", color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        if (results.cleanBonus > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Clean Racing Bonus", color = TextMuted, fontSize = 12.sp)
                                Text("+$${results.cleanBonus}", color = ApexGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        if (results.driftBonus > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Drift Style Bonus", color = TextMuted, fontSize = 12.sp)
                                Text("+$${results.driftBonus}", color = ApexOrange, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reputation Gained", color = TextMuted, fontSize = 12.sp)
                            Text("+${results.repEarned} REP", color = ApexGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        val total = results.basePrize + results.cleanBonus + results.driftBonus
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL REWARD", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "$ ${String.format("%,d", total)}",
                                color = ApexGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("RETRY", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onContinue,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("continue_race_summary_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ApexCyan, contentColor = CarbonBlack),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("CONTINUE", fontWeight = FontWeight.Black)
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
