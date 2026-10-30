package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RacingRepository
import com.example.model.CareerRegistry
import com.example.model.TireCompound
import com.example.model.TrackRegistry
import com.example.model.VehicleRegistry
import com.example.model.VehicleUpgrades
import org.junit.Assert.assertEquals
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
}
