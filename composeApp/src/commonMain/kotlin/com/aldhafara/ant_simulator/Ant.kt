package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset

data class Ant(
    var position: Offset,
    var direction: Offset,
    var currentAngle: Float,
    val currentTarget: Target,
    var fieldViewAngleRange: Float = 90f,
    var maxTurnAngle: Float = 10f,
    var sightDistance: Float = 50f,
    val pheromoneInterval: Long = 50,
    var lastPheromoneTime: Long = 0
) {
    fun reactToPheromones(pheromones: List<Pheromone>){}
}