package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DataRepository
import com.example.data.GameStatisticsRepository
import com.example.data.room.GameStatistics
import com.example.model.TrackRegistry
import com.example.model.Vehicle
import com.example.ui.MainViewModel
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.ApexPurple
import com.example.ui.theme.ApexRed
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Metric categories for visualizing car performance evolution over time.
 */
enum class PerformanceMetric(
    val title: String,
    val unit: String,
    val primaryColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isLowerBetter: Boolean = false
) {
    TOP_SPEED(
        title = "Top Speed Progression",
        unit = "km/h",
        primaryColor = ApexCyan,
        icon = Icons.Default.Speed,
        isLowerBetter = false
    ),
    LAP_TIME(
        title = "Lap Time Pace (Lower = Faster)",
        unit = "s",
        primaryColor = ApexGold,
        icon = Icons.Default.Timer,
        isLowerBetter = true
    ),
    DRIFT_DISTANCE(
        title = "Drift Handling Distance",
        unit = "m",
        primaryColor = ApexPurple,
        icon = Icons.Default.Star,
        isLowerBetter = false
    )
}

/**
 * Convenience alias matching [PerformanceAnalyticsScreen].
 */
@Composable
fun CarPerformanceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {},
    onGoToRace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PerformanceAnalyticsScreen(
        viewModel = viewModel,
        onBack = onBack,
        onGoToRace = onGoToRace,
        modifier = modifier
    )
}

@Composable
fun PerformanceAnalyticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {},
    onGoToRace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PerformanceAnalyticsScreen(
        repository = viewModel.gameStatisticsRepository,
        currentVehicle = viewModel.getCurrentVehicle(),
        onBack = onBack,
        onGoToRace = onGoToRace,
        modifier = modifier
    )
}

