package org.example.project

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.streams.*
import java.io.File // W KMP używamy odpowiedników okio lub expect/actual dla File

class FileTransferService(private val client: HttpClient) {

    suspend fun sendFile(targetIp: String, file: File) {
        client.submitFormWithBinaryData(
            url = "http://$targetIp:${AppConfig.SERVER_PORT}/upload",
            formData = formData {
                append("file", InputProvider(file.length()) {
                    file.inputStream().asInput() // Strumieniowanie z dysku
                }, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                })
            }
        )
    }
}