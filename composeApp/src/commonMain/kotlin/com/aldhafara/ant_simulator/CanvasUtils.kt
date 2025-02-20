package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Size

fun DrawScope.drawGrid(gridSize: Int, cellSize: Dp) {
    for (i in 0 until gridSize) {
        for (j in 0 until gridSize) {
            drawRect(
                color = Color.LightGray,
                topLeft = Offset(i * cellSize.value, j * cellSize.value),
                size = Size(cellSize.value, cellSize.value)
            )
        }
    }
}

fun DrawScope.drawAnt(cellSize: Dp, antPosition: Offset, direction: Offset) {
    val circleRadius = cellSize.value / 4
    drawCircle(Color.Red, radius = circleRadius, center = antPosition)
    drawDirectionLine(antPosition, direction, circleRadius)
}

fun DrawScope.drawTarget(cellSize: Dp, position: Offset) {
    val circleRadius = cellSize.value
    drawCircle(Color(0xFF005700), radius = circleRadius, center = position)
}

fun DrawScope.drawNest(cellSize: Dp, position: Offset) {
    val circleRadius = cellSize.value
    drawCircle(Color.Red, radius = circleRadius, center = position)
}

fun DrawScope.drawPheromones(pheromones: List<Pheromone>) {
    pheromones.forEach {
        drawCircle(
            color = Color.Magenta.copy(alpha = it.strength),
            radius = 3f,
            center = it.position
        )
    }
}

private fun DrawScope.drawDirectionLine(
    antPosition: Offset,
    direction: Offset,
    circleRadius: Float
) {
    val stickLength = 30f
    val lineStart = antPosition + (direction * circleRadius)
    val lineEnd = lineStart + (direction * stickLength)
    drawLine(Color.DarkGray, lineStart, lineEnd, strokeWidth = 2f)
}
