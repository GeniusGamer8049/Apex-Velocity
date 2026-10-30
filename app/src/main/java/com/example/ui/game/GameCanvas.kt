package com.example.ui.game

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.GhostFrame
import com.example.model.SurfaceHazardType
import com.example.model.SurfacePatch
import com.example.model.Track
import com.example.model.Vehicle
import com.example.model.WeatherType
import com.example.physics.AiRacer
import com.example.physics.CarPhysicsState
import com.example.physics.Particle
import com.example.physics.SkidMark
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.ApexRed
import com.example.ui.theme.AsphaltColor
import com.example.ui.theme.AsphaltWetColor
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.GrassGreenColor
import com.example.ui.theme.GravelSandColor
import com.example.ui.theme.IcePatchColor
import com.example.ui.theme.KerbRedColor
import com.example.ui.theme.KerbWhiteColor
import com.example.ui.theme.PuddleBlueColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameCanvas(
    modifier: Modifier = Modifier,
    track: Track,
    playerVehicle: Vehicle,
    playerState: CarPhysicsState,
    aiRacers: List<AiRacer>,
    ghostFrame: GhostFrame?,
    currentWeather: WeatherType,
    trackWetness: Float,
    lightningAlpha: Float,
    particles: List<Particle>,
    skidMarks: List<SkidMark>
) {
    Canvas(modifier = modifier.fillMaxSize().background(GrassGreenColor)) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. Dynamic Camera with speed zoom
        val speedKmh = playerState.speedKmh
        val zoom = (1.10f - (speedKmh / 360f) * 0.40f).coerceIn(0.70f, 1.15f)

        // Center on player
        val cx = canvasWidth / 2f
        val cy = canvasHeight / 2f

        translate(left = cx, top = cy) {
            // Apply camera zoom
            drawContext.transform.scale(zoom, zoom, Offset.Zero)
            // Camera position tracking
            translate(left = -playerState.x, top = -playerState.y) {

                // 2. Render Track Asphalt, Kerbs, and Hazards
                drawTrackSurfaces(track, trackWetness)

                // 3. Render Skid Marks
                drawSkidMarks(skidMarks)

                // 4. Render Surface Hazard Patches (Puddles, Ice, Oil)
                drawHazardPatches(track.surfacePatches)

                // 5. Render Ghost Car (if Time Trial active)
                if (ghostFrame != null) {
                    drawGhostVehicle(ghostFrame)
                }

                // 6. Render AI Racers
                for (ai in aiRacers) {
                    drawCar(
                        x = ai.state.x,
                        y = ai.state.y,
                        headingDeg = ai.state.headingDeg,
                        bodyColor = parseHexColor(ai.vehicle.bodyColorHex),
                        isBraking = ai.state.isBraking,
                        isPlayer = false,
                        underglowColor = null,
                        speedKmh = ai.state.speedKmh
                    )
                }

                // 7. Render Player Vehicle
                drawCar(
                    x = playerState.x,
                    y = playerState.y,
                    headingDeg = playerState.headingDeg,
                    bodyColor = parseHexColor(playerVehicle.bodyColorHex),
                    isBraking = playerState.isBraking,
                    isPlayer = true,
                    underglowColor = playerVehicle.underglowColorHex?.let { parseHexColor(it) },
                    speedKmh = playerState.speedKmh
                )

                // 8. Render Particles (Smoke, Water Spray, Sparks)
                drawParticles(particles)
            }
        }

        // 9. Weather Ambient Overlays (Rain Streaks, Mist, Lightning Flash)
        drawWeatherOverlay(currentWeather, trackWetness, lightningAlpha, canvasWidth, canvasHeight)

        // 10. Mini-Map in top corner
        drawMiniMap(
            track = track,
            playerState = playerState,
            aiRacers = aiRacers,
            ghostFrame = ghostFrame,
            mapWidth = 140f,
            mapHeight = 140f,
            padding = 24f
        )
    }
}

