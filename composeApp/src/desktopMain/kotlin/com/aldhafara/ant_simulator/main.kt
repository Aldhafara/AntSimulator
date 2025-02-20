package com.aldhafara.ant_simulator

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AntSimulator",
    ) {
        antSimulator()
    }
}