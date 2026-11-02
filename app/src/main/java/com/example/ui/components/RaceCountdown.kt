package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay

/**
 * Stages of the visual countdown sequence.
 */
enum class CountdownStage {
    READY,
    THREE,
    TWO,
    ONE,
    GO,
    FINISHED
}

/**
 * State holder for [RaceCountdown] sequencing.
 */
@Stable
class RaceCountdownState(
    val initialSeconds: Int = 3,
    autoStart: Boolean = true,
    var onFinishedCallback: () -> Unit = {}
) {
    var count by mutableIntStateOf(initialSeconds)
        private set

    var stage by mutableStateOf(if (autoStart) CountdownStage.THREE else CountdownStage.READY)
        private set

    var isRunning by mutableStateOf(autoStart)
        private set

    var isFinished by mutableStateOf(false)
        private set

    fun start() {
        if (!isRunning && !isFinished) {
            count = initialSeconds
            stage = CountdownStage.THREE
            isRunning = true
            isFinished = false
        }
    }

    fun restart() {
        count = initialSeconds
        stage = CountdownStage.THREE
        isRunning = true
        isFinished = false
    }

    fun skip() {
        count = 0
        stage = CountdownStage.GO
        isRunning = false
        isFinished = true
        onFinishedCallback()
    }

    internal fun advance() {
        when (count) {
            3 -> {
                count = 2
                stage = CountdownStage.TWO
            }
            2 -> {
                count = 1
                stage = CountdownStage.ONE
            }
            1 -> {
                count = 0
                stage = CountdownStage.GO
            }
            0 -> {
                stage = CountdownStage.FINISHED
                isRunning = false
                isFinished = true
                onFinishedCallback()
            }
        }
    }
}

/**
 * Creates and remembers a [RaceCountdownState].
 */
@Composable
fun rememberRaceCountdownState(
    initialSeconds: Int = 3,
    autoStart: Boolean = true,
    onFinished: () -> Unit = {}
): RaceCountdownState {
    val state = remember {
        RaceCountdownState(
            initialSeconds = initialSeconds,
            autoStart = autoStart,
            onFinishedCallback = onFinished
        )
    }

    state.onFinishedCallback = onFinished

    LaunchedEffect(state.isRunning, state.stage) {
        if (state.isRunning && !state.isFinished) {
            // Stage "GO" is held for 650ms before disappearing and unlocking
            val holdTime = if (state.stage == CountdownStage.GO) 650L else 1000L
            delay(holdTime)
            state.advance()
        }
    }

    return state
}

/**
 * A reusable visual Countdown Composable that presents a 3-2-1 sequence with a
 * Formula 1 starting lights gantry and animated typography before triggering timer start
 * and unlocking vehicle controls.
 */
@Composable
fun RaceCountdown(
    modifier: Modifier = Modifier,
    countdownState: RaceCountdownState = rememberRaceCountdownState(),
    onCountdownFinished: () -> Unit = {},
    showSkipButton: Boolean = true
) {
    if (countdownState.isFinished && countdownState.stage == CountdownStage.FINISHED) {
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CarbonBlack.copy(alpha = 0.55f),
                        CarbonBlack.copy(alpha = 0.85f)
                    )
                )
            )
            .testTag("race_countdown_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // 1. Motorsport Starting Lights Gantry
            StartingLightsGantry(
                stage = countdownState.stage,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .testTag("countdown_lights_gantry")
            )

            // 2. Central Animated 3-2-1 / GO! Display
            AnimatedCountdownDisplay(
                stage = countdownState.stage,
                modifier = Modifier.testTag("countdown_display")
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 3. Skip Button (Touch target >= 48dp)
            if (showSkipButton && !countdownState.isFinished) {
                OutlinedButton(
                    onClick = {
                        countdownState.skip()
                        onCountdownFinished()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextWhite
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(ApexCyan, ApexGold))
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("countdown_skip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Skip Countdown",
                        tint = ApexCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAUNCH NOW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * 5-Pod Motorsport Starting Lights Gantry.
 * Lights illuminate progressively:
 * - Stage 3: 2 Red lights
 * - Stage 2: 4 Red lights
 * - Stage 1: All 5 Red lights
 * - Stage GO: All 5 lights switch to luminous green!
 */
@Composable
fun StartingLightsGantry(
    stage: CountdownStage,
    modifier: Modifier = Modifier
) {
    val activeRedCount = when (stage) {
        CountdownStage.THREE -> 2
        CountdownStage.TWO -> 4
        CountdownStage.ONE -> 5
        CountdownStage.GO -> 0
        else -> 0
    }
    val isGo = stage == CountdownStage.GO

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CarbonBlack)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Carbon top bar label
            Text(
                text = "STARTING GRID LIGHTS",
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // 5 Light Pods
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 5) {
                    val isLit = (i < activeRedCount) || isGo
                    val targetColor = if (isGo) {
                        ApexGreen
                    } else if (i < activeRedCount) {
                        ApexRed
                    } else {
                        Color(0xFF202020)
                    }

                    val color by animateColorAsState(
                        targetValue = targetColor,
                        animationSpec = tween(150),
                        label = "LightPodColor_$i"
                    )

                    LightPod(
                        isLit = isLit,
                        lightColor = color,
                        isGo = isGo,
                        index = i
                    )
                }
            }
        }
    }
}

@Composable
private fun LightPod(
    isLit: Boolean,
    lightColor: Color,
    isGo: Boolean,
    index: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LightGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isLit) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LightPodPulse"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF151515))
            .border(2.dp, if (isLit) lightColor.copy(alpha = 0.8f) else Color(0xFF333333), CircleShape)
            .testTag("light_pod_$index"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(if (isLit) pulseScale else 1.0f)
                .clip(CircleShape)
                .background(
                    if (isLit) {
                        Brush.radialGradient(
                            colors = listOf(
                                lightColor,
                                lightColor.copy(alpha = 0.7f),
                                lightColor.copy(alpha = 0.2f)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2A2A2A),
                                Color(0xFF141414)
                            )
                        )
                    }
                )
        )
    }
}

