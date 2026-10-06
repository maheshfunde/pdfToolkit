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
    primary = LightAccentRed,
    onPrimary = Color.White,
    primaryContainer = LightAccentRedTint,
    onPrimaryContainer = LightAccentRed,
    inversePrimary = LightAccentRed,
    surfaceTint = LightAccentRed,

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
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = DarkHighlightSurface,
    onPrimaryContainer = DarkTextPrimary,
    inversePrimary = PrimaryIndigo,
    surfaceTint = PrimaryIndigo,

    secondary = DarkTextSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkElevatedSurface,
    onSecondaryContainer = DarkTextPrimary,

    tertiary = DarkTextSecondary,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkHighlightSurface,
    onTertiaryContainer = DarkTextPrimary,

    background = DarkBackground,               // #0D0E10
    onBackground = DarkTextPrimary,            // #F9FAFB
    surface = DarkBackground,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkElevatedSurface,      // #1D2025
    onSurfaceVariant = DarkTextSecondary,      // #E5E7EB (dark white)

    surfaceDim = DarkBackground,
    surfaceBright = DarkHighlightSurface,
    surfaceContainerLowest = DarkPrimarySurface, // #16181C (cards)
    surfaceContainerLow = DarkElevatedSurface,    // #1D2025
    surfaceContainer = DarkHighlightSurface,      // #24272D
    surfaceContainerHigh = DarkHighlightSurface,  // #24272D
    surfaceContainerHighest = DarkBorder,         // #2B2F36

    inverseSurface = DarkTextPrimary,
    inverseOnSurface = DarkBackground,

    outline = DarkTextMuted,                      // #A1A1AA
    outlineVariant = DarkBorder,                  // #2B2F36

    error = SemanticDanger,                       // #F87171
    onError = Color.White,
    errorContainer = Color(0xFF451A1A),
    onErrorContainer = SemanticDanger,

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