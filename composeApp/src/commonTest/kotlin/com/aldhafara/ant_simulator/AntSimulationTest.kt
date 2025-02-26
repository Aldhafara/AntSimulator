package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.abs

class AntSimulationTest {

    @Test
    fun `should detect when ant is near the edge`() {
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
    fun `should convert direction vector to angle correctly`() {
        assertEquals(0f, directionToAngle(Offset(1f, 0f)), 0.01f)
        assertEquals(90f, directionToAngle(Offset(0f, 1f)), 0.01f)
        assertEquals(180f, directionToAngle(Offset(-1f, 0f)), 0.01f)
        assertEquals(270f, directionToAngle(Offset(0f, -1f)), 0.01f)
    }

    @Test
    fun `should reflect direction correctly from boundaries`() {
        val minBound = 0f
        val maxBound = 100f

        assertEquals(Offset(-1f, 0f), reflectDirection(Offset(1f, 0f), Offset(maxBound + 1, 50f), minBound, maxBound))
        assertEquals(Offset(1f, 0f), reflectDirection(Offset(-1f, 0f), Offset(minBound - 1, 50f), minBound, maxBound))
        assertEquals(Offset(0f, -1f), reflectDirection(Offset(0f, 1f), Offset(50f, maxBound + 1), minBound, maxBound))
        assertEquals(Offset(0f, 1f), reflectDirection(Offset(0f, -1f), Offset(50f, minBound - 1), minBound, maxBound))
    }

    @Test
    fun `should reflect vector correctly`() {
        assertEquals(Offset(-1f, 1f), reflect(Offset(1f, 1f), Offset(1f, 0f)))
        assertEquals(Offset(1f, -1f), reflect(Offset(1f, 1f), Offset(0f, 1f)))
    }

    @Test
    fun `should convert angle to direction vector correctly`() {
        val angle = 45f
        val expectedDirection = Offset(0.707f, 0.707f)
        val result = angleToDirection(angle)

        val tolerance = 0.01f

        assertTrue(abs(result.x - expectedDirection.x) < tolerance)
        assertTrue(abs(result.y - expectedDirection.y) < tolerance)
    }

    @Test
    fun `should update ant position correctly`() {
        val currentTarget = Target(Offset(100f, 100f), TargetType.FOOD)
        val nest = Target(Offset(1f, 1f), TargetType.NEST)
        val ant = Ant(Offset(50f, 50f), Offset(1f, 0f), 0f, currentTarget)
        val gridSize = 10
        val cellSize = 10f

        val newPosition = updateAntPosition(ant, cellSize, gridSize, nest, currentTarget, emptyList())

        assertNotEquals(ant.position, newPosition)
    }

    @Test
    fun `should detect when target is within threshold range`() {
        val position = Offset(0f, 0f)
        val targetPosition = Offset(3f, 4f)
        val threshold = 5f

        val result = reachedTarget(position, targetPosition, threshold)

        assertTrue(result)
    }

    @Test
    fun `should detect when target is beyond threshold range`() {
        val position = Offset(0f, 0f)
        val targetPosition = Offset(3f, 4f)
        val threshold = 4f

        val result = reachedTarget(position, targetPosition, threshold)

        assertFalse(result)
    }

    @Test
    fun `should calculate positive angle difference`() {
        val result = angleDifference(30f, 60f)
        assertEquals(30f, result)
    }

    @ParameterizedTest
    @CsvSource(
        "0, 349, 20, 350",
        "0, 350, 20, 350",
        "0, 351, 20, 351",
        "0, 0, 20, 0",
        "0, 9, 20, 9",
        "0, 10, 20, 10",
        "0, 11, 20, 10",

        "0, 90, 20, 10",
        "90, 90, 20, 90",
        "45, 90, 20, 55",
        "180, 90, 20, 170",
        "270, 90, 20, 280"
    )
    fun `should calculate offset and turn`(
        antAngle: Float,
        targetAngle: Float,
        maxTurnAngle: Float,
        expectedAngle: Float
    ) {
        val result = offset(antAngle, targetAngle, maxTurnAngle)
        val actualAngle = directionToAngle(result)
        assertEquals(expectedAngle, actualAngle, 0.01f)
    }

    @Test
    fun `should calculate negative angle difference`() {
        val result = angleDifference(60f, 30f)
        assertEquals(-30f, result)
    }

    @Test
    fun `should handle angle difference over 180 degrees`() {
        val result = angleDifference(350f, 10f)
        assertEquals(20f, result)
    }

    @Test
    fun `should handle angle difference under -180 degrees`() {
        val result = angleDifference(10f, 350f)
        assertEquals(-20f, result)
    }

    @Test
    fun `should normalize 180-degree difference`() {
        val result = angleDifference(150f, 330f)
        assertEquals(180f, result)
    }

    @Test
    fun `should return zero for identical angles`() {
        val result = angleDifference(45f, 45f)
        assertEquals(0f, result)
    }

    @Test
    fun `should return zero for 0 and 360 degrees`() {
        val result = angleDifference(0f, 360f)
        assertEquals(0f, result)
    }

    @Test
    fun `should return zero for 180 and -180 degrees`() {
        val result = angleDifference(180f, -180f)
        assertEquals(0f, result)
    }

    @ParameterizedTest
    @CsvSource(
        "90, 90, 60, true",
        "100, 90, 60, true",
        "80, 90, 60, true",
        "130, 90, 60, false",
        "50, 90, 60, false",
        "60, 90, 60, true",
        "120, 90, 60, true",
        "355, 10, 30, true",
        "330, 10, 30, false",
        "360, 180, 360, true",
        "5, 350, 30, true",
        "320, 350, 30, false",

        "0, 180, 90, false",
        "0, 350, 90, true",

        "10, 4, 10, false",
        "10, 5, 10, true",
        "10, 15, 10, true",
        "10, 16, 10, false",
    )
    fun `angleIsInRange should behave correctly`(
        angle: Float,
        previousDirection: Float,
        fieldViewAngleRange: Float,
        expected: Boolean
    ) {
        val result = angleIsInRange(angle, previousDirection, fieldViewAngleRange)
        if (expected) {
            assertTrue(result)
        } else {
            assertFalse(result)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "5, 5, 5, 5, 0",
        "0, 0, 10, 0, 10",
        "0, 0, 0, 10, 10",
        "0, 0, 3, 4, 5",
        "-3, -4, 0, 0, 5",
        "1, 2, 4, 6, 5"
    )
    fun `calculateDistance should return correct value`(
        x1: Float, y1: Float, x2: Float, y2: Float, expectedDistance: Float
    ) {
        val position = Offset(x1, y1)
        val target = Offset(x2, y2)
        val actualDistance = calculateDistance(position, target)
        assertEquals(expectedDistance, actualDistance, 1e-6f)
    }

    @Test
    fun `direction to pheromone with both negative X and Y should normalize correctly`() {
        val pheromonePosition = Offset(-5f, -5f)
        val result = directionToPheromone(pheromonePosition)
        assertEquals(Offset(-1f, -1f), result)
    }

    @Test
    fun `direction to pheromone on negative X and positive Y axis should be correct`() {
        val pheromonePosition = Offset(-2f, 10f)
        val result = directionToPheromone(pheromonePosition)
        assertEquals(Offset(-0.2f, 1f), result)
    }

    @Test
    fun `analyzePheromones should return empty when no pheromones are in range`() {
        val position = Offset(0f, 0f)
        val direction = Offset(1f, 0f)
        val fieldViewAngleRange = 90f
        val sightDistance = 10f
        val pheromones = listOf(
            Pheromone(Offset(20f, 0f), 5f, System.currentTimeMillis(), TargetType.NEST),
            Pheromone(Offset(0f, 20f), 5f, System.currentTimeMillis(), TargetType.NEST)
        )

        val result = analyzePheromones(position, direction, fieldViewAngleRange, sightDistance, TargetType.FOOD, pheromones)

        assertNull(result.strongest)
        assertNull(result.weakest)
        assertNull(result.closest)
        assertNull(result.farthest)
    }

    @Test
    fun `analyzePheromones should return the only pheromone within range`() {
        val position = Offset(0f, 0f)
        val direction = Offset(1f, 0f)
        val fieldViewAngleRange = 90f
        val sightDistance = 10f
        val pheromones = listOf(
            Pheromone(Offset(5f, 0f), 5f, System.currentTimeMillis(), TargetType.NEST)
        )

        val result = analyzePheromones(position, direction, fieldViewAngleRange, sightDistance, TargetType.FOOD, pheromones)

        assertNotNull(result.strongest)
        assertEquals(Offset(5f, 0f), result.strongest)
        assertEquals(result.weakest, result.strongest)
        assertEquals(result.closest, result.strongest)
        assertEquals(result.farthest, result.strongest)
    }

    @Test
    fun `analyzePheromones should return the strongest and weakest pheromones when multiple are within range`() {
        val position = Offset(0f, 0f)
        val direction = Offset(1f, 0f)
        val fieldViewAngleRange = 90f
        val sightDistance = 10f
        val pheromones = listOf(
            Pheromone(Offset(3f, 0f), 5f, System.currentTimeMillis(), TargetType.NEST),
            Pheromone(Offset(7f, 0f), 10f, System.currentTimeMillis(), TargetType.NEST),
            Pheromone(Offset(9f, 0f), 2f, System.currentTimeMillis(), TargetType.NEST)
        )

        val result = analyzePheromones(position, direction, fieldViewAngleRange, sightDistance, TargetType.FOOD, pheromones)

        assertNotNull(result.strongest)
        assertEquals(Offset(7f, 0f), result.strongest)

        assertNotNull(result.weakest)
        assertEquals(Offset(9f, 0f), result.weakest)

        assertNotNull(result.closest)
        assertEquals(Offset(3f, 0f), result.closest)

        assertNotNull(result.farthest)
        assertEquals(Offset(9f, 0f), result.farthest)
    }

    @Test
    fun `analyzePheromones should exclude pheromones out of the field of view`() {
        val position = Offset(0f, 0f)
        val direction = Offset(1f, 0f)
        val fieldViewAngleRange = 45f
        val sightDistance = 10f
        val pheromones = listOf(
            Pheromone(Offset(3f, 0f), 5f, System.currentTimeMillis(), TargetType.NEST),
            Pheromone(Offset(0f, 40f), 5f, System.currentTimeMillis(), TargetType.NEST),
            Pheromone(Offset(-3f, 0f), 5f, System.currentTimeMillis(), TargetType.NEST)
        )

        val result = analyzePheromones(position, direction, fieldViewAngleRange, sightDistance, TargetType.FOOD, pheromones)

        assertNotNull(result.closest)
        assertEquals(Offset(3f, 0f), result.closest)

        assertEquals(result.closest, result.weakest)
        assertEquals(result.closest, result.farthest)
        assertEquals(result.closest, result.strongest)
    }

    @Test
    fun `analyzePheromones should handle pheromones exactly at the sight distance limit`() {
        val position = Offset(0f, 0f)
        val direction = Offset(1f, 0f)
        val fieldViewAngleRange = 90f
        val sightDistance = 10f
        val pheromones = listOf(
            Pheromone(Offset(10f, 0f), 5f, System.currentTimeMillis(), TargetType.NEST)
        )

        val result = analyzePheromones(position, direction, fieldViewAngleRange, sightDistance, TargetType.FOOD, pheromones)

        assertNotNull(result.closest)
        assertEquals(Offset(10f, 0f), result.closest)
    }

    @ParameterizedTest
    @CsvSource(
        "0.75, true, 90, 90, 90, 90, 90",
        "0.75, true, 179.5, 180, 180.5",
        "0.75, false, 271, 272, 273",
        "0.75, false, 30, 40, 50, 60",
        "0.75, true, 359.9, 360, 0.1",
        "1.0, true, 89.9, 90, 90.1",
        "0.75, false, empty"
    )
    fun testIsStuck(
        tolerance: Float,
        expected: Boolean,
        angles: String
    ) {
        val directionHistory = if (angles == "empty") emptyList() else angles.split(", ").map { it.toFloat() }
        val result = isStuck(directionHistory, tolerance)
        assertEquals(expected, result)
    }
}
