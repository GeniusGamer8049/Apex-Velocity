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
}
