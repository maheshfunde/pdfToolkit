package com.yashodatech.pdftoolkit

import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.components.PrecisionGradientButton
import com.yashodatech.pdftoolkit.theme.GradientViewVibrant
import com.yashodatech.pdftoolkit.theme.ErrorRed
import com.yashodatech.pdftoolkit.theme.InkBorder
import com.yashodatech.pdftoolkit.theme.InkFaint
import com.yashodatech.pdftoolkit.theme.PrecisionInverseOnSurface
import com.yashodatech.pdftoolkit.theme.PrecisionInverseSurface
import com.yashodatech.pdftoolkit.theme.PrecisionOnSurface
import com.yashodatech.pdftoolkit.theme.PrecisionSurface
import com.yashodatech.pdftoolkit.theme.DarkBackground
import com.yashodatech.pdftoolkit.theme.DarkPrimarySurface
import com.yashodatech.pdftoolkit.theme.DarkTextPrimary
import com.yashodatech.pdftoolkit.theme.ToolSecurityAccent
import com.yashodatech.pdftoolkit.theme.Vermilion
import com.yashodatech.pdftoolkit.theme.VermilionDeep
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.itextpdf.kernel.exceptions.BadPasswordException
import com.yashodatech.pdftoolkit.pdf.PdfUnlocker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.RecentDoc
import android.util.LruCache
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itextpdf.kernel.pdf.PdfDocument as ITextPdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.yashodatech.pdftoolkit.pdf.PdfDocumentMetadata
import com.yashodatech.pdftoolkit.pdf.PdfOutlineExtractor
import com.yashodatech.pdftoolkit.pdf.PdfOutlineItem
import com.yashodatech.pdftoolkit.pdf.PdfPrintHelper
import com.yashodatech.pdftoolkit.pdf.PdfTtsManager
import com.yashodatech.pdftoolkit.pdf.TtsAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy


private val GradientView = listOf(Vermilion, VermilionDeep)
private val AccentView = Vermilion

enum class ViewMode { CONTINUOUS, SINGLE_PAGE, HORIZONTAL }

enum class DrawingTool { PEN, HIGHLIGHTER, ERASER }

data class DrawingPoint(val xRatio: Float, val yRatio: Float)

data class DrawingStroke(
    val points: List<DrawingPoint>,
    val color: Color,
    val strokeWidth: Float,
    val isHighlighter: Boolean
)

// ── Reading surfaces, folded into the Precision (Light-primary) system ──
// The reader is the one surface that renders actual page "paper", so its
// three moods sit on the platform's own paper/ink tiers — Light paper,
// the system's inverse (Night) dark, and an aged Sepia stock. Text stays a
// declared ink so nothing is invisible against its surface in any theme.
private val PaperLight   = Color(0xFFFFFFFF)     // pure white paper
private val PaperInk     = Color(0xFF111827)     // crisp dark text
private val NightBg      = DarkBackground        // pure dark background #0D0E10
private val NightInk     = DarkTextPrimary       // pure dark-mode white text #F9FAFB
private val SepiaBg      = Color(0xFFE8D4B8)     // aged paper
private val SepiaInk     = Color(0xFF5B4636)     // sepia-ink text

// Raised surfaces sit one step off the page (cards, bars, chips).
private val LightPageSurface = Color(0xFFFFFFFF)   // white paper surface
private val NightPageSurface = DarkPrimarySurface  // pure dark surface #16181C (NO navy blue!)
private val SepiaPageSurface = Color(0xFFE0C9A6)   // deeper aged paper

private fun readingPageBg(theme: ReadingTheme) = when (theme) {
    ReadingTheme.LIGHT -> PaperLight
    ReadingTheme.DARK -> NightBg
    ReadingTheme.SEPIA -> SepiaBg
}
private fun readingSurface(theme: ReadingTheme) = when (theme) {
    ReadingTheme.LIGHT -> LightPageSurface
    ReadingTheme.DARK -> NightPageSurface
    ReadingTheme.SEPIA -> SepiaPageSurface
}
private fun readingInk(theme: ReadingTheme) = when (theme) {
    ReadingTheme.LIGHT -> PaperInk
    ReadingTheme.DARK -> NightInk
    ReadingTheme.SEPIA -> SepiaInk
}
private fun readingInk(theme: ReadingTheme, alpha: Float) =
    readingInk(theme).copy(alpha = alpha)

enum class ReadingTheme(
    val label: String,
    val bgColor: Color,
    val icon: ImageVector
) {
    LIGHT("Light", PaperLight, Icons.Rounded.LightMode),
    DARK("Dark", NightBg, Icons.Rounded.DarkMode),
    SEPIA("Sepia", SepiaBg, Icons.Rounded.Palette)
}

data class SearchResult(
    val pageIndex: Int,
    val snippet: String,
    val matchCount: Int
)
data class HighlightRect(
    val leftRatio: Float,
    val topRatio: Float,
    val widthRatio: Float,
    val heightRatio: Float
)

// ═══════════════════════════════════════════════════════════
//  OVERFLOW BOTTOM SHEET ROWS (Reading / Navigate / Share)
//  Grouped sheet, folded into the reading surface.
// ═══════════════════════════════════════════════════════════

/** Section eyebrow inside the overflow sheet — small vermilion group label. */
@Composable
private fun SheetSectionLabel(label: String, readingTheme: ReadingTheme) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = Vermilion,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 2.dp)
    )
}

/** One actionable sheet row: soft tonal icon tile + label, folded into surfaces. */
@Composable
private fun SheetActionRow(
    title: String,
    icon: ImageVector,
    readingTheme: ReadingTheme,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (accent) Vermilion.copy(alpha = 0.12f)
                    else readingInk(readingTheme, 0.08f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (accent) Vermilion else readingInk(readingTheme, 0.75f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = readingInk(readingTheme)
        )
    }
}


// ═══════════════════════════════════════════════════════════
//  PDF RENDERER HOLDER
// ═══════════════════════════════════════════════════════════
class PdfRendererHolder(
    private val fileDescriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer
) {
    val pageCount: Int get() = renderer.pageCount
    private val mutex = Mutex()

    // Cache up to 1/6th of app heap or at least 1MB
    private val maxCacheMemory = (Runtime.getRuntime().maxMemory() / 1024 / 6).toInt().coerceAtLeast(1024)
    private val pageCache = object : LruCache<String, Bitmap>(maxCacheMemory) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    companion object {
        private const val MAX_PIXELS = 1440 * 1920
    }

    suspend fun renderPage(pageIndex: Int, quality: Float = 2.0f, rotation: Int = 0): Bitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount) return null
        val cacheKey = "$pageIndex-$quality-$rotation"
        pageCache.get(cacheKey)?.let {
            if (!it.isRecycled) return it
        }

        return mutex.withLock {
            pageCache.get(cacheKey)?.let {
                if (!it.isRecycled) return it
            }
            try {
                val page = renderer.openPage(pageIndex)
                val origW = page.width; val origH = page.height
                val safeScale = calculateSafeScale(origW, origH, quality)
                val isRotated90or270 = (rotation % 180 != 0)
                val renderW = ((if (isRotated90or270) origH else origW) * safeScale).toInt().coerceAtLeast(1)
                val renderH = ((if (isRotated90or270) origW else origH) * safeScale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(AndroidColor.WHITE)
                val matrix = Matrix().apply {
                    setScale(safeScale, safeScale)
                    if (rotation % 360 != 0) {
                        postRotate(rotation.toFloat())
                        when (rotation % 360) {
                            90, -270 -> postTranslate(renderW.toFloat(), 0f)
                            180, -180 -> postTranslate(renderW.toFloat(), renderH.toFloat())
                            270, -90 -> postTranslate(0f, renderH.toFloat())
                        }
                    }
                }
                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pageCache.put(cacheKey, bitmap)
                bitmap
            } catch (_: OutOfMemoryError) {
                pageCache.evictAll()
                System.gc()
                try {
                    val lowRes = renderPageLowRes(pageIndex, rotation)
                    if (lowRes != null) {
                        pageCache.put(cacheKey, lowRes)
                    }
                    lowRes
                } catch (_: Throwable) { null }
            } catch (_: Throwable) { null }
        }
    }

    private fun renderPageLowRes(pageIndex: Int, rotation: Int = 0): Bitmap? {
        val page = renderer.openPage(pageIndex)
        val origW = page.width; val origH = page.height
        val scale = calculateSafeScale(origW, origH, 0.75f)
        val isRotated90or270 = (rotation % 180 != 0)
        val renderW = ((if (isRotated90or270) origH else origW) * scale).toInt().coerceAtLeast(1)
        val renderH = ((if (isRotated90or270) origW else origH) * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(AndroidColor.WHITE)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            if (rotation % 360 != 0) {
                postRotate(rotation.toFloat())
                when (rotation % 360) {
                    90, -270 -> postTranslate(renderW.toFloat(), 0f)
                    180, -180 -> postTranslate(renderW.toFloat(), renderH.toFloat())
                    270, -90 -> postTranslate(0f, renderH.toFloat())
                }
            }
        }
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    private fun calculateSafeScale(origW: Int, origH: Int, requested: Float): Float {
        val pixels = origW * requested * origH * requested
        return if (pixels > MAX_PIXELS) {
            Math.sqrt(MAX_PIXELS.toDouble() / (origW.toDouble() * origH.toDouble()))
                .toFloat().coerceAtMost(requested)
        } else requested
    }

    fun close() {
        try { pageCache.evictAll() } catch (_: Exception) {}
        try { renderer.close() } catch (_: Exception) {}
        try { fileDescriptor.close() } catch (_: Exception) {}
    }
}


// ═══════════════════════════════════════════════════════════
//  TEXT EXTRACTION & SEARCH
// ═══════════════════════════════════════════════════════════
suspend fun extractTextFromPdf(
    context: Context,
    uri: Uri
): Map<Int, String> = withContext(Dispatchers.IO) {
    val pageTexts = mutableMapOf<Int, String>()
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext pageTexts
        val reader = PdfReader(inputStream)
        val pdfDoc = ITextPdfDocument(reader)
        val totalPages = pdfDoc.numberOfPages
        for (i in 1..totalPages) {
            try {
                pageTexts[i - 1] = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i))
            } catch (_: Exception) {
                pageTexts[i - 1] = ""
            }
        }
        pdfDoc.close()
        reader.close()
        inputStream.close()
    } catch (_: Exception) {}
    pageTexts
}

fun searchInPages(
    pageTexts: Map<Int, String>,
    query: String
): List<SearchResult> {
    if (query.isBlank()) return emptyList()
    val lowerQuery = query.lowercase()
    return pageTexts.entries
        .filter { it.value.lowercase().contains(lowerQuery) }
        .map { (pageIndex, text) ->
            val lowerText = text.lowercase()
            var count = 0
            var idx = lowerText.indexOf(lowerQuery)
            val firstIdx = idx
            while (idx >= 0) {
                count++
                idx = lowerText.indexOf(lowerQuery, idx + 1)
            }
            val snippetStart = (firstIdx - 40).coerceAtLeast(0)
            val snippetEnd = (firstIdx + query.length + 40).coerceAtMost(text.length)
            val snippet = (if (snippetStart > 0) "…" else "") +
                    text.substring(snippetStart, snippetEnd).replace("\n", " ") +
                    (if (snippetEnd < text.length) "…" else "")
            SearchResult(pageIndex = pageIndex, snippet = snippet, matchCount = count)
        }
        .sortedBy { it.pageIndex }
}

class HighlightExtractionStrategy(
    private val query: String
) : LocationTextExtractionStrategy() {

    val highlights = mutableListOf<Rectangle>()

    override fun eventOccurred(
        data: IEventData,
        type: EventType
    ) {
        super.eventOccurred(data, type)

        if (type == EventType.RENDER_TEXT) {
            val renderInfo = data as TextRenderInfo
            val text = renderInfo.text ?: return
            if (text.contains(query, ignoreCase = true)) {
                try {
                    val ascent = renderInfo.ascentLine
                    val descent = renderInfo.descentLine
                    val minX = minOf(ascent.startPoint.get(0), descent.startPoint.get(0))
                    val maxX = maxOf(ascent.endPoint.get(0), descent.endPoint.get(0))
                    val minY = minOf(descent.startPoint.get(1), descent.endPoint.get(1))
                    val maxY = maxOf(ascent.startPoint.get(1), descent.endPoint.get(1))
                    highlights.add(Rectangle(minX, minY, maxOf(maxX - minX, 2f), maxOf(maxY - minY, 8f)))
                } catch (_: Throwable) {
                    val rect = renderInfo.baseline.boundingRectangle
                    highlights.add(rect)
                }
            }
        }
    }
}

