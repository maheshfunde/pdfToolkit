package com.yashodatech.pdftoolkit.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState

/**
 * Burns a text watermark onto every page of a PDF, producing a new file
 * in the Downloads folder.
 */
class PdfWatermarker {

    fun addTextWatermark(
        context: Context,
        sourceUri: Uri,
        config: WatermarkConfig,
        outputFileName: String
    ): String {
        val safeText = config.text.ifBlank { "DRAFT" }
        val cleanName = if (outputFileName.endsWith(".pdf", true))
            outputFileName else "$outputFileName.pdf"
        val resolver = context.contentResolver

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

                // Deep-copy all pages (preserves existing content).
                src.copyPagesTo(1, src.numberOfPages, out)

                val font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
                val r = Color.red(config.colorArgb)
                val g = Color.green(config.colorArgb)
                val b = Color.blue(config.colorArgb)
                val fill = DeviceRgb(r, g, b)
                val opacity = config.opacity.coerceIn(0f, 1f)
                val extGState = PdfExtGState().setFillOpacity(opacity)

                for (i in 1..out.numberOfPages) {
                    val page = out.getPage(i)
                    val w = page.pageSize.width
                    val h = page.pageSize.height

                    val canvas = PdfCanvas(
                        page.newContentStreamAfter(),
                        page.resources,
                        out
                    )

                    canvas.saveState()
                    canvas.setExtGState(extGState)
                    canvas.setFillColor(fill)
                    canvas.beginText()
                    canvas.setFontAndSize(font, config.fontSize)

                    // Rotate the text and centre its midpoint on the page.
                    val theta = Math.toRadians(config.rotation.toDouble())
                    val cos = Math.cos(theta).toFloat()
                    val sin = Math.sin(theta).toFloat()
                    val textW = font.getWidth(safeText, config.fontSize)
                    val startX = w / 2f - (textW / 2f) * cos
                    val startY = h / 2f - (textW / 2f) * sin
                    canvas.setTextMatrix(cos, -sin, sin, cos, startX, startY)
                    canvas.showText(safeText)

                    canvas.endText()
                    canvas.restoreState()
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