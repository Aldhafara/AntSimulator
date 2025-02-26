package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun updateAntPosition(
    ant: Ant,
    cellSize: Float,
    gridSize: Int,
    nest: Target,
    foodSource: Target,
    pheromones: List<Pheromone>
): Ant {
    val chanceToIgnorePheromones = 15

    // The chances below must add up to 100
    val strongestChance = 80
    val farthestChance = 7
    val closestChance = 7
    val weakestChance = 6

    val minBound = cellSize / 2
    val maxBound = gridSize * cellSize - cellSize / 2

    val potentialNewPosition = ant.position + (ant.direction * cellSize)
    val newHistory = (ant.directionHistory + ant.currentAngle).takeLast(25)

    val randomDirection = getRandomDirectionInRange(ant.direction, ant.maxTurnAngle)

    if (!isNearEdge(potentialNewPosition, minBound, maxBound)) {
        val newTarget = if (reachedTarget(ant.position, ant.currentTarget.position, cellSize * 2)) {
            if (ant.currentTarget.type == TargetType.FOOD) nest else foodSource
        } else ant.currentTarget

        val pheromoneInfo = analyzePheromones(
            ant.position, ant.direction, ant.fieldViewAngleRange, ant.sightDistance, pheromones
        )

        val antAngle = directionToAngle(ant.direction)
        val targetPosition = if (ant.currentTarget.type == TargetType.FOOD) foodSource.position else nest.position
        val targetInSight = isTargetInSight(ant.position, targetPosition, antAngle, ant.fieldViewAngleRange, ant.sightDistance)

        val followChance = Random.nextFloat() * 100

        val newDirection = when {
            targetInSight -> offset(antAngle, directionToAngle(newTarget.position - ant.position), ant.maxTurnAngle)

            followChance < chanceToIgnorePheromones -> randomDirection

            else -> {
                val adjustedChance = followChance - chanceToIgnorePheromones
                val scaledChance = (0.7 * adjustedChance + 30)

                when {
                    scaledChance < strongestChance -> pheromoneInfo.strongest?.let { directionToPheromone(it) }
                    scaledChance < strongestChance + farthestChance -> pheromoneInfo.farthest?.let { directionToPheromone(it) }
                    scaledChance < strongestChance + farthestChance + closestChance -> pheromoneInfo.closest?.let { directionToPheromone(it) }
                    else -> pheromoneInfo.weakest?.let { directionToPheromone(it) }
                } ?: randomDirection
            }
        }

        return ant.copy(
            position = ant.position + (newDirection * cellSize),
            directionHistory = newHistory,
            direction = newDirection,
            currentAngle = directionToAngle(newDirection),
            currentTarget = newTarget
        )
    } else {
        val reflectedDirection = if (isStuck(ant.directionHistory)) randomDirection
        else reflectDirection(ant.direction, potentialNewPosition, minBound, maxBound)

        return ant.copy(
            position = (ant.position + (reflectedDirection * cellSize)).coerceIn(minBound, maxBound),
            directionHistory = newHistory,
            direction = reflectedDirection,
            currentAngle = directionToAngle(reflectedDirection)
        )
    }
}

fun isStuck(directionHistory: List<Float>, tolerance: Float = 0.75f): Boolean {
    val averageDirection = directionHistory.average().toFloat()
    val criticalAngles = listOf(0f, 90f, 180f, 270f, 360f)

    return criticalAngles.any { angle ->
        abs(averageDirection - angle) <= tolerance
    }
}

fun directionToPheromone(
    pheromonePosition: Offset
): Offset {
    val scalar = max(abs(pheromonePosition.x), abs(pheromonePosition.y))
    return Offset(pheromonePosition.x.div(scalar), pheromonePosition.y.div(scalar))
}

data class PheromoneInfo(
    val strongest: Offset? = null,
    val weakest: Offset? = null,
    val closest: Offset? = null,
    val farthest: Offset? = null
) {
    fun hasAnyValue() = listOf(strongest, weakest, closest, farthest).any { it != null }
}

fun analyzePheromones(
    position: Offset,
    direction: Offset,
    fieldViewAngleRange: Float,
    sightDistance: Float,
    pheromones: List<Pheromone>
): PheromoneInfo {
    var strongest: Pair<Pheromone, Float>? = null
    var weakest: Pair<Pheromone, Float>? = null
    var closest: Pair<Pheromone, Float>? = null
    var farthest: Pair<Pheromone, Float>? = null

    val epsilon = 1e-3f

    pheromones.forEach { pheromone ->
        val distance = calculateDistance(position, pheromone.position)

        if (distance > sightDistance || distance < epsilon) return@forEach

        val targetAngleRad = atan2(pheromone.position.y - position.y, pheromone.position.x - position.x)
        val targetAngle = Math.toDegrees(targetAngleRad.toDouble()).toFloat()
        val normalizedTargetAngle = normalizeAngle(targetAngle)

        val antAngle = directionToAngle(direction)
        if (!angleIsInRange(normalizedTargetAngle, antAngle, fieldViewAngleRange)) return@forEach

        if (strongest == null || pheromone.strength > strongest!!.first.strength) {
            strongest = Pair(pheromone, distance)
        }

        if (weakest == null || pheromone.strength < weakest!!.first.strength) {
            weakest = Pair(pheromone, distance)
        }

        if (closest == null || distance < closest!!.second) {
            closest = Pair(pheromone, distance)
        }

        if (farthest == null || distance > farthest!!.second) {
            farthest = Pair(pheromone, distance)
        }
    }

    return PheromoneInfo(
        strongest = strongest?.first?.position?.relativeTo(position),
        weakest = weakest?.first?.position?.relativeTo(position),
        closest = closest?.first?.position?.relativeTo(position),
        farthest = farthest?.first?.position?.relativeTo(position)
    )
}

