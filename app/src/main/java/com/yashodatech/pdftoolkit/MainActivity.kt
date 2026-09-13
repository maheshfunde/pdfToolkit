package com.yashodatech.pdftoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.yashodatech.pdftoolkit.theme.PDFToolkitTheme
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        // ── Splash Screen ───────────────────────
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)

            // Initialize Remote Config
            RemoteConfigManager.initialize()

            lifecycleScope.launch {
                try {
                    RemoteConfigManager.fetchAndActivate()
                    Config.SHOW_ADS = RemoteConfigManager.shouldShowAds()
                    
                    // ── Initialize AdMob ────────────────────
                    if (Config.SHOW_ADS) {
                        MobileAds.initialize(this@MainActivity) { }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // ── Edge to Edge ────────────────────────
        enableEdgeToEdge()

        // ── Set Content ─────────────────────────
        setContent {
            PDFToolkitTheme {
                AppNavigation()
            }
        }
    }
}