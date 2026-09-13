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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
// In text-recognition 16.x the concrete options class lives under the `latin`
// sub-package; `getClient` accepts it via TextRecognizerOptionsInterface.
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Makes an image-only (scanned) PDF searchable and selectable by:
 *  1. rendering each page to a bitmap and OCR'ing it with ML Kit Text Recognition,
 *  2. writing a new PDF whose page is the original scan image with an *invisible*
 *     recognized-text layer on top (text rendering mode 3), so the visual stays
 *     identical but the text can be found, copied and indexed.
 *
 * Requires the (bundled, on-device) ML Kit model — downloaded automatically on
 * first use, so the device needs a network connection the first time.
 */
class PdfOcrExporter(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // OCR runs on a 2x render for better accuracy (ML Kit accuracy degrades past
    // ~2Mpx) so the long edge is capped; coordinates are scaled back by the actual
    // scale applied. The background page image is capped too so a high-resolution
    // scan doesn't allocate a multi-megapixel bitmap per page.
    private val maxOcrEdge = 2048f
    private val maxBgEdge = 2560f

    /**
     * OCRs every page of [sourceUri]. Writes [baseName]_ocr.pdf to Downloads and
     * returns its URI. [onProgress] is invoked as (done, total) after each page.
     */
    suspend fun ocrPdf(
        sourceUri: Uri,
        baseName: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): String {
        // PdfRenderer wants a file descriptor → copy to a temp file.
        val src = File.createTempFile("ocr_src_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(sourceUri)?.use { i ->
            src.outputStream().use { i.copyTo(it) }
        }

        val outFile = File.createTempFile("ocr_out_", ".pdf", context.cacheDir)

        try {
            val fd = ParcelFileDescriptor.open(src, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val total = renderer.pageCount
            if (total == 0) throw IOException("PDF has no pages")

            PdfDocument(PdfWriter(FileOutputStream(outFile))).use { pdf ->
                val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
                for (index in 0 until total) {
                    // Native page size in points (== pixels at scale 1).
                    val sizePage = renderer.openPage(index)
                    val nativeW = sizePage.width.toFloat()
                    val nativeH = sizePage.height.toFloat()
                    sizePage.close()

                    val longEdge = maxOf(nativeW, nativeH)
                    val ocrScale = minOf(2f, maxOcrEdge / longEdge)
                    val bgScale = minOf(1f, maxBgEdge / longEdge)

                    val ocr = renderPage(renderer, index, ocrScale)
                    val bg = renderPage(renderer, index, bgScale)
                    val ocrBmp = ocr.bitmap
                    val imgBmp = bg.bitmap

                    // New page at the native size; a (possibly downscaled) copy of the
                    // scan is stretched to fill it exactly, so text stays in native
                    // coordinate space even when the background is capped.
                    val outPage = pdf.addNewPage(PageSize(nativeW, nativeH))
                    val canvas = PdfCanvas(outPage)

                    try {
                        // Background: the original scan as a full-page image.
                        val xobject = PdfImageXObject(ImageDataFactory.create(bitmapToJpeg(imgBmp)))
                        canvas.addXObjectFittedIntoRectangle(
                            xobject, Rectangle(nativeW, nativeH)
                        )
                        // Release iText's in-memory copy of THIS page's image now, so the
                        // heap does not accumulate a full-page image for every page of the
                        // document simultaneously (this was the OOM around page 22).
                        xobject.flush()

                        // Foreground: invisible recognized words at their boxes.
                        if (ocrBmp.isRecycled || imgBmp.isRecycled) throw IOException("Render failed")
                        val text = recognizer.process(InputImage.fromBitmap(ocrBmp, 0)).await()
                        canvas.setTextRenderingMode(PdfCanvasConstants.TextRenderingMode.INVISIBLE)
                        for (block in text.textBlocks) {
                            for (line in block.lines) {
                                for (element in line.elements) {
                                    val box = element.boundingBox ?: continue
                                    val word = element.text
                                    if (word.isBlank()) continue
                                    val x = box.left / ocr.appliedScale
                                    val top = box.top / ocr.appliedScale
                                    val h = box.height() / ocr.appliedScale
                                    val fontSize = (h * 0.92f).coerceAtLeast(4f)
                                    // baseline at bottom of the box, flipped to PDF's bottom-left origin
                                    val baselineY = nativeH - (top + h)
                                    canvas.beginText()
                                    canvas.setFontAndSize(font, fontSize)
                                    canvas.setTextMatrix(fontSize, 0f, 0f, fontSize, x, baselineY)
                                    canvas.showText(word)
                                    canvas.endText()
                                }
                            }
                        }
                    } finally {
                        // Recycle even when OCR throws, or every leaked bitmap adds up.
                        ocrBmp.recycle()
                        imgBmp.recycle()
                    }
                    onProgress(index + 1, total)
                }
            }

            renderer.close()
            fd.close()

            // Copy the finished PDF into Downloads.
            val cleanBase = baseName.removeSuffix(".pdf")
                .substringBeforeLast(".")
                .ifBlank { "document" }
            return copyToDownloads(outFile, "${cleanBase}_ocr.pdf", context)
        } finally {
            src.delete()
            outFile.delete()
        }
    }

    /** A rendered page plus the scale that was actually applied to the bitmap. */
    private class RenderedPage(val bitmap: Bitmap, val appliedScale: Float)

    /** Rendering a page at [requestedScale] relative to its native point size. */
    private fun renderPage(
        renderer: PdfRenderer,
        index: Int,
        requestedScale: Float
    ): RenderedPage {
        val page = renderer.openPage(index)
        try {
            val w = (page.width * requestedScale).toInt().coerceAtLeast(1)
            val h = (page.height * requestedScale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(bmp).apply { drawColor(Color.WHITE) }
            page.render(
                bmp, null, Matrix().apply { setScale(requestedScale, requestedScale) },
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )
            return RenderedPage(bmp, requestedScale)
        } finally {
            page.close()
        }
    }

    // JPEG (not PNG) for the background layer: a PNG of a scanned page is often
    // many megabytes and iText retains each page's image in memory until the
    // document closes, which blew the heap on multi-page scans. JPEG is ~40x
    // smaller for scans, so all pages' images fit comfortably in the heap.
    private fun bitmapToJpeg(bmp: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        return bos.toByteArray()
    }

    /** Pages in the PDF (for the info row). */
    fun pageCount(sourceUri: Uri): Int {
        return try {
            val f = File.createTempFile("ocr_count_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(sourceUri)?.use { i ->
                f.outputStream().use { i.copyTo(it) }
            }
            val fd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val n = renderer.pageCount
            renderer.close()
            fd.close()
            f.delete()
            n
        } catch (_: Exception) {
            0
        }
    }

    companion object {
        private fun copyToDownloads(
            source: File,
            name: String,
            context: Context
        ): String {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Files.getContentUri("external"), values
            ) ?: throw IOException("Failed to create output file")
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: throw IOException("Cannot open output stream")
            return uri.toString()
        }
    }
}