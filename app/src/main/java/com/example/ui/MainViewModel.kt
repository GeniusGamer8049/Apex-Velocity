package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ProceduralAudio
import com.example.audio.RacingSoundManager
import com.example.data.ControlScheme
import com.example.data.PlayerProfile
import com.example.data.RacingRepository
import com.example.model.CareerRaceEvent
import com.example.model.CareerRegistry
import com.example.model.DynamicWeatherState
import com.example.model.GhostFrame
import com.example.model.GhostLap
import com.example.model.SurfaceHazardType
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
import com.example.model.Achievement
import com.example.model.ChallengeType
import com.example.model.TelemetryPoint
import com.example.model.PostRaceResult
import com.example.model.CareerObjectiveResult
import com.example.model.RivalFinishResult
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
    RACING,
    POST_RACE
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
    val soundManager = RacingSoundManager()
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
    val postRaceResult = MutableStateFlow<PostRaceResult?>(null)
    val latestUnlockedBadges = MutableStateFlow<List<Achievement>>(emptyList())

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
    private var maxSpeedReached: Float = 0f
    private var sessionDriftMeters: Float = 0f
    private val completedLapTimes = mutableListOf<Long>()

    init {
        // Start procedural audio daemon
        val isMuted = repository.profile.value.isAudioMuted
        soundManager.isMuted = isMuted
        proceduralAudio.isMuted = isMuted
        soundManager.start()
    }

    override fun onCleared() {
        super.onCleared()
        raceLoopJob?.cancel()
        soundManager.stop()
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
        val muted = repository.profile.value.isAudioMuted
        soundManager.isMuted = muted
        proceduralAudio.isMuted = muted
    }

    fun toggleControlScheme() {
        val curr = repository.profile.value.controlScheme
        val next = if (curr == ControlScheme.BUTTONS) ControlScheme.WHEEL else ControlScheme.BUTTONS
        repository.setControlScheme(next)
    }

    fun getGhostLap(trackId: String): GhostLap? = repository.getGhostLap(trackId)

    // --- Achievement & Badge Actions ---
    fun claimAchievement(id: String) = repository.claimAchievementReward(id)
    fun equipBadge(id: String) = repository.equipBadge(id)
    fun dismissUnlockedBadgeAlert() { latestUnlockedBadges.value = emptyList() }

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
        completedLapTimes.clear()
        maxSpeedReached = 0f
        sessionDriftMeters = 0f
        cleanLap = true
        raceElapsedSec = 0f
        isPaused.value = false
        raceResults.value = null
        postRaceResult.value = null
        latestUnlockedBadges.value = emptyList()

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
        soundManager.resume()
        startRaceLoop(isDynamic)
    }

    private fun startRaceLoop(isDynamic: Boolean) {
        raceLoopJob = viewModelScope.launch {
            val dt = 0.016f // 60 FPS update step

            while (isActive) {
                if (!isPaused.value && raceResults.value == null && postRaceResult.value == null) {
                    val pState = playerState.value
                    val vehicle = getCurrentVehicle()
                    val wState = weatherState.value

                    raceElapsedSec += dt
                    if (pState.speedKmh > maxSpeedReached) {
                        maxSpeedReached = pState.speedKmh
                    }

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
                        dt = dt,
                        onCollision = { impact ->
                            soundManager.playCollision(impact)
                            if (impact > 0.35f) {
                                cleanLap = false
                            }
                        }
                    )

                    // 3. AI Racers Update & Bump Collision Detection
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

                        // Car-to-Car contact collision checks
                        for (ai in aiRacers.value) {
                            val dx = pState.x - ai.state.x
                            val dy = pState.y - ai.state.y
                            val distSq = dx * dx + dy * dy
                            val contactDist = 28f
                            if (distSq < contactDist * contactDist && distSq > 0.01f) {
                                val dist = kotlin.math.sqrt(distSq)
                                val nx = dx / dist
                                val ny = dy / dist
                                val overlap = contactDist - dist
                                pState.x += nx * overlap * 0.5f
                                pState.y += ny * overlap * 0.5f
                                ai.state.x -= nx * overlap * 0.5f
                                ai.state.y -= ny * overlap * 0.5f

                                val relSpeed = kotlin.math.sqrt(
                                    (pState.vx - ai.state.vx) * (pState.vx - ai.state.vx) +
                                    (pState.vy - ai.state.vy) * (pState.vy - ai.state.vy)
                                )
                                val impact = (relSpeed / 18f).coerceIn(0.25f, 0.95f)
                                soundManager.playCollision(impact)
                                particleSystem.spawnSparks((pState.x + ai.state.x) * 0.5f, (pState.y + ai.state.y) * 0.5f)
                            }
                        }
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
                        val driftSpeedMs = pState.speedKmh / 3.6f
                        sessionDriftMeters += driftSpeedMs * dt
                    }

                    // 6. Update Sound Manager (Engine Revs, Tire Screech, Immersion SFX)
                    val isKerb = pState.currentSurfaceHazard == SurfaceHazardType.KERB_RUMBLE
                    val isRain = wState.currentWeather == WeatherType.LIGHT_RAIN || wState.currentWeather == WeatherType.THUNDERSTORM

                    soundManager.updateTelemetry(
                        rpm = pState.rpm,
                        maxRpm = vehicle.baseMaxRpm.toFloat(),
                        currentGear = pState.currentGear,
                        throttle = throttleInput,
                        speedKmh = pState.speedKmh,
                        isDrifting = pState.isDrifting,
                        slipAngleDeg = pState.slipAngleDeg,
                        lateralG = pState.lateralG,
                        isBraking = pState.isBraking,
                        isBoosting = pState.isBoosting,
                        isKerbRumble = isKerb,
                        isRaining = isRain
                    )

                    proceduralAudio.targetRpm = pState.rpm
                    proceduralAudio.isDrifting = pState.isDrifting
                    proceduralAudio.isRaining = isRain

                    // 7. Check Lap Completion & Race Finish
                    if (pState.lastLapTimeMs > 0L && (completedLapTimes.isEmpty() || completedLapTimes.size < pState.currentLap - 1)) {
                        completedLapTimes.add(pState.lastLapTimeMs)
                    }

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
        val totalEarnings = basePrize + cleanBonus + driftBonus

        val currProfile = profile.value
        val creditsBefore = currProfile.credits
        val repBefore = currProfile.reputation

        if (event != null) {
            repository.completeRaceEvent(event.id, pos, totalEarnings, repGain)
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

        val unlockedBadges = repository.recordRaceMilestones(
            isWin = pos == 1,
            isClean = cleanLap,
            isWetWin = pos == 1 && isWetRace,
            maxSpeedKmh = maxSpeedReached.coerceAtLeast(pState.speedKmh),
            driftMeters = sessionDriftMeters
        )
        latestUnlockedBadges.value = unlockedBadges

        val vehicle = getCurrentVehicle()
        val currentBest = currProfile.bestLapTimes[activeTrack.id] ?: Long.MAX_VALUE
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

        // Capture total race time and lap list
        val totalRaceTimeMs = (raceElapsedSec * 1000f).toLong().coerceAtLeast(pState.bestLapTimeMs)
        if (completedLapTimes.size < activeTotalLaps && pState.lastLapTimeMs > 0L) {
            completedLapTimes.add(pState.lastLapTimeMs)
        }
        val finalLapTimes = if (completedLapTimes.isNotEmpty()) {
            completedLapTimes.toList()
        } else if (pState.bestLapTimeMs > 0L) {
            listOf(pState.bestLapTimeMs)
        } else {
            listOf(totalRaceTimeMs / activeTotalLaps.coerceAtLeast(1))
        }

        // Compute Career Goals Progression
        val allTiers = CareerRegistry.createDefaultTiers()
        val eventTier = if (event != null) {
            allTiers.find { it.id == event.tierId } ?: allTiers.first()
        } else {
            allTiers.first()
        }

        val completedSet = currProfile.completedEventIds.toMutableSet()
        if (event != null) {
            completedSet.add(event.id)
        }

        val tierCompletedCount = eventTier.events.count { completedSet.contains(it.id) }
        val tierTotalCount = eventTier.events.size
        val isTierTrophyUnlocked = tierCompletedCount >= tierTotalCount

        val currentTierIdx = allTiers.indexOfFirst { it.id == eventTier.id }
        val nextTier = allTiers.getOrNull(currentTierIdx + 1)
        val repAfter = repBefore + repGain
        val creditsAfter = creditsBefore + totalEarnings
        val repProgressToNextTier = if (nextTier != null && nextTier.requiredRep > 0) {
            (repAfter.toFloat() / nextTier.requiredRep.toFloat()).coerceIn(0f, 1f)
        } else 1.0f

        val nextCareerEvent = eventTier.events.firstOrNull { !completedSet.contains(it.id) }
            ?: nextTier?.events?.firstOrNull { !completedSet.contains(it.id) }

        val careerObjectives = mutableListOf<CareerObjectiveResult>()
        if (event != null) {
            careerObjectives.add(
                CareerObjectiveResult(
                    title = "Championship Podium (Top 3)",
                    description = "Finish 1st, 2nd, or 3rd to gain championship reputation",
                    isAchieved = pos <= 3,
                    rewardText = if (pos <= 3) "+$repGain REP" else "0 REP"
                )
            )
            careerObjectives.add(
                CareerObjectiveResult(
                    title = "Event Victory (1st Place)",
                    description = "Take the checkered flag in ${event.title}",
                    isAchieved = pos == 1,
                    rewardText = if (pos == 1) "+$${event.firstPrize} 1st Prize" else "P1 Required"
                )
            )
            careerObjectives.add(
                CareerObjectiveResult(
                    title = "Clean Race Discipline",
                    description = "Complete all laps with zero severe barrier impacts",
                    isAchieved = cleanLap,
                    rewardText = if (cleanLap) "+$800 Purse" else "$0 Penalty"
                )
            )
            careerObjectives.add(
                CareerObjectiveResult(
                    title = "Championship Trophy: ${eventTier.trophyName}",
                    description = "$tierCompletedCount of $tierTotalCount tier events won",
                    isAchieved = isTierTrophyUnlocked,
                    rewardText = if (isTierTrophyUnlocked) "🏆 TROPHY CLAIMED!" else "${tierTotalCount - tierCompletedCount} Races Left"
                )
            )
        } else {
            careerObjectives.add(
                CareerObjectiveResult(
                    title = "Circuit Record Target",
                    description = "Set the fastest all-time lap on ${activeTrack.name}",
                    isAchieved = isNewBest,
                    rewardText = if (isNewBest) "★ NEW RECORD" else "P.B. Target"
                )
            )
            careerObjectives.add(
                CareerObjectiveResult(
                    title = "Clean Lap Performance",
                    description = "No grass or wall contacts throughout the session",
                    isAchieved = cleanLap,
                    rewardText = if (cleanLap) "+$800 Purse" else "Contact Incurred"
                )
            )
        }

        // Compute Official Rivals Grid
        val rivals = mutableListOf<RivalFinishResult>()
        val pTime = totalRaceTimeMs
        rivals.add(
            RivalFinishResult(
                position = pos,
                name = currProfile.driverName,
                vehicleName = vehicle.name,
                totalTimeMs = pTime,
                deltaFormatted = if (pos == 1) "LEADER" else "+${formatDelta((pos - 1) * 1250L + 540L)}",
                isPlayer = true
            )
        )

        var aiRank = 0
        for (ai in aiRacers.value) {
            aiRank++
            val place = if (aiRank < pos) aiRank else aiRank + 1
            val deltaMs = if (place < pos) {
                -((pos - place) * 1250L + 540L)
            } else {
                (place - pos) * 1380L + 620L
            }
            val aiTime = (pTime + deltaMs).coerceAtLeast(15000L)
            val deltaStr = if (place == 1) "LEADER" else "+${formatDelta(kotlin.math.abs(deltaMs))}"
            rivals.add(
                RivalFinishResult(
                    position = place,
                    name = ai.name,
                    vehicleName = ai.vehicle.name,
                    totalTimeMs = aiTime,
                    deltaFormatted = deltaStr,
                    isPlayer = false
                )
            )
        }
        rivals.sortBy { it.position }

        val postResult = PostRaceResult(
            position = pos,
            totalRacers = aiRacers.value.size + 1,
            totalRaceTimeMs = totalRaceTimeMs,
            bestLapTimeMs = pState.bestLapTimeMs,
            isNewBest = isNewBest,
            lapTimesMs = finalLapTimes,
            topSpeedKmh = maxSpeedReached.coerceAtLeast(pState.speedKmh),
            driftScore = pState.driftScore,
            isCleanRace = cleanLap,
            basePrize = basePrize,
            cleanBonus = cleanBonus,
            driftBonus = driftBonus,
            repEarned = repGain,
            totalEarnings = totalEarnings,
            creditsBefore = creditsBefore,
            creditsAfter = creditsAfter,
            repBefore = repBefore,
            repAfter = repAfter,
            isTimeTrial = isTimeTrial,
            trackId = activeTrack.id,
            trackName = activeTrack.name,
            vehicleName = vehicle.name,
            careerEvent = event,
            tierName = eventTier.name,
            tierSubtitle = eventTier.subtitle,
            tierTrophyName = eventTier.trophyName,
            tierCompletedEvents = tierCompletedCount,
            tierTotalEvents = tierTotalCount,
            isTierTrophyUnlocked = isTierTrophyUnlocked,
            nextTierName = nextTier?.name,
            nextTierRequiredRep = nextTier?.requiredRep,
            repProgressToNextTier = repProgressToNextTier,
            nextCareerEvent = nextCareerEvent,
            careerObjectives = careerObjectives,
            rivalResults = rivals,
            dailyChallengesUpdated = repository.profile.value.dailyChallenges,
            unlockedBadges = unlockedBadges
        )

        postRaceResult.value = postResult

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

        soundManager.pause()
        _currentScreen.value = AppScreen.POST_RACE
    }

    private fun formatDelta(deltaMs: Long): String {
        val sec = deltaMs / 1000f
        return String.format(java.util.Locale.US, "%.2fs", kotlin.math.abs(sec))
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
        soundManager.pause()
    }

    fun resumeRace() {
        isPaused.value = false
        soundManager.resume()
    }

    fun pitStopTireSwap(compound: TireCompound) {
        val v = getCurrentVehicle()
        repository.equipTireCompound(v.id, compound)
    }

    fun restartRace() {
        soundManager.resume()
        if (isTimeTrial) {
            startTimeTrial(activeTrack, weatherState.value.currentWeather, activeGhostLap.value != null)
        } else if (activeCareerEvent != null) {
            startCareerRace(activeCareerEvent!!)
        }
    }

    fun stopRace() {
        raceLoopJob?.cancel()
        particleSystem.clear()
        soundManager.updateTelemetry(rpm = 900f, speedKmh = 0f, throttle = 0f)
        soundManager.pause()
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
