package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.dialogs.PauseMenuDialog
import com.example.ui.dialogs.RaceSummaryDialog
import com.example.ui.game.GameCanvas
import com.example.ui.game.RaceHud
import com.example.ui.theme.CarbonBlack

@Composable
fun RaceScreen(
    viewModel: MainViewModel,
    onBackToMenu: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val aiRacers by viewModel.aiRacers.collectAsState()
    val ghostFrame by viewModel.ghostFrame.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val raceResults by viewModel.raceResults.collectAsState()
    val telemetryHistory by viewModel.telemetryHistory.collectAsState()

    var showTelemetryScreen by remember { mutableStateOf(false) }

    val currentVehicle = viewModel.getCurrentVehicle()
    val currentPosition = viewModel.calculatePlayerPosition()

    BackHandler {
        if (showTelemetryScreen) {
            showTelemetryScreen = false
        } else if (!isPaused && raceResults == null) {
            viewModel.pauseRace()
        } else {
            viewModel.stopRace()
            onBackToMenu()
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.soundManager.resume()
        onDispose {
            viewModel.soundManager.pause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
    ) {
        // 1. Hardware Accelerated Game Canvas
        GameCanvas(
            track = viewModel.activeTrack,
            playerVehicle = currentVehicle,
            playerState = playerState,
            aiRacers = aiRacers,
            ghostFrame = ghostFrame,
            currentWeather = weatherState.currentWeather,
            trackWetness = weatherState.trackWetness,
            lightningAlpha = weatherState.lightningFlashAlpha,
            particles = viewModel.particleSystem.particles,
            skidMarks = viewModel.particleSystem.skidMarks
        )

        // 2. Interactive Race HUD
        RaceHud(
            playerState = playerState,
            weatherState = weatherState,
            totalLaps = viewModel.activeTotalLaps,
            playerPosition = currentPosition,
            totalRacers = aiRacers.size + 1,
            isTimeTrial = viewModel.isTimeTrial,
            ghostFrame = ghostFrame,
            controlScheme = profile.controlScheme,
            tireCompound = currentVehicle.upgrades.tireCompound,
            isMuted = profile.isAudioMuted,
            onToggleAudio = { viewModel.toggleAudio() },
            onThrottleChange = { viewModel.throttleInput = it },
            onBrakeChange = { viewModel.brakeInput = it },
            onSteerChange = { viewModel.steerInput = it },
            onHandbrakeChange = { viewModel.handbrakeInput = it },
            onBoostChange = { viewModel.boostInput = it },
            onPauseClick = { viewModel.pauseRace() }
        )

        // 3. Pause & Pit Stop Dialog
        if (isPaused && raceResults == null && !showTelemetryScreen) {
            PauseMenuDialog(
                currentCompound = currentVehicle.upgrades.tireCompound,
                trackWetness = weatherState.trackWetness,
                isMuted = profile.isAudioMuted,
                controlScheme = profile.controlScheme,
                onResume = { viewModel.resumeRace() },
                onPitStopTireSwap = { viewModel.pitStopTireSwap(it) },
                onToggleAudio = { viewModel.toggleAudio() },
                onToggleControls = { viewModel.toggleControlScheme() },
                onOpenTelemetry = { showTelemetryScreen = true },
                onRestart = { viewModel.restartRace() },
                onExit = {
                    viewModel.stopRace()
                    onBackToMenu()
                }
            )
        }

        // 4. Live Telemetry & Performance Screen Overlay
        if (showTelemetryScreen) {
            TelemetryScreen(
                vehicle = currentVehicle,
                telemetryHistory = telemetryHistory,
                onBack = { showTelemetryScreen = false }
            )
        }

        // 5. Post-Race Summary Screen
        val postResult by viewModel.postRaceResult.collectAsState()
        if (postResult != null) {
            PostRaceSummaryScreen(
                result = postResult!!,
                onContinueToCareer = { viewModel.navigateTo(AppScreen.CAREER) },
                onRetryRace = { viewModel.restartRace() },
                onStartNextEvent = { nextEvent -> viewModel.startCareerRace(nextEvent) },
                onGoToGarage = { viewModel.navigateTo(AppScreen.GARAGE) },
                onViewLeaderboard = { trackId ->
                    viewModel.selectLeaderboardTrack(trackId)
                    viewModel.navigateTo(AppScreen.LEADERBOARD)
                }
            )
        } else if (raceResults != null) {
            val summary = raceResults!!
            RaceSummaryDialog(
                results = summary,
                onContinue = {
                    viewModel.stopRace()
                    onBackToMenu()
                },
                onRetry = { viewModel.restartRace() },
                onViewLeaderboard = {
                    viewModel.stopRace()
                    viewModel.selectLeaderboardTrack(summary.trackId)
                    viewModel.navigateTo(AppScreen.LEADERBOARD)
                }
            )
        }
    }
}
