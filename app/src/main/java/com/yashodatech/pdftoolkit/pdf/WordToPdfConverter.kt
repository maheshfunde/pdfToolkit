package com.yashodatech.pdftoolkit.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.abs
import kotlin.math.max

// ═══════════════════════════════════════════════════════════
// Data Classes
// ═══════════════════════════════════════════════════════════

data class WordToPdfResult(
    val success: Boolean,
    val tempFile: File? = null,
    val error: String? = null,
    val pageCount: Int = 0
)

data class TextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isSuperscript: Boolean = false,
    val isSubscript: Boolean = false,
    val fontSize: Float = -1f,
    val fontColor: Int = -1
)

data class ParagraphSpacing(
    val beforeTwips: Float = -1f,
    val afterTwips: Float = -1f,
    val lineValue: Float = -1f,
    val lineRule: String = "auto"
)

data class ParagraphIndent(
    val leftTwips: Float = 0f,
    val rightTwips: Float = 0f,
    val firstLineTwips: Float = 0f,
    val hangingTwips: Float = 0f
)

data class DocParagraph(
    val runs: List<TextRun> = emptyList(),
    val isHeading: Boolean = false,
    val headingLevel: Int = 0,
    val isBullet: Boolean = false,
    val isNumbered: Boolean = false,
    val alignment: String = "left",
    val styleId: String = "",
    val spacing: ParagraphSpacing = ParagraphSpacing(),
    val indent: ParagraphIndent = ParagraphIndent(),
    val isPageBreakBefore: Boolean = false
) {
    fun hasText(): Boolean = runs.any { it.text.isNotBlank() }
}

data class PageMargins(
    val top: Float = 1440f,
    val bottom: Float = 1440f,
    val left: Float = 1440f,
    val right: Float = 1440f
)

data class StyleInfo(
    val spaceBeforeTwips: Float = -1f,
    val spaceAfterTwips: Float = -1f,
    val lineValue: Float = -1f,
    val lineRule: String = "auto",
    val fontSizeHalfPt: Float = -1f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val indentLeftTwips: Float = 0f,
    val indentFirstLineTwips: Float = 0f,
    val parentStyleId: String = ""
)

// ═══════════════════════════════════════════════════════════
// Converter
// ═══════════════════════════════════════════════════════════

class WordToPdfConverter(private val context: Context) {

    companion object {
        private const val A4_WIDTH = 595
        private const val A4_HEIGHT = 842

        private const val DEFAULT_FONT_SIZE = 11f
        private const val DEFAULT_LINE_SPACING_VALUE = 276f  // 1.15 × 240
        private const val DEFAULT_SPACE_AFTER_TWIPS = 160f   // 8pt
        private const val DEFAULT_SPACE_BEFORE_TWIPS = 0f

        private fun twipsToPoints(twips: Float): Float = twips / 20f
        private fun halfPtToPoints(halfPt: Float): Float = halfPt / 2f

        // ✅ FIX #1: Removed \u200C (ZWNJ) and \u200D (ZWJ)
        // These are CRITICAL for Devanagari/Marathi conjuncts & half-forms.
        // \u200C (ZWNJ) prevents conjunct → shows half-form (e.g., र्‌य)
        // \u200D (ZWJ)  forces conjunct  → eyelash-ra, special ligatures
        private val UNWANTED_CHARS = setOf(
            '\u200B', // Zero Width Space — safe to remove
            '\u200E', // Left-to-Right Mark
            '\u200F', // Right-to-Left Mark
            '\uFEFF', // BOM
            '\u00AD'  // Soft Hyphen
        )
    }

    private var pageMargins = PageMargins()
    private var styles = mutableMapOf<String, StyleInfo>()
    private var defaultBodyStyle = StyleInfo()
    private var defaultFontSize = DEFAULT_FONT_SIZE

