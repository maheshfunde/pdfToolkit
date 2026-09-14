package com.yashodatech.pdftoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.ThemeMode
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
            // Resolve the effective theme from the user's stored preference
            // (collected on onboarding, changeable in Settings). SYSTEM follows
            // the OS setting; LIGHT / DARK pin the scheme regardless.
            val context = LocalContext.current
            val prefsManager = remember { PreferencesManager(context) }
            val themeMode by prefsManager.themeMode.collectAsState(
                initial = ThemeMode.SYSTEM.key
            )
            val effectiveDark = when (themeMode) {
                ThemeMode.DARK.key -> true
                ThemeMode.LIGHT.key -> false
                else -> isSystemInDarkTheme()
            }

            PDFToolkitTheme(darkTheme = effectiveDark) {
                AppNavigation()
            }
        }
    }
}