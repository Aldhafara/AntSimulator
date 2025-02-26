package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset

class PheromoneTrail(
    private val decayTime: Long = 5000L
) {
    private val pheromones = mutableListOf<Pheromone>()
    private var isPaused: Boolean = false

    private var pauseTime: Long? = null
    private var pausedDuration: Long = 0

fun addPheromone(position: Offset, currentTime: Long, type :TargetType) {
    pheromones.add(Pheromone(position, 1f, currentTime, type))
}

    fun decay() {
        if (isPaused) return

        val currentTime = System.currentTimeMillis() - pausedDuration

        pheromones.forEach { pheromone ->
            val elapsedTime = (currentTime - pheromone.timestamp).toFloat()
            pheromone.strength = maxOf(0f, 1f - (elapsedTime / decayTime))
        }

        pheromones.removeAll { it.strength <= 0f }
    }

    fun getPheromones(): List<Pheromone> {
        return pheromones
    }

    fun pause() {
        if (!isPaused) {
            pauseTime = System.currentTimeMillis()
            isPaused = true
        }
    }

    fun resume() {
        if (isPaused) {
            pauseTime?.let {
                pausedDuration += System.currentTimeMillis() - it
            }
            isPaused = false
            pauseTime = null
        }
    }
}
