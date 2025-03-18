package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset
import java.time.Instant
import java.time.Clock


data class Ant(
    var position: Offset,
    var direction: Offset,
    var currentAngle: Angle,
    val currentTarget: Target,
    val directionHistory: List<Angle> = emptyList(),
    var fieldViewAngleRange: Float = 90f,
    var maxTurnAngle: Float = 10f,
    var sightDistance: Float = 50f,
    val pheromoneInterval: Long = 50,
    var lastPheromoneTime: Long = 0,
    private val clock: Clock = Clock.systemDefaultZone(),
    var tripStartTime: Instant = Instant.now(clock)
)
