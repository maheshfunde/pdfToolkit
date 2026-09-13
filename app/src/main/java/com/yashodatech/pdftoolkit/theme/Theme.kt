package com.yashodatech.pdftoolkit.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ════════════════════════════════════════════════════════════════════════════════
//  EDITORIAL INK THEME — muted ink & warm paper with a single vermilion stamp
// ════════════════════════════════════════════════════════════════════════════════

private val LightColorScheme = lightColorScheme(
    primary = ElectricSapphire,
    onPrimary = InkBone,
    primaryContainer = ElectricSapphire.copy(alpha = 0.14f),
    onPrimaryContainer = ElectricSapphire,

    secondary = InkMuted,
    onSecondary = InkBlack,
    secondaryContainer = InkSurfaceElevated,
    onSecondaryContainer = InkMuted,

    tertiary = CompressColor,
    onTertiary = InkBone,

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,

    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.5f),

    error = LightError,
    onError = InkBone,
    errorContainer = LightError.copy(alpha = 0.14f),
    onErrorContainer = LightError,

    inverseSurface = DarkSurface,
    inverseOnSurface = DarkOnSurface,
    inversePrimary = ElectricSapphire.copy(alpha = 0.8f),

    scrim = Color.Black.copy(alpha = 0.36f)
)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricSapphire,
    onPrimary = InkBone,
    primaryContainer = ElectricSapphire.copy(alpha = 0.2f),
    onPrimaryContainer = Vermilion,

    secondary = InkMuted,
    onSecondary = InkBlack,
    secondaryContainer = InkSurfaceElevated,
    onSecondaryContainer = InkMuted,

    tertiary = CompressColor,
    onTertiary = InkBone,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.5f),

    error = DarkError,
    onError = InkBone,
    errorContainer = DarkError.copy(alpha = 0.2f),
    onErrorContainer = DarkError,

    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    inversePrimary = ElectricSapphire,

    scrim = Color.Black.copy(alpha = 0.5f)
)

@Composable
fun PDFToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Enforce high-end iOS Red Gradient theme consistently across both Light and Dark mode settings on device
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode && view.context is Activity) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        // Keep status bar & navigation bar text/icons crisp light on the dark velvet background
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}