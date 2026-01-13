package org.example.project

import androidx.compose.runtime.mutableStateListOf
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

class DeviceDiscovery {
    // Teraz przechowujemy Trojkę: Nazwa, IP, Port serwera HTTP
    val foundDevices = mutableStateListOf<DeviceData>()
    private var isRunning = false
    private val discoveryPort = 9876 // Port do rozsyłania sygnału (UDP)
    private var activeSocket: DatagramSocket? = null

    data class DeviceData(val name: String, val ip: String, val serverPort: Int)

    fun start(deviceName: String, serverPort: Int) {
        if (isRunning) stop() // Najpierw czyścimy stare wątki
        isRunning = true

        // WĄTEK NADAWANIA (Informujemy innych o nas)
        thread(isDaemon = true) {
            var sendSocket: DatagramSocket? = null
            try {
                sendSocket = DatagramSocket()
                sendSocket.broadcast = true
                while (isRunning) {
                    // Format: Nazwa|PORT|PING
                    val message = "$deviceName|$serverPort|PING"
                    val bytes = message.toByteArray()
                    val packet = DatagramPacket(
                        bytes, bytes.size,
                        InetAddress.getByName("255.255.255.255"), discoveryPort
                    )
                    sendSocket.send(packet)
                    Thread.sleep(3000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                sendSocket?.close()
            }
        }

        // WĄTEK NASŁUCHIWANIA (Szukamy innych urządzeń)
        thread(isDaemon = true) {
            try {
                activeSocket = DatagramSocket(discoveryPort)
                val buffer = ByteArray(1024)

                while (isRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    activeSocket?.receive(packet)

                    val message = String(packet.data, 0, packet.length)
                    val data = message.split("|")
                    val senderIp = packet.address.hostAddress

                    // Sprawdzamy czy pakiet ma odpowiedni format (Nazwa, Port, PING)
                    if (data.size >= 2 && senderIp != null) {
                        val name = data[0]
                        val remoteServerPort = data[1].toIntOrNull() ?: 9999

                        // Unikamy duplikatów po IP
                        if (foundDevices.none { it.ip == senderIp }) {
                            foundDevices.add(DeviceData(name, senderIp, remoteServerPort))
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRunning) { // Logujemy błąd tylko jeśli nie zamykaliśmy celowo
                    println("Błąd nasłuchiwania: ${e.message}")
                }
            } finally {
                activeSocket?.close()
                activeSocket = null
            }
        }
    }

    fun stop() {
        isRunning = false
        activeSocket?.close() // To przerwie blokadę na activeSocket.receive()
        activeSocket = null
        foundDevices.clear() // Czyścimy listę przy stopie
    }
}