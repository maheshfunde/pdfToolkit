package com.yashodatech.pdftoolkit.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import java.io.File
import kotlin.math.roundToInt

/**
 * One page in the output document of the Page Organizer.
 *
 * [sourcePage] is the 1-based page number in the original PDF; the item may be
 * repeated (duplicate) or omitted (delete) and carries its own rotation.
 */
data class PageInfo(
    val sourcePage: Int,
    val rotation: Int = 0 // clockwise degrees: 0, 90, 180, 270
)

/**
 * Page-level operations for the Page Organizer tool.
 *
 * Thumbnails are rendered with Android's native [PdfRenderer] (no extra
 * dependency, works on a temp copy of the picked file). Reorder / rotate /
 * delete / duplicate / extract is done with iText 7, which is already bundled.
 */
class PdfPageManager(private val context: Context) {

    private var renderer: PdfRenderer? = null
    private var tempFile: File? = null

    /** Opens the source PDF and returns its page count. Call [close] when done. */
    fun load(uri: Uri): Int {
        close()
        val file = File.createTempFile("organize_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        tempFile = file
        val r = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        renderer = r
        return r.pageCount
    }

    fun getPageCount(): Int = renderer?.pageCount ?: 0

    /** Renders 0-based [index] to a bitmap [width]px wide, then applies [rotationDeg]. */
    fun renderPage(index: Int, width: Int, rotationDeg: Int = 0): Bitmap? {
        val r = renderer ?: return null
        val page = r.openPage(index)
        val scale = width.toFloat() / page.width
        val w = width
        val h = (page.height * scale).roundToInt().coerceAtLeast(1)

        var bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        val canvas = Canvas(bmp)
        canvas.scale(scale, scale)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        if (rotationDeg % 360 != 0) {
            val rotated = rotateBitmap(bmp, rotationDeg)
            if (rotated !== bmp) bmp.recycle()
            bmp = rotated
        }
        return bmp
    }

    private fun rotateBitmap(src: Bitmap, deg: Int): Bitmap {
        val m = Matrix().apply { postRotate(deg.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    fun close() {
        try { renderer?.close() } catch (_: Exception) {}
        renderer = null
        try { tempFile?.delete() } catch (_: Exception) {}
        tempFile = null
    }

    /**
     * Writes a new PDF containing [pages] in order (repeat to duplicate, omit
     * to delete) with each page's rotation applied. Returns the output URI as a
     * string.
     */
    fun reorderAndSave(
        sourceUri: Uri,
        pages: List<PageInfo>,
        outputFileName: String
    ): String {
        if (pages.isEmpty()) throw Exception("No pages to save")

        val resolver = context.contentResolver
        val cleanName = if (outputFileName.endsWith(".pdf", true))
            outputFileName else "$outputFileName.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val outputUri = resolver.insert(
            MediaStore.Files.getContentUri("external"), contentValues
        ) ?: throw Exception("Failed to create output file")

        resolver.openInputStream(sourceUri)?.use { input ->
            resolver.openOutputStream(outputUri)?.use { output ->
                val reader = PdfReader(input).apply { setUnethicalReading(true) }
                val src = PdfDocument(reader)
                val writer = PdfWriter(output)
                val out = PdfDocument(writer)

                for (p in pages) {
                    val n = p.sourcePage.coerceIn(1, src.numberOfPages)
                    src.copyPagesTo(n, n, out)
                    // Apply rotation to the freshly-copied page (the last one).
                    if (p.rotation % 360 != 0) {
                        out.getPage(out.numberOfPages).setRotation(p.rotation)
                    }
                }

                out.close()
                writer.close()
                src.close()
                reader.close()
            } ?: throw Exception("Cannot open output stream")
        } ?: throw Exception("Cannot open source PDF")

        return outputUri.toString()
    }
}