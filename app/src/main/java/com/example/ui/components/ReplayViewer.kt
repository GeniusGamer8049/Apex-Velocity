package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.example.data.ai.GeminiCinematicHighlightService
import com.example.model.CinematicHighlightReel
import kotlinx.coroutines.launch
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReplayStorageRepository
import com.example.data.room.ReplayStorage
import com.example.model.GhostFrame
import com.example.model.GhostLap
import com.example.model.Track
import com.example.model.TrackRegistry
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

/**
 * Interactive ReplayViewer UI component that loads [ReplayStorage] telemetry data
 * from Room and visualizes the ghost car path, coordinates, and braking zones
 * with full playback controls (scrubber, speed multiplier, loop, play/pause).
 */
@Composable
fun ReplayViewer(
    initialTrackId: String = TrackRegistry.allTracks.first().id,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    repository: ReplayStorageRepository = ReplayStorageRepository.getInstance(LocalContext.current),
    replayManager: com.example.data.ReplayManager = com.example.data.ReplayManager(LocalContext.current)
) {
    BackHandler { onClose() }

    val tracks = TrackRegistry.allTracks
    var selectedTrackId by remember { mutableStateOf(initialTrackId) }
    val currentTrack = tracks.find { it.id == selectedTrackId } ?: tracks.first()

    val replayEntity by repository.getBestReplayForTrackFlow(selectedTrackId).collectAsState(initial = null)

    // Ensure default benchmark ghost replays exist
    LaunchedEffect(Unit) {
        repository.ensureDefaultReplays()
    }

    val ghostLap = remember(replayEntity) {
        replayEntity?.toGhostLap() ?: GhostLap(
            trackId = currentTrack.id,
            carName = "Hyperion GT-R",
            lapTimeMs = currentTrack.targetLapTimeMs,
            frames = emptyList()
        )
    }

    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentPlaybackTimeMs by remember { mutableLongStateOf(0L) }
    var isLooping by remember { mutableStateOf(true) }

    val totalDurationMs = remember(replayEntity, currentTrack) {
        (replayEntity?.lapTimeMs ?: currentTrack.targetLapTimeMs).coerceAtLeast(1000L)
    }

    // Reset playback position when changing tracks
    LaunchedEffect(selectedTrackId) {
        currentPlaybackTimeMs = 0L
        isPlaying = true
    }

    // 60 FPS animation ticker loop for replay timeline playback
    LaunchedEffect(isPlaying, playbackSpeed, totalDurationMs, isLooping) {
        if (!isPlaying) return@LaunchedEffect
        var lastNanos = 0L

        while (true) {
            withFrameNanos { nowNanos ->
                if (lastNanos != 0L) {
                    val deltaSec = (nowNanos - lastNanos) / 1_000_000_000f
                    val deltaMs = (deltaSec * 1000f * playbackSpeed).toLong()
                    val newTime = currentPlaybackTimeMs + deltaMs

                    if (newTime >= totalDurationMs) {
                        if (isLooping) {
                            currentPlaybackTimeMs = 0L
                        } else {
                            currentPlaybackTimeMs = totalDurationMs
                            isPlaying = false
                        }
                    } else {
                        currentPlaybackTimeMs = newTime
                    }
                }
                lastNanos = nowNanos
            }
        }
    }

    val currentGhostFrame = remember(ghostLap, currentPlaybackTimeMs) {
        ghostLap.sampleAt(currentPlaybackTimeMs)
    }

    val coroutineScope = rememberCoroutineScope()
    val geminiHighlightService = remember { GeminiCinematicHighlightService() }
    var showCinematicModal by remember { mutableStateOf(false) }
    var isGeneratingHighlights by remember { mutableStateOf(false) }
    var activeReel by remember { mutableStateOf<CinematicHighlightReel?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .testTag("replay_viewer")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Header: Title, Track Selector, and Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            .background(ApexCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Replay Mode",
                            tint = ApexCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "GHOST REPLAY VIEWER",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${currentTrack.name} • ${replayEntity?.vehicleName ?: "Telemetry Ghost"}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Gemini Omni Flash Cinematic Video Highlight Trigger Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ApexCyan.copy(alpha = 0.18f))
                            .border(1.dp, ApexCyan, RoundedCornerShape(8.dp))
                            .clickable(enabled = !isGeneratingHighlights) {
                                val currentReplay = replayEntity
                                if (currentReplay != null) {
                                    isGeneratingHighlights = true
                                    coroutineScope.launch {
                                        try {
                                            val reel = geminiHighlightService.generateHighlightReel(currentReplay)
                                            activeReel = reel
                                            showCinematicModal = true
                                        } finally {
                                            isGeneratingHighlights = false
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("replay_gemini_highlight_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (isGeneratingHighlights) {
                                CircularProgressIndicator(color = ApexCyan, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Text("DIRECTING...", color = ApexCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(14.dp))
                                Text("GEMINI HIGHLIGHTS", color = ApexCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp).testTag("replay_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Replay Viewer",
                            tint = TextWhite
                        )
                    }
                }
            }

            // Track Selector Tabs (Monaco Riviera, Apex Speedway, Neon Tokyo, Mount Akina)
            TabRow(
                selectedTabIndex = tracks.indexOfFirst { it.id == selectedTrackId }.coerceAtLeast(0),
                containerColor = DarkSurface,
                contentColor = ApexCyan,
                indicator = { tabPositions ->
                    val tabIdx = tracks.indexOfFirst { it.id == selectedTrackId }.coerceAtLeast(0)
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabIdx]),
                        color = ApexCyan
                    )
                }
            ) {
                tracks.forEach { track ->
                    val isSelected = track.id == selectedTrackId
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTrackId = track.id },
                        text = {
                            Text(
                                text = track.name.substringBefore(" Grand").uppercase(),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) ApexCyan else TextMuted
                            )
                        },
                        modifier = Modifier.testTag("replay_track_tab_${track.id}")
                    )
                }
            }

            // Main 2D Track Map & Ghost Path Visualizer Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                ReplayCanvas(
                    track = currentTrack,
                    ghostLap = ghostLap,
                    currentFrame = currentGhostFrame,
                    modifier = Modifier.fillMaxSize().testTag("replay_canvas")
                )

                // Top Floating Telemetry Readout HUD
                ReplayTelemetryHud(
                    ghostFrame = currentGhostFrame,
                    replayEntity = replayEntity,
                    currentLapTimeMs = currentPlaybackTimeMs,
                    totalDurationMs = totalDurationMs,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )

                // Bottom Floating Track Info Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CarbonBlack.copy(alpha = 0.85f))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${ghostLap.frames.size} COORD NODES RECORDED",
                        color = ApexCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Interactive Playback Control Bar
            ReplayControlBar(
                isPlaying = isPlaying,
                isLooping = isLooping,
                playbackSpeed = playbackSpeed,
                currentTimeMs = currentPlaybackTimeMs,
                totalDurationMs = totalDurationMs,
                onPlayPauseToggle = { isPlaying = !isPlaying },
                onSeek = { currentPlaybackTimeMs = it.toLong() },
                onSkip = { offsetMs ->
                    currentPlaybackTimeMs = (currentPlaybackTimeMs + offsetMs).coerceIn(0L, totalDurationMs)
                },
                onRestart = { currentPlaybackTimeMs = 0L },
                onToggleLoop = { isLooping = !isLooping },
                onChangeSpeed = { playbackSpeed = it },
            )
        }

        // Overlay: AI Cinematic Highlight Video Reel Player
        if (showCinematicModal && activeReel != null && replayEntity != null) {
            CinematicHighlightViewer(
                reel = activeReel!!,
                replay = replayEntity!!,
                onClose = { showCinematicModal = false },
                onRegenerate = {
                    val currentReplay = replayEntity
                    if (currentReplay != null) {
                        coroutineScope.launch {
                            isGeneratingHighlights = true
                            try {
                                activeReel = geminiHighlightService.generateHighlightReel(currentReplay)
                            } finally {
                                isGeneratingHighlights = false
                            }
                        }
                    }
                },
                isGenerating = isGeneratingHighlights,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 2D Canvas rendering the complete track circuit geometry and the animated ghost car path.
 */
@Composable
private fun ReplayCanvas(
    track: Track,
    ghostLap: GhostLap,
    currentFrame: GhostFrame?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val waypoints = track.waypoints
        if (waypoints.isEmpty()) return@Canvas

        // Calculate auto-fit bounding box from waypoints and ghost frames
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

        for (f in ghostLap.frames) {
            if (f.x < minX) minX = f.x
            if (f.x > maxX) maxX = f.x
            if (f.y < minY) minY = f.y
            if (f.y > maxY) maxY = f.y
        }

        val trackWidth = (maxX - minX).coerceAtLeast(100f)
        val trackHeight = (maxY - minY).coerceAtLeast(100f)

        val padding = 45f
        val availableW = size.width - padding * 2f
        val availableH = size.height - padding * 2f

        val scale = (availableW / trackWidth).coerceAtMost(availableH / trackHeight)
        val offsetX = padding + (availableW - trackWidth * scale) * 0.5f - minX * scale
        val offsetY = padding + (availableH - trackHeight * scale) * 0.5f - minY * scale

        // 1. Draw Asphalt Track Circuit Ribbon
        val trackPath = Path()
        waypoints.forEachIndexed { i, wp ->
            val cx = wp.x * scale + offsetX
            val cy = wp.y * scale + offsetY
            if (i == 0) trackPath.moveTo(cx, cy) else trackPath.lineTo(cx, cy)
        }
        trackPath.close()

        // Track asphalt base outline
        drawPath(
            path = trackPath,
            color = Color(0xFF262D3D),
            style = Stroke(width = 24f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = trackPath,
            color = Color(0xFF141824),
            style = Stroke(width = 16f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 2. Draw Full Recorded Ghost Trajectory Path (Stored coordinates from Room)
        val frames = ghostLap.frames
        if (frames.size >= 2) {
            // Draw continuous ghost trajectory glow path
            val ghostPath = Path()
            frames.forEachIndexed { idx, f ->
                val gx = f.x * scale + offsetX
                val gy = f.y * scale + offsetY
                if (idx == 0) ghostPath.moveTo(gx, gy) else ghostPath.lineTo(gx, gy)
            }

            // Holographic background glow
            drawPath(
                path = ghostPath,
                color = ApexCyan.copy(alpha = 0.35f),
                style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Primary trajectory path line
            drawPath(
                path = ghostPath,
                color = ApexCyan.copy(alpha = 0.85f),
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw colored braking zones along ghost path (red/orange where driver applied brakes)
            for (i in 0 until frames.size - 1) {
                val f1 = frames[i]
                val f2 = frames[i + 1]
                if (f1.isBraking || f2.isBraking) {
                    val p1 = Offset(f1.x * scale + offsetX, f1.y * scale + offsetY)
                    val p2 = Offset(f2.x * scale + offsetX, f2.y * scale + offsetY)
                    drawLine(
                        color = ApexRed,
                        start = p1,
                        end = p2,
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // 3. Draw Start/Finish Line Indicator
        if (waypoints.isNotEmpty()) {
            val startWp = waypoints.first()
            val sx = startWp.x * scale + offsetX
            val sy = startWp.y * scale + offsetY
            drawCircle(color = ApexGreen, radius = 7f, center = Offset(sx, sy))
            drawCircle(color = Color.White, radius = 3.5f, center = Offset(sx, sy))
        }

        // 4. Draw Animated Ghost Car at Current Frame
        if (currentFrame != null) {
            val carX = currentFrame.x * scale + offsetX
            val carY = currentFrame.y * scale + offsetY

            translate(left = carX, top = carY) {
                rotate(degrees = currentFrame.heading) {
                    val carLength = 22f
                    val carWidth = 12f

                    // Translucent Holographic Aura
                    drawCircle(
                        color = (if (currentFrame.isBraking) ApexRed else ApexCyan).copy(alpha = 0.35f),
                        radius = carLength * 0.9f
                    )

                    // Ghost car body
                    val body = Path().apply {
                        moveTo(carLength * 0.5f, 0f)
                        lineTo(carLength * 0.35f, -carWidth * 0.5f)
                        lineTo(-carLength * 0.45f, -carWidth * 0.5f)
                        lineTo(-carLength * 0.5f, 0f)
                        lineTo(-carLength * 0.45f, carWidth * 0.5f)
                        lineTo(carLength * 0.35f, carWidth * 0.5f)
                        close()
                    }

                    drawPath(
                        path = body,
                        color = if (currentFrame.isBraking) ApexRed.copy(alpha = 0.75f) else ApexCyan.copy(alpha = 0.75f)
                    )
                    drawPath(
                        path = body,
                        color = Color.White,
                        style = Stroke(width = 1.5f)
                    )

                    // Front heading indicator dot
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = Offset(carLength * 0.45f, 0f)
                    )
                }
            }
        }
    }
}

/**
 * Floating HUD displaying live speed, braking status, and coordinates of the ghost car.
 */
@Composable
private fun ReplayTelemetryHud(
    ghostFrame: GhostFrame?,
    replayEntity: ReplayStorage?,
    currentLapTimeMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("replay_telemetry_hud"),
        colors = CardDefaults.cardColors(containerColor = CarbonBlack.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Speedometer & Braking Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val speed = ghostFrame?.speedKmh ?: 0f
                Text(
                    text = "${speed.toInt()} KM/H",
                    color = ApexCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                val isBraking = ghostFrame?.isBraking == true
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isBraking) ApexRed else ApexGreen.copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isBraking) "BRAKING" else "ACCEL",
                        color = if (isBraking) Color.White else ApexGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Elapsed Time / Total Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = ApexGold, modifier = Modifier.size(12.dp))
                Text(
                    text = "${formatTimeMs(currentLapTimeMs)} / ${formatTimeMs(totalDurationMs)}",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Coordinates Readout
            if (ghostFrame != null) {
                Text(
                    text = String.format(Locale.US, "X: %.0f  Y: %.0f | HDG: %.0f°", ghostFrame.x, ghostFrame.y, ghostFrame.heading),
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Interactive timeline scrubber and playback control bar.
 */
@Composable
private fun ReplayControlBar(
    isPlaying: Boolean,
    isLooping: Boolean,
    playbackSpeed: Float,
    currentTimeMs: Long,
    totalDurationMs: Long,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkip: (Long) -> Unit,
    onRestart: () -> Unit,
    onToggleLoop: () -> Unit,
    onChangeSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Scrubber Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatTimeMs(currentTimeMs),
                    color = ApexCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Slider(
                    value = currentTimeMs.toFloat(),
                    onValueChange = onSeek,
                    valueRange = 0f..totalDurationMs.toFloat(),
                    modifier = Modifier.weight(1f).testTag("replay_scrubber_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = ApexCyan,
                        activeTrackColor = ApexCyan,
                        inactiveTrackColor = Color(0xFF262D3D)
                    )
                )

                Text(
                    text = formatTimeMs(totalDurationMs),
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Action Buttons Row: Restart, -5s, Play/Pause, +5s, Speed, Loop
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Restart Button
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier.size(40.dp).testTag("replay_restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Restart Replay",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Skip Back 5s
                IconButton(
                    onClick = { onSkip(-5000L) },
                    modifier = Modifier.size(40.dp).testTag("replay_skip_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 5 Seconds",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Primary Play / Pause Button (Touch target >= 48dp)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ApexCyan)
                        .clickable(onClick = onPlayPauseToggle)
                        .testTag("replay_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Replay" else "Play Replay",
                        tint = CarbonBlack,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Skip Forward 5s
                IconButton(
                    onClick = { onSkip(5000L) },
                    modifier = Modifier.size(40.dp).testTag("replay_skip_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Fast Forward 5 Seconds",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Loop Toggle Button
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier.size(40.dp).testTag("replay_loop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Toggle Loop Replay",
                        tint = if (isLooping) ApexCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Speed Selector Pill (0.5x, 1x, 2x, 4x)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            val nextSpeed = when (playbackSpeed) {
                                0.5f -> 1.0f
                                1.0f -> 2.0f
                                2.0f -> 4.0f
                                else -> 0.5f
                            }
                            onChangeSpeed(nextSpeed)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("replay_speed_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        color = ApexGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun formatTimeMs(timeMs: Long): String {
    val totalSec = timeMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val ms = (timeMs % 1000) / 10
    return String.format(Locale.US, "%d:%02d.%02d", min, sec, ms)
}
