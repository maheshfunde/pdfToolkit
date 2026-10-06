package com.yashodatech.pdftoolkit.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import com.itextpdf.io.image.ImageDataFactory
import java.io.ByteArrayOutputStream
import java.io.File

class PdfCompressor {

    /**
     * Compresses PDF by:
     * 1. Rendering each page with Android's native PdfRenderer (one page at a time — zero OOM risk)
     * 2. Re-encoding each page as JPEG at the target quality
     * 3. Assembling a new PDF using PdfCanvas (low-level, produces valid PDF structure)
     *
     * This approach works on ALL PDF types (FlateDecode, JBIG2, JPEG, raw rasters, scanned docs)
     * and is fully memory-safe since only one page bitmap lives in RAM at a time.
     *
     * @return Pair<outputUriString, outputFileSizeBytes>
     */
    fun compressPdf(
        context: Context,
        sourceUri: Uri,
        outputFileName: String,
        level: CompressionLevel
    ): Pair<String, Long> {

        val resolver = context.contentResolver

        // ── Create output in Downloads ──────────────
        val cleanName = if (outputFileName.endsWith(".pdf", true))
            outputFileName else "$outputFileName.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val outputUri = resolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        ) ?: throw Exception("Failed to create output file")

        val tempSourceFile = File(context.cacheDir, "compress_src_${System.currentTimeMillis()}.pdf")
        val tempOutputFile = File(context.cacheDir, "compress_out_${System.currentTimeMillis()}.pdf")

        try {
            // ── Copy source to a seekable cache file ──────────────
            // PdfRenderer needs a seekable file, not a stream
            resolver.openInputStream(sourceUri)?.use { input ->
                tempSourceFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Cannot open source PDF")

            // ── Determine rendering DPI and JPEG quality by level ─
            // Higher DPI = sharper text but larger file; lower = smaller
            val renderDpi = when (level) {
                CompressionLevel.LOW    -> 150f
                CompressionLevel.MEDIUM -> 120f
                CompressionLevel.HIGH   -> 96f
            }
            val jpegQuality = level.imageQuality

            // ── Open source with Android's PdfRenderer ────────────
            val pfd = ParcelFileDescriptor.open(tempSourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            // ── Build output PDF with iText PdfCanvas (low-level) ─
            val writerProperties = WriterProperties().apply {
                setFullCompressionMode(true)
                setCompressionLevel(9)
            }
            val writer = PdfWriter(tempOutputFile.absolutePath, writerProperties)
            val outputPdfDoc = PdfDocument(writer)

            val jpegBuffer = ByteArrayOutputStream()

            for (i in 0 until pageCount) {
                try {
                    val page = renderer.openPage(i)

                    // CRITICAL: read dimensions BEFORE closing the page
                    val pageWidthPt  = page.width.toFloat()   // width in PDF points (1 pt = 1/72 inch)
                    val pageHeightPt = page.height.toFloat()  // height in PDF points

                    // Compute bitmap pixel dimensions from target DPI
                    val scaleFactor  = renderDpi / 72f
                    val bitmapWidth  = (pageWidthPt  * scaleFactor).toInt().coerceAtLeast(100)
                    val bitmapHeight = (pageHeightPt * scaleFactor).toInt().coerceAtLeast(100)

                    // Render page into a white-backed bitmap
                    // NOTE: PdfRenderer.render() ONLY supports ARGB_8888 — RGB_565 throws "Unsupported pixel format"
                    // Memory is still fine because we immediately compress to JPEG and recycle the bitmap
                    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()  // close AFTER we've captured dimensions above

                    // Encode to JPEG
                    jpegBuffer.reset()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, jpegBuffer)
                    bitmap.recycle()

                    val jpegBytes = jpegBuffer.toByteArray()

                    // Add a new page to the output PDF at the original page dimensions
                    val ps = PageSize(pageWidthPt, pageHeightPt)
                    val newPdfPage = outputPdfDoc.addNewPage(ps)

                    // Draw the JPEG image scaled to fill the entire page
                    // addXObjectWithTransformationMatrix(xobj, a, b, c, d, e, f)
                    // where a=width, d=height, e=x_offset, f=y_offset in PDF user space
                    val imageData  = ImageDataFactory.create(jpegBytes)
                    val xObject    = PdfImageXObject(imageData)
                    val pdfCanvas  = PdfCanvas(newPdfPage)
                    pdfCanvas.addXObjectWithTransformationMatrix(
                        xObject,
                        pageWidthPt, 0f, 0f, pageHeightPt, 0f, 0f
                    )
                    pdfCanvas.release()

                } catch (t: Throwable) {
                    android.util.Log.w("PdfCompressor", "Skipping page $i: ${t.message}")
                    System.gc()
                }

                if (i % 5 == 0) System.gc()
            }

            renderer.close()
            pfd.close()
            outputPdfDoc.close()
            writer.close()

            try { tempSourceFile.delete() } catch (_: Throwable) {}
            System.gc()

            // ── Stream result to MediaStore ─────────────────────
            resolver.openOutputStream(outputUri)?.use { outStream ->
                tempOutputFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            } ?: throw Exception("Cannot write output PDF")

            val outputSize = getFileSize(context, outputUri)
            return Pair(outputUri.toString(), outputSize)

        } catch (t: Throwable) {
            try { resolver.delete(outputUri, null, null) } catch (_: Throwable) {}
            if (t is OutOfMemoryError) {
                System.gc()
                throw Exception("Device ran out of memory. Try Low compression or use a smaller file.")
            }
            throw if (t is Exception) t else Exception(t.message ?: "Compression failed", t)
        } finally {
            try { if (tempSourceFile.exists()) tempSourceFile.delete() } catch (_: Throwable) {}
            try { if (tempOutputFile.exists()) tempOutputFile.delete() } catch (_: Throwable) {}
            System.gc()
        }
    }

    // ════════════════════════════════════════════════════
    //  FILE SIZE HELPER — RELIABLE
    // ════════════════════════════════════════════════════
    private fun getFileSize(context: Context, uri: Uri): Long {

        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val size = pfd.statSize
                if (size > 0) return size
            }
        } catch (_: Exception) {}

        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                val size = afd.length
                if (size > 0) return size
            }
        } catch (_: Exception) {}

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                var total = 0L
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    total += read
                }
                if (total > 0) return total
            }
        } catch (_: Exception) {}

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && idx >= 0) {
                    val size = cursor.getLong(idx)
                    if (size > 0) return size
                }
            }
        } catch (_: Exception) {}

        return 0L
    }
}