    // ═══════════════════════════════════════════
    // Main Convert
    // ═══════════════════════════════════════════
    suspend fun convert(
        inputUri: Uri,
        outputFileName: String = "converted_document",
        onProgress: (Float) -> Unit = {}
    ): WordToPdfResult = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f)
            val xmlFiles = extractAllXml(inputUri)
            onProgress(0.15f)

            xmlFiles["word/styles.xml"]?.let { parseStyles(it) }
            onProgress(0.2f)

            val documentXml = xmlFiles["word/document.xml"]
                ?: return@withContext WordToPdfResult(
                    success = false,
                    error = "Cannot read Word document."
                )

            parsePageMargins(documentXml)
            onProgress(0.3f)

            val paragraphs = parseDocumentXml(documentXml)
            if (paragraphs.isEmpty()) {
                return@withContext WordToPdfResult(
                    success = false,
                    error = "Document is empty or not supported"
                )
            }

            onProgress(0.5f)
            renderToPdf(paragraphs, outputFileName, onProgress)

        } catch (e: Exception) {
            e.printStackTrace()
            WordToPdfResult(false, error = "Error: ${e.localizedMessage}")
        }
    }

    // ═══════════════════════════════════════════
    // Extract XML from DOCX
    // ═══════════════════════════════════════════
    private fun extractAllXml(uri: Uri): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".xml")) {
                            map[entry.name] = zip.bufferedReader().readText()
                        }
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return map
    }

    // ═══════════════════════════════════════════
    // Parse Page Margins
    // ═══════════════════════════════════════════
    private fun parsePageMargins(xml: String) {
        try {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
            val parser = factory.newPullParser()
            parser.setInput(xml.reader())
            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    val tag = parser.name?.substringAfterLast(":") ?: ""
                    if (tag == "pgMar") {
                        pageMargins = PageMargins(
                            top = attr(parser, "top").toFloatOrNull() ?: 1440f,
                            bottom = attr(parser, "bottom").toFloatOrNull() ?: 1440f,
                            left = attr(parser, "left").toFloatOrNull() ?: 1440f,
                            right = attr(parser, "right").toFloatOrNull() ?: 1440f
                        )
                    }
                }
                ev = parser.next()
            }
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════
    // Parse Styles
    // ═══════════════════════════════════════════
    private fun parseStyles(xml: String) {
        try {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
            val parser = factory.newPullParser()
            parser.setInput(xml.reader())

            var styleId = ""
            var parentId = ""
            var isDefault = false
            var styleType = ""
            var inStyle = false; var inPPr = false; var inRPr = false

            var sBefore = -1f; var sAfter = -1f; var sLine = -1f; var sRule = "auto"
            var sFontSize = -1f; var sBold = false; var sItalic = false
            var sIndentLeft = 0f; var sIndentFirst = 0f

            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name?.substringAfterLast(":") ?: ""
                when (ev) {
                    XmlPullParser.START_TAG -> when (tag) {
                        "style" -> {
                            inStyle = true
                            styleId = attr(parser, "styleId")
                            isDefault = attr(parser, "default") == "1"
                            styleType = attr(parser, "type")
                            parentId = ""
                            sBefore = -1f; sAfter = -1f; sLine = -1f; sRule = "auto"
                            sFontSize = -1f; sBold = false; sItalic = false
                            sIndentLeft = 0f; sIndentFirst = 0f
                        }
                        "basedOn" -> if (inStyle) parentId = attr(parser, "val")
                        "pPr" -> if (inStyle) inPPr = true
                        "rPr" -> if (inStyle) inRPr = true
                        "spacing" -> if (inStyle && inPPr) {
                            attr(parser, "before").toFloatOrNull()?.let { sBefore = it }
                            attr(parser, "after").toFloatOrNull()?.let { sAfter = it }
                            attr(parser, "line").toFloatOrNull()?.let { sLine = it }
                            attr(parser, "lineRule").let { if (it.isNotEmpty()) sRule = it }
                        }
                        "ind" -> if (inStyle && inPPr) {
                            attr(parser, "left").toFloatOrNull()?.let { sIndentLeft = it }
                            attr(parser, "firstLine").toFloatOrNull()?.let { sIndentFirst = it }
                        }
                        "b" -> if (inStyle && inRPr) sBold = true
                        "i" -> if (inStyle && inRPr) sItalic = true
                        "sz" -> if (inStyle && inRPr) {
                            attr(parser, "val").toFloatOrNull()?.let { sFontSize = it }
                        }
                        "szCs" -> if (inStyle && inRPr && sFontSize < 0) {
                            attr(parser, "val").toFloatOrNull()?.let { sFontSize = it }
                        }
                    }
                    XmlPullParser.END_TAG -> when (tag) {
                        "style" -> {
                            val info = StyleInfo(
                                sBefore, sAfter, sLine, sRule,
                                sFontSize, sBold, sItalic,
                                sIndentLeft, sIndentFirst, parentId
                            )
                            if (styleId.isNotEmpty()) styles[styleId] = info
                            if (isDefault && styleType == "paragraph") defaultBodyStyle = info
                            if (isDefault && styleType == "character" && sFontSize > 0) {
                                defaultFontSize = halfPtToPoints(sFontSize)
                            }
                            inStyle = false
                        }
                        "pPr" -> inPPr = false
                        "rPr" -> inRPr = false
                    }
                }
                ev = parser.next()
            }

            if (defaultBodyStyle.fontSizeHalfPt > 0) {
                defaultFontSize = halfPtToPoints(defaultBodyStyle.fontSizeHalfPt)
            }

        } catch (e: Exception) { e.printStackTrace() }
    }

    // ═══════════════════════════════════════════
    // Resolve style with inheritance
    // ═══════════════════════════════════════════
    private fun resolveStyle(styleId: String): StyleInfo {
        val style = styles[styleId] ?: return defaultBodyStyle
        val parent = if (style.parentStyleId.isNotEmpty()) {
            styles[style.parentStyleId] ?: defaultBodyStyle
        } else defaultBodyStyle

        return StyleInfo(
            spaceBeforeTwips = if (style.spaceBeforeTwips >= 0) style.spaceBeforeTwips else parent.spaceBeforeTwips,
            spaceAfterTwips = if (style.spaceAfterTwips >= 0) style.spaceAfterTwips else parent.spaceAfterTwips,
            lineValue = if (style.lineValue > 0) style.lineValue else parent.lineValue,
            lineRule = if (style.lineValue > 0) style.lineRule else parent.lineRule,
            fontSizeHalfPt = if (style.fontSizeHalfPt > 0) style.fontSizeHalfPt else parent.fontSizeHalfPt,
            isBold = style.isBold || parent.isBold,
            isItalic = style.isItalic || parent.isItalic,
            indentLeftTwips = if (style.indentLeftTwips > 0) style.indentLeftTwips else parent.indentLeftTwips,
            indentFirstLineTwips = if (style.indentFirstLineTwips > 0) style.indentFirstLineTwips else parent.indentFirstLineTwips,
            parentStyleId = style.parentStyleId
        )
    }

    // ═══════════════════════════════════════════
    // Parse Document XML
    // ═══════════════════════════════════════════
    private fun parseDocumentXml(xml: String): List<DocParagraph> {
        val paragraphs = mutableListOf<DocParagraph>()

        try {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
            val parser = factory.newPullParser()
            parser.setInput(xml.reader())

            var inRun = false; var inText = false
            var inPPr = false; var inRPr = false

            var runs = mutableListOf<TextRun>()
            var text = StringBuilder()

            var rB = false; var rI = false; var rU = false
            var rStrike = false; var rSuper = false; var rSub = false
            var rSize = -1f; var rColor = -1

            var pStyle = ""; var pHeading = false; var pLevel = 0
            var pBullet = false; var pNumbered = false
            var pAlign = "left"; var pBreak = false

            var sBefore = -1f; var sAfter = -1f
            var sLine = -1f; var sRule = "auto"

            var iLeft = 0f; var iRight = 0f
            var iFirst = 0f; var iHanging = 0f

            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name?.substringAfterLast(":") ?: ""
                when (ev) {
                    XmlPullParser.START_TAG -> when (tag) {
                        "p" -> {
                            runs = mutableListOf()
                            pStyle = ""; pHeading = false; pLevel = 0
                            pBullet = false; pNumbered = false
                            pAlign = "left"; pBreak = false
                            sBefore = -1f; sAfter = -1f
                            sLine = -1f; sRule = "auto"
                            iLeft = 0f; iRight = 0f; iFirst = 0f; iHanging = 0f
                        }
                        "pPr" -> inPPr = true
                        "pStyle" -> if (inPPr) {
                            pStyle = attr(parser, "val")
                            if (pStyle.contains("Heading", true)) {
                                pHeading = true
                                pLevel = pStyle.filter { it.isDigit() }.toIntOrNull() ?: 1
                            }
                            if (pStyle.contains("ListBullet", true)) pBullet = true
                            if (pStyle.contains("ListNumber", true)) pNumbered = true
                            if (pStyle.contains("ListParagraph", true)) pBullet = true

                            val resolved = resolveStyle(pStyle)
                            if (resolved.spaceBeforeTwips >= 0) sBefore = resolved.spaceBeforeTwips
                            if (resolved.spaceAfterTwips >= 0) sAfter = resolved.spaceAfterTwips
                            if (resolved.lineValue > 0) { sLine = resolved.lineValue; sRule = resolved.lineRule }
                            if (resolved.indentLeftTwips > 0) iLeft = resolved.indentLeftTwips
                            if (resolved.indentFirstLineTwips > 0) iFirst = resolved.indentFirstLineTwips
                        }
                        "spacing" -> if (inPPr) {
                            val bStr = attr(parser, "before")
                            if (bStr.isNotEmpty()) sBefore = bStr.toFloatOrNull() ?: sBefore
                            val aStr = attr(parser, "after")
                            if (aStr.isNotEmpty()) sAfter = aStr.toFloatOrNull() ?: sAfter
                            val lStr = attr(parser, "line")
                            if (lStr.isNotEmpty()) sLine = lStr.toFloatOrNull() ?: sLine
                            val rStr = attr(parser, "lineRule")
                            if (rStr.isNotEmpty()) sRule = rStr
                        }
                        "ind" -> if (inPPr) {
                            attr(parser, "left").toFloatOrNull()?.let { iLeft = it }
                            attr(parser, "right").toFloatOrNull()?.let { iRight = it }
                            attr(parser, "firstLine").toFloatOrNull()?.let { iFirst = it }
                            attr(parser, "hanging").toFloatOrNull()?.let { iHanging = it }
                        }
                        "jc" -> if (inPPr) { pAlign = attr(parser, "val").ifEmpty { "left" } }
                        "numPr" -> if (inPPr && !pNumbered) pBullet = true
                        "numFmt" -> if (inPPr) {
                            when (attr(parser, "val").lowercase()) {
                                "bullet" -> { pBullet = true; pNumbered = false }
                                "decimal", "lowerletter", "upperletter",
                                "lowerroman", "upperroman" -> { pNumbered = true; pBullet = false }
                            }
                        }
                        "pageBreakBefore" -> if (inPPr) {
                            val v = attr(parser, "val"); pBreak = v.isEmpty() || v != "0"
                        }
                        "r" -> {
                            inRun = true
                            rB = pHeading; rI = false; rU = false
                            rStrike = false; rSuper = false; rSub = false
                            rSize = -1f; rColor = -1
                        }
                        "rPr" -> inRPr = true
                        "b" -> if (inRPr) { val v = attr(parser, "val"); rB = v.isEmpty() || (v != "0" && v != "false") }
                        "bCs" -> if (inRPr && !rB) { val v = attr(parser, "val"); rB = v.isEmpty() || (v != "0" && v != "false") }
                        "i" -> if (inRPr) { val v = attr(parser, "val"); rI = v.isEmpty() || (v != "0" && v != "false") }
                        "u" -> if (inRPr) { val v = attr(parser, "val"); rU = v.isNotEmpty() && v != "none" }
                        "strike" -> if (inRPr) { val v = attr(parser, "val"); rStrike = v.isEmpty() || (v != "0" && v != "false") }
                        "vertAlign" -> if (inRPr) { when (attr(parser, "val")) { "superscript" -> rSuper = true; "subscript" -> rSub = true } }
                        "sz" -> if (inRPr) { attr(parser, "val").toFloatOrNull()?.let { if (it > 0) rSize = it } }
                        "szCs" -> if (inRPr && rSize < 0) { attr(parser, "val").toFloatOrNull()?.let { if (it > 0) rSize = it } }
                        "color" -> if (inRPr) {
                            val c = attr(parser, "val")
                            if (c.isNotEmpty() && c != "auto") {
                                try { rColor = Color.parseColor("#$c") } catch (_: Exception) {}
                            }
                        }
                        "t" -> if (inRun) { inText = true; text = StringBuilder() }
                        "tab" -> if (inRun) text.append("\t")
                        "br" -> if (inRun) {
                            addRun(text, runs, rB, rI, rU, rStrike, rSuper, rSub, rSize, rColor)
                            text = StringBuilder()
                            runs.add(TextRun("\n"))
                        }
                    }
                    XmlPullParser.TEXT -> if (inText) text.append(parser.text)
                    XmlPullParser.END_TAG -> when (tag) {
                        "p" -> {
                            addRun(text, runs, rB, rI, rU, rStrike, rSuper, rSub, rSize, rColor)
                            text = StringBuilder()

                            if (sBefore < 0) sBefore = if (pHeading) when (pLevel) {
                                1 -> 480f; 2 -> 360f; 3 -> 280f; else -> 240f
                            } else if (defaultBodyStyle.spaceBeforeTwips >= 0) defaultBodyStyle.spaceBeforeTwips else DEFAULT_SPACE_BEFORE_TWIPS

                            if (sAfter < 0) sAfter = if (pHeading) when (pLevel) {
                                1 -> 120f; 2 -> 80f; else -> 80f
                            } else if (defaultBodyStyle.spaceAfterTwips >= 0) defaultBodyStyle.spaceAfterTwips else DEFAULT_SPACE_AFTER_TWIPS

                            if (sLine < 0) sLine = if (pHeading) 240f
                            else if (defaultBodyStyle.lineValue > 0) defaultBodyStyle.lineValue
                            else DEFAULT_LINE_SPACING_VALUE

                            if (sLine < 0) sRule = "auto"
                            if (pHeading && sRule.isEmpty()) sRule = "auto"

                            paragraphs.add(
                                DocParagraph(
                                    runs = runs.toList(),
                                    isHeading = pHeading,
                                    headingLevel = pLevel,
                                    isBullet = pBullet && runs.any { it.text.isNotBlank() },
                                    isNumbered = pNumbered && runs.any { it.text.isNotBlank() },
                                    alignment = pAlign,
                                    styleId = pStyle,
                                    spacing = ParagraphSpacing(sBefore, sAfter, sLine, sRule),
                                    indent = ParagraphIndent(iLeft, iRight, iFirst, iHanging),
                                    isPageBreakBefore = pBreak
                                )
                            )
                            runs = mutableListOf()
                        }
                        "r" -> {
                            addRun(text, runs, rB, rI, rU, rStrike, rSuper, rSub, rSize, rColor)
                            text = StringBuilder(); inRun = false
                        }
                        "t" -> inText = false
                        "pPr" -> inPPr = false
                        "rPr" -> inRPr = false
                    }
                }
                ev = parser.next()
            }
        } catch (e: Exception) { e.printStackTrace() }

        return paragraphs
    }

    private fun addRun(
        text: StringBuilder, runs: MutableList<TextRun>,
        b: Boolean, i: Boolean, u: Boolean, strike: Boolean,
        sup: Boolean, sub: Boolean, size: Float, color: Int
    ) {
        if (text.isEmpty()) return
        val cleaned = text.toString().filter { it !in UNWANTED_CHARS }
        if (cleaned.isEmpty()) return
        runs.add(TextRun(cleaned, b, i, u, strike, sup, sub, size, color))
    }

    // ═══════════════════════════════════════════════════════════
    // Line Height Calculation
    // ═══════════════════════════════════════════════════════════

    private fun calculateLineHeight(
        paint: Paint,
        spacing: ParagraphSpacing
    ): Float {
        val fontSpacing = paint.fontSpacing

        return when (spacing.lineRule) {
            "exact" -> {
                if (spacing.lineValue > 0) twipsToPoints(spacing.lineValue)
                else fontSpacing
            }
            "atLeast" -> {
                val minHeight = if (spacing.lineValue > 0) twipsToPoints(spacing.lineValue) else 0f
                max(fontSpacing, minHeight)
            }
            else -> {
                if (spacing.lineValue > 0) {
                    val multiplier = spacing.lineValue / 240f
                    fontSpacing * multiplier
                } else {
                    fontSpacing * (DEFAULT_LINE_SPACING_VALUE / 240f)
                }
            }
        }
    }

    // ✅ FIX #3: Per-line line height based on actual runs (handles mixed font sizes)
    private fun calculateLineHeightForRuns(
        lineRuns: List<TextRun>,
        para: DocParagraph,
        spacing: ParagraphSpacing
    ): Float {
        var maxFontSpacing = 0f
        var maxAscent = 0f
        var maxDescent = 0f

        for (run in lineRuns) {
            if (run.text == "\n" || run.text.isBlank()) continue
            val paint = makeRunPaint(run, para)
            val metrics = paint.fontMetrics
            maxFontSpacing = max(maxFontSpacing, paint.fontSpacing)
            maxAscent = max(maxAscent, abs(metrics.ascent))
            maxDescent = max(maxDescent, metrics.descent)
        }

        if (maxFontSpacing == 0f) {
            val refPaint = Paint().apply {
                isAntiAlias = true
                textSize = getParaFontSize(para)
                typeface = Typeface.DEFAULT
            }
            maxFontSpacing = refPaint.fontSpacing
            maxAscent = abs(refPaint.fontMetrics.ascent)
            maxDescent = refPaint.fontMetrics.descent
        }

        return when (spacing.lineRule) {
            "exact" -> {
                if (spacing.lineValue > 0) twipsToPoints(spacing.lineValue)
                else maxFontSpacing
            }
            "atLeast" -> {
                val minHeight = if (spacing.lineValue > 0) twipsToPoints(spacing.lineValue) else 0f
                max(maxFontSpacing, minHeight)
            }
            else -> {
                val multiplier = if (spacing.lineValue > 0) spacing.lineValue / 240f
                else DEFAULT_LINE_SPACING_VALUE / 240f
                maxFontSpacing * multiplier
            }
        }
    }

    private fun calculateEmptyLineHeight(paragraph: DocParagraph): Float {
        val fontSize = getParaFontSize(paragraph)
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = fontSize
            typeface = Typeface.DEFAULT
        }
        return calculateLineHeight(paint, paragraph.spacing)
    }

    private fun getParaFontSize(paragraph: DocParagraph): Float {
        if (paragraph.isHeading) {
            return when (paragraph.headingLevel) {
                1 -> 26f; 2 -> 21f; 3 -> 16f; else -> 14f
            }
        }
        paragraph.runs.firstOrNull { it.fontSize > 0 }?.let {
            return halfPtToPoints(it.fontSize)
        }
        val resolved = resolveStyle(paragraph.styleId)
        if (resolved.fontSizeHalfPt > 0) return halfPtToPoints(resolved.fontSizeHalfPt)
        return defaultFontSize
    }

    // ✅ FIX #3 helper: Get max ascent for a line of runs
    private fun getLineAscent(lineRuns: List<TextRun>, para: DocParagraph): Float {
        var maxAscent = 0f
        for (run in lineRuns) {
            if (run.text == "\n" || run.text.isBlank()) continue
            val paint = makeRunPaint(run, para)
            maxAscent = max(maxAscent, abs(paint.fontMetrics.ascent))
        }
        if (maxAscent == 0f) {
            val refPaint = Paint().apply {
                isAntiAlias = true
                textSize = getParaFontSize(para)
                typeface = Typeface.DEFAULT
            }
            maxAscent = abs(refPaint.fontMetrics.ascent)
        }
        return maxAscent
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ RENDER TO PDF  (FIX #2: Inter-paragraph spacing)
    // ═══════════════════════════════════════════════════════════
    private fun renderToPdf(
        paragraphs: List<DocParagraph>,
        outputFileName: String,
        onProgress: (Float) -> Unit
    ): WordToPdfResult {

        val pdfDocument = PdfDocument()
        val marginLeft = twipsToPoints(pageMargins.left)
        val marginRight = twipsToPoints(pageMargins.right)
        val marginTop = twipsToPoints(pageMargins.top)
        val marginBottom = twipsToPoints(pageMargins.bottom)
        val contentWidth = A4_WIDTH - marginLeft - marginRight

        var pageNumber = 1
        var yPos = marginTop
        var numberedIdx = 0
        var prevSpaceAfterPt = 0f
        var isFirstOnPage = true

        var page = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
        )
        var canvas = page.canvas
        val total = paragraphs.size

        fun startNewPage() {
            pdfDocument.finishPage(page)
            pageNumber++
            page = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
            )
            canvas = page.canvas
            yPos = marginTop
            prevSpaceAfterPt = 0f
            isFirstOnPage = true
        }

        for ((idx, para) in paragraphs.withIndex()) {

            // ── Page break ──────────────────────
            if (para.isPageBreakBefore && !isFirstOnPage) {
                startNewPage()
            }

            // ── Spacing values in POINTS ────────
            val spaceBeforePt = twipsToPoints(
                if (para.spacing.beforeTwips >= 0) para.spacing.beforeTwips
                else DEFAULT_SPACE_BEFORE_TWIPS
            )
            val spaceAfterPt = twipsToPoints(
                if (para.spacing.afterTwips >= 0) para.spacing.afterTwips
                else DEFAULT_SPACE_AFTER_TWIPS
            )

            // ✅ FIX #2: Word adds BOTH spaceAfter(prev) + spaceBefore(current).
            // No CSS-style margin collapsing. This is how Word actually works.
            val spaceBetween = if (isFirstOnPage) 0f
            else prevSpaceAfterPt + spaceBeforePt

            // ═══════════════════════════════════
            // EMPTY PARAGRAPH
            // ═══════════════════════════════════
            if (!para.hasText()) {
                val emptyHeight = calculateEmptyLineHeight(para)
                val totalNeeded = spaceBetween + emptyHeight

                if (yPos + totalNeeded > A4_HEIGHT - marginBottom) {
                    startNewPage()
                } else {
                    yPos += spaceBetween   // ✅ FIX: was effectiveSpaceBefore
                    isFirstOnPage = false
                }

                yPos += emptyHeight
                prevSpaceAfterPt = spaceAfterPt
                if (!para.isNumbered) numberedIdx = 0

                onProgress(0.5f + (idx.toFloat() / total) * 0.4f)
                continue
            }

            // ═══════════════════════════════════
            // TEXT PARAGRAPH
            // ═══════════════════════════════════

            // Build prefix
            val prefix = buildString {
                if (para.isBullet) append("  •  ")
                if (para.isNumbered) { numberedIdx++; append("  $numberedIdx.  ") }
                else if (!para.isBullet) numberedIdx = 0
            }

            // Build all display runs
            val displayRuns = mutableListOf<TextRun>()
            if (prefix.isNotEmpty()) {
                val first = para.runs.firstOrNull { it.text.isNotBlank() }
                displayRuns.add(TextRun(prefix, first?.isBold ?: false, fontSize = first?.fontSize ?: -1f))
            }
            for (run in para.runs) {
                if (run.text.isNotEmpty()) displayRuns.add(run)
            }

            if (displayRuns.all { it.text.isBlank() || it.text == "\n" }) {
                yPos += calculateEmptyLineHeight(para)
                prevSpaceAfterPt = spaceAfterPt
                continue
            }

            // ── Indentation in points ──
            val indentLeft = twipsToPoints(para.indent.leftTwips)
            val indentRight = twipsToPoints(para.indent.rightTwips)
            val indentFirst = twipsToPoints(para.indent.firstLineTwips)
            val indentHanging = twipsToPoints(para.indent.hangingTwips)

            val availWidth = contentWidth - indentLeft - indentRight

            // ── Layout lines ────────────────────
            val lines = layoutLines(displayRuns, para, availWidth)

            if (lines.isEmpty()) {
                yPos += calculateEmptyLineHeight(para)
                prevSpaceAfterPt = spaceAfterPt
                continue
            }

            // ✅ FIX #3: Calculate per-line heights for accuracy
            val lineHeights = lines.map { lineRuns ->
                calculateLineHeightForRuns(lineRuns, para, para.spacing)
            }
            val blockHeight = lineHeights.sum()

            // ── Check page overflow ─────────────
            if (yPos + spaceBetween + blockHeight > A4_HEIGHT - marginBottom) {
                val firstLineH = lineHeights.firstOrNull() ?: blockHeight
                if (yPos + spaceBetween + firstLineH > A4_HEIGHT - marginBottom) {
                    startNewPage()
                }
            }

            // ── Apply space between paragraphs ──
            if (!isFirstOnPage) {
                yPos += prevSpaceAfterPt + spaceBeforePt   // ✅ FIX #2
            }
            isFirstOnPage = false

            // ── Draw lines ──────────────────────
            val paraFontSize = getParaFontSize(para)
            var lineYStart = yPos

            for ((lineIdx, lineRuns) in lines.withIndex()) {
                val lineH = lineHeights[lineIdx]
                val ascent = getLineAscent(lineRuns, para)

                // Baseline positioned within the line box
                val baseline = lineYStart + ascent

                // Check page break mid-paragraph
                if (baseline + (lineH - ascent) > A4_HEIGHT - marginBottom) {
                    startNewPage()
                    isFirstOnPage = false
                    lineYStart = yPos
                    val newBaseline = lineYStart + ascent
                    drawLine(
                        canvas, lineRuns, para, newBaseline,
                        marginLeft, indentLeft, indentFirst, indentHanging,
                        contentWidth, indentRight, lineIdx, paraFontSize
                    )
                    lineYStart += lineH
                    yPos = lineYStart
                    continue
                }

                drawLine(
                    canvas, lineRuns, para, baseline,
                    marginLeft, indentLeft, indentFirst, indentHanging,
                    contentWidth, indentRight, lineIdx, paraFontSize
                )
                lineYStart += lineH
            }

            // ✅ Update yPos to AFTER the last line
            yPos = lineYStart
            prevSpaceAfterPt = spaceAfterPt

            onProgress(0.5f + (idx.toFloat() / total) * 0.4f)
        }

        pdfDocument.finishPage(page)
        onProgress(0.95f)

        val cacheDir = File(context.cacheDir, "pdf_output").apply { mkdirs() }
        val tempFile = File(cacheDir, "$outputFileName.pdf")
        FileOutputStream(tempFile).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        onProgress(1f)

        return WordToPdfResult(true, tempFile, pageCount = pageNumber)
    }

    // ═══════════════════════════════════════════
    // ✅ Draw a single line of mixed-format runs
    // ═══════════════════════════════════════════
    private fun drawLine(
        canvas: Canvas,
        lineRuns: List<TextRun>,
        para: DocParagraph,
        baseline: Float,
        marginLeft: Float,
        indentLeft: Float,
        indentFirst: Float,
        indentHanging: Float,
        contentWidth: Float,
        indentRight: Float,
        lineIndex: Int,
        paraFontSize: Float
    ) {
        // Calculate total width for alignment
        var totalWidth = 0f
        val paintedRuns = lineRuns.map { run ->
            val paint = makeRunPaint(run, para)
            val w = paint.measureText(run.text)
            totalWidth += w
            Triple(run, paint, w)
        }

        // Calculate X start position
        val lineIndent = if (lineIndex == 0) {
            if (indentHanging > 0) -indentHanging else indentFirst
        } else 0f

        val baseX = marginLeft + indentLeft + lineIndent

        val startX = when (para.alignment) {
            "center" -> marginLeft + (contentWidth - totalWidth) / 2
            "right" -> marginLeft + contentWidth - indentRight - totalWidth
            else -> baseX
        }

        // Draw each run
        var xPos = startX
        for ((run, paint, width) in paintedRuns) {
            if (run.text.isEmpty() || run.text == "\n") continue

            // Superscript/Subscript offset
            val yOffset = when {
                run.isSuperscript -> -(paraFontSize * 0.35f)
                run.isSubscript -> (paraFontSize * 0.2f)
                else -> 0f
            }

            canvas.drawText(run.text, xPos, baseline + yOffset, paint)

            // Underline
            if (run.isUnderline) {
                val ulY = baseline + yOffset + paraFontSize * 0.12f
                canvas.drawLine(xPos, ulY, xPos + width, ulY,
                    Paint().apply {
                        isAntiAlias = true; color = paint.color
                        style = Paint.Style.STROKE; strokeWidth = paraFontSize * 0.05f
                    }
                )
            }

            // Strikethrough
            if (run.isStrikethrough) {
                val stY = baseline + yOffset - paraFontSize * 0.22f
                canvas.drawLine(xPos, stY, xPos + width, stY,
                    Paint().apply {
                        isAntiAlias = true; color = paint.color
                        style = Paint.Style.STROKE; strokeWidth = paraFontSize * 0.04f
                    }
                )
            }

            xPos += width
        }
    }

    // ═══════════════════════════════════════════
    // Layout runs into lines (word wrapping)
    // ═══════════════════════════════════════════
    private fun layoutLines(
        runs: List<TextRun>,
        para: DocParagraph,
        maxWidth: Float
    ): List<List<TextRun>> {

        val lines = mutableListOf<List<TextRun>>()
        var currentLine = mutableListOf<TextRun>()
        var currentWidth = 0f

        for (run in runs) {
            if (run.text == "\n") {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toList())
                currentLine = mutableListOf()
                currentWidth = 0f
                continue
            }

            val paint = makeRunPaint(run, para)

            // Handle tabs
            val segments = run.text.split("\t")
            for ((segIdx, segment) in segments.withIndex()) {
                // Add tab space
                if (segIdx > 0) {
                    val tabStop = ((currentWidth / 36f).toInt() + 1) * 36f
                    val tabWidth = tabStop - currentWidth
                    currentLine.add(
                        run.copy(
                            text = " ".repeat(
                                (tabWidth / paint.measureText(" ")).toInt().coerceAtLeast(1)
                            )
                        )
                    )
                    currentWidth = tabStop
                }

                val words = segment.split(" ")
                for ((wordIdx, word) in words.withIndex()) {
                    if (word.isEmpty() && wordIdx > 0) continue

                    val needsSpace = wordIdx > 0 || (currentLine.isNotEmpty() &&
                            currentLine.last().text.isNotEmpty() &&
                            !currentLine.last().text.endsWith(" "))

                    val displayWord = if (needsSpace) " $word" else word
                    val wordWidth = paint.measureText(displayWord)

                    if (currentWidth + wordWidth <= maxWidth || currentLine.isEmpty()) {
                        currentLine.add(run.copy(text = displayWord))
                        currentWidth += wordWidth
                    } else {
                        if (currentLine.isNotEmpty()) lines.add(currentLine.toList())
                        currentLine = mutableListOf(run.copy(text = word))
                        currentWidth = paint.measureText(word)
                    }
                }
            }
        }

        if (currentLine.isNotEmpty()) lines.add(currentLine.toList())
        return lines
    }

    // ═══════════════════════════════════════════
    // Create Paint for a run
    // ═══════════════════════════════════════════
    private fun makeRunPaint(run: TextRun, para: DocParagraph): Paint {
        val baseFontSize = if (run.fontSize > 0) halfPtToPoints(run.fontSize)
        else getParaFontSize(para)

        val size = if (run.isSuperscript || run.isSubscript) baseFontSize * 0.65f
        else baseFontSize

        return Paint().apply {
            isAntiAlias = true
            textSize = size
            color = if (run.fontColor >= 0) run.fontColor else Color.BLACK
            typeface = when {
                run.isBold && run.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                run.isBold -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                run.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                else -> Typeface.DEFAULT
            }
        }
    }

    private fun attr(parser: XmlPullParser, name: String): String {
        return parser.getAttributeValue(null, "w:$name")
            ?: parser.getAttributeValue(null, name)
            ?: ""
    }
}