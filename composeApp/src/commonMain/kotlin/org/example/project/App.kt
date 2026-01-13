package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.network.FileClient
import java.io.File

// DEFINICJA KOLORÓW (CIEMNY MOTYW)
val DarkBackground = Color(0xFF121212)
val SurfaceColor = Color(0xFF1E1E1E)
val AccentPurple = Color(0xFFD0BCFF)
val TextPrimary = Color(0xFFE6E1E5)

@Composable
fun App() {
    // 1. STANY GŁÓWNE
    var deviceName by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }

    // OWIJAMY CAŁĄ APLIKACJĘ W CIEMNY MOTYW
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AccentPurple,
            background = DarkBackground,
            surface = SurfaceColor,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
            if (!isInitialized) {
                // EKRAN STARTOWY: Pytanie o nazwę
                InitialSetupScreen { name ->
                    deviceName = name
                    isInitialized = true
                }
            } else {
                // WŁAŚCIWA APLIKACJA
                MainAppScreen(deviceName)
            }
        }
    }
}

@Composable
fun InitialSetupScreen(onConfirm: (String) -> Unit) {
    var tempName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Witaj w TransferFile", style = MaterialTheme.typography.headlineMedium, color = AccentPurple)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Podaj nazwę tego urządzenia:", color = TextPrimary)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = tempName,
            onValueChange = { tempName = it },
            label = { Text("Nazwa urządzenia") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if (tempName.isNotBlank()) onConfirm(tempName) },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Rozpocznij")
        }
    }
}

@Composable
fun MainAppScreen(deviceName: String) {
    val client = remember { FileClient() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val discovery = remember { DeviceDiscovery() }

    var currentDeviceName by remember { mutableStateOf(deviceName) }
    var portText by remember { mutableStateOf("9999") }
    val receivedFiles by FileServer.receivedFiles.collectAsState()
    var targetIp by remember { mutableStateOf("") }
    var uploadProgress by remember { mutableStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }
    var lastFilesCount by remember { mutableStateOf(receivedFiles.size) }
    var targetPort by remember { mutableStateOf(9999) } // DODAJ TĘ LINIĘ

    var currentUploadingFileName by remember { mutableStateOf("") }

    // Pobieranie ścieżki
    val downloadPath = remember {
        val isAndroid = System.getProperty("os.name").lowercase().contains("android") ||
                System.getProperty("java.vendor").lowercase().contains("android")
        if (isAndroid) "/storage/emulated/0/Download" else {
            val userHome = System.getProperty("user.home") ?: ""
            if (userHome.isNotEmpty()) "$userHome/Downloads" else "Downloads"
        }
    }

    // Picker plików
    val triggerPicker = openFilePicker { selectedFiles: List<File> -> // Zmienione na listę
        if (targetIp.isNotEmpty()) {
            scope.launch {
                try {
                    isUploading = true
                    val total = selectedFiles.size

                    selectedFiles.forEachIndexed { index, file ->
                        currentUploadingFileName = "Wysyłanie (${index + 1}/$total): ${file.name}"

                        client.uploadFile(targetIp, targetPort, file) { progress -> // DODANO targetPort
                            uploadProgress = progress
                        }
                    }

                    snackbarHostState.showSnackbar("Pomyślnie wysłano $total plików")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Błąd: ${e.message}")
                } finally {
                    isUploading = false
                    currentUploadingFileName = ""
                    uploadProgress = 0f
                }
            }
        }
    }

    LaunchedEffect(receivedFiles.size) {
        // Jeśli przybyło plików od ostatniego sprawdzenia
        if (receivedFiles.size > lastFilesCount && lastFilesCount != 0) {
            val newestFile = receivedFiles.maxByOrNull { it.lastModified() }
            newestFile?.let {
                snackbarHostState.showSnackbar("Odebrano nowy plik: ${it.name}")
            }
        }
        lastFilesCount = receivedFiles.size
    }

    // Auto-start serwera
    LaunchedEffect(Unit) {
        val p = portText.toIntOrNull() ?: 9999
        FileServer.start(downloadPath, p)
        discovery.start(currentDeviceName, p)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // SEKCJA: USTAWIENIA (KARTA)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Twoje urządzenie: $currentDeviceName", color = AccentPurple)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = portText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) portText = it },
                            label = { Text("Port") },
                            modifier = Modifier.width(100.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val p = portText.toIntOrNull() ?: 9999
                            scope.launch(Dispatchers.IO) {
                                FileServer.stop()
                                discovery.stop()
                                delay(800)
                                FileServer.start(downloadPath, p)
                                discovery.start(currentDeviceName, p)
                            }
                        }
                        ) { Text("Zastosuj") }
                    }
                }
            }

            if (isUploading) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = currentUploadingFileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentPurple
                    )
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEKCJA: URZĄDZENIA
            Text("Dostępne urządzenia:", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
            LazyColumn(modifier = Modifier.height(150.dp)) {
                items(discovery.foundDevices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (targetIp == device.ip) Color(0xFF3700B3) else SurfaceColor
                        ),
                        onClick = {
                            targetIp = device.ip
                            targetPort = device.serverPort // TA LINIA JEST KLUCZOWA!
                            triggerPicker()
                        }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, color = Color.White)
                                Text("${device.ip}:${device.serverPort}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                            }
                            if (targetIp == device.ip) Icon(Icons.Default.Check, "Wybrano", tint = AccentPurple)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)

            // SEKCJA: PLIKI
            Text("Odebrane pliki:", style = MaterialTheme.typography.titleMedium, color = AccentPurple)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(receivedFiles) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
                    ) {
                        Text(file.name, modifier = Modifier.padding(12.dp), color = Color.White)
                    }
                }
            }
        }
    }
}