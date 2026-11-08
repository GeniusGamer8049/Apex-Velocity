package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * Camera perspective for cinematic highlight generation.
 */
enum class CameraShotType(val label: String, val description: String) {
    CHASE_CAM("Chase Tracking", "Dynamic third-person chase camera tracking behind vehicle with motion blur"),
    DRONE_SWOOP("Drone Swoop", "FPV drone sweeping low across the apex line and banking into corner exit"),
    FRONT_BUMPER("Asphalt Bumper POV", "Ultra-low ground level perspective showing asphalt rush and kerb impacts"),
    COCKPIT_POV("Cockpit View", "Driver helmet view with dashboard tachometer and rapid counter-steering"),
    WHEEL_BLUR("Wheel & Apex Cam", "Trackside close-up showing glowing brake discs, tire slip, and kerb vibration"),
    HELICOPTER_OVERHEAD("Aerial Heli Track", "Cinematic high-altitude wide orbital angle showing racing line geometry")
}

/**
 * Visual color grading and lens filters for cinematic highlights.
 */
enum class VisualFilterType(
    val label: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val hasVignette: Boolean = true
) {
    NEON_CYBERPUNK("Neon Cyberpunk", 0xFF00E5FF, 0xFFFF007F),
    GOLDEN_HOUR("Golden Sunset Flare", 0xFFFFB300, 0xFFFF5722),
    DRAMATIC_MONOCHROME("Noir High Contrast", 0xFFE0E0E0, 0xFF212121),
    HIGH_VELOCITY_BLUR("Velocity Streaks", 0xFF00E676, 0xFF18FFFF),
    RAIN_STREAK("Rain Streaked Lens", 0xFF80D8FF, 0xFF37474F);

    val primaryColor: Color get() = Color(primaryColorHex)
    val accentColor: Color get() = Color(accentColorHex)
}

/**
 * Represents a single director scene cut generated from race telemetry.
 */
data class CinematicScene(
    val sceneNumber: Int,
    val title: String,
    val cameraAngle: CameraShotType,
    val visualFilter: VisualFilterType,
    val speedMultiplier: Float, // 0.35f (Slow-mo), 0.5f, 1.0f (Realtime), 1.5f (Rush)
    val directorCommentary: String,
    val telemetryCallout: String,
    val musicMood: String = "CYBER_SYNTH_PULSE",
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationSec: Float = 3.5f,
    val peakSpeedKmh: Float = 180f,
    val isBrakingScene: Boolean = false,
    val lateralGForce: Float = 1.8f
) {
    val formattedDuration: String get() = String.format(Locale.US, "%.1fs", durationSec)
}

/**
 * Complete highlight reel directed from telemetry.
 */
data class CinematicHighlightReel(
    val id: String,
    val trackId: String,
    val trackName: String,
    val vehicleName: String,
    val lapTimeMs: Long,
    val formattedLapTime: String,
    val overallDirectorSummary: String,
    val cinematicPacing: String,
    val scenes: List<CinematicScene>,
    val generatedAt: Long = System.currentTimeMillis()
) {
    val totalDurationSec: Float
        get() = scenes.map { it.durationSec }.sum()

    val formattedTotalDuration: String
        get() = String.format(Locale.US, "%.1f SECONDS", totalDurationSec)
}

/**
 * Sealed interface for UI state during highlight generation.
 */
sealed interface CinematicHighlightUiState {
    data object Idle : CinematicHighlightUiState
    data class AnalyzingTelemetry(val step: String) : CinematicHighlightUiState
    data class GeneratingWithGemini(val promptPreview: String) : CinematicHighlightUiState
    data class Success(val reel: CinematicHighlightReel) : CinematicHighlightUiState
    data class Error(val message: String) : CinematicHighlightUiState
}
