package org.example.project.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.streams.asInput
import java.io.File

class FileClient {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 24 * 60 * 60 * 1000L
            connectTimeoutMillis = 10000L
            socketTimeoutMillis = 24 * 60 * 60 * 1000L
        }
    }

    suspend fun uploadFile(ip: String, file: File, onProgress: (Float) -> Unit) {
        try {
            println("Rozpoczynam wysyłanie do: http://$ip:9999/upload")

            val response: io.ktor.client.statement.HttpResponse = client.post("http://$ip:9999/upload") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "file",
                                InputProvider(file.length()) { file.inputStream().asInput() },
                                Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                                }
                            )
                        }
                    )
                )

                onUpload { bytesSent, totalBytes ->
                    if (totalBytes != null && totalBytes > 0L) {
                        onProgress(bytesSent.toFloat() / totalBytes)
                    }
                }
            }

            println("Serwer odpowiedział: ${response.status}")

        } catch (e: Exception) {
            println("BŁĄD Klienta: ${e.message}")
            e.printStackTrace()
        }
    }
}