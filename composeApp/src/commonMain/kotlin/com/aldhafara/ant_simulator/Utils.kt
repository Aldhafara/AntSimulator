package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun updateAntPosition(ant: Ant, cellSize: Float, gridSize: Int, angleRange: Float = 10f): Offset {
    val minBound = cellSize / 2
    val maxBound = gridSize * cellSize - cellSize / 2

    val potentialNewPosition = ant.position + (ant.direction * cellSize)

    return if (!isNearEdge(potentialNewPosition, minBound, maxBound)) {
        val randomAngle = ant.currentAngle + (Random.nextFloat() * angleRange * 2 - angleRange)

        ant.direction = angleToDirection(randomAngle)
        ant.currentAngle = randomAngle

        ant.position + (ant.direction * cellSize)
    } else {
        val reflectedDirection = reflectDirection(ant.direction, potentialNewPosition, minBound, maxBound)

        ant.direction = reflectedDirection
        ant.currentAngle = directionToAngle(reflectedDirection)

        ant.position + (reflectedDirection * cellSize)
    }.coerceIn(minBound, maxBound)
}

fun isNearEdge(position: Offset, minBound: Float, maxBound: Float, threshold: Float = 5f): Boolean {
    return position.x <= minBound + threshold || position.x >= maxBound - threshold || position.y <= minBound + threshold || position.y >= maxBound - threshold
}

fun directionToAngle(direction: Offset): Float {
    return Math.toDegrees(atan2(direction.y.toDouble(), direction.x.toDouble())).toFloat()
}

fun reflectDirection(direction: Offset, position: Offset, minBound: Float, maxBound: Float): Offset {
    val normal: Offset
    val reflectedDirection: Offset

    if (position.y <= minBound || position.y >= maxBound) {
        normal = Offset(0f, if (position.y <= minBound) 1f else -1f)
        reflectedDirection = reflect(direction, normal)
    }
    else if (position.x <= minBound || position.x >= maxBound) {
        normal = Offset(if (position.x <= minBound) 1f else -1f, 0f)
        reflectedDirection = reflect(direction, normal)
    } else {
        return direction
    }

    return reflectedDirection
}

fun reflect(direction: Offset, normal: Offset): Offset {
    val dotProduct = direction.x * normal.x + direction.y * normal.y
    val scale = 2 * dotProduct
    return Offset(
        direction.x - scale * normal.x,
        direction.y - scale * normal.y
    )
}

fun angleToDirection(angle: Float): Offset {
    val angleInRadians = Math.toRadians(angle.toDouble()).toFloat()
    return Offset(cos(angleInRadians), sin(angleInRadians))
}

fun Offset.coerceIn(minBound: Float, maxBound: Float): Offset {
    val clampedX = x.coerceIn(minBound, maxBound)
    val clampedY = y.coerceIn(minBound, maxBound)
    return Offset(clampedX, clampedY)
}

operator fun Offset.times(scalar: Float) = Offset(x * scalar, y * scalar)
operator fun Offset.plus(other: Offset) = Offset(x + other.x, y + other.y)
