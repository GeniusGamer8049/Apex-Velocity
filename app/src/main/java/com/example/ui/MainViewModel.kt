package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ProceduralAudio
import com.example.data.ControlScheme
import com.example.data.PlayerProfile
import com.example.data.RacingRepository
import com.example.model.CareerRaceEvent
import com.example.model.CareerRegistry
import com.example.model.DynamicWeatherState
import com.example.model.GhostFrame
import com.example.model.GhostLap
import com.example.model.TireCompound
import com.example.model.Track
import com.example.model.TrackRegistry
import com.example.model.Vehicle
import com.example.model.WeatherType
import com.example.physics.AiOpponentManager
import com.example.physics.AiRacer
import com.example.physics.CarPhysicsState
import com.example.physics.ParticleSystem
import com.example.physics.PhysicsEngine
import com.example.model.ChallengeType
import com.example.model.TelemetryPoint
import com.example.ui.dialogs.RaceResults
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AppScreen {
    GARAGE,
    CAREER,
    TIME_TRIAL,
    LEADERBOARD,
    TELEMETRY,
    RACING
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RacingRepository(application)
    val profile: StateFlow<PlayerProfile> = repository.profile

    private val _currentScreen = MutableStateFlow(AppScreen.GARAGE)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Physics & Simulation Engines
    private val physicsEngine = PhysicsEngine()
    private val aiOpponentManager = AiOpponentManager()
    val particleSystem = ParticleSystem()
    val proceduralAudio = ProceduralAudio()

    // Active Race State
    var activeTrack: Track = TrackRegistry.allTracks.first()
        private set
    var activeTotalLaps: Int = 2
        private set
    var isTimeTrial: Boolean = false
        private set
    private var activeCareerEvent: CareerRaceEvent? = null

    val playerState = MutableStateFlow(CarPhysicsState(0f, 0f))
    val aiRacers = MutableStateFlow<List<AiRacer>>(emptyList())
    val ghostFrame = MutableStateFlow<GhostFrame?>(null)
    val activeGhostLap = MutableStateFlow<GhostLap?>(null)
    val weatherState = MutableStateFlow(DynamicWeatherState())
    val isPaused = MutableStateFlow(false)
    val raceResults = MutableStateFlow<RaceResults?>(null)

    private val _telemetryHistory = MutableStateFlow<List<TelemetryPoint>>(createInitialTelemetry())
    val telemetryHistory: StateFlow<List<TelemetryPoint>> = _telemetryHistory.asStateFlow()
    private var telemetrySampleAccumulator: Float = 0f

    val leaderboardSelectedTrackId = MutableStateFlow(TrackRegistry.allTracks.first().id)

    // Player inputs
    var throttleInput: Float = 0f
    var brakeInput: Float = 0f
    var steerInput: Float = 0f
    var handbrakeInput: Boolean = false
    var boostInput: Boolean = false

    private var raceLoopJob: Job? = null
    private var raceElapsedSec: Float = 0f
    private val recordedLapFrames = mutableListOf<GhostFrame>()
    private var cleanLap: Boolean = true

    init {
        // Start procedural audio daemon
        proceduralAudio.isMuted = repository.profile.value.isAudioMuted
        proceduralAudio.start()
    }

    override fun onCleared() {
        super.onCleared()
        raceLoopJob?.cancel()
        proceduralAudio.stop()
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value == AppScreen.RACING && screen != AppScreen.RACING) {
            stopRace()
        }
        _currentScreen.value = screen
    }

    // --- Garage Actions ---
    fun selectVehicle(vehicleId: String) = repository.selectVehicle(vehicleId)
    fun purchaseVehicle(vehicleId: String) = repository.purchaseVehicle(vehicleId)
    fun upgradeVehicle(vehicleId: String, category: String) = repository.upgradeVehicle(vehicleId, category)
    fun equipTireCompound(vehicleId: String, compound: TireCompound) = repository.equipTireCompound(vehicleId, compound)
    fun updateVehicleCustomization(vehicleId: String, colorHex: String, underglowHex: String?) =
        repository.updateVehicleCustomization(vehicleId, colorHex, underglowHex)

    fun toggleAudio() {
        repository.toggleAudio()
        proceduralAudio.isMuted = repository.profile.value.isAudioMuted
    }

    fun toggleControlScheme() {
        val curr = repository.profile.value.controlScheme
        val next = if (curr == ControlScheme.BUTTONS) ControlScheme.WHEEL else ControlScheme.BUTTONS
        repository.setControlScheme(next)
    }

    fun getGhostLap(trackId: String): GhostLap? = repository.getGhostLap(trackId)

    // --- Race Launching ---
    fun startCareerRace(event: CareerRaceEvent) {
        val track = TrackRegistry.getTrackById(event.trackId)
        activeTrack = track
        activeTotalLaps = event.laps
        isTimeTrial = false
        activeCareerEvent = event
        activeGhostLap.value = null

        launchRace(track, track.defaultWeather, event.dynamicWeather)
    }

    fun startTimeTrial(track: Track, weather: WeatherType, enableGhost: Boolean) {
        activeTrack = track
        activeTotalLaps = 99 // Endless hot laps in Time Trial
        isTimeTrial = true
        activeCareerEvent = null
        activeGhostLap.value = if (enableGhost) repository.getGhostLap(track.id) else null

        launchRace(track, weather, isDynamic = false)
    }

    private fun launchRace(track: Track, initialWeather: WeatherType, isDynamic: Boolean) {
        raceLoopJob?.cancel()
        particleSystem.clear()
        recordedLapFrames.clear()
        cleanLap = true
        raceElapsedSec = 0f
        isPaused.value = false
        raceResults.value = null

        val startWp = track.startWaypoint
        playerState.value = CarPhysicsState(
            x = startWp.x,
            y = startWp.y,
            headingDeg = 0f,
            currentGear = 1
        )

        val playerVehicle = getCurrentVehicle()

        // Spawn AI opponents if career race
        if (!isTimeTrial) {
            val aiCount = when (activeCareerEvent?.tierId) {
                "tier_1" -> 3
                "tier_2" -> 3
                else -> 4
            }
            aiRacers.value = aiOpponentManager.createOpponents(track, playerVehicle, count = aiCount)
        } else {
            aiRacers.value = emptyList()
        }

        // Initialize Dynamic Weather
        weatherState.value = DynamicWeatherState(
            currentWeather = initialWeather,
            trackWetness = if (initialWeather == WeatherType.SUNNY_CLEAR) 0f else if (initialWeather == WeatherType.LIGHT_RAIN) 0.5f else 0.85f,
            forecastWeather = initialWeather
        )

        _currentScreen.value = AppScreen.RACING
        startRaceLoop(isDynamic)
    }

    private fun startRaceLoop(isDynamic: Boolean) {
        raceLoopJob = viewModelScope.launch {
            val dt = 0.016f // 60 FPS update step

            while (isActive) {
                if (!isPaused.value && raceResults.value == null) {
                    val pState = playerState.value
                    val vehicle = getCurrentVehicle()
                    val wState = weatherState.value

                    raceElapsedSec += dt

                    // 1. Dynamic Weather Progression
                    if (isDynamic && activeTrack.dynamicWeatherProgression.isNotEmpty()) {
                        updateDynamicWeather(raceElapsedSec, dt)
                    }

                    // 2. Physics Engine Update for Player
                    physicsEngine.updatePlayer(
                        state = pState,
                        vehicle = vehicle,
                        track = activeTrack,
                        currentWeather = wState.currentWeather,
                        trackWetness = wState.trackWetness,
                        throttleInput = throttleInput,
                        brakeInput = brakeInput,
                        steerInput = steerInput,
                        handbrakeInput = handbrakeInput,
                        boostInput = boostInput,
                        particleSystem = particleSystem,
                        dt = dt
                    )

                    // 3. AI Racers Update
                    if (!isTimeTrial && aiRacers.value.isNotEmpty()) {
                        aiOpponentManager.updateAiRacers(
                            racers = aiRacers.value,
                            track = activeTrack,
                            currentWeather = wState.currentWeather,
                            trackWetness = wState.trackWetness,
                            playerState = pState,
                            particleSystem = particleSystem,
                            dt = dt
                        )
                    }

                    // 4. Ghost Car Sample & Player Frame Recording
                    val currentGhost = activeGhostLap.value
                    if (currentGhost != null) {
                        ghostFrame.value = currentGhost.sampleAt(pState.currentLapTimeMs)
                    } else {
                        ghostFrame.value = null
                    }

                    // Record player lap for new ghost
                    recordedLapFrames.add(
                        GhostFrame(
                            timeMs = pState.currentLapTimeMs,
                            x = pState.x,
                            y = pState.y,
                            heading = pState.headingDeg,
                            speedKmh = pState.speedKmh,
                            isBraking = pState.isBraking
                        )
                    )

                    // 5. Update Particles
                    particleSystem.update(dt)

                    // 5b. Sample Real-Time Telemetry
                    telemetrySampleAccumulator += dt
                    if (telemetrySampleAccumulator >= 0.08f) {
                        telemetrySampleAccumulator = 0f
                        val hist = _telemetryHistory.value.toMutableList()
                        if (hist.size >= 80) hist.removeAt(0)
                        hist.add(
                            TelemetryPoint(
                                timeSec = raceElapsedSec,
                                speedKmh = pState.speedKmh,
                                rpm = pState.rpm,
                                throttle = throttleInput,
                                brake = brakeInput,
                                lateralG = pState.lateralG,
                                longitudinalG = pState.longitudinalG,
                                tempFL = pState.tireTempFL,
                                tempFR = pState.tireTempFR,
                                tempRL = pState.tireTempRL,
                                tempRR = pState.tireTempRR,
                                gear = pState.currentGear,
                                downforceN = pState.downforceN
                            )
                        )
                        _telemetryHistory.value = hist
                    }

                    // 5c. Track Real-Time Daily Challenges
                    if (pState.speedKmh > 100f) {
                        repository.updateChallengeProgress(ChallengeType.TOP_SPEED, absoluteValue = pState.speedKmh.toInt())
                    }
                    if (pState.isDrifting) {
                        repository.updateChallengeProgress(ChallengeType.DRIFT_POINTS, addValue = (abs(pState.slipAngleDeg) * 0.25f).toInt())
                    }

                    // 6. Update Procedural Audio
                    proceduralAudio.targetRpm = pState.rpm
                    proceduralAudio.isDrifting = pState.isDrifting
                    proceduralAudio.isRaining = wState.currentWeather == WeatherType.LIGHT_RAIN || wState.currentWeather == WeatherType.THUNDERSTORM

                    // 7. Check Lap Completion & Race Finish
                    if (pState.currentLap > activeTotalLaps && !isTimeTrial) {
                        handleRaceFinish(pState)
                    }

                    // In time trial, when completing a lap, check for record
                    if (pState.lastLapTimeMs > 0L) {
                        val pb = profile.value.bestLapTimes[activeTrack.id] ?: Long.MAX_VALUE
                        if (pState.lastLapTimeMs < pb) {
                            val newGhost = GhostLap(
                                trackId = activeTrack.id,
                                carName = vehicle.name,
                                lapTimeMs = pState.lastLapTimeMs,
                                frames = recordedLapFrames.toList()
                            )
                            repository.recordBestLap(activeTrack.id, pState.lastLapTimeMs, newGhost, vehicle.name)
                            activeGhostLap.value = newGhost
                        }
                        recordedLapFrames.clear()
                    }
                }

                delay(16)
            }
        }
    }

    private fun updateDynamicWeather(elapsedSec: Float, dt: Float) {
        val schedule = activeTrack.dynamicWeatherProgression
        var targetWeather = schedule.first().second
        var nextWeather = targetWeather
        var secToNext = 0

        for (i in schedule.indices) {
            val (t, w) = schedule[i]
            if (elapsedSec >= t) {
                targetWeather = w
                if (i + 1 < schedule.size) {
                    nextWeather = schedule[i + 1].second
                    secToNext = (schedule[i + 1].first - elapsedSec).toInt().coerceAtLeast(0)
                }
            }
        }

        val currW = weatherState.value
        // Dynamic wetness transition
        val targetWetness = when (targetWeather) {
            WeatherType.SUNNY_CLEAR -> 0.0f
            WeatherType.OVERCAST -> 0.15f
            WeatherType.LIGHT_RAIN -> 0.65f
            WeatherType.THUNDERSTORM -> 1.0f
            WeatherType.FOG_MIST -> 0.40f
            WeatherType.FREEZING_ICE -> 0.75f
        }

        val newWetness = currW.trackWetness + (targetWetness - currW.trackWetness) * (0.05f * dt)

        // Random lightning flash in thunderstorm
        val lightningAlpha = if (targetWeather == WeatherType.THUNDERSTORM && Random.nextFloat() < 0.015f) {
            0.65f
        } else {
            (currW.lightningFlashAlpha - 2f * dt).coerceAtLeast(0f)
        }

        weatherState.value = currW.copy(
            currentWeather = targetWeather,
            trackWetness = newWetness.coerceIn(0f, 1f),
            forecastWeather = nextWeather,
            secondsToTransition = secToNext,
            lightningFlashAlpha = lightningAlpha
        )
    }

    private fun handleRaceFinish(pState: CarPhysicsState) {
        pState.raceFinished = true

        // Compute final position compared to AI
        var pos = 1
        for (ai in aiRacers.value) {
            if (ai.state.currentLap > pState.currentLap ||
                (ai.state.currentLap == pState.currentLap && ai.state.lapProgressPct > pState.lapProgressPct)
            ) {
                pos++
            }
        }

        val event = activeCareerEvent
        val basePrize = if (event != null) {
            when (pos) {
                1 -> event.firstPrize
                2 -> event.secondPrize
                3 -> event.thirdPrize
                else -> (event.entryFee * 0.5f).toInt()
            }
        } else 0

        val cleanBonus = if (cleanLap) 800 else 0
        val driftBonus = (pState.driftScore * 0.5f).toInt()
        val repGain = if (event != null && pos <= 3) event.repReward else 20

        if (event != null) {
            repository.completeRaceEvent(event.id, pos, basePrize + cleanBonus + driftBonus, repGain)
        }

        // Daily Challenge Progress on Finish
        if (cleanLap) {
            repository.updateChallengeProgress(ChallengeType.CLEAN_RACE, addValue = 1)
        }
        if (pos <= 2) {
            repository.updateChallengeProgress(ChallengeType.PODIUM_FINISH, addValue = 1)
        }
        val isWetRace = (weatherState.value.currentWeather == WeatherType.LIGHT_RAIN ||
                weatherState.value.currentWeather == WeatherType.THUNDERSTORM ||
                weatherState.value.trackWetness > 0.4f)
        if (pos == 1 && isWetRace) {
            repository.updateChallengeProgress(ChallengeType.WET_RACE_WIN, addValue = 1)
        }
        repository.updateChallengeProgress(ChallengeType.COMPLETE_LAPS, addValue = activeTotalLaps)

        val vehicle = getCurrentVehicle()
        val currentBest = profile.value.bestLapTimes[activeTrack.id] ?: Long.MAX_VALUE
        val isNewBest = pState.bestLapTimeMs in 1 until currentBest
        if (isNewBest) {
            val newGhost = if (recordedLapFrames.isNotEmpty()) {
                GhostLap(
                    trackId = activeTrack.id,
                    carName = vehicle.name,
                    lapTimeMs = pState.bestLapTimeMs,
                    frames = recordedLapFrames.toList()
                )
            } else null
            repository.recordBestLap(activeTrack.id, pState.bestLapTimeMs, newGhost, vehicle.name)
        }

        val trackLeaderboard = repository.getLeaderboard(activeTrack.id)
        val rankOnBoard = trackLeaderboard.find { it.isPlayer }?.rank

        raceResults.value = RaceResults(
            position = pos,
            totalRacers = aiRacers.value.size + 1,
            bestLapTimeMs = pState.bestLapTimeMs,
            isNewBest = isNewBest,
            basePrize = basePrize,
            cleanBonus = cleanBonus,
            driftBonus = driftBonus,
            repEarned = repGain,
            isTimeTrial = false,
            globalRank = rankOnBoard,
            trackId = activeTrack.id
        )
    }

    fun selectLeaderboardTrack(trackId: String) {
        leaderboardSelectedTrackId.value = trackId
    }

    fun updateDriverProfile(name: String, country: String) {
        repository.updateDriverProfile(name, country)
    }

    fun getLeaderboard(trackId: String) = repository.getLeaderboard(trackId)

    fun claimChallenge(challengeId: String) = repository.claimChallengeReward(challengeId)
    fun claimAllDailyBonus() = repository.claimAllDailyBonus()

    fun pauseRace() {
        isPaused.value = true
    }

    fun resumeRace() {
        isPaused.value = false
    }

    fun pitStopTireSwap(compound: TireCompound) {
        val v = getCurrentVehicle()
        repository.equipTireCompound(v.id, compound)
    }

    fun restartRace() {
        if (isTimeTrial) {
            startTimeTrial(activeTrack, weatherState.value.currentWeather, activeGhostLap.value != null)
        } else if (activeCareerEvent != null) {
            startCareerRace(activeCareerEvent!!)
        }
    }

    fun stopRace() {
        raceLoopJob?.cancel()
        particleSystem.clear()
        proceduralAudio.targetRpm = 1000f
        proceduralAudio.isDrifting = false
        proceduralAudio.isRaining = false
        raceResults.value = null
        isPaused.value = false
        _currentScreen.value = if (isTimeTrial) AppScreen.TIME_TRIAL else AppScreen.CAREER
    }

    fun getCurrentVehicle(): Vehicle {
        val p = profile.value
        return p.vehicles.find { it.id == p.selectedVehicleId } ?: p.vehicles.first()
    }

    fun calculatePlayerPosition(): Int {
        if (isTimeTrial) return 1
        var pos = 1
        val p = playerState.value
        for (ai in aiRacers.value) {
            if (ai.state.currentLap > p.currentLap ||
                (ai.state.currentLap == p.currentLap && ai.state.lapProgressPct > p.lapProgressPct)
            ) {
                pos++
            }
        }
        return pos
    }

    private fun createInitialTelemetry(): List<TelemetryPoint> {
        val list = mutableListOf<TelemetryPoint>()
        for (i in 0 until 40) {
            val t = i * 0.1f
            val spd = 120f + kotlin.math.sin(t * 1.5).toFloat() * 60f
            val rpm = 4500f + kotlin.math.sin(t * 1.5).toFloat() * 2200f
            val lat = kotlin.math.abs(kotlin.math.sin(t * 1.2)).toFloat() * 1.25f
            list.add(
                TelemetryPoint(
                    timeSec = t,
                    speedKmh = spd,
                    rpm = rpm,
                    throttle = (0.5f + kotlin.math.sin(t).toFloat() * 0.5f).coerceIn(0f, 1f),
                    brake = if (spd < 90f) 0.8f else 0f,
                    lateralG = lat,
                    longitudinalG = (kotlin.math.cos(t * 1.5).toFloat() * 0.8f),
                    tempFL = 86f + kotlin.math.sin(t).toFloat() * 8f,
                    tempFR = 84f + kotlin.math.cos(t).toFloat() * 6f,
                    tempRL = 91f + kotlin.math.sin(t * 1.3).toFloat() * 7f,
                    tempRR = 89f + kotlin.math.cos(t * 1.3).toFloat() * 7f,
                    gear = (spd / 45f).toInt().coerceIn(1, 5),
                    downforceN = spd * 1.4f
                )
            )
        }
        return list
    }
}
