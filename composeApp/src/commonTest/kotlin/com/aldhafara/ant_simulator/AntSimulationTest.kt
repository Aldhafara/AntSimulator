package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class AntSimulationTest {

    @Test
    fun `isNearEdge detects when ant is near the edge`() {
        val gridSize = 10
        val cellSize = 10f
        val minBound = cellSize / 2
        val maxBound = gridSize * cellSize - cellSize / 2

        assertTrue(isNearEdge(Offset(minBound + 1, 50f), minBound, maxBound))
        assertTrue(isNearEdge(Offset(maxBound - 1, 50f), minBound, maxBound))
        assertTrue(isNearEdge(Offset(50f, minBound + 1), minBound, maxBound))
        assertTrue(isNearEdge(Offset(50f, maxBound - 1), minBound, maxBound))
        assertFalse(isNearEdge(Offset(50f, 50f), minBound, maxBound))
    }

    @Test
    fun `directionToAngle converts direction vector to angle correctly`() {
        assertEquals(0f, directionToAngle(Offset(1f, 0f)), 0.01f)
        assertEquals(90f, directionToAngle(Offset(0f, 1f)), 0.01f)
        assertEquals(180f, directionToAngle(Offset(-1f, 0f)), 0.01f)
        assertEquals(-90f, directionToAngle(Offset(0f, -1f)), 0.01f)
    }

    @Test
    fun `reflectDirection correctly reflects from boundaries`() {
        val minBound = 0f
        val maxBound = 100f

        assertEquals(Offset(-1f, 0f), reflectDirection(Offset(1f, 0f), Offset(maxBound + 1, 50f), minBound, maxBound))
        assertEquals(Offset(1f, 0f), reflectDirection(Offset(-1f, 0f), Offset(minBound - 1, 50f), minBound, maxBound))
        assertEquals(Offset(0f, -1f), reflectDirection(Offset(0f, 1f), Offset(50f, maxBound + 1), minBound, maxBound))
        assertEquals(Offset(0f, 1f), reflectDirection(Offset(0f, -1f), Offset(50f, minBound - 1), minBound, maxBound))
    }

    @Test
    fun `reflect reflects vector correctly`() {
        assertEquals(Offset(-1f, 1f), reflect(Offset(1f, 1f), Offset(1f, 0f)))
        assertEquals(Offset(1f, -1f), reflect(Offset(1f, 1f), Offset(0f, 1f)))
    }

    @Test
    fun `angleToDirection converts angle to direction vector correctly`() {
        val angle = 45f
        val expectedDirection = Offset(0.707f, 0.707f)
        val result = angleToDirection(angle)

        val tolerance = 0.01f

        assertTrue(abs(result.x - expectedDirection.x) < tolerance)
        assertTrue(abs(result.y - expectedDirection.y) < tolerance)
    }

    @Test
    fun `updateAntPosition moves ant correctly`() {
        val currentTarget = Target(Offset((100f), (100f)), TargetType.FOOD)
        val nest = Target(Offset((1f), (1f)), TargetType.NEST)
        val ant = Ant(Offset(50f, 50f), Offset(1f, 0f), 0f, currentTarget)
        val gridSize = 10
        val cellSize = 10f

        val newPosition = updateAntPosition(ant, cellSize, gridSize, nest, currentTarget)

        assertNotEquals(ant.position, newPosition)
    }

    @Test
    fun `test reachedTarget - within threshold`() {
        val position = Offset(0f, 0f)
        val targetPosition = Offset(3f, 4f)
        val threshold = 5f

        val result = reachedTarget(position, targetPosition, threshold)

        assertTrue(result)
    }

    @Test
    fun `test reachedTarget - beyond threshold`() {
        val position = Offset(0f, 0f)
        val targetPosition = Offset(3f, 4f)
        val threshold = 4f

        val result = reachedTarget(position, targetPosition, threshold)

        assertFalse(result)
    }

    @Test
    fun `test calculateDirection - within sight range`() {
        val from = Offset(0f, 0f)
        val to = Offset(3f, 4f)
        val antDirection = Offset(1f, 0f)
        val angleRange = 30f
        val antSightRange = 10f

        val result = calculateDirection(from, to, antDirection, angleRange, antSightRange)

        val expectedDirection = Offset(3f, 4f).let {
            val angle = atan2(it.y - from.y, it.x - from.x)
            Offset(cos(angle), sin(angle))
        }

        assertEquals(expectedDirection.x, result.x, 0.01f)
        assertEquals(expectedDirection.y, result.y, 0.01f)
    }

    @Test
    fun `test calculateDirection - out of sight range`() {
        val from = Offset(0f, 0f)
        val to = Offset(15f, 15f)
        val antDirection = Offset(1f, 0f)
        val angleRange = 30f
        val antSightRange = 10f

        val result = calculateDirection(from, to, antDirection, angleRange, antSightRange)

        assertNotEquals(0f, result.x)
        assertNotEquals(0f, result.y)
    }

    @Test
    fun `test calculateDistance - positive distance`() {
        val from = Offset(0f, 0f)
        val to = Offset(3f, 4f)

        val result = calculateDistance(from, to)

        assertEquals(5f, result, 0.01f)
    }

    @Test
    fun `test calculateDistance - zero distance`() {
        val from = Offset(0f, 0f)
        val to = Offset(0f, 0f)

        val result = calculateDistance(from, to)

        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `test calculateDistance - negative distance values`() {
        val from = Offset(-3f, -4f)
        val to = Offset(0f, 0f)

        val result = calculateDistance(from, to)

        assertEquals(5f, result, 0.01f)
    }
}