suspend fun extractSearchHighlights(
    context: Context,
    uri: Uri,
    query: String,
    matchingPageIndices: List<Int>
): Map<Int, List<HighlightRect>> = withContext(Dispatchers.IO) {
    val result = mutableMapOf<Int, List<HighlightRect>>()
    if (query.isBlank() || matchingPageIndices.isEmpty()) return@withContext result
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext result
        val reader = PdfReader(inputStream)
        val pdfDoc = ITextPdfDocument(reader)

        for (pageIndex in matchingPageIndices) {
            val pageNum = pageIndex + 1
            if (pageNum > pdfDoc.numberOfPages) continue
            try {
                val page = pdfDoc.getPage(pageNum)
                val pageSize = page.pageSize
                val pageWidth = pageSize.width
                val pageHeight = pageSize.height
                val strategy = HighlightExtractionStrategy(query)
                PdfTextExtractor.getTextFromPage(page, strategy)
                val rects = strategy.highlights.map { rect ->
                    HighlightRect(
                        leftRatio = (rect.x / pageWidth).coerceIn(0f, 1f),
                        topRatio = ((pageHeight - (rect.y + rect.height)) / pageHeight).coerceIn(0f, 1f),
                        widthRatio = (rect.width / pageWidth).coerceIn(0f, 1f),
                        heightRatio = (rect.height / pageHeight).coerceIn(0f, 1f)
                    )
                }
                if (rects.isNotEmpty()) {
                    result[pageIndex] = rects
                }
            } catch (_: Exception) {}
        }
        pdfDoc.close()
        reader.close()
        inputStream.close()
    } catch (_: Exception) {}
    result
}


