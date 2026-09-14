package com.yashodatech.pdftoolkit.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════
//  PRECISION PDF ENGINE DESIGN SYSTEM
//  Light-primary Material3 — Plus Jakarta Sans, tiered paper
//  surfaces, vermilion primary CTA, domain-accent categories.
// ═══════════════════════════════════════════════════════════

// ── Core Precision palette (source of truth) ───────────────
val PrecisionPrimary = Color(0xFFB7131A)        // vermilion core — high-intent CTAs
val PrecisionPrimaryContainer = Color(0xFFDB322F)
val PrecisionOnPrimary = Color(0xFFFFFFFF)
val PrecisionOnPrimaryContainer = Color(0xFFFFFBFF)
val PrecisionPrimaryTint = Color(0xFFBB171C)    // surface-tint
val PrecisionInversePrimary = Color(0xFFFFB4AC)

val PrecisionSecondary = Color(0xFF565E74)      // slate
val PrecisionOnSecondary = Color(0xFFFFFFFF)
val PrecisionSecondaryContainer = Color(0xFFDAE2FD)
val PrecisionOnSecondaryContainer = Color(0xFF5C647A)

val PrecisionTertiary = Color(0xFF4B41E1)       // indigo
val PrecisionOnTertiary = Color(0xFFFFFFFF)
val PrecisionTertiaryContainer = Color(0xFF645EFB)
val PrecisionOnTertiaryContainer = Color(0xFFFFFBFF)

// ── Surfaces (paper tiering) ───────────────────────────────
// Cool paper that mirrors the navy dark palette: a faintly-tinted canvas,
// pure-white cards that lift a clear step off it, and a cool slate container
// ramp — so light and dark read as the same design language.
val PrecisionBackground = Color(0xFFF5F7FC)
val PrecisionOnBackground = Color(0xFF0E1526)
val PrecisionSurface = Color(0xFFF5F7FC)
val PrecisionOnSurface = Color(0xFF0E1526)
val PrecisionSurfaceVariant = Color(0xFFDCE4F1)
val PrecisionOnSurfaceVariant = Color(0xFF545F73)
val PrecisionSurfaceDim = Color(0xFFE2E8F4)
val PrecisionSurfaceBright = Color(0xFFF5F7FC)

val PrecisionSurfaceLowest = Color(0xFFFFFFFF)  // cards, headers, docked bars
val PrecisionSurfaceLow = Color(0xFFEEF2F9)
val PrecisionSurfaceContainer = Color(0xFFE5EAF4)
val PrecisionSurfaceHigh = Color(0xFFDCE4F1)
val PrecisionSurfaceHighest = Color(0xFFCDD6E6)

val PrecisionInverseSurface = Color(0xFF1B2434)
val PrecisionInverseOnSurface = Color(0xFFE8EEF8)

// ── Lines & outlines ───────────────────────────────────────
val PrecisionOutline = Color(0xFF687390)
val PrecisionOutlineVariant = Color(0xFFDCE4F1)

// ── Error ────────────────────────────────────────────────────
val PrecisionError = Color(0xFFBA1A1A)
val PrecisionOnError = Color(0xFFFFFFFF)
val PrecisionErrorContainer = Color(0xFFFFDAD6)
val PrecisionOnErrorContainer = Color(0xFF93000A)

// ── Functional domain accents (category anchors) ─────────────
// Indigo  → Organize (Merge, Split, Reorder, Extract)
// Amber   → Convert (Image-to-PDF, Word, OCR, PDF→Images/Word)
// Emerald → Protect & Optimize (Compress, Watermark, Unlock)
val DomainIndigo = Color(0xFF4F46E5)
val DomainAmber = Color(0xFFD97706)
val DomainEmerald = Color(0xFF059669)

// ── Semantic status colors ───────────────────────────────────
val SuccessGreen = Color(0xFF059669)
val InfoBlue = Color(0xFF4F46E5)
val WarningAmber = Color(0xFFD97706)
val ErrorRed = Color(0xFFBA1A1A)

// ── Domain-tinted tool gradients ─────────────────────────────
// Soft accent-wash so each tile reads its domain without neon.
fun domainCardGradient(accent: Color): List<Color> = listOf(
    accent.copy(alpha = 0.14f),
    PrecisionSurfaceLowest.copy(alpha = 0.0f)
)

