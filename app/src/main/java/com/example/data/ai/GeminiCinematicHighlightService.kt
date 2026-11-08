package com.example.data.ai

import android.util.Log
import com.example.data.room.ReplayStorage
import com.example.model.CameraShotType
import com.example.model.CinematicHighlightReel
import com.example.model.CinematicScene
import com.example.model.VisualFilterType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private const val TAG = "GeminiCinematicHighlight"

/**
 * Service integrating Gemini Omni Flash via Firebase AI Logic (`Firebase.ai`)
 * to generate cinematic directorial highlight reels from race telemetry physics data
 * captured by ReplayManager.
 */
class GeminiCinematicHighlightService(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // Mandated lazy initialization to avoid class construction crashes
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
            }
        )
    }

    /**
     * Generates a complete cinematic highlight reel based on [ReplayStorage] physics data.
     */
    suspend fun generateHighlightReel(replay: ReplayStorage): CinematicHighlightReel = withContext(ioDispatcher) {
        val inflections = TelemetryPhysicsAnalyzer.analyzeReplay(replay)
        val prompt = buildDirectorPrompt(replay, inflections)

        var geminiResponseText: String? = null
        try {
            // Mandated 30-60s timeout for Gemini model inference
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            geminiResponseText = response.text
            Log.d(TAG, "Gemini Omni Flash generated highlight script:\n$geminiResponseText")
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Gemini generation timed out, using physics-directed synthesis", e)
        } catch (e: CancellationException) {
            // Mandated re-throw of CancellationException
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic inference error: ${e.message}", e)
        }

        // Parse structured scenes from Gemini or build rich physics-based director cut
        return@withContext parseOrSynthesizeReel(
            replay = replay,
            inflections = inflections,
            rawAiResponse = geminiResponseText
        )
    }

    private fun buildDirectorPrompt(
        replay: ReplayStorage,
        inflections: List<TelemetryInflectionPoint>
    ): String {
        val inflectionDescriptions = StringBuilder()
        inflections.forEachIndexed { idx, point ->
            inflectionDescriptions.append(
                "- Key Moment #${idx + 1} (${point.eventType.name}) at ${point.timeMs}ms: " +
                        "${point.telemetryDescription}, speed: ${point.speedKmh.toInt()} km/h, " +
                        "heading: ${point.headingDeg.toInt()}°, braking: ${point.isBraking}, lateralG: ${point.lateralGForce}G\n"
            )
        }

        return """
            You are an elite motorsport broadcast director producing a short, dramatic cinematic video highlight reel from captured race telemetry.
            
            RACE TELEMETRY METRICS:
            Circuit Track: ${replay.trackName}
            Vehicle: ${replay.vehicleName}
            Lap Time: ${replay.formattedLapTime}
            Max Speed: ${replay.maxSpeedKmh.toInt()} KM/H
            Average Speed: ${replay.avgSpeedKmh.toInt()} KM/H
            Weather Conditions: ${replay.weatherType}
            Clean Driving Lap: ${replay.cleanLap}
            
            CAPTURED PHYSICS INFLECTION POINTS:
            $inflectionDescriptions
            
            DIRECTORIAL TASK:
            Direct a 4-to-5 scene cinematic video highlight reel showcasing this driver's most breathtaking telemetry moments.
            For each scene, choose:
            - cameraAngle: One of [CHASE_CAM, DRONE_SWOOP, FRONT_BUMPER, COCKPIT_POV, WHEEL_BLUR, HELICOPTER_OVERHEAD]
            - visualFilter: One of [NEON_CYBERPUNK, GOLDEN_HOUR, DRAMATIC_MONOCHROME, HIGH_VELOCITY_BLUR, RAIN_STREAK]
            - speedMultiplier: 0.35 (Slow-Mo apex), 0.5 (Half-Speed drift), 1.0 (Realtime roar), 1.5 (High Speed straight)
            - title: Short dramatic punchy scene title (e.g. "SMOKING CHICANE SLIP", "APEX BRAKING CLIFF", "CHECKERED FLAG CHARGE")
            - directorCommentary: 1-2 sentences of enthusiastic, broadcast-level play-by-play racing commentary.
            - telemetryCallout: Highlighting physics metrics (e.g. "SPEED: 218 KM/H • LATERAL G: 2.4G • BRAKE: 90%")
            - musicMood: One of [CYBER_SYNTH_PULSE, EPIC_ORCHESTRAL, INTENSE_PERCUSSION]
            
            Format your response strictly as valid JSON with:
            {
              "overallDirectorSummary": "1-2 sentence dramatic summary of the lap performance",
              "cinematicPacing": "High-octane / Technical / Aggressive",
              "scenes": [
                {
                  "sceneNumber": 1,
                  "title": "SCENE TITLE",
                  "cameraAngle": "CHASE_CAM",
                  "visualFilter": "HIGH_VELOCITY_BLUR",
                  "speedMultiplier": 1.0,
                  "directorCommentary": "Broadcast commentary text here",
                  "telemetryCallout": "SPEED: 215 KM/H • LATERAL G: 1.2G",
                  "musicMood": "CYBER_SYNTH_PULSE"
                }
              ]
            }
        """.trimIndent()
    }

    private fun parseOrSynthesizeReel(
        replay: ReplayStorage,
        inflections: List<TelemetryInflectionPoint>,
        rawAiResponse: String?
    ): CinematicHighlightReel {
        if (!rawAiResponse.isNullOrBlank()) {
            try {
                // Find JSON object boundary in case model included markdown fences
                val jsonStart = rawAiResponse.indexOf('{')
                val jsonEnd = rawAiResponse.lastIndexOf('}')
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    val cleanJson = rawAiResponse.substring(jsonStart, jsonEnd + 1)
                    val root = JSONObject(cleanJson)
                    val summary = root.optString("overallDirectorSummary", "High-speed masterclass on telemetry limits.")
                    val pacing = root.optString("cinematicPacing", "Dynamic Motorsport Direction")
                    val scenesArray = root.optJSONArray("scenes")

                    if (scenesArray != null && scenesArray.length() > 0) {
                        val parsedScenes = mutableListOf<CinematicScene>()
                        for (i in 0 until scenesArray.length()) {
                            val obj = scenesArray.getJSONObject(i)
                            val matchedPoint = if (i < inflections.size) inflections[i] else inflections.last()
                            val camName = obj.optString("cameraAngle", "CHASE_CAM")
                            val filterName = obj.optString("visualFilter", "NEON_CYBERPUNK")

                            val cameraShot = runCatching { CameraShotType.valueOf(camName) }.getOrDefault(
                                getDefaultCamera(i)
                            )
                            val visualFilter = runCatching { VisualFilterType.valueOf(filterName) }.getOrDefault(
                                getDefaultFilter(replay.weatherType, i)
                            )

                            val speedMul = obj.optDouble("speedMultiplier", 1.0).toFloat().coerceIn(0.25f, 2.0f)
                            val title = obj.optString("title", "DYNAMIC HIGHLIGHT #${i + 1}")
                            val commentary = obj.optString("directorCommentary", matchedPoint.telemetryDescription)
                            val callout = obj.optString("telemetryCallout", "SPEED: ${matchedPoint.speedKmh.toInt()} KM/H • ${matchedPoint.lateralGForce}G")
                            val mood = obj.optString("musicMood", "CYBER_SYNTH_PULSE")

                            val startMs = (matchedPoint.timeMs - 1200L).coerceAtLeast(0L)
                            val endMs = (matchedPoint.timeMs + 2300L).coerceAtMost(replay.lapTimeMs)
                            val duration = ((endMs - startMs) / 1000f).coerceAtLeast(2.5f)

                            parsedScenes.add(
                                CinematicScene(
                                    sceneNumber = i + 1,
                                    title = title,
                                    cameraAngle = cameraShot,
                                    visualFilter = visualFilter,
                                    speedMultiplier = speedMul,
                                    directorCommentary = commentary,
                                    telemetryCallout = callout,
                                    musicMood = mood,
                                    startTimeMs = startMs,
                                    endTimeMs = endMs,
                                    durationSec = duration,
                                    peakSpeedKmh = matchedPoint.speedKmh,
                                    isBrakingScene = matchedPoint.isBraking,
                                    lateralGForce = matchedPoint.lateralGForce
                                )
                            )
                        }

                        if (parsedScenes.isNotEmpty()) {
                            return CinematicHighlightReel(
                                id = "highlight_${replay.id}_${System.currentTimeMillis()}",
                                trackId = replay.trackId,
                                trackName = replay.trackName,
                                vehicleName = replay.vehicleName,
                                lapTimeMs = replay.lapTimeMs,
                                formattedLapTime = replay.formattedLapTime,
                                overallDirectorSummary = summary,
                                cinematicPacing = pacing,
                                scenes = parsedScenes
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing Gemini response, falling back to deterministic synthesis", e)
            }
        }

        // Deterministic director cut based on telemetry inflection points
        return synthesizePhysicsDirectorCut(replay, inflections)
    }

    private fun synthesizePhysicsDirectorCut(
        replay: ReplayStorage,
        inflections: List<TelemetryInflectionPoint>
    ): CinematicHighlightReel {
        val scenes = mutableListOf<CinematicScene>()

        inflections.forEachIndexed { index, point ->
            val camera = when (point.eventType) {
                InflectionEventType.TOP_SPEED_STRAIGHT -> CameraShotType.FRONT_BUMPER
                InflectionEventType.HEAVY_BRAKING_ZONE -> CameraShotType.WHEEL_BLUR
                InflectionEventType.APEX_CORNER_ENTRY -> CameraShotType.DRONE_SWOOP
                InflectionEventType.HIGH_SLIP_DRIFT -> CameraShotType.CHASE_CAM
                InflectionEventType.SECTOR_SPLIT_TRANSITION -> CameraShotType.COCKPIT_POV
                InflectionEventType.FINISH_LINE_SPRINT -> CameraShotType.HELICOPTER_OVERHEAD
            }

            val filter = when (index % 4) {
                0 -> VisualFilterType.HIGH_VELOCITY_BLUR
                1 -> VisualFilterType.NEON_CYBERPUNK
                2 -> VisualFilterType.GOLDEN_HOUR
                else -> if (replay.weatherType.contains("RAIN")) VisualFilterType.RAIN_STREAK else VisualFilterType.DRAMATIC_MONOCHROME
            }

            val speedMultiplier = when (point.eventType) {
                InflectionEventType.APEX_CORNER_ENTRY -> 0.40f // Dramatic slow-mo apex
                InflectionEventType.HEAVY_BRAKING_ZONE -> 0.50f
                InflectionEventType.TOP_SPEED_STRAIGHT -> 1.25f // Rush
                else -> 1.0f
            }

            val title = when (point.eventType) {
                InflectionEventType.TOP_SPEED_STRAIGHT -> "TERMINAL VELOCITY STRETCH"
                InflectionEventType.HEAVY_BRAKING_ZONE -> "THRESHOLD BRAKING COMPLEX"
                InflectionEventType.APEX_CORNER_ENTRY -> "SURGICAL APEX CLIPPING"
                InflectionEventType.HIGH_SLIP_DRIFT -> "HIGH-G LATERAL DRIFT SLIDE"
                InflectionEventType.SECTOR_SPLIT_TRANSITION -> "COCKPIT SECTOR SPLIT"
                InflectionEventType.FINISH_LINE_SPRINT -> "CHECKERED FLAG VICTORY CHARGE"
            }

            val commentary = when (point.eventType) {
                InflectionEventType.TOP_SPEED_STRAIGHT ->
                    "Wringing out every last horsepower from the ${replay.vehicleName}, rocketing down the tarmac at ${point.speedKmh.toInt()} km/h!"
                InflectionEventType.HEAVY_BRAKING_ZONE ->
                    "Brake rotors glowing incandescent! Maximum hydraulic bite hauling the machine down into the turning pocket."
                InflectionEventType.APEX_CORNER_ENTRY ->
                    "Incredible steering precision! Kissing the kerb with ${point.lateralGForce}G of lateral bite through the apex."
                InflectionEventType.HIGH_SLIP_DRIFT ->
                    "Rear end stepping out in a graceful drift angle, balancing throttle feathering with tire smoke!"
                InflectionEventType.SECTOR_SPLIT_TRANSITION ->
                    "Stamping down fast splits with flawless rhythm across the transition kerbs."
                InflectionEventType.FINISH_LINE_SPRINT ->
                    "Crossing the start-finish line to cement a blistering ${replay.formattedLapTime} benchmark!"
            }

            val startMs = (point.timeMs - 1200L).coerceAtLeast(0L)
            val endMs = (point.timeMs + 2400L).coerceAtMost(replay.lapTimeMs)
            val duration = ((endMs - startMs) / 1000f).coerceAtLeast(3.2f)

            scenes.add(
                CinematicScene(
                    sceneNumber = index + 1,
                    title = title,
                    cameraAngle = camera,
                    visualFilter = filter,
                    speedMultiplier = speedMultiplier,
                    directorCommentary = commentary,
                    telemetryCallout = "SPEED: ${point.speedKmh.toInt()} KM/H • LATERAL: ${point.lateralGForce}G • ROTATION: ${point.headingDeg.toInt()}°",
                    musicMood = if (point.isBraking) "INTENSE_PERCUSSION" else "CYBER_SYNTH_PULSE",
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    durationSec = duration,
                    peakSpeedKmh = point.speedKmh,
                    isBrakingScene = point.isBraking,
                    lateralGForce = point.lateralGForce
                )
            )
        }

        return CinematicHighlightReel(
            id = "highlight_${replay.id}_${System.currentTimeMillis()}",
            trackId = replay.trackId,
            trackName = replay.trackName,
            vehicleName = replay.vehicleName,
            lapTimeMs = replay.lapTimeMs,
            formattedLapTime = replay.formattedLapTime,
            overallDirectorSummary = "A masterclass in precision driving around ${replay.trackName}, capturing peak apex speeds and threshold deceleration.",
            cinematicPacing = "High-Octane Cinematic Cut",
            scenes = scenes
        )
    }

    private fun getDefaultCamera(index: Int): CameraShotType = when (index % 6) {
        0 -> CameraShotType.CHASE_CAM
        1 -> CameraShotType.DRONE_SWOOP
        2 -> CameraShotType.FRONT_BUMPER
        3 -> CameraShotType.COCKPIT_POV
        4 -> CameraShotType.WHEEL_BLUR
        else -> CameraShotType.HELICOPTER_OVERHEAD
    }

    private fun getDefaultFilter(weather: String, index: Int): VisualFilterType = when {
        weather.contains("RAIN") -> VisualFilterType.RAIN_STREAK
        index % 3 == 0 -> VisualFilterType.NEON_CYBERPUNK
        index % 3 == 1 -> VisualFilterType.GOLDEN_HOUR
        else -> VisualFilterType.HIGH_VELOCITY_BLUR
    }
}
