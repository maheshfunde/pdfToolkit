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
import com.yashodatech.pdftoolkit.theme.Vermilion
import com.yashodatech.pdftoolkit.theme.VermilionDeep

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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itextpdf.kernel.pdf.PdfDocument as ITextPdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
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

enum class ViewMode { CONTINUOUS, SINGLE_PAGE }

// ── Reading surfaces, folded into the Precision (Light-primary) system ──
// The reader is the one surface that renders actual page "paper", so its
// three moods sit on the platform's own paper/ink tiers — Light paper,
// the system's inverse (Night) dark, and an aged Sepia stock. Text stays a
// declared ink so nothing is invisible against its surface in any theme.
private val PaperLight   = PrecisionSurface      // light paper (F8F9FF)
private val PaperInk     = PrecisionOnSurface    // dark ink text on Light
private val NightBg      = PrecisionInverseSurface // night reading (dark)
private val NightInk     = PrecisionInverseOnSurface
private val SepiaBg      = Color(0xFFE8D4B8)     // aged paper
private val SepiaInk     = Color(0xFF5B4636)     // sepia-ink text

// Raised surfaces sit one step off the page (cards, bars, chips).
private val LightPageSurface = Color(0xFFF1ECE3)   // warm paper step
private val NightPageSurface = Color(0xFF31425C)   // lighter navy step for night
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

    companion object {
        private const val MAX_PIXELS = 2560 * 2560
    }

    suspend fun renderPage(pageIndex: Int, quality: Float = 2.0f): Bitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount) return null
        return mutex.withLock {
            try {
                val page = renderer.openPage(pageIndex)
                val origW = page.width; val origH = page.height
                val safeScale = calculateSafeScale(origW, origH, quality)
                val renderW = (origW * safeScale).toInt().coerceAtLeast(1)
                val renderH = (origH * safeScale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(AndroidColor.WHITE)
                page.render(bitmap, null, Matrix().apply { setScale(safeScale, safeScale) },
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap
            } catch (_: OutOfMemoryError) {
                System.gc()
                try { renderPageLowRes(pageIndex) } catch (_: Throwable) { null }
            } catch (_: Exception) { null }
        }
    }

    private fun renderPageLowRes(pageIndex: Int): Bitmap? {
        val page = renderer.openPage(pageIndex)
        val origW = page.width; val origH = page.height
        val scale = calculateSafeScale(origW, origH, 0.75f)
        val renderW = (origW * scale).toInt().coerceAtLeast(1)
        val renderH = (origH * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(AndroidColor.WHITE)
        page.render(bitmap, null, Matrix().apply { setScale(scale, scale) },
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
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

            val renderInfo =
                data as TextRenderInfo

            val text = renderInfo.text ?: return

            if (text.contains(query, ignoreCase = true)) {

                val rect = renderInfo.baseline.boundingRectangle

                highlights.add(rect)
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

                        topRatio = ((pageHeight - rect.y - rect.height) / pageHeight)
                            .coerceIn(0f, 1f),

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewPdfScreen(onBack: () -> Unit, initialUri: Uri? = null) {

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

    var readingTheme by remember { mutableStateOf(ReadingTheme.LIGHT) }
    var showThemeSelector by remember { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(true) }
    var showThumbnails by remember { mutableStateOf(false) }

    val currentVisiblePage by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    DisposableEffect(Unit) {
        onDispose { rendererHolder?.close() }
    }

    fun openPdf(uri: Uri) {
        rendererHolder?.close(); rendererHolder = null
        sourceUri = uri; totalPages = 0; currentPage = 0
        scale = 1f; offsetX = 0f; offsetY = 0f
        errorMessage = null; bookmarks = emptySet()
        pageTexts = emptyMap(); searchResults = emptyList()
        searchQuery = ""; showSearchBar = false
        showSearchResults = false; currentSearchResultIndex = -1

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) sourceName = cursor.getString(nameIdx) ?: "Unknown.pdf"
                if (sizeIdx >= 0) sourceSizeText = formatViewSize(cursor.getLong(sizeIdx))
            }
        }

        isLoading = true
        openStatus = "Opening PDF…"
        scope.launch {
            try {
                val holder = withContext(Dispatchers.IO) {
                    val fd = context.contentResolver.openFileDescriptor(uri, "r")
                        ?: throw Exception("Cannot open PDF")
                    PdfRendererHolder(fd, PdfRenderer(fd))
                }
                rendererHolder = holder; totalPages = holder.pageCount
                openStatus = "Extracting searchable text…"
                isLoading = false

                isExtractingText = true
                pageTexts = extractTextFromPdf(context, uri)
                isExtractingText = false
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage ?: "Cannot open PDF"}"
                isLoading = false
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            openPdf(it)
        }
    }

    // Open a URI handed in via navigation (e.g. from Recent documents or
    // an "Open Document" action on a success screen).
    LaunchedEffect(initialUri) {
        initialUri?.let { openPdf(it) }
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
            searchHighlights = emptyMap()  // ◀ ADDED
            return
        }
        isSearching = true
        scope.launch {
            val results = withContext(Dispatchers.Default) { searchInPages(pageTexts, query) }
            searchResults = results
            showSearchResults = results.isNotEmpty()
            currentSearchResultIndex = if (results.isNotEmpty()) 0 else -1
            if (results.isNotEmpty()) goToPage(results[0].pageIndex)

            // ◀ ADDED: Extract highlight positions
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

    fun navigateSearchResult(direction: Int) {
        if (searchResults.isEmpty()) return
        currentSearchResultIndex = (currentSearchResultIndex + direction)
            .coerceIn(0, searchResults.size - 1)
        goToPage(searchResults[currentSearchResultIndex].pageIndex)
    }

    val bgColor = readingPageBg(readingTheme)

    Scaffold(
        topBar = {
            Column {
                AnimatedVisibility(
                    visible = showToolbar,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    Column {
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
                                    // Search toggle
                                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                                        Icon(
                                            Icons.Rounded.Search, "Search",
                                            tint = if (showSearchBar) AccentView
                                            else readingInk(readingTheme, 0.7f)
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

                                    // More menu
                                    var showMenu by remember { mutableStateOf(false) }
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
                                                        "PDF actions",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = readingInk(readingTheme),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        "Navigate, read, and share",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = readingInk(readingTheme, 0.6f)
                                                    )
                                                }
                                            }

                                            SheetSectionLabel("Navigate", readingTheme)
                                            SheetActionRow(
                                                title = when (viewMode) {
                                                    ViewMode.CONTINUOUS -> "Single Page Mode"
                                                    ViewMode.SINGLE_PAGE -> "Continuous Mode"
                                                },
                                                icon = when (viewMode) {
                                                    ViewMode.CONTINUOUS -> Icons.Rounded.ViewDay
                                                    ViewMode.SINGLE_PAGE -> Icons.Rounded.ViewAgenda
                                                },
                                                readingTheme = readingTheme,
                                                onClick = {
                                                    viewMode = when (viewMode) {
                                                        ViewMode.CONTINUOUS -> {
                                                            currentPage = currentVisiblePage
                                                            ViewMode.SINGLE_PAGE
                                                        }
                                                        ViewMode.SINGLE_PAGE -> {
                                                            scope.launch { listState.scrollToItem(currentPage) }
                                                            ViewMode.CONTINUOUS
                                                        }
                                                    }
                                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                                    showMenu = false
                                                }
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

                                            SheetSectionLabel("Reading", readingTheme)
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

                                            SheetSectionLabel("Share", readingTheme)
                                            SheetActionRow(
                                                title = "Share PDF",
                                                icon = Icons.Outlined.Share,
                                                readingTheme = readingTheme,
                                                onClick = {
                                                    sourceUri?.let { uri ->
                                                        context.startActivity(
                                                            Intent.createChooser(
                                                                Intent(Intent.ACTION_SEND).apply {
                                                                    type = "application/pdf"
                                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                                    flags =
                                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                }, "Share PDF"
                                                            )
                                                        )
                                                    }; showMenu = false
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
                                                                    setDataAndType(
                                                                        uri,
                                                                        "application/pdf"
                                                                    )
                                                                    flags =
                                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                })
                                                        } catch (_: Exception) {
                                                            Toast.makeText(
                                                                context,
                                                                "No app found",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
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
                        ViewEmptyState()
                    }
                }

                isLoading -> Box(Modifier.fillMaxSize())

                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.ErrorOutline, null,
                                tint = ErrorRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                errorMessage!!,
                                color = ErrorRed.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }
                            ) {
                                Text("Try Another File")
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

                        when (viewMode) {
                            ViewMode.CONTINUOUS -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = { showToolbar = !showToolbar },
                                                onDoubleTap = {
                                                    scale =
                                                        if (scale > 1.5f) 1f else 2.5f
                                                    offsetX = 0f; offsetY = 0f
                                                }
                                            )
                                        }
                                ) {
                                    val transformState =
                                        rememberTransformableState { zoomChange, panChange, _ ->
                                            scale =
                                                (scale * zoomChange).coerceIn(0.5f, 5f)
                                            if (scale > 1f) {
                                                offsetX += panChange.x; offsetY += panChange.y
                                            } else {
                                                offsetX = 0f; offsetY = 0f
                                            }
                                        }

                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .transformable(state = transformState)
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offsetX,
                                                translationY = offsetY
                                            ),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(totalPages) { pageIndex ->
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
                                                onToggleBookmark = {
                                                    bookmarks =
                                                        if (pageIndex in bookmarks) bookmarks - pageIndex
                                                        else bookmarks + pageIndex
                                                }
                                            )
                                        }
                                    }

                                    // Zoom indicator
                                    if (scale != 1f) {
                                        Surface(
                                            color = NightBg.copy(alpha = 0.92f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.TopEnd)
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                "${(scale * 100).toInt()}%",
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

                                ContinuousBottomBar(
                                    currentPage = currentVisiblePage,
                                    totalPages = totalPages,
                                    onChangeFile = {
                                        pdfLauncher.launch(arrayOf("application/pdf"))
                                    },
                                    readingTheme = readingTheme
                                )
                            }

                            ViewMode.SINGLE_PAGE -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = { showToolbar = !showToolbar },
                                                onDoubleTap = {
                                                    scale =
                                                        if (scale > 1.5f) 1f else 2.5f
                                                    offsetX = 0f; offsetY = 0f
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val transformState =
                                        rememberTransformableState { zoomChange, panChange, _ ->
                                            scale =
                                                (scale * zoomChange).coerceIn(0.5f, 5f)
                                            if (scale > 1f) {
                                                offsetX += panChange.x; offsetY += panChange.y
                                            } else {
                                                offsetX = 0f; offsetY = 0f
                                            }
                                        }

                                    SinglePageView(
                                        rendererHolder = rendererHolder!!,
                                        pageIndex = currentPage,
                                        scale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        readingTheme = readingTheme,
                                        highlights = searchHighlights[currentPage] ?: emptyList(),
                                        modifier = Modifier.transformable(state = transformState)
                                    )

                                    if (scale != 1f) {
                                        Surface(
                                            color = NightBg.copy(alpha = 0.92f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.TopEnd)
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                "${(scale * 100).toInt()}%",
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
                                    }
                                )
                            }
                        }
                    }

                    // Floating zoom controls
                    if (showToolbar) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 140.dp)
                        ) {
                            FloatingActionButton(
                                onClick = { scale = (scale + 0.5f).coerceAtMost(5f) },
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
                                    scale = (scale - 0.5f).coerceAtLeast(0.5f)
                                    if (scale <= 1f) {
                                        offsetX = 0f; offsetY = 0f
                                    }
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
                            if (scale != 1f) {
                                FloatingActionButton(
                                    onClick = {
                                        scale = 1f; offsetX = 0f; offsetY = 0f
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
    DisposableEffect(pageIndex) {
        onDispose { thumbnail?.recycle(); thumbnail = null }
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
    highlights: List<HighlightRect>,  // ◀ NEW PARAM
    onToggleBookmark: () -> Unit
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }
    var hasError by remember(pageIndex) { mutableStateOf(false) }

    LaunchedEffect(pageIndex) {
        isLoading = true; hasError = false
        val result = withContext(Dispatchers.IO) { rendererHolder.renderPage(pageIndex) }
        if (result != null) bitmap = result else hasError = true
        isLoading = false
    }
    DisposableEffect(pageIndex) { onDispose { bitmap?.recycle(); bitmap = null } }

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
                    // ◀ CHANGED: Image with highlight overlay using drawWithContent
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
                                    // Draw border around highlight
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
    highlights: List<HighlightRect> = emptyList(),  // ◀ NEW PARAM
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }

    LaunchedEffect(pageIndex) {
        isLoading = true; bitmap?.recycle()
        bitmap = withContext(Dispatchers.IO) { rendererHolder.renderPage(pageIndex) }
        isLoading = false
    }
    DisposableEffect(pageIndex) {
        onDispose { bitmap?.recycle(); bitmap = null }
    }

    val cardBg = readingPageBg(readingTheme)

    when {
        isLoading -> CircularProgressIndicator(
            color = AccentView, modifier = Modifier.size(40.dp)
        )

        bitmap != null -> {
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = offsetX, translationY = offsetY
                    )
            ) {
                // ◀ CHANGED: Image with highlight overlay
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            drawContent()
                            for (hl in highlights) {
                                drawRect(
                                    color = Color(0xFFFFEB3B).copy(alpha = 0.45f),
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
                        }
                )
            }
        }

        else -> Text("Failed to load page", color = ErrorRed.copy(alpha = 0.8f))
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
    onToggleBookmark: () -> Unit
) {
    val barBg = readingSurface(readingTheme)

    Surface(
        color = barBg, tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
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
                    fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    color = AccentView
                )
                Text(
                    "of $totalPages", fontSize = 12.sp,
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
    readingTheme: ReadingTheme
) {
    val barBg = readingSurface(readingTheme)

    Surface(
        color = barBg, tonalElevation = 4.dp, shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Page ${currentPage + 1} of $totalPages",
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = readingInk(readingTheme)
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = {
                        if (totalPages > 1) (currentPage + 1).toFloat() / totalPages else 1f
                    },
                    modifier = Modifier.width(120.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentView,
                    trackColor = AccentView.copy(alpha = 0.15f)
                )
            }
            OutlinedButton(
                onClick = onChangeFile,
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(GradientView)
                )
            ) {
                Icon(
                    Icons.Rounded.SwapHoriz, null,
                    tint = AccentView, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Change File", color = AccentView,
                    fontWeight = FontWeight.Medium, fontSize = 13.sp
                )
            }
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
private fun ViewEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(100.dp).clip(CircleShape)
                    .background(AccentView.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.MenuBook, null,
                    tint = AccentView.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "No PDF opened",
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                color = PaperInk.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select a PDF file to view its contents",
                fontSize = 14.sp,
                color = PaperInk.copy(alpha = 0.45f),
                textAlign = TextAlign.Center, lineHeight = 20.sp
            )
            Spacer(Modifier.height(24.dp))
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                ViewFeatureRow(Icons.Rounded.Search, "Search text in PDF")
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.ZoomIn, "Pinch to zoom & double-tap")
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.Bookmark, "Bookmark pages")
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.Palette, "Reading themes (Light/Dark/Sepia)")
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.GridView, "Thumbnail navigation")
                Spacer(Modifier.height(6.dp))
                ViewFeatureRow(Icons.Rounded.Pin, "Go to any page")
            }
        }
    }
}

@Composable
private fun ViewFeatureRow(
    icon: ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, null,
            tint = AccentView.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text, fontSize = 12.sp,
            color = PaperInk.copy(alpha = 0.5f)
        )
    }
}