package com.yashodatech.pdftoolkit.pdf

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.kernel.pdf.PdfDocument as ITextPdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Converts the readable text of a PDF into a Word (.docx) document.
 *
 * A .docx is a ZIP archive of OOXML parts, so a valid—and Word/WPS/Google-Docs
 * readable—file can be produced with plain [ZipOutputStream]: no external POI /
 * docx4j dependency is needed. Each line of extracted text becomes one paragraph.
 * Image-only pages (scans) contribute no text.
 */
class PdfWordExporter(private val context: Context) {

    /**
     * Extracts text from [sourceUri], wraps it in a minimal .docx, and writes it to
     * Downloads/PDF Toolkit. Returns the output URI as a string.
     */
    fun exportDocx(sourceUri: Uri, baseName: String): String {
        val pages = extractPages(sourceUri)
        if (pages.all { it.isBlank() }) {
            throw IOException("No extractable text found — this PDF is likely image-only (a scan).")
        }
        val cleanBase = baseName.removeSuffix(".pdf")
            .substringBeforeLast(".")
            .ifBlank { "document" }
        return writeDocx("${cleanBase}_document.docx", buildDocx(pages))
    }

    private fun extractPages(sourceUri: Uri): List<String> {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IOException("Cannot open PDF")
        val reader = PdfReader(inputStream)
        val pdfDoc = ITextPdfDocument(reader)
        val pages = mutableListOf<String>()
        try {
            for (i in 1..pdfDoc.numberOfPages) {
                val text = try {
                    PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i))
                } catch (_: Exception) {
                    ""
                }
                pages.add(cleanText(text))
            }
        } finally {
            pdfDoc.close()
            reader.close()
            inputStream.close()
        }
        return pages
    }

    /**
     * Scrubs extracted text to printable, XML-legal Unicode so the .docx has no
     * garbage glyphs or malformed parts:
     *  - Drops control characters (illegal in XML 1.0) and noncharacters.
     *  - Maps the Private Use Area (PUA, U+E000–U+F8FF) — where unresolvable PDF
     *    glyph codes land when the font has no proper Unicode map, the usual cause
     *    of "unrecognised characters" — to a space.
     *  - Preserves genuine Unicode: accented Latin, CJK, Devanagari, math, emoji.
     * Iterates by code point so surrogate pairs (emoji etc.) survive intact.
     */
    private fun cleanText(raw: String): String {
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val cp = raw.codePointAt(i)
            i += Character.charCount(cp)
            when {
                cp == '\n'.code || cp == '\r'.code || cp == '\t'.code -> sb.appendCodePoint(cp)
                cp < 0x20 -> {}                       // XML-illegal controls
                cp == 0xFFFE || cp == 0xFFFF -> {}    // noncharacters
                cp in 0xFDD0..0xFDEF -> {}            // noncharacters
                cp in 0xE000..0xF8FF -> sb.append(' ')// PUA glyph garbage — keep word breaks
                else -> sb.appendCodePoint(cp)
            }
        }
        return sb.toString().replace(Regex("[ \t]+"), " ").trim()
    }

    /** Builds the minimal .docx ZIP for [pages], one paragraph per non-empty line. */
    private fun buildDocx(pages: List<String>): ByteArray {
        val docBody = StringBuilder()
        for (text in pages) {
            if (text.isBlank()) continue
            for (line in text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }) {
                docBody.append("<w:p><w:r><w:t xml:space=\"preserve\">")
                    .append(escapeXml(line))
                    .append("</w:t></w:r></w:p>")
            }
        }

        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$docBody<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/></w:sectPr></w:body></w:document>"""

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(contentTypes.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(rels.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun writeDocx(name: String, bytes: ByteArray): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(
                MediaStore.Downloads.MIME_TYPE,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/PDF Toolkit"
            )
        }
        val uri = resolver.insert(
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
        ) ?: throw IOException("Failed to create Word file")

        resolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
        } ?: throw IOException("Cannot open output stream")

        return uri.toString()
    }

    /** Pages in the PDF (for the info row). */
    fun pageCount(sourceUri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return 0
            val reader = PdfReader(inputStream)
            val pdfDoc = ITextPdfDocument(reader)
            val n = pdfDoc.numberOfPages
            pdfDoc.close()
            reader.close()
            inputStream.close()
            n
        } catch (_: Exception) {
            0
        }
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}