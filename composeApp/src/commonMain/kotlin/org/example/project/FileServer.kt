package org.example.project

import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.call
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

object FileServer {
    private var server: ApplicationEngine? = null
    private var watchJob: Job? = null

    private val _receivedFiles = MutableStateFlow<List<File>>(emptyList())
    val receivedFiles: StateFlow<List<File>> = _receivedFiles

    fun start(basePath: String, port: Int = 9999) {
        // 1. ZAWSZE stopuj przed startem, żeby móc zmienić port
        stop()

        println("DEBUG: Startuje serwer. Ścieżka: $basePath Port: $port")

        refreshFiles(basePath) // Pierwsze skanowanie
        startWatching(basePath) // Uruchomienie pętli (Job)

        try {
            server = embeddedServer(CIO, port = port, host = "0.0.0.0") {

                routing {
                    post("/upload") {
                        val multipart = call.receiveMultipart()
                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val originalName = part.originalFileName ?: "file_${System.currentTimeMillis()}"
                                val destinationFile = File(basePath, originalName)
                                destinationFile.parentFile?.mkdirs()

                                part.streamProvider().use { input ->
                                    destinationFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                // Odśwież listę zaraz po odebraniu pliku
                                refreshFiles(basePath)
                            }
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK, "Pomyślnie odebrano")
                    }
                }
            }.start(wait = false)
            println("Serwer uruchomiony na porcie: $port")
        } catch (e: Exception) {
            println("Błąd startu serwera: ${e.message}")
            server = null
        }
    }

    fun startWatching(path: String) {
        watchJob?.cancel()
        // Używamy GlobalScope lub podanego CoroutineScope, Dispatchers.IO jest kluczowe
        watchJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) { //isActive z kotlinx.coroutines
                refreshFiles(path)
                delay(3000)
            }
        }
    }

    fun refreshFiles(path: String) {
        try {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()?.filter { it.isFile } ?: emptyList()
                _receivedFiles.value = files.sortedByDescending { it.lastModified() }
            }
        } catch (e: Exception) {
            println("Błąd odświeżania: ${e.message}")
        }
    }

    fun initInitialList(path: String) {
        refreshFiles(path)
    }

    fun stop() {
        watchJob?.cancel()
        watchJob = null
        server?.stop(500, 1000)
        server = null
        println("Server and watcher stopped")
    }
}