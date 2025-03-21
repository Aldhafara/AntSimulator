package com.aldhafara.ant_simulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import kotlin.math.max
import kotlin.random.Random

@Composable
fun AntSimulator(sizeDp: Dp) {

    val obstacles = remember { mutableStateOf<Set<Offset>>(emptySet()) }

    val cellSize = 5.dp
    val gridSize = (sizeDp / cellSize).toInt()

    val gridWidth = gridSize * cellSize.value
    val binSize = 1000
    val clock: Clock = Clock.systemDefaultZone()

    val foodSource = Target(Offset((gridSize - 5) * cellSize.value, (gridSize - 5) * cellSize.value), TargetType.FOOD)
    val nest = Target(Offset(5 * cellSize.value, 5 * cellSize.value), TargetType.NEST)

    val pheromoneTrail = remember { PheromoneTrail() }

    val numberOfAnts = 25
    val ants = remember { mutableStateListOf<Ant>() }

    var isRunning by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val stats = remember { AntSimulationStats() }
    var histogramState by remember { mutableStateOf(stats.getHistogram(binSize)) }
    val cellSizePx = with(LocalDensity.current) { cellSize.toPx() }

    LaunchedEffect(Unit) {
        repeat(numberOfAnts) {
            ants.add(Ant(nest.position, getRandomDirection(), Angle(30f), foodSource))
        }
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            stats.onResume()
            pheromoneTrail.resume()
            coroutineScope.launch {
                while (isRunning) {
                    delay(10)
                    pheromoneTrail.decay()

                    ants.forEachIndexed { index, ant ->
                        val previousTarget = ants[index].currentTarget.type
                        ants[index] = updateAntPosition(
                            ant,
                            cellSize.value,
                            gridSize,
                            nest,
                            foodSource,
                            pheromoneTrail.getPheromones(),
                            obstacles.value
                        )
                        val currentTime = Instant.now(clock).toEpochMilli()
                        if (currentTime - ant.lastPheromoneTime >= ant.pheromoneInterval) {
                            pheromoneTrail.addPheromone(
                                snapToGrid(ant.position, cellSizePx, previousTarget),
                                currentTime,
                                previousTarget
                            )
                            ants[index] = ant.copy(lastPheromoneTime = currentTime)
                        }
                        val updated = stats.updateStatistics(
                            ants[index].currentTarget.type,
                            previousTarget,
                            ants[index].tripStartTime
                        )
                        if (updated) {
                            ants[index].tripStartTime = Instant.now(clock)
                            histogramState = stats.getHistogram(binSize)
                        }
                    }
                }
            }
        } else {
            stats.onPause()
            pheromoneTrail.pause()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        antSimulationControlsAndStatistics(
            isRunning,
            stats.getFoodDelivered(),
            stats.getTripsCount(),
            stats.getAvgTravelTime()
        ) { isRunning = !isRunning }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(16.dp))
            antSimulationCanvas(
                gridSize,
                cellSize,
                gridWidth,
                ants,
                foodSource.position,
                nest.position,
                pheromoneTrail,
                obstacles
            )
            Spacer(modifier = Modifier.width(16.dp))


            travelTimeHistogram(histogramState, stats.getAvgTravelTime(), binSize, gridWidth)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

private fun getRandomDirection() = Offset(Random.nextFloat() * 2 - 1, Random.nextFloat() * 2 - 1)

@Composable
fun antSimulationControlsAndStatistics(
    isRunning: Boolean,
    foodDelivered: Int,
    tripsCount: Int,
    avgTravelTime: Long,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 16.dp)) {
            Text("Food Delivered: $foodDelivered")
            Text("Trips: $tripsCount")
        }

        Button(
            onClick = onToggle,
            modifier = Modifier.width(250.dp)
        ) {
            Text(if (isRunning) "Pause" else "Start Simulation")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 16.dp)) {
            Text("Avg. Time:")
            Text("${if (tripsCount > 0) avgTravelTime else 0} ms")
        }
    }
}

