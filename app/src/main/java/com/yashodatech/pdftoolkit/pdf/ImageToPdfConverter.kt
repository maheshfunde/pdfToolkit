package com.yashodatech.pdftoolkit.pdf

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import java.io.File

enum class PdfQuality(val label: String, val scale: Float) {
    LOW("Low (fast)", 0.5f),
    MEDIUM("Medium", 0.75f),
    HIGH("High (best)", 1.0f)
}

enum class PdfPageSize(val label: String) {
    FIT_IMAGE("Fit Image"),
    A4("A4"),
    LETTER("Letter")
}

class ImageToPdfConverter {

    fun createPdf(
        context: Context,
        imageUris: List<Uri>,
        fileName: String,
        quality: PdfQuality = PdfQuality.HIGH,
        pageSize: PdfPageSize = PdfPageSize.FIT_IMAGE
    ): String {

        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        ) ?: throw Exception("Failed to create file")

        val outputStream = resolver.openOutputStream(uri)
            ?: throw Exception("Failed to open output stream")

        val writer = PdfWriter(outputStream)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)

        // Remove default margins
        document.setMargins(0f, 0f, 0f, 0f)

        // List to keep track of temp files for cleanup
        val tempFiles = mutableListOf<File>()

        try {
            for (imageUri in imageUris) {
                // FIX: Instead of inputStream.readBytes(), copy to a temp file.
                // This prevents loading the entire image into RAM.
                val tempFile = File.createTempFile("pdf_img_", ".tmp", context.cacheDir)
                tempFiles.add(tempFile)

                resolver.openInputStream(imageUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: continue

                // iText can read metadata and data from the file path efficiently
                val imageData = ImageDataFactory.create(tempFile.absolutePath)
                val image = Image(imageData)

                // Scale by quality (layout scaling)
                val scaledWidth = image.imageWidth * quality.scale
                val scaledHeight = image.imageHeight * quality.scale

                when (pageSize) {
                    PdfPageSize.FIT_IMAGE -> {
                        val page = PageSize(scaledWidth, scaledHeight)
                        pdfDoc.addNewPage(page)
                        image.scaleToFit(scaledWidth, scaledHeight)
                        image.setFixedPosition(pdfDoc.numberOfPages, 0f, 0f)
                    }

                    PdfPageSize.A4 -> {
                        val a4 = PageSize.A4
                        pdfDoc.addNewPage(a4)
                        val maxWidth = a4.width - 72f
                        val maxHeight = a4.height - 72f
                        val ratio = minOf(maxWidth / image.imageWidth, maxHeight / image.imageHeight)
                        val fitWidth = image.imageWidth * ratio
                        val fitHeight = image.imageHeight * ratio
                        val x = (a4.width - fitWidth) / 2f
                        val y = (a4.height - fitHeight) / 2f
                        image.scaleToFit(fitWidth, fitHeight)
                        image.setFixedPosition(pdfDoc.numberOfPages, x, y)
                    }

                    PdfPageSize.LETTER -> {
                        val letter = PageSize.LETTER
                        pdfDoc.addNewPage(letter)
                        val maxWidth = letter.width - 72f
                        val maxHeight = letter.height - 72f
                        val ratio = minOf(maxWidth / image.imageWidth, maxHeight / image.imageHeight)
                        val fitWidth = image.imageWidth * ratio
                        val fitHeight = image.imageHeight * ratio
                        val x = (letter.width - fitWidth) / 2f
                        val y = (letter.height - fitHeight) / 2f
                        image.scaleToFit(fitWidth, fitHeight)
                        image.setFixedPosition(pdfDoc.numberOfPages, x, y)
                    }
                }

                document.add(image)
            }
        } finally {
            // Close the document to finish writing to the outputStream
            document.close()
            outputStream.close()

            // CRITICAL: Clean up temp files AFTER the document is closed
            tempFiles.forEach { it.delete() }
        }

        return uri.toString()
    }
}