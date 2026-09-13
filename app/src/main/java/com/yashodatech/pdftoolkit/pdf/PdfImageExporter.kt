package com.yashodatech.pdftoolkit.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File

enum class ExportFormat(val ext: String, val mime: String) {
    JPG("jpg", "image/jpeg"),
    PNG("png", "image/png")
}

/**
 * Renders every page of a PDF to individual image files (JPG / PNG) in the
 * Pictures/PDF Toolkit folder, using Android's native [PdfRenderer] — no extra
 * dependency required.
 */
class PdfImageExporter(private val context: Context) {

    /**
     * Exports the given [pageIndices] (0-based) of [sourceUri] one image per page
     * at native page resolution. Returns the output URIs (as strings) in the
     * order of [pageIndices]. Pass all indices to export the whole document.
     */
    fun exportPages(
        sourceUri: Uri,
        baseName: String,
        pageIndices: List<Int>,
        format: ExportFormat,
        quality: Int // 0..100, JPG only
    ): List<String> {
        if (pageIndices.isEmpty()) throw Exception("No pages selected")

        // Copy to a temp file (PdfRenderer wants a file descriptor).
        val file = File.createTempFile("export_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }

        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val results = mutableListOf<String>()
        val cleanBase = baseName.removeSuffix(".pdf")
            .substringBeforeLast(".")
            .ifBlank { "export" }

        try {
            for (index in pageIndices) {
                if (index < 0 || index >= renderer.pageCount) continue
                val page = renderer.openPage(index)
                val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val name = "${cleanBase}_${index + 1}.${format.ext}"
                results.add(writeImage(bmp, name, format, quality))
                bmp.recycle()
            }
        } finally {
            renderer.close()
            fd.close()
            file.delete()
        }
        return results
    }

    private fun writeImage(
        bmp: Bitmap,
        name: String,
        format: ExportFormat,
        quality: Int
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/PDF Toolkit"
            )
        }
        val uri = resolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
        ) ?: throw Exception("Failed to create image file")

        resolver.openOutputStream(uri)?.use { out ->
            val compressFormat = if (format == ExportFormat.JPG)
                Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
            val ok = bmp.compress(compressFormat, quality, out)
            if (!ok) throw Exception("Failed to write image")
        } ?: throw Exception("Cannot open output stream")

        return uri.toString()
    }

    /**
     * Renders every page of [sourceUri] as a small preview [Bitmap]. Caller owns the
     * bitmaps and must recycle() them when done. Used for the page-selection strip.
     */
    fun renderThumbnails(sourceUri: Uri, maxEdge: Int = 160): List<Bitmap> {
        val file = File.createTempFile("thumbs_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val result = mutableListOf<Bitmap>()
        val base = Matrix()
        try {
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val longest = maxOf(page.width, page.height)
                val scale = maxEdge / longest.toFloat()
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                base.setScale(scale, scale)
                page.render(bmp, null, base, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                result.add(bmp)
            }
        } finally {
            renderer.close()
            fd.close()
            file.delete()
        }
        return result
    }

    /** Quickly checks a PDF is readable before export starts. */
    fun validate(sourceUri: Uri): Int {
        val file = File.createTempFile("validate_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val count = renderer.pageCount
        renderer.close()
        fd.close()
        file.delete()
        return count
    }
}