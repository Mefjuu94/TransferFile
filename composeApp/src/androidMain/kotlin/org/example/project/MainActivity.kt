package org.example.project

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var multicastLock: WifiManager.MulticastLock? = null
    // USUNIĘTO: private val server = FileServer() -> bo FileServer to teraz object
    private val discovery = DeviceDiscovery()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Uprawnienia
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        // 2. Start Serwera (Używamy nazwy obiektu FileServer)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        lifecycleScope.launch(Dispatchers.IO) {
            // Wywołujemy bezpośrednio na obiekcie
            FileServer.start(downloadsDir)
        }

        // 3. Multicast Lock
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("FileTransferLock").apply {
            setReferenceCounted(true)
            acquire()
        }

        // 4. Start Discovery
        discovery.start(Build.MODEL)

        setContent {
            App(discovery = discovery)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // POPRAWKA: Wywołujemy stop() na obiekcie FileServer
        FileServer.stop()
        discovery.stop()
        if (multicastLock?.isHeld == true) {
            multicastLock?.release()
        }
    }
}