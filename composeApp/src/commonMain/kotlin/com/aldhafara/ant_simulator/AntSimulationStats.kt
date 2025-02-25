package com.aldhafara.ant_simulator

import java.time.Clock
import java.time.Duration
import java.time.Instant

class AntSimulationStats(private val clock: Clock = Clock.systemDefaultZone()) {
    private val tripTimes = mutableListOf<Long>()
    private var foodDelivered = 0
    private var tripsCount = 0
    private var totalTravelTime = 0L

    private val pauseDurations = mutableListOf<Pair<Instant, Instant>>()
    private var pauseStartTime: Instant? = null
    private var isPaused = false

    fun updateStatistics(currentTarget: TargetType, previousTarget: TargetType, tripStartTime: Instant): Boolean {
        if (previousTarget == currentTarget) return false

        val now = Instant.now(clock)
        val tripTime = Duration.between(tripStartTime, now).toMillis() - getTotalPausedTime(tripStartTime, now)

        totalTravelTime += tripTime
        tripsCount++

        if (currentTarget == TargetType.FOOD) {
            foodDelivered++
        }

        tripTimes.add(tripTime)
        return true
    }

    fun onPause() {
        if (!isPaused) {
            pauseStartTime = Instant.now(clock)
            isPaused = true
        }
    }

    fun onResume() {
        pauseStartTime?.let {
            pauseDurations.add(it to Instant.now(clock))
            pauseStartTime = null
        }

        isPaused = false
    }

    private fun getTotalPausedTime(start: Instant, end: Instant): Long {
        return pauseDurations
            .filter { it.second.isAfter(start) && it.first.isBefore(end) }
            .sumOf { Duration.between(it.first, it.second).toMillis() }
    }

    fun getHistogram(binSize: Int): Map<Long, Int> {
            val histogram = tripTimes.groupingBy { it / binSize * binSize }.eachCount()
            return fillMissingBins(histogram, binSize)
        }

    fun fillMissingBins(histogram: Map<Long, Int>, binSize: Int): Map<Long, Int> {
            if (histogram.isEmpty()) return emptyMap()

            val minTime = (histogram.keys.minOrNull() ?: 0L)
            val maxTime = (histogram.keys.maxOrNull() ?: 1L)

            val bins = mutableMapOf<Long, Int>()
            var time = minTime
            while (time <= maxTime) {
                    bins[time] = histogram[time] ?: 0
                    time += binSize
                }
            return bins
        }

    fun getAvgTravelTime(): Long {
        val validTripTimes = tripTimes.filter { it > 0 }
        return if (validTripTimes.isNotEmpty()) validTripTimes.average().toLong() else 0L
    }

    fun getFoodDelivered() = foodDelivered
    fun getTripsCount() = tripsCount
}
