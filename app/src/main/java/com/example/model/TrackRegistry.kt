package com.example.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object TrackRegistry {

    val allTracks: List<Track> by lazy {
        listOf(
            createMonacoRiviera(),
            createApexSpeedway(),
            createNeonTokyo(),
            createMountAkina()
        )
    }

    fun getTrackById(id: String): Track {
        return allTracks.find { it.id == id } ?: allTracks.first()
    }

    private fun createMonacoRiviera(): Track {
        // High-profile Mediterranean coastal circuit with chicane, seaside straight, and harbor hairpin
        val points = mutableListOf<TrackWaypoint>()
        val centerX = 1600f
        val centerY = 1400f

        // Loop points definition (oval with harbor chicane and hairpin)
        val rawNodes = listOf(
            // Start straight
            TrackWaypoint(1200f, 600f, 150f, isStartFinish = true, isCheckpoint = true, targetSpeedKmh = 190f),
            TrackWaypoint(1600f, 600f, 150f, targetSpeedKmh = 210f),
            TrackWaypoint(2000f, 620f, 150f, isCheckpoint = true, targetSpeedKmh = 230f),
            // Turn 1 Ste. Devote
            TrackWaypoint(2350f, 750f, 140f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 110f),
            TrackWaypoint(2500f, 1000f, 140f, targetSpeedKmh = 140f),
            // Uphill sweep
            TrackWaypoint(2550f, 1350f, 140f, isCheckpoint = true, targetSpeedKmh = 160f),
            TrackWaypoint(2400f, 1700f, 140f, isApexKerb = true, kerbSide = -1, targetSpeedKmh = 120f),
            // Casino square
            TrackWaypoint(2100f, 1950f, 140f, targetSpeedKmh = 150f),
            TrackWaypoint(1700f, 2150f, 140f, isCheckpoint = true, targetSpeedKmh = 140f),
            // Grand Hotel Hairpin (tight 180 turn)
            TrackWaypoint(1300f, 2200f, 130f, isApexKerb = true, kerbSide = -1, targetSpeedKmh = 75f),
            TrackWaypoint(1000f, 2050f, 130f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 70f),
            TrackWaypoint(850f, 1800f, 130f, targetSpeedKmh = 95f),
            // Harbor chicane
            TrackWaypoint(800f, 1450f, 135f, isCheckpoint = true, targetSpeedKmh = 130f),
            TrackWaypoint(920f, 1250f, 135f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 115f),
            TrackWaypoint(750f, 1050f, 135f, isApexKerb = true, kerbSide = -1, targetSpeedKmh = 110f),
            // Swimming pool section & Tabac
            TrackWaypoint(850f, 800f, 140f, targetSpeedKmh = 135f),
            TrackWaypoint(1000f, 650f, 140f, isCheckpoint = true, targetSpeedKmh = 160f)
        )

        val waypoints = interpolateWaypoints(rawNodes, steps = 5)

        val patches = listOf(
            SurfacePatch("monaco_puddle_1", SurfaceHazardType.PUDDLE, 880f, 1250f, 45f),
            SurfacePatch("monaco_puddle_2", SurfaceHazardType.PUDDLE, 1700f, 2150f, 40f),
            SurfacePatch("monaco_kerb_1", SurfaceHazardType.KERB_RUMBLE, 2350f, 750f, 35f),
            SurfacePatch("monaco_kerb_2", SurfaceHazardType.KERB_RUMBLE, 1000f, 2050f, 30f)
        )

        return Track(
            id = "monaco_riviera",
            name = "Monaco Riviera Grand Prix",
            location = "Monte Carlo, Monaco",
            lengthMeters = 3337,
            turnsCount = 19,
            defaultWeather = WeatherType.SUNNY_CLEAR,
            dynamicWeatherProgression = listOf(
                0 to WeatherType.SUNNY_CLEAR,
                35 to WeatherType.OVERCAST,
                65 to WeatherType.LIGHT_RAIN,
                110 to WeatherType.THUNDERSTORM
            ),
            surfacePatches = patches,
            waypoints = waypoints,
            targetLapTimeMs = 54_000L,
            description = "The crown jewel of street racing. Tight harbor barriers, the famous hairpin, and sudden coastal rain showers test precision and tire selection.",
            unlockReputation = 0
        )
    }

    private fun createApexSpeedway(): Track {
        // High speed modern GP track with sweeping turns and high downforce requirements
        val rawNodes = listOf(
            TrackWaypoint(800f, 500f, 160f, isStartFinish = true, isCheckpoint = true, targetSpeedKmh = 230f),
            TrackWaypoint(1400f, 500f, 160f, targetSpeedKmh = 260f),
            TrackWaypoint(2000f, 520f, 160f, isCheckpoint = true, targetSpeedKmh = 280f),
            // High-speed right hand sweeper
            TrackWaypoint(2500f, 700f, 150f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 175f),
            TrackWaypoint(2750f, 1100f, 150f, targetSpeedKmh = 190f),
            TrackWaypoint(2600f, 1600f, 150f, isCheckpoint = true, targetSpeedKmh = 170f),
            // Infield technical section
            TrackWaypoint(2100f, 1800f, 140f, isApexKerb = true, kerbSide = -1, targetSpeedKmh = 125f),
            TrackWaypoint(1600f, 1650f, 140f, targetSpeedKmh = 145f),
            TrackWaypoint(1300f, 1950f, 140f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 110f),
            TrackWaypoint(950f, 2100f, 140f, isCheckpoint = true, targetSpeedKmh = 135f),
            // Fast stadium curve
            TrackWaypoint(600f, 1750f, 150f, isApexKerb = true, kerbSide = -1, targetSpeedKmh = 160f),
            TrackWaypoint(500f, 1200f, 150f, targetSpeedKmh = 185f),
            TrackWaypoint(550f, 750f, 155f, isCheckpoint = true, targetSpeedKmh = 200f)
        )

        val waypoints = interpolateWaypoints(rawNodes, steps = 6)

        val patches = listOf(
            SurfacePatch("apex_puddle_1", SurfaceHazardType.PUDDLE, 1600f, 1650f, 50f),
            SurfacePatch("apex_puddle_2", SurfaceHazardType.PUDDLE, 2750f, 1100f, 55f),
            SurfacePatch("apex_kerb_1", SurfaceHazardType.KERB_RUMBLE, 2500f, 700f, 40f),
            SurfacePatch("apex_kerb_2", SurfaceHazardType.KERB_RUMBLE, 1300f, 1950f, 35f),
            SurfacePatch("apex_kerb_3", SurfaceHazardType.KERB_RUMBLE, 600f, 1750f, 40f)
        )

        return Track(
            id = "apex_speedway",
            name = "Apex International Speedway",
            location = "Silverstone, UK",
            lengthMeters = 4890,
            turnsCount = 14,
            defaultWeather = WeatherType.SUNNY_CLEAR,
            dynamicWeatherProgression = listOf(
                0 to WeatherType.SUNNY_CLEAR,
                40 to WeatherType.OVERCAST,
                80 to WeatherType.THUNDERSTORM,
                140 to WeatherType.LIGHT_RAIN
            ),
            surfacePatches = patches,
            waypoints = waypoints,
            targetLapTimeMs = 62_000L,
            description = "High-speed sweeping aerodromes where downforce and engine power reign supreme. Dynamic summer storms frequently drench the asphalt mid-race.",
            unlockReputation = 150
        )
    }

    private fun createNeonTokyo(): Track {
        // High-octane Tokyo midnight expressway with neon signs and underpasses
        val rawNodes = listOf(
            TrackWaypoint(900f, 600f, 145f, isStartFinish = true, isCheckpoint = true, targetSpeedKmh = 210f),
            TrackWaypoint(1500f, 600f, 145f, targetSpeedKmh = 240f),
            TrackWaypoint(2100f, 650f, 140f, isCheckpoint = true, targetSpeedKmh = 200f),
            // Shinjuku 90-degree street curve
            TrackWaypoint(2450f, 900f, 135f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 95f),
            TrackWaypoint(2450f, 1400f, 140f, targetSpeedKmh = 160f),
            TrackWaypoint(2350f, 1850f, 135f, isCheckpoint = true, targetSpeedKmh = 120f),
            // Shibuya crossover
            TrackWaypoint(1950f, 2100f, 135f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 100f),
            TrackWaypoint(1450f, 2050f, 140f, targetSpeedKmh = 170f),
            TrackWaypoint(1050f, 2250f, 135f, isApexKerb = true, kerbSide = -1, targetSpeedKmh = 105f),
            TrackWaypoint(700f, 2000f, 135f, isCheckpoint = true, targetSpeedKmh = 140f),
            // Ginza underpass
            TrackWaypoint(650f, 1450f, 140f, targetSpeedKmh = 175f),
            TrackWaypoint(680f, 950f, 140f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 130f)
        )

        val waypoints = interpolateWaypoints(rawNodes, steps = 5)

        val patches = listOf(
            SurfacePatch("tokyo_puddle_1", SurfaceHazardType.PUDDLE, 1450f, 2050f, 48f),
            SurfacePatch("tokyo_oil_1", SurfaceHazardType.OIL_SLICK, 2450f, 1150f, 38f),
            SurfacePatch("tokyo_puddle_2", SurfaceHazardType.PUDDLE, 650f, 1450f, 50f)
        )

        return Track(
            id = "neon_tokyo",
            name = "Tokyo Midnight Expressway",
            location = "Tokyo, Japan",
            lengthMeters = 3850,
            turnsCount = 16,
            defaultWeather = WeatherType.LIGHT_RAIN,
            dynamicWeatherProgression = listOf(
                0 to WeatherType.LIGHT_RAIN,
                45 to WeatherType.THUNDERSTORM,
                90 to WeatherType.FOG_MIST
            ),
            surfacePatches = patches,
            waypoints = waypoints,
            targetLapTimeMs = 57_500L,
            description = "Neon-lit asphalt skyscraper canyons. Wet road reflections and highway puddles make throttle management and intermediate tires essential.",
            unlockReputation = 450
        )
    }

    private fun createMountAkina(): Track {
        // Touge canyon downhill mountain pass with treacherous freezing summit & ice patches
        val rawNodes = listOf(
            TrackWaypoint(800f, 450f, 130f, isStartFinish = true, isCheckpoint = true, targetSpeedKmh = 160f),
            TrackWaypoint(1300f, 500f, 130f, targetSpeedKmh = 180f),
            // Hairpin 1
            TrackWaypoint(1700f, 650f, 125f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 80f),
            TrackWaypoint(1400f, 950f, 125f, targetSpeedKmh = 120f),
            // Hairpin 2
            TrackWaypoint(950f, 1100f, 125f, isApexKerb = true, kerbSide = -1, targetSpeedKmh = 75f),
            TrackWaypoint(1250f, 1350f, 125f, isCheckpoint = true, targetSpeedKmh = 135f),
            // Hairpin 3
            TrackWaypoint(1750f, 1500f, 125f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 75f),
            TrackWaypoint(1500f, 1800f, 125f, targetSpeedKmh = 125f),
            // Hairpin 4 & Summit pass
            TrackWaypoint(1000f, 1950f, 120f, isApexKerb = true, kerbSide = -1, targetSpeedKmh = 70f),
            TrackWaypoint(650f, 1800f, 125f, isCheckpoint = true, targetSpeedKmh = 110f),
            TrackWaypoint(550f, 1350f, 130f, targetSpeedKmh = 140f),
            TrackWaypoint(500f, 850f, 130f, isApexKerb = true, kerbSide = 1, targetSpeedKmh = 120f)
        )

        val waypoints = interpolateWaypoints(rawNodes, steps = 6)

        val patches = listOf(
            SurfacePatch("akina_ice_1", SurfaceHazardType.ICE_PATCH, 1750f, 1500f, 42f),
            SurfacePatch("akina_ice_2", SurfaceHazardType.ICE_PATCH, 1000f, 1950f, 40f),
            SurfacePatch("akina_ice_3", SurfaceHazardType.ICE_PATCH, 650f, 1800f, 38f)
        )

        return Track(
            id = "mount_akina",
            name = "Mount Akina Touge Pass",
            location = "Gunma Prefecture, Japan",
            lengthMeters = 3150,
            turnsCount = 18,
            defaultWeather = WeatherType.SUNNY_CLEAR,
            dynamicWeatherProgression = listOf(
                0 to WeatherType.SUNNY_CLEAR,
                30 to WeatherType.FOG_MIST,
                70 to WeatherType.FREEZING_ICE
            ),
            surfacePatches = patches,
            waypoints = waypoints,
            targetLapTimeMs = 52_000L,
            description = "The ultimate downhill touge challenge. Hairpin gutters, thick mountain fog, and black ice patches near the summit demand supreme drift mastery.",
            unlockReputation = 800
        )
    }

    /**
     * Catmull-Rom spline interpolation to turn raw corner nodes into a buttery smooth loop.
     */
    private fun interpolateWaypoints(nodes: List<TrackWaypoint>, steps: Int): List<TrackWaypoint> {
        val result = mutableListOf<TrackWaypoint>()
        val count = nodes.size
        for (i in 0 until count) {
            val p0 = nodes[(i - 1 + count) % count]
            val p1 = nodes[i]
            val p2 = nodes[(i + 1) % count]
            val p3 = nodes[(i + 2) % count]

            for (step in 0 until steps) {
                val t = step.toFloat() / steps.toFloat()
                val t2 = t * t
                val t3 = t2 * t

                // Catmull-Rom spline formula
                val x = 0.5f * ((2f * p1.x) +
                        (-p0.x + p2.x) * t +
                        (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
                        (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3)

                val y = 0.5f * ((2f * p1.y) +
                        (-p0.y + p2.y) * t +
                        (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                        (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3)

                val width = p1.width + (p2.width - p1.width) * t
                val targetSpeed = p1.targetSpeedKmh + (p2.targetSpeedKmh - p1.targetSpeedKmh) * t

                val isKerb = (step == 0 && p1.isApexKerb) || (step == 1 && p1.isApexKerb)
                val kerbSide = if (isKerb) p1.kerbSide else 0

                result.add(
                    TrackWaypoint(
                        x = x,
                        y = y,
                        width = width,
                        isApexKerb = isKerb,
                        kerbSide = kerbSide,
                        targetSpeedKmh = targetSpeed,
                        isCheckpoint = (step == 0 && p1.isCheckpoint),
                        isStartFinish = (step == 0 && p1.isStartFinish)
                    )
                )
            }
        }
        return result
    }
}
