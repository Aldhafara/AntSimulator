package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class PheromoneDecisionConfig(
    val chanceToIgnore: Float = 5f,
// The chances below must add up to 100
    val strongestChance: Float = 0f,
    val farthestChance: Float = 0f,
    val closestChance: Float = 0f,
    val weakestChance: Float = 100f
) {
    val totalChance = strongestChance + farthestChance + closestChance + weakestChance
}

fun updateAntPosition(
    ant: Ant,
    cellSize: Float,
    gridSize: Int,
    nest: Target,
    foodSource: Target,
    pheromones: List<Pheromone>,
    obstacles: Set<Offset>
): Ant {
    val newHistory = ArrayDeque(ant.directionHistory)
    if (newHistory.size >= 25) newHistory.removeFirst()
    newHistory.addLast(ant.currentAngle)

    val minBound = cellSize / 2
    val maxBound = gridSize * cellSize - cellSize / 2

    val potentialNewPosition = ant.position + (ant.direction * cellSize)

    val randomDirection = getRandomDirectionInRange(ant.direction, ant.maxTurnAngle)

    if (!isNearEdge(potentialNewPosition, minBound, maxBound)) {
        val newTarget = if (reachedTarget(ant.position, ant.currentTarget.position, cellSize * 2)) {
            if (ant.currentTarget.type == TargetType.FOOD) nest else foodSource
        } else ant.currentTarget

        val pheromoneInfo = analyzePheromones(
            ant.position, ant.direction, ant.fieldViewAngleRange, ant.sightDistance, ant.currentTarget.type, pheromones
        )

        val antAngle = directionToAngle(ant.direction)
        val targetPosition = if (ant.currentTarget.type == TargetType.FOOD) foodSource.position else nest.position
        val targetInSight =
            isTargetInSight(ant.position, targetPosition, antAngle, ant.fieldViewAngleRange, ant.sightDistance)
        val obstacleInSight = if (obstacles.isEmpty()) false else areObstaclesInSight(
            ant.position,
            obstacles,
            antAngle,
            ant.fieldViewAngleRange,
            ant.sightDistance
        )

        val followChance = Random.nextFloat() * 100

        val config = PheromoneDecisionConfig()

        val scaledChance =
            (100 - config.chanceToIgnore) / 100 * (followChance - config.chanceToIgnore) + config.chanceToIgnore

        val newDirection = when {
            targetInSight -> getOffset(antAngle, directionToAngle(newTarget.position - ant.position), ant.maxTurnAngle)
            obstacleInSight -> offsetFromObstacles(
                ant.position,
                ant.currentAngle,
                obstacles,
                ant.maxTurnAngle,
                ant.sightDistance,
                ant.fieldViewAngleRange,
                cellSize
            )

            (followChance < config.chanceToIgnore || !pheromoneInfo.hasAnyValue()) -> randomDirection

            else -> when {
                scaledChance < config.strongestChance -> pheromoneInfo.strongest?.let { directionToPheromone(it) }
                scaledChance < config.strongestChance + config.farthestChance -> pheromoneInfo.farthest?.let {
                    directionToPheromone(
                        it
                    )
                }

                scaledChance < config.strongestChance + config.farthestChance + config.closestChance -> pheromoneInfo.closest?.let {
                    directionToPheromone(
                        it
                    )
                }

                else -> pheromoneInfo.weakest?.let { directionToPheromone(it) }
            } ?: randomDirection
        }

        return ant.copy(
            position = ant.position + (newDirection * cellSize),
            directionHistory = newHistory.toList(),
            direction = newDirection,
            currentAngle = directionToAngle(newDirection),
            currentTarget = newTarget
        )
    } else {
        val reflectedDirection = if (isStuck(ant.directionHistory)) randomDirection
        else reflectDirection(ant.direction, potentialNewPosition, minBound, maxBound)

        return ant.copy(
            position = (ant.position + (reflectedDirection * cellSize)).coerceIn(minBound, maxBound),
            directionHistory = newHistory.toList(),
            direction = reflectedDirection,
            currentAngle = directionToAngle(reflectedDirection)
        )
    }
}

