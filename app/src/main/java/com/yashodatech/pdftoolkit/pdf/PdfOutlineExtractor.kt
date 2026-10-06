package com.yashodatech.pdftoolkit.pdf

import android.content.Context
import android.net.Uri
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfOutline
import com.itextpdf.kernel.pdf.PdfDictionary
import com.itextpdf.kernel.geom.PageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class PdfOutlineItem(
    val title: String,
    val pageIndex: Int,
    val children: List<PdfOutlineItem> = emptyList()
)

data class PdfDocumentMetadata(
    val title: String = "",
    val author: String = "",
    val subject: String = "",
    val keywords: String = "",
    val creator: String = "",
    val producer: String = "",
    val pageCount: Int = 0,
    val fileSize: String = "",
    val pageDimensions: String = ""
)

object PdfOutlineExtractor {

    suspend fun extractOutlines(context: Context, uri: Uri): List<PdfOutlineItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PdfOutlineItem>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext result
            val reader = PdfReader(inputStream)
            val pdfDoc = PdfDocument(reader)
            val outlines = pdfDoc.getOutlines(false)

            if (outlines != null) {
                fun parseOutline(outline: PdfOutline): PdfOutlineItem? {
                    val rawTitle = outline.title?.trim() ?: return null
                    if (rawTitle.isEmpty()) return null

                    var pageIdx = 0
                    try {
                        val dest = outline.destination
                        if (dest != null) {
                            val destPageObj = dest.getDestinationPage(null)
                            if (destPageObj is PdfDictionary) {
                                val num = pdfDoc.getPageNumber(destPageObj)
                                if (num in 1..pdfDoc.numberOfPages) {
                                    pageIdx = num - 1
                                }
                            }
                        }
                    } catch (_: Throwable) {}

                    val childItems = outline.allChildren.mapNotNull { parseOutline(it) }
                    return PdfOutlineItem(title = rawTitle, pageIndex = pageIdx, children = childItems)
                }

                outlines.allChildren.forEach { child ->
                    parseOutline(child)?.let { result.add(it) }
                }
            }

            pdfDoc.close()
            reader.close()
            inputStream.close()
        } catch (_: Throwable) {}
        result
    }

    suspend fun extractMetadata(
        context: Context,
        uri: Uri,
        pageCount: Int,
        fileSizeText: String
    ): PdfDocumentMetadata = withContext(Dispatchers.IO) {
        var metadata = PdfDocumentMetadata(pageCount = pageCount, fileSize = fileSizeText)
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext metadata
            val reader = PdfReader(inputStream)
            val pdfDoc = PdfDocument(reader)
            val info = pdfDoc.documentInfo

            var dimenStr = ""
            if (pdfDoc.numberOfPages > 0) {
                try {
                    val page = pdfDoc.getPage(1)
                    val rect = page.pageSizeWithRotation
                    val widthPt = rect.width
                    val heightPt = rect.height
                    val widthMm = (widthPt * 25.4f / 72f).toInt()
                    val heightMm = (heightPt * 25.4f / 72f).toInt()
                    val widthIn = String.format(Locale.US, "%.1f", widthPt / 72f)
                    val heightIn = String.format(Locale.US, "%.1f", heightPt / 72f)
                    dimenStr = "$widthMm × $heightMm mm ($widthIn × $heightIn in)"
                } catch (_: Throwable) {}
            }

            metadata = PdfDocumentMetadata(
                title = info?.title?.trim() ?: "",
                author = info?.author?.trim() ?: "",
                subject = info?.subject?.trim() ?: "",
                keywords = info?.keywords?.trim() ?: "",
                creator = info?.creator?.trim() ?: "",
                producer = info?.producer?.trim() ?: "",
                pageCount = if (pageCount > 0) pageCount else pdfDoc.numberOfPages,
                fileSize = fileSizeText,
                pageDimensions = dimenStr
            )

            pdfDoc.close()
            reader.close()
            inputStream.close()
        } catch (_: Throwable) {}
        metadata
    }
}
