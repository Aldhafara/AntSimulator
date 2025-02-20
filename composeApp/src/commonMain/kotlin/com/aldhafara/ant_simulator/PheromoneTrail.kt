package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset

class PheromoneTrail(
    val decayTime: Long = 10000L,
    val decayRate: Float = 0.001f,
    val pheromoneInterval: Long = 5
) {
    private val pheromones = mutableListOf<Pheromone>()

    fun addPheromone(position: Offset) {
//        if ((System.currentTimeMillis() % pheromoneInterval) == 0) {
            pheromones.add(Pheromone(position, 1f, System.currentTimeMillis()))
//        }
    }

    fun decay() {
        val currentTime = System.currentTimeMillis()
        pheromones.removeAll { currentTime - it.timestamp > decayTime }
        pheromones.forEach { it.strength = maxOf(0f, it.strength - decayRate) }
    }

    fun getPheromones(): List<Pheromone> {
        return pheromones
    }
}