private fun DrawScope.drawTrackSurfaces(track: Track, wetness: Float) {
    val waypoints = track.waypoints
    if (waypoints.isEmpty()) return

    val asphaltColor = if (wetness > 0.3f) AsphaltWetColor else AsphaltColor

    // Outer gravel border for extra runoff detail
    val gravelPath = Path()
    val asphaltPath = Path()
    val centerLinePath = Path()

    for (i in waypoints.indices) {
        val wp = waypoints[i]
        if (i == 0) {
            asphaltPath.moveTo(wp.x, wp.y)
        } else {
            asphaltPath.lineTo(wp.x, wp.y)
        }
    }
    asphaltPath.close()

    // Draw asphalt base ribbon
    drawPath(
        path = asphaltPath,
        color = asphaltColor,
        style = Stroke(width = waypoints[0].width)
    )

    // Inner & Outer track edge lines
    drawPath(
        path = asphaltPath,
        color = Color(0xFFE0E0E0),
        style = Stroke(width = 3.5f)
    )

    // Draw Apex Kerbs (Red & White rumble strips)
    for (i in waypoints.indices) {
        val wp = waypoints[i]
        if (wp.isApexKerb) {
            val nextWp = waypoints[(i + 1) % waypoints.size]
            val dx = nextWp.x - wp.x
            val dy = nextWp.y - wp.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val perpX = -dy / len
            val perpY = dx / len
            val kerbOffset = (wp.width * 0.5f) * wp.kerbSide

            val kerbX = wp.x + perpX * kerbOffset
            val kerbY = wp.y + perpY * kerbOffset

            val kerbColor = if ((i / 2) % 2 == 0) KerbRedColor else KerbWhiteColor
            drawCircle(
                color = kerbColor,
                radius = 16f,
                center = Offset(kerbX, kerbY)
            )
        }
    }

    // Start / Finish Line Gantry
    val startWp = track.startWaypoint
    val nextWp = waypoints[1]
    val dx = nextWp.x - startWp.x
    val dy = nextWp.y - startWp.y
    val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val perpX = -dy / len
    val perpY = dx / len
    val halfW = startWp.width * 0.5f

    drawLine(
        color = Color.White,
        start = Offset(startWp.x - perpX * halfW, startWp.y - perpY * halfW),
        end = Offset(startWp.x + perpX * halfW, startWp.y + perpY * halfW),
        strokeWidth = 8f
    )
    // Checkered pattern marks
    for (step in -4..4) {
        val t = step / 4f
        val px = startWp.x + perpX * (halfW * t)
        val py = startWp.y + perpY * (halfW * t)
        if (step % 2 == 0) {
            drawCircle(color = CarbonBlack, radius = 5f, center = Offset(px, py))
        }
    }
}

private fun DrawScope.drawSkidMarks(skidMarks: List<SkidMark>) {
    for (sm in skidMarks) {
        drawLine(
            color = Color.Black.copy(alpha = sm.alpha),
            start = Offset(sm.x1, sm.y1),
            end = Offset(sm.x2, sm.y2),
            strokeWidth = sm.width
        )
    }
}

private fun DrawScope.drawHazardPatches(patches: List<SurfacePatch>) {
    for (p in patches) {
        when (p.type) {
            SurfaceHazardType.PUDDLE -> {
                // Standing water with reflective cyan sheen
                drawCircle(
                    color = PuddleBlueColor,
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = p.radius * 0.55f,
                    center = Offset(p.x, p.y)
                )
            }
            SurfaceHazardType.ICE_PATCH -> {
                // Crystalline frozen ice patch
                drawCircle(
                    color = IcePatchColor,
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = p.radius * 0.7f,
                    center = Offset(p.x, p.y),
                    style = Stroke(width = 3f)
                )
            }
            SurfaceHazardType.OIL_SLICK -> {
                drawCircle(
                    color = Color(0xCC263238),
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = ApexOrange.copy(alpha = 0.45f),
                    radius = p.radius * 0.6f,
                    center = Offset(p.x, p.y)
                )
            }
            else -> {}
        }
    }
}