fun isStuck(directionHistory: List<Angle>, tolerance: Float = 0.75f): Boolean {
    val averageDirection = directionHistory.map { it.normalizedValue }.average().toFloat()
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
    targetType: TargetType,
    pheromones: List<Pheromone>
): PheromoneInfo {
    var strongest: Pheromone? = null
    var weakest: Pheromone? = null
    var closest: Pheromone? = null
    var farthest: Pheromone? = null
    var minStrength = Float.MAX_VALUE
    var maxStrength = Float.MIN_VALUE
    var minDistance = Float.MAX_VALUE
    var maxDistance = Float.MIN_VALUE

    pheromones.forEach { pheromone ->
        if (pheromone.type == targetType) return@forEach

        val distance = calculateDistance(position, pheromone.position)
        if (distance > sightDistance) return@forEach

        val angle = directionToAngle(pheromone.position - position)
        if (!angleIsInRange(angle, directionToAngle(direction), fieldViewAngleRange)) return@forEach

        if (pheromone.strength > maxStrength) {
            strongest = pheromone
            maxStrength = pheromone.strength
        }
        if (pheromone.strength < minStrength && pheromone.strength >= 0.25f) {
            weakest = pheromone
            minStrength = pheromone.strength
        }
        if (distance < minDistance) {
            closest = pheromone
            minDistance = distance
        }
        if (distance > maxDistance) {
            farthest = pheromone
            maxDistance = distance
        }
    }

    return PheromoneInfo(
        strongest?.position?.relativeTo(position),
        weakest?.position?.relativeTo(position),
        closest?.position?.relativeTo(position),
        farthest?.position?.relativeTo(position)
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

fun areObstaclesInSight(
    antPosition: Offset,
    targetPositions: Set<Offset>,
    targetAngle: Angle,
    fieldViewAngleRange: Float,
    sightDistance: Float
): Boolean {
    return targetPositions.any { targetPosition ->
        isTargetInSight(antPosition, targetPosition, targetAngle, fieldViewAngleRange, sightDistance)
    }
}

fun isTargetInSight(
    antPosition: Offset,
    targetPosition: Offset,
    targetAngle: Angle,
    fieldViewAngleRange: Float,
    sightDistance: Float
): Boolean {
    return reachedTarget(antPosition, targetPosition, sightDistance) &&
            angleIsInRange(directionToAngle(targetPosition - antPosition), targetAngle, fieldViewAngleRange)
}

fun offsetFromObstacles(
    antPosition: Offset,
    antCurrentAngle: Angle,
    obstacles: Set<Offset>,
    maxTurnAngle: Float,
    sightDistance: Float,
    fieldViewAngleRange: Float,
    cellSize: Float
): Offset {

    val obstaclesInSight = obstacles
        .map { it to antPosition.getDistanceTo(it) }
        .filter { (_, distance) -> distance < sightDistance }

    val ranges = obstaclesInSight.map { (obstacle, distance) ->
        val directionAngle = directionToAngle(obstacle - antPosition)
        val angularSize = angularSize(cellSize * 2, distance)
        AngleRange(
            Angle(directionAngle.normalizedValue - angularSize),
            Angle(directionAngle.normalizedValue + angularSize)
        )
    }

    val allowRanges = calculateRemainingAngleRange(ranges, fieldViewAngleRange, antCurrentAngle)

    if (allowRanges.isEmpty()) {
        val sortedObstacles = obstaclesInSight.sortedBy { it.second }
        val closestObstacle = sortedObstacles.firstOrNull()
        val farthestObstacle = sortedObstacles.lastOrNull()

        closestObstacle?.second
            ?.takeIf { it <= cellSize * 3 }
            ?.let { return angleToDirection(antCurrentAngle.normalizedValue + 180) }

        farthestObstacle?.let { (obstacle, _) ->
            val angle = directionToAngle(antPosition - obstacle)
            return getOffset(antCurrentAngle, angle, maxTurnAngle)
        }
    }

    findWidestRange(allowRanges)
        ?.let { return getOffset(antCurrentAngle, getMidValue(it), maxTurnAngle) }

    return angleToDirection(antCurrentAngle.normalizedValue)
}

fun findWidestRange(offsets: List<AngleRange>): AngleRange? {
    return offsets.maxByOrNull { it.right.getValue() - it.left.getValue() }
}

fun getMidValue(offset: AngleRange): Angle {
    return Angle((offset.left.getValue() + offset.right.getValue()) / 2)
}

fun calculateRemainingAngleRange(
    exclusions: List<AngleRange>,
    fieldViewAngleRange: Float,
    antAngle: Angle
): List<AngleRange> {
    val mainRange = AngleRange(antAngle.minus(fieldViewAngleRange / 2), antAngle.plus(fieldViewAngleRange / 2))
    var remainingRanges = listOf(mainRange)

    for (exclusion in exclusions) {
        val newRanges = mutableListOf<AngleRange>()

        for (range in remainingRanges) {
            if (exclusion.right.getValue() <= range.left.getValue() || exclusion.left.getValue() >= range.right.getValue()) {
                newRanges.add(range)
            } else {
                if (exclusion.left.getValue() > range.left.getValue()) {
                    newRanges.add(AngleRange(range.left, exclusion.left))
                }
                if (exclusion.right.getValue() < range.right.getValue()) {
                    newRanges.add(AngleRange(exclusion.right, range.right))
                }
            }
        }
        remainingRanges = newRanges
    }

    return remainingRanges
}

fun Offset.getDistanceTo(other: Offset): Float {
    return sqrt((this.x - other.x).pow(2) + (this.y - other.y).pow(2))
}

fun angularSize(diameter: Float, distance: Float): Float {
    return Math.toDegrees((2 * atan((sqrt(2.0) * diameter / 2) / distance))).toFloat()
}

fun getOffset(
    antAngle: Angle,
    targetAngle: Angle,
    maxTurnAngle: Float
): Offset {
    val angleDiff = angularDistance(antAngle, targetAngle)

    if (abs(angleDiff) <= (maxTurnAngle / 2)) {
        return Offset(
            cos(Math.toRadians(targetAngle.normalizedValue.toDouble())).toFloat(),
            sin(Math.toRadians(targetAngle.normalizedValue.toDouble())).toFloat()
        )
    } else {
        val newAngle = antAngle + sign(angleDiff) * (maxTurnAngle / 2)
        return Offset(
            cos(Math.toRadians(newAngle.normalizedValue.toDouble())).toFloat(),
            sin(Math.toRadians(newAngle.normalizedValue.toDouble())).toFloat()
        )
    }
}

fun angularDistance(angle1: Angle, angle2: Angle): Float {
    var diff = Angle(angle2.getValue() - angle1.getValue()).normalizedValue
    if (diff > 180) diff -= 360
    if (diff == -0f) diff = 0f
    return diff
}

private fun getRandomDirectionInRange(
    antDirection: Offset,
    angleRange: Float
): Offset {
    val baseAngle = directionToAngle(antDirection)
    val randomOffset = Random.nextFloat() * angleRange * 2 - angleRange
    val randomAngle = Angle(baseAngle.normalizedValue + randomOffset)

    return angleToDirection(randomAngle.normalizedValue)
}

fun angleIsInRange(angle: Angle, previousDirection: Angle, fieldViewAngleRange: Float): Boolean {
    val halfRange = fieldViewAngleRange / 2
    val minAngle = normalizeAngle(previousDirection.normalizedValue - halfRange)
    val maxAngle = normalizeAngle(previousDirection.normalizedValue + halfRange)

    return if (minAngle < maxAngle) {
        angle.normalizedValue in minAngle..maxAngle
    } else {
        angle.normalizedValue in minAngle..360F || angle.normalizedValue in 0f..maxAngle
    }
}

fun isNearEdge(position: Offset, minBound: Float, maxBound: Float, threshold: Float = 5f): Boolean {
    return position.x <= minBound + threshold || position.x >= maxBound - threshold ||
            position.y <= minBound + threshold || position.y >= maxBound - threshold
}

fun directionToAngle(direction: Offset): Angle {
    val angle = Math.toDegrees(atan2(direction.y.toDouble(), direction.x.toDouble())).toFloat()
    return Angle(normalizeAngle(angle))
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
    val rad = angle * (PI.toFloat() / 180f)
    return Offset(cos(rad), sin(rad))
}

fun Offset.coerceIn(minBound: Float, maxBound: Float): Offset {
    val clampedX = x.coerceIn(minBound, maxBound)
    val clampedY = y.coerceIn(minBound, maxBound)
    return Offset(clampedX, clampedY)
}

operator fun Offset.times(scalar: Float) = Offset(x * scalar, y * scalar)
operator fun Offset.plus(other: Offset) = Offset(x + other.x, y + other.y)
