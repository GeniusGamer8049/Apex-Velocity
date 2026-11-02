package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverProfileRepository
import com.example.data.room.DriverProfile
import com.example.ui.MainViewModel
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Jetpack Compose screen that uses [DriverProfileRepository] to display the current user's stats
 * and provides text fields to update their username.
 */
@Composable
fun DriverProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {},
    onOpenRaceHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    DriverProfileScreen(
        repository = viewModel.driverProfileRepository,
        onBack = onBack,
        onOpenRaceHistory = onOpenRaceHistory,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(
    onBack: () -> Unit = {},
    onOpenRaceHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
    repository: DriverProfileRepository = DriverProfileRepository.getInstance(LocalContext.current)
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val profileState by repository.driverProfileFlow.collectAsState(initial = null)

    // Fallback profile if repository hasn't emitted yet
    val currentProfile = profileState ?: remember {
        DriverProfile(
            id = "primary_driver",
            username = "Apex Pilot",
            avatarPath = "avatar_cyber_neon",
            totalDistance = 0.0,
            totalWins = 0,
            country = "US"
        )
    }

    var usernameInput by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Sync input field with loaded profile
    LaunchedEffect(profileState) {
        if (profileState != null && !isInitialized) {
            usernameInput = profileState!!.username
            isInitialized = true
        } else if (!isInitialized && currentProfile.username.isNotEmpty()) {
            usernameInput = currentProfile.username
            isInitialized = true
        }
    }

    // Auto-hide success message after 3 seconds
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(3000)
            showSuccessMessage = false
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("driver_profile_screen"),
        containerColor = CarbonBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DRIVER PROFILE",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp,
                            color = TextWhite,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "Telemetry & License Management",
                            fontSize = 11.sp,
                            color = ApexCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextWhite
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 1. Hero Identity Card
            item {
                DriverHeroCard(
                    profile = currentProfile,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Edit Username Card (Requested feature)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_edit_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ApexCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = ApexCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Update Driver Callsign",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Your public racing name displayed on leaderboards",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Username Text Field
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { input ->
                                if (input.length <= 24) {
                                    usernameInput = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("Driver Username") },
                            placeholder = { Text("e.g. Apex Pilot, Ghost Racer") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ApexCyan
                                )
                            },
                            singleLine = true,
                            isError = errorMessage != null,
                            supportingText = {
                                if (errorMessage != null) {
                                    Text(
                                        text = errorMessage!!,
                                        color = ApexRed,
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Text(
                                        text = "${usernameInput.trim().length}/24 characters",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ApexCyan,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedLabelColor = ApexCyan,
                                unfocusedLabelColor = TextMuted,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                cursorColor = ApexCyan
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input")
                        )

                        // Save Button
                        Button(
                            onClick = {
                                val trimmed = usernameInput.trim()
                                if (trimmed.isEmpty()) {
                                    errorMessage = "Username cannot be empty"
                                    return@Button
                                }
                                if (trimmed.length < 3) {
                                    errorMessage = "Username must be at least 3 characters"
                                    return@Button
                                }
                                isSaving = true
                                errorMessage = null
                                coroutineScope.launch {
                                    repository.updateUsername(trimmed, currentProfile.id)
                                    isSaving = false
                                    showSuccessMessage = true
                                }
                            },
                            enabled = !isSaving && usernameInput.trim().isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_username_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ApexCyan,
                                contentColor = CarbonBlack,
                                disabledContainerColor = DarkSurface,
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isSaving) "SAVING TO DATABASE..." else "SAVE USERNAME",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        // Success confirmation banner
                        AnimatedVisibility(
                            visible = showSuccessMessage,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ApexGreen.copy(alpha = 0.15f))
                                    .border(1.dp, ApexGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ApexGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Username saved to database successfully!",
                                    color = ApexGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Career Statistics Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = null,
                        tint = ApexGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "CAREER STATS & TELEMETRY",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = TextWhite
                    )
                }
            }

            // 4. Performance Metrics Grid
            item {
                val totalDistanceKm = currentProfile.totalDistance / 1000.0
                val totalDriftKm = currentProfile.totalDriftDistance / 1000.0
                val winPct = if (currentProfile.totalRacesCompleted > 0) {
                    (currentProfile.totalWins.toDouble() / currentProfile.totalRacesCompleted.toDouble()) * 100.0
                } else 0.0
                val podiumPct = if (currentProfile.totalRacesCompleted > 0) {
                    (currentProfile.totalPodiums.toDouble() / currentProfile.totalRacesCompleted.toDouble()) * 100.0
                } else 0.0

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("driver_stats_card")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileStatCard(
                            label = "Total Distance",
                            value = String.format(Locale.US, "%.1f km", totalDistanceKm),
                            subText = "${currentProfile.totalDistance.toLong()} meters logged",
                            icon = Icons.Default.DriveEta,
                            tint = ApexCyan,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            label = "Race Wins",
                            value = "${currentProfile.totalWins}",
                            subText = String.format(Locale.US, "%.1f%% win rate", winPct),
                            icon = Icons.Default.EmojiEvents,
                            tint = ApexGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileStatCard(
                            label = "Races Completed",
                            value = "${currentProfile.totalRacesCompleted}",
                            subText = "Official career events",
                            icon = Icons.Default.QueryStats,
                            tint = ApexPurple,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            label = "Podium Finishes",
                            value = "${currentProfile.totalPodiums}",
                            subText = String.format(Locale.US, "%.1f%% podium rate", podiumPct),
                            icon = Icons.Default.MilitaryTech,
                            tint = ApexOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileStatCard(
                            label = "Highest Speed",
                            value = String.format(Locale.US, "%.0f km/h", currentProfile.highestSpeedKmh),
                            subText = "Top speed telemetry",
                            icon = Icons.Default.Speed,
                            tint = ApexRed,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            label = "Drift Distance",
                            value = String.format(Locale.US, "%.2f km", totalDriftKm),
                            subText = "${currentProfile.totalDriftDistance.toLong()} m angle slide",
                            icon = Icons.Default.Star,
                            tint = ApexCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileStatCard(
                            label = "Clean Races",
                            value = "${currentProfile.cleanRacesCount}",
                            subText = "Zero collision finishes",
                            icon = Icons.Default.Shield,
                            tint = ApexGreen,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            label = "Wet Race Wins",
                            value = "${currentProfile.wetRaceWinsCount}",
                            subText = "Rain & storm victories",
                            icon = Icons.Default.WaterDrop,
                            tint = ApexCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Win Rate Progress Indicator
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val winRatio = if (currentProfile.totalRacesCompleted > 0) {
                            (currentProfile.totalWins.toFloat() / currentProfile.totalRacesCompleted.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Career Victory Ratio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextWhite
                            )
                            Text(
                                text = "${(winRatio * 100f).toInt()}%",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = ApexGold
                            )
                        }

                        LinearProgressIndicator(
                            progress = { winRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ApexGold,
                            trackColor = DarkSurfaceElevated
                        )

                        Text(
                            text = "${currentProfile.totalWins} wins out of ${currentProfile.totalRacesCompleted} total races completed",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // 6. View Full Race History Button
            item {
                Button(
                    onClick = onOpenRaceHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("view_race_history_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = ApexCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ApexCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VIEW FULL RACE HISTORY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DriverHeroCard(
    profile: DriverProfile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(ApexCyan.copy(alpha = 0.5f), SurfaceBorder))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Large Avatar View
            DriverAvatarView(
                avatarId = profile.avatarPath,
                size = AvatarSize.LARGE,
                showEquippedBadgeIndicator = true,
                hasEquippedBadge = profile.equippedBadgeId.isNotEmpty()
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = profile.username,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = TextWhite,
                        maxLines = 1
                    )
                    Text(
                        text = getCountryFlag(profile.country),
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = "ID: ${profile.id} • ${profile.country.uppercase()}",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🪙",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${profile.credits}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ApexGold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "⭐",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${profile.reputation} REP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ApexCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    label: String,
    value: String,
    subText: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite,
                fontFamily = FontFamily.SansSerif
            )

            Text(
                text = subText,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

private fun getCountryFlag(countryCode: String): String {
    return when (countryCode.uppercase()) {
        "US" -> "🇺🇸"
        "JP" -> "🇯🇵"
        "DE" -> "🇩🇪"
        "GB" -> "🇬🇧"
        "IT" -> "🇮🇹"
        "FR" -> "🇫🇷"
        "BR" -> "🇧🇷"
        "CA" -> "🇨🇦"
        "AU" -> "🇦🇺"
        "KR" -> "🇰🇷"
        "ES" -> "🇪🇸"
        "NL" -> "🇳🇱"
        "SE" -> "🇸🇪"
        "MX" -> "🇲🇽"
        else -> "🏁"
    }
}