private fun DrawScope.drawCar(
    x: Float,
    y: Float,
    headingDeg: Float,
    bodyColor: Color,
    isBraking: Boolean,
    isPlayer: Boolean,
    underglowColor: Color?,
    speedKmh: Float
) {
    translate(left = x, top = y) {
        rotate(degrees = headingDeg) {
            val carLength = 46f
            val carWidth = 24f

            // Underglow neon
            if (underglowColor != null) {
                drawRoundRect(
                    color = underglowColor.copy(alpha = 0.65f),
                    topLeft = Offset(-carLength * 0.55f, -carWidth * 0.7f),
                    size = Size(carLength * 1.1f, carWidth * 1.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
            }

            // Headlight beams
            val beamLength = 95f
            val beamColor = Color(0x33FFF59D)
            val beamPath = Path().apply {
                moveTo(carLength * 0.45f, -carWidth * 0.35f)
                lineTo(carLength * 0.45f + beamLength, -carWidth * 0.9f)
                lineTo(carLength * 0.45f + beamLength, carWidth * 0.9f)
                lineTo(carLength * 0.45f, carWidth * 0.35f)
                close()
            }
            drawPath(beamPath, beamColor)

            // Wheels / Tires (4 corners)
            val wheelW = 10f
            val wheelH = 4.5f
            val wheelColor = Color(0xFF1E1E1E)
            // Front left & right
            drawRoundRect(wheelColor, Offset(carLength * 0.22f, -carWidth * 0.58f), Size(wheelW, wheelH))
            drawRoundRect(wheelColor, Offset(carLength * 0.22f, carWidth * 0.58f - wheelH), Size(wheelW, wheelH))
            // Rear left & right
            drawRoundRect(wheelColor, Offset(-carLength * 0.38f, -carWidth * 0.58f), Size(wheelW, wheelH))
            drawRoundRect(wheelColor, Offset(-carLength * 0.38f, carWidth * 0.58f - wheelH), Size(wheelW, wheelH))

            // Main Car Body Shell
            val bodyPath = Path().apply {
                // Nose
                moveTo(carLength * 0.5f, 0f)
                lineTo(carLength * 0.45f, -carWidth * 0.42f)
                // Front wheel arch
                lineTo(carLength * 0.15f, -carWidth * 0.48f)
                // Door indent
                lineTo(-carLength * 0.15f, -carWidth * 0.44f)
                // Rear fender
                lineTo(-carLength * 0.45f, -carWidth * 0.48f)
                // Rear bumper
                lineTo(-carLength * 0.5f, -carWidth * 0.35f)
                lineTo(-carLength * 0.5f, carWidth * 0.35f)
                lineTo(-carLength * 0.45f, carWidth * 0.48f)
                lineTo(-carLength * 0.15f, carWidth * 0.44f)
                lineTo(carLength * 0.15f, carWidth * 0.48f)
                lineTo(carLength * 0.45f, carWidth * 0.42f)
                close()
            }
            drawPath(bodyPath, bodyColor)
            drawPath(bodyPath, Color.Black.copy(alpha = 0.5f), style = Stroke(width = 1.8f))

            // Windshield and roof cabin (dark glass)
            val cabinPath = Path().apply {
                moveTo(carLength * 0.22f, 0f)
                lineTo(carLength * 0.14f, -carWidth * 0.28f)
                lineTo(-carLength * 0.22f, -carWidth * 0.28f)
                lineTo(-carLength * 0.30f, 0f)
                lineTo(-carLength * 0.22f, carWidth * 0.28f)
                lineTo(carLength * 0.14f, carWidth * 0.28f)
                close()
            }
            drawPath(cabinPath, Color(0xFF151821))

            // Rear GT Wing / Spoiler
            drawRoundRect(
                color = Color(0xFF101216),
                topLeft = Offset(-carLength * 0.52f, -carWidth * 0.52f),
                size = Size(5f, carWidth * 1.04f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )

            // Headlights
            drawCircle(Color(0xFF80DEEA), radius = 2.5f, center = Offset(carLength * 0.48f, -carWidth * 0.32f))
            drawCircle(Color(0xFF80DEEA), radius = 2.5f, center = Offset(carLength * 0.48f, carWidth * 0.32f))

            // Tail lights / Brake lights
            val brakeLightColor = if (isBraking) ApexRed else Color(0xFFB71C1C)
            val brakeLightRadius = if (isBraking) 4.5f else 2.5f
            drawCircle(brakeLightColor, radius = brakeLightRadius, center = Offset(-carLength * 0.49f, -carWidth * 0.34f))
            drawCircle(brakeLightColor, radius = brakeLightRadius, center = Offset(-carLength * 0.49f, carWidth * 0.34f))

            // Player Marker Chevron
            if (isPlayer) {
                val playerIndicatorPath = Path().apply {
                    moveTo(-carLength * 0.05f, -6f)
                    lineTo(carLength * 0.10f, 0f)
                    lineTo(-carLength * 0.05f, 6f)
                    close()
                }
                drawPath(playerIndicatorPath, Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

private fun DrawScope.drawGhostVehicle(ghost: GhostFrame) {
    translate(left = ghost.x, top = ghost.y) {
        rotate(degrees = ghost.heading) {
            val carLength = 46f
            val carWidth = 24f

            // Holographic translucent aura
            drawRoundRect(
                color = ApexCyan.copy(alpha = 0.25f),
                topLeft = Offset(-carLength * 0.55f, -carWidth * 0.6f),
                size = Size(carLength * 1.1f, carWidth * 1.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )

            // Ghost outline
            val bodyPath = Path().apply {
                moveTo(carLength * 0.5f, 0f)
                lineTo(carLength * 0.45f, -carWidth * 0.4f)
                lineTo(-carLength * 0.45f, -carWidth * 0.4f)
                lineTo(-carLength * 0.5f, 0f)
                lineTo(-carLength * 0.45f, carWidth * 0.4f)
                lineTo(carLength * 0.45f, carWidth * 0.4f)
                close()
            }
            drawPath(bodyPath, ApexCyan.copy(alpha = 0.45f))
            drawPath(bodyPath, Color.White.copy(alpha = 0.75f), style = Stroke(width = 2f))
        }
    }
}

private fun DrawScope.drawParticles(particles: List<Particle>) {
    for (p in particles) {
        drawCircle(
            color = Color(p.color).copy(alpha = p.alpha),
            radius = p.size,
            center = Offset(p.x, p.y)
        )
    }
}

private fun DrawScope.drawWeatherOverlay(
    weather: WeatherType,
    wetness: Float,
    lightningAlpha: Float,
    w: Float,
    h: Float
) {
    // Lightning flash overlay
    if (lightningAlpha > 0.01f) {
        drawRect(
            color = Color.White.copy(alpha = lightningAlpha.coerceIn(0f, 0.7f)),
            topLeft = Offset.Zero,
            size = Size(w, h)
        )
    }

    // Rain streaks simulation
    if (weather == WeatherType.LIGHT_RAIN || weather == WeatherType.THUNDERSTORM) {
        val count = if (weather == WeatherType.THUNDERSTORM) 70 else 35
        val streakColor = Color(0x66B0BEC5)
        for (i in 0 until count) {
            val rx = (i * 37f + (System.currentTimeMillis() % 1000) * 1.8f) % w
            val ry = (i * 61f + (System.currentTimeMillis() % 800) * 2.2f) % h
            drawLine(
                color = streakColor,
                start = Offset(rx, ry),
                end = Offset(rx - 8f, ry + 22f),
                strokeWidth = 1.8f
            )
        }
    }

    // Mountain mist / fog layer
    if (weather == WeatherType.FOG_MIST) {
        drawRect(
            color = Color(0x55ECEFF1),
            topLeft = Offset.Zero,
            size = Size(w, h)
        )
    }
}

private fun DrawScope.drawMiniMap(
    track: Track,
    playerState: CarPhysicsState,
    aiRacers: List<AiRacer>,
    ghostFrame: GhostFrame?,
    mapWidth: Float,
    mapHeight: Float,
    padding: Float
) {
    val wp = track.waypoints
    if (wp.isEmpty()) return

    val minX = track.minX - 100f
    val maxX = track.maxX + 100f
    val minY = track.minY - 100f
    val maxY = track.maxY + 100f

    val spanX = (maxX - minX).coerceAtLeast(1f)
    val spanY = (maxY - minY).coerceAtLeast(1f)

    val scale = minOf(mapWidth / spanX, mapHeight / spanY)

    val mapLeft = size.width - mapWidth - padding
    val mapTop = padding

    // Background HUD card
    drawRoundRect(
        color = CarbonBlack.copy(alpha = 0.75f),
        topLeft = Offset(mapLeft - 6f, mapTop - 6f),
        size = Size(mapWidth + 12f, mapHeight + 12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color(0xFF333A4A),
        topLeft = Offset(mapLeft - 6f, mapTop - 6f),
        size = Size(mapWidth + 12f, mapHeight + 12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
        style = Stroke(width = 1.5f)
    )

    // Track path on map
    val path = Path()
    for (i in wp.indices) {
        val mx = mapLeft + (wp[i].x - minX) * scale
        val my = mapTop + (wp[i].y - minY) * scale
        if (i == 0) path.moveTo(mx, my) else path.lineTo(mx, my)
    }
    path.close()

    drawPath(path, Color(0xFF546E7A), style = Stroke(width = 4f))

    // Start / finish marker
    val sf = track.startWaypoint
    val sfx = mapLeft + (sf.x - minX) * scale
    val sfy = mapTop + (sf.y - minY) * scale
    drawCircle(Color.White, radius = 3.5f, center = Offset(sfx, sfy))

    // Ghost dot
    if (ghostFrame != null) {
        val gx = mapLeft + (ghostFrame.x - minX) * scale
        val gy = mapTop + (ghostFrame.y - minY) * scale
        drawCircle(ApexCyan.copy(alpha = 0.8f), radius = 3.5f, center = Offset(gx, gy))
    }

    // AI dots
    for (ai in aiRacers) {
        val ax = mapLeft + (ai.state.x - minX) * scale
        val ay = mapTop + (ai.state.y - minY) * scale
        drawCircle(ApexOrange, radius = 3.5f, center = Offset(ax, ay))
    }

    // Player dot (bright glowing cyan)
    val px = mapLeft + (playerState.x - minX) * scale
    val py = mapTop + (playerState.y - minY) * scale
    drawCircle(ApexCyan, radius = 5.5f, center = Offset(px, py))
    drawCircle(Color.White, radius = 2.5f, center = Offset(px, py))
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        ApexCyan
    }
}
