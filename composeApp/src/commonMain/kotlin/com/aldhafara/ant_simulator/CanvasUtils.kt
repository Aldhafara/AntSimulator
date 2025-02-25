package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Size
import kotlin.math.atan2

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

fun DrawScope.drawAnt(
    cellSize: Dp,
    antPosition: Offset,
    direction: Offset,
    sightDistance: Float,
    fieldViewAngleRange: Float
) {
    val antRadius = cellSize.value / 4
    drawCircle(Color.Red, radius = antRadius, center = antPosition)
    drawFieldOfView(antPosition, direction, sightDistance, fieldViewAngleRange)
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
            color = Color(0xFF53BB41).copy(alpha = it.strength),
            radius = 3f,
            center = it.position
        )
    }
}

private fun DrawScope.drawFieldOfView(
    antPosition: Offset,
    direction: Offset,
    sightDistance: Float,
    fieldOfViewAngle: Float
) {
    val directionAngle = Math.toDegrees(atan2(direction.y.toDouble(), direction.x.toDouble())).toFloat()

    val startAngle = directionAngle - fieldOfViewAngle / 2

    drawArc(
        color = Color(0xFF888888),
        startAngle = startAngle,
        sweepAngle = fieldOfViewAngle,
        useCenter = true,
        topLeft = Offset(antPosition.x - sightDistance, antPosition.y - sightDistance),
        size = Size(sightDistance * 2, sightDistance * 2),
        alpha = 0.3f
    )
}
