package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
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

        val newPosition = updateAntPosition(ant, cellSize, gridSize, nest, currentTarget)

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
    fun `should move directly toward target when already aligned`() {
        val from = Offset(0f, 0f)
        val to = Offset(5f, 0f)
        val antDirection = Offset(1f, 0f)
        val fieldViewAngleRange = 60f
        val maxTurnAngle = 30f
        val antSightDistance = 10f

        val result = calculateDirection(from, to, antDirection, fieldViewAngleRange, maxTurnAngle, antSightDistance)

        assertEquals(1f, result.x, 0.01f)
        assertEquals(0f, result.y, 0.01f)
    }

    @Test
    fun `should turn right correctly when target is at the right edge of view range`() {
        val from = Offset(0f, 0f)
        val to = Offset(3f, -4f)
        val antDirection = Offset(-1f, 0f)
        val fieldViewAngleRange = 90f
        val maxTurnAngle = 45f
        val antSightDistance = 10f

        val result = calculateDirection(from, to, antDirection, fieldViewAngleRange, maxTurnAngle, antSightDistance)

        assertTrue(result.x > -1f)
    }

    @Test
    fun `should ignore target when it is outside field of view`() {
        val from = Offset(0f, 0f)
        val to = Offset(-10f, 10f)
        val antDirection = Offset(1f, 0f)
        val fieldViewAngleRange = 45f
        val maxTurnAngle = 30f
        val antSightDistance = 15f

        val result = calculateDirection(from, to, antDirection, fieldViewAngleRange, maxTurnAngle, antSightDistance)

        assertNotEquals(-1f, result.x)
    }

    @Test
    fun `should move in random direction when no target is visible`() {
        val from = Offset(0f, 0f)
        val to = Offset(30f, 30f)
        val antDirection = Offset(1f, 0f)
        val fieldViewAngleRange = 60f
        val maxTurnAngle = 45f
        val antSightDistance = 10f

        val result = calculateDirection(from, to, antDirection, fieldViewAngleRange, maxTurnAngle, antSightDistance)

        assertNotEquals(0f, result.x)
        assertNotEquals(0f, result.y)
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
}
