package com.yashodatech.pdftoolkit.pdf

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.utils.PdfMerger as ITextPdfMerger

class PdfMerger {

    fun mergePdfs(
        context: Context,
        pdfUris: List<Uri>,
        outputFileName: String
    ): String {

        val resolver = context.contentResolver

        // ── Create output file ──────────────────────
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val outputUri = resolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        ) ?: throw Exception("Failed to create output file")

        val outputStream = resolver.openOutputStream(outputUri)
            ?: throw Exception("Failed to open output stream")

        // ── Create merged PDF ───────────────────────
        val writer = PdfWriter(outputStream)
        val mergedPdf = PdfDocument(writer)
        val merger = ITextPdfMerger(mergedPdf)

        for (pdfUri in pdfUris) {

            val inputStream = resolver.openInputStream(pdfUri)
                ?: continue

            try {
                val reader = PdfReader(inputStream)
                val sourcePdf = PdfDocument(reader)

                // Merge all pages from this PDF
                merger.merge(sourcePdf, 1, sourcePdf.numberOfPages)

                sourcePdf.close()
                reader.close()
            } catch (e: Exception) {
                // Skip corrupt/unreadable PDFs
                e.printStackTrace()
            } finally {
                inputStream.close()
            }
        }

        mergedPdf.close()
        writer.close()
        outputStream.close()

        return outputUri.toString()
    }
}