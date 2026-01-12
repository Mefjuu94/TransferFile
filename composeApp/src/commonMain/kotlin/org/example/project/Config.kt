package org.example.project

object AppConfig {
    const val SERVER_PORT = 8080
    const val SERVICE_TYPE = "_filetransfer._tcp.local."
    const val CHUNK_SIZE = 1024 * 1024 // 1MB bufor dla stabilności
}