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

class MainActivity : ComponentActivity() {
    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Uprawnienia (Zostawiamy, bo są niezbędne na Androidzie)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        // 2. Multicast Lock (Zostawiamy, żeby UDP działało przy wygaszonym ekranie)
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("FileTransferLock").apply {
            setReferenceCounted(true)
            acquire()
        }

        setContent {
            // WYWOŁANIE BEZ ARGUMENTÓW
            // App() sama zainicjuje FileServer i Discovery w swoim LaunchedEffect
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Czyścimy przy zamknięciu
        FileServer.stop()
        if (multicastLock?.isHeld == true) {
            multicastLock?.release()
        }
    }
}