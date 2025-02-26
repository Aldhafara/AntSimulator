package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset

data class Pheromone (
    val position: Offset,
    var strength: Float,
    val timestamp: Long,
    val type: TargetType
)