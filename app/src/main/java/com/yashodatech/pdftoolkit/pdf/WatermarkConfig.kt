package com.yashodatech.pdftoolkit.pdf

/**
 * Configuration for the text watermark.
 *
 * [fontSize] is in PDF points (1 pt ≈ 1/72 in). [opacity] is 0f–1f.
 * [rotation] is in degrees — the classic DRAFT stamp uses -45°.
 */
data class WatermarkConfig(
    val text: String,
    val fontSize: Float = 60f,
    val colorArgb: Int = 0xFFFF4444.toInt(), // vermilion-like
    val opacity: Float = 0.25f,
    val rotation: Float = -45f
)