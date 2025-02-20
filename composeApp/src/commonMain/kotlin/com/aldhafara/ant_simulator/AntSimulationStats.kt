package com.aldhafara.ant_simulator

import java.time.Clock
import java.time.Duration
import java.time.Instant

class AntSimulationStats(private val clock: Clock = Clock.systemDefaultZone()) {

    private val tripTimes = mutableListOf<Long>()
    private var foodDelivered = 0
    private var tripsCount = 0
    private var totalTravelTime = 0L
    private var tripStartTime: Instant = Instant.now(clock)
    private var pauseStartTime: Instant? = null
    private var totalPausedTime = 0L

    fun updateStatistics(currentTarget: TargetType, previousTarget: TargetType): Boolean {
        if (previousTarget != currentTarget) {
            val tripTime = Duration.between(tripStartTime, Instant.now(clock)).toMillis() - totalPausedTime
            totalTravelTime += tripTime
            tripsCount++
            tripStartTime = Instant.now(clock)
            totalPausedTime = 0L

            if (currentTarget == TargetType.FOOD) {
                foodDelivered++
            }

            tripTimes.add(tripTime)
            return true
        }
        return false
    }

    fun onPause() {
        pauseStartTime = Instant.now(clock)
    }

    fun onResume() {
        pauseStartTime?.let {
            totalPausedTime += Duration.between(it, Instant.now(clock)).toMillis()
            pauseStartTime = null
        }
    }

    fun getTotalTravelTime(): Long {
        return if (tripsCount > 0) totalTravelTime else 0
    }

    fun getAvgTravelTime(): Long {
        return if (tripsCount > 0) totalTravelTime / tripsCount else 0
    }

    fun getFoodDelivered() = foodDelivered
    fun getTripsCount() = tripsCount

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
}
