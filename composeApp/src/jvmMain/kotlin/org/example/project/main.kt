package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "TransferFile",
    ) {
        // Po prostu wywołujemy App() bez żadnych argumentów
        // Cała logika (discovery i nazwa) jest teraz wewnątrz App.kt
        App()
    }
}