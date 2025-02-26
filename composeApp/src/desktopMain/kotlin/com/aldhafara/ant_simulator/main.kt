package com.aldhafara.ant_simulator

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Toolkit

fun main() = application {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val windowSize = minOf(screenSize.width, screenSize.height) * 0.9
    val sizeDp = windowSize.dp

    Window(
        onCloseRequest = ::exitApplication,
        title = "AntSimulator",
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            width = sizeDp,
            height = sizeDp
        )
    ) {
        AntSimulator(sizeDp)
    }
}