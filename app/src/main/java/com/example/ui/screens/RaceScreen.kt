package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    val currentVehicle = viewModel.getCurrentVehicle()
    val currentPosition = viewModel.calculatePlayerPosition()

    BackHandler {
        if (!isPaused && raceResults == null) {
            viewModel.pauseRace()
        } else {
            viewModel.stopRace()
            onBackToMenu()
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
            onThrottleChange = { viewModel.throttleInput = it },
            onBrakeChange = { viewModel.brakeInput = it },
            onSteerChange = { viewModel.steerInput = it },
            onHandbrakeChange = { viewModel.handbrakeInput = it },
            onBoostChange = { viewModel.boostInput = it },
            onPauseClick = { viewModel.pauseRace() }
        )

        // 3. Pause & Pit Stop Dialog
        if (isPaused && raceResults == null) {
            PauseMenuDialog(
                currentCompound = currentVehicle.upgrades.tireCompound,
                trackWetness = weatherState.trackWetness,
                isMuted = profile.isAudioMuted,
                controlScheme = profile.controlScheme,
                onResume = { viewModel.resumeRace() },
                onPitStopTireSwap = { viewModel.pitStopTireSwap(it) },
                onToggleAudio = { viewModel.toggleAudio() },
                onToggleControls = { viewModel.toggleControlScheme() },
                onRestart = { viewModel.restartRace() },
                onExit = {
                    viewModel.stopRace()
                    onBackToMenu()
                }
            )
        }

        // 4. Race Results Summary Dialog
        if (raceResults != null) {
            RaceSummaryDialog(
                results = raceResults!!,
                onContinue = {
                    viewModel.stopRace()
                    onBackToMenu()
                },
                onRetry = { viewModel.restartRace() }
            )
        }
    }
}
