package com.yashodatech.pdftoolkit

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.ThemeMode
import com.yashodatech.pdftoolkit.theme.PDFToolkitTheme
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var incomingPdfUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {

        // ── Splash Screen ───────────────────────
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOut = android.animation.ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                android.view.View.ALPHA,
                1f,
                0f
            )
            fadeOut.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            fadeOut.duration = 220L
            fadeOut.doOnEnd { splashScreenViewProvider.remove() }
            fadeOut.start()
        }

        super.onCreate(savedInstanceState)

        // Capture incoming PDF URI if launched via ACTION_VIEW or ACTION_SEND
        incomingPdfUri = extractPdfUri(intent)
        if (incomingPdfUri != null) {
            android.util.Log.d("PDFToolkit", "onCreate set ActivePdfHolder: $incomingPdfUri")
            com.yashodatech.pdftoolkit.pdf.ActivePdfHolder.set(incomingPdfUri)
        }

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

            val route = intent?.getStringExtra("route")

            PDFToolkitTheme(darkTheme = effectiveDark) {
                AppNavigation(
                    initialRoute = route,
                    externalPdfUri = incomingPdfUri,
                    onPdfUriConsumed = { incomingPdfUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractPdfUri(intent)?.let { uri ->
            android.util.Log.d("PDFToolkit", "onNewIntent extracted PDF URI: $uri")
            com.yashodatech.pdftoolkit.pdf.ActivePdfHolder.set(uri)
            incomingPdfUri = uri
        }
    }

    private fun extractPdfUri(intent: Intent?): Uri? {
        if (intent == null) return null
        android.util.Log.d("PDFToolkit", "extractPdfUri: action=${intent.action}, data=${intent.data}, dataString=${intent.dataString}, type=${intent.type}")

        // 1. Direct intent data (standard for ACTION_VIEW)
        intent.data?.let { return it }

        // 2. Data string fallback
        if (!intent.dataString.isNullOrBlank()) {
            try {
                return Uri.parse(intent.dataString)
            } catch (_: Exception) {}
        }

        // 3. ClipData URI (used by some file managers & share sheets)
        intent.clipData?.let { clip ->
            if (clip.itemCount > 0) {
                val itemUri = clip.getItemAt(0)?.uri
                if (itemUri != null) return itemUri
            }
        }

        // 4. ACTION_SEND extra stream (shared from another app)
        if (intent.action == Intent.ACTION_SEND) {
            val extraStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            if (extraStream != null) return extraStream
        }

        // 5. ACTION_SEND_MULTIPLE (first PDF in stream list)
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }
            if (!list.isNullOrEmpty()) return list.first()
        }

        return null
    }
}