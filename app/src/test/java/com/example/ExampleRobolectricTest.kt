package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RacingRepository
import com.example.model.AchievementCategory
import com.example.model.AchievementRegistry
import com.example.model.BadgeRarity
import com.example.model.CareerRegistry
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
}


