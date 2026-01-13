package org.example.project

import java.io.File
import androidx.compose.runtime.Composable

@Composable
expect fun openFilePicker(onFilesSelected: (List<File>) -> Unit): () -> Unit