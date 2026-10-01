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
//  PRECISION PDF ENGINE THEME — light-primary Material3, follows system dark mode.
//  Pure presentation layer: all tokens come from Color.kt / Type.kt.
// ════════════════════════════════════════════════════════════════════════════════

private val LightColorScheme = lightColorScheme(
    primary = PrecisionPrimary,
    onPrimary = PrecisionOnPrimary,
    primaryContainer = PrecisionPrimaryContainer,
    onPrimaryContainer = PrecisionOnPrimaryContainer,
    inversePrimary = PrecisionInversePrimary,
    surfaceTint = PrecisionPrimaryTint,

    secondary = PrecisionSecondary,
    onSecondary = PrecisionOnSecondary,
    secondaryContainer = PrecisionSecondaryContainer,
    onSecondaryContainer = PrecisionOnSecondaryContainer,

    tertiary = PrecisionTertiary,
    onTertiary = PrecisionOnTertiary,
    tertiaryContainer = PrecisionTertiaryContainer,
    onTertiaryContainer = PrecisionOnTertiaryContainer,

    background = PrecisionBackground,
    onBackground = PrecisionOnBackground,
    surface = PrecisionSurface,
    onSurface = PrecisionOnSurface,
    surfaceVariant = PrecisionSurfaceVariant,
    onSurfaceVariant = PrecisionOnSurfaceVariant,

    surfaceDim = PrecisionSurfaceDim,
    surfaceBright = PrecisionSurfaceBright,
    surfaceContainerLowest = PrecisionSurfaceLowest,
    surfaceContainerLow = PrecisionSurfaceLow,
    surfaceContainer = PrecisionSurfaceContainer,
    surfaceContainerHigh = PrecisionSurfaceHigh,
    surfaceContainerHighest = PrecisionSurfaceHighest,

    inverseSurface = PrecisionInverseSurface,
    inverseOnSurface = PrecisionInverseOnSurface,

    outline = PrecisionOutline,
    outlineVariant = PrecisionOutlineVariant,

    error = PrecisionError,
    onError = PrecisionOnError,
    errorContainer = PrecisionErrorContainer,
    onErrorContainer = PrecisionOnErrorContainer,

    scrim = Color.Black.copy(alpha = 0.36f)
)

// Dark variant: stepped dark surfaces with crisp edge borders, per the design's
// dark-mode adaptation note. Primary/secondary/tertiary keep their light hues so
// CTAs stay instantly recognizable.
private val DarkColorScheme = darkColorScheme(
    primary = PrecisionPrimary,
    onPrimary = PrecisionOnPrimary,
    primaryContainer = PrecisionPrimaryContainer,
    onPrimaryContainer = PrecisionOnPrimaryContainer,
    inversePrimary = PrecisionInversePrimary,
    surfaceTint = PrecisionPrimaryTint,

    secondary = Color(0xFFBEC6E0),
    onSecondary = Color(0xFF131B2E),
    secondaryContainer = Color(0xFF3F465C),
    onSecondaryContainer = Color(0xFFDAE2FD),

    tertiary = Color(0xFFC3C0FF),
    onTertiary = Color(0xFF0F0069),
    tertiaryContainer = Color(0xFF3323CC),
    onTertiaryContainer = Color(0xFFE2DFFF),

    background = NeutralCanvasDark,            // deep charcoal neutral canvas
    onBackground = NeutralTextPrimaryDark,
    surface = NeutralCanvasDark,
    onSurface = NeutralTextPrimaryDark,
    surfaceVariant = Color(0xFF26282E),
    onSurfaceVariant = NeutralTextSecondaryDark,

    surfaceDim = Color(0xFF0B0C0E),
    surfaceBright = Color(0xFF2C2F36),
    surfaceContainerLowest = NeutralCardDark,  // cards — crisp dark charcoal
    surfaceContainerLow = Color(0xFF16181B),
    surfaceContainer = Color(0xFF202327),
    surfaceContainerHigh = Color(0xFF282B31),    // tab strip / segmented control
    surfaceContainerHighest = Color(0xFF32363D),

    inverseSurface = NeutralTextPrimaryDark,
    inverseOnSurface = NeutralCanvasDark,

    outline = Color(0xFF6B7280),
    outlineVariant = NeutralCardBorderDark,

    error = Color(0xFFFFB4AC),
    onError = Color(0xFF410002),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    scrim = Color.Black.copy(alpha = 0.5f)
)

@Composable
fun PDFToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode && view.context is Activity) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        // Status/navigation bar icons follow the theme luminance.
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme

        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}