@Composable
fun antSimulationCanvas(
    gridSize: Int,
    cellSize: Dp,
    gridWidth: Float,
    ants: List<Ant>,
    initialTarget: Offset,
    nest: Offset,
    pheromoneTrail: PheromoneTrail,
    obstacles: MutableState<Set<Offset>>
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .size(with(density) { (gridWidth + 4).toDp() })
            .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
            .padding(2.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position
                        when {
                            event.type == PointerEventType.Press && event.buttons.isPrimaryPressed -> {
                                obstacles.value += snapToGrid(position, cellSize, density)
                            }

                            event.type == PointerEventType.Press && event.buttons.isSecondaryPressed -> {
                                obstacles.value -= snapToGrid(position, cellSize, density)
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var pressStartTime = 0L
                    var isLongPress = false
                    val timeThreshold = 50

                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position

                        when {
                            event.type == PointerEventType.Press -> {
                                pressStartTime = System.currentTimeMillis()
                                isLongPress = false
                            }

                            event.type == PointerEventType.Move && event.buttons.isPrimaryPressed -> {
                                if (!isLongPress && System.currentTimeMillis() - pressStartTime > timeThreshold) {
                                    isLongPress = true
                                }

                                if (isLongPress && event.buttons.isPrimaryPressed) {
                                    if (position.x in 0f..gridWidth && position.y in 0f..gridWidth) {
                                        obstacles.value += snapToGridWithNeighbors(position, cellSize, density)
                                    }
                                }
                            }

                            event.type == PointerEventType.Move && event.buttons.isSecondaryPressed -> {
                                if (!isLongPress && System.currentTimeMillis() - pressStartTime > timeThreshold) {
                                    isLongPress = true
                                }

                                if (isLongPress && event.buttons.isSecondaryPressed) {
                                    obstacles.value -= snapToGridWithNeighbors(position, cellSize, density)
                                }
                            }

                            event.type == PointerEventType.Release -> {
                                pressStartTime = 0L
                                isLongPress = false
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGrid(gridSize, cellSize)
            drawPheromones(pheromoneTrail.getPheromones())
            drawObstacles(obstacles, cellSize)

            ants.forEach { ant ->
                drawAnt(cellSize, ant.position, ant.direction, ant.sightDistance, ant.fieldViewAngleRange)
            }
            drawTarget(cellSize, initialTarget)
            drawNest(cellSize, nest)
        }
    }
}

fun snapToGrid(offset: Offset, cellSize: Dp, density: Density): Offset {
    val cellSizePx = with(density) { cellSize.toPx() }
    return Offset(
        (offset.x / cellSizePx).toInt() * cellSizePx,
        (offset.y / cellSizePx).toInt() * cellSizePx
    )
}

fun snapToGridWithNeighbors(offset: Offset, cellSize: Dp, density: Density): Set<Offset> {
    val cellSizePx = with(density) { cellSize.toPx() }

    val baseX = (offset.x / cellSizePx).toInt() * cellSizePx
    val baseY = (offset.y / cellSizePx).toInt() * cellSizePx

    val neighbors = setOf(
        Offset(baseX, baseY),
        Offset(baseX - cellSizePx, baseY),
        Offset(baseX + cellSizePx, baseY),
        Offset(baseX, baseY - cellSizePx),
        Offset(baseX, baseY + cellSizePx),
        Offset(baseX - cellSizePx, baseY - cellSizePx),
        Offset(baseX + cellSizePx, baseY - cellSizePx),
        Offset(baseX - cellSizePx, baseY + cellSizePx),
        Offset(baseX + cellSizePx, baseY + cellSizePx)
    )

    return neighbors
}


fun snapToGrid(offset: Offset, cellSizePx: Float, targetType: TargetType): Offset {
    val typeOffset = if (targetType == TargetType.FOOD) -1 else 1
    return Offset(
        (offset.x / cellSizePx).toInt() * cellSizePx + typeOffset,
        (offset.y / cellSizePx).toInt() * cellSizePx + typeOffset
    )
}

@Composable
fun travelTimeHistogram(
    histogram: Map<Long, Int>,
    avgTravelTime: Long,
    binSize: Int,
    gridWidth: Float
) {
    val maxCount = histogram.values.maxOrNull() ?: 1
    val minTime = histogram.keys.minOrNull() ?: 0L
    val maxTime = histogram.keys.maxOrNull() ?: 1L

    val density = LocalDensity.current

    val textHeightSp = 25.sp
    val spacer = 4.dp
    val textHeightDp = with(density) { textHeightSp.toDp() }

    val canvasHeightDp = with(density) { gridWidth.toDp() } - (textHeightDp + spacer) * 2

    Box(
        modifier = Modifier
            .size(100.dp, with(density) { gridWidth.toDp() })
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$minTime ms",
                lineHeight = textHeightSp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(spacer))

            Canvas(
                modifier = Modifier
                    .size(100.dp, canvasHeightDp)
                    .border(1.dp, Color.Black)
            ) {
                val canvasHeightPx = size.height
                val barHeightPx = canvasHeightPx / histogram.size.toFloat()
                val avgYPx = canvasHeightPx * (avgTravelTime - minTime).toFloat() /
                        (maxTime + binSize - minTime).toFloat()

                histogram.entries.sortedBy { it.key }.forEachIndexed { index, entry ->
                    val barWidthPx = (entry.value.toFloat() / maxCount) * size.width
                    drawRect(
                        color = Color.Blue,
                        topLeft = Offset(0f, barHeightPx * index),
                        size = Size(barWidthPx, max(barHeightPx - 2, 2f))
                    )
                }

                drawLine(
                    color = Color.Red,
                    start = Offset(0f, avgYPx),
                    end = Offset(size.width, avgYPx),
                    strokeWidth = 2f
                )
            }

            Spacer(modifier = Modifier.height(spacer))

            Text(
                text = "${maxTime + binSize} ms",
                lineHeight = textHeightSp,
                color = Color.Black
            )
        }
    }
}
