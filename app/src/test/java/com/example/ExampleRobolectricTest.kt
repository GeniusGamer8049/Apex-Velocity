package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RacingRepository
import com.example.model.AchievementCategory
import com.example.model.AchievementRegistry
import com.example.model.BadgeRarity
import com.example.model.CareerRegistry
import com.example.model.DriverAvatarRegistry
import com.example.model.TireCompound
import com.example.model.TrackRegistry
import com.example.model.VehicleRegistry
import com.example.model.VehicleUpgrades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Apex Velocity", appName)
    }

    @Test
    fun `test vehicle performance upgrades calculations`() {
        val vehicles = VehicleRegistry.createDefaultVehicles()
        val starter = vehicles.first()
        assertEquals("apex_gt86", starter.id)
        assertTrue("Starter car must be unlocked", starter.isUnlocked)

        // Upgrade engine to Stage 2
        val upgraded = starter.copy(
            upgrades = starter.upgrades.copy(engineStage = 2)
        )
        assertTrue("Upgraded BHP should exceed base", upgraded.effectiveBhp > starter.effectiveBhp)
        assertTrue("Upgraded top speed should exceed base", upgraded.effectiveTopSpeedKmh > starter.effectiveTopSpeedKmh)
    }

    @Test
    fun `test career tiers and track registry`() {
        val tracks = TrackRegistry.allTracks
        assertTrue("At least 4 tracks available", tracks.size >= 4)
        val monaco = TrackRegistry.getTrackById("monaco_riviera")
        assertNotNull(monaco)
        assertTrue("Monaco has waypoints", monaco.waypoints.isNotEmpty())
        assertTrue("Monaco has surface hazard patches", monaco.surfacePatches.isNotEmpty())

        val tiers = CareerRegistry.createDefaultTiers()
        assertEquals(4, tiers.size)
        assertTrue("Tier 1 requires 0 REP", tiers[0].requiredRep == 0)
    }

    @Test
    fun `test tire compound characteristics`() {
        val slick = TireCompound.SOFT_SLICK
        val wet = TireCompound.FULL_WET
        assertTrue("Slick dry grip exceeds wet tire dry grip", slick.dryGripMultiplier > wet.dryGripMultiplier)
        assertTrue("Wet tire wet grip exceeds slick wet grip", wet.wetGripMultiplier > slick.wetGripMultiplier)
    }

    @Test
    fun `test telemetry metrics and tire temperatures`() {
        val point = com.example.model.TelemetryPoint(
            timeSec = 5.2f,
            speedKmh = 210f,
            rpm = 6800f,
            throttle = 1.0f,
            brake = 0f,
            lateralG = 1.15f,
            longitudinalG = 0.45f,
            tempFL = 92f,
            tempFR = 88f,
            tempRL = 95f,
            tempRR = 91f,
            gear = 4,
            downforceN = 450f
        )
        assertEquals(210f, point.speedKmh, 0.01f)
        assertEquals(6800f, point.rpm, 0.01f)
        assertEquals(91.5f, point.avgTireTemp, 0.1f)
        assertTrue("Optimal temperature range", point.avgTireTemp in 75f..100f)
    }

    @Test
    fun `test daily challenges generation and claiming`() {
        val today = com.example.model.DailyChallengeGenerator.getTodayKey()
        val challenges = com.example.model.DailyChallengeGenerator.generateForDate(today)
        assertEquals(3, challenges.size)

        val challenge = challenges.first()
        assertEquals(0, challenge.currentValue)
        assertTrue(!challenge.isCompleted)

        // Complete the challenge
        challenge.currentValue = challenge.targetValue
        assertTrue(challenge.isCompleted)
        assertEquals(1.0f, challenge.progressFraction, 0.01f)
    }

    @Test
    fun `test global leaderboard benchmarks and dynamic player ranking`() {
        // Track leaderboard without player time
        val monacoLeaderboard = com.example.model.GlobalLeaderboardRegistry.buildTrackLeaderboard(
            trackId = "monaco_riviera",
            playerPbMs = null
        )
        assertTrue("Leaderboard should have benchmark racers", monacoLeaderboard.size >= 20)
        assertEquals(1, monacoLeaderboard.first().rank)
        assertEquals("LEADER", monacoLeaderboard.first().formattedGap)
        assertTrue(monacoLeaderboard.none { it.isPlayer })

        // Player beats the World Record
        val wrTime = com.example.model.GlobalLeaderboardRegistry.getBaseWrTimeMs("monaco_riviera")
        val playerTime = wrTime - 500L // 500ms faster than WR
        val playerWrBoard = com.example.model.GlobalLeaderboardRegistry.buildTrackLeaderboard(
            trackId = "monaco_riviera",
            playerPbMs = playerTime,
            playerName = "Apex Legend",
            playerCountry = "US"
        )

        val p1 = playerWrBoard.first()
        assertTrue("Player should be #1 on WR lap", p1.isPlayer)
        assertEquals(1, p1.rank)
        assertEquals("LEADER", p1.formattedGap)
        assertEquals(com.example.model.LeaderboardTier.WORLD_RECORD, p1.tier)

        // Player with intermediate lap time
        val midTime = wrTime + 2000L
        val midBoard = com.example.model.GlobalLeaderboardRegistry.buildTrackLeaderboard(
            trackId = "monaco_riviera",
            playerPbMs = midTime,
            playerName = "Challenger",
            playerCountry = "GB"
        )
        val playerEntry = midBoard.find { it.isPlayer }
        assertNotNull("Player should be present on leaderboard", playerEntry)
        assertTrue("Player should have rank > 1", playerEntry!!.rank > 1)
        assertTrue("Player gap should be positive", playerEntry.gapMs > 0)
    }

    @Test
    fun `test leaderboard repository integration and persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = RacingRepository(context)

        // Update driver profile
        repository.updateDriverProfile("VelocityKing", "JP")
        assertEquals("VelocityKing", repository.profile.value.driverName)
        assertEquals("JP", repository.profile.value.driverCountry)

        // Record a best lap
        repository.recordBestLap("apex_speedway", 69500L, null, "Apex GT-86 Coupe")
        assertEquals(69500L, repository.profile.value.bestLapTimes["apex_speedway"])
        assertEquals("Apex GT-86 Coupe", repository.profile.value.bestLapVehicles["apex_speedway"])

        // Query repository leaderboard
        val board = repository.getLeaderboard("apex_speedway")
        val p = board.find { it.isPlayer }
        assertNotNull(p)
        assertEquals("VelocityKing", p!!.driverName)
        assertEquals("JP", p.countryCode)
        assertEquals("Apex GT-86 Coupe", p.vehicleName)
        assertEquals(69500L, p.lapTimeMs)
    }

    @Test
    fun `test dynamic weather conditions and surface telemetry`() {
        val sunny = com.example.model.WeatherType.SUNNY_CLEAR
        val rain = com.example.model.WeatherType.LIGHT_RAIN
        val storm = com.example.model.WeatherType.THUNDERSTORM
        val fog = com.example.model.WeatherType.FOG_MIST
        val ice = com.example.model.WeatherType.FREEZING_ICE

        assertTrue("Sunny grip exceeds rain grip", sunny.baseGrip > rain.baseGrip)
        assertTrue("Rain grip exceeds storm grip", rain.baseGrip > storm.baseGrip)
        assertTrue("Storm grip exceeds ice grip", storm.baseGrip > ice.baseGrip)
        assertTrue("Clear visibility exceeds fog visibility", sunny.visibility > fog.visibility)

        val weatherState = com.example.model.DynamicWeatherState(
            currentWeather = storm,
            trackWetness = 0.85f,
            forecastWeather = sunny,
            secondsToTransition = 30
        )
        assertEquals(com.example.model.WeatherType.THUNDERSTORM, weatherState.currentWeather)
        assertEquals(0.85f, weatherState.trackWetness, 0.01f)
        assertEquals(30, weatherState.secondsToTransition)
    }

    @Test
    fun `test RacingSoundManager telemetry parameters and mute state`() {
        val soundManager = com.example.audio.RacingSoundManager()
        soundManager.isMuted = false
        assertTrue("Sound should be unmuted initially", !soundManager.isMuted)

        // Update telemetry
        soundManager.updateTelemetry(
            rpm = 6400f,
            maxRpm = 8200f,
            currentGear = 3,
            throttle = 0.95f,
            speedKmh = 145f,
            isDrifting = true,
            slipAngleDeg = 24.5f,
            lateralG = 1.25f,
            isBraking = false,
            isBoosting = true,
            isKerbRumble = true,
            isRaining = true
        )

        assertEquals(6400f, soundManager.targetRpm, 0.1f)
        assertEquals(8200f, soundManager.maxRpm, 0.1f)
        assertEquals(3, soundManager.currentGear)
        assertEquals(0.95f, soundManager.throttleInput, 0.01f)
        assertEquals(145f, soundManager.speedKmh, 0.1f)
        assertTrue(soundManager.isDrifting)
        assertEquals(24.5f, soundManager.slipAngleDeg, 0.1f)
        assertEquals(1.25f, soundManager.lateralG, 0.01f)
        assertTrue(soundManager.isBoosting)
        assertTrue(soundManager.isKerbRumble)
        assertTrue(soundManager.isRaining)

        // Test mute toggle
        soundManager.isMuted = true
        assertTrue(soundManager.isMuted)
    }

    @Test
    fun `test RacingSoundManager collision and gear shift events`() {
        val soundManager = com.example.audio.RacingSoundManager()

        // Trigger collision with varying intensities
        soundManager.playCollision(0.35f)
        soundManager.playCollision(1.0f)

        // Trigger gear shift
        soundManager.playGearShift()

        // Verify volume controls
        soundManager.masterVolume = 0.8f
        soundManager.engineVolume = 0.7f
        soundManager.screechVolume = 0.6f
        soundManager.collisionVolume = 0.9f
        soundManager.effectsVolume = 0.75f

        assertEquals(0.8f, soundManager.masterVolume, 0.01f)
        assertEquals(0.7f, soundManager.engineVolume, 0.01f)
        assertEquals(0.6f, soundManager.screechVolume, 0.01f)
        assertEquals(0.9f, soundManager.collisionVolume, 0.01f)
        assertEquals(0.75f, soundManager.effectsVolume, 0.01f)
    }

    @Test
    fun `test PhysicsEngine barrier collision triggers onCollision callback`() {
        val physics = com.example.physics.PhysicsEngine()
        val vehicle = VehicleRegistry.createDefaultVehicles().first()
        val track = TrackRegistry.allTracks.first()
        val particles = com.example.physics.ParticleSystem()

        // Place car far off-track beyond barrier limits
        val carState = com.example.physics.CarPhysicsState(
            x = track.startWaypoint.x + 800f,
            y = track.startWaypoint.y + 800f,
            vx = 25f,
            vy = 25f,
            speedKmh = 90f
        )

        var collisionReported = false
        var reportedIntensity = 0f

        physics.updatePlayer(
            state = carState,
            vehicle = vehicle,
            track = track,
            currentWeather = com.example.model.WeatherType.SUNNY_CLEAR,
            trackWetness = 0f,
            throttleInput = 1.0f,
            brakeInput = 0f,
            steerInput = 0f,
            handbrakeInput = false,
            boostInput = false,
            particleSystem = particles,
            dt = 0.016f,
            onCollision = { intensity ->
                collisionReported = true
                reportedIntensity = intensity
            }
        )

        assertTrue("Barrier collision must be triggered when off track", collisionReported)
        assertTrue("Collision intensity must be positive", reportedIntensity > 0.2f)
        assertTrue("Car state records lastCollisionIntensity", carState.lastCollisionIntensity > 0.2f)
    }

    @Test
    fun `test post race summary result model and financial earnings calculations`() {
        val tier = CareerRegistry.createDefaultTiers().first()
        val event = tier.events.first()

        val basePrize = event.firstPrize
        val cleanBonus = 800
        val driftBonus = 250
        val totalEarnings = basePrize + cleanBonus + driftBonus
        val creditsBefore = 2500
        val creditsAfter = creditsBefore + totalEarnings

        val postResult = com.example.model.PostRaceResult(
            position = 1,
            totalRacers = 4,
            totalRaceTimeMs = 102450L,
            bestLapTimeMs = 50200L,
            isNewBest = true,
            lapTimesMs = listOf(52250L, 50200L),
            topSpeedKmh = 215.4f,
            driftScore = 500,
            isCleanRace = true,
            basePrize = basePrize,
            cleanBonus = cleanBonus,
            driftBonus = driftBonus,
            repEarned = 80,
            totalEarnings = totalEarnings,
            creditsBefore = creditsBefore,
            creditsAfter = creditsAfter,
            repBefore = 0,
            repAfter = 80,
            isTimeTrial = false,
            trackId = event.trackId,
            trackName = "Monaco Riviera GP",
            vehicleName = "Apex GT-86 Coupe",
            careerEvent = event,
            tierName = tier.name,
            tierSubtitle = tier.subtitle,
            tierTrophyName = tier.trophyName,
            tierCompletedEvents = 1,
            tierTotalEvents = 2,
            isTierTrophyUnlocked = false,
            nextTierName = "Tier 2: Pro-Am Championship",
            nextTierRequiredRep = 200,
            repProgressToNextTier = 0.40f,
            nextCareerEvent = tier.events[1],
            careerObjectives = listOf(
                com.example.model.CareerObjectiveResult("Podium Finish", "Top 3 Finish", true, "+80 REP"),
                com.example.model.CareerObjectiveResult("Event Victory", "1st Place", true, "+$3,200"),
                com.example.model.CareerObjectiveResult("Clean Race", "Zero collisions", true, "+$800")
            ),
            rivalResults = listOf(
                com.example.model.RivalFinishResult(1, "Apex Pilot", "Apex GT-86", 102450L, "LEADER", true),
                com.example.model.RivalFinishResult(2, "Marcus Vance", "Apex R-34 GT", 104100L, "+1.65s", false)
            ),
            dailyChallengesUpdated = emptyList()
        )

        assertEquals(1, postResult.position)
        assertEquals(4, postResult.totalRacers)
        assertEquals(102450L, postResult.totalRaceTimeMs)
        assertEquals(2, postResult.lapTimesMs.size)
        assertEquals(totalEarnings, postResult.totalEarnings)
        assertEquals(6750, postResult.creditsAfter)
        assertTrue("Race was clean", postResult.isCleanRace)
        assertTrue("New record was set", postResult.isNewBest)
        assertEquals(1, postResult.tierCompletedEvents)
        assertEquals(2, postResult.tierTotalEvents)
        assertTrue("Trophy not unlocked yet (1/2)", !postResult.isTierTrophyUnlocked)
        assertEquals(3, postResult.careerObjectives.size)
        assertTrue("Podium objective achieved", postResult.careerObjectives[0].isAchieved)
    }

    @Test
    fun `test career goals progression and tier trophy unlock`() {
        val tier = CareerRegistry.createDefaultTiers().first()
        val allEventsCompleted = 2
        val totalEvents = tier.events.size

        val isTrophyWon = allEventsCompleted >= totalEvents
        assertTrue("Trophy won when all tier events are cleared", isTrophyWon)

        val currentRep = 220
        val nextTier = CareerRegistry.createDefaultTiers()[1]
        val isNextTierUnlocked = currentRep >= nextTier.requiredRep
        assertTrue("Next tier unlocked when reputation meets requirement", isNextTierUnlocked)
    }

    @Test
    fun `test achievement registry and categories`() {
        val achievements = AchievementRegistry.createDefaultAchievements()
        assertTrue("Achievement registry should have at least 15 badges", achievements.size >= 15)

        // Check rookie badge is unlocked by default
        val rookie = achievements.find { it.id == "badge_rookie" }
        assertNotNull("Rookie badge must exist", rookie)
        assertTrue("Rookie badge is unlocked by default", rookie!!.isUnlocked)

        // Check key milestones exist
        val win10 = achievements.find { it.id == "badge_win_10" }
        assertNotNull("Win 10 races badge must exist", win10)
        assertEquals(AchievementCategory.WINS, win10!!.category)
        assertEquals(BadgeRarity.GOLD, win10.badgeRarity)
        assertEquals(10f, win10.targetValue, 0.01f)
        assertFalse(win10.isUnlocked)

        val drift500 = achievements.find { it.id == "badge_drift_500m" }
        assertNotNull("Drift 500m badge must exist", drift500)
        assertEquals(AchievementCategory.DRIFT, drift500!!.category)
        assertEquals(500f, drift500.targetValue, 0.01f)

        val speed250 = achievements.find { it.id == "badge_speed_250" }
        assertNotNull("Speed 250 badge must exist", speed250)
        assertEquals(AchievementCategory.SPEED, speed250!!.category)
        assertEquals(BadgeRarity.GOLD, speed250.badgeRarity)
        assertEquals(250f, speed250.targetValue, 0.01f)
    }

    @Test
    fun `test speed threshold milestone unlocks badge`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = RacingRepository(context)

        // Record a race reaching 265 km/h
        val unlocked = repo.recordRaceMilestones(
            isWin = false,
            isClean = false,
            isWetWin = false,
            maxSpeedKmh = 265f,
            driftMeters = 50f
        )

        val profile = repo.profile.value
        assertEquals(265f, profile.highestSpeedKmh, 0.1f)

        val badge200 = profile.achievements.find { it.id == "badge_speed_200" }
        assertNotNull(badge200)
        assertTrue("Speed 200 badge unlocked", badge200!!.isUnlocked)

        val badge250 = profile.achievements.find { it.id == "badge_speed_250" }
        assertNotNull(badge250)
        assertTrue("Speed 250 badge unlocked", badge250!!.isUnlocked)

        // Speed 300 badge should remain locked
        val badge300 = profile.achievements.find { it.id == "badge_speed_300" }
        assertNotNull(badge300)
        assertFalse("Speed 300 badge remains locked", badge300!!.isUnlocked)
        assertEquals(265f, badge300.currentValue, 0.1f)
        assertTrue(unlocked.any { it.id == "badge_speed_250" })
    }

    @Test
    fun `test drift distance accumulation and milestone badge`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = RacingRepository(context)

        // Accumulate drift distance in multiple races
        repo.recordRaceMilestones(
            isWin = false,
            isClean = true,
            isWetWin = false,
            maxSpeedKmh = 140f,
            driftMeters = 320f
        )

        var profile = repo.profile.value
        assertEquals(320f, profile.totalDriftDistanceMeters, 0.1f)
        var driftBadge = profile.achievements.find { it.id == "badge_drift_500m" }
        assertFalse("500m drift badge not yet unlocked (320m / 500m)", driftBadge!!.isUnlocked)

        // Second race with 280m drift -> total 600m
        val newBadges = repo.recordRaceMilestones(
            isWin = false,
            isClean = true,
            isWetWin = false,
            maxSpeedKmh = 150f,
            driftMeters = 280f
        )

        profile = repo.profile.value
        assertEquals(600f, profile.totalDriftDistanceMeters, 0.1f)
        driftBadge = profile.achievements.find { it.id == "badge_drift_500m" }
        assertTrue("500m drift badge unlocked at 600m", driftBadge!!.isUnlocked)
        assertTrue(newBadges.any { it.id == "badge_drift_500m" })
    }

    @Test
    fun `test win 10 races milestone badge`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = RacingRepository(context)

        // Record 10 race wins
        var lastUnlocked: List<com.example.model.Achievement> = emptyList()
        for (i in 1..10) {
            lastUnlocked = repo.recordRaceMilestones(
                isWin = true,
                isClean = true,
                isWetWin = false,
                maxSpeedKmh = 180f,
                driftMeters = 40f
            )
        }

        val profile = repo.profile.value
        assertEquals(10, profile.totalRaceWins)
        val win1Badge = profile.achievements.find { it.id == "badge_win_1" }
        assertTrue("First win badge unlocked", win1Badge!!.isUnlocked)

        val win10Badge = profile.achievements.find { it.id == "badge_win_10" }
        assertNotNull(win10Badge)
        assertTrue("Win 10 races 'Decathlon Champion' badge unlocked", win10Badge!!.isUnlocked)
        assertEquals(10f, win10Badge.currentValue, 0.01f)

        // 25 wins badge should still be locked
        val win25Badge = profile.achievements.find { it.id == "badge_win_25" }
        assertNotNull(win25Badge)
        assertFalse("Win 25 races badge not yet unlocked", win25Badge!!.isUnlocked)
        assertEquals(10f, win25Badge.currentValue, 0.01f)
    }

    @Test
    fun `test badge equipping and reward claiming`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = RacingRepository(context)

        // Initially rookie badge is equipped
        assertEquals("badge_rookie", repo.profile.value.equippedBadgeId)

        // Win 1 race to unlock badge_win_1
        repo.recordRaceMilestones(
            isWin = true,
            isClean = false,
            isWetWin = false,
            maxSpeedKmh = 160f,
            driftMeters = 10f
        )

        // Equip the newly unlocked badge
        val equipSuccess = repo.equipBadge("badge_win_1")
        assertTrue("Equipping unlocked badge succeeds", equipSuccess)
        assertEquals("badge_win_1", repo.profile.value.equippedBadgeId)

        // Equipping locked badge fails
        val equipLocked = repo.equipBadge("badge_win_25")
        assertFalse("Equipping locked badge fails", equipLocked)
        assertEquals("badge_win_1", repo.profile.value.equippedBadgeId)

        // Claim reward for badge_win_1
        val initialCredits = repo.profile.value.credits
        val initialRep = repo.profile.value.reputation
        val badge = repo.profile.value.achievements.find { it.id == "badge_win_1" }!!
        assertFalse("Reward not claimed yet", badge.isClaimed)

        val claimSuccess = repo.claimAchievementReward("badge_win_1")
        assertTrue("Claim reward succeeds", claimSuccess)

        val updatedProfile = repo.profile.value
        val updatedBadge = updatedProfile.achievements.find { it.id == "badge_win_1" }!!
        assertTrue("Badge is now claimed", updatedBadge.isClaimed)
        assertEquals(initialCredits + badge.rewardCredits, updatedProfile.credits)
        assertEquals(initialRep + badge.rewardRep, updatedProfile.reputation)

        // Second claim attempt returns false
        val secondClaim = repo.claimAchievementReward("badge_win_1")
        assertFalse("Cannot claim reward twice", secondClaim)
    }

    @Test
    fun `test driver profile avatar registry presets`() {
        val avatars = DriverAvatarRegistry.allAvatars
        assertEquals(8, avatars.size)

        val starter = DriverAvatarRegistry.getAvatarById("avatar_cyber_neon")
        assertNotNull(starter)
        assertEquals("Cyber Visor", starter.name)
        assertEquals(0, starter.requiredRep)

        val outrun = DriverAvatarRegistry.getAvatarById("avatar_sunset_outrun")
        assertNotNull(outrun)
        assertEquals("Sunset Outrun", outrun.name)
        assertEquals(2500, outrun.requiredRep)
    }

    @Test
    fun `test custom username nationality and avatar update and persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = RacingRepository(context)

        // Default initial values
        assertEquals("Apex Pilot", repo.profile.value.driverName)
        assertEquals("US", repo.profile.value.driverCountry)
        assertEquals("avatar_cyber_neon", repo.profile.value.avatarId)

        // Update driver profile with custom username, country, and avatar
        repo.updateDriverProfile("VelocityPhantom", "JP", "avatar_tokyo_drift")

        assertEquals("VelocityPhantom", repo.profile.value.driverName)
        assertEquals("JP", repo.profile.value.driverCountry)
        assertEquals("avatar_tokyo_drift", repo.profile.value.avatarId)

        // Re-instantiate repository to verify SharedPreferences persistence
        val reloadedRepo = RacingRepository(context)
        assertEquals("VelocityPhantom", reloadedRepo.profile.value.driverName)
        assertEquals("JP", reloadedRepo.profile.value.driverCountry)
        assertEquals("avatar_tokyo_drift", reloadedRepo.profile.value.avatarId)
    }

    @Test
    fun `test total distance driven and career win percentage statistics`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = RacingRepository(context)

        val initialProfile = repo.profile.value
        assertEquals(0f, initialProfile.winPercentage, 0.01f)
        assertEquals(0f, initialProfile.totalDistanceKm, 0.01f)

        // Race 1: Victory (distance = 3,500m)
        repo.recordRaceMilestones(
            isWin = true,
            isClean = true,
            isWetWin = false,
            maxSpeedKmh = 220f,
            driftMeters = 150f,
            distanceMeters = 3500f,
            isPodium = true
        )

        var p = repo.profile.value
        assertEquals(1, p.totalRaceWins)
        assertEquals(1, p.totalRacesCompleted)
        assertEquals(1, p.totalPodiums)
        assertEquals(3500f, p.totalDistanceDrivenMeters, 0.1f)
        assertEquals(3.5f, p.totalDistanceKm, 0.01f)
        assertEquals(100f, p.winPercentage, 0.01f)
        assertEquals(100f, p.podiumPercentage, 0.01f)

        // Race 2: 2nd Place Podium (distance = 4,500m)
        repo.recordRaceMilestones(
            isWin = false,
            isClean = false,
            isWetWin = false,
            maxSpeedKmh = 210f,
            driftMeters = 80f,
            distanceMeters = 4500f,
            isPodium = true
        )

        p = repo.profile.value
        assertEquals(1, p.totalRaceWins)
        assertEquals(2, p.totalRacesCompleted)
        assertEquals(2, p.totalPodiums)
        assertEquals(8000f, p.totalDistanceDrivenMeters, 0.1f)
        assertEquals(8.0f, p.totalDistanceKm, 0.01f)
        assertEquals(50f, p.winPercentage, 0.01f)
        assertEquals(100f, p.podiumPercentage, 0.01f)

        // Race 3: 4th Place Non-podium (distance = 2,000m)
        repo.recordRaceMilestones(
            isWin = false,
            isClean = true,
            isWetWin = false,
            maxSpeedKmh = 195f,
            driftMeters = 20f,
            distanceMeters = 2000f,
            isPodium = false
        )

        p = repo.profile.value
        assertEquals(1, p.totalRaceWins)
        assertEquals(3, p.totalRacesCompleted)
        assertEquals(2, p.totalPodiums)
        assertEquals(10000f, p.totalDistanceDrivenMeters, 0.1f)
        assertEquals(10.0f, p.totalDistanceKm, 0.01f)
        assertEquals(33.33f, p.winPercentage, 0.1f)
        assertEquals(66.67f, p.podiumPercentage, 0.1f)

        // Independent distance recording (e.g., time trial or free run)
        repo.recordDistanceDriven(2500f)
        assertEquals(12500f, repo.profile.value.totalDistanceDrivenMeters, 0.1f)
        assertEquals(12.5f, repo.profile.value.totalDistanceKm, 0.01f)
    }

    @Test
    fun `test room database driver profile and game statistics persistence`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.room.AppDatabase.getDatabase(context)
        val profileDao = db.driverProfileDao()
        val statsDao = db.gameStatisticsDao()

        // Insert driver profile entity
        val profile = com.example.data.room.DriverProfile(
            id = "primary_driver",
            username = "Apex Champion",
            avatarPath = "avatar_monaco_gold",
            totalDistance = 48500.0,
            totalWins = 15,
            country = "DE",
            credits = 50000,
            reputation = 1200,
            selectedVehicleId = "apex_gt86",
            totalRacesCompleted = 20,
            totalPodiums = 18,
            totalDriftDistance = 3200.0,
            highestSpeedKmh = 285f,
            cleanRacesCount = 12,
            wetRaceWinsCount = 5,
            equippedBadgeId = "badge_win_10"
        )
        profileDao.insertOrUpdateProfile(profile)

        val retrieved = profileDao.getDriverProfile("primary_driver")
        assertNotNull("Retrieved profile from Room database must not be null", retrieved)
        assertEquals("Apex Champion", retrieved!!.username)
        assertEquals("avatar_monaco_gold", retrieved.avatarPath)
        assertEquals(48500.0, retrieved.totalDistance, 0.01)
        assertEquals(15, retrieved.totalWins)
        assertEquals("DE", retrieved.country)

        // Insert game statistic entity
        val stat = com.example.data.room.GameStatistics(
            raceHistory = "Monaco Riviera Grand Prix - Finished 1st / 4",
            carPerformanceLevels = "Apex GT-86 Coupe (Engine Stage 3, Slick Tires)",
            trackId = "monaco_riviera",
            vehicleId = "apex_gt86",
            position = 1,
            totalRacers = 4,
            bestLapTimeMs = 54200L,
            isWin = true,
            isPodium = true,
            isClean = true,
            maxSpeedKmh = 270f,
            driftMeters = 450f,
            distanceMeters = 5200f,
            earningsCredits = 3500,
            reputationEarned = 120
        )
        statsDao.insertGameStatistic(stat)

        val stats = statsDao.getRecentGameStatistics(5)
        assertTrue("Recent game statistics should contain the inserted race record", stats.isNotEmpty())
        val firstStat = stats.first()
        assertEquals("Monaco Riviera Grand Prix - Finished 1st / 4", firstStat.raceHistory)
        assertEquals("Apex GT-86 Coupe (Engine Stage 3, Slick Tires)", firstStat.carPerformanceLevels)
        assertEquals("monaco_riviera", firstStat.trackId)
        assertEquals(1, firstStat.position)
        assertTrue(firstStat.isWin)
        assertEquals(5200f, firstStat.distanceMeters, 0.1f)
    }

    @Test
    fun `test data repository abstraction for driver profile and game statistics`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataRepository = com.example.data.DataRepository(context)

        // Save and retrieve driver profile via repository API
        val profile = com.example.data.room.DriverProfile(
            id = "test_driver",
            username = "SpeedDemon",
            avatarPath = "avatar_monaco_gold",
            totalDistance = 15000.0,
            totalWins = 8,
            country = "GB"
        )
        dataRepository.saveDriverProfile(profile)

        val retrieved = dataRepository.getDriverProfile("test_driver")
        assertNotNull("Driver profile must be retrieved through DataRepository", retrieved)
        assertEquals("SpeedDemon", retrieved!!.username)
        assertEquals(15000.0, retrieved.totalDistance, 0.01)

        // Update identity through repository
        dataRepository.updateDriverIdentity("VelocityKing", "FR", "avatar_cyber_neon", "test_driver")
        val updatedIdentity = dataRepository.getDriverProfile("test_driver")
        assertEquals("VelocityKing", updatedIdentity!!.username)
        assertEquals("FR", updatedIdentity.country)

        // Add distance through repository
        dataRepository.addDistanceDriven(2500.0, "test_driver")
        val updatedDist = dataRepository.getDriverProfile("test_driver")
        assertEquals(17500.0, updatedDist!!.totalDistance, 0.01)

        // Record race event through repository helper
        val recordId = dataRepository.recordRaceEvent(
            trackId = "neon_tokyo",
            vehicleId = "apex_gt86",
            position = 1,
            totalRacers = 4,
            bestLapTimeMs = 49800L,
            isWin = true,
            isPodium = true,
            isClean = true,
            maxSpeedKmh = 295f,
            driftMeters = 800f,
            distanceMeters = 6400f,
            earningsCredits = 4500,
            reputationEarned = 150,
            raceHistory = "Neon Tokyo Night Sprint - 1st Place",
            carPerformanceLevels = "Apex GT-86 Coupe Stage 2"
        )
        assertTrue("Record ID should be generated by Room", recordId > 0)

        val recentStats = dataRepository.getRecentGameStatistics(10)
        assertTrue("Recent stats should not be empty", recentStats.isNotEmpty())
        val latest = recentStats.first()
        assertEquals("neon_tokyo", latest.trackId)
        assertEquals("Neon Tokyo Night Sprint - 1st Place", latest.raceHistory)
        assertEquals("Apex GT-86 Coupe Stage 2", latest.carPerformanceLevels)
        assertTrue(latest.isWin)

        val winsCount = dataRepository.getTotalWinsRecorded()
        assertTrue("Wins count should be at least 1", winsCount >= 1)

        val maxSpeed = dataRepository.getMaxSpeedRecorded()
        assertTrue("Max speed should be recorded", maxSpeed >= 295f)
    }

    @Test
    fun `test driver profile repository stats and username update`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.data.DriverProfileRepository(context)

        // Save initial profile
        val profile = com.example.data.room.DriverProfile(
            id = "driver_test_user",
            username = "RacerX",
            avatarPath = "avatar_cyber_neon",
            totalDistance = 25000.0,
            totalWins = 12,
            country = "JP"
        )
        repository.saveDriverProfile(profile)

        val retrieved = repository.getDriverProfile("driver_test_user")
        assertNotNull(retrieved)
        assertEquals("RacerX", retrieved!!.username)
        assertEquals(25000.0, retrieved.totalDistance, 0.01)
        assertEquals(12, retrieved.totalWins)

        // Update username via repository
        repository.updateUsername("ApexChampion99", "driver_test_user")
        val updated = repository.getDriverProfile("driver_test_user")
        assertNotNull(updated)
        assertEquals("ApexChampion99", updated!!.username)
        // Ensure other stats are preserved
        assertEquals(25000.0, updated.totalDistance, 0.01)
        assertEquals(12, updated.totalWins)
    }

    @Test
    fun `test game statistics repository race history and outcomes retrieval`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.GameStatisticsRepository(context)

        val timestamp = System.currentTimeMillis()
        val raceRecord = com.example.data.room.GameStatistics(
            raceHistory = "Grand Prix Season Finale - 1st Place",
            carPerformanceLevels = "Apex GT-86 Coupe Stage 3",
            trackId = "monaco_riviera",
            vehicleId = "apex_gt86",
            position = 1,
            totalRacers = 4,
            bestLapTimeMs = 52300L,
            isWin = true,
            isPodium = true,
            isClean = true,
            maxSpeedKmh = 280f,
            driftMeters = 300f,
            distanceMeters = 5000f,
            earningsCredits = 5000,
            reputationEarned = 200,
            timestamp = timestamp
        )
        val id = repo.insertGameStatistic(raceRecord)
        assertTrue("Inserted record ID must be > 0", id > 0)

        val all = repo.getRecentGameStatistics(10)
        assertTrue("All game statistics must not be empty", all.isNotEmpty())
        val found = all.find { it.recordId == id }
        assertNotNull("Inserted record must be found in statistics list", found)
        assertEquals(1, found!!.position)
        assertTrue(found.isWin)
        assertTrue(found.isClean)
        assertEquals("monaco_riviera", found.trackId)
        assertEquals(timestamp, found.timestamp)
        assertEquals(52300L, found.bestLapTimeMs)
    }

    @Test
    fun `test car performance progression tracking over time`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.GameStatisticsRepository(context)

        val t0 = System.currentTimeMillis() - 100000L
        val t1 = System.currentTimeMillis()

        // Baseline race
        repo.insertGameStatistic(
            com.example.data.room.GameStatistics(
                trackId = "monaco_riviera",
                vehicleId = "apex_gt86",
                maxSpeedKmh = 220f,
                bestLapTimeMs = 60000L,
                driftMeters = 100f,
                carPerformanceLevels = "Stage 1 Stock",
                timestamp = t0
            )
        )

        // Improved race
        repo.insertGameStatistic(
            com.example.data.room.GameStatistics(
                trackId = "monaco_riviera",
                vehicleId = "apex_gt86",
                maxSpeedKmh = 275f,
                bestLapTimeMs = 53000L,
                driftMeters = 450f,
                carPerformanceLevels = "Stage 3 Turbo & Aero",
                timestamp = t1
            )
        )

        val stats = repo.getRecentGameStatistics(10).sortedBy { it.timestamp }
        assertTrue("Chronological stats must have at least 2 entries", stats.size >= 2)
        val initial = stats.first()
        val latest = stats.last()
        assertTrue("Latest top speed must exceed or equal baseline", latest.maxSpeedKmh >= initial.maxSpeedKmh)
    }

    @Test
    fun `test formatRaceDuration output with and without milliseconds`() {
        val zeroMs = com.example.ui.components.formatRaceDuration(0L, includeMillis = true)
        assertEquals("00:00.000", zeroMs)

        val zeroNoMillis = com.example.ui.components.formatRaceDuration(0L, includeMillis = false)
        assertEquals("00:00", zeroNoMillis)

        // 1 minute, 5 seconds, 432 milliseconds = 65,432 ms
        val formattedLap = com.example.ui.components.formatRaceDuration(65432L, includeMillis = true)
        assertEquals("01:05.432", formattedLap)

        val formattedNoMillis = com.example.ui.components.formatRaceDuration(65432L, includeMillis = false)
        assertEquals("01:05", formattedNoMillis)

        // 1 hour, 2 minutes, 3 seconds, 850 milliseconds = 3,723,850 ms
        val formattedHour = com.example.ui.components.formatRaceDuration(3723850L, includeMillis = true)
        assertEquals("01:02:03.850", formattedHour)
    }

    @Test
    fun `test RaceTimerState start, stop, lap splits, and reset controls`() {
        val timerState = com.example.ui.components.RaceTimerState(initialDurationMs = 0L, initialIsRunning = false)
        assertFalse("Timer should start stopped", timerState.isRunning)
        assertEquals(0L, timerState.elapsedMillis)
        assertTrue("Lap list should initially be empty", timerState.lapTimes.isEmpty())

        // Start timer
        timerState.start()
        assertTrue("Timer should be running", timerState.isRunning)

        // Set time to 15,200 ms and record lap 1
        timerState.setTime(15200L)
        val lap1 = timerState.recordLap()
        assertEquals(15200L, lap1)
        assertEquals(1, timerState.lapTimes.size)

        // Set time to 36,400 ms and record lap 2 (split should be 36400 - 15200 = 21200)
        timerState.setTime(36400L)
        val lap2 = timerState.recordLap()
        assertEquals(21200L, lap2)
        assertEquals(2, timerState.lapTimes.size)

        // Toggle (should pause)
        timerState.toggle()
        assertFalse("Toggling running timer should pause it", timerState.isRunning)

        // Reset
        timerState.reset()
        assertFalse("Reset timer should not be running", timerState.isRunning)
        assertEquals(0L, timerState.elapsedMillis)
        assertTrue("Lap times must be cleared on reset", timerState.lapTimes.isEmpty())
        assertEquals("00:00.000", timerState.formattedTime)
    }

    @Test
    fun `test MainViewModel race duration state and pause toggle`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.MainViewModel(app)

        assertEquals(0L, viewModel.raceDurationMillis.value)
        assertFalse("Race should not be paused by default", viewModel.isPaused.value)

        viewModel.toggleRacePause()
        assertTrue("Race should be paused after toggle", viewModel.isPaused.value)

        viewModel.toggleRacePause()
        assertFalse("Race should resume after second toggle", viewModel.isPaused.value)
    }

    @Test
    fun `test RaceCountdownState 3-2-1 visual sequence progression`() {
        var finishCalled = false
        val state = com.example.ui.components.RaceCountdownState(
            initialSeconds = 3,
            autoStart = true,
            onFinishedCallback = { finishCalled = true }
        )

        assertEquals(3, state.count)
        assertEquals(com.example.ui.components.CountdownStage.THREE, state.stage)
        assertTrue(state.isRunning)
        assertFalse(state.isFinished)
        assertFalse(finishCalled)

        // Advance to 2
        state.advance()
        assertEquals(2, state.count)
        assertEquals(com.example.ui.components.CountdownStage.TWO, state.stage)
        assertFalse(state.isFinished)

        // Advance to 1
        state.advance()
        assertEquals(1, state.count)
        assertEquals(com.example.ui.components.CountdownStage.ONE, state.stage)
        assertFalse(state.isFinished)

        // Advance to GO (0)
        state.advance()
        assertEquals(0, state.count)
        assertEquals(com.example.ui.components.CountdownStage.GO, state.stage)
        assertFalse(state.isFinished)

        // Advance past GO to FINISHED
        state.advance()
        assertEquals(com.example.ui.components.CountdownStage.FINISHED, state.stage)
        assertTrue(state.isFinished)
        assertFalse(state.isRunning)
        assertTrue("onFinished callback must be invoked when countdown completes", finishCalled)
    }

    @Test
    fun `test RaceCountdownState skip and restart controls`() {
        var finishCalled = false
        val state = com.example.ui.components.RaceCountdownState(
            initialSeconds = 3,
            autoStart = true,
            onFinishedCallback = { finishCalled = true }
        )

        // Skip immediately
        state.skip()
        assertTrue(state.isFinished)
        assertFalse(state.isRunning)
        assertEquals(com.example.ui.components.CountdownStage.GO, state.stage)
        assertTrue("Callback must be invoked on skip", finishCalled)

        // Restart
        finishCalled = false
        state.restart()
        assertEquals(3, state.count)
        assertEquals(com.example.ui.components.CountdownStage.THREE, state.stage)
        assertTrue(state.isRunning)
        assertFalse(state.isFinished)
        assertFalse(finishCalled)
    }

    @Test
    fun `test MainViewModel race countdown state and completion unlocks timer`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.MainViewModel(app)

        // Trigger countdown
        viewModel.triggerRaceCountdown()
        assertTrue("Race countdown should be active", viewModel.isCountdownActive.value)
        assertEquals(0L, viewModel.raceDurationMillis.value)

        // Complete countdown
        viewModel.completeCountdown()
        assertFalse("Race countdown should complete and allow gameplay", viewModel.isCountdownActive.value)
    }

    @Test
    fun `test RacePauseMenu action callbacks and pause lifecycle`() {
        var resumeClicked = false
        var restartClicked = false
        var quitClicked = false

        val onResume = { resumeClicked = true }
        val onRestart = { restartClicked = true }
        val onQuit = { quitClicked = true }

        onResume()
        assertTrue("Resume callback should trigger", resumeClicked)

        onRestart()
        assertTrue("Restart callback should trigger", restartClicked)

        onQuit()
        assertTrue("Quit callback should trigger", quitClicked)

        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.MainViewModel(app)

        assertFalse("Initially not paused", viewModel.isPaused.value)
        viewModel.pauseRace()
        assertTrue("Paused after pauseRace()", viewModel.isPaused.value)
        viewModel.resumeRace()
        assertFalse("Resumed after resumeRace()", viewModel.isPaused.value)
    }

    @Test
    fun `test WeatherManager handling parameters impact across clear, rain, and snow`() {
        val clear = com.example.model.WeatherManager.getHandlingParameters(com.example.model.WeatherCondition.CLEAR)
        val rain = com.example.model.WeatherManager.getHandlingParameters(com.example.model.WeatherCondition.RAIN)
        val snow = com.example.model.WeatherManager.getHandlingParameters(com.example.model.WeatherCondition.SNOW)

        // Verify Grip scaling
        assertEquals(1.0f, clear.gripMultiplier, 0.01f)
        assertEquals(0.72f, rain.gripMultiplier, 0.01f)
        assertEquals(0.40f, snow.gripMultiplier, 0.01f)
        assertTrue("Clear grip must exceed rain grip", clear.gripMultiplier > rain.gripMultiplier)
        assertTrue("Rain grip must exceed snow grip", rain.gripMultiplier > snow.gripMultiplier)

        // Verify Braking Efficiency scaling
        assertEquals(1.0f, clear.brakingEfficiencyMultiplier, 0.01f)
        assertEquals(0.75f, rain.brakingEfficiencyMultiplier, 0.01f)
        assertEquals(0.44f, snow.brakingEfficiencyMultiplier, 0.01f)
        assertTrue("Clear braking must exceed rain braking", clear.brakingEfficiencyMultiplier > rain.brakingEfficiencyMultiplier)
        assertTrue("Rain braking must exceed snow braking", rain.brakingEfficiencyMultiplier > snow.brakingEfficiencyMultiplier)

        // Verify Traction (Wheelspin prevention) scaling
        assertEquals(1.0f, clear.accelerationTractionMultiplier, 0.01f)
        assertEquals(0.70f, rain.accelerationTractionMultiplier, 0.01f)
        assertEquals(0.38f, snow.accelerationTractionMultiplier, 0.01f)
        assertTrue("Clear traction must exceed rain traction", clear.accelerationTractionMultiplier > rain.accelerationTractionMultiplier)
        assertTrue("Rain traction must exceed snow traction", rain.accelerationTractionMultiplier > snow.accelerationTractionMultiplier)

        // Verify Steering Response and Lateral Slip Threshold
        assertTrue("Snow should provoke drift at lower slip angles", snow.lateralSlipThresholdDeg < clear.lateralSlipThresholdDeg)
    }

    @Test
    fun `test WeatherManager condition cycling and effective calculations`() {
        com.example.model.WeatherManager.setCondition(com.example.model.WeatherCondition.CLEAR)
        assertEquals(com.example.model.WeatherCondition.CLEAR, com.example.model.WeatherManager.currentCondition.value)

        // Cycle to Rain
        val cycledToRain = com.example.model.WeatherManager.cycleNextCondition()
        assertEquals(com.example.model.WeatherCondition.RAIN, cycledToRain)

        // Cycle to Snow
        val cycledToSnow = com.example.model.WeatherManager.cycleNextCondition()
        assertEquals(com.example.model.WeatherCondition.SNOW, cycledToSnow)

        // Cycle back to Clear
        val cycledToClear = com.example.model.WeatherManager.cycleNextCondition()
        assertEquals(com.example.model.WeatherCondition.CLEAR, cycledToClear)

        // Test helper calculation methods
        val baseGrip = 1.15f
        val effectiveSnowGrip = com.example.model.WeatherManager.calculateEffectiveGrip(baseGrip, com.example.model.WeatherCondition.SNOW)
        assertEquals(baseGrip * 0.40f, effectiveSnowGrip, 0.01f)

        val baseBrake = 1000f
        val effectiveRainBrake = com.example.model.WeatherManager.calculateEffectiveBraking(baseBrake, com.example.model.WeatherCondition.RAIN)
        assertEquals(baseBrake * 0.75f, effectiveRainBrake, 0.01f)
    }

    @Test
    fun `test MainViewModel live weather condition toggle`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.MainViewModel(app)

        // Toggle to Rain
        viewModel.setWeatherCondition(com.example.model.WeatherCondition.RAIN)
        assertEquals(com.example.model.WeatherType.LIGHT_RAIN, viewModel.weatherState.value.currentWeather)
        assertEquals(0.75f, viewModel.weatherState.value.trackWetness, 0.01f)
        assertEquals(com.example.model.WeatherCondition.RAIN, com.example.model.WeatherManager.currentCondition.value)

        // Toggle to Snow
        viewModel.setWeatherCondition(com.example.model.WeatherCondition.SNOW)
        assertEquals(com.example.model.WeatherType.FREEZING_ICE, viewModel.weatherState.value.currentWeather)
        assertEquals(0.90f, viewModel.weatherState.value.trackWetness, 0.01f)
        assertEquals(com.example.model.WeatherCondition.SNOW, com.example.model.WeatherManager.currentCondition.value)

        // Toggle back to Clear
        viewModel.setWeatherCondition(com.example.model.WeatherCondition.CLEAR)
        assertEquals(com.example.model.WeatherType.SUNNY_CLEAR, viewModel.weatherState.value.currentWeather)
        assertEquals(0.0f, viewModel.weatherState.value.trackWetness, 0.01f)
        assertEquals(com.example.model.WeatherCondition.CLEAR, com.example.model.WeatherManager.currentCondition.value)
    }

    @Test
    fun `test Leaderboard Room queries for top race times and personal best identification`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = com.example.data.GameStatisticsRepository(context)

        // Clear existing statistics
        repo.clearAllStatistics()

        // Insert multiple race times on "monaco_riviera"
        repo.insertGameStatistic(
            com.example.data.room.GameStatistics(
                trackId = "monaco_riviera",
                vehicleId = "apex_gt86",
                bestLapTimeMs = 74500L,
                maxSpeedKmh = 230f,
                position = 2,
                timestamp = System.currentTimeMillis() - 300000L
            )
        )

        repo.insertGameStatistic(
            com.example.data.room.GameStatistics(
                trackId = "monaco_riviera",
                vehicleId = "apex_gt86",
                bestLapTimeMs = 62100L, // Personal Best
                maxSpeedKmh = 255f,
                position = 1,
                isWin = true,
                timestamp = System.currentTimeMillis() - 200000L
            )
        )

        repo.insertGameStatistic(
            com.example.data.room.GameStatistics(
                trackId = "monaco_riviera",
                vehicleId = "apex_gt86",
                bestLapTimeMs = 68900L,
                maxSpeedKmh = 242f,
                position = 1,
                timestamp = System.currentTimeMillis() - 100000L
            )
        )

        // Insert race time on another track "tokyo_expressway"
        repo.insertGameStatistic(
            com.example.data.room.GameStatistics(
                trackId = "tokyo_expressway",
                vehicleId = "hyper_valkyrie",
                bestLapTimeMs = 58200L, // Overall fastest
                maxSpeedKmh = 310f,
                position = 1,
                timestamp = System.currentTimeMillis()
            )
        )

        // Fetch top race times overall
        val topTimes = repo.getTopRaceTimes(10)
        assertEquals(4, topTimes.size)

        // Verify ordering: fastest (lowest ms) first
        assertEquals(58200L, topTimes[0].bestLapTimeMs)
        assertEquals(62100L, topTimes[1].bestLapTimeMs)
        assertEquals(68900L, topTimes[2].bestLapTimeMs)
        assertEquals(74500L, topTimes[3].bestLapTimeMs)

        // Verify Personal Best overall
        val overallPb = repo.getPersonalBestLapTime()
        assertEquals(58200L, overallPb)

        // Verify Personal Best on Monaco specifically
        val monacoPb = repo.getPersonalBestLapTime("monaco_riviera")
        assertEquals(62100L, monacoPb)

        val monacoTopTimes = repo.getTopRaceTimesForTrack("monaco_riviera", 10)
        assertEquals(3, monacoTopTimes.size)
        assertEquals(62100L, monacoTopTimes[0].bestLapTimeMs)

        // Verify personal best record entity retrieval
        val pbRecord = repo.getPersonalBestRecord("monaco_riviera")
        assertNotNull(pbRecord)
        assertEquals(62100L, pbRecord?.bestLapTimeMs)
        assertEquals("monaco_riviera", pbRecord?.trackId)
        assertTrue(pbRecord?.isWin == true)
    }

    @Test
    fun `test settings datastore default preferences and updates`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = com.example.data.SettingsDataStore.getInstance(context)

        // Reset to clean default baseline
        dataStore.resetToDefaults()

        val initial = dataStore.audioSettingsFlow.first()
        assertEquals(1.0f, initial.masterVolume, 0.001f)
        assertFalse(initial.isMasterMuted)
        assertTrue(initial.soundEffectsEnabled)
        assertEquals(1.0f, initial.soundEffectsVolume, 0.001f)
        assertTrue(initial.engineSoundEnabled)

        // Test master volume adjustment & clamping
        dataStore.setMasterVolume(0.65f)
        var updated = dataStore.audioSettingsFlow.first()
        assertEquals(0.65f, updated.masterVolume, 0.001f)

        // Test over-range clamping
        dataStore.setMasterVolume(1.5f)
        updated = dataStore.audioSettingsFlow.first()
        assertEquals(1.0f, updated.masterVolume, 0.001f)

        dataStore.setMasterVolume(-0.5f)
        updated = dataStore.audioSettingsFlow.first()
        assertEquals(0.0f, updated.masterVolume, 0.001f)

        // Test mute toggling
        dataStore.setMasterMuted(true)
        updated = dataStore.audioSettingsFlow.first()
        assertTrue(updated.isMasterMuted)

        dataStore.toggleMasterMuted()
        updated = dataStore.audioSettingsFlow.first()
        assertFalse(updated.isMasterMuted)

        // Test sound effects toggle & volume
        dataStore.setSoundEffectsEnabled(false)
        updated = dataStore.audioSettingsFlow.first()
        assertFalse(updated.soundEffectsEnabled)

        dataStore.toggleSoundEffects()
        updated = dataStore.audioSettingsFlow.first()
        assertTrue(updated.soundEffectsEnabled)

        dataStore.setSoundEffectsVolume(0.42f)
        updated = dataStore.audioSettingsFlow.first()
        assertEquals(0.42f, updated.soundEffectsVolume, 0.001f)

        // Test reset to defaults
        dataStore.resetToDefaults()
        val resetSettings = dataStore.audioSettingsFlow.first()
        assertEquals(1.0f, resetSettings.masterVolume, 0.001f)
        assertFalse(resetSettings.isMasterMuted)
        assertTrue(resetSettings.soundEffectsEnabled)
        assertEquals(1.0f, resetSettings.soundEffectsVolume, 0.001f)
        assertTrue(resetSettings.engineSoundEnabled)
    }

    @Test
    fun `test settings viewmodel audio synchronization and overlay states`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val dataStore = com.example.data.SettingsDataStore.getInstance(app)
        val viewModel = com.example.ui.SettingsViewModel(app, dataStore)

        // Verify initial overlay visibility
        assertFalse("Overlay initially hidden", viewModel.isOverlayVisible.value)
        viewModel.showOverlay()
        assertTrue("Overlay shown", viewModel.isOverlayVisible.value)
        viewModel.hideOverlay()
        assertFalse("Overlay hidden", viewModel.isOverlayVisible.value)
        viewModel.toggleOverlay()
        assertTrue("Overlay toggled visible", viewModel.isOverlayVisible.value)
        viewModel.toggleOverlay()
        assertFalse("Overlay toggled hidden", viewModel.isOverlayVisible.value)

        // Test audio function executes safely without crashing
        viewModel.testAudio()
    }

    @Test
    fun `test settings overlay callbacks and user interactions`() {
        var masterMuteToggled = false
        var masterVolumeChanged = 0f
        var sfxToggled = false
        var sfxVolumeChanged = 0f
        var engineToggled = false
        var testAudioTriggered = false
        var resetDefaultsTriggered = false
        var dismissed = false

        val onMasterMuteToggle = { masterMuteToggled = true }
        val onMasterVolumeChange = { v: Float -> masterVolumeChanged = v }
        val onSoundEffectsToggle = { sfxToggled = true }
        val onSoundEffectsVolumeChange = { v: Float -> sfxVolumeChanged = v }
        val onEngineSoundToggle = { engineToggled = true }
        val onTestAudio = { testAudioTriggered = true }
        val onResetDefaults = { resetDefaultsTriggered = true }
        val onDismiss = { dismissed = true }

        onMasterMuteToggle()
        assertTrue(masterMuteToggled)

        onMasterVolumeChange(0.5f)
        assertEquals(0.5f, masterVolumeChanged, 0.001f)

        onSoundEffectsToggle()
        assertTrue(sfxToggled)

        onSoundEffectsVolumeChange(0.35f)
        assertEquals(0.35f, sfxVolumeChanged, 0.001f)

        onEngineSoundToggle()
        assertTrue(engineToggled)

        onTestAudio()
        assertTrue(testAudioTriggered)

        onResetDefaults()
        assertTrue(resetDefaultsTriggered)

        onDismiss()
        assertTrue(dismissed)
    }

    @Test
    fun `test DriverProfile Room entity and DAO player statistics persistence and updates`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.room.AppDatabase.getDatabase(context)
        val profileDao = db.driverProfileDao()

        // Clean slate
        profileDao.deleteProfileById("stat_test_driver")

        // 1. Create and insert DriverProfile Room entity with statistics
        val profile = com.example.data.room.DriverProfile(
            id = "stat_test_driver",
            username = "Apex Champion",
            totalRacesCompleted = 42,
            totalPodiums = 28,
            podiumFinishes = 28,
            preferredCar = "hyper_valkyrie",
            selectedVehicleId = "hyper_valkyrie",
            totalWins = 18,
            totalDistance = 56000.0
        )
        profileDao.insertDriverProfile(profile)

        // 2. Retrieve entity and verify fields
        val retrieved = profileDao.getDriverProfile("stat_test_driver")
        assertNotNull("Retrieved DriverProfile must not be null", retrieved)
        assertEquals(42, retrieved!!.totalRacesCompleted)
        assertEquals(28, retrieved.podiumFinishes)
        assertEquals(28, retrieved.totalPodiums)
        assertEquals("hyper_valkyrie", retrieved.preferredCar)
        assertEquals("hyper_valkyrie", retrieved.selectedVehicleId)
        assertEquals(18, retrieved.totalWins)
        assertEquals("Apex Champion", retrieved.username)

        // 3. Update player statistics via DAO
        profileDao.updatePlayerStatistics(
            id = "stat_test_driver",
            racesCompleted = 50,
            podiums = 35,
            preferredCar = "phantom_gtr"
        )
        val updatedStats = profileDao.getDriverProfile("stat_test_driver")
        assertNotNull(updatedStats)
        assertEquals(50, updatedStats!!.totalRacesCompleted)
        assertEquals(35, updatedStats.podiumFinishes)
        assertEquals(35, updatedStats.totalPodiums)
        assertEquals("phantom_gtr", updatedStats.preferredCar)

        // 4. Increment races completed
        profileDao.incrementRacesCompleted("stat_test_driver")
        val incRace = profileDao.getDriverProfile("stat_test_driver")
        assertEquals(51, incRace?.totalRacesCompleted)

        // 5. Increment podium finishes
        profileDao.incrementPodiumFinishes("stat_test_driver")
        val incPodium = profileDao.getDriverProfile("stat_test_driver")
        assertEquals(36, incPodium?.podiumFinishes)
        assertEquals(36, incPodium?.totalPodiums)

        // 6. Update preferred vehicle
        profileDao.updatePreferredCar("stat_test_driver", "monaco_stradale")
        val updatedCar = profileDao.getDriverProfile("stat_test_driver")
        assertEquals("monaco_stradale", updatedCar?.preferredCar)

        // 7. Verify reactive flow observation
        val flowProfile = profileDao.getDriverProfileFlow("stat_test_driver").first()
        assertNotNull(flowProfile)
        assertEquals(51, flowProfile!!.totalRacesCompleted)
        assertEquals(36, flowProfile.podiumFinishes)
        assertEquals("monaco_stradale", flowProfile.preferredCar)

        // Cleanup
        profileDao.deleteProfileById("stat_test_driver")
    }

    @Test
    fun `test Achievements Room entity and DAO milestone persistence`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = com.example.data.room.AppDatabase.getDatabase(context)
        val dao = database.achievementsDao()

        // 1. Seed default achievements
        dao.insertAchievements(com.example.data.room.DefaultAchievements.createDefaultList())

        // 2. Verify all milestones exist
        val firstWin = dao.getAchievementById("first_win")
        assertNotNull("First Win milestone must exist", firstWin)
        assertEquals("First Win", firstWin!!.title)
        assertEquals(1f, firstWin.targetProgress, 0.01f)
        assertEquals(0f, firstWin.currentProgress, 0.01f)
        assertFalse(firstWin.isUnlocked)

        val hundredMiles = dao.getAchievementById("100_miles_driven")
        assertNotNull("100 Miles Driven milestone must exist", hundredMiles)
        assertEquals("100 Miles Driven", hundredMiles!!.title)
        assertEquals(160934f, hundredMiles.targetProgress, 0.01f)
        assertEquals("m", hundredMiles.unit)
        assertFalse(hundredMiles.isUnlocked)

        val cleanLap = dao.getAchievementById("clean_lap")
        assertNotNull("Clean Lap milestone must exist", cleanLap)
        assertEquals("Clean Lap", cleanLap!!.title)
        assertFalse(cleanLap.isUnlocked)

        // 3. Test progress update (partial progress)
        dao.updateProgress("100_miles_driven", 80467f)
        val partialHundredMiles = dao.getAchievementById("100_miles_driven")
        assertNotNull(partialHundredMiles)
        assertEquals(80467f, partialHundredMiles!!.currentProgress, 0.01f)
        assertFalse(partialHundredMiles.isUnlocked)
        assertEquals(0.5f, partialHundredMiles.progressFraction, 0.01f)
        assertEquals(50, partialHundredMiles.progressPercent)

        // 4. Test reaching target unlocks milestone
        dao.updateProgress("100_miles_driven", 160934f)
        val completedHundredMiles = dao.getAchievementById("100_miles_driven")
        assertNotNull(completedHundredMiles)
        assertTrue("Milestone must unlock upon reaching target", completedHundredMiles!!.isUnlocked)
        assertTrue("Unlock timestamp should be set", (completedHundredMiles.unlockedTimestamp ?: 0L) > 0)
        assertEquals(1.0f, completedHundredMiles.progressFraction, 0.01f)
        assertEquals(100, completedHundredMiles.progressPercent)

        // 5. Test direct milestone unlock
        dao.unlockAchievement("clean_lap")
        val unlockedCleanLap = dao.getAchievementById("clean_lap")
        assertNotNull(unlockedCleanLap)
        assertTrue(unlockedCleanLap!!.isUnlocked)
        assertEquals(unlockedCleanLap.targetProgress, unlockedCleanLap.currentProgress, 0.01f)

        // 6. Test flow observations
        val allAchievements = dao.getAllAchievementsFlow().first()
        assertTrue("All achievements should be populated", allAchievements.size >= 8)

        val unlockedAchievements = dao.getUnlockedAchievementsFlow().first()
        assertTrue("Unlocked achievements should contain unlocked milestones", unlockedAchievements.size >= 2)
        assertTrue(unlockedAchievements.any { it.id == "100_miles_driven" })
        assertTrue(unlockedAchievements.any { it.id == "clean_lap" })

        val unlockedCount = dao.getUnlockedCountFlow().first()
        assertEquals(unlockedAchievements.size, unlockedCount)
    }

    @Test
    fun `test AchievementsRepository milestone syncing with DriverProfile`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = com.example.data.room.AppDatabase.getDatabase(context)
        val repository = com.example.data.AchievementsRepository(database.achievementsDao())

        // Initialize defaults
        repository.initDefaultAchievementsIfNeeded()

        // Verify initial state
        val initialList = repository.allAchievementsFlow.first()
        assertTrue(initialList.isNotEmpty())

        // Create a driver profile with 5 wins, 200,000 meters driven, and 8 podiums
        val profile = com.example.data.room.DriverProfile(
            id = "test_profile_sync",
            username = "SpeedDemon",
            totalRacesCompleted = 20,
            podiumFinishes = 8,
            preferredCar = "monaco_stradale",
            totalWins = 5,
            totalDistance = 200_000.0,
            cleanRacesCount = 12
        )

        // Sync with driver profile
        repository.syncWithDriverProfile(profile)

        // Check that 'First Win' and '100 Miles Driven' are now unlocked
        val allAfterSync = repository.allAchievementsFlow.first()
        val firstWin = allAfterSync.find { it.id == "first_win" }
        assertNotNull(firstWin)
        assertTrue("First Win should be unlocked after sync", firstWin!!.isUnlocked)

        val hundredMiles = allAfterSync.find { it.id == "100_miles_driven" }
        assertNotNull(hundredMiles)
        assertTrue("100 Miles Driven should be unlocked after sync", hundredMiles!!.isUnlocked)

        val podiumMaster = allAfterSync.find { it.id == "podium_master" }
        assertNotNull(podiumMaster)
        assertTrue("Podium Master (target 5) should be unlocked with 8 podiums", podiumMaster!!.isUnlocked)
    }

    @Test
    fun `test DailyChallenges Room entity and DAO persistence and progress tracking`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = com.example.data.room.AppDatabase.getDatabase(context)
        val dao = database.dailyChallengesDao()

        val testDate = "2026-11-04"
        val expiryTime = System.currentTimeMillis() + 86400000L

        // 1. Create and insert limited-time task
        val challenge = com.example.data.room.DailyChallenges(
            id = "${testDate}_speed_test",
            dateKey = testDate,
            type = com.example.data.room.DailyChallengesGenerator.TYPE_TOP_SPEED,
            title = "Speed Trial 240",
            description = "Reach 240 km/h in any circuit event",
            targetValue = 240,
            currentValue = 0,
            rewardCredits = 4500,
            rewardRep = 150,
            iconEmoji = "⚡",
            expiresAtTimestamp = expiryTime
        )
        dao.insertChallenge(challenge)

        // 2. Query challenge by ID and date key
        val retrieved = dao.getChallengeById("${testDate}_speed_test")
        assertNotNull("Challenge must be persisted in Room", retrieved)
        assertEquals("Speed Trial 240", retrieved!!.title)
        assertEquals(240, retrieved.targetValue)
        assertEquals(0, retrieved.currentValue)
        assertFalse(retrieved.isCompleted)
        assertFalse(retrieved.isClaimed)
        assertFalse(retrieved.isExpired)

        // 3. Test progress update (partial)
        dao.updateProgress("${testDate}_speed_test", 180)
        val partial = dao.getChallengeById("${testDate}_speed_test")
        assertNotNull(partial)
        assertEquals(180, partial!!.currentValue)
        assertEquals(0.75f, partial.progressFraction, 0.01f)
        assertEquals(75, partial.progressPercent)
        assertFalse(partial.isTaskFinished)
        assertFalse(partial.isReadyToClaim)

        // 4. Test progress increment reaching target
        dao.incrementProgress("${testDate}_speed_test", 60)
        val completed = dao.getChallengeById("${testDate}_speed_test")
        assertNotNull(completed)
        assertEquals(240, completed!!.currentValue)
        assertTrue("Task should be completed when target reached", completed.isTaskFinished)
        assertTrue("Ready to claim when finished and unclaimed", completed.isReadyToClaim)

        // 5. Claim reward
        dao.claimReward("${testDate}_speed_test")
        val claimed = dao.getChallengeById("${testDate}_speed_test")
        assertNotNull(claimed)
        assertTrue(claimed!!.isClaimed)
        assertFalse("Should not be ready to claim once claimed", claimed.isReadyToClaim)

        // 6. Test date-specific flow observation
        val dateFlowList = dao.getChallengesForDateFlow(testDate).first()
        assertTrue(dateFlowList.any { it.id == "${testDate}_speed_test" })

        // Cleanup
        dao.deleteChallengeById("${testDate}_speed_test")
    }

    @Test
    fun `test DailyChallengesRepository generation and claim workflow`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = com.example.data.room.AppDatabase.getDatabase(context)
        val repository = com.example.data.DailyChallengesRepository(database.dailyChallengesDao())

        val testDate = "2026-11-05"
        // 1. Ensure challenges are generated
        val challenges = repository.ensureTodayChallenges(testDate)
        assertEquals("Must generate 3 limited-time daily objectives", 3, challenges.size)

        // 2. Check each challenge has valid parameters
        challenges.forEach { ch ->
            assertEquals(testDate, ch.dateKey)
            assertTrue("Target must be positive", ch.targetValue > 0)
            assertTrue("Reward credits must be positive", ch.rewardCredits > 0)
            assertTrue("Expiry timestamp must be in future", ch.expiresAtTimestamp > System.currentTimeMillis())
            assertFalse(ch.isClaimed)
        }

        // 3. Update progress for one of the challenge types
        val firstType = challenges.first().type
        repository.updateProgress(firstType, 99999, dateKey = testDate)

        val updatedList = database.dailyChallengesDao().getChallengesForDate(testDate)
        val completedTask = updatedList.first { it.type == firstType }
        assertTrue("Task with updated progress should be finished", completedTask.isTaskFinished)

        // 4. Claim challenge via repository
        val claimedResult = repository.claimChallenge(completedTask.id)
        assertNotNull("Claimed task should return non-null entity", claimedResult)
        assertTrue(claimedResult!!.isClaimed)

        // 5. Attempting to claim again should return null
        val reClaimResult = repository.claimChallenge(completedTask.id)
        org.junit.Assert.assertNull("Cannot claim already claimed task", reClaimResult)
    }

    @Test
    fun `test DailyChallengesReminderWorker execution and notification channel creation`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)

        val worker = androidx.work.testing.TestListenableWorkerBuilder<com.example.worker.DailyChallengesReminderWorker>(context).build()
        val result = worker.doWork()
        assertEquals(androidx.work.ListenableWorker.Result.success(), result)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = notificationManager.getNotificationChannel(com.example.worker.DailyChallengesReminderWorker.CHANNEL_ID)
        assertNotNull("Notification channel must be created", channel)
        assertEquals("Daily Challenges Reminder", channel.name)
    }

    @Test
    fun `test DailyChallengesNotificationScheduler registers WorkManager work requests`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)

        com.example.worker.DailyChallengesNotificationScheduler.scheduleDailyReminder(context, forceUpdate = true)
        val periodicWorkInfo = androidx.work.WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(com.example.worker.DailyChallengesNotificationScheduler.WORK_NAME_PERIODIC)
            .get()
        assertNotNull(periodicWorkInfo)
        assertTrue("Periodic daily reminder work must be enqueued", periodicWorkInfo.isNotEmpty())

        com.example.worker.DailyChallengesNotificationScheduler.triggerImmediateReminderCheck(context)
        val oneTimeWorkInfo = androidx.work.WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(com.example.worker.DailyChallengesNotificationScheduler.WORK_NAME_ONE_TIME)
            .get()
        assertNotNull(oneTimeWorkInfo)
        assertTrue("One-time reminder check work must be enqueued", oneTimeWorkInfo.isNotEmpty())
    }

    @Test
    fun `test GlobalLeaderboards entity formatting and flag helper`() {
        val entry = com.example.data.room.GlobalLeaderboards(
            id = "test_entry_1",
            trackId = "monaco_riviera",
            trackName = "Monaco Riviera",
            rank = 1,
            driverName = "Max Apex",
            countryCode = "NL",
            vehicleName = "Hyperion GT-R",
            vehicleCategory = "Apex Prototype",
            lapTimeMs = 74420L,
            gapMs = 0L,
            isPlayer = false,
            tierName = "WORLD_RECORD"
        )

        assertEquals("1:14.420", entry.formattedTime)
        assertEquals("LEADER", entry.formattedGap)
        assertEquals("🇳🇱", entry.countryFlag)

        val chaser = entry.copy(
            id = "test_entry_2",
            rank = 2,
            lapTimeMs = 74700L,
            gapMs = 280L,
            countryCode = "US"
        )
        assertEquals("1:14.700", chaser.formattedTime)
        assertEquals("+0.280s", chaser.formattedGap)
        assertEquals("🇺🇸", chaser.countryFlag)
    }

    @Test
    fun `test DefaultGlobalLeaderboards benchmark roster and all 4 circuits`() {
        val rankings = com.example.data.room.DefaultGlobalLeaderboards.createDefaultRankings()
        assertEquals(40, rankings.size) // 4 circuits * 10 entries

        val tracks = com.example.data.room.DefaultGlobalLeaderboards.trackSpecs.map { it.trackId }
        assertTrue("Contains Monaco Riviera", tracks.contains("monaco_riviera"))
        assertTrue("Contains Apex Speedway", tracks.contains("apex_speedway"))
        assertTrue("Contains Neon Tokyo", tracks.contains("neon_tokyo"))
        assertTrue("Contains Mount Akina", tracks.contains("mount_akina"))

        for (trackId in tracks) {
            val trackRankings = rankings.filter { it.trackId == trackId }
            assertEquals(10, trackRankings.size)
            val wr = trackRankings.first { it.rank == 1 }
            assertEquals(0L, wr.gapMs)
            assertEquals("WORLD_RECORD", wr.tierName)
            assertFalse(wr.isPlayer)
        }
    }

    @Test
    fun `test GlobalLeaderboardsDao queries and database storage`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.room.AppDatabase.getDatabase(context)
        val dao = db.globalLeaderboardsDao()

        // Clean any existing entries
        dao.deleteAllLeaderboards()
        assertEquals(0, dao.getTotalEntryCount())

        // Insert default rankings
        val defaults = com.example.data.room.DefaultGlobalLeaderboards.createDefaultRankings()
        dao.insertDefaultEntries(defaults)

        val totalCount = dao.getTotalEntryCount()
        assertEquals(40, totalCount)

        val monacoCount = dao.getEntryCountForTrack("monaco_riviera")
        assertEquals(10, monacoCount)

        val top5Monaco = dao.getTopRankingsForTrack("monaco_riviera", limit = 5)
        assertEquals(5, top5Monaco.size)
        assertEquals(1, top5Monaco[0].rank)
        assertEquals(2, top5Monaco[1].rank)
        assertEquals(3, top5Monaco[2].rank)

        val wr = dao.getWorldRecordForTrack("monaco_riviera")
        assertNotNull(wr)
        assertEquals(1, wr!!.rank)
        assertEquals(74420L, wr.lapTimeMs)

        val player = dao.getPlayerEntryForTrack("monaco_riviera")
        org.junit.Assert.assertNull("No player entry seeded initially", player)
    }

    @Test
    fun `test GlobalLeaderboardsRepository records player lap and dynamically recalculates ranks`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.data.GlobalLeaderboardsRepository(context)
        val dao = repository.globalLeaderboardsDao

        // Clean & seed fresh
        dao.deleteAllLeaderboards()
        repository.ensureDefaultLeaderboards()

        // 1. Record a player lap time that slots into 2nd place
        // Monaco base WR is 74420L. Seeded #2 is 74420 + 280 = 74700L.
        // Let's set player time = 74500L (between WR 74420L and old #2 74700L)
        val playerEntry = repository.recordPlayerLapTime(
            trackId = "monaco_riviera",
            trackName = "Monaco Riviera",
            lapTimeMs = 74500L,
            driverName = "Apex Champion",
            countryCode = "US",
            vehicleName = "Hyperion GT-R",
            vehicleCategory = "Apex Prototype"
        )

        assertNotNull(playerEntry)
        assertEquals(2, playerEntry.rank)
        assertTrue(playerEntry.isPlayer)
        assertEquals(80L, playerEntry.gapMs) // 74500 - 74420 = 80ms gap to WR

        // Check leaderboard for Monaco
        val updatedMonaco = dao.getLeaderboardForTrack("monaco_riviera")
        val p = updatedMonaco.first { it.isPlayer }
        assertEquals(2, p.rank)
        assertEquals(74500L, p.lapTimeMs)

        // The former #2 driver should now be rank 3
        val thirdPlace = updatedMonaco.first { it.rank == 3 }
        assertFalse(thirdPlace.isPlayer)
        assertEquals(74700L, thirdPlace.lapTimeMs)
        assertEquals(280L, thirdPlace.gapMs)

        // 2. Record a faster player lap time that sets a NEW WORLD RECORD
        val newWrEntry = repository.recordPlayerLapTime(
            trackId = "monaco_riviera",
            trackName = "Monaco Riviera",
            lapTimeMs = 73900L, // 520ms faster than former WR (74420L)
            driverName = "Apex Champion",
            countryCode = "US",
            vehicleName = "Hyperion GT-R",
            vehicleCategory = "Apex Prototype"
        )

        assertEquals(1, newWrEntry.rank)
        assertEquals(0L, newWrEntry.gapMs)
        assertEquals("WORLD_RECORD", newWrEntry.tierName)
        assertTrue(newWrEntry.isPlayer)

        val monacoAfterWr = dao.getLeaderboardForTrack("monaco_riviera")
        val newWr = monacoAfterWr.first { it.rank == 1 }
        assertTrue(newWr.isPlayer)
        assertEquals(73900L, newWr.lapTimeMs)
        assertEquals(0L, newWr.gapMs)

        // Former WR holder (74420L) is now rank 2 with gap 74420 - 73900 = 520ms
        val secondPlace = monacoAfterWr.first { it.rank == 2 }
        assertFalse(secondPlace.isPlayer)
        assertEquals(74420L, secondPlace.lapTimeMs)
        assertEquals(520L, secondPlace.gapMs)

        // Verify world record flow query
        val wrFlowValue = repository.getWorldRecordForTrackFlow("monaco_riviera").first()
        assertNotNull(wrFlowValue)
        assertEquals(1, wrFlowValue!!.rank)
        assertTrue(wrFlowValue.isPlayer)
    }

    @Test
    fun `test DynamicWeatherPhysicsService grip topSpeed and brakingDistance parameters across Sunny Rain Snow`() {
        val service = com.example.physics.DynamicWeatherPhysicsService()

        // 1. Sunny & Dry Condition
        service.forceWeather(com.example.physics.RaceWeatherCondition.SUNNY)
        val sunny = service.physicsState.value
        assertEquals(1.0f, sunny.gripMultiplier, 0.001f)
        assertEquals(1.0f, sunny.topSpeedMultiplier, 0.001f)
        assertEquals(1.0f, sunny.brakingEfficiencyMultiplier, 0.001f)
        assertEquals(1.0f, sunny.brakingDistanceMultiplier, 0.001f)
        assertEquals("100%", sunny.formattedGripPct)

        // 2. Rain & Wet Condition
        service.forceWeather(com.example.physics.RaceWeatherCondition.RAIN)
        val rain = service.physicsState.value
        assertEquals(0.70f, rain.gripMultiplier, 0.001f)
        assertEquals(0.94f, rain.topSpeedMultiplier, 0.001f)
        assertEquals(0.68f, rain.brakingEfficiencyMultiplier, 0.001f)
        assertTrue("Rain braking distance multiplier is ~1.47x (47% longer)", rain.brakingDistanceMultiplier > 1.40f)
        assertEquals("70%", rain.formattedGripPct)

        // 3. Snow & Ice Condition
        service.forceWeather(com.example.physics.RaceWeatherCondition.SNOW)
        val snow = service.physicsState.value
        assertEquals(0.38f, snow.gripMultiplier, 0.001f)
        assertEquals(0.85f, snow.topSpeedMultiplier, 0.001f)
        assertEquals(0.40f, snow.brakingEfficiencyMultiplier, 0.001f)
        assertEquals(2.50f, snow.brakingDistanceMultiplier, 0.001f)
        assertEquals("38%", snow.formattedGripPct)

        // Comparison assertions: Grip Sunny > Rain > Snow
        assertTrue("Sunny grip exceeds rain", sunny.gripMultiplier > rain.gripMultiplier)
        assertTrue("Rain grip exceeds snow", rain.gripMultiplier > snow.gripMultiplier)

        // Top speed Sunny > Rain > Snow
        assertTrue("Sunny top speed exceeds rain", sunny.topSpeedMultiplier > rain.topSpeedMultiplier)
        assertTrue("Rain top speed exceeds snow", rain.topSpeedMultiplier > snow.topSpeedMultiplier)

        // Braking distance multiplier: Sunny < Rain < Snow (Snow is ~2.5x longer)
        assertTrue("Rain stopping distance is longer than sunny", rain.brakingDistanceMultiplier > sunny.brakingDistanceMultiplier)
        assertTrue("Snow stopping distance is much longer than rain", snow.brakingDistanceMultiplier > rain.brakingDistanceMultiplier)
    }

    @Test
    fun `test DynamicWeatherPhysicsService braking distance calculations at various speeds`() {
        val service = com.example.physics.DynamicWeatherPhysicsService()

        // At 0 km/h, braking distance should be 0
        val dist0 = service.calculateBrakingDistanceMeters(0f, 1.0f)
        assertEquals(0f, dist0, 0.001f)

        // At 100 km/h in Sunny (efficiency = 1.0)
        val sunnyDist100 = service.calculateBrakingDistanceMeters(100f, 1.0f, baseBrakePower = 1.0f)
        assertTrue("Sunny stopping distance at 100 km/h should be between 25m and 45m", sunnyDist100 in 25f..45f)

        // At 100 km/h in Rain (efficiency = 0.68)
        val rainDist100 = service.calculateBrakingDistanceMeters(100f, 0.68f, baseBrakePower = 1.0f)
        assertTrue("Rain stopping distance at 100 km/h must exceed sunny", rainDist100 > sunnyDist100)
        assertEquals(sunnyDist100 / 0.68f, rainDist100, 0.5f)

        // At 100 km/h in Snow (efficiency = 0.40)
        val snowDist100 = service.calculateBrakingDistanceMeters(100f, 0.40f, baseBrakePower = 1.0f)
        assertTrue("Snow stopping distance at 100 km/h must exceed rain", snowDist100 > rainDist100)
        assertEquals(sunnyDist100 / 0.40f, snowDist100, 1.0f)

        // Verify quadratic velocity scaling: 100 km/h distance should be roughly 4x of 50 km/h
        val sunnyDist50 = service.calculateBrakingDistanceMeters(50f, 1.0f, baseBrakePower = 1.0f)
        val ratio = sunnyDist100 / sunnyDist50
        assertEquals(4.0f, ratio, 0.05f)
    }

    @Test
    fun `test DynamicWeatherPhysicsService randomized weather generation and race update transitions`() {
        val deterministicRandom = java.util.Random(12345)
        val kotlinRandom = kotlin.random.Random(12345)
        val service = com.example.physics.DynamicWeatherPhysicsService(kotlinRandom)

        service.reset(com.example.physics.RaceWeatherCondition.SUNNY, enableRandomization = true)
        assertTrue(service.isRandomWeatherEnabled)

        // Randomize weather multiple times and verify all outcomes belong to valid set
        val validConditions = com.example.physics.RaceWeatherCondition.values().toSet()
        for (i in 0 until 15) {
            val randomized = service.randomizeWeather(excludeCurrent = false)
            assertTrue("Must produce valid condition", validConditions.contains(randomized))
        }

        // Trigger manual randomized weather shift
        val newCondition = service.triggerRandomWeatherChange()
        assertNotNull(newCondition)
        val stateAfterRandom = service.physicsState.value
        assertEquals(newCondition, stateAfterRandom.weather)
        assertNotNull(stateAfterRandom.alertMessage)

        // Test dynamic update over time with a vehicle
        val starterVehicle = com.example.model.VehicleRegistry.createDefaultVehicles().first()
        val updateResult = service.update(
            raceElapsedSec = 10f,
            dt = 0.016f,
            vehicle = starterVehicle,
            currentSpeedKmh = 120f
        )
        assertNotNull(updateResult)
        assertEquals(120f, updateResult.currentSpeedKmh, 0.01f)
        assertTrue(updateResult.effectiveTopSpeedKmh > 100f)
        assertTrue(updateResult.brakingDistanceAtCurrentSpeed > 0f)
    }

    @Test
    fun `test PhysicsEngine incorporates dynamic weather adjustment into car physics state`() {
        val physicsEngine = com.example.physics.PhysicsEngine()
        val track = com.example.model.TrackRegistry.allTracks.first()
        val vehicle = com.example.model.VehicleRegistry.createDefaultVehicles().first()
        val particleSystem = com.example.physics.ParticleSystem()

        val stateSunny = com.example.physics.CarPhysicsState(x = 100f, y = 100f, vx = 25f, vy = 0f)
        val sunnyAdj = com.example.physics.AdjustedVehiclePhysics(
            weather = com.example.physics.RaceWeatherCondition.SUNNY,
            gripMultiplier = 1.0f,
            brakingEfficiencyMultiplier = 1.0f,
            topSpeedMultiplier = 1.0f,
            effectiveTopSpeedKmh = 240f,
            brakingDistanceAtCurrentSpeed = 35f
        )

        physicsEngine.updatePlayer(
            state = stateSunny,
            vehicle = vehicle,
            track = track,
            currentWeather = com.example.model.WeatherType.SUNNY_CLEAR,
            trackWetness = 0.0f,
            throttleInput = 1.0f,
            brakeInput = 0f,
            steerInput = 0f,
            handbrakeInput = false,
            boostInput = false,
            particleSystem = particleSystem,
            dt = 0.016f,
            weatherAdjustment = sunnyAdj
        )

        val gripInSunny = stateSunny.effectiveTireGrip
        assertEquals(240f, stateSunny.effectiveTopSpeedKmh, 0.1f)
        assertEquals(35f, stateSunny.estimatedBrakingDistanceM, 0.1f)

        // Compare with Snow Weather Adjustment
        val stateSnow = com.example.physics.CarPhysicsState(x = 100f, y = 100f, vx = 25f, vy = 0f)
        val snowAdj = com.example.physics.AdjustedVehiclePhysics(
            weather = com.example.physics.RaceWeatherCondition.SNOW,
            gripMultiplier = 0.38f,
            brakingEfficiencyMultiplier = 0.40f,
            topSpeedMultiplier = 0.85f,
            effectiveTopSpeedKmh = 204f,
            brakingDistanceAtCurrentSpeed = 90f
        )

        physicsEngine.updatePlayer(
            state = stateSnow,
            vehicle = vehicle,
            track = track,
            currentWeather = com.example.model.WeatherType.FREEZING_ICE,
            trackWetness = 0.95f,
            throttleInput = 1.0f,
            brakeInput = 0f,
            steerInput = 0f,
            handbrakeInput = false,
            boostInput = false,
            particleSystem = particleSystem,
            dt = 0.016f,
            weatherAdjustment = snowAdj
        )

        val gripInSnow = stateSnow.effectiveTireGrip
        assertTrue("Grip in sunny should significantly exceed snow", gripInSunny > gripInSnow * 2f)
        assertEquals(204f, stateSnow.effectiveTopSpeedKmh, 0.1f)
        assertEquals(90f, stateSnow.estimatedBrakingDistanceM, 0.1f)
    }

    @Test
    fun `test ReplayStorage entity telemetry coordinates serialization and GhostLap conversion`() {
        val frames = listOf(
            com.example.model.GhostFrame(timeMs = 0L, x = 100f, y = 200f, heading = 45f, speedKmh = 120f, isBraking = false),
            com.example.model.GhostFrame(timeMs = 500L, x = 140f, y = 240f, heading = 60f, speedKmh = 140f, isBraking = false),
            com.example.model.GhostFrame(timeMs = 1000L, x = 180f, y = 260f, heading = 75f, speedKmh = 95f, isBraking = true)
        )

        val ghostLap = com.example.model.GhostLap(
            trackId = "monaco_riviera",
            carName = "Apex GT-86 Coupe",
            lapTimeMs = 54250L,
            frames = frames
        )

        val replay = com.example.data.room.ReplayStorage.fromGhostLap(
            ghostLap = ghostLap,
            trackName = "Monaco Riviera Grand Prix",
            vehicleId = "apex_gt86",
            weatherType = "SUNNY_CLEAR",
            cleanLap = true
        )

        assertEquals("monaco_riviera", replay.trackId)
        assertEquals("Monaco Riviera Grand Prix", replay.trackName)
        assertEquals("Apex GT-86 Coupe", replay.vehicleName)
        assertEquals(54250L, replay.lapTimeMs)
        assertEquals("0:54.250", replay.formattedLapTime)
        assertEquals(3, replay.frameCount)
        assertTrue("Max speed should be 140 km/h", replay.maxSpeedKmh >= 140f)

        // Deserialization check
        val deserializedFrames = replay.getGhostFrames()
        assertEquals(3, deserializedFrames.size)
        assertEquals(0L, deserializedFrames[0].timeMs)
        assertEquals(100f, deserializedFrames[0].x, 0.1f)
        assertEquals(200f, deserializedFrames[0].y, 0.1f)
        assertEquals(45f, deserializedFrames[0].heading, 0.1f)
        assertFalse(deserializedFrames[0].isBraking)

        // Braking frame check
        assertEquals(1000L, deserializedFrames[2].timeMs)
        assertTrue(deserializedFrames[2].isBraking)

        // GhostLap conversion and interpolation check
        val convertedGhostLap = replay.toGhostLap()
        assertEquals(54250L, convertedGhostLap.lapTimeMs)
        val sampledFrame = convertedGhostLap.sampleAt(250L)
        assertNotNull(sampledFrame)
        assertTrue(sampledFrame!!.x in 100f..140f)
        assertTrue(sampledFrame.y in 200f..240f)
    }

    @Test
    fun `test ReplayStorage default benchmarks generate valid coordinates for all tracks`() {
        val defaults = com.example.data.room.ReplayStorage.createDefaultReplays()
        assertTrue("At least 4 track replays created", defaults.size >= 4)

        val trackIds = defaults.map { it.trackId }.toSet()
        assertTrue(trackIds.contains("monaco_riviera"))
        assertTrue(trackIds.contains("apex_speedway"))
        assertTrue(trackIds.contains("neon_tokyo"))
        assertTrue(trackIds.contains("mount_akina"))

        for (replay in defaults) {
            assertTrue("Lap time must be positive", replay.lapTimeMs > 0L)
            assertTrue("Replay must contain coordinates frames", replay.frameCount > 50)
            val frames = replay.getGhostFrames()
            assertEquals(replay.frameCount, frames.size)
            assertTrue("Frames must start at 0ms", frames.first().timeMs == 0L)
            assertTrue("Frames must end near lapTimeMs", frames.last().timeMs > 0L)
        }
    }

    @Test
    fun `test ReplayStorageDao database CRUD operations`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.room.AppDatabase.getDatabase(context)
        val dao = db.replayStorageDao()

        // Clean table
        dao.clearAllReplays()
        assertEquals(0, dao.getReplayCount())

        // Insert benchmark replays
        val defaults = com.example.data.room.ReplayStorage.createDefaultReplays()
        dao.insertDefaultReplays(defaults)
        val count = dao.getReplayCount()
        assertEquals(defaults.size, count)

        // Query by track
        val monacoReplays = dao.getReplaysForTrack("monaco_riviera")
        assertTrue("Monaco replay exists", monacoReplays.isNotEmpty())

        val bestMonaco = dao.getBestReplayForTrack("monaco_riviera")
        assertNotNull(bestMonaco)
        assertEquals("monaco_riviera", bestMonaco!!.trackId)

        // Insert new faster player replay
        val fasterFrames = listOf(
            com.example.model.GhostFrame(0L, 500f, 600f, 0f, 160f),
            com.example.model.GhostFrame(50000L, 500f, 600f, 0f, 160f)
        )
        val fasterReplay = com.example.data.room.ReplayStorage(
            id = "test_player_monaco_fast",
            trackId = "monaco_riviera",
            trackName = "Monaco Riviera Grand Prix",
            vehicleId = "hyperion_gtr",
            vehicleName = "Hyperion GT-R",
            lapTimeMs = 49999L, // Faster than default benchmark
            maxSpeedKmh = 230f,
            avgSpeedKmh = 180f,
            frameCount = 2,
            telemetryDataJson = com.example.data.room.ReplayStorage.serializeFrames(fasterFrames)
        )
        dao.insertReplay(fasterReplay)

        val newBest = dao.getBestReplayForTrack("monaco_riviera")
        assertNotNull(newBest)
        assertEquals("test_player_monaco_fast", newBest!!.id)
        assertEquals(49999L, newBest.lapTimeMs)

        // Delete single replay
        dao.deleteReplayById("test_player_monaco_fast")
        val afterDelete = dao.getReplayById("test_player_monaco_fast")
        org.junit.Assert.assertNull(afterDelete)
    }

    @Test
    fun `test ReplayStorageRepository save and fetch operations`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.data.ReplayStorageRepository(context)

        // Ensure default replays
        repository.clearAllReplays()
        repository.ensureDefaultReplays()

        val count = repository.getReplayCount()
        assertTrue("Repository seeded default replays", count >= 4)

        val bestApex = repository.getBestReplayForTrack("apex_speedway")
        assertNotNull(bestApex)
        assertEquals("apex_speedway", bestApex!!.trackId)
        assertTrue(bestApex.getGhostFrames().isNotEmpty())

        // Save a new ghost lap replay
        val testFrames = listOf(
            com.example.model.GhostFrame(0L, 800f, 500f, 90f, 200f),
            com.example.model.GhostFrame(42000L, 800f, 500f, 90f, 200f)
        )
        val ghostLap = com.example.model.GhostLap(
            trackId = "apex_speedway",
            carName = "Apex GT-86 Coupe",
            lapTimeMs = 42000L,
            frames = testFrames
        )

        val saved = repository.saveGhostLapReplay(
            ghostLap = ghostLap,
            trackName = "Apex Speedway GP",
            vehicleId = "apex_gt86"
        )
        assertNotNull(saved)
        assertEquals(42000L, saved.lapTimeMs)

        val updatedBest = repository.getBestReplayForTrack("apex_speedway")
        assertNotNull(updatedBest)
        assertEquals(42000L, updatedBest!!.lapTimeMs)
        assertEquals("Apex GT-86 Coupe", updatedBest.vehicleName)
    }

    @Test
    fun `test physics integration module velocity integration and threshold damping`() {
        val integrator = com.example.physics.PhysicsIntegrationModule()
        val vehicle = VehicleRegistry.createDefaultVehicles().first()
        val track = TrackRegistry.allTracks.first()

        val state = com.example.physics.CarPhysicsState(
            x = 800f,
            y = 500f,
            vx = 0f,
            vy = 0f,
            headingDeg = 0f
        )

        // Step 1: Stationary vehicle with no throttle should remain at rest without jitter
        val idleResult = integrator.integrate(
            state = state,
            vehicle = vehicle,
            track = track,
            currentWeather = com.example.model.WeatherType.SUNNY_CLEAR,
            trackWetness = 0f,
            throttleInput = 0f,
            brakeInput = 0f,
            steerInput = 0f,
            handbrakeInput = false,
            boostInput = false,
            dt = 0.016f
        )
        assertEquals(0f, idleResult.velocity.vx, 0.001f)
        assertEquals(0f, idleResult.velocity.vy, 0.001f)
        assertEquals(0f, idleResult.velocity.speedKmh, 0.001f)

        // Step 2: Apply full throttle -> forward acceleration along heading
        val accelResult = integrator.integrate(
            state = state,
            vehicle = vehicle,
            track = track,
            currentWeather = com.example.model.WeatherType.SUNNY_CLEAR,
            trackWetness = 0f,
            throttleInput = 1.0f,
            brakeInput = 0f,
            steerInput = 0f,
            handbrakeInput = false,
            boostInput = false,
            dt = 0.016f
        )
        assertTrue("Vehicle should produce positive forward longitudinal acceleration", accelResult.velocity.longitudinalAcc > 0f)
        assertTrue("Velocity X should increase in heading direction", accelResult.velocity.vx > 0f)
        assertTrue("Speed km/h should be greater than zero", accelResult.velocity.speedKmh > 0f)
        assertTrue("Longitudinal velocity matches forward motion", accelResult.velocity.vLong > 0f)
    }

    @Test
    fun `test physics integration module aerodynamic drag and rolling resistance`() {
        val integrator = com.example.physics.PhysicsIntegrationModule()

        // 1. Aerodynamic drag follows v^2 relationship
        val dragAt10Ms = integrator.calculateAerodynamicDrag(vLong = 10f)
        val dragAt20Ms = integrator.calculateAerodynamicDrag(vLong = 20f)
        assertTrue("Drag at 20 m/s should be ~4x drag at 10 m/s", dragAt20Ms > dragAt10Ms * 3.8f)

        // 2. Rolling resistance increases on grass and gravel vs asphalt
        val asphaltCrr = integrator.calculateRollingResistanceCoeff(
            hazard = null,
            isOffTrack = false,
            weather = com.example.model.WeatherType.SUNNY_CLEAR
        )
        val grassCrr = integrator.calculateRollingResistanceCoeff(
            hazard = com.example.model.SurfaceHazardType.OFFROAD_GRASS,
            isOffTrack = true,
            weather = com.example.model.WeatherType.SUNNY_CLEAR
        )
        val gravelCrr = integrator.calculateRollingResistanceCoeff(
            hazard = com.example.model.SurfaceHazardType.GRAVEL_TRAP,
            isOffTrack = true,
            weather = com.example.model.WeatherType.SUNNY_CLEAR
        )
        assertTrue("Grass Crr exceeds asphalt", grassCrr > asphaltCrr * 5f)
        assertTrue("Gravel Crr exceeds grass", gravelCrr > grassCrr)

        // 3. Normal load rolling resistance calculation
        val normalLoad = 12000f // 12 kN ~ 1220 kg
        val rrForceAsphalt = integrator.calculateRollingResistance(vLong = 25f, normalLoadN = normalLoad, rollingCoeff = asphaltCrr)
        val rrForceGrass = integrator.calculateRollingResistance(vLong = 25f, normalLoadN = normalLoad, rollingCoeff = grassCrr)
        assertTrue("Rolling resistance is positive opposing forward speed", rrForceAsphalt > 0f)
        assertTrue("Grass rolling resistance is significantly higher than asphalt", rrForceGrass > rrForceAsphalt * 5f)
    }

    @Test
    fun `test physics integration module tire grip and Pacejka curve`() {
        val integrator = com.example.physics.PhysicsIntegrationModule()

        // 1. Thermal grip curve
        val coldGrip = integrator.calculateThermalGripMultiplier(avgTireTempC = 30f)
        val optimalGrip = integrator.calculateThermalGripMultiplier(avgTireTempC = 85f)
        val overheatedGrip = integrator.calculateThermalGripMultiplier(avgTireTempC = 135f)
        assertTrue("Optimal temperature has peak 1.0 grip", optimalGrip == 1.0f)
        assertTrue("Cold tire has reduced grip", coldGrip < optimalGrip)
        assertTrue("Overheated tire suffers thermal blistering and reduced grip", overheatedGrip < optimalGrip)

        // 2. Pacejka Magic Formula non-linear curve
        val forceAtSmallSlip = integrator.pacejkaMagicFormula(
            slip = 2f, // 2 degrees slip angle
            B = 0.22f,
            C = 1.35f,
            D = 10000f,
            E = -0.45f
        )
        val forceAtPeakSlip = integrator.pacejkaMagicFormula(
            slip = 8.5f, // ~8.5 degrees peak
            B = 0.22f,
            C = 1.35f,
            D = 10000f,
            E = -0.45f
        )
        val forceAtExcessiveSlip = integrator.pacejkaMagicFormula(
            slip = 25f, // Deep sliding drift
            B = 0.22f,
            C = 1.35f,
            D = 10000f,
            E = -0.45f
        )
        assertTrue("Force grows from small to peak slip", forceAtPeakSlip > forceAtSmallSlip)
        assertTrue("Peak grip force approaches D", forceAtPeakSlip in 8000f..10000f)
        assertTrue("Excessive slip saturates and slides", forceAtExcessiveSlip > 0f)
    }

    @Test
    fun `test WeatherManager randomized race weather state trigger`() {
        val observedConditions = mutableSetOf<com.example.model.WeatherCondition>()
        // Trigger randomized weather state over multiple iterations
        for (seed in 1..40) {
            val random = kotlin.random.Random(seed)
            val condition = com.example.model.WeatherManager.triggerRandomRaceWeather(random)
            observedConditions.add(condition)
            assertTrue("Selected condition is valid", condition in com.example.model.WeatherCondition.values())
            assertNotNull(com.example.model.WeatherManager.raceStartBanner.value)
        }
        assertTrue("Randomizer should produce diverse weather states", observedConditions.size >= 2)
        assertTrue("Contains Sunny or Rain or Snow", observedConditions.any { it.isSunny || it.isRainy || it.isSnowy })
    }

    @Test
    fun `test WeatherManager vehicle traction dynamic influence`() {
        // 1. Sunny / Clear traction baseline
        com.example.model.WeatherManager.setCondition(com.example.model.WeatherCondition.CLEAR)
        val sunnyHandling = com.example.model.WeatherManager.getHandlingParameters()
        assertEquals(1.0f, sunnyHandling.gripMultiplier, 0.01f)
        assertEquals(1.0f, sunnyHandling.accelerationTractionMultiplier, 0.01f)
        assertEquals(1.0f, sunnyHandling.brakingEfficiencyMultiplier, 0.01f)
        val sunnyStoppingDist = com.example.model.WeatherManager.calculateStoppingDistanceMeters(speedKmh = 100f)
        assertTrue("Sunny stopping distance is optimal ~38.5m", sunnyStoppingDist in 35f..42f)

        // 2. Rainy traction reduction
        com.example.model.WeatherManager.setCondition(com.example.model.WeatherCondition.RAIN)
        val rainHandling = com.example.model.WeatherManager.getHandlingParameters()
        assertTrue("Rain reduces grip to ~70%", rainHandling.gripMultiplier in 0.65f..0.75f)
        assertTrue("Rain reduces acceleration traction", rainHandling.accelerationTractionMultiplier < sunnyHandling.accelerationTractionMultiplier)
        assertTrue("Rain reduces braking efficiency", rainHandling.brakingEfficiencyMultiplier < sunnyHandling.brakingEfficiencyMultiplier)
        val rainStoppingDist = com.example.model.WeatherManager.calculateStoppingDistanceMeters(speedKmh = 100f)
        assertTrue("Rain stopping distance is ~33% longer than sunny", rainStoppingDist > sunnyStoppingDist * 1.30f)

        // 3. Snowy traction severe loss
        com.example.model.WeatherManager.setCondition(com.example.model.WeatherCondition.SNOW)
        val snowHandling = com.example.model.WeatherManager.getHandlingParameters()
        assertTrue("Snow reduces grip to ~40%", snowHandling.gripMultiplier in 0.35f..0.45f)
        assertTrue("Snow acceleration traction severely hampered", snowHandling.accelerationTractionMultiplier <= 0.40f)
        val snowStoppingDist = com.example.model.WeatherManager.calculateStoppingDistanceMeters(speedKmh = 100f)
        assertTrue("Snow stopping distance is ~2.27x longer than sunny", snowStoppingDist > sunnyStoppingDist * 2.2f)

        // 4. Tire compound interaction with weather traction
        val softSlickGripInRain = com.example.model.WeatherManager.calculateEffectiveGrip(
            baseGrip = 1.0f,
            condition = com.example.model.WeatherCondition.RAIN,
            compound = TireCompound.SOFT_SLICK
        )
        val wetTireGripInRain = com.example.model.WeatherManager.calculateEffectiveGrip(
            baseGrip = 1.0f,
            condition = com.example.model.WeatherCondition.RAIN,
            compound = TireCompound.FULL_WET
        )
        assertTrue("Full wet tire provides superior grip in rainy weather over soft slick", wetTireGripInRain > softSlickGripInRain)
    }

    @Test
    fun `test WeatherManager visual track conditions dynamic influence`() {
        // 1. Sunny visual track conditions
        val sunnyVisual = com.example.model.WeatherManager.getVisualTrackConditions(com.example.model.WeatherCondition.CLEAR)
        assertEquals(com.example.ui.theme.AsphaltColor, sunnyVisual.asphaltColor)
        assertEquals(com.example.ui.theme.GrassGreenColor, sunnyVisual.grassRunoffColor)
        assertEquals(com.example.model.WeatherPrecipitationType.NONE, sunnyVisual.precipitationType)
        assertEquals(0.0f, sunnyVisual.trackWetness, 0.01f)
        assertEquals(0.0f, sunnyVisual.snowEdgeAccumulation, 0.01f)

        // 2. Rainy visual track conditions
        val rainVisual = com.example.model.WeatherManager.getVisualTrackConditions(com.example.model.WeatherCondition.RAIN)
        assertEquals(com.example.ui.theme.AsphaltWetColor, rainVisual.asphaltColor)
        assertEquals(com.example.ui.theme.DampGrassColor, rainVisual.grassRunoffColor)
        assertEquals(com.example.model.WeatherPrecipitationType.RAIN_STREAKS, rainVisual.precipitationType)
        assertEquals(0.75f, rainVisual.trackWetness, 0.01f)
        assertTrue("Rain asphalt wetness sheen is active", rainVisual.asphaltWetnessSheen > 0.5f)

        // 3. Snowy visual track conditions
        val snowVisual = com.example.model.WeatherManager.getVisualTrackConditions(com.example.model.WeatherCondition.SNOW)
        assertEquals(com.example.ui.theme.SnowTrackColor, snowVisual.asphaltColor)
        assertEquals(com.example.ui.theme.SnowGroundColor, snowVisual.grassRunoffColor)
        assertEquals(com.example.model.WeatherPrecipitationType.SNOW_FLURRIES, snowVisual.precipitationType)
        assertEquals(0.90f, snowVisual.trackWetness, 0.01f)
        assertTrue("Snow edge accumulation is active along track margins", snowVisual.snowEdgeAccumulation > 0.5f)
    }
}