// ═══════════════════════════════════════════════════════════
//  MAIN SCREEN
// ═══════════════════════════════════════════════════════════
fun sharePdfWithAppLink(context: Context, uri: Uri?, fileName: String) {
    if (uri == null) {
        Toast.makeText(context, "No PDF to share", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "Shared via PDF Toolkit. Edit, Compress, Merge & View PDFs on Android:\n${Config.PLAY_STORE_URL}"
            )
            putExtra(Intent.EXTRA_SUBJECT, fileName.ifBlank { "Document.pdf" })
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot share PDF: ${e.localizedMessage ?: "Error"}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewPdfScreen(
    onBack: () -> Unit,
    initialUri: Uri? = null,
    onNavigateToTool: (String) -> Unit = {}
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var sourceSizeText by remember { mutableStateOf("") }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var openStatus by remember { mutableStateOf("Opening…") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rendererHolder by remember { mutableStateOf<PdfRendererHolder?>(null) }
    val listState = rememberLazyListState()

    var viewMode by remember { mutableStateOf(ViewMode.CONTINUOUS) }
    var currentPage by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    var zoomJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    fun animateZoom(targetScale: Float, targetOffsetX: Float = 0f, targetOffsetY: Float = 0f) {
        val clampedTargetScale = targetScale.coerceIn(1f, 5f)
        val maxPanX = if (clampedTargetScale > 1.05f) {
            ((clampedTargetScale - 1f) * viewportSize.width.coerceAtLeast(1)) / 2f
        } else 0f
        val maxPanY = if (clampedTargetScale > 1.05f) {
            ((clampedTargetScale - 1f) * viewportSize.height.coerceAtLeast(1)) / 2f
        } else 0f
        val destX = if (clampedTargetScale <= 1.05f) 0f else targetOffsetX.coerceIn(-maxPanX, maxPanX)
        val destY = if (clampedTargetScale <= 1.05f) 0f else targetOffsetY.coerceIn(-maxPanY, maxPanY)
        val startScale = scale
        val startX = offsetX
        val startY = offsetY

        zoomJob?.cancel()
        zoomJob = scope.launch {
            androidx.compose.animation.core.animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240)
            ) { fraction, _ ->
                scale = startScale + (clampedTargetScale - startScale) * fraction
                offsetX = startX + (destX - startX) * fraction
                offsetY = startY + (destY - startY) * fraction
            }
        }
    }

    fun handleDoubleTap(tapOffset: Offset? = null) {
        if (scale > 1.2f) {
            animateZoom(1f, 0f, 0f)
        } else {
            val targetScale = 2.5f
            if (tapOffset != null && viewportSize.width > 0 && viewportSize.height > 0) {
                val vpCenterX = viewportSize.width / 2f
                val vpCenterY = viewportSize.height / 2f
                val targetX = -(tapOffset.x - vpCenterX) * (targetScale - 1f)
                val targetY = -(tapOffset.y - vpCenterY) * (targetScale - 1f)
                animateZoom(targetScale, targetX, targetY)
                if (viewMode == ViewMode.CONTINUOUS) {
                    val visibleItemTopOffset = listState.firstVisibleItemScrollOffset.toFloat()
                    val deltaScrollY = (visibleItemTopOffset + tapOffset.y) * (targetScale - 1f)
                    scope.launch {
                        listState.animateScrollBy(deltaScrollY)
                    }
                }
            } else {
                animateZoom(targetScale, 0f, 0f)
            }
        }
    }

    // Search
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var pageTexts by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isExtractingText by remember { mutableStateOf(false) }
    var currentSearchResultIndex by remember { mutableIntStateOf(-1) }
    var searchHighlights by remember { mutableStateOf<Map<Int, List<HighlightRect>>>(emptyMap()) }


    // Go to page
    var showGoToPage by remember { mutableStateOf(false) }
    var goToPageText by remember { mutableStateOf("") }

    val isAppDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var readingTheme by remember(isAppDark) {
        mutableStateOf(if (isAppDark) ReadingTheme.DARK else ReadingTheme.LIGHT)
    }
    var showThemeSelector by remember { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(true) }
    var showThumbnails by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Outlines / Table of Contents
    var outlines by remember { mutableStateOf<List<PdfOutlineItem>>(emptyList()) }
    var showOutlineSheet by remember { mutableStateOf(false) }

    // Page Rotation
    var rotationDegrees by remember { mutableIntStateOf(0) }

    // Keep screen on
    var keepScreenOn by remember { mutableStateOf(false) }
    val activity = context as? Activity
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Document Properties
    var documentMetadata by remember { mutableStateOf<PdfDocumentMetadata?>(null) }
    var showMetadataDialog by remember { mutableStateOf(false) }

    // Text-to-Speech (Read Aloud)
    var isTtsActive by remember { mutableStateOf(false) }
    var ttsTrigger by remember { mutableIntStateOf(0) }
    val ttsManager = remember {
        PdfTtsManager(context) { ttsTrigger++ }
    }
    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    // Freehand Drawing / Markup
    var isDrawingMode by remember { mutableStateOf(false) }
    var selectedDrawingTool by remember { mutableStateOf(DrawingTool.PEN) }
    var selectedColor by remember { mutableStateOf(Color(0xFFE53935)) } // Vermilion
    var pageStrokes by remember { mutableStateOf<Map<Int, List<DrawingStroke>>>(emptyMap()) }

    val currentVisiblePage by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    val unlocker = remember { PdfUnlocker(context) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingProtectedUri by remember { mutableStateOf<Uri?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isVerifyingPassword by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { rendererHolder?.close() }
    }

    fun openPdf(uri: Uri, password: String? = null) {
        if (password == null) {
            rendererHolder?.close(); rendererHolder = null
            sourceUri = uri; totalPages = 0; currentPage = 0
            scale = 1f; offsetX = 0f; offsetY = 0f
            errorMessage = null; bookmarks = emptySet()
            pageTexts = emptyMap(); searchResults = emptyList()
            searchQuery = ""; showSearchBar = false
            showSearchResults = false; currentSearchResultIndex = -1
            rotationDegrees = 0
            outlines = emptyList()
            showOutlineSheet = false
            documentMetadata = null
            showMetadataDialog = false
            isDrawingMode = false
            pageStrokes = emptyMap()
            if (isTtsActive) {
                ttsManager.stop()
                isTtsActive = false
            }

            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}

            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) sourceName = cursor.getString(nameIdx) ?: "Unknown.pdf"
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            sourceSizeText = formatViewSize(cursor.getLong(sizeIdx))
                        }
                    }
                }
            } catch (_: Exception) {
                if (sourceName.isBlank()) {
                    sourceName = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
                }
            }
        }

        isLoading = true
        openStatus = if (password != null) "Verifying password…" else "Opening PDF…"
        if (password != null) isVerifyingPassword = true

        scope.launch {
            try {
                val (holder, effectiveUri) = withContext(Dispatchers.IO) {
                    var resolvedUri = uri
                    val fd: ParcelFileDescriptor

                    if (!password.isNullOrEmpty()) {
                        // User provided password — decrypt to temp file and load
                        val tempDecrypted = unlocker.decryptToTempFile(uri, password)
                        resolvedUri = Uri.fromFile(tempDecrypted)
                        fd = ParcelFileDescriptor.open(tempDecrypted, ParcelFileDescriptor.MODE_READ_ONLY)
                    } else {
                        // Try opening directly if not encrypted
                        val directFd = try {
                            context.contentResolver.openFileDescriptor(uri, "r")
                        } catch (_: Exception) {
                            null
                        }

                        var renderedDirectly = false
                        var directHolder: PdfRendererHolder? = null

                        if (directFd != null) {
                            try {
                                val testRenderer = PdfRenderer(directFd)
                                directHolder = PdfRendererHolder(directFd, testRenderer)
                                renderedDirectly = true
                            } catch (_: Exception) {
                                directFd.close()
                            }
                        }

                        if (renderedDirectly && directHolder != null) {
                            return@withContext directHolder to resolvedUri
                        }

                        // Direct open failed or encrypted — copy to cache and probe
                        val cacheFile = java.io.File(context.cacheDir, "view_cache_${System.currentTimeMillis()}.pdf")
                        val stream = try {
                            context.contentResolver.openInputStream(uri)
                        } catch (e: Exception) {
                            if (uri.scheme == "file" && uri.path != null) {
                                java.io.FileInputStream(java.io.File(uri.path!!))
                            } else null
                        } ?: throw Exception("Cannot read PDF document")

                        stream.use { input ->
                            cacheFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        resolvedUri = Uri.fromFile(cacheFile)

                        // Check if file is password-protected
                        val lock = try {
                            unlocker.probeLock(resolvedUri)
                        } catch (_: Exception) {
                            PdfUnlocker.LockState.USER_PASSWORD
                        }

                        if (lock == PdfUnlocker.LockState.USER_PASSWORD) {
                            throw BadPasswordException("Password required")
                        } else if (lock == PdfUnlocker.LockState.PERMISSIONS_ONLY) {
                            val unl = unlocker.decryptToTempFile(resolvedUri, null)
                            resolvedUri = Uri.fromFile(unl)
                            fd = ParcelFileDescriptor.open(unl, ParcelFileDescriptor.MODE_READ_ONLY)
                        } else {
                            fd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        }
                    }

                    PdfRendererHolder(fd, PdfRenderer(fd)) to resolvedUri
                }

                rendererHolder?.close()
                rendererHolder = holder; totalPages = holder.pageCount
                sourceUri = effectiveUri
                pageTexts = emptyMap()
                searchResults = emptyList()
                showSearchResults = false
                currentSearchResultIndex = -1
                searchHighlights = emptyMap()

                android.util.Log.d("PDFToolkit", "openPdf SUCCESS: $effectiveUri, totalPages=$totalPages")
                openStatus = ""
                isLoading = false
                isVerifyingPassword = false
                showPasswordDialog = false
                passwordInput = ""
                passwordError = null
                pendingProtectedUri = null

                // Record in recently opened files
                scope.launch {
                    try {
                        val docName = sourceName.ifBlank {
                            effectiveUri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
                        }
                        PreferencesManager(context).addRecentDoc(
                            RecentDoc(
                                name = docName,
                                uri = effectiveUri.toString(),
                                tool = "View",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } catch (_: Exception) {}
                }

                // Extract Table of Contents in background
                scope.launch {
                    try {
                        outlines = PdfOutlineExtractor.extractOutlines(context, effectiveUri)
                    } catch (_: Exception) {}
                }
            } catch (e: BadPasswordException) {
                android.util.Log.w("PDFToolkit", "Password required/incorrect: ${e.message}")
                isLoading = false
                isVerifyingPassword = false
                pendingProtectedUri = uri
                showPasswordDialog = true
                if (password != null) {
                    passwordError = "Incorrect password — please verify and try again."
                } else {
                    passwordError = null
                }
            } catch (e: Exception) {
                val isEncrypted = e is SecurityException ||
                    e.message?.contains("encrypted", ignoreCase = true) == true ||
                    e.message?.contains("password", ignoreCase = true) == true

                if (isEncrypted) {
                    android.util.Log.w("PDFToolkit", "Document is encrypted: ${e.message}")
                    isLoading = false
                    isVerifyingPassword = false
                    pendingProtectedUri = uri
                    showPasswordDialog = true
                    if (password != null) {
                        passwordError = "Incorrect password — please verify and try again."
                    } else {
                        passwordError = null
                    }
                } else {
                    android.util.Log.e("PDFToolkit", "openPdf FAILED: $uri", e)
                    errorMessage = "Error: ${e.localizedMessage ?: "Cannot open PDF"}"
                    isLoading = false
                    isVerifyingPassword = false
                }
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            openPdf(it)
        }
    }

    val activePdfUri = initialUri

    // Open a URI handed in via navigation or external launch
    LaunchedEffect(activePdfUri) {
        activePdfUri?.let { uri ->
            android.util.Log.d("PDFToolkit", "ViewPdfScreen LaunchedEffect opening: $uri (current sourceUri=$sourceUri)")
            if (sourceUri != uri || rendererHolder == null) {
                openPdf(uri)
            }
        }
    }

    fun goToPage(page: Int) {
        val target = page.coerceIn(0, totalPages - 1)
        if (viewMode == ViewMode.CONTINUOUS) {
            scope.launch { listState.animateScrollToItem(target) }
        } else {
            currentPage = target; scale = 1f; offsetX = 0f; offsetY = 0f
        }
    }

    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            showSearchResults = false
            currentSearchResultIndex = -1
            searchHighlights = emptyMap()
            return
        }
        isSearching = true
        scope.launch {
            if (pageTexts.isEmpty() && sourceUri != null) {
                isExtractingText = true
                try {
                    pageTexts = extractTextFromPdf(context, sourceUri!!)
                } catch (_: Exception) {}
                isExtractingText = false
            }
            val results = withContext(Dispatchers.Default) { searchInPages(pageTexts, query) }
            searchResults = results
            showSearchResults = results.isNotEmpty()
            currentSearchResultIndex = if (results.isNotEmpty()) 0 else -1
            if (results.isNotEmpty()) goToPage(results[0].pageIndex)

            if (results.isNotEmpty() && sourceUri != null) {
                val highlights = extractSearchHighlights(
                    context = context,
                    uri = sourceUri!!,
                    query = query,
                    matchingPageIndices = results.map { it.pageIndex }
                )
                searchHighlights = highlights
            } else {
                searchHighlights = emptyMap()
            }
            isSearching = false
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            showSearchResults = false
            currentSearchResultIndex = -1
            searchHighlights = emptyMap()
        } else if (searchQuery.trim().length >= 2) {
            kotlinx.coroutines.delay(300)
            performSearch(searchQuery.trim())
        }
    }

    fun navigateSearchResult(direction: Int) {
        if (searchResults.isEmpty()) return
        currentSearchResultIndex = (currentSearchResultIndex + direction)
            .coerceIn(0, searchResults.size - 1)
        goToPage(searchResults[currentSearchResultIndex].pageIndex)
    }

    // Automatically hide tools while reading / scrolling
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && showToolbar && !isDrawingMode && !showSearchBar) {
            showToolbar = false
        }
    }

    val bgColor = readingPageBg(readingTheme)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column {
                AnimatedVisibility(
                    visible = showToolbar,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    Column {
                        if (isDrawingMode) {
                            DrawingToolBar(
                                selectedTool = selectedDrawingTool,
                                onSelectTool = { selectedDrawingTool = it },
                                selectedColor = selectedColor,
                                onSelectColor = { selectedColor = it },
                                canUndo = (pageStrokes[currentPage]?.isNotEmpty() == true),
                                onUndo = {
                                    val list = pageStrokes[currentPage] ?: emptyList()
                                    if (list.isNotEmpty()) {
                                        pageStrokes = pageStrokes + (currentPage to list.dropLast(1))
                                    }
                                },
                                onClear = {
                                    pageStrokes = pageStrokes - currentPage
                                },
                                onDone = {
                                    isDrawingMode = false
                                },
                                readingTheme = readingTheme
                            )
                        } else {
                            TopAppBar(
                                title = {
                                    if (sourceUri != null && !isLoading && totalPages > 0) {
                                        Column {
                                            Text(
                                                sourceName, fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp, maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                buildString {
                                                    append("$totalPages pages • $sourceSizeText")
                                                    if (viewMode == ViewMode.CONTINUOUS)
                                                        append(" • Page ${currentVisiblePage + 1}")
                                                    else
                                                        append(" • Page ${currentPage + 1}")
                                                    if (rotationDegrees != 0) {
                                                        append(" • ${rotationDegrees}°")
                                                    }
                                                },
                                                fontSize = 12.sp,
                                                color = readingInk(readingTheme, 0.7f)
                                            )
                                        }
                                    } else {
                                        Text("View PDF", fontWeight = FontWeight.Bold)
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { rendererHolder?.close(); onBack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                    }
                                },
                                actions = {
                                    if (sourceUri != null && !isLoading && totalPages > 0) {
                                        // Table of contents button
                                        IconButton(onClick = { showOutlineSheet = true }) {
                                            Icon(
                                                Icons.Rounded.MenuBook, "Table of Contents",
                                                tint = if (outlines.isNotEmpty()) AccentView
                                                else readingInk(readingTheme, 0.7f)
                                            )
                                        }

                                        // Search toggle
                                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                                            Icon(
                                                Icons.Rounded.Search, "Search",
                                                tint = if (showSearchBar) AccentView
                                                else readingInk(readingTheme, 0.7f)
                                            )
                                        }

                                        // Rotate 90°
                                        IconButton(onClick = {
                                            rotationDegrees = (rotationDegrees + 90) % 360
                                        }) {
                                            Icon(
                                                Icons.Rounded.RotateRight, "Rotate 90°",
                                                tint = if (rotationDegrees != 0) AccentView else readingInk(readingTheme, 0.7f)
                                            )
                                        }

                                        // Draw & Markup
                                        IconButton(onClick = {
                                            isDrawingMode = true
                                            if (viewMode == ViewMode.CONTINUOUS) {
                                                currentPage = currentVisiblePage
                                                viewMode = ViewMode.SINGLE_PAGE
                                            }
                                        }) {
                                            Icon(
                                                Icons.Rounded.Draw, "Draw & Markup",
                                                tint = readingInk(readingTheme, 0.7f)
                                            )
                                        }

                                        // Bookmark
                                        val pageToBookmark =
                                            if (viewMode == ViewMode.CONTINUOUS) currentVisiblePage else currentPage
                                        val isBookmarked = pageToBookmark in bookmarks
                                        IconButton(onClick = {
                                            bookmarks = if (isBookmarked) bookmarks - pageToBookmark
                                            else bookmarks + pageToBookmark
                                            Toast.makeText(
                                                context,
                                                if (!isBookmarked) "Page ${pageToBookmark + 1} bookmarked"
                                                else "Bookmark removed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }) {
                                            Icon(
                                                if (isBookmarked) Icons.Rounded.Bookmark
                                                else Icons.Rounded.BookmarkBorder,
                                                "Bookmark",
                                                tint = if (isBookmarked) AccentView
                                                else readingInk(readingTheme, 0.7f)
                                            )
                                        }

                                        // Share PDF with app link
                                        IconButton(onClick = { sourceUri?.let { sharePdfWithAppLink(context, it, sourceName) } }) {
                                            Icon(Icons.Outlined.Share, "Share PDF", tint = readingInk(readingTheme, 0.85f))
                                        }

                                        // More menu
                                        IconButton(onClick = { showMenu = true }) {
                                            Icon(Icons.Rounded.MoreVert, "More", tint = readingInk(readingTheme, 0.85f))
                                        }
                                        if (showMenu) {
                                            ModalBottomSheet(
                                                onDismissRequest = { showMenu = false },
                                                containerColor = readingSurface(readingTheme),
                                                contentColor = readingInk(readingTheme),
                                                dragHandle = {
                                                    BottomSheetDefaults.DragHandle(
                                                        color = readingInk(readingTheme, 0.3f)
                                                    )
                                                }
                                            ) {
                                                // Header
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Vermilion.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Article,
                                                            null,
                                                            tint = Vermilion,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            "PDF tools & actions",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = readingInk(readingTheme),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            "Read, annotate, listen, and organize",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = readingInk(readingTheme, 0.6f)
                                                        )
                                                    }
                                                }

                                                SheetSectionLabel("Audio & Reading", readingTheme)
                                                SheetActionRow(
                                                    title = if (isTtsActive && ttsManager.isSpeaking) "Pause Read Aloud"
                                                    else if (isTtsActive) "Resume Read Aloud"
                                                    else "Read Aloud (Listen)",
                                                    icon = Icons.Rounded.VolumeUp,
                                                    readingTheme = readingTheme,
                                                    accent = isTtsActive,
                                                    onClick = {
                                                        showMenu = false
                                                        val p = if (viewMode == ViewMode.CONTINUOUS) currentVisiblePage else currentPage
                                                        if (ttsManager.isSpeaking) {
                                                            ttsManager.stop()
                                                        } else {
                                                            val t = pageTexts[p]
                                                            if (!t.isNullOrBlank()) {
                                                                isTtsActive = true
                                                                ttsManager.speak(t)
                                                            } else {
                                                                Toast.makeText(context, "No text on page ${p + 1}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                )
                                                SheetActionRow(
                                                    title = if (keepScreenOn) "Keep Screen On (Active)" else "Keep Screen On",
                                                    icon = Icons.Rounded.Lightbulb,
                                                    readingTheme = readingTheme,
                                                    accent = keepScreenOn,
                                                    onClick = {
                                                        keepScreenOn = !keepScreenOn
                                                        Toast.makeText(
                                                            context,
                                                            if (keepScreenOn) "Screen will stay awake" else "Normal screen timeout restored",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        showMenu = false
                                                    }
                                                )
                                                SheetActionRow(
                                                    title = "Reading Theme",
                                                    icon = Icons.Rounded.Palette,
                                                    readingTheme = readingTheme,
                                                    onClick = { showThemeSelector = true; showMenu = false }
                                                )
                                                SheetActionRow(
                                                    title = if (showThumbnails) "Hide Thumbnails"
                                                    else "Show Thumbnails",
                                                    icon = Icons.Rounded.GridView,
                                                    readingTheme = readingTheme,
                                                    accent = showThumbnails,
                                                    onClick = {
                                                        showThumbnails = !showThumbnails; showMenu = false
                                                    }
                                                )

                                                SheetSectionLabel("View & Navigation", readingTheme)
                                                SheetActionRow(
                                                    title = "Continuous Vertical Scroll",
                                                    icon = Icons.Rounded.ViewDay,
                                                    readingTheme = readingTheme,
                                                    accent = viewMode == ViewMode.CONTINUOUS,
                                                    onClick = {
                                                        if (viewMode != ViewMode.CONTINUOUS) {
                                                            scope.launch { listState.scrollToItem(currentPage) }
                                                            viewMode = ViewMode.CONTINUOUS
                                                            scale = 1f; offsetX = 0f; offsetY = 0f
                                                        }
                                                        showMenu = false
                                                    }
                                                )
                                                SheetActionRow(
                                                    title = "Single Page (Zoom & Pan)",
                                                    icon = Icons.Rounded.ViewAgenda,
                                                    readingTheme = readingTheme,
                                                    accent = viewMode == ViewMode.SINGLE_PAGE,
                                                    onClick = {
                                                        if (viewMode != ViewMode.SINGLE_PAGE) {
                                                            currentPage = if (viewMode == ViewMode.CONTINUOUS) currentVisiblePage else currentPage
                                                            viewMode = ViewMode.SINGLE_PAGE
                                                            scale = 1f; offsetX = 0f; offsetY = 0f
                                                        }
                                                        showMenu = false
                                                    }
                                                )
                                                SheetActionRow(
                                                    title = "Horizontal Page-Flip (Swipe)",
                                                    icon = Icons.Rounded.Swipe,
                                                    readingTheme = readingTheme,
                                                    accent = viewMode == ViewMode.HORIZONTAL,
                                                    onClick = {
                                                        if (viewMode != ViewMode.HORIZONTAL) {
                                                            currentPage = if (viewMode == ViewMode.CONTINUOUS) currentVisiblePage else currentPage
                                                            viewMode = ViewMode.HORIZONTAL
                                                            scale = 1f; offsetX = 0f; offsetY = 0f
                                                        }
                                                        showMenu = false
                                                    }
                                                )
                                                SheetActionRow(
                                                    title = "Table of Contents (${outlines.size})",
                                                    icon = Icons.Rounded.MenuBook,
                                                    readingTheme = readingTheme,
                                                    onClick = { showOutlineSheet = true; showMenu = false }
                                                )
                                                SheetActionRow(
                                                    title = "Go to Page",
                                                    icon = Icons.Rounded.Pin,
                                                    readingTheme = readingTheme,
                                                    onClick = { showGoToPage = true; showMenu = false }
                                                )
                                                if (bookmarks.isNotEmpty()) {
                                                    SheetActionRow(
                                                        title = "Bookmarks (${bookmarks.size})",
                                                        icon = Icons.Rounded.Bookmarks,
                                                        readingTheme = readingTheme,
                                                        onClick = { showBookmarks = true; showMenu = false }
                                                    )
                                                }

                                                SheetSectionLabel("Markup & Tools", readingTheme)
                                                SheetActionRow(
                                                    title = "Draw & Markup Notes",
                                                    icon = Icons.Rounded.Draw,
                                                    readingTheme = readingTheme,
                                                    onClick = {
                                                        isDrawingMode = true
                                                        if (viewMode == ViewMode.CONTINUOUS) {
                                                            currentPage = currentVisiblePage
                                                            viewMode = ViewMode.SINGLE_PAGE
                                                        }
                                                        showMenu = false
                                                    }
                                                )
                                                SheetActionRow(
                                                    title = "Rotate Page (Current: ${rotationDegrees}°)",
                                                    icon = Icons.Rounded.RotateRight,
                                                    readingTheme = readingTheme,
                                                    onClick = {
                                                        rotationDegrees = (rotationDegrees + 90) % 360
                                                        showMenu = false
                                                    }
                                                )
                                                SheetActionRow(
                                                    title = "Document Properties",
                                                    icon = Icons.Rounded.Info,
                                                    readingTheme = readingTheme,
                                                    onClick = {
                                                        scope.launch {
                                                            sourceUri?.let { u ->
                                                                documentMetadata = PdfOutlineExtractor.extractMetadata(
                                                                    context = context,
                                                                    uri = u,
                                                                    pageCount = totalPages,
                                                                    fileSizeText = sourceSizeText
                                                                )
                                                                showMetadataDialog = true
                                                            }
                                                        }
                                                        showMenu = false
                                                    }
                                                )
                                                SheetActionRow(
                                                    title = "Print Document",
                                                    icon = Icons.Rounded.Print,
                                                    readingTheme = readingTheme,
                                                    onClick = {
                                                        sourceUri?.let { u ->
                                                            PdfPrintHelper.printDocument(context, u, sourceName)
                                                        }
                                                        showMenu = false
                                                    }
                                                )

                                                SheetSectionLabel("Share & Export", readingTheme)
                                                SheetActionRow(
                                                    title = "Share PDF (with App Link)",
                                                    icon = Icons.Outlined.Share,
                                                    readingTheme = readingTheme,
                                                    onClick = {
                                                        sourceUri?.let { uri ->
                                                            sharePdfWithAppLink(context, uri, sourceName)
                                                        }
                                                        showMenu = false
                                                    }
                                                )
                                                SheetActionRow(
                                                    title = "Open in Other App",
                                                    icon = Icons.Outlined.OpenInNew,
                                                    readingTheme = readingTheme,
                                                    onClick = {
                                                        sourceUri?.let { uri ->
                                                            try {
                                                                context.startActivity(
                                                                    Intent(Intent.ACTION_VIEW).apply {
                                                                        setDataAndType(uri, "application/pdf")
                                                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                    })
                                                            } catch (_: Exception) {
                                                                Toast.makeText(context, "No app found", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }; showMenu = false
                                                    }
                                                )

                                                Spacer(Modifier.height(8.dp))
                                                Spacer(Modifier.navigationBarsPadding())
                                            }
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = readingSurface(readingTheme),
                                    titleContentColor = readingInk(readingTheme),
                                    navigationIconContentColor = readingInk(readingTheme),
                                    actionIconContentColor = readingInk(readingTheme, 0.85f)
                                )
                            )
                        }

                        // Vermilion hairline — the signature stamp under the bar
                        Box(
                            Modifier.fillMaxWidth().height(2.dp)
                                .background(Brush.horizontalGradient(GradientView))
                        )

                        // Search bar
                        if (showSearchBar) {
                            PdfSearchBar(
                                readingTheme = readingTheme,
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onSearch = { performSearch(searchQuery) },
                                onClose = {
                                    showSearchBar = false
                                    searchQuery = ""
                                    searchResults = emptyList()
                                    showSearchResults = false
                                    currentSearchResultIndex = -1
                                    searchHighlights = emptyMap()
                                },
                                isSearching = isSearching,
                                isExtractingText = isExtractingText,
                                resultCount = searchResults.size,
                                currentResultIdx = currentSearchResultIndex,
                                onPreviousResult = { navigateSearchResult(-1) },
                                onNextResult = { navigateSearchResult(1) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->

        Box(Modifier.fillMaxSize().padding(padding).background(bgColor)) {

            when {
                sourceUri == null -> {
                    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(16.dp))
                        ViewSelectButton { pdfLauncher.launch(arrayOf("application/pdf")) }
                        Spacer(Modifier.height(20.dp))
                        ViewEmptyState(readingTheme = readingTheme)
                    }
                }

                isLoading -> Box(Modifier.fillMaxSize())

                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            val isPasswordIssue = pendingProtectedUri != null ||
                                errorMessage?.contains("password", ignoreCase = true) == true ||
                                errorMessage?.contains("encrypted", ignoreCase = true) == true

                            Icon(
                                if (isPasswordIssue) Icons.Rounded.Lock else Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = if (isPasswordIssue) ToolSecurityAccent else ErrorRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                if (isPasswordIssue) "This document is protected with a password." else errorMessage!!,
                                color = if (isPasswordIssue) MaterialTheme.colorScheme.onSurface else ErrorRed.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                fontSize = 15.sp,
                                fontWeight = if (isPasswordIssue) FontWeight.Medium else FontWeight.Normal
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (isPasswordIssue && pendingProtectedUri != null) {
                                    Button(
                                        onClick = {
                                            passwordError = null
                                            passwordInput = ""
                                            showPasswordDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ToolSecurityAccent)
                                    ) {
                                        Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Enter Password")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }
                                ) {
                                    Text("Try Another File")
                                }
                            }
                        }
                    }
                }

                rendererHolder != null && totalPages > 0 -> {

                    Column(Modifier.fillMaxSize()) {

                        // Search results strip
                        if (showSearchResults && searchResults.isNotEmpty()) {
                            SearchResultsStrip(
                                results = searchResults,
                                query = searchQuery,
                                currentIndex = currentSearchResultIndex,
                                onResultTap = { index ->
                                    currentSearchResultIndex = index
                                    goToPage(searchResults[index].pageIndex)
                                },
                                readingTheme = readingTheme
                            )
                        }

                        // Thumbnails
                        if (showThumbnails) {
                            ThumbnailStrip(
                                rendererHolder = rendererHolder!!,
                                totalPages = totalPages,
                                currentPage = if (viewMode == ViewMode.CONTINUOUS) currentVisiblePage else currentPage,
                                bookmarks = bookmarks,
                                searchPages = searchResults.map { it.pageIndex }.toSet(),
                                onPageTap = { goToPage(it) }
                            )
                        }

                        // Read Aloud / TTS Player Bar
                        AnimatedVisibility(
                            visible = isTtsActive,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                        ) {
                            val activePage = if (viewMode == ViewMode.CONTINUOUS) currentVisiblePage else currentPage
                            TtsPlayerBar(
                                currentPage = activePage,
                                totalPages = totalPages,
                                ttsManager = ttsManager,
                                ttsTrigger = ttsTrigger,
                                pageTexts = pageTexts,
                                onClose = {
                                    ttsManager.stop()
                                    isTtsActive = false
                                },
                                readingTheme = readingTheme
                            )
                        }

                        when (viewMode) {
                            ViewMode.CONTINUOUS -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clipToBounds()
                                        .onSizeChanged { viewportSize = it },
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    val density = LocalDensity.current
                                    val contentWidthDp = if (viewportSize.width > 0) {
                                        with(density) { (viewportSize.width * scale).toDp() }
                                    } else {
                                        androidx.compose.ui.unit.Dp.Unspecified
                                    }

                                    LazyColumn(
                                        state = listState,
                                        // Continuous scrolling is always enabled across all pages
                                        userScrollEnabled = true,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .then(
                                                if (contentWidthDp != androidx.compose.ui.unit.Dp.Unspecified) {
                                                    Modifier.requiredWidth(contentWidthDp)
                                                } else {
                                                    Modifier.fillMaxWidth()
                                                }
                                            )
                                            .graphicsLayer {
                                                translationX = offsetX
                                            }
                                            // ── Pinch-to-zoom via Initial pass ─────────────────────
                                            // Intercept 2-finger pinch gestures to zoom smoothly.
                                            // Anchors the zoom exactly around the pinch centroid focal point.
                                            .pointerInput(Unit) {
                                                awaitEachGesture {
                                                    awaitFirstDown(requireUnconsumed = false)

                                                    do {
                                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                                        val pressed = event.changes.filter { it.pressed }

                                                        if (pressed.size >= 2) {
                                                            // Multi-touch: pinch zoom + pan anchored at touch centroid
                                                            val centroid = event.calculateCentroid(useCurrent = true)
                                                            val zoomChange = event.calculateZoom()
                                                            val panChange = event.calculatePan()

                                                            val oldScale = scale
                                                            val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)
                                                            val effectiveZoom = if (oldScale > 0f) newScale / oldScale else 1f
                                                            scale = newScale

                                                            val vpWidth = viewportSize.width.coerceAtLeast(1).toFloat()
                                                            val vpCenterX = vpWidth / 2f
                                                            val maxPanX = ((newScale - 1f) * vpWidth) / 2f

                                                            if (newScale > 1.05f) {
                                                                val focalX = if (centroid != Offset.Unspecified) centroid.x else vpCenterX
                                                                // Focal-point invariant formula:
                                                                // Keeps the content point under focalX stationary under the user's fingers
                                                                val deltaX = panChange.x - (effectiveZoom - 1f) * (focalX - vpCenterX - offsetX)
                                                                offsetX = (offsetX + deltaX).coerceIn(-maxPanX, maxPanX)
                                                            } else {
                                                                offsetX = 0f
                                                            }

                                                            // Vertical focal-point adjustment:
                                                            if (newScale > 1.001f && centroid != Offset.Unspecified) {
                                                                val focalY = centroid.y
                                                                val visibleItemTopOffset = listState.firstVisibleItemScrollOffset.toFloat()
                                                                val deltaScrollY = (visibleItemTopOffset + focalY) * (effectiveZoom - 1f) - panChange.y
                                                                if (kotlin.math.abs(deltaScrollY) > 0.5f) {
                                                                    listState.dispatchRawDelta(deltaScrollY)
                                                                }
                                                            } else if (kotlin.math.abs(panChange.y) > 0.5f) {
                                                                listState.dispatchRawDelta(-panChange.y)
                                                            }

                                                            // Consume so LazyColumn doesn't scroll erratically during pinch
                                                            event.changes.forEach { if (it.positionChanged()) it.consume() }

                                                        } else if (pressed.size == 1 && scale > 1.05f) {
                                                            // Single-finger drag while zoomed:
                                                            // Only pan horizontally. DO NOT consume event changes here,
                                                            // allowing LazyColumn to handle vertical scrolling through all pages!
                                                            val panChange = event.calculatePan()
                                                            if (kotlin.math.abs(panChange.x) > 0.5f) {
                                                                val maxPanX = ((scale - 1f) * viewportSize.width.coerceAtLeast(1).toFloat()) / 2f
                                                                offsetX = (offsetX + panChange.x).coerceIn(-maxPanX, maxPanX)
                                                            }
                                                        }
                                                    } while (event.changes.any { it.pressed })
                                                }
                                            }
                                            // ── Tap & double-tap (independent of zoom handler) ──
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onTap = { showToolbar = !showToolbar },
                                                    onDoubleTap = { tapOffset -> handleDoubleTap(tapOffset) }
                                                )
                                            },
                                        // Minimal top/bottom padding, no side padding — pages fill edge-to-edge
                                        contentPadding = PaddingValues(
                                            top = 4.dp,
                                            bottom = 4.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                        ),
                                        // Tight 4 dp gap between pages for a book-like feel
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(count = totalPages, key = { it }) { pageIndex ->
                                            val hasSearchHit =
                                                searchResults.any { it.pageIndex == pageIndex }
                                            LazyPageCard(
                                                rendererHolder = rendererHolder!!,
                                                pageIndex = pageIndex,
                                                totalPages = totalPages,
                                                readingTheme = readingTheme,
                                                isBookmarked = pageIndex in bookmarks,
                                                hasSearchHit = hasSearchHit,
                                                searchQuery = if (hasSearchHit) searchQuery else "",
                                                highlights = searchHighlights[pageIndex] ?: emptyList(),
                                                rotation = rotationDegrees,
                                                strokes = pageStrokes[pageIndex] ?: emptyList(),
                                                onToggleBookmark = {
                                                    bookmarks =
                                                        if (pageIndex in bookmarks) bookmarks - pageIndex
                                                        else bookmarks + pageIndex
                                                }
                                            )
                                        }
                                    }

                                    // Zoom indicator
                                    if (scale > 1.05f) {
                                        Surface(
                                            onClick = { animateZoom(1f, 0f, 0f) },
                                            color = NightBg.copy(alpha = 0.92f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.TopEnd)
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                "${(scale * 100).toInt()}% • Reset",
                                                color = NightInk,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = showToolbar && !isDrawingMode,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                ) {
                                    ContinuousBottomBar(
                                        currentPage = currentVisiblePage,
                                        totalPages = totalPages,
                                        onChangeFile = {
                                            pdfLauncher.launch(arrayOf("application/pdf"))
                                        },
                                        readingTheme = readingTheme,
                                        onOrganize = {
                                            sourceUri?.let { onNavigateToTool(Screen.OrganizePages.createRoute(it.toString())) }
                                        },
                                        onWatermark = {
                                            sourceUri?.let { onNavigateToTool(Screen.WatermarkPdf.createRoute(it.toString())) }
                                        },
                                        onCompress = {
                                            sourceUri?.let { onNavigateToTool(Screen.CompressPdf.createRoute(it.toString())) }
                                        },
                                        onSplit = {
                                            sourceUri?.let { onNavigateToTool(Screen.SplitPdf.createRoute(it.toString())) }
                                        },
                                        onMarkup = {
                                            isDrawingMode = true
                                            currentPage = currentVisiblePage
                                            viewMode = ViewMode.SINGLE_PAGE
                                        },
                                        onShare = {
                                            sharePdfWithAppLink(context, sourceUri, sourceName)
                                        },
                                        onMore = { showMenu = true }
                                    )
                                }
                            }

                            ViewMode.SINGLE_PAGE -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clipToBounds()
                                        .onSizeChanged { viewportSize = it }
                                        .pointerInput(isDrawingMode) {
                                            if (!isDrawingMode) {
                                                detectTapGestures(
                                                    onTap = { showToolbar = !showToolbar },
                                                    onDoubleTap = { tapOffset -> handleDoubleTap(tapOffset) }
                                                )
                                            }
                                        }
                                        .pointerInput(isDrawingMode) {
                                            if (!isDrawingMode) {
                                                awaitEachGesture {
                                                    awaitFirstDown(requireUnconsumed = false)
                                                    do {
                                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                                        val pressed = event.changes.filter { it.pressed }
                                                        if (pressed.size >= 2) {
                                                            val centroid = event.calculateCentroid(useCurrent = true)
                                                            val zoomChange = event.calculateZoom()
                                                            val panChange = event.calculatePan()

                                                            val oldScale = scale
                                                            val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)
                                                            val effectiveZoom = if (oldScale > 0f) newScale / oldScale else 1f
                                                            scale = newScale

                                                            val vpWidth = viewportSize.width.coerceAtLeast(1).toFloat()
                                                            val vpHeight = viewportSize.height.coerceAtLeast(1).toFloat()
                                                            val vpCenterX = vpWidth / 2f
                                                            val vpCenterY = vpHeight / 2f

                                                            val maxPanX = ((newScale - 1f) * vpWidth) / 2f
                                                            val maxPanY = ((newScale - 1f) * vpHeight) / 2f

                                                            if (newScale > 1.001f) {
                                                                val focalX = if (centroid != Offset.Unspecified) centroid.x else vpCenterX
                                                                val focalY = if (centroid != Offset.Unspecified) centroid.y else vpCenterY

                                                                val deltaX = panChange.x - (effectiveZoom - 1f) * (focalX - vpCenterX - offsetX)
                                                                val deltaY = panChange.y - (effectiveZoom - 1f) * (focalY - vpCenterY - offsetY)

                                                                offsetX = (offsetX + deltaX).coerceIn(-maxPanX, maxPanX)
                                                                offsetY = (offsetY + deltaY).coerceIn(-maxPanY, maxPanY)
                                                            } else {
                                                                offsetX = 0f
                                                                offsetY = 0f
                                                            }
                                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                        } else if (pressed.size == 1 && scale > 1.05f) {
                                                            val panChange = event.calculatePan()
                                                            val vpWidth = viewportSize.width.coerceAtLeast(1).toFloat()
                                                            val vpHeight = viewportSize.height.coerceAtLeast(1).toFloat()
                                                            val maxPanX = ((scale - 1f) * vpWidth) / 2f
                                                            val maxPanY = ((scale - 1f) * vpHeight) / 2f
                                                            offsetX = (offsetX + panChange.x).coerceIn(-maxPanX, maxPanX)
                                                            offsetY = (offsetY + panChange.y).coerceIn(-maxPanY, maxPanY)
                                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                        }
                                                    } while (event.changes.any { it.pressed })
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {

                                    SinglePageView(
                                        rendererHolder = rendererHolder!!,
                                        pageIndex = currentPage,
                                        scale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        readingTheme = readingTheme,
                                        highlights = searchHighlights[currentPage] ?: emptyList(),
                                        rotation = rotationDegrees,
                                        strokes = pageStrokes[currentPage] ?: emptyList(),
                                        isDrawingMode = isDrawingMode,
                                        selectedDrawingTool = selectedDrawingTool,
                                        selectedColor = selectedColor,
                                        onAddStroke = { stroke ->
                                            val existing = pageStrokes[currentPage] ?: emptyList()
                                            pageStrokes = pageStrokes + (currentPage to (existing + stroke))
                                        },
                                        onEraseStrokeAt = { erasePt ->
                                            val existing = pageStrokes[currentPage] ?: emptyList()
                                            val filtered = existing.filterNot { stroke ->
                                                stroke.points.any { p ->
                                                    val dx = p.xRatio - erasePt.xRatio
                                                    val dy = p.yRatio - erasePt.yRatio
                                                    (dx * dx + dy * dy) < 0.0025f
                                                }
                                            }
                                            if (filtered.size != existing.size) {
                                                pageStrokes = pageStrokes + (currentPage to filtered)
                                            }
                                        },
                                        modifier = Modifier
                                    )

                                    if (scale > 1.05f && !isDrawingMode) {
                                        Surface(
                                            onClick = { animateZoom(1f, 0f, 0f) },
                                            color = NightBg.copy(alpha = 0.92f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.TopEnd)
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                "${(scale * 100).toInt()}% • Reset",
                                                color = NightInk,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = showToolbar && !isDrawingMode,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                ) {
                                    SinglePageNavBar(
                                        currentPage = currentPage,
                                        totalPages = totalPages,
                                        readingTheme = readingTheme,
                                        isBookmarked = currentPage in bookmarks,
                                        onPrevious = {
                                            if (currentPage > 0) {
                                                currentPage--; scale = 1f
                                                offsetX = 0f; offsetY = 0f
                                            }
                                        },
                                        onNext = {
                                            if (currentPage < totalPages - 1) {
                                                currentPage++; scale = 1f
                                                offsetX = 0f; offsetY = 0f
                                            }
                                        },
                                        onToggleBookmark = {
                                            bookmarks =
                                                if (currentPage in bookmarks) bookmarks - currentPage
                                                else bookmarks + currentPage
                                        },
                                        onOrganize = {
                                            sourceUri?.let { onNavigateToTool(Screen.OrganizePages.createRoute(it.toString())) }
                                        },
                                        onWatermark = {
                                            sourceUri?.let { onNavigateToTool(Screen.WatermarkPdf.createRoute(it.toString())) }
                                        },
                                        onCompress = {
                                            sourceUri?.let { onNavigateToTool(Screen.CompressPdf.createRoute(it.toString())) }
                                        },
                                        onSplit = {
                                            sourceUri?.let { onNavigateToTool(Screen.SplitPdf.createRoute(it.toString())) }
                                        },
                                        onMarkup = {
                                            isDrawingMode = true
                                        },
                                        onShare = {
                                            sharePdfWithAppLink(context, sourceUri, sourceName)
                                        },
                                        onMore = { showMenu = true }
                                    )
                                }
                            }

                            ViewMode.HORIZONTAL -> {
                                val pagerState = rememberPagerState(initialPage = currentPage) { totalPages }

                                LaunchedEffect(pagerState.currentPage) {
                                    currentPage = pagerState.currentPage
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                }
                                LaunchedEffect(currentPage) {
                                    if (pagerState.currentPage != currentPage) {
                                        pagerState.scrollToPage(currentPage)
                                    }
                                }
                                LaunchedEffect(pagerState.isScrollInProgress) {
                                    if (pagerState.isScrollInProgress && showToolbar && !isDrawingMode && !showSearchBar) {
                                        showToolbar = false
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clipToBounds()
                                        .onSizeChanged { viewportSize = it }
                                        .pointerInput(isDrawingMode) {
                                            if (!isDrawingMode) {
                                                detectTapGestures(
                                                    onTap = { showToolbar = !showToolbar },
                                                    onDoubleTap = { tapOffset -> handleDoubleTap(tapOffset) }
                                                )
                                            }
                                        }
                                        .pointerInput(isDrawingMode) {
                                            if (!isDrawingMode && scale > 1.05f) {
                                                awaitEachGesture {
                                                    awaitFirstDown(requireUnconsumed = false)
                                                    do {
                                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                                        val pressed = event.changes.filter { it.pressed }
                                                        if (pressed.size >= 2) {
                                                            val centroid = event.calculateCentroid(useCurrent = true)
                                                            val zoomChange = event.calculateZoom()
                                                            val panChange = event.calculatePan()

                                                            val oldScale = scale
                                                            val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)
                                                            val effectiveZoom = if (oldScale > 0f) newScale / oldScale else 1f
                                                            scale = newScale

                                                            val vpWidth = viewportSize.width.coerceAtLeast(1).toFloat()
                                                            val vpHeight = viewportSize.height.coerceAtLeast(1).toFloat()
                                                            val vpCenterX = vpWidth / 2f
                                                            val vpCenterY = vpHeight / 2f

                                                            val maxPanX = ((newScale - 1f) * vpWidth) / 2f
                                                            val maxPanY = ((newScale - 1f) * vpHeight) / 2f

                                                            if (newScale > 1.001f) {
                                                                val focalX = if (centroid != Offset.Unspecified) centroid.x else vpCenterX
                                                                val focalY = if (centroid != Offset.Unspecified) centroid.y else vpCenterY

                                                                val deltaX = panChange.x - (effectiveZoom - 1f) * (focalX - vpCenterX - offsetX)
                                                                val deltaY = panChange.y - (effectiveZoom - 1f) * (focalY - vpCenterY - offsetY)

                                                                offsetX = (offsetX + deltaX).coerceIn(-maxPanX, maxPanX)
                                                                offsetY = (offsetY + deltaY).coerceIn(-maxPanY, maxPanY)
                                                            } else {
                                                                offsetX = 0f
                                                                offsetY = 0f
                                                            }
                                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                        } else if (pressed.size == 1) {
                                                            val panChange = event.calculatePan()
                                                            val vpWidth = viewportSize.width.coerceAtLeast(1).toFloat()
                                                            val vpHeight = viewportSize.height.coerceAtLeast(1).toFloat()
                                                            val maxPanX = ((scale - 1f) * vpWidth) / 2f
                                                            val maxPanY = ((scale - 1f) * vpHeight) / 2f
                                                            offsetX = (offsetX + panChange.x).coerceIn(-maxPanX, maxPanX)
                                                            offsetY = (offsetY + panChange.y).coerceIn(-maxPanY, maxPanY)
                                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                        }
                                                    } while (event.changes.any { it.pressed })
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {

                                    HorizontalPager(
                                        state = pagerState,
                                        userScrollEnabled = !isDrawingMode && (scale <= 1.05f),
                                        modifier = Modifier.fillMaxSize()
                                    ) { pageIdx ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                ,
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SinglePageView(
                                                rendererHolder = rendererHolder!!,
                                                pageIndex = pageIdx,
                                                scale = if (pageIdx == currentPage) scale else 1f,
                                                offsetX = if (pageIdx == currentPage) offsetX else 0f,
                                                offsetY = if (pageIdx == currentPage) offsetY else 0f,
                                                readingTheme = readingTheme,
                                                highlights = searchHighlights[pageIdx] ?: emptyList(),
                                                rotation = rotationDegrees,
                                                strokes = pageStrokes[pageIdx] ?: emptyList(),
                                                isDrawingMode = isDrawingMode && (pageIdx == currentPage),
                                                selectedDrawingTool = selectedDrawingTool,
                                                selectedColor = selectedColor,
                                                onAddStroke = { stroke ->
                                                    val existing = pageStrokes[pageIdx] ?: emptyList()
                                                    pageStrokes = pageStrokes + (pageIdx to (existing + stroke))
                                                },
                                                onEraseStrokeAt = { erasePt ->
                                                    val existing = pageStrokes[pageIdx] ?: emptyList()
                                                    val filtered = existing.filterNot { stroke ->
                                                        stroke.points.any { p ->
                                                            val dx = p.xRatio - erasePt.xRatio
                                                            val dy = p.yRatio - erasePt.yRatio
                                                            (dx * dx + dy * dy) < 0.0025f
                                                        }
                                                    }
                                                    if (filtered.size != existing.size) {
                                                        pageStrokes = pageStrokes + (pageIdx to filtered)
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    if (scale > 1.05f && !isDrawingMode) {
                                        Surface(
                                            onClick = { animateZoom(1f, 0f, 0f) },
                                            color = NightBg.copy(alpha = 0.92f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.TopEnd)
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                "${(scale * 100).toInt()}% • Reset",
                                                color = NightInk,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = showToolbar && !isDrawingMode,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                ) {
                                    SinglePageNavBar(
                                        currentPage = currentPage,
                                        totalPages = totalPages,
                                        readingTheme = readingTheme,
                                        isBookmarked = currentPage in bookmarks,
                                        onPrevious = {
                                            if (currentPage > 0) {
                                                scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                                            }
                                        },
                                        onNext = {
                                            if (currentPage < totalPages - 1) {
                                                scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                                            }
                                        },
                                        onToggleBookmark = {
                                            bookmarks =
                                                if (currentPage in bookmarks) bookmarks - currentPage
                                                else bookmarks + currentPage
                                        },
                                        onOrganize = {
                                            sourceUri?.let { onNavigateToTool(Screen.OrganizePages.createRoute(it.toString())) }
                                        },
                                        onWatermark = {
                                            sourceUri?.let { onNavigateToTool(Screen.WatermarkPdf.createRoute(it.toString())) }
                                        },
                                        onCompress = {
                                            sourceUri?.let { onNavigateToTool(Screen.CompressPdf.createRoute(it.toString())) }
                                        },
                                        onSplit = {
                                            sourceUri?.let { onNavigateToTool(Screen.SplitPdf.createRoute(it.toString())) }
                                        },
                                        onMarkup = {
                                            isDrawingMode = true
                                        },
                                        onShare = {
                                            sharePdfWithAppLink(context, sourceUri, sourceName)
                                        },
                                        onMore = { showMenu = true }
                                    )
                                }
                            }
                        }
                    }

                    // Floating zoom controls
                    if (showToolbar && !isDrawingMode) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 140.dp)
                        ) {
                            FloatingActionButton(
                                onClick = { animateZoom(scale + 0.5f, offsetX, offsetY) },
                                containerColor = AccentView,
                                contentColor = Color.White,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.ZoomIn, "Zoom In",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            FloatingActionButton(
                                onClick = {
                                    animateZoom(scale - 0.5f, offsetX, offsetY)
                                },
                                containerColor = AccentView,
                                contentColor = Color.White,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.ZoomOut, "Zoom Out",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            if (scale > 1.05f) {
                                FloatingActionButton(
                                    onClick = {
                                        animateZoom(1f, 0f, 0f)
                                    },
                                    containerColor = AccentView,
                                    contentColor = Color.White,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.FitScreen, "Fit",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Blocking open modal — holds until the renderer + text index are ready.
        ModernGlassLoader(
            isShowing = isLoading,
            title = if (sourceUri != null) "Opening PDF" else "Preparing viewer",
            statusText = openStatus
        )

        // Go to Page Dialog
        if (showGoToPage) {
            AlertDialog(
                onDismissRequest = { showGoToPage = false },
                shape = RoundedCornerShape(20.dp),
                title = { Text("Go to Page", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Enter page number (1–$totalPages)", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = goToPageText,
                            onValueChange = { goToPageText = it.filter { c -> c.isDigit() } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Page number") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentView, cursorColor = AccentView
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val page = goToPageText.toIntOrNull()
                            if (page != null && page in 1..totalPages) {
                                goToPage(page - 1); showGoToPage = false; goToPageText = ""
                            } else Toast.makeText(context, "Invalid page", Toast.LENGTH_SHORT)
                                .show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentView)
                    ) { Text("Go", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showGoToPage = false; goToPageText = "" }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Theme Selector
        if (showThemeSelector) {
            AlertDialog(
                onDismissRequest = { showThemeSelector = false },
                shape = RoundedCornerShape(20.dp),
                title = { Text("Reading Theme", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReadingTheme.entries.forEach { theme ->
                            Card(
                                onClick = {
                                    readingTheme = theme; showThemeSelector = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (readingTheme == theme) AccentView.copy(
                                        alpha = 0.1f
                                    )
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (readingTheme == theme) BorderStroke(
                                    2.dp,
                                    AccentView
                                ) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier.size(36.dp).clip(CircleShape)
                                            .background(theme.bgColor)
                                            .border(1.dp, readingInk(theme, 0.35f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            theme.icon, null,
                                            modifier = Modifier.size(18.dp),
                                            tint = readingInk(theme)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        theme.label, fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (readingTheme == theme)
                                        Icon(Icons.Rounded.Check, null, tint = AccentView)
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // Bookmarks Dialog
        if (showBookmarks && bookmarks.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showBookmarks = false },
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bookmarks, null, tint = AccentView)
                        Spacer(Modifier.width(8.dp))
                        Text("Bookmarks", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        bookmarks.sorted().forEach { pageIdx ->
                            Card(
                                onClick = { goToPage(pageIdx); showBookmarks = false },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Bookmark, null,
                                        tint = AccentView, modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Page ${pageIdx + 1}",
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.weight(1f))
                                    IconButton(
                                        onClick = { bookmarks = bookmarks - pageIdx },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close, "Remove",
                                            tint = InkFaint(),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBookmarks = false }) {
                        Text("Close", color = AccentView)
                    }
                }
            )
        }

        // Outline / Table of Contents sheet
        if (showOutlineSheet) {
            OutlineBottomSheet(
                outlines = outlines,
                onSelectPage = { page -> goToPage(page) },
                onDismiss = { showOutlineSheet = false },
                readingTheme = readingTheme
            )
        }

        // Document Metadata Dialog
        if (showMetadataDialog && documentMetadata != null) {
            MetadataDialog(
                metadata = documentMetadata,
                onDismiss = { showMetadataDialog = false },
                readingTheme = readingTheme
            )
        }

        // Password Prompt Dialog for protected / encrypted PDFs
        if (showPasswordDialog && pendingProtectedUri != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isVerifyingPassword) {
                        showPasswordDialog = false
                        passwordError = null
                        passwordInput = ""
                    }
                },
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(ToolSecurityAccent.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = ToolSecurityAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Password Protected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "This document is encrypted. Enter the password to unlock and view it.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                passwordError = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter document password") },
                            singleLine = true,
                            enabled = !isVerifyingPassword,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (passwordInput.isNotBlank() && !isVerifyingPassword) {
                                    openPdf(pendingProtectedUri!!, passwordInput)
                                }
                            }),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            isError = passwordError != null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (passwordError != null) {
                            Text(
                                text = passwordError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (passwordInput.isNotBlank() && !isVerifyingPassword) {
                                openPdf(pendingProtectedUri!!, passwordInput)
                            }
                        },
                        enabled = passwordInput.isNotBlank() && !isVerifyingPassword,
                        colors = ButtonDefaults.buttonColors(containerColor = ToolSecurityAccent)
                    ) {
                        if (isVerifyingPassword) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Unlocking…")
                        } else {
                            Text("Unlock & Open")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPasswordDialog = false
                            passwordError = null
                            passwordInput = ""
                        },
                        enabled = !isVerifyingPassword
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  PDF SEARCH BAR
// ═══════════════════════════════════════════════════════════
@Composable
private fun PdfSearchBar(
    readingTheme: ReadingTheme,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClose: () -> Unit,
    isSearching: Boolean,
    isExtractingText: Boolean,
    resultCount: Int,
    currentResultIdx: Int,
    onPreviousResult: () -> Unit,
    onNextResult: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val ink = readingInk(readingTheme)

    Surface(
        color = readingSurface(readingTheme),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ◀ CHANGED: Removed fixed .height(48.dp), added proper sizing
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            if (isExtractingText) "Indexing text…" else "Search in PDF…",
                            fontSize = 14.sp, color = readingInk(readingTheme, 0.5f)
                        )
                    },
                    singleLine = true,
                    enabled = !isExtractingText,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        onSearch(); focusManager.clearFocus()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ink,
                        unfocusedTextColor = ink,
                        cursorColor = AccentView,
                        focusedBorderColor = AccentView.copy(alpha = 0.7f),
                        unfocusedBorderColor = readingInk(readingTheme, 0.35f),
                        disabledTextColor = readingInk(readingTheme, 0.3f),
                        disabledBorderColor = readingInk(readingTheme, 0.2f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),  // ◀ CHANGED: heightIn instead of fixed height
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 15.sp,  // ◀ CHANGED: slightly larger
                        color = ink
                    )
                    // ◀ trailingIcon stays the same
                    ,trailingIcon = {
                        if (isSearching || isExtractingText) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentView, strokeWidth = 2.dp
                            )
                        } else if (query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    Icons.Rounded.Clear, "Clear",
                                    tint = readingInk(readingTheme, 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))

                FilledIconButton(
                    onClick = { onSearch(); focusManager.clearFocus() },
                    enabled = query.isNotBlank() && !isExtractingText,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentView,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.Search, "Search", modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.width(4.dp))

                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Rounded.Close, "Close",
                        tint = readingInk(readingTheme, 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (resultCount > 0) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${currentResultIdx + 1} of $resultCount result${if (resultCount != 1) "s" else ""}",
                        color = readingInk(readingTheme, 0.85f),
                        fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onPreviousResult,
                        enabled = currentResultIdx > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp, "Previous",
                            tint = if (currentResultIdx > 0) readingInk(readingTheme)
                            else readingInk(readingTheme, 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onNextResult,
                        enabled = currentResultIdx < resultCount - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown, "Next",
                            tint = if (currentResultIdx < resultCount - 1) readingInk(readingTheme)
                            else readingInk(readingTheme, 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else if (query.isNotBlank() && !isSearching) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "No results found",
                    color = readingInk(readingTheme, 0.6f), fontSize = 12.sp
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  SEARCH RESULTS STRIP
// ═══════════════════════════════════════════════════════════
@Composable
private fun SearchResultsStrip(
    results: List<SearchResult>,
    query: String,
    currentIndex: Int,
    onResultTap: (Int) -> Unit,
    readingTheme: ReadingTheme
) {
    val stripBg = readingSurface(readingTheme)

    Surface(color = stripBg, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results.size) { index ->
                val result = results[index]
                val isSelected = index == currentIndex

                Card(
                    onClick = { onResultTap(index) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) AccentView.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) BorderStroke(1.5.dp, AccentView) else null,
                    modifier = Modifier.width(220.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(22.dp).clip(CircleShape)
                                    .background(
                                        if (isSelected) AccentView
                                        else AccentView.copy(alpha = 0.3f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${result.pageIndex + 1}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Page ${result.pageIndex + 1}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Surface(
                                color = AccentView.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "${result.matchCount}×",
                                    fontSize = 10.sp,
                                    color = AccentView,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        horizontal = 4.dp,
                                        vertical = 1.dp
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = buildHighlightedSnippet(result.snippet, query),
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun buildHighlightedSnippet(snippet: String, query: String) = buildAnnotatedString {
    if (query.isBlank()) {
        append(snippet); return@buildAnnotatedString
    }
    val lowerSnippet = snippet.lowercase()
    val lowerQuery = query.lowercase()
    var lastIndex = 0
    var idx = lowerSnippet.indexOf(lowerQuery)
    while (idx >= 0) {
        append(snippet.substring(lastIndex, idx))
        withStyle(
            SpanStyle(
                background = AccentView.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold,
                color = AccentView
            )
        ) {
            append(snippet.substring(idx, idx + query.length))
        }
        lastIndex = idx + query.length
        idx = lowerSnippet.indexOf(lowerQuery, lastIndex)
    }
    if (lastIndex < snippet.length) append(snippet.substring(lastIndex))
}


// ═══════════════════════════════════════════════════════════
//  THUMBNAIL STRIP
// ═══════════════════════════════════════════════════════════
@Composable
private fun ThumbnailStrip(
    rendererHolder: PdfRendererHolder,
    totalPages: Int,
    currentPage: Int,
    bookmarks: Set<Int>,
    searchPages: Set<Int>,
    onPageTap: (Int) -> Unit
) {
    val thumbnailListState = rememberLazyListState()

    LaunchedEffect(currentPage) {
        thumbnailListState.animateScrollToItem(currentPage.coerceIn(0, totalPages - 1))
    }

    Surface(
        tonalElevation = 4.dp, shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            state = thumbnailListState,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(totalPages) { pageIndex ->
                ThumbnailItem(
                    rendererHolder = rendererHolder,
                    pageIndex = pageIndex,
                    isSelected = pageIndex == currentPage,
                    isBookmarked = pageIndex in bookmarks,
                    hasSearchHit = pageIndex in searchPages,
                    onClick = { onPageTap(pageIndex) }
                )
            }
        }
    }
}

@Composable
private fun ThumbnailItem(
    rendererHolder: PdfRendererHolder,
    pageIndex: Int,
    isSelected: Boolean,
    isBookmarked: Boolean,
    hasSearchHit: Boolean,
    onClick: () -> Unit
) {
    var thumbnail by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        thumbnail = withContext(Dispatchers.IO) {
            rendererHolder.renderPage(pageIndex, quality = 0.5f)
        }
    }

    Box(
        modifier = Modifier
            .width(60.dp).height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else if (hasSearchHit) 1.5.dp else 1.dp,
                color = if (isSelected) AccentView
                else if (hasSearchHit) AccentView.copy(alpha = 0.8f)
                else InkBorder.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(InkBorder.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AccentView, strokeWidth = 2.dp
                )
            }
        }

        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f)).padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${pageIndex + 1}",
                color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold
            )
        }

        if (hasSearchHit) {
            Box(
                Modifier.size(12.dp).clip(CircleShape)
                    .background(AccentView)
                    .align(Alignment.TopStart).padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Search, null,
                    tint = Color.White, modifier = Modifier.size(8.dp)
                )
            }
        }

        if (isBookmarked) {
            Icon(
                Icons.Rounded.Bookmark, null, tint = AccentView,
                modifier = Modifier.size(14.dp).align(Alignment.TopEnd).padding(2.dp)
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  LAZY PAGE CARD
// ═══════════════════════════════════════════════════════════
@Composable
private fun LazyPageCard(
    rendererHolder: PdfRendererHolder,
    pageIndex: Int,
    totalPages: Int,
    readingTheme: ReadingTheme,
    isBookmarked: Boolean,
    hasSearchHit: Boolean,
    searchQuery: String,
    highlights: List<HighlightRect>,
    rotation: Int = 0,
    strokes: List<DrawingStroke> = emptyList(),
    onToggleBookmark: () -> Unit
) {
    var bitmap by remember(pageIndex, rotation) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex, rotation) { mutableStateOf(true) }
    var hasError by remember(pageIndex, rotation) { mutableStateOf(false) }

    LaunchedEffect(pageIndex, rotation) {
        isLoading = true; hasError = false
        val result = withContext(Dispatchers.IO) { rendererHolder.renderPage(pageIndex, rotation = rotation) }
        if (result != null) bitmap = result else hasError = true
        isLoading = false
    }

    val cardBg = readingSurface(readingTheme)

    Card(
        modifier = Modifier.fillMaxWidth()
            .then(
                if (hasSearchHit) Modifier.border(
                    2.dp, AccentView, RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column {
            if (hasSearchHit && searchQuery.isNotBlank()) {
                Surface(
                    color = AccentView,
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Search, null,
                            tint = Color.White, modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "\"$searchQuery\" found",
                            color = Color.White, fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        Modifier.fillMaxWidth().height(400.dp).background(cardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = AccentView, modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Rendering page ${pageIndex + 1}…", fontSize = 12.sp,
                                color = readingInk(readingTheme, 0.4f)
                            )
                        }
                    }
                }

                hasError -> {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp)
                            .background(ErrorRed.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.ErrorOutline, null,
                                tint = ErrorRed.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Failed to load page ${pageIndex + 1}",
                                color = ErrorRed.copy(alpha = 0.85f), fontSize = 13.sp
                            )
                        }
                    }
                }

                bitmap != null -> {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                drawContent()
                                // Draw yellow highlight rectangles over matched text
                                for (hl in highlights) {
                                    drawRect(
                                        color = Color(0xFFFFEB3B).copy(alpha = 0.6f),
                                        topLeft = Offset(
                                            hl.leftRatio * size.width,
                                            hl.topRatio * size.height
                                        ),
                                        size = Size(
                                            hl.widthRatio * size.width,
                                            hl.heightRatio * size.height
                                        )
                                    )
                                    drawRect(
                                        color = Color(0xFFFF9800).copy(alpha = 0.7f),
                                        topLeft = Offset(
                                            hl.leftRatio * size.width,
                                            hl.topRatio * size.height
                                        ),
                                        size = Size(
                                            hl.widthRatio * size.width,
                                            hl.heightRatio * size.height
                                        ),
                                        style = Stroke(
                                            width = 1.dp.toPx()
                                        )
                                    )
                                }
                                // Draw freehand drawing strokes
                                for (stroke in strokes) {
                                    if (stroke.points.size > 1) {
                                        val path = Path()
                                        val start = stroke.points.first()
                                        path.moveTo(start.xRatio * size.width, start.yRatio * size.height)
                                        for (i in 1 until stroke.points.size) {
                                            val pt = stroke.points[i]
                                            path.lineTo(pt.xRatio * size.width, pt.yRatio * size.height)
                                        }
                                        drawPath(
                                            path = path,
                                            color = stroke.color,
                                            style = Stroke(
                                                width = stroke.strokeWidth,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    } else if (stroke.points.size == 1) {
                                        val pt = stroke.points.first()
                                        drawCircle(
                                            color = stroke.color,
                                            radius = stroke.strokeWidth / 2f,
                                            center = Offset(pt.xRatio * size.width, pt.yRatio * size.height)
                                        )
                                    }
                                }
                            }
                    )
                }
            }

            // Footer
            Row(
                Modifier.fillMaxWidth().background(
                    readingSurface(readingTheme)
                ).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(Brush.horizontalGradient(GradientView)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${pageIndex + 1}", color = Color.White,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Page ${pageIndex + 1} of $totalPages",
                        fontSize = 12.sp,
                        color = readingInk(readingTheme, 0.6f)
                    )
                }
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (isBookmarked) Icons.Rounded.Bookmark
                        else Icons.Rounded.BookmarkBorder,
                        "Bookmark",
                        tint = if (isBookmarked) AccentView
                        else readingInk(readingTheme, 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  SINGLE PAGE VIEW
// ═══════════════════════════════════════════════════════════
@Composable
private fun SinglePageView(
    rendererHolder: PdfRendererHolder,
    pageIndex: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    readingTheme: ReadingTheme,
    highlights: List<HighlightRect> = emptyList(),
    rotation: Int = 0,
    strokes: List<DrawingStroke> = emptyList(),
    isDrawingMode: Boolean = false,
    selectedDrawingTool: DrawingTool = DrawingTool.PEN,
    selectedColor: Color = Color(0xFFE53935),
    onAddStroke: (DrawingStroke) -> Unit = {},
    onEraseStrokeAt: (DrawingPoint) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex, rotation) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex, rotation) { mutableStateOf(true) }
    var activeStroke by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

    LaunchedEffect(pageIndex, rotation) {
        isLoading = true
        bitmap = withContext(Dispatchers.IO) { rendererHolder.renderPage(pageIndex, rotation = rotation) }
        isLoading = false
    }

    val cardBg = readingPageBg(readingTheme)

    when {
        isLoading -> CircularProgressIndicator(
            color = AccentView, modifier = Modifier.size(40.dp)
        )

        bitmap != null -> {
            val drawModifier = if (isDrawingMode) {
                Modifier.pointerInput(isDrawingMode, selectedDrawingTool, selectedColor) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val currentPoints = mutableListOf<DrawingPoint>()
                        val startPt = DrawingPoint(
                            (down.position.x / size.width).coerceIn(0f, 1f),
                            (down.position.y / size.height).coerceIn(0f, 1f)
                        )
                        if (selectedDrawingTool == DrawingTool.ERASER) {
                            onEraseStrokeAt(startPt)
                        } else {
                            currentPoints.add(startPt)
                            activeStroke = currentPoints.toList()
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            val movePt = DrawingPoint(
                                (change.position.x / size.width).coerceIn(0f, 1f),
                                (change.position.y / size.height).coerceIn(0f, 1f)
                            )
                            if (selectedDrawingTool == DrawingTool.ERASER) {
                                onEraseStrokeAt(movePt)
                            } else {
                                currentPoints.add(movePt)
                                activeStroke = currentPoints.toList()
                            }
                        }

                        if (selectedDrawingTool != DrawingTool.ERASER && currentPoints.isNotEmpty()) {
                            val isHl = (selectedDrawingTool == DrawingTool.HIGHLIGHTER)
                            val newStroke = DrawingStroke(
                                points = currentPoints.toList(),
                                color = if (isHl) selectedColor.copy(alpha = 0.4f) else selectedColor,
                                strokeWidth = if (isHl) 24f else 5f,
                                isHighlighter = isHl
                            )
                            onAddStroke(newStroke)
                        }
                        activeStroke = emptyList()
                    }
                }
            } else {
                Modifier
            }

            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            ) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(drawModifier)
                        .drawWithContent {
                            drawContent()

                            // 1. Search highlights
                            for (hl in highlights) {
                                drawRect(
                                    color = Color(0xFFFFEB3B).copy(alpha = 0.45f),
                                    topLeft = Offset(hl.leftRatio * size.width, hl.topRatio * size.height),
                                    size = Size(hl.widthRatio * size.width, hl.heightRatio * size.height)
                                )
                                drawRect(
                                    color = Color(0xFFFF9800).copy(alpha = 0.7f),
                                    topLeft = Offset(hl.leftRatio * size.width, hl.topRatio * size.height),
                                    size = Size(hl.widthRatio * size.width, hl.heightRatio * size.height),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }

                            // 2. Saved drawing strokes
                            for (stroke in strokes) {
                                if (stroke.points.size > 1) {
                                    val path = Path()
                                    val start = stroke.points.first()
                                    path.moveTo(start.xRatio * size.width, start.yRatio * size.height)
                                    for (i in 1 until stroke.points.size) {
                                        val pt = stroke.points[i]
                                        path.lineTo(pt.xRatio * size.width, pt.yRatio * size.height)
                                    }
                                    drawPath(
                                        path = path,
                                        color = stroke.color,
                                        style = Stroke(
                                            width = stroke.strokeWidth,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                } else if (stroke.points.size == 1) {
                                    val pt = stroke.points.first()
                                    drawCircle(
                                        color = stroke.color,
                                        radius = stroke.strokeWidth / 2f,
                                        center = Offset(pt.xRatio * size.width, pt.yRatio * size.height)
                                    )
                                }
                            }

                            // 3. Active live stroke in progress
                            if (activeStroke.size > 1) {
                                val activePath = Path()
                                val start = activeStroke.first()
                                activePath.moveTo(start.xRatio * size.width, start.yRatio * size.height)
                                for (i in 1 until activeStroke.size) {
                                    val pt = activeStroke[i]
                                    activePath.lineTo(pt.xRatio * size.width, pt.yRatio * size.height)
                                }
                                val isHl = (selectedDrawingTool == DrawingTool.HIGHLIGHTER)
                                drawPath(
                                    path = activePath,
                                    color = if (isHl) selectedColor.copy(alpha = 0.4f) else selectedColor,
                                    style = Stroke(
                                        width = if (isHl) 24f else 5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            } else if (activeStroke.size == 1) {
                                val pt = activeStroke.first()
                                val isHl = (selectedDrawingTool == DrawingTool.HIGHLIGHTER)
                                drawCircle(
                                    color = if (isHl) selectedColor.copy(alpha = 0.4f) else selectedColor,
                                    radius = if (isHl) 12f else 3f,
                                    center = Offset(pt.xRatio * size.width, pt.yRatio * size.height)
                                )
                            }
                        }
                )
            }
        }

        else -> Text("Failed to load page", color = ErrorRed.copy(alpha = 0.8f))
    }
}


// ═══════════════════════════════════════════════════════════
//  MODIFY PDF TABS BAR
// ═══════════════════════════════════════════════════════════
@Composable
private fun PdfModifyTabBar(
    readingTheme: ReadingTheme,
    onOrganize: () -> Unit,
    onWatermark: () -> Unit,
    onCompress: () -> Unit,
    onSplit: () -> Unit,
    onMarkup: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModifyTabItem(
            icon = Icons.Rounded.ViewAgenda,
            label = "Organize",
            readingTheme = readingTheme,
            onClick = onOrganize
        )
        ModifyTabItem(
            icon = Icons.Rounded.BrandingWatermark,
            label = "Watermark",
            readingTheme = readingTheme,
            onClick = onWatermark
        )
        ModifyTabItem(
            icon = Icons.Rounded.Compress,
            label = "Compress",
            readingTheme = readingTheme,
            onClick = onCompress
        )
        ModifyTabItem(
            icon = Icons.Rounded.CallSplit,
            label = "Split",
            readingTheme = readingTheme,
            onClick = onSplit
        )
        ModifyTabItem(
            icon = Icons.Rounded.Edit,
            label = "Markup",
            readingTheme = readingTheme,
            onClick = onMarkup
        )
        ModifyTabItem(
            icon = Icons.Outlined.Share,
            label = "Share",
            readingTheme = readingTheme,
            onClick = onShare
        )
        ModifyTabItem(
            icon = Icons.Rounded.MoreHoriz,
            label = "More",
            readingTheme = readingTheme,
            onClick = onMore
        )
    }
}

@Composable
private fun ModifyTabItem(
    icon: ImageVector,
    label: String,
    readingTheme: ReadingTheme,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = readingSurface(readingTheme).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, readingInk(readingTheme, 0.12f)),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AccentView,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = readingInk(readingTheme, 0.9f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  SINGLE PAGE NAV BAR
// ═══════════════════════════════════════════════════════════
@Composable
private fun SinglePageNavBar(
    currentPage: Int,
    totalPages: Int,
    readingTheme: ReadingTheme,
    isBookmarked: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOrganize: () -> Unit = {},
    onWatermark: () -> Unit = {},
    onCompress: () -> Unit = {},
    onSplit: () -> Unit = {},
    onMarkup: () -> Unit = {},
    onShare: () -> Unit = {},
    onMore: () -> Unit = {}
) {
    val barBg = readingSurface(readingTheme)

    Surface(
        color = barBg, tonalElevation = 4.dp, shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onPrevious, enabled = currentPage > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentView.copy(alpha = 0.15f),
                        contentColor = AccentView
                    )
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowLeft, "Prev",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Prev", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${currentPage + 1}",
                        fontWeight = FontWeight.Bold, fontSize = 20.sp,
                        color = AccentView
                    )
                    Text(
                        "of $totalPages", fontSize = 11.sp,
                        color = readingInk(readingTheme, 0.5f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Rounded.Bookmark
                            else Icons.Rounded.BookmarkBorder,
                            "Bookmark",
                            tint = if (isBookmarked) AccentView else readingInk(readingTheme, 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    LinearProgressIndicator(
                        progress = {
                            if (totalPages > 1) (currentPage + 1).toFloat() / totalPages else 1f
                        },
                        modifier = Modifier.width(50.dp).height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AccentView,
                        trackColor = AccentView.copy(alpha = 0.15f)
                    )
                }

                FilledTonalButton(
                    onClick = onNext, enabled = currentPage < totalPages - 1,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentView.copy(alpha = 0.15f),
                        contentColor = AccentView
                    )
                ) {
                    Text("Next", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.KeyboardArrowRight, "Next",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(
                color = readingInk(readingTheme, 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
            PdfModifyTabBar(
                readingTheme = readingTheme,
                onOrganize = onOrganize,
                onWatermark = onWatermark,
                onCompress = onCompress,
                onSplit = onSplit,
                onMarkup = onMarkup,
                onShare = onShare,
                onMore = onMore
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  CONTINUOUS BOTTOM BAR
// ═══════════════════════════════════════════════════════════
@Composable
private fun ContinuousBottomBar(
    currentPage: Int,
    totalPages: Int,
    onChangeFile: () -> Unit,
    readingTheme: ReadingTheme,
    onOrganize: () -> Unit = {},
    onWatermark: () -> Unit = {},
    onCompress: () -> Unit = {},
    onSplit: () -> Unit = {},
    onMarkup: () -> Unit = {},
    onShare: () -> Unit = {},
    onMore: () -> Unit = {}
) {
    val barBg = readingSurface(readingTheme)

    Surface(
        color = barBg, tonalElevation = 4.dp, shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Page ${currentPage + 1} of $totalPages",
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        color = readingInk(readingTheme)
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (totalPages > 1) (currentPage + 1).toFloat() / totalPages else 1f
                        },
                        modifier = Modifier.width(100.dp).height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AccentView,
                        trackColor = AccentView.copy(alpha = 0.15f)
                    )
                }
                OutlinedButton(
                    onClick = onChangeFile,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = Brush.horizontalGradient(GradientView)
                    )
                ) {
                    Icon(
                        Icons.Rounded.SwapHoriz, null,
                        tint = AccentView, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Change File", color = AccentView,
                        fontWeight = FontWeight.Medium, fontSize = 12.sp
                    )
                }
            }
            HorizontalDivider(
                color = readingInk(readingTheme, 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
            PdfModifyTabBar(
                readingTheme = readingTheme,
                onOrganize = onOrganize,
                onWatermark = onWatermark,
                onCompress = onCompress,
                onSplit = onSplit,
                onMarkup = onMarkup,
                onShare = onShare,
                onMore = onMore
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════

private fun formatViewSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

@Composable
private fun ViewSelectButton(onClick: () -> Unit) {
    PrecisionGradientButton(
        text = "Open PDF File",
        onClick = onClick,
        height = 60.dp,
        gradient = GradientView,
        icon = Icons.Rounded.FileOpen
    )
}

@Composable
private fun ViewEmptyState(readingTheme: ReadingTheme) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(100.dp).clip(CircleShape)
                    .background(AccentView.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.MenuBook, null,
                    tint = AccentView,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "No PDF opened",
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                color = readingInk(readingTheme, 0.9f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select a PDF file to view its contents",
                fontSize = 14.sp,
                color = readingInk(readingTheme, 0.65f),
                textAlign = TextAlign.Center, lineHeight = 20.sp
            )
            Spacer(Modifier.height(24.dp))
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                ViewFeatureRow(Icons.Rounded.Search, "Search text & highlight matches", readingTheme)
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.VolumeUp, "Natural voice Read Aloud (TTS)", readingTheme)
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.Edit, "Freehand pen & highlighter markup", readingTheme)
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.ZoomIn, "Pinch to zoom & double-tap", readingTheme)
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.Bookmark, "Bookmark pages & outline navigation", readingTheme)
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.Palette, "Reading themes (Light/Dark/Sepia)", readingTheme)
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.GridView, "Thumbnail & flip page navigation", readingTheme)
            }
        }
    }
}

@Composable
private fun ViewFeatureRow(
    icon: ImageVector,
    text: String,
    readingTheme: ReadingTheme
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, null,
            tint = AccentView.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text, fontSize = 12.sp,
            color = readingInk(readingTheme, 0.7f)
        )
    }
}


// ═══════════════════════════════════════════════════════════
//  DRAWING TOOL BAR
// ═══════════════════════════════════════════════════════════
@Composable
private fun DrawingToolBar(
    selectedTool: DrawingTool,
    onSelectTool: (DrawingTool) -> Unit,
    selectedColor: Color,
    onSelectColor: (Color) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    readingTheme: ReadingTheme
) {
    val colors = listOf(
        Color(0xFFE53935), // Vermilion
        Color(0xFFFFB300), // Amber
        Color(0xFF43A047), // Green
        Color(0xFF1E88E5), // Blue
        Color(0xFF8E24AA), // Purple
        Color(0xFF212121)  // Dark Ink
    )

    Surface(
        color = readingSurface(readingTheme),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tool selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onSelectTool(DrawingTool.PEN) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (selectedTool == DrawingTool.PEN) AccentView.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Draw,
                            contentDescription = "Pen",
                            tint = if (selectedTool == DrawingTool.PEN) AccentView else readingInk(readingTheme, 0.65f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSelectTool(DrawingTool.HIGHLIGHTER) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (selectedTool == DrawingTool.HIGHLIGHTER) AccentView.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Brush,
                            contentDescription = "Highlighter",
                            tint = if (selectedTool == DrawingTool.HIGHLIGHTER) AccentView else readingInk(readingTheme, 0.65f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSelectTool(DrawingTool.ERASER) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (selectedTool == DrawingTool.ERASER) AccentView.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AutoFixNormal,
                            contentDescription = "Eraser",
                            tint = if (selectedTool == DrawingTool.ERASER) AccentView else readingInk(readingTheme, 0.65f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Action buttons: Undo, Clear, Done
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) readingInk(readingTheme) else readingInk(readingTheme, 0.25f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear All",
                            tint = readingInk(readingTheme, 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    FilledTonalButton(
                        onClick = onDone,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentView,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Color palette (only when pen or highlighter is selected)
            if (selectedTool != DrawingTool.ERASER) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { c ->
                        val isSelected = (selectedColor == c)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { onSelectColor(c) }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, readingInk(readingTheme), CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  TTS PLAYER BAR
// ═══════════════════════════════════════════════════════════
@Composable
private fun TtsPlayerBar(
    currentPage: Int,
    totalPages: Int,
    ttsManager: PdfTtsManager,
    ttsTrigger: Int,
    pageTexts: Map<Int, String>,
    onClose: () -> Unit,
    readingTheme: ReadingTheme
) {
    val ink = readingInk(readingTheme)
    val surface = readingSurface(readingTheme)
    val _reactiveTrigger = ttsTrigger // reads trigger state

    Surface(
        color = surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentView.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = AccentView,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Read Aloud (Listen)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ink
                        )
                        Text(
                            "Page ${currentPage + 1} of $totalPages • ${ttsManager.currentAccent.label}",
                            fontSize = 11.sp,
                            color = readingInk(readingTheme, 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice Accent switcher (Indian English / Hindi / Default)
                    FilledTonalButton(
                        onClick = {
                            val nextAccent = when (ttsManager.currentAccent) {
                                TtsAccent.INDIAN_ENGLISH -> TtsAccent.INDIAN_HINDI
                                TtsAccent.INDIAN_HINDI -> TtsAccent.SYSTEM_DEFAULT
                                TtsAccent.SYSTEM_DEFAULT -> TtsAccent.INDIAN_ENGLISH
                            }
                            ttsManager.setAccent(nextAccent)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentView.copy(alpha = 0.12f),
                            contentColor = AccentView
                        )
                    ) {
                        Text(ttsManager.currentAccent.shortName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.width(6.dp))

                    // Speech rate multiplier
                    FilledTonalButton(
                        onClick = {
                            val nextRate = when (ttsManager.currentSpeed) {
                                0.75f -> 1.0f
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 0.75f
                            }
                            ttsManager.setSpeed(nextRate)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentView.copy(alpha = 0.12f),
                            contentColor = AccentView
                        )
                    ) {
                        Text("${ttsManager.currentSpeed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.width(6.dp))

                    // Play / Pause
                    IconButton(
                        onClick = {
                            if (ttsManager.isSpeaking) {
                                ttsManager.stop()
                            } else {
                                val text = pageTexts[currentPage]
                                if (!text.isNullOrBlank()) {
                                    ttsManager.speak(text)
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (ttsManager.isSpeaking) Icons.Rounded.PauseCircle
                            else Icons.Rounded.PlayCircle,
                            contentDescription = if (ttsManager.isSpeaking) "Pause" else "Play",
                            tint = AccentView,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(Modifier.width(2.dp))

                    // Stop & Close
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Stop & Close",
                            tint = readingInk(readingTheme, 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  OUTLINE / TABLE OF CONTENTS BOTTOM SHEET
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutlineBottomSheet(
    outlines: List<PdfOutlineItem>,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit,
    readingTheme: ReadingTheme
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = readingSurface(readingTheme),
        contentColor = readingInk(readingTheme),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = readingInk(readingTheme, 0.3f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentView.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = AccentView,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Table of Contents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = readingInk(readingTheme)
                    )
                    Text(
                        "${outlines.size} chapters & sections",
                        style = MaterialTheme.typography.bodySmall,
                        color = readingInk(readingTheme, 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (outlines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.BookmarkBorder,
                            contentDescription = null,
                            tint = readingInk(readingTheme, 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No bookmarks or outline found in this document",
                            style = MaterialTheme.typography.bodyMedium,
                            color = readingInk(readingTheme, 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(outlines.size) { index ->
                        val item = outlines[index]
                        OutlineItemView(
                            item = item,
                            level = 0,
                            onSelectPage = { page ->
                                onSelectPage(page)
                                onDismiss()
                            },
                            readingTheme = readingTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlineItemView(
    item: PdfOutlineItem,
    level: Int = 0,
    onSelectPage: (Int) -> Unit,
    readingTheme: ReadingTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                onSelectPage(item.pageIndex)
            }
            .padding(
                start = (level * 16 + 8).dp,
                end = 8.dp,
                top = 10.dp,
                bottom = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (item.children.isNotEmpty()) Icons.Rounded.Folder
            else Icons.Rounded.Description,
            contentDescription = null,
            tint = if (level == 0) AccentView else readingInk(readingTheme, 0.5f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (level == 0) FontWeight.SemiBold else FontWeight.Normal,
            color = readingInk(readingTheme),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            color = AccentView.copy(alpha = 0.1f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "p. ${item.pageIndex + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AccentView,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }

    // Render children recursively if any
    item.children.forEach { child ->
        OutlineItemView(item = child, level = level + 1, onSelectPage = onSelectPage, readingTheme = readingTheme)
    }
}


// ═══════════════════════════════════════════════════════════
//  DOCUMENT METADATA DIALOG
// ═══════════════════════════════════════════════════════════
@Composable
private fun MetadataDialog(
    metadata: PdfDocumentMetadata?,
    onDismiss: () -> Unit,
    readingTheme: ReadingTheme
) {
    if (metadata == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = readingSurface(readingTheme),
        titleContentColor = readingInk(readingTheme),
        textContentColor = readingInk(readingTheme),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentView.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Info, null, tint = AccentView, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Document Properties", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { MetadataPropertyRow("Title", metadata.title.ifBlank { "Untitled" }, readingTheme) }
                    item { MetadataPropertyRow("Author", metadata.author.ifBlank { "Unknown" }, readingTheme) }
                    item { MetadataPropertyRow("Subject", metadata.subject.ifBlank { "None" }, readingTheme) }
                    item { MetadataPropertyRow("Keywords", metadata.keywords.ifBlank { "None" }, readingTheme) }
                    item { MetadataPropertyRow("Producer", metadata.producer.ifBlank { "Unknown" }, readingTheme) }
                    item { MetadataPropertyRow("Creator", metadata.creator.ifBlank { "Unknown" }, readingTheme) }
                    item { MetadataPropertyRow("Pages", "${metadata.pageCount}", readingTheme) }
                    item { MetadataPropertyRow("File Size", metadata.fileSize.ifBlank { "Unknown" }, readingTheme) }
                    if (metadata.pageDimensions.isNotBlank()) {
                        item {
                            MetadataPropertyRow(
                                "Page Size",
                                metadata.pageDimensions,
                                readingTheme
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentView, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun MetadataPropertyRow(
    label: String,
    value: String,
    readingTheme: ReadingTheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(readingInk(readingTheme, 0.04f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentView
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            color = readingInk(readingTheme),
            lineHeight = 16.sp
        )
    }
}