fun Offset.relativeTo(reference: Offset) = this - reference

fun reachedTarget(position: Offset, targetPosition: Offset, threshold: Float): Boolean {
    return calculateDistance(position, targetPosition) <= threshold
}

fun calculateDistance(
    position: Offset,
    targetPosition: Offset
) = sqrt((position.x - targetPosition.x).pow(2) + (position.y - targetPosition.y).pow(2))

fun isTargetInSight(
    antPosition: Offset,
    targetPosition: Offset,
    targetAngle: Float,
    fieldViewAngleRange: Float,
    sightDistance: Float
): Boolean {
    return reachedTarget(antPosition, targetPosition, sightDistance) &&
            angleIsInRange(directionToAngle(targetPosition - antPosition), targetAngle, fieldViewAngleRange)
}

fun offset(
    antAngle: Float,
    targetAngle: Float,
    maxTurnAngle: Float
): Offset {
    val angleDiff = angleDifference(antAngle, targetAngle)

    if (abs(angleDiff) <= (maxTurnAngle / 2)) {
        return Offset(
            cos(Math.toRadians(targetAngle.toDouble())).toFloat(),
            sin(Math.toRadians(targetAngle.toDouble())).toFloat()
        )
    } else {
        val newAngle = antAngle + sign(angleDiff) * (maxTurnAngle / 2)
        return Offset(
            cos(Math.toRadians(newAngle.toDouble())).toFloat(),
            sin(Math.toRadians(newAngle.toDouble())).toFloat()
        )
    }
}

fun angleDifference(angle1: Float, angle2: Float): Float {
    var diff = normalizeAngle(angle2 - angle1)
    if (diff > 180) diff -= 360
    return diff
}

private fun getRandomDirectionInRange(
    antDirection: Offset,
    angleRange: Float
): Offset {
    val baseAngle = directionToAngle(antDirection)
    val randomOffset = Random.nextFloat() * angleRange * 2 - angleRange
    val randomAngle = normalizeAngle(baseAngle + randomOffset)

    return angleToDirection(randomAngle)
}

fun angleIsInRange(angle: Float, previousDirection: Float, fieldViewAngleRange: Float): Boolean {
    val halfRange = fieldViewAngleRange / 2
    val minAngle = normalizeAngle(previousDirection - halfRange)
    val maxAngle = normalizeAngle(previousDirection + halfRange)

    return if (minAngle < maxAngle) {
        angle in minAngle..maxAngle
    } else {
        angle in minAngle..360F || angle in 0f..maxAngle
    }
}

fun isNearEdge(position: Offset, minBound: Float, maxBound: Float, threshold: Float = 5f): Boolean {
    return position.x <= minBound + threshold || position.x >= maxBound - threshold ||
            position.y <= minBound + threshold || position.y >= maxBound - threshold
}

fun directionToAngle(direction: Offset): Float {
    val angle = Math.toDegrees(atan2(direction.y.toDouble(), direction.x.toDouble())).toFloat()
    return normalizeAngle(angle)
}

fun normalizeAngle(angle: Float): Float {
    return (angle + 360) % 360
}

fun reflectDirection(direction: Offset, position: Offset, minBound: Float, maxBound: Float): Offset {
    val normal: Offset
    val reflectedDirection: Offset

    if (position.y <= minBound || position.y >= maxBound) {
        normal = Offset(0f, if (position.y <= minBound) 1f else -1f)
        reflectedDirection = reflect(direction, normal)
    } else if (position.x <= minBound || position.x >= maxBound) {
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
    return getOffsetFromAngle(angleInRadians)
}

private fun getOffsetFromAngle(targetAngle: Float) = Offset(cos(targetAngle), sin(targetAngle))

fun Offset.coerceIn(minBound: Float, maxBound: Float): Offset {
    val clampedX = x.coerceIn(minBound, maxBound)
    val clampedY = y.coerceIn(minBound, maxBound)
    return Offset(clampedX, clampedY)
}

operator fun Offset.times(scalar: Float) = Offset(x * scalar, y * scalar)
operator fun Offset.plus(other: Offset) = Offset(x + other.x, y + other.y)
