package com.yashodatech.pdftoolkit.pdf

enum class CompressionLevel(
    val label: String,
    val description: String,
    val reductionPercent: Int,
    val imageQuality: Int       // 0-100, higher = better quality
) {
    LOW(
        label = "Low Compression",
        description = "Lossless structure optimization",
        reductionPercent = 15,
        imageQuality = 95        // Nearly identical to original
    ),
    MEDIUM(
        label = "Medium Compression",
        description = "Balanced optimization",
        reductionPercent = 35,
        imageQuality = 80        // Very good quality
    ),
    HIGH(
        label = "High Compression",
        description = "Maximum size reduction",
        reductionPercent = 55,
        imageQuality = 60        // Good quality, smallest size
    )
}