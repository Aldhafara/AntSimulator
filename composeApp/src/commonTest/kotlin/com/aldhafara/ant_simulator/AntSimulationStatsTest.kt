package com.aldhafara.ant_simulator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class AntSimulationStatsTest {

    private val fixedStartTime = Instant.parse("2025-01-01T12:00:00Z")
    private val baseClock = Clock.systemUTC()
    private val fixedClock = Clock.offset(baseClock, Duration.between(baseClock.instant(), fixedStartTime))

    private var stats = AntSimulationStats(clock = fixedClock)

    @BeforeEach
    fun setUp() {
        stats = AntSimulationStats(clock = fixedClock)
    }

    @Test
    fun `updateStatistics should update trip count and food delivered correctly`() {
        val initialTrips = stats.getTripsCount()
        val initialFood = stats.getFoodDelivered()

        stats.updateStatistics(TargetType.FOOD, TargetType.NEST, Instant.now(fixedClock))
        stats.updateStatistics(TargetType.NEST, TargetType.FOOD, Instant.now(fixedClock))

        assertEquals(initialTrips + 2, stats.getTripsCount())
        assertEquals(initialFood + 1, stats.getFoodDelivered())
    }

    @Test
    fun `onPause should set the pause start time`() {
        stats.onPause()

        val pauseStartTime = stats::class.java.getDeclaredField("pauseStartTime")
        pauseStartTime.isAccessible = true
        val actualPauseStartTime = pauseStartTime.get(stats) as Instant

        assertTrue(Duration.between(actualPauseStartTime, Instant.now(fixedClock)).toMillis() in 0..10)
    }

    @Test
    fun `onPause and onResume should track pause time correctly`() {
        stats.updateStatistics(TargetType.NEST, TargetType.FOOD, Instant.now(fixedClock).minusMillis(2200))

        stats.onPause()
        Thread.sleep(500)
        stats.onResume()

        stats.updateStatistics(TargetType.NEST, TargetType.FOOD, Instant.now(fixedClock).minusMillis(500))

        val histogram = stats.getHistogram(10)

        assertTrue(histogram.contains(0))
        assertTrue(histogram.contains(2200L))
    }

    @Test
    fun `onResume should reset pauseStartTime to null`() {
        stats.onPause()
        stats.onResume()

        val pauseStartTime = stats::class.java.getDeclaredField("pauseStartTime")
        pauseStartTime.isAccessible = true
        val actualPauseStartTime = pauseStartTime.get(stats)

        assertNull(actualPauseStartTime)
    }

    @Test
    fun `getAvgTravelTime should return correct average`() {
        stats.updateStatistics(TargetType.NEST, TargetType.FOOD, Instant.now(fixedClock).minusMillis(100))
        stats.updateStatistics(TargetType.FOOD, TargetType.NEST, Instant.now(fixedClock).minusMillis(200))

        assertTrue(abs(150L - stats.getAvgTravelTime()) in 0..5)
    }

    @Test
    fun `getHistogram should return correct binning`() {
        stats.updateStatistics(TargetType.FOOD, TargetType.NEST, Instant.now(fixedClock).minusMillis(250))

        stats.updateStatistics(TargetType.NEST, TargetType.FOOD, Instant.now(fixedClock).minusMillis(150))

        val binSize = 100
        val histogram = stats.getHistogram(binSize)

        val minBin = histogram.keys.minOrNull() ?: 0L
        val maxBin = histogram.keys.maxOrNull() ?: 0L

        assertTrue(histogram.isNotEmpty())
        assertEquals(((maxBin - minBin) / binSize + 1).toInt(), histogram.size)
    }

    @Test
    fun `getHistogram should include empty bins`() {
        stats.updateStatistics(TargetType.FOOD, TargetType.NEST, Instant.now(fixedClock).minusMillis(150))

        stats.updateStatistics(TargetType.NEST, TargetType.FOOD, Instant.now(fixedClock).minusMillis(1250))

        val binSize = 100
        val histogram = stats.getHistogram(binSize)

        assertTrue(histogram.contains(100L))
        assertTrue(histogram.contains(1200L))
        assertTrue(histogram.contains(200L))
        assertEquals(0, histogram[200L])
    }

    @Test
    fun `fillMissingBins should add missing bins`() {
        val histogram = mapOf(200L to 1, 500L to 2)
        val binSize = 100
        val expected = mapOf(200L to 1, 300L to 0, 400L to 0, 500L to 2)

        val result = stats.fillMissingBins(histogram, binSize)

        assertEquals(expected, result)
    }

    @Test
    fun `fillMissingBins should return empty map for empty input`() {
        val histogram = emptyMap<Long, Int>()
        val binSize = 100

        val result = stats.fillMissingBins(histogram, binSize)

        assertEquals(emptyMap<Long, Int>(), result)
    }

    @Test
    fun `fillMissingBins should return same map when all bins are present`() {
        val histogram = mapOf(100L to 1, 200L to 2, 300L to 3, 400L to 4)
        val binSize = 100
        val expected = histogram

        val result = stats.fillMissingBins(histogram, binSize)

        assertEquals(expected, result)
    }
}
