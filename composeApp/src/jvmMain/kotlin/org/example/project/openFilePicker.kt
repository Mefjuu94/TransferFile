package org.example.project

import java.io.File
import java.awt.FileDialog
import java.awt.Frame
import androidx.compose.runtime.Composable

@Composable
actual fun openFilePicker(onFilesSelected: (List<File>) -> Unit): () -> Unit {
    return {
        // Używamy null jako Frame, co jest dopuszczalne w AWT
        val fileDialog = FileDialog(null as Frame?, "Wybierz pliki", FileDialog.LOAD)

        // KLUCZ: Włączenie wyboru wielu plików
        fileDialog.isMultipleMode = true

        fileDialog.isVisible = true

        // Pobieramy tablicę wybranych plików (może być pusta)
        val selectedFiles = fileDialog.files

        if (selectedFiles != null && selectedFiles.isNotEmpty()) {
            onFilesSelected(selectedFiles.toList())
        }
    }
}