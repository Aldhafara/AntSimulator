package com.aldhafara.ant_simulator

import androidx.compose.ui.geometry.Offset

data class Target(
    val position: Offset,
    val type: TargetType)