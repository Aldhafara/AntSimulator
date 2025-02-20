package com.aldhafara.ant_simulator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AntSimulationStatsTest {

    private val systemClock = Clock.systemDefaultZone()
    private val fixedStartTime = ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)
    private val fixedClock = Clock.offset(systemClock, Duration.between(Instant.now(), fixedStartTime.toInstant()))

    private var stats = AntSimulationStats(clock = fixedClock)

    @BeforeEach
    fun setUp() {
        stats = AntSimulationStats(clock = fixedClock)
    }

    @Test
    fun `updateStatistics should update trip count and food delivered correctly`() {
        val initialTrips = stats.getTripsCount()
        val initialFood = stats.getFoodDelivered()

        stats.updateStatistics(TargetType.FOOD, TargetType.NEST)
        stats.updateStatistics(TargetType.NEST, TargetType.FOOD)

        assertEquals(initialTrips + 2, stats.getTripsCount())
        assertEquals(initialFood + 1, stats.getFoodDelivered())
    }

    @Test
    fun `onPause should set the pause start time`() {
        stats.onPause()

        val pauseStartTime = stats::class.java.getDeclaredField("pauseStartTime")
        pauseStartTime.isAccessible = true
        val actualPauseStartTime = pauseStartTime.get(stats) as Instant

        assertTrue(Duration.between(actualPauseStartTime, fixedClock.instant()).toMillis() in 0..10)
    }

    @Test
    fun `onPause and onResume should track pause time correctly`() {
        stats.updateStatistics(TargetType.NEST, TargetType.FOOD)
        Thread.sleep(5)
        stats.onPause()
        Thread.sleep(500)
        stats.onResume()

        stats.updateStatistics(TargetType.FOOD, TargetType.NEST)

        val tripTime = stats.getTotalTravelTime()

        assertTrue(tripTime in 0..20)
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
        stats.updateStatistics(TargetType.FOOD, TargetType.NEST)
        Thread.sleep(50)

        stats.updateStatistics(TargetType.NEST, TargetType.FOOD)

        val avgTime = stats.getAvgTravelTime()
        assertTrue(avgTime > 0)
    }

    @Test
    fun `getHistogram should return correct binning`() {
        Thread.sleep(150) // First trip: 150ms
        stats.updateStatistics(TargetType.FOOD, TargetType.NEST)

        Thread.sleep(250) // Second trip: 250ms
        stats.updateStatistics(TargetType.NEST, TargetType.FOOD)

        val binSize = 100
        val histogram = stats.getHistogram(binSize)

        val minBin = histogram.keys.minOrNull() ?: 0L
        val maxBin = histogram.keys.maxOrNull() ?: 0L

        assertTrue(histogram.isNotEmpty())
        assertEquals(((maxBin - minBin) / binSize + 1).toInt(), histogram.size)
    }

    @Test
    fun `getHistogram should include empty bins`() {
        Thread.sleep(150) // First trip: 150ms
        stats.updateStatistics(TargetType.FOOD, TargetType.NEST)

        Thread.sleep(1250) // Second trip: 1250ms
        stats.updateStatistics(TargetType.NEST, TargetType.FOOD)

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
