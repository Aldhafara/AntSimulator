package com.aldhafara.ant_simulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AntSimulator() {
    val gridSize = 100
    val cellSize = 5.dp
    val gridWidth = gridSize * cellSize.value

    val foodSource = Target(Offset((gridSize -5) * cellSize.value, (gridSize -5) * cellSize.value), TargetType.FOOD)
    val nest = Target(Offset((5) * cellSize.value, (5) * cellSize.value), TargetType.NEST)
    val ant = remember { mutableStateOf(Ant(Offset(gridWidth / 2, gridWidth / 2), Offset(0f, -1f), 30f, foodSource)) }
    var isRunning by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isRunning) {
        if (isRunning) {
            coroutineScope.launch {
                while (isRunning) {
                    delay(10) //Speed
                    ant.value = updateAntPosition(ant.value, cellSize.value, gridSize, nest, foodSource)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AntSimulationControls(isRunning) { isRunning = !isRunning }
        Spacer(modifier = Modifier.height(16.dp))
        AntSimulationCanvas(gridSize, cellSize, gridWidth, ant.value, foodSource.position, nest.position)
    }
}

@Composable
fun AntSimulationControls(isRunning: Boolean, onToggle: () -> Unit) {
    Button(onClick = onToggle, modifier = Modifier.padding(16.dp)) {
        Text(if (isRunning) "Pause" else "Start Simulation")
    }
}

@Composable
fun AntSimulationCanvas(
    gridSize: Int,
    cellSize: Dp,
    gridWidth: Float,
    ant: Ant,
    initialTarget: Offset,
    nest: Offset
) {
    Box(
        modifier = Modifier
            .size(with(LocalDensity.current) { (gridWidth + 4).toDp() })
            .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
            .padding(2.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGrid(gridSize, cellSize)
            drawAnt(cellSize, ant.position, ant.direction)
            drawTarget(cellSize, initialTarget)
            drawNest(cellSize, nest)
        }
    }
}