package com.yashodatech.pdftoolkit.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.yashodatech.pdftoolkit.ScanMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class DocumentProcessor {

    // ═══════════════════════════════════════════
    // Main Enhancement Function
    // ═══════════════════════════════════════════
    fun enhance(bitmap: Bitmap, mode: ScanMode): Bitmap {
        return when (mode) {
            ScanMode.ORIGINAL -> bitmap.copy(Bitmap.Config.ARGB_8888, true)
            ScanMode.SCAN -> applyScanMode(bitmap)
            ScanMode.GRAYSCALE -> applyGrayscale(bitmap)
            ScanMode.BLACK_WHITE -> applyBlackWhite(bitmap)
            ScanMode.LIGHTEN -> applyLighten(bitmap)
            ScanMode.DARKEN -> applyDarken(bitmap)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ SCAN MODE — Makes documents look like professional scans
    //
    // This uses adaptive background detection and enhancement:
    // 1. Detect the background color (usually white/light)
    // 2. Push background towards pure white
    // 3. Enhance text darkness for readability
    // 4. Apply local adaptive contrast
    // 5. Sharpen edges slightly
    // ═══════════════════════════════════════════════════════════
    private fun applyScanMode(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // ── Step 1: Convert to grayscale values & analyze ──
        val gray = IntArray(pixels.size)
        var totalBrightness = 0L

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            // Luminance-based grayscale
            val grayVal = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt().coerceIn(0, 255)
            gray[i] = grayVal
            totalBrightness += grayVal
        }

        val avgBrightness = (totalBrightness / pixels.size).toInt()

        // ── Step 2: Build histogram ──
        val histogram = IntArray(256)
        for (g in gray) histogram[g]++

        // ── Step 3: Find background threshold using Otsu's method ──
        val backgroundThreshold = otsuThreshold(histogram, pixels.size)

        // ── Step 4: Adaptive enhancement ──
        // Determine if this is a light document or dark image
        val isLightDocument = avgBrightness > 120

        // ── Step 5: Process each pixel ──
        val result = IntArray(pixels.size)

        if (isLightDocument) {
            // Document mode: white background, dark text
            processDocument(pixels, gray, result, width, height, backgroundThreshold)
        } else {
            // Dark image: enhance contrast without washing out
            processDarkContent(pixels, gray, result, width, height)
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(result, 0, width, 0, 0, width, height)

        // ── Step 6: Apply subtle sharpening ──
        return applySharpen(output, 0.3f)
    }

    // ═══════════════════════════════════════════
    // ✅ Process document (light background)
    // ═══════════════════════════════════════════
    private fun processDocument(
        pixels: IntArray,
        gray: IntArray,
        result: IntArray,
        width: Int,
        height: Int,
        bgThreshold: Int
    ) {
        // ── Adaptive block processing ──
        // Process in blocks to handle uneven lighting (shadows, etc.)
        val blockSize = 32  // Block size for local analysis
        val blocksX = (width + blockSize - 1) / blockSize
        val blocksY = (height + blockSize - 1) / blockSize

        // Calculate local average brightness per block
        val blockAvg = Array(blocksY) { IntArray(blocksX) }

        for (by in 0 until blocksY) {
            for (bx in 0 until blocksX) {
                var sum = 0L
                var count = 0
                val startX = bx * blockSize
                val startY = by * blockSize
                val endX = min(startX + blockSize, width)
                val endY = min(startY + blockSize, height)

                for (y in startY until endY) {
                    for (x in startX until endX) {
                        sum += gray[y * width + x]
                        count++
                    }
                }
                blockAvg[by][bx] = if (count > 0) (sum / count).toInt() else 128
            }
        }

        // ── Process each pixel ──
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val pixel = pixels[i]
                val grayVal = gray[i]

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Get local block average (with interpolation for smoothness)
                val bx = (x / blockSize).coerceIn(0, blocksX - 1)
                val by2 = (y / blockSize).coerceIn(0, blocksY - 1)
                val localAvg = getInterpolatedBlockAvg(
                    blockAvg, x, y, blockSize, blocksX, blocksY
                )

                // ── Determine if pixel is text or background ──
                // Text detection: significantly darker than local background
                val localThreshold = (localAvg * 0.65f).toInt()
                val isText = grayVal < localThreshold
                val isDarkText = grayVal < localAvg * 0.5f

                if (isText) {
                    // ── TEXT PIXEL: Darken for readability ──
                    val darkenFactor = if (isDarkText) {
                        // Already dark text → make it darker/blacker
                        val ratio = grayVal.toFloat() / max(localAvg, 1)
                        // Map ratio to darkening: darker input → more darkening
                        (ratio * 0.6f).coerceIn(0.1f, 0.7f)
                    } else {
                        // Light-ish text (gray) → moderate darkening
                        val ratio = grayVal.toFloat() / max(localAvg, 1)
                        (ratio * 0.75f).coerceIn(0.3f, 0.85f)
                    }

                    val newR = (r * darkenFactor).roundToInt().coerceIn(0, 255)
                    val newG = (g * darkenFactor).roundToInt().coerceIn(0, 255)
                    val newB = (b * darkenFactor).roundToInt().coerceIn(0, 255)

                    result[i] = Color.rgb(newR, newG, newB)

                } else {
                    // ── BACKGROUND PIXEL: Push towards white ──
                    val brightnessRatio = grayVal.toFloat() / 255f

                    // How much to whiten: brighter areas → more white
                    // This preserves slight color tints while cleaning background
                    val whitenAmount = when {
                        brightnessRatio > 0.85f -> 0.95f  // Very light → almost white
                        brightnessRatio > 0.70f -> 0.85f  // Light → mostly white
                        brightnessRatio > 0.55f -> 0.70f  // Medium → some whitening
                        else -> 0.50f                       // Darker bg → less whitening
                    }

                    val newR = lerp(r, 255, whitenAmount)
                    val newG = lerp(g, 255, whitenAmount)
                    val newB = lerp(b, 255, whitenAmount)

                    result[i] = Color.rgb(newR, newG, newB)
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    // Process dark content (non-document images)
    // ═══════════════════════════════════════════
    private fun processDarkContent(
        pixels: IntArray,
        gray: IntArray,
        result: IntArray,
        width: Int,
        height: Int
    ) {
        // For dark images: apply CLAHE-like local contrast enhancement
        // Find min/max for stretching
        var minGray = 255
        var maxGray = 0
        for (g in gray) {
            if (g < minGray) minGray = g
            if (g > maxGray) maxGray = g
        }

        val range = max(maxGray - minGray, 1)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            // Contrast stretch
            val factor = 255f / range
            val newR = ((r - minGray) * factor).roundToInt().coerceIn(0, 255)
            val newG = ((g - minGray) * factor).roundToInt().coerceIn(0, 255)
            val newB = ((b - minGray) * factor).roundToInt().coerceIn(0, 255)

            // Apply slight gamma correction to brighten shadows
            val gamma = 0.85f
            val gR = (255 * Math.pow(newR / 255.0, gamma.toDouble())).roundToInt().coerceIn(0, 255)
            val gG = (255 * Math.pow(newG / 255.0, gamma.toDouble())).roundToInt().coerceIn(0, 255)
            val gB = (255 * Math.pow(newB / 255.0, gamma.toDouble())).roundToInt().coerceIn(0, 255)

            result[i] = Color.rgb(gR, gG, gB)
        }
    }

    // ═══════════════════════════════════════════
    // ✅ Interpolated block average for smooth transitions
    // ═══════════════════════════════════════════
    private fun getInterpolatedBlockAvg(
        blockAvg: Array<IntArray>,
        x: Int, y: Int,
        blockSize: Int,
        blocksX: Int,
        blocksY: Int
    ): Int {
        val bxf = x.toFloat() / blockSize - 0.5f
        val byf = y.toFloat() / blockSize - 0.5f

        val bx0 = bxf.toInt().coerceIn(0, blocksX - 1)
        val by0 = byf.toInt().coerceIn(0, blocksY - 1)
        val bx1 = (bx0 + 1).coerceIn(0, blocksX - 1)
        val by1 = (by0 + 1).coerceIn(0, blocksY - 1)

        val fx = (bxf - bx0).coerceIn(0f, 1f)
        val fy = (byf - by0).coerceIn(0f, 1f)

        val v00 = blockAvg[by0][bx0].toFloat()
        val v10 = blockAvg[by0][bx1].toFloat()
        val v01 = blockAvg[by1][bx0].toFloat()
        val v11 = blockAvg[by1][bx1].toFloat()

        val top = v00 + (v10 - v00) * fx
        val bottom = v01 + (v11 - v01) * fx
        return (top + (bottom - top) * fy).roundToInt().coerceIn(0, 255)
    }

    // ═══════════════════════════════════════════
    // ✅ Otsu's threshold method
    // ═══════════════════════════════════════════
    private fun otsuThreshold(histogram: IntArray, totalPixels: Int): Int {
        var sumTotal = 0L
        for (i in 0..255) sumTotal += i.toLong() * histogram[i]

        var sumBg = 0L
        var weightBg = 0
        var maxVariance = 0.0
        var bestThreshold = 128

        for (t in 0..255) {
            weightBg += histogram[t]
            if (weightBg == 0) continue

            val weightFg = totalPixels - weightBg
            if (weightFg == 0) break

            sumBg += t.toLong() * histogram[t]
            val meanBg = sumBg.toDouble() / weightBg
            val meanFg = (sumTotal - sumBg).toDouble() / weightFg

            val variance = weightBg.toDouble() * weightFg * (meanBg - meanFg) * (meanBg - meanFg)
            if (variance > maxVariance) {
                maxVariance = variance
                bestThreshold = t
            }
        }

        return bestThreshold
    }

    // ═══════════════════════════════════════════
    // ✅ Subtle sharpening
    // ═══════════════════════════════════════════
    private fun applySharpen(bitmap: Bitmap, amount: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 3 || height < 3) return bitmap

        val src = IntArray(width * height)
        bitmap.getPixels(src, 0, width, 0, 0, width, height)
        val dst = src.copyOf()

        // Unsharp mask: sharpen = original + amount * (original - blurred)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x

                // Simple 3x3 blur for unsharp mask
                var sumR = 0; var sumG = 0; var sumB = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val ni = (y + dy) * width + (x + dx)
                        sumR += Color.red(src[ni])
                        sumG += Color.green(src[ni])
                        sumB += Color.blue(src[ni])
                    }
                }
                val blurR = sumR / 9
                val blurG = sumG / 9
                val blurB = sumB / 9

                val origR = Color.red(src[i])
                val origG = Color.green(src[i])
                val origB = Color.blue(src[i])

                // Unsharp mask
                val newR = (origR + amount * (origR - blurR)).roundToInt().coerceIn(0, 255)
                val newG = (origG + amount * (origG - blurG)).roundToInt().coerceIn(0, 255)
                val newB = (origB + amount * (origB - blurB)).roundToInt().coerceIn(0, 255)

                dst[i] = Color.rgb(newR, newG, newB)
            }
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(dst, 0, width, 0, 0, width, height)
        bitmap.recycle()
        return output
    }

    // ═══════════════════════════════════════════
    // Grayscale
    // ═══════════════════════════════════════════
    private fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    // ═══════════════════════════════════════════
    // ✅ Black & White (proper adaptive threshold)
    // ═══════════════════════════════════════════
    private fun applyBlackWhite(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = IntArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            gray[i] = (0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p))
                .roundToInt().coerceIn(0, 255)
        }

        // Adaptive threshold using local mean
        val blockSize = 15
        val c = 10  // Constant subtracted from mean

        val result = IntArray(pixels.size)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x

                // Calculate local mean in a window
                var sum = 0L
                var count = 0
                val halfBlock = blockSize / 2

                for (dy in -halfBlock..halfBlock) {
                    for (dx in -halfBlock..halfBlock) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        sum += gray[ny * width + nx]
                        count++
                    }
                }

                val localMean = (sum / count).toInt()
                val threshold = localMean - c

                result[i] = if (gray[i] < threshold) Color.BLACK else Color.WHITE
            }
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(result, 0, width, 0, 0, width, height)
        return output
    }

    // ═══════════════════════════════════════════
    // Lighten
    // ═══════════════════════════════════════════
    private fun applyLighten(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gamma = 0.7f  // < 1 brightens

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (255 * Math.pow(Color.red(p) / 255.0, gamma.toDouble())).roundToInt().coerceIn(0, 255)
            val g = (255 * Math.pow(Color.green(p) / 255.0, gamma.toDouble())).roundToInt().coerceIn(0, 255)
            val b = (255 * Math.pow(Color.blue(p) / 255.0, gamma.toDouble())).roundToInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    // ═══════════════════════════════════════════
    // Darken
    // ═══════════════════════════════════════════
    private fun applyDarken(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gamma = 1.5f  // > 1 darkens

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (255 * Math.pow(Color.red(p) / 255.0, gamma.toDouble())).roundToInt().coerceIn(0, 255)
            val g = (255 * Math.pow(Color.green(p) / 255.0, gamma.toDouble())).roundToInt().coerceIn(0, 255)
            val b = (255 * Math.pow(Color.blue(p) / 255.0, gamma.toDouble())).roundToInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    // ═══════════════════════════════════════════
    // Helper: Linear interpolation
    // ═══════════════════════════════════════════
    private fun lerp(a: Int, b: Int, t: Float): Int {
        return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
    }
}