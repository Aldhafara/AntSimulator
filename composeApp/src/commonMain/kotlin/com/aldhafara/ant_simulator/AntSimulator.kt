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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import kotlin.math.max
import kotlin.random.Random

@Composable
fun AntSimulator(sizeDp: Dp) {

    val cellSize = 5.dp
    val gridSize = (sizeDp/cellSize).toInt()

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

    LaunchedEffect(Unit) {
        repeat(numberOfAnts) {
            ants.add(Ant(nest.position, getRandomDirection(), 30f, foodSource))
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
                            pheromoneTrail.getPheromones()
                        )
                        val currentTime = Instant.now(clock).toEpochMilli()
                        if (currentTime - ant.lastPheromoneTime >= ant.pheromoneInterval) {
                            pheromoneTrail.addPheromone(ant.position, currentTime, previousTarget)
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

        Spacer(modifier = Modifier.height(1.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            antSimulationCanvas(
                gridSize,
                cellSize,
                gridWidth,
                ants,
                foodSource.position,
                nest.position,
                pheromoneTrail
            )
            Spacer(modifier = Modifier.width(16.dp))
            travelTimeHistogram(histogramState, stats.getAvgTravelTime(), binSize, gridWidth)
        }
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
    pheromoneTrail: PheromoneTrail
) {
    Box(
        modifier = Modifier
            .size(with(LocalDensity.current) { (gridWidth + 4).toDp() })
            .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
            .padding(2.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGrid(gridSize, cellSize)
            drawPheromones(pheromoneTrail.getPheromones())
            ants.forEach { ant ->
                drawAnt(cellSize, ant.position, ant.direction, ant.sightDistance, ant.fieldViewAngleRange)
            }
            drawTarget(cellSize, initialTarget)
            drawNest(cellSize, nest)
        }
    }
}

@Composable
fun travelTimeHistogram(histogram: Map<Long, Int>, avgTravelTime: Long, binSize: Int, gridWidth: Float) {
    val maxCount = histogram.values.maxOrNull() ?: 1
    val minTime = histogram.keys.minOrNull() ?: 0L
    val maxTime = histogram.keys.maxOrNull() ?: 1L

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$minTime ms", color = Color.Black)

        Spacer(modifier = Modifier.height(4.dp))

        val canvasHeight = (gridWidth.dp - 150.dp) - 50.dp

        Canvas(modifier = Modifier.size(100.dp, canvasHeight).border(1.dp, Color.Black)) {
            val barHeight = size.height.div(histogram.size)
            val avgY = size.height * (avgTravelTime - minTime).toFloat() / (maxTime + binSize - minTime).toFloat()

            histogram.entries.sortedBy { it.key }.forEachIndexed { index, entry ->
                val barWidth = (entry.value.toFloat() / maxCount) * size.width
                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(0f, barHeight.times(index)),
                    size = Size(barWidth, max(barHeight - 2, 2f))
                )
            }

            drawLine(
                color = Color.Red,
                start = Offset(0f, avgY),
                end = Offset(size.width, avgY),
                strokeWidth = 2f
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text("${maxTime + binSize} ms", color = Color.Black)
    }
}