val GradientImageToPdfVibrant = domainCardGradient(DomainAmber)
val GradientWordToPdfVibrant = domainCardGradient(DomainAmber)
val GradientMergeVibrant = domainCardGradient(DomainIndigo)
val GradientSplitVibrant = domainCardGradient(DomainIndigo)
val GradientCompressVibrant = domainCardGradient(DomainEmerald)
val GradientOrganizeVibrant = domainCardGradient(DomainIndigo)
val GradientWatermarkVibrant = domainCardGradient(DomainEmerald)
val GradientExportImagesVibrant = domainCardGradient(DomainAmber)
val GradientWordVibrant = domainCardGradient(DomainAmber)
val GradientOcrVibrant = domainCardGradient(DomainAmber)
val GradientUnlockVibrant = domainCardGradient(DomainEmerald)
val GradientViewVibrant = domainCardGradient(PrecisionPrimary)

// ── Legacy gradient aliases (retire as screens are reskinned) ──
val ModernHeaderGradient = listOf(PrecisionSurfaceLowest, PrecisionSurfaceLow)
val GlassHeaderGradient = listOf(PrecisionSurfaceLowest, PrecisionSurfaceLow)
val HeroBannerGradient = listOf(PrecisionSurfaceLow, PrecisionSurfaceLowest)
val SapphireCardGradient = listOf(PrecisionOnSurface.copy(alpha = 0.06f), Color.Transparent)
val ButtonSapphireGradient = listOf(PrecisionPrimary, PrecisionPrimaryContainer)
val CardHaloSapphireGradient = listOf(PrecisionPrimary.copy(alpha = 0.10f), Color.Transparent)

// ═══════════════════════════════════════════════════════════
//  BACKWARD-COMPATIBLE ALIASES (Editorial Ink names → Precision)
//  Kept so the app compiles through the reskin; removed per-screen
//  as each screen stops referencing them.
// ═══════════════════════════════════════════════════════════
val InkBlack = PrecisionBackground          // base canvas
val InkSurface = PrecisionSurfaceLowest     // raised cards / headers
val InkSurfaceElevated = PrecisionSurfaceContainer
val InkSurfacePressed = PrecisionSurfaceHigh
val InkBorder = PrecisionOutlineVariant     // hairlines / rules
val InkBorderFaint = PrecisionOutlineVariant.copy(alpha = 0.5f)
// Ink = text tokens. These adapt to the active color scheme (light or dark) so
// ink text never turns invisible against a scheme surface in dark mode.
@Composable
fun InkBone() = MaterialTheme.colorScheme.onSurface          // primary text
@Composable
fun InkMuted() = MaterialTheme.colorScheme.onSurfaceVariant  // secondary text
@Composable
fun InkFaint() = MaterialTheme.colorScheme.outline           // tertiary / disabled text
val Vermilion = PrecisionPrimary            // the signature stamp
val VermilionDeep = PrecisionPrimaryContainer

// ── Theme aliases (Editorial Ink → Precision equivalents) ──
val ElectricSapphire = PrecisionPrimary
val BrightSapphire = PrecisionPrimaryContainer
val DeepCobalt = PrecisionSurfaceContainer
val SapphireDark = PrecisionPrimaryContainer
val PrimaryPurple = PrecisionPrimary
val SecondaryPurple = PrecisionSecondary
val CompressColor = PrecisionPrimary

// ── Light background aliases ───────────────────────────────
val LightBackground = PrecisionBackground
val LightOnBackground = PrecisionOnBackground
val LightSurface = PrecisionSurface
val LightOnSurface = PrecisionOnSurface
val LightSurfaceVariant = PrecisionSurfaceVariant
val LightOnSurfaceVariant = PrecisionOnSurfaceVariant
val LightOutline = PrecisionOutline
val LightError = PrecisionError

val DarkBackground = PrecisionSurfaceDim
val DarkOnBackground = PrecisionOnSurface
val DarkSurface = PrecisionSurfaceDim
val DarkOnSurface = PrecisionOnSurface
val DarkSurfaceVariant = PrecisionSurfaceVariant
val DarkOnSurfaceVariant = PrecisionOnSurfaceVariant
val DarkOutline = PrecisionOutline
val DarkError = PrecisionError

// ── Surface & border helpers ───────────────────────────────
val MidnightSlate = PrecisionBackground
val DeepSlate = PrecisionSurfaceLowest
val SlateCardBg = PrecisionSurfaceLowest
val SlateBorder = PrecisionOutlineVariant
val SlateBorderLight = PrecisionOutlineVariant.copy(alpha = 0.5f)

// ── Accent status alias ────────────────────────────────────
val ErrorRedStatus = PrecisionError

// ── Legacy gloss aliases ───────────────────────────────────
val GlossDarkBg = PrecisionBackground
val GlossSurfaceLight = Color(0x14FFFFFF)      // subtle 8% white sheen
val GlossSurfaceBorder = PrecisionOutlineVariant
val GlossActiveIndigo = PrecisionPrimary