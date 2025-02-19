package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun updateAntPosition(ant: Ant, cellSize: Float, gridSize: Int, nest: Target, foodSource: Target): Ant {
    val minBound = cellSize / 2
    val maxBound = gridSize * cellSize - cellSize / 2

    val potentialNewPosition = ant.position + (ant.direction * cellSize)

    return if (!isNearEdge(potentialNewPosition, minBound, maxBound)) {
        val newTarget = if (reachedTarget(ant.position, ant.currentTarget.position, cellSize)) {
            if (ant.currentTarget.type == TargetType.FOOD) {
                nest
            } else {
                foodSource
            }
        } else {
            ant.currentTarget
        }

        val newDirection = calculateDirection(ant.position, newTarget.position, ant.direction, ant.angleRange, ant.sightRange)
        val newAngle = directionToAngle(newDirection)

        val newPosition = ant.position + (newDirection * cellSize)

        ant.copy(
            position = newPosition,
            direction = newDirection,
            currentAngle = newAngle,
            currentTarget = newTarget
        )
    } else {
        val reflectedDirection = reflectDirection(ant.direction, potentialNewPosition, minBound, maxBound)
        val newPosition = ant.position + (reflectedDirection * cellSize)

        ant.copy(
            position = newPosition.coerceIn(minBound, maxBound),
            direction = reflectedDirection,
            currentAngle = directionToAngle(reflectedDirection)
        )
    }
}

fun reachedTarget(position: Offset, targetPosition: Offset, threshold: Float): Boolean {
    return sqrt((position.x - targetPosition.x).pow(2) + (position.y - targetPosition.y).pow(2)) <= threshold
}

fun calculateDirection(from: Offset, to: Offset, antDirection: Offset, angleRange: Float, antSightRange: Float): Offset {
    if (calculateDistance(from, to) > antSightRange) {
        val randomAngle = directionToAngle(antDirection) + (Random.nextFloat() * angleRange * 2 - angleRange)
        return angleToDirection(randomAngle)
    }
    val angle = atan2(to.y - from.y, to.x - from.x)
    return Offset(cos(angle), sin(angle))
}

fun calculateDistance(from: Offset, to: Offset): Float {
    val deltaX = to.x - from.x
    val deltaY = to.y - from.y
    return sqrt(deltaX * deltaX + deltaY * deltaY)
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
