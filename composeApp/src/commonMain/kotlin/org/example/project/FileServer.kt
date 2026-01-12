package org.example.project

import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

object FileServer {
    private var server: ApplicationEngine? = null

    // Lista plików dostępna dla UI
    private val _receivedFiles = MutableStateFlow<List<File>>(emptyList())
    val receivedFiles: StateFlow<List<File>> = _receivedFiles

    fun start(basePath: String) {
        if (server != null) return

        // Na starcie przeskanuj folder, żeby pokazać stare pliki
        refreshFiles(basePath)

        try {
            server = embeddedServer(CIO, port = 9999, host = "0.0.0.0") {
                routing {
                    post("/upload") {
                        val multipart = call.receiveMultipart()
                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val fileName = part.originalFileName ?: "file_${System.currentTimeMillis()}"
                                val file = File(basePath, fileName)
                                file.parentFile?.mkdirs()

                                part.streamProvider().use { input ->
                                    file.outputStream().use { output -> input.copyTo(output) }
                                }

                                // KLUCZ: Po zapisaniu odśwież listę dla UI
                                refreshFiles(basePath)
                            }
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK, "OK")
                    }
                }
            }
            server?.start(wait = false)
        } catch (e: Exception) {
            server = null
        }
    }

    // Funkcja pomocnicza do czytania zawartości folderu
    private fun refreshFiles(path: String) {
        val folder = File(path)
        val files = folder.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        _receivedFiles.value = files
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
}