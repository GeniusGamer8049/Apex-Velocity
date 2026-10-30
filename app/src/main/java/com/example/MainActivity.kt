package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.CareerScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.RaceScreen
import com.example.ui.screens.TimeTrialScreen
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                RacingApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RacingApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val profile by viewModel.profile.collectAsState()

    if (currentScreen == AppScreen.RACING) {
        // Fullscreen edge-to-edge immersive race experience
        RaceScreen(
            viewModel = viewModel,
            onBackToMenu = { viewModel.navigateTo(AppScreen.GARAGE) }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextWhite,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.GARAGE,
                        onClick = { viewModel.navigateTo(AppScreen.GARAGE) },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Garage") },
                        label = { Text("Garage", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlack,
                            selectedTextColor = ApexCyan,
                            indicatorColor = ApexCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_garage")
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.CAREER,
                        onClick = { viewModel.navigateTo(AppScreen.CAREER) },
                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Career") },
                        label = { Text("Career", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlack,
                            selectedTextColor = ApexCyan,
                            indicatorColor = ApexCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_career")
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.TIME_TRIAL,
                        onClick = { viewModel.navigateTo(AppScreen.TIME_TRIAL) },
                        icon = { Icon(Icons.Default.Timer, contentDescription = "Time Trial") },
                        label = { Text("Time Trial", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlack,
                            selectedTextColor = ApexCyan,
                            indicatorColor = ApexCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_time_trial")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CarbonBlack)
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                when (currentScreen) {
                    AppScreen.GARAGE -> {
                        GarageScreen(
                            profile = profile,
                            onSelectVehicle = { viewModel.selectVehicle(it) },
                            onPurchaseVehicle = { viewModel.purchaseVehicle(it) },
                            onUpgradeVehicle = { vid, cat -> viewModel.upgradeVehicle(vid, cat) },
                            onEquipTireCompound = { vid, comp -> viewModel.equipTireCompound(vid, comp) },
                            onUpdateCustomization = { vid, color, underglow ->
                                viewModel.updateVehicleCustomization(vid, color, underglow)
                            }
                        )
                    }

                    AppScreen.CAREER -> {
                        CareerScreen(
                            profile = profile,
                            onStartCareerRace = { viewModel.startCareerRace(it) }
                        )
                    }

                    AppScreen.TIME_TRIAL -> {
                        TimeTrialScreen(
                            profile = profile,
                            ghostLapProvider = { viewModel.getGhostLap(it) },
                            onStartTimeTrial = { track, weather, ghost ->
                                viewModel.startTimeTrial(track, weather, ghost)
                            }
                        )
                    }

                    AppScreen.RACING -> {
                        // Handled above in root branch
                    }
                }
            }
        }
    }
}
