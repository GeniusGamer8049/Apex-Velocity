package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.room.ReplayStorage
import com.example.model.CameraShotType
import com.example.model.CinematicHighlightReel
import com.example.model.CinematicHighlightUiState
import com.example.model.CinematicScene
import com.example.model.GhostFrame
import com.example.model.GhostLap
import com.example.model.Track
import com.example.model.TrackRegistry
import com.example.model.VisualFilterType
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.ApexRed
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dedicated Cinematic Highlight Video Player rendering AI-directed camera angles,
 * visual filters, slow-motion apex replays, director commentary subtitles,
 * and telemetry physics HUD overlays.
 */
@Composable
fun CinematicHighlightViewer(
    reel: CinematicHighlightReel,
    replay: ReplayStorage,
    onClose: () -> Unit,
    onRegenerate: () -> Unit,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    val track = remember(reel.trackId) {
        TrackRegistry.getTrackById(reel.trackId)
    }

    val ghostLap = remember(replay) {
        replay.toGhostLap()
    }

    var activeSceneIndex by remember { mutableIntStateOf(0) }
    val currentScene = reel.scenes.getOrNull(activeSceneIndex) ?: reel.scenes.first()

    var isPlaying by remember { mutableStateOf(true) }
    var speedMultiplierOverride by remember { mutableFloatStateOf(currentScene.speedMultiplier) }
    var currentReplayTimeMs by remember { mutableLongStateOf(currentScene.startTimeMs) }
    var isAudioEnabled by remember { mutableStateOf(true) }

    // When user changes active scene, snap playback position and speed
    LaunchedEffect(activeSceneIndex) {
        currentReplayTimeMs = currentScene.startTimeMs
        speedMultiplierOverride = currentScene.speedMultiplier
        isPlaying = true
    }

    // High precision frame ticker for cinematic video rendering
    LaunchedEffect(isPlaying, speedMultiplierOverride, activeSceneIndex) {
        if (!isPlaying) return@LaunchedEffect
        var lastNanos = 0L

        while (true) {
            withFrameNanos { nowNanos ->
                if (lastNanos != 0L) {
                    val deltaSec = (nowNanos - lastNanos) / 1_000_000_000f
                    val effectiveSpeed = speedMultiplierOverride.coerceAtLeast(0.2f)
                    val deltaMs = (deltaSec * 1000f * effectiveSpeed).toLong()

                    val nextTime = currentReplayTimeMs + deltaMs
                    if (nextTime >= currentScene.endTimeMs) {
                        // Advance to next scene or loop current scene
                        if (activeSceneIndex < reel.scenes.size - 1) {
                            activeSceneIndex++
                        } else {
                            activeSceneIndex = 0
                            currentReplayTimeMs = reel.scenes.first().startTimeMs
                        }
                    } else {
                        currentReplayTimeMs = nextTime
                    }
                }
                lastNanos = nowNanos
            }
        }
    }

    val currentFrame = remember(ghostLap, currentReplayTimeMs) {
        ghostLap.sampleAt(currentReplayTimeMs)
    }

    // Animation ticker for pulse effects and scanner lines
    val infiniteTransition = rememberInfiniteTransition(label = "cinematic_effects")
    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanlines"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("cinematic_highlight_viewer")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Cinematic Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ApexCyan.copy(alpha = 0.2f))
                            .border(1.dp, ApexCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI Director",
                            tint = ApexCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "GEMINI OMNI FLASH",
                                color = ApexCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ApexGold.copy(alpha = 0.25f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("DIRECTOR'S CUT", color = ApexGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "${reel.trackName} • ${reel.vehicleName} (${reel.formattedLapTime})",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onRegenerate,
                        enabled = !isGenerating,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("cinematic_regenerate_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = ApexCyan, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Replay, contentDescription = "Regenerate Highlights", tint = TextWhite)
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("cinematic_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Highlight Reel", tint = TextWhite)
                    }
                }
            }

            // 2. Primary 2.39:1 Anamorphic Widescreen Video Stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF07090E))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
            ) {
                // Interactive Cinematic Canvas
                CinematicVideoCanvas(
                    track = track,
                    ghostLap = ghostLap,
                    currentFrame = currentFrame,
                    currentScene = currentScene,
                    scanlineOffset = scanlineOffset,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("cinematic_video_canvas")
                )

                // Top Left Overlay: Camera Angle & Visual Filter Tag
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .border(1.dp, currentScene.visualFilter.primaryColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = currentScene.visualFilter.primaryColor, modifier = Modifier.size(12.dp))
                            Text(
                                text = currentScene.cameraAngle.label.uppercase(),
                                color = TextWhite,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentScene.visualFilter.label.uppercase(),
                            color = currentScene.visualFilter.accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (speedMultiplierOverride < 1.0f) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ApexOrange.copy(alpha = 0.3f))
                                .border(1.dp, ApexOrange, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "SLOW-MO ${speedMultiplierOverride}x",
                                color = ApexOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Top Right Overlay: Live Speedometer & G-Meter Telemetry HUD
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .testTag("cinematic_telemetry_hud"),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val currentSpeed = currentFrame?.speedKmh ?: currentScene.peakSpeedKmh
                        Text(
                            text = "${currentSpeed.toInt()} KM/H",
                            color = currentScene.visualFilter.primaryColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "LATERAL: ${String.format(Locale.US, "%.1fG", currentScene.lateralGForce)}",
                            color = ApexGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Bottom Overlay: Broadcast Director Commentary Subtitle Card
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("cinematic_director_commentary"),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.88f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .border(1.dp, currentScene.visualFilter.primaryColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCENE ${currentScene.sceneNumber}: ${currentScene.title}",
                                color = currentScene.visualFilter.primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = currentScene.telemetryCallout,
                                color = ApexGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "\"${currentScene.directorCommentary}\"",
                            color = TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 3. Scene Timeline Selector Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(reel.scenes) { index, scene ->
                    val isSelected = index == activeSceneIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) currentScene.visualFilter.primaryColor.copy(alpha = 0.25f) else DarkSurface)
                            .border(
                                1.dp,
                                if (isSelected) currentScene.visualFilter.primaryColor else SurfaceBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { activeSceneIndex = index }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("cinematic_scene_tab_$index")
                    ) {
                        Column {
                            Text(
                                text = "SHOT ${index + 1}",
                                color = if (isSelected) currentScene.visualFilter.primaryColor else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = scene.title.take(18),
                                color = if (isSelected) TextWhite else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Playback Controls Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous Scene
                    IconButton(
                        onClick = {
                            if (activeSceneIndex > 0) activeSceneIndex-- else activeSceneIndex = reel.scenes.size - 1
                        },
                        modifier = Modifier.size(40.dp).testTag("cinematic_prev_button")
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Previous Scene", tint = TextWhite)
                    }

                    // Play/Pause Primary Action
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(currentScene.visualFilter.primaryColor)
                            .clickable { isPlaying = !isPlaying }
                            .testTag("cinematic_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = CarbonBlack,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Next Scene
                    IconButton(
                        onClick = {
                            if (activeSceneIndex < reel.scenes.size - 1) activeSceneIndex++ else activeSceneIndex = 0
                        },
                        modifier = Modifier.size(40.dp).testTag("cinematic_next_button")
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Next Scene", tint = TextWhite)
                    }

                    // Speed Override Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                speedMultiplierOverride = when (speedMultiplierOverride) {
                                    0.35f -> 0.5f
                                    0.5f -> 1.0f
                                    1.0f -> 1.5f
                                    else -> 0.35f
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("cinematic_speed_button")
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2fx", speedMultiplierOverride),
                            color = ApexGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Audio Mute/Unmute
                    IconButton(
                        onClick = { isAudioEnabled = !isAudioEnabled },
                        modifier = Modifier.size(40.dp).testTag("cinematic_audio_toggle")
                    ) {
                        Icon(
                            imageVector = if (isAudioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = if (isAudioEnabled) "Mute Audio" else "Enable Audio",
                            tint = if (isAudioEnabled) ApexCyan else TextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom canvas that renders the dynamic camera shot and visual filter effects.
 */
@Composable
private fun CinematicVideoCanvas(
    track: Track,
    ghostLap: GhostLap,
    currentFrame: GhostFrame?,
    currentScene: CinematicScene,
    scanlineOffset: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val waypoints = track.waypoints
        if (waypoints.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height

        // Calculate track circuit scale
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (wp in waypoints) {
            if (wp.x < minX) minX = wp.x
            if (wp.x > maxX) maxX = wp.x
            if (wp.y < minY) minY = wp.y
            if (wp.y > maxY) maxY = wp.y
        }

        val trackW = (maxX - minX).coerceAtLeast(100f)
        val trackH = (maxY - minY).coerceAtLeast(100f)

        // Adjust camera viewpoint and framing based on CameraShotType
        when (currentScene.cameraAngle) {
            CameraShotType.FRONT_BUMPER -> {
                // Low-slung asphalt perspective with speed streaks
                renderAsphaltBumperPov(
                    w = w,
                    h = h,
                    currentFrame = currentFrame,
                    filter = currentScene.visualFilter
                )
            }
            CameraShotType.COCKPIT_POV -> {
                // Driver cockpit view with dashboard tachometer
                renderCockpitPov(
                    w = w,
                    h = h,
                    currentFrame = currentFrame,
                    filter = currentScene.visualFilter
                )
            }
            CameraShotType.WHEEL_BLUR -> {
                // Close-up wheel rim with glowing brake caliper
                renderWheelCam(
                    w = w,
                    h = h,
                    currentFrame = currentFrame,
                    isBraking = currentScene.isBrakingScene,
                    filter = currentScene.visualFilter
                )
            }
            else -> {
                // 3D/Top-down tracking view (CHASE_CAM, DRONE_SWOOP, HELICOPTER_OVERHEAD)
                renderCircuitTrackingCam(
                    track = track,
                    ghostLap = ghostLap,
                    currentFrame = currentFrame,
                    currentScene = currentScene,
                    minX = minX,
                    maxX = maxX,
                    minY = minY,
                    maxY = maxY,
                    trackW = trackW,
                    trackH = trackH
                )
            }
        }

        // Apply cinematic visual filter grading overlays
        applyCinematicColorGrading(
            w = w,
            h = h,
            filter = currentScene.visualFilter,
            scanlineOffset = scanlineOffset
        )
    }
}

private fun DrawScope.renderCircuitTrackingCam(
    track: Track,
    ghostLap: GhostLap,
    currentFrame: GhostFrame?,
    currentScene: CinematicScene,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    trackW: Float,
    trackH: Float
) {
    val padding = 40f
    val availW = size.width - padding * 2f
    val availH = size.height - padding * 2f

    val baseScale = (availW / trackW).coerceAtMost(availH / trackH)
    val zoomFactor = when (currentScene.cameraAngle) {
        CameraShotType.DRONE_SWOOP -> 1.7f
        CameraShotType.CHASE_CAM -> 1.4f
        else -> 1.0f // HELICOPTER_OVERHEAD
    }
    val scale = baseScale * zoomFactor

    val carX = (currentFrame?.x ?: (minX + trackW * 0.5f)) * scale
    val carY = (currentFrame?.y ?: (minY + trackH * 0.5f)) * scale

    val centerOffsetX = if (currentScene.cameraAngle == CameraShotType.HELICOPTER_OVERHEAD) {
        padding + (availW - trackW * scale) * 0.5f - minX * scale
    } else {
        size.width * 0.5f - carX
    }

    val centerOffsetY = if (currentScene.cameraAngle == CameraShotType.HELICOPTER_OVERHEAD) {
        padding + (availH - trackH * scale) * 0.5f - minY * scale
    } else {
        size.height * 0.55f - carY
    }

    // Draw asphalt track ribbon
    val waypoints = track.waypoints
    val trackPath = Path()
    waypoints.forEachIndexed { i, wp ->
        val px = wp.x * scale + centerOffsetX
        val py = wp.y * scale + centerOffsetY
        if (i == 0) trackPath.moveTo(px, py) else trackPath.lineTo(px, py)
    }
    trackPath.close()

    // Asphalt surface & curbs
    drawPath(
        path = trackPath,
        color = Color(0xFF1B202D),
        style = Stroke(width = 28f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = trackPath,
        color = Color(0xFF0F131C),
        style = Stroke(width = 18f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Stored ghost trajectory
    val frames = ghostLap.frames
    if (frames.size >= 2) {
        val ghostPath = Path()
        frames.forEachIndexed { idx, f ->
            val gx = f.x * scale + centerOffsetX
            val gy = f.y * scale + centerOffsetY
            if (idx == 0) ghostPath.moveTo(gx, gy) else ghostPath.lineTo(gx, gy)
        }
        drawPath(
            path = ghostPath,
            color = currentScene.visualFilter.primaryColor.copy(alpha = 0.35f),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }

    // Render Car with motion lines
    if (currentFrame != null) {
        val cx = currentFrame.x * scale + centerOffsetX
        val cy = currentFrame.y * scale + centerOffsetY

        translate(left = cx, top = cy) {
            rotate(degrees = currentFrame.heading) {
                val carLen = 28f
                val carWid = 16f

                // Speed motion streaks behind car
                if (currentFrame.speedKmh > 100f) {
                    val streakLen = (currentFrame.speedKmh * 0.25f).coerceAtMost(60f)
                    drawLine(
                        color = currentScene.visualFilter.accentColor.copy(alpha = 0.6f),
                        start = Offset(-carLen * 0.5f, -carWid * 0.4f),
                        end = Offset(-carLen * 0.5f - streakLen, -carWid * 0.4f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = currentScene.visualFilter.accentColor.copy(alpha = 0.6f),
                        start = Offset(-carLen * 0.5f, carWid * 0.4f),
                        end = Offset(-carLen * 0.5f - streakLen, carWid * 0.4f),
                        strokeWidth = 2f
                    )
                }

                // Car Chassis
                val carBody = Path().apply {
                    moveTo(carLen * 0.5f, 0f)
                    lineTo(carLen * 0.35f, -carWid * 0.5f)
                    lineTo(-carLen * 0.45f, -carWid * 0.5f)
                    lineTo(-carLen * 0.5f, 0f)
                    lineTo(-carLen * 0.45f, carWid * 0.5f)
                    lineTo(carLen * 0.35f, carWid * 0.5f)
                    close()
                }

                drawPath(path = carBody, color = currentScene.visualFilter.primaryColor)
                drawPath(path = carBody, color = Color.White, style = Stroke(width = 1.5f))

                // Brake lights on braking
                if (currentFrame.isBraking) {
                    drawCircle(color = ApexRed, radius = 3.5f, center = Offset(-carLen * 0.48f, -carWid * 0.35f))
                    drawCircle(color = ApexRed, radius = 3.5f, center = Offset(-carLen * 0.48f, carWid * 0.35f))
                }
            }
        }
    }
}

private fun DrawScope.renderAsphaltBumperPov(
    w: Float,
    h: Float,
    currentFrame: GhostFrame?,
    filter: VisualFilterType
) {
    // Ground level asphalt rushing past with high-speed motion blur
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF0A0D14), Color(0xFF1E2536), Color(0xFF111520))
        )
    )

    // Center road dash lines
    val speed = currentFrame?.speedKmh ?: 160f
    val dashCount = 8
    for (i in 0 until dashCount) {
        val yProg = (i.toFloat() / dashCount.toFloat())
        val startY = h * (0.35f + yProg * 0.65f)
        val lineW = 6f + yProg * 18f
        val lineH = 12f + yProg * 35f

        drawRect(
            color = Color.White.copy(alpha = 0.7f),
            topLeft = Offset(w * 0.5f - lineW * 0.5f, startY),
            size = Size(lineW, lineH)
        )
    }

    // Kerb red/white curbs on sides
    val curbW = 40f
    drawRect(
        brush = Brush.verticalGradient(listOf(ApexRed, Color.White, ApexRed)),
        topLeft = Offset(0f, h * 0.4f),
        size = Size(curbW, h * 0.6f)
    )
    drawRect(
        brush = Brush.verticalGradient(listOf(Color.White, ApexRed, Color.White)),
        topLeft = Offset(w - curbW, h * 0.4f),
        size = Size(curbW, h * 0.6f)
    )

    // Front splitter bumper bar at bottom
    drawRect(
        color = filter.primaryColor,
        topLeft = Offset(w * 0.15f, h - 22f),
        size = Size(w * 0.70f, 22f)
    )
}

private fun DrawScope.renderCockpitPov(
    w: Float,
    h: Float,
    currentFrame: GhostFrame?,
    filter: VisualFilterType
) {
    // Road view through windshield
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF080C14), Color(0xFF141A26), Color(0xFF222B3D))
        )
    )

    // Cockpit dashboard surround
    val dashPath = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * 0.65f)
        lineTo(w * 0.25f, h * 0.58f)
        lineTo(w * 0.75f, h * 0.58f)
        lineTo(w, h * 0.65f)
        lineTo(w, h)
        close()
    }
    drawPath(path = dashPath, color = Color(0xFF12151D))
    drawPath(path = dashPath, color = SurfaceBorder, style = Stroke(width = 2f))

    // Digital Instrument cluster
    val speed = currentFrame?.speedKmh ?: 180f
    val rpmPct = (speed / 240f).coerceIn(0.2f, 1.0f)

    drawArc(
        color = filter.primaryColor,
        startAngle = 180f,
        sweepAngle = 180f * rpmPct,
        useCenter = false,
        topLeft = Offset(w * 0.5f - 60f, h * 0.68f),
        size = Size(120f, 120f),
        style = Stroke(width = 8f, cap = StrokeCap.Round)
    )

    // Steering wheel top arc
    drawArc(
        color = Color(0xFF2D3342),
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(w * 0.5f - 110f, h * 0.75f),
        size = Size(220f, 220f),
        style = Stroke(width = 16f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.renderWheelCam(
    w: Float,
    h: Float,
    currentFrame: GhostFrame?,
    isBraking: Boolean,
    filter: VisualFilterType
) {
    // Close-up wheel rim with brake rotor
    drawRect(color = Color(0xFF090B10))

    val centerX = w * 0.5f
    val centerY = h * 0.5f
    val rimRadius = (h * 0.42f).coerceAtLeast(80f)

    // Tire tread
    drawCircle(color = Color(0xFF181C26), radius = rimRadius * 1.25f, center = Offset(centerX, centerY))

    // Glowing brake disc rotor (incandescent red/orange when braking)
    val rotorColor = if (isBraking) ApexOrange else Color(0xFF424A5C)
    drawCircle(color = rotorColor, radius = rimRadius * 0.75f, center = Offset(centerX, centerY))

    // Wheel spokes
    for (i in 0 until 5) {
        val angle = (i * 72f) * (Math.PI / 180.0)
        val ex = centerX + cos(angle).toFloat() * rimRadius * 0.9f
        val ey = centerY + sin(angle).toFloat() * rimRadius * 0.9f
        drawLine(
            color = filter.primaryColor,
            start = Offset(centerX, centerY),
            end = Offset(ex, ey),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
    }

    // Wheel center hub
    drawCircle(color = Color.White, radius = 16f, center = Offset(centerX, centerY))
}

private fun DrawScope.applyCinematicColorGrading(
    w: Float,
    h: Float,
    filter: VisualFilterType,
    scanlineOffset: Float
) {
    // 1. Color tint wash
    drawRect(
        brush = Brush.verticalGradient(
            listOf(filter.primaryColor.copy(alpha = 0.08f), filter.accentColor.copy(alpha = 0.05f))
        )
    )

    // 2. Anamorphic lens flare bar across mid-screen
    if (filter == VisualFilterType.GOLDEN_HOUR || filter == VisualFilterType.NEON_CYBERPUNK) {
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    filter.accentColor.copy(alpha = 0.25f),
                    filter.primaryColor.copy(alpha = 0.45f),
                    filter.accentColor.copy(alpha = 0.25f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(0f, h * 0.35f),
            size = Size(w, 4f)
        )
    }

    // 3. Scanline effect
    val scanY = h * scanlineOffset
    drawLine(
        color = filter.primaryColor.copy(alpha = 0.18f),
        start = Offset(0f, scanY),
        end = Offset(w, scanY),
        strokeWidth = 2f
    )

    // 4. Cinematic Letterbox Bars (Top & Bottom Anamorphic 2.39:1)
    val barHeight = h * 0.08f
    drawRect(color = Color.Black, topLeft = Offset(0f, 0f), size = Size(w, barHeight))
    drawRect(color = Color.Black, topLeft = Offset(0f, h - barHeight), size = Size(w, barHeight))
}
