package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PlayerProfile
import com.example.model.Achievement
import com.example.model.AchievementCategory
import com.example.model.BadgeRarity
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
fun AchievementsDialog(
    profile: PlayerProfile,
    onDismiss: () -> Unit,
    onClaimReward: (String) -> Unit,
    onEquipBadge: (String) -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = remember { AchievementCategory.entries.toTypedArray() }
    val selectedCategory = categories[selectedCategoryIndex]

    val filteredAchievements = remember(selectedCategory, profile.achievements) {
        if (selectedCategory == AchievementCategory.ALL) {
            profile.achievements
        } else {
            profile.achievements.filter { it.category == selectedCategory }
        }
    }

    val totalUnlocked = profile.achievements.count { it.isUnlocked }
    val totalBadges = profile.achievements.size
    val equippedBadge = profile.achievements.find { it.id == profile.equippedBadgeId }
        ?: profile.achievements.firstOrNull { it.isUnlocked }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .border(1.5.dp, ApexGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CarbonBlack),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
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
                                .background(ApexGold.copy(alpha = 0.2f))
                                .border(1.dp, ApexGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = ApexGold, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = "TROPHIES & SPECIAL BADGES",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Milestones • Velocity • Drifting • Victories",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_achievements_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                    }
                }

                // Equipped Badge & Driver Stats Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("DRIVER TITLE & BADGE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = profile.driverName,
                                        color = TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (equippedBadge != null) {
                                        BadgePill(badge = equippedBadge)
                                    }
                                }
                            }

                            // Unlocked Progress Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, ApexGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$totalUnlocked / $totalBadges UNLOCKED",
                                    color = ApexGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Milestone Telemetry Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMiniChip("WINS", "${profile.totalRaceWins} 🏆")
                            StatMiniChip("DRIFT", "${profile.totalDriftDistanceMeters.toInt()}m 💨")
                            StatMiniChip("TOP SPEED", "${profile.highestSpeedKmh.toInt()} km/h ⚡")
                            StatMiniChip("CLEAN", "${profile.cleanRacesCount} 🛡️")
                        }

                        // Linear overall progress
                        LinearProgressIndicator(
                            progress = { (totalUnlocked.toFloat() / totalBadges.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ApexGold,
                            trackColor = DarkSurface
                        )
                    }
                }

                // Category Filter Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    containerColor = DarkSurface,
                    contentColor = ApexCyan,
                    edgePadding = 6.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                            color = ApexCyan
                        )
                    }
                ) {
                    categories.forEachIndexed { index, cat ->
                        val count = if (cat == AchievementCategory.ALL) {
                            profile.achievements.size
                        } else {
                            profile.achievements.count { it.category == cat }
                        }
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = {
                                Text(
                                    text = "${cat.displayName.uppercase()} ($count)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (selectedCategoryIndex == index) ApexCyan else TextMuted
                                )
                            }
                        )
                    }
                }

                // Badges List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAchievements, key = { it.id }) { ach ->
                        AchievementBadgeCard(
                            achievement = ach,
                            isEquipped = profile.equippedBadgeId == ach.id,
                            onClaimReward = { onClaimReward(ach.id) },
                            onEquipBadge = { onEquipBadge(ach.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
fun AchievementBadgeCard(
    achievement: Achievement,
    isEquipped: Boolean,
    onClaimReward: () -> Unit,
    onEquipBadge: () -> Unit
) {
    val rarityColor = when (achievement.badgeRarity) {
        BadgeRarity.BRONZE -> Color(0xFFCD7F32)
        BadgeRarity.SILVER -> Color(0xFFC0C0C0)
        BadgeRarity.GOLD -> ApexGold
        BadgeRarity.PLATINUM -> ApexCyan
        BadgeRarity.TITANIUM -> Color(0xFFB388FF)
    }

    val iconVector: ImageVector = when (achievement.category) {
        AchievementCategory.SPEED -> Icons.Default.Speed
        AchievementCategory.DRIFT -> Icons.Default.Flag
        AchievementCategory.WINS -> Icons.Default.EmojiEvents
        AchievementCategory.MASTERY -> Icons.Default.WorkspacePremium
        AchievementCategory.GARAGE -> Icons.Default.Build
        AchievementCategory.ALL -> Icons.Default.Star
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isEquipped) 1.5.dp else if (achievement.isUnlocked) 1.dp else 0.5.dp,
                color = if (isEquipped) ApexGreen else if (achievement.isUnlocked) rarityColor else SurfaceBorder,
                shape = RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = if (achievement.isUnlocked) DarkSurface else CarbonBlack.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp)
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
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge Shield / Emblem Icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    if (achievement.isUnlocked) {
                                        listOf(rarityColor.copy(alpha = 0.35f), DarkSurfaceElevated)
                                    } else {
                                        listOf(DarkSurfaceElevated, CarbonBlack)
                                    }
                                )
                            )
                            .border(1.dp, if (achievement.isUnlocked) rarityColor else SurfaceBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = if (achievement.isUnlocked) rarityColor else TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = achievement.title,
                                color = if (achievement.isUnlocked) TextWhite else TextMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Rarity badge label
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(rarityColor.copy(alpha = 0.18f))
                                    .border(0.5.dp, rarityColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = achievement.badgeRarity.label.uppercase(),
                                    color = rarityColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Text(
                            text = achievement.description,
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Text(
                            text = "Special Badge: \"${achievement.badgeTitle}\"",
                            color = if (achievement.isUnlocked) rarityColor else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Milestone Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (achievement.isUnlocked) "MILESTONE COMPLETED" else "PROGRESS",
                        color = if (achievement.isUnlocked) ApexGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (achievement.targetValue >= 1000f) {
                            "${achievement.currentValue.toInt()} / ${achievement.targetValue.toInt()} ${achievement.unit}"
                        } else {
                            "${achievement.currentValue.toInt()} / ${achievement.targetValue.toInt()} ${achievement.unit}"
                        },
                        color = if (achievement.isUnlocked) ApexGreen else TextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                LinearProgressIndicator(
                    progress = { achievement.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (achievement.isUnlocked) ApexGreen else rarityColor,
                    trackColor = DarkSurfaceElevated
                )
            }

            // Rewards & Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reward tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "+$${String.format("%,d", achievement.rewardCredits)}",
                        color = ApexGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "+${achievement.rewardRep} REP",
                        color = ApexGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Action Button
                if (achievement.isUnlocked) {
                    if (!achievement.isClaimed) {
                        Button(
                            onClick = onClaimReward,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("claim_badge_${achievement.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = ApexGreen, contentColor = CarbonBlack),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CLAIM REWARD", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    } else if (isEquipped) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ApexGreen.copy(alpha = 0.2f))
                                .border(1.dp, ApexGreen, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ApexGreen, modifier = Modifier.size(14.dp))
                                Text("EQUIPPED BADGE", color = ApexGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onEquipBadge,
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("equip_badge_${achievement.id}"),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, rarityColor)
                        ) {
                            Text("EQUIP BADGE", color = rarityColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Text("LOCKED", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BadgePill(badge: Achievement) {
    val rarityColor = when (badge.badgeRarity) {
        BadgeRarity.BRONZE -> Color(0xFFCD7F32)
        BadgeRarity.SILVER -> Color(0xFFC0C0C0)
        BadgeRarity.GOLD -> ApexGold
        BadgeRarity.PLATINUM -> ApexCyan
        BadgeRarity.TITANIUM -> Color(0xFFB388FF)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(rarityColor.copy(alpha = 0.2f))
            .border(1.dp, rarityColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = rarityColor, modifier = Modifier.size(13.dp))
            Text(
                text = badge.badgeTitle,
                color = rarityColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun StatMiniChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}
