package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.model.Achievement
import com.example.model.BadgeRarity
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun BadgeUnlockBanner(
    unlockedBadges: List<Achievement>,
    onDismiss: () -> Unit,
    onViewBadge: (Achievement) -> Unit
) {
    val activeBadge = unlockedBadges.firstOrNull() ?: return

    LaunchedEffect(activeBadge.id) {
        delay(6000)
        onDismiss()
    }

    val rarityColor = when (activeBadge.badgeRarity) {
        BadgeRarity.BRONZE -> Color(0xFFCD7F32)
        BadgeRarity.SILVER -> Color(0xFFC0C0C0)
        BadgeRarity.GOLD -> ApexGold
        BadgeRarity.PLATINUM -> ApexCyan
        BadgeRarity.TITANIUM -> Color(0xFFB388FF)
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, rarityColor, RoundedCornerShape(16.dp))
                .clickable { onViewBadge(activeBadge) }
                .testTag("badge_unlock_banner"),
            colors = CardDefaults.cardColors(containerColor = CarbonBlack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(rarityColor.copy(alpha = 0.25f), DarkSurfaceElevated)
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(rarityColor.copy(alpha = 0.25f))
                                .border(1.5.dp, rarityColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MilitaryTech,
                                contentDescription = null,
                                tint = rarityColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "★ SPECIAL BADGE UNLOCKED!",
                                    color = rarityColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "• ${activeBadge.badgeRarity.label.uppercase()}",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${activeBadge.title}: \"${activeBadge.badgeTitle}\"",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "+$${String.format("%,d", activeBadge.rewardCredits)}",
                                    color = ApexGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "+${activeBadge.rewardRep} REP",
                                    color = ApexGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "(Tap to view)",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
