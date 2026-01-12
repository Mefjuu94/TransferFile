package org.example.project

import java.io.File
import java.awt.FileDialog
import java.awt.Frame
import androidx.compose.runtime.Composable

@Composable
actual fun openFilePicker(onFileSelected: (File) -> Unit): () -> Unit {
    return {
        val fileDialog = FileDialog(null as Frame?, "Wybierz plik", FileDialog.LOAD)
        fileDialog.isVisible = true
        if (fileDialog.file != null) {
            onFileSelected(File(fileDialog.directory + fileDialog.file))
        }
    }
}