/**
 * Animated High-Impact Countdown Typography.
 */
@Composable
fun AnimatedCountdownDisplay(
    stage: CountdownStage,
    modifier: Modifier = Modifier
) {
    val displayData = when (stage) {
        CountdownStage.THREE -> CountdownDisplayInfo(
            text = "3",
            subText = "GET READY",
            color = ApexRed,
            testTag = "countdown_digit_3"
        )
        CountdownStage.TWO -> CountdownDisplayInfo(
            text = "2",
            subText = "SET REVS",
            color = ApexOrange,
            testTag = "countdown_digit_2"
        )
        CountdownStage.ONE -> CountdownDisplayInfo(
            text = "1",
            subText = "CLUTCH READY",
            color = ApexGold,
            testTag = "countdown_digit_1"
        )
        CountdownStage.GO -> CountdownDisplayInfo(
            text = "GO!",
            subText = "ACCELERATE",
            color = ApexGreen,
            testTag = "countdown_digit_go"
        )
        else -> CountdownDisplayInfo(
            text = "",
            subText = "",
            color = TextWhite,
            testTag = "countdown_digit_empty"
        )
    }

    AnimatedContent(
        targetState = displayData,
        transitionSpec = {
            (scaleIn(
                initialScale = 0.4f,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(150)))
                .togetherWith(
                    scaleOut(
                        targetScale = 1.4f,
                        animationSpec = tween(200)
                    ) + fadeOut(animationSpec = tween(150))
                )
        },
        label = "CountdownAnimation",
        modifier = modifier
    ) { target ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.testTag(target.testTag)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                target.color.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Text(
                    text = target.text,
                    color = target.color,
                    fontSize = if (target.text == "GO!") 84.sp else 96.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-2).sp
                )
            }

            Text(
                text = target.subText,
                color = target.color,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
        }
    }
}

private data class CountdownDisplayInfo(
    val text: String,
    val subText: String,
    val color: Color,
    val testTag: String
)

/**
 * Interactive demo composable combining [RaceCountdown] with [RaceTimer],
 * showcasing the countdown sequence driving the start of the race timer and unlocking controls.
 */
@Composable
fun RaceCountdownWithTimerDemo(
    modifier: Modifier = Modifier
) {
    val countdownState = rememberRaceCountdownState(
        initialSeconds = 3,
        autoStart = false
    )
    val timerState = rememberRaceTimerState(autoStart = false)
    var controlsUnlocked by remember { mutableStateOf(false) }

    countdownState.onFinishedCallback = {
        timerState.start()
        controlsUnlocked = true
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .testTag("countdown_timer_demo_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RACE LAUNCH CONTROLLER",
                    color = ApexCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                // Controls Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (controlsUnlocked) ApexGreen.copy(alpha = 0.15f) else ApexRed.copy(alpha = 0.15f))
                        .border(1.dp, if (controlsUnlocked) ApexGreen else ApexRed, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("demo_controls_status")
                ) {
                    Text(
                        text = if (controlsUnlocked) "CONTROLS: ENABLED" else "CONTROLS: LOCKED",
                        color = if (controlsUnlocked) ApexGreen else ApexRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Embedded Countdown or Status
            if (countdownState.isRunning && !countdownState.isFinished) {
                StartingLightsGantry(
                    stage = countdownState.stage,
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedCountdownDisplay(
                    stage = countdownState.stage,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Embedded Reusable Timer
            RaceTimer(
                timerState = timerState,
                showLapControls = controlsUnlocked,
                title = "ACTIVE RACE CHRONO"
            )

            // Trigger Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        controlsUnlocked = false
                        timerState.reset()
                        countdownState.restart()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApexCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("demo_start_sequence_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CarbonBlack)
                    Spacer(Modifier.width(6.dp))
                    Text("START SEQUENCE", color = CarbonBlack, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        countdownState.skip()
                        timerState.start()
                        controlsUnlocked = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("demo_skip_countdown_button")
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = ApexGold)
                    Spacer(Modifier.width(6.dp))
                    Text("FAST LAUNCH", color = ApexGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
