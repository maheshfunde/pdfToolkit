package com.yashodatech.pdftoolkit.pdf

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter


data class SplitRange(val from: Int, val to: Int)


class PdfSplitter {

    fun getPageCount(context: Context, uri: Uri): Int {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return 0

        return try {
            val reader = PdfReader(inputStream)
            val pdf = PdfDocument(reader)
            val count = pdf.numberOfPages
            pdf.close()
            reader.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            inputStream.close()
        }
    }

    fun splitPdf(
        context: Context,
        sourceUri: Uri,
        ranges: List<SplitRange>,
        baseFileName: String
    ): List<Uri> {

        val resolver = context.contentResolver
        val outputUris = mutableListOf<Uri>()

        for ((index, range) in ranges.withIndex()) {

            // ── Read source PDF ─────────────────────
            val inputStream = resolver.openInputStream(sourceUri)
                ?: continue

            try {
                val reader = PdfReader(inputStream)
                val sourcePdf = PdfDocument(reader)

                // ── Create output file ──────────────
                val outputName = if (ranges.size == 1) {
                    "${baseFileName}_split.pdf"
                } else {
                    "${baseFileName}_part${index + 1}_pages${range.from}-${range.to}.pdf"
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, outputName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                    )
                }

                val outputUri = resolver.insert(
                    MediaStore.Files.getContentUri("external"),
                    contentValues
                ) ?: continue

                val outputStream = resolver.openOutputStream(outputUri)
                    ?: continue

                // ── Write split PDF ─────────────────
                val writer = PdfWriter(outputStream)
                val outputPdf = PdfDocument(writer)

                // Copy pages in range
                sourcePdf.copyPagesTo(
                    range.from,
                    range.to,
                    outputPdf
                )

                outputPdf.close()
                writer.close()
                outputStream.close()
                sourcePdf.close()
                reader.close()

                outputUris.add(outputUri)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                inputStream.close()
            }
        }

        return outputUris
    }
}