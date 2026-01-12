package org.example.project

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val discovery = remember { DeviceDiscovery() }

    LaunchedEffect(Unit) {
        discovery.start("Komputer Stacjonarny")
    }

    Window(onCloseRequest = ::exitApplication, title = "TransferFile") {
        App(discovery = discovery)
    }
}