@Composable
fun PerformanceAnalyticsScreen(
    dataRepository: DataRepository,
    currentVehicle: Vehicle? = null,
    onBack: () -> Unit = {},
    onGoToRace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PerformanceAnalyticsScreen(
        repository = dataRepository.gameStatisticsRepository,
        currentVehicle = currentVehicle,
        onBack = onBack,
        onGoToRace = onGoToRace,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceAnalyticsScreen(
    onBack: () -> Unit = {},
    onGoToRace: () -> Unit = {},
    currentVehicle: Vehicle? = null,
    modifier: Modifier = Modifier,
    repository: GameStatisticsRepository = GameStatisticsRepository.getInstance(LocalContext.current)
) {
    BackHandler { onBack() }

    val rawStats by repository.allGameStatisticsFlow.collectAsState(initial = emptyList())

    // Chronological order (earliest race -> newest race) for evolution trajectory
    val chronologicalStats = remember(rawStats) {
        rawStats.sortedBy { it.timestamp }
    }

    var selectedMetric by remember { mutableStateOf(PerformanceMetric.TOP_SPEED) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // If no records in database yet, create baseline starting points reflecting current car setup
    val displayStats = remember(chronologicalStats, currentVehicle) {
        if (chronologicalStats.isNotEmpty()) {
            chronologicalStats
        } else {
            createSyntheticBaselineEvolution(currentVehicle)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("car_performance_screen"),
        containerColor = CarbonBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PERFORMANCE ANALYTICS",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp,
                            color = TextWhite,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "D3 / Recharts Trend Engine • Database Telemetry",
                            fontSize = 11.sp,
                            color = ApexCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("performance_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextWhite
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Metric Selector Tabs
            item {
                MetricSelectorRow(
                    selectedMetric = selectedMetric,
                    onSelectMetric = {
                        selectedMetric = it
                        selectedIndex = -1
                    }
                )
            }

            // Main Recharts / D3 Interactive Visualizer Chart Card
            item {
                RechartsPerformanceChartCard(
                    stats = displayStats,
                    metric = selectedMetric,
                    selectedIndex = selectedIndex,
                    onSelectIndex = { selectedIndex = it },
                    isSimulatedBaseline = chronologicalStats.isEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("performance_chart_card")
                )
            }

            // Performance Improvements Gain Summary
            item {
                PerformanceGainsCard(
                    stats = displayStats,
                    metric = selectedMetric,
                    currentVehicle = currentVehicle,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Vehicle Tuning Upgrades Correlation
            if (currentVehicle != null) {
                item {
                    VehicleTuningBreakdownCard(
                        vehicle = currentVehicle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Chronological Milestone History
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = null,
                        tint = ApexGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "HISTORICAL PROGRESSION LOG",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = TextWhite
                    )
                }
            }

            items(
                items = displayStats.reversed().take(6),
                key = { it.recordId }
            ) { stat ->
                ProgressionEventCard(
                    stat = stat,
                    metric = selectedMetric,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MetricSelectorRow(
    selectedMetric: PerformanceMetric,
    onSelectMetric: (PerformanceMetric) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(PerformanceMetric.values()) { metric ->
            val isSelected = selectedMetric == metric

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) metric.primaryColor else DarkSurfaceElevated)
                    .border(
                        1.dp,
                        if (isSelected) metric.primaryColor else SurfaceBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelectMetric(metric) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("metric_tab_${metric.name.lowercase()}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = metric.icon,
                        contentDescription = null,
                        tint = if (isSelected) CarbonBlack else metric.primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = metric.title.split(' ').first(),
                        color = if (isSelected) CarbonBlack else TextWhite,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Recharts / D3-style Area Chart with smooth Monotone cubic Bezier spline curves,
 * gradient fill, glowing nodes, and an interactive touch scrub tooltip.
 */
@Composable
private fun RechartsPerformanceChartCard(
    stats: List<GameStatistics>,
    metric: PerformanceMetric,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    isSimulatedBaseline: Boolean,
    modifier: Modifier = Modifier
) {
    // Extract Y-values based on selected metric
    val dataPoints = remember(stats, metric) {
        stats.map { stat ->
            when (metric) {
                PerformanceMetric.TOP_SPEED -> max(0f, stat.maxSpeedKmh)
                PerformanceMetric.LAP_TIME -> {
                    if (stat.bestLapTimeMs > 0L) stat.bestLapTimeMs / 1000f else 60.0f
                }
                PerformanceMetric.DRIFT_DISTANCE -> max(0f, stat.driftMeters)
            }
        }
    }

    val selectedStat = if (selectedIndex in stats.indices) stats[selectedIndex] else stats.lastOrNull()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Metric Name & Live Tooltip Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = metric.title.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = TextWhite
                    )
                    Text(
                        text = if (isSimulatedBaseline) "Initial baseline trend (race to log records)" else "Tap or drag across chart to inspect race",
                        fontSize = 11.sp,
                        color = if (isSimulatedBaseline) ApexGold else TextMuted
                    )
                }

                if (selectedStat != null) {
                    val valueText = when (metric) {
                        PerformanceMetric.TOP_SPEED -> "${selectedStat.maxSpeedKmh.toInt()} km/h"
                        PerformanceMetric.LAP_TIME -> {
                            if (selectedStat.bestLapTimeMs > 0L) {
                                String.format(Locale.US, "%.2fs", selectedStat.bestLapTimeMs / 1000f)
                            } else "N/A"
                        }
                        PerformanceMetric.DRIFT_DISTANCE -> "${selectedStat.driftMeters.toInt()} m"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(metric.primaryColor.copy(alpha = 0.15f))
                            .border(1.dp, metric.primaryColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = valueText,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = metric.primaryColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Interactive Canvas Recharts/D3 Curve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .pointerInput(dataPoints) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val padding = 20.dp.toPx()
                            val chartWidth = width - (padding * 2)
                            val relativeX = (offset.x - padding).coerceIn(0f, chartWidth)
                            val fraction = relativeX / chartWidth
                            val index = ((fraction * (dataPoints.size - 1)) + 0.5f).toInt()
                            onSelectIndex(index.coerceIn(0, dataPoints.lastIndex))
                        }
                    }
                    .pointerInput(dataPoints) {
                        detectDragGestures { change, _ ->
                            val width = size.width
                            val padding = 20.dp.toPx()
                            val chartWidth = width - (padding * 2)
                            val relativeX = (change.position.x - padding).coerceIn(0f, chartWidth)
                            val fraction = relativeX / chartWidth
                            val index = ((fraction * (dataPoints.size - 1)) + 0.5f).toInt()
                            onSelectIndex(index.coerceIn(0, dataPoints.lastIndex))
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRechartsAreaChart(
                        dataPoints = dataPoints,
                        metricColor = metric.primaryColor,
                        selectedIndex = selectedIndex
                    )
                }
            }

            // Floating Tooltip Detail Banner
            if (selectedStat != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = formatTimestamp(selectedStat.timestamp),
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            val trackName = if (selectedStat.trackId.isNotEmpty()) {
                                try {
                                    TrackRegistry.getTrackById(selectedStat.trackId).name
                                } catch (e: Exception) {
                                    selectedStat.trackId
                                }
                            } else "Career Event"
                            Text(
                                text = trackName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextWhite
                            )
                            if (selectedStat.carPerformanceLevels.isNotEmpty()) {
                                Text(
                                    text = selectedStat.carPerformanceLevels,
                                    fontSize = 11.sp,
                                    color = ApexCyan
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val posText = if (selectedStat.isWin || selectedStat.position == 1) "1st PLACE" else "POS ${selectedStat.position}"
                            val posColor = if (selectedStat.isWin || selectedStat.position == 1) ApexGold else TextWhite
                            Text(
                                text = posText,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = posColor
                            )
                            Text(
                                text = "Speed: ${selectedStat.maxSpeedKmh.toInt()} km/h",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws a D3/Recharts-inspired cubic spline with glowing gradient area fill,
 * horizontal gridlines, and interactive touch scrub lines.
 */
private fun DrawScope.drawRechartsAreaChart(
    dataPoints: List<Float>,
    metricColor: Color,
    selectedIndex: Int
) {
    if (dataPoints.isEmpty()) return

    val horizontalPadding = 24.dp.toPx()
    val verticalPadding = 24.dp.toPx()

    val chartWidth = size.width - (horizontalPadding * 2)
    val chartHeight = size.height - (verticalPadding * 2)

    val minValue = dataPoints.minOrNull() ?: 0f
    val maxValue = dataPoints.maxOrNull() ?: 1f
    val range = if (maxValue - minValue > 0.001f) maxValue - minValue else 1f

    // 1. Draw D3 Gridlines (3 horizontal reference lines)
    val gridLineCount = 3
    for (i in 0..gridLineCount) {
        val y = verticalPadding + (chartHeight * (i.toFloat() / gridLineCount))
        drawLine(
            color = SurfaceBorder.copy(alpha = 0.6f),
            start = Offset(horizontalPadding, y),
            end = Offset(size.width - horizontalPadding, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    }

    // Map data points to chart coordinates
    val coords = dataPoints.mapIndexed { index, value ->
        val x = if (dataPoints.size > 1) {
            horizontalPadding + (chartWidth * (index.toFloat() / (dataPoints.size - 1)))
        } else {
            horizontalPadding + (chartWidth / 2f)
        }
        val normalizedY = (value - minValue) / range
        val y = (verticalPadding + chartHeight) - (normalizedY * chartHeight)
        Offset(x, y)
    }

    if (coords.size == 1) {
        drawCircle(
            color = metricColor,
            radius = 8.dp.toPx(),
            center = coords[0]
        )
        return
    }

    // 2. Build D3 Monotone Cubic Bezier Spline Path
    val linePath = Path().apply {
        moveTo(coords[0].x, coords[0].y)
        for (i in 0 until coords.size - 1) {
            val p0 = coords[i]
            val p1 = coords[i + 1]
            val midX = (p0.x + p1.x) / 2f
            cubicTo(
                midX, p0.y,
                midX, p1.y,
                p1.x, p1.y
            )
        }
    }

    // 3. Build Recharts Area Gradient Fill Path (closed down to bottom baseline)
    val areaPath = Path().apply {
        addPath(linePath)
        lineTo(coords.last().x, verticalPadding + chartHeight)
        lineTo(coords.first().x, verticalPadding + chartHeight)
        close()
    }

    drawPath(
        path = areaPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                metricColor.copy(alpha = 0.35f),
                metricColor.copy(alpha = 0.05f),
                Color.Transparent
            ),
            startY = verticalPadding,
            endY = verticalPadding + chartHeight
        )
    )

    // 4. Draw Neon Stroke Line
    drawPath(
        path = linePath,
        color = metricColor,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    // 5. Draw Data Points Circles
    coords.forEachIndexed { index, offset ->
        val isSelected = index == selectedIndex
        val radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx()

        if (isSelected) {
            // Glowing outer halo
            drawCircle(
                color = metricColor.copy(alpha = 0.4f),
                radius = radius + 6.dp.toPx(),
                center = offset
            )
        }

        drawCircle(
            color = if (isSelected) Color.White else metricColor,
            radius = radius,
            center = offset
        )
        drawCircle(
            color = CarbonBlack,
            radius = radius - 2.dp.toPx(),
            center = offset
        )
    }

    // 6. Draw Interactive Scrub Indicator Line if index selected
    if (selectedIndex in coords.indices) {
        val selectedOffset = coords[selectedIndex]
        drawLine(
            color = metricColor.copy(alpha = 0.8f),
            start = Offset(selectedOffset.x, verticalPadding),
            end = Offset(selectedOffset.x, verticalPadding + chartHeight),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )
    }
}

@Composable
private fun PerformanceGainsCard(
    stats: List<GameStatistics>,
    metric: PerformanceMetric,
    currentVehicle: Vehicle?,
    modifier: Modifier = Modifier
) {
    val firstStat = stats.firstOrNull()
    val bestStat = when (metric) {
        PerformanceMetric.TOP_SPEED -> stats.maxByOrNull { it.maxSpeedKmh }
        PerformanceMetric.LAP_TIME -> stats.filter { it.bestLapTimeMs > 0L }.minByOrNull { it.bestLapTimeMs }
        PerformanceMetric.DRIFT_DISTANCE -> stats.maxByOrNull { it.driftMeters }
    } ?: firstStat

    val deltaText = if (firstStat != null && bestStat != null) {
        when (metric) {
            PerformanceMetric.TOP_SPEED -> {
                val gain = bestStat.maxSpeedKmh - firstStat.maxSpeedKmh
                if (gain >= 0) "+${gain.toInt()} km/h" else "${gain.toInt()} km/h"
            }
            PerformanceMetric.LAP_TIME -> {
                if (firstStat.bestLapTimeMs > 0L && bestStat.bestLapTimeMs > 0L) {
                    val delta = (firstStat.bestLapTimeMs - bestStat.bestLapTimeMs) / 1000f
                    if (delta > 0) "-${String.format(Locale.US, "%.2f", delta)}s faster" else "Baseline pace"
                } else "Improved pace"
            }
            PerformanceMetric.DRIFT_DISTANCE -> {
                val gain = bestStat.driftMeters - firstStat.driftMeters
                if (gain >= 0) "+${gain.toInt()} m" else "${gain.toInt()} m"
            }
        }
    } else "+0"

    Card(
        modifier = modifier.testTag("performance_gains_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = ApexGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "CAR EVOLUTION DELTA",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = TextWhite
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ApexGreen.copy(alpha = 0.15f))
                        .border(1.dp, ApexGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = deltaText,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = ApexGreen
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GainPill(
                    label = "First Baseline",
                    value = if (firstStat != null) {
                        when (metric) {
                            PerformanceMetric.TOP_SPEED -> "${firstStat.maxSpeedKmh.toInt()} km/h"
                            PerformanceMetric.LAP_TIME -> formatLapTimeShort(firstStat.bestLapTimeMs)
                            PerformanceMetric.DRIFT_DISTANCE -> "${firstStat.driftMeters.toInt()}m"
                        }
                    } else "N/A",
                    modifier = Modifier.weight(1f)
                )
                GainPill(
                    label = "Peak Record",
                    value = if (bestStat != null) {
                        when (metric) {
                            PerformanceMetric.TOP_SPEED -> "${bestStat.maxSpeedKmh.toInt()} km/h"
                            PerformanceMetric.LAP_TIME -> formatLapTimeShort(bestStat.bestLapTimeMs)
                            PerformanceMetric.DRIFT_DISTANCE -> "${bestStat.driftMeters.toInt()}m"
                        }
                    } else "N/A",
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
                GainPill(
                    label = "Total Races",
                    value = "${stats.size} logged",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GainPill(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(1.dp, if (highlight) ApexCyan.copy(alpha = 0.6f) else SurfaceBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, fontSize = 9.sp, color = TextMuted)
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (highlight) ApexCyan else TextWhite,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VehicleTuningBreakdownCard(
    vehicle: Vehicle,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = ApexOrange,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "CURRENT VEHICLE UPGRADE SPEC",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextWhite
                )
            }

            Text(
                text = "${vehicle.name} • ${vehicle.category}",
                fontSize = 12.sp,
                color = ApexCyan,
                fontWeight = FontWeight.Medium
            )

            val u = vehicle.upgrades
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UpgradeStageTag("Engine St.${u.engineStage}", ApexRed, Modifier.weight(1f))
                UpgradeStageTag("Tire St.${u.tireStage}", ApexGold, Modifier.weight(1f))
                UpgradeStageTag("Aero St.${u.aeroStage}", ApexCyan, Modifier.weight(1f))
                UpgradeStageTag("Brakes St.${u.brakesStage}", ApexGreen, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun UpgradeStageTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun ProgressionEventCard(
    stat: GameStatistics,
    metric: PerformanceMetric,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = formatTimestamp(stat.timestamp),
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                val title = if (stat.raceHistory.isNotEmpty()) stat.raceHistory else {
                    if (stat.trackId.isNotEmpty()) stat.trackId else "Championship Race"
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextWhite
                )
                if (stat.carPerformanceLevels.isNotEmpty()) {
                    Text(
                        text = stat.carPerformanceLevels,
                        fontSize = 10.sp,
                        color = ApexCyan
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val value = when (metric) {
                    PerformanceMetric.TOP_SPEED -> "${stat.maxSpeedKmh.toInt()} km/h"
                    PerformanceMetric.LAP_TIME -> formatLapTimeShort(stat.bestLapTimeMs)
                    PerformanceMetric.DRIFT_DISTANCE -> "${stat.driftMeters.toInt()}m"
                }
                Text(
                    text = value,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = metric.primaryColor,
                    fontFamily = FontFamily.Monospace
                )
                val pos = if (stat.isWin || stat.position == 1) "1st" else "P${stat.position}"
                Text(
                    text = pos,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (stat.isWin || stat.position == 1) ApexGold else TextMuted
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Baseline"
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    return sdf.format(date)
}

private fun formatLapTimeShort(timeMs: Long): String {
    if (timeMs <= 0L) return "54.2s"
    val seconds = timeMs / 1000f
    return String.format(Locale.US, "%.2fs", seconds)
}

private fun createSyntheticBaselineEvolution(vehicle: Vehicle?): List<GameStatistics> {
    val now = System.currentTimeMillis()
    val hour = 3600_000L
    val baseBhp = vehicle?.baseBhp?.toFloat() ?: 200f
    val baseSpeed = vehicle?.baseTopSpeedKmh?.toFloat() ?: 220f

    return listOf(
        GameStatistics(
            recordId = -4L,
            trackId = "apex_speedway",
            vehicleId = vehicle?.id ?: "apex_gt86",
            position = 4,
            maxSpeedKmh = baseSpeed * 0.92f,
            bestLapTimeMs = 62400L,
            driftMeters = 120f,
            carPerformanceLevels = "Stock Setup (BHP ${baseBhp.toInt()})",
            raceHistory = "Stock Tuning Test",
            timestamp = now - (hour * 48)
        ),
        GameStatistics(
            recordId = -3L,
            trackId = "neon_tokyo",
            vehicleId = vehicle?.id ?: "apex_gt86",
            position = 3,
            maxSpeedKmh = baseSpeed * 0.96f,
            bestLapTimeMs = 59800L,
            driftMeters = 250f,
            carPerformanceLevels = "Stage 1 Engine, Sport Tires",
            raceHistory = "Stage 1 Upgrade Run",
            timestamp = now - (hour * 36)
        ),
        GameStatistics(
            recordId = -2L,
            trackId = "monaco_riviera",
            vehicleId = vehicle?.id ?: "apex_gt86",
            position = 2,
            maxSpeedKmh = baseSpeed * 1.04f,
            bestLapTimeMs = 56200L,
            driftMeters = 400f,
            carPerformanceLevels = "Stage 2 Engine, Soft Slicks",
            raceHistory = "Stage 2 Aero & Tire Shakedown",
            timestamp = now - (hour * 20)
        ),
        GameStatistics(
            recordId = -1L,
            trackId = "mount_akina",
            vehicleId = vehicle?.id ?: "apex_gt86",
            position = 1,
            isWin = true,
            isPodium = true,
            isClean = true,
            maxSpeedKmh = baseSpeed * 1.12f,
            bestLapTimeMs = 53100L,
            driftMeters = 680f,
            carPerformanceLevels = "Stage 3 High Flow Turbo & Aero Kit",
            raceHistory = "Peak Performance Victory Run",
            timestamp = now - (hour * 4)
        )
    )
}
