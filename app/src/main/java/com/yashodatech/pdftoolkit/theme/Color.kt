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

// ═══════════════════════════════════════════════════════════
//  DESIGN TOKENS FROM SPEC (MOCKUP PALETTE)
// ═══════════════════════════════════════════════════════════
// Recommended Dark Color Palette (No Blue)
val DarkBackground = Color(0xFF0D0E10)
val DarkPrimarySurface = Color(0xFF16181C)
val DarkElevatedSurface = Color(0xFF1D2025)
val DarkHighlightSurface = Color(0xFF24272D)
val DarkBorder = Color(0xFF2B2F36)
val DarkTextPrimary = Color(0xFFF9FAFB)      // Crisp white
val DarkTextSecondary = Color(0xFFE5E7EB)    // Dark white (neutral light silver-gray, NO blue)
val DarkTextMuted = Color(0xFFA1A1AA)        // Neutral light-medium gray (NO blue)

// Accent Colors (No Blue)
val AccentPrimaryScan = Color(0xFFEF4444)       // Primary / Scan (#EF4444 app red)
val AccentConvertCreate = Color(0xFF34D399)     // Convert / Create (#34D399)
val AccentCompressOptimize = Color(0xFFFBBF24)  // Compress / Optimize (#FBBF24)
val AccentSecurityUnlock = Color(0xFFF87171)    // Security / Unlock (#F87171)
val AccentOrganizeEdit = Color(0xFFF472B6)      // Organize / Edit (#F472B6)
val AccentSuccess = Color(0xFF34D399)
val AccentWarning = Color(0xFFF59E0B)

val PrimaryIndigo = AccentPrimaryScan

// Semantic Aliases
val SemanticSuccess = AccentConvertCreate
val SemanticWarning = AccentCompressOptimize
val SemanticDanger = AccentSecurityUnlock
val SemanticInfo = AccentPrimaryScan
val SemanticSecurity = AccentSecurityUnlock

// Light Theme (White + Red symbols like icon of the app)
val LightBackground = Color(0xFFF8F9FA)
val LightPrimarySurface = Color(0xFFFFFFFF)    // Pure White components
val LightElevatedSurface = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE5E7EB)
val LightTextPrimary = Color(0xFF111827)
val LightTextSecondary = Color(0xFF6B7280)
val LightTextMuted = Color(0xFF9CA3AF)
val LightAccentRed = Color(0xFFDC2626)         // App icon red
val LightAccentRedTint = Color(0xFFFEE2E2)     // 10% red tint

val NeutralCanvasLight = LightBackground
val NeutralCardLight = LightPrimarySurface
val NeutralCardBorderLight = LightBorder
val NeutralTextPrimaryLight = LightTextPrimary
val NeutralTextSecondaryLight = LightTextSecondary

val NeutralCanvasDark = DarkBackground
val NeutralCardDark = DarkPrimarySurface
val NeutralCardBorderDark = DarkBorder
val NeutralTextPrimaryDark = DarkTextPrimary
val NeutralTextSecondaryDark = DarkTextSecondary

// Tool Accent Mapping
val ToolScanAccent = AccentPrimaryScan
val ToolConvertAccent = AccentConvertCreate
val ToolOrganizeAccent = AccentOrganizeEdit
val ToolCompressAccent = AccentCompressOptimize
val ToolSecurityAccent = AccentSecurityUnlock
val ToolReadAccent = AccentPrimaryScan

// ── Surfaces (paper tiering) ───────────────────────────────
val PrecisionBackground = LightBackground
val PrecisionOnBackground = LightTextPrimary
val PrecisionSurface = LightPrimarySurface          // Pure White
val PrecisionOnSurface = LightTextPrimary           // Black text #111827
val PrecisionSurfaceVariant = Color(0xFFF3F4F6)
val PrecisionOnSurfaceVariant = LightTextSecondary
val PrecisionSurfaceDim = Color(0xFFE5E7EB)
val PrecisionSurfaceBright = LightPrimarySurface

val PrecisionSurfaceLowest = LightPrimarySurface    // #FFFFFF Pure White cards
val PrecisionSurfaceLow = LightPrimarySurface       // #FFFFFF Pure White
val PrecisionSurfaceContainer = LightPrimarySurface // #FFFFFF Pure White
val PrecisionSurfaceHigh = Color(0xFFF3F4F6)
val PrecisionSurfaceHighest = Color(0xFFE5E7EB)

val PrecisionInverseSurface = Color(0xFF111827)
val PrecisionInverseOnSurface = Color(0xFFF9FAFB)

// ── Lines & outlines ───────────────────────────────────────
val PrecisionOutline = Color(0xFF9CA3AF)
val PrecisionOutlineVariant = NeutralCardBorderLight

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
val LightOnBackground = PrecisionOnBackground
val LightSurface = PrecisionSurface
val LightOnSurface = PrecisionOnSurface
val LightSurfaceVariant = PrecisionSurfaceVariant
val LightOnSurfaceVariant = PrecisionOnSurfaceVariant
val LightOutline = PrecisionOutline
val LightError = PrecisionError

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