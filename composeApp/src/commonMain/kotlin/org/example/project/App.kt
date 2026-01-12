package org.example.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.network.FileClient

@Composable
fun App(discovery: DeviceDiscovery, deviceName: String = "Urządzenie") {
    val client = remember { FileClient() }
    val scope = rememberCoroutineScope()
    var targetIp by remember { mutableStateOf("") }
    var uploadProgress by remember { mutableStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val receivedFiles by FileServer.receivedFiles.collectAsState()
    var lastFilesCount by remember { mutableStateOf(receivedFiles.size) }

    val triggerPicker = openFilePicker { selectedFile ->
        if (targetIp.isNotEmpty()) {
            scope.launch {
                try {
                    isUploading = true
                    client.uploadFile(targetIp, selectedFile) { progress ->
                        uploadProgress = progress
                    }
                    snackbarHostState.showSnackbar("Wysłano pomyślnie: ${selectedFile.name}")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Błąd wysyłania: ${e.message}")
                } finally {
                    isUploading = false
                    uploadProgress = 0f
                }
            }
        }
    }

    LaunchedEffect(receivedFiles.size) {
        if (receivedFiles.size > lastFilesCount) {
            val lastFile = receivedFiles.last()
            snackbarHostState.showSnackbar("Odebrano nowy plik: ${lastFile.name}")
        }
        lastFilesCount = receivedFiles.size
    }

    LaunchedEffect(Unit) {
        val osName = System.getProperty("os.name")?.lowercase() ?: ""
        val isDesktop = osName.contains("windows") || osName.contains("mac") || osName.contains("linux")

        if (isDesktop) {
            discovery.start("Komputer")
            val userHome = System.getProperty("user.home") ?: ""
            FileServer.start("$userHome/Downloads")
        } else {
            discovery.start(deviceName)
        }
    }

    // Scaffold to najlepszy sposób na obsługę SnackbarHost na Desktopie i Androidzie
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Scaffold rezerwuje tu miejsce na powiadomienia
                .padding(16.dp)
        ) {
            if (isUploading) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Wysyłanie pliku: ${(uploadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Text("Dostępne urządzenia:", style = MaterialTheme.typography.headlineSmall)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(discovery.foundDevices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            targetIp = device.second
                            triggerPicker()
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = device.first, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "IP: ${device.second}",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Odebrane pliki:", style = MaterialTheme.typography.headlineSmall)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(receivedFiles) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = file.name,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}