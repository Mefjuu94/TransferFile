package org.example.project

import androidx.compose.runtime.mutableStateListOf
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

class DeviceDiscovery {
    val foundDevices = mutableStateListOf<Pair<String, String>>()
    private var isRunning = false
    private val port = 9876
    private var activeSocket: DatagramSocket? = null

    fun start(deviceName: String) {
        if (isRunning) return
        isRunning = true

        // WĄTEK NADAWANIA
        thread(isDaemon = true) {
            try {
                val sendSocket = DatagramSocket()
                sendSocket.broadcast = true
                while (isRunning) {
                    val message = "$deviceName|PING"
                    val bytes = message.toByteArray()
                    val packet = DatagramPacket(
                        bytes, bytes.size,
                        InetAddress.getByName("255.255.255.255"), port
                    )
                    sendSocket.send(packet)
                    Thread.sleep(3000)
                }
                sendSocket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // WĄTEK NASŁUCHIWANIA
        thread(isDaemon = true) {
            try {
                if (activeSocket == null || activeSocket?.isClosed == true) {
                    activeSocket = DatagramSocket(port)
                    val buffer = ByteArray(1024)

                    while (isRunning) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        activeSocket?.receive(packet)

                        val data = String(packet.data, 0, packet.length).split("|")
                        val senderIp = packet.address.hostAddress

                        if (data.isNotEmpty() && senderIp != null) {
                            val name = data[0]
                            if (foundDevices.none { it.second == senderIp }) {
                                foundDevices.add(name to senderIp)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Błąd nasłuchiwania (prawdopodobnie port zajęty): ${e.message}")
            } finally {
                activeSocket?.close()
                activeSocket = null
            }
        }
    }

    fun stop() {
        isRunning = false
        activeSocket?.close()
        activeSocket = null
    }
}