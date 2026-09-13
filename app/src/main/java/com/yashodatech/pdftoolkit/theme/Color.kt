package com.yashodatech.pdftoolkit.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════
//  EDITORIAL INK DESIGN SYSTEM
//  Muted ink, warm paper, one vermilion stamp.
// ═══════════════════════════════════════════════════════════

// ── Core Editorial Ink palette ──────────────────────────────
val InkBlack = Color(0xFF151210)               // main background — warm ink, not pure black
val InkSurface = Color(0xFF1F1C19)             // raised cards
val InkSurfaceElevated = Color(0xFF26221E)     // raised / pressed
val InkSurfacePressed = Color(0xFF2C2620)
val InkBorder = Color(0xFF3A342D)              // hairlines / rules
val InkBorderFaint = Color(0xFF2A2621)
val InkBone = Color(0xFFF2EEE7)                // primary text — warm paper bone, not pure white
val InkMuted = Color(0xFFA49D92)               // secondary text
val InkFaint = Color(0xFF6E675C)               // tertiary / disabled text
val Vermilion = Color(0xFFE4572E)              // the one signature stamp
val VermilionDeep = Color(0xFFB23F1F)          // pressed / deep state

// ── Semantic status colors (kept distinct for accessibility) ──
val SuccessGreen = Color(0xFF5CB87A)
val InfoBlue = Color(0xFF4F88C6)
val WarningAmber = Color(0xFFD9A52E)
val ErrorRed = Color(0xFFE5484D)

// ── Theme aliases (for backward compatibility across all screens) ──
// Mapped onto the Editorial Ink system so the app compiles with the
// same names while rendering the new identity.
val ElectricSapphire = Vermilion
val BrightSapphire = VermilionDeep
val DeepCobalt = InkSurfaceElevated
val SapphireDark = VermilionDeep
val PrimaryPurple = Vermilion
val SecondaryPurple = Vermilion
val CompressColor = Vermilion

// ── Ink warm-paper theme state (identical for dark & light on device) ──
val LightBackground = InkBlack          // ink background
val LightOnBackground = InkBone
val LightSurface = InkSurface
val LightOnSurface = InkBone
val LightSurfaceVariant = InkSurfaceElevated
val LightOnSurfaceVariant = InkMuted
val LightOutline = InkBorder
val LightError = ErrorRed

val DarkBackground = InkBlack
val DarkOnBackground = InkBone
val DarkSurface = InkSurface
val DarkOnSurface = InkBone
val DarkSurfaceVariant = InkSurfaceElevated
val DarkOnSurfaceVariant = InkMuted
val DarkOutline = InkBorder
val DarkError = ErrorRed

// ── Surface & border helpers ─────────────────────────────────
val MidnightSlate = InkBlack
val DeepSlate = InkSurface
val SlateCardBg = InkSurface
val SlateBorder = InkBorder
val SlateBorderLight = InkBorderFaint

// ── Accent status aliases ────────────────────────────────────
val ErrorRedStatus = Color(0xFFE5484D)

// ── Editorial Ink gradients (subtle, tonal — no chromatic wash) ──
val ModernHeaderGradient = listOf(Color(0xFF1A1612), Color(0xFF221D18), Color(0xFF27211B), Color(0xFF2B241D))
val GlassHeaderGradient = listOf(Color(0xFF191512), Color(0xFF201B16))
val HeroBannerGradient = listOf(InkSurface, InkSurfaceElevated)
val SapphireCardGradient = listOf(InkSurface, InkBlack)
val ButtonSapphireGradient = listOf(Vermilion, VermilionDeep)
val CardHaloSapphireGradient = listOf(Vermilion.copy(alpha = 0.10f), Color.Transparent)

// Subtle per-tool category accents (restrained, monochrome-leaning)
// Kept as tonal ink so any remaining usage reads calm, not neon.
val GradientImageToPdfVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientWordToPdfVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientMergeVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientSplitVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientCompressVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientViewVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientOrganizeVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientWatermarkVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientExportImagesVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientWordVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientOcrVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))
val GradientUnlockVibrant = listOf(Color(0xFF3A322B), Color(0xFF2B2621))

// ── Legacy gloss aliases ─────────────────────────────────────
val GlossDarkBg = InkBlack
val GlossSurfaceLight = Color(0x14FFFFFF)     // subtle 8% white sheen, not glassy
val GlossSurfaceBorder = InkBorder
val GlossActiveIndigo = Vermilion