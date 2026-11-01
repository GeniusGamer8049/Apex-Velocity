package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.model.DriverAvatarRegistry
import com.example.ui.components.AvatarSize
import com.example.ui.components.DriverAvatarView
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
import java.util.Locale

private data class CountryOption(val code: String, val name: String, val flagEmoji: String)

private val COUNTRY_OPTIONS = listOf(
    CountryOption("US", "United States", "🇺🇸"),
    CountryOption("JP", "Japan", "🇯🇵"),
    CountryOption("DE", "Germany", "🇩🇪"),
    CountryOption("GB", "Great Britain", "🇬🇧"),
    CountryOption("IT", "Italy", "🇮🇹"),
    CountryOption("FR", "France", "🇫🇷"),
    CountryOption("BR", "Brazil", "🇧🇷"),
    CountryOption("CA", "Canada", "🇨🇦"),
    CountryOption("AU", "Australia", "🇦🇺"),
    CountryOption("KR", "South Korea", "🇰🇷"),
    CountryOption("ES", "Spain", "🇪🇸"),
    CountryOption("NL", "Netherlands", "🇳🇱")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DriverProfileDialog(
    profile: PlayerProfile,
    onDismiss: () -> Unit,
    onSaveProfile: (name: String, country: String, avatarId: String) -> Unit,
    onOpenAchievements: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editedName by remember { mutableStateOf(profile.driverName) }
    var selectedCountry by remember { mutableStateOf(profile.driverCountry) }
    var selectedAvatarId by remember { mutableStateOf(profile.avatarId) }

    val equippedBadge = profile.achievements.find { it.id == profile.equippedBadgeId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(CarbonBlack)
                .border(1.5.dp, SurfaceBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ApexCyan.copy(alpha = 0.2f))
                                .border(1.dp, ApexCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = ApexCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "DRIVER IDENTITY",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Custom Profile & Career Telemetry",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_driver_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextWhite
                        )
                    }
                }

                // Driver Showcase Hero Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ApexCyan.copy(alpha = 0.5f), ApexPurple.copy(alpha = 0.5f))))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DriverAvatarView(
                            avatarId = selectedAvatarId,
                            size = AvatarSize.LARGE,
                            showEquippedBadgeIndicator = true,
                            hasEquippedBadge = equippedBadge != null
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (editedName.isNotBlank()) editedName else "Apex Pilot",
                                    color = TextWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                val cOption = COUNTRY_OPTIONS.find { it.code == selectedCountry }
                                Text(
                                    text = "${cOption?.flagEmoji ?: "🏁"} $selectedCountry",
                                    color = ApexCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (equippedBadge != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MilitaryTech,
                                        contentDescription = null,
                                        tint = ApexGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = equippedBadge.badgeTitle,
                                        color = ApexGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• ${equippedBadge.badgeRarity.label}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = ApexGold, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "${profile.reputation} REP",
                                        color = ApexGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$",
                                        color = ApexGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%,d", profile.credits),
                                        color = ApexGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Tabs: CAREER STATS vs EDIT IDENTITY
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = ApexCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ApexCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("CAREER STATS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("tab_career_stats")
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("EDIT IDENTITY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("tab_edit_identity")
                    )
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (selectedTab == 0) {
                        CareerStatsTab(
                            profile = profile,
                            onOpenAchievements = {
                                onDismiss()
                                onOpenAchievements()
                            }
                        )
                    } else {
                        EditIdentityTab(
                            currentName = editedName,
                            onNameChange = { editedName = it },
                            selectedCountry = selectedCountry,
                            onCountryChange = { selectedCountry = it },
                            selectedAvatarId = selectedAvatarId,
                            onAvatarChange = { selectedAvatarId = it },
                            userReputation = profile.reputation,
                            onSave = {
                                onSaveProfile(editedName, selectedCountry, selectedAvatarId)
                                onDismiss()
                            },
                            onOpenAchievements = {
                                onDismiss()
                                onOpenAchievements()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerStatsTab(
    profile: PlayerProfile,
    onOpenAchievements: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // High-impact Metric 1: Career Win Percentage
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, ApexCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(ApexCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "CAREER WIN RATE",
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${profile.totalRaceWins} Wins / ${profile.totalRacesCompleted} Events",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f%%", profile.winPercentage),
                            color = ApexCyan,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (profile.winPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ApexCyan,
                        trackColor = CarbonBlack
                    )
                }
            }
        }

        // High-impact Metric 2: Total Distance Driven
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, ApexGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ApexGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = ApexGreen, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "TOTAL DISTANCE DRIVEN",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = String.format(Locale.US, "%,.0f meters on track", profile.totalDistanceDrivenMeters),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "%.1f", profile.totalDistanceKm),
                            color = ApexGreen,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "KILOMETERS",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2x2 Telemetry Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "PODIUM RATE",
                    value = "${profile.totalPodiums} (${String.format(Locale.US, "%.0f%%", profile.podiumPercentage)})",
                    subtitle = "Top 3 finishes",
                    accentColor = ApexGold,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "TOP VELOCITY",
                    value = String.format(Locale.US, "%.1f km/h", profile.highestSpeedKmh),
                    subtitle = "Radar top record",
                    accentColor = ApexRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "TOTAL DRIFT",
                    value = String.format(Locale.US, "%,.0f m", profile.totalDriftDistanceMeters),
                    subtitle = "Drifting distance",
                    accentColor = ApexPurple,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "CLEAN RACES",
                    value = "${profile.cleanRacesCount}",
                    subtitle = "Zero-contact finishes",
                    accentColor = ApexCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "WET VICTORIES",
                    value = "${profile.wetRaceWinsCount}",
                    subtitle = "Rain & storm wins",
                    accentColor = Color(0xFF64B5F6),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "BADGES EARNED",
                    value = "${profile.achievements.count { it.isUnlocked }} / ${profile.achievements.size}",
                    subtitle = "Special badges unlocked",
                    accentColor = ApexGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Achievements Shortcut Action
        item {
            Button(
                onClick = onOpenAchievements,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("manage_badges_from_stats_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexGold)
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = ApexGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VIEW & EQUIP MILESTONE BADGES",
                    color = ApexGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = TextMuted.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditIdentityTab(
    currentName: String,
    onNameChange: (String) -> Unit,
    selectedCountry: String,
    onCountryChange: (String) -> Unit,
    selectedAvatarId: String,
    onAvatarChange: (String) -> Unit,
    userReputation: Int,
    onSave: () -> Unit,
    onOpenAchievements: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Driver Username Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PILOT CALLSIGN / USERNAME",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = currentName,
                    onValueChange = {
                        if (it.length <= 18) onNameChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("driver_name_input"),
                    singleLine = true,
                    placeholder = { Text("Enter driver name...", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ApexCyan)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = ApexCyan,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Nationality / Country Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RACING NATIONALITY",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Selected: $selectedCountry",
                        color = ApexCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    COUNTRY_OPTIONS.forEach { c ->
                        val isSelected = selectedCountry == c.code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ApexCyan.copy(alpha = 0.25f) else DarkSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) ApexCyan else SurfaceBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onCountryChange(c.code) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("country_chip_${c.code}")
                        ) {
                            Text(
                                text = "${c.flagEmoji} ${c.code}",
                                color = if (isSelected) TextWhite else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Avatar Selection Grid
        item {
            Text(
                text = "CHOOSE RACING AVATAR",
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        items(DriverAvatarRegistry.allAvatars) { avatar ->
            val isSelected = selectedAvatarId == avatar.id
            val isUnlocked = userReputation >= avatar.requiredRep

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.5.dp,
                        if (isSelected) avatar.primaryColor else SurfaceBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = isUnlocked) {
                        onAvatarChange(avatar.id)
                    }
                    .testTag("avatar_option_${avatar.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DarkSurfaceElevated else DarkSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DriverAvatarView(
                        avatarId = avatar.id,
                        size = AvatarSize.MEDIUM
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = avatar.name,
                                color = if (isUnlocked) TextWhite else TextMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(avatar.primaryColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = avatar.tag,
                                    color = avatar.primaryColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = avatar.subtitle,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        if (!isUnlocked) {
                            Text(
                                text = "Requires ${avatar.requiredRep} REP to unlock",
                                color = ApexGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(avatar.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = CarbonBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (!isUnlocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons Row: SAVE & MANAGE BADGES
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ApexCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CarbonBlack,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAVE PROFILE CHANGES",
                    color = CarbonBlack,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        item {
            Button(
                onClick = onOpenAchievements,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("open_badges_from_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = ApexGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OPEN BADGES & ACHIEVEMENTS",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
