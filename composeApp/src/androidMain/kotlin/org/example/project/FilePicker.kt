package org.example.project

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun openFilePicker(onFilesSelected: (List<File>) -> Unit): () -> Unit {
    val context = LocalContext.current

    // ZMIANA: Używamy OpenMultipleDocuments zamiast GetContent
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val files = uris.mapNotNull { uri ->
                copyUriToFile(context, uri)
            }
            if (files.isNotEmpty()) onFilesSelected(files)
        }
    }

    // ZMIANA: Contract OpenMultipleDocuments wymaga tablicy typów MIME
    return { launcher.launch(arrayOf("*/*")) }
}

fun copyUriToFile(context: Context, uri: Uri): File? {
    return try {
        val fileName = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
        val inputStream = context.contentResolver.openInputStream(uri)

        // Pliki tymczasowe lądują w cache, skąd zostaną wysłane
        val tempFile = File(context.cacheDir, fileName)

        val outputStream = FileOutputStream(tempFile)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name
}