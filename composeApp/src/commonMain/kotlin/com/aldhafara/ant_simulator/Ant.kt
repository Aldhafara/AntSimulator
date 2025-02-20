package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset

data class Ant(
    var position: Offset,
    var direction: Offset,
    var currentAngle: Float,
    val currentTarget: Target,
    var angleRange: Float = 10f,
    var sightRange: Float = 100f,
    val pheromoneInterval: Int = 10
) {
    fun reactToPheromones(pheromones: List<Pheromone>){}
}