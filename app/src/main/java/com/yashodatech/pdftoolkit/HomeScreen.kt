package com.yashodatech.pdftoolkit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.BrandingWatermark
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.yashodatech.pdftoolkit.components.PrecisionCategoryPill
import com.yashodatech.pdftoolkit.components.PrecisionCard
import com.yashodatech.pdftoolkit.components.PrecisionSegmentTabs
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yashodatech.pdftoolkit.data.RecentDoc
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar

data class ToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val route: String,
    val category: String,
    val badge: String
)

private data class HomeSection(
    val title: String,
    val accent: Color,
    val tools: List<ToolItem>
)

private data class Feature(
    val title: String,
    val subtitle: String,
    val body: String,
    val icon: ImageVector,
    val accent: Color,
    val route: String
)

// ═══════════════════════════════════════════════════════════
//  FEATURE SHOWCASE CAROUSEL — auto-advancing highlight banner
//  Replaces the recent-docs shelf so the band is always filled
//  with a self-advancing, ad-like spotlight of each core tool.
// ═══════════════════════════════════════════════════════════
@Composable
private fun FeatureShowcaseCarousel(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val features = remember {
        listOf(
            Feature(
                title = "Merge PDF",
                subtitle = "Combine 2+ PDFs",
                body = "Join files into one document in seconds.",
                icon = Icons.AutoMirrored.Rounded.CallMerge,
                accent = DomainIndigo,
                route = Screen.MergePdf.route
            ),
            Feature(
                title = "Compress",
                subtitle = "Shrink file size",
                body = "Make oversized PDFs lighter and shareable.",
                icon = Icons.Rounded.Compress,
                accent = DomainEmerald,
                route = Screen.CompressPdf.route
            ),
            Feature(
                title = "Scan & Convert",
                subtitle = "Image to PDF",
                body = "Turn JPGs and PNGs into tidy PDF documents.",
                icon = Icons.Rounded.Image,
                accent = DomainAmber,
                route = Screen.ImageToPdf.route
            ),
            Feature(
                title = "Secure & Unlock",
                subtitle = "Remove a password",
                body = "Unlock a PDF you own — fast, right on device.",
                icon = Icons.Rounded.LockOpen,
                accent = PrecisionPrimary,
                route = Screen.UnlockPdf.route
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { features.size })

    // Gentle self-advance: every ~3.6s move to the next page, wrapping around.
    LaunchedEffect(pagerState, Unit) {
        while (true) {
            delay(3600)
            val next = (pagerState.currentPage + 1) % features.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "HIGHLIGHTS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) { page ->
            val feature = features[page]
            FeatureCard(
                feature = feature,
                onClick = { onToolClick(feature.route) },
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Indicator dots — vermilion for the active page.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            features.indices.forEach { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (active) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) PrecisionPrimary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: Feature,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            feature.accent.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Domain-tinted icon tile
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(feature.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = feature.title,
                        tint = feature.accent,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = feature.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = feature.accent,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = feature.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Vermilion "Try" pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PrecisionPrimary)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Try",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  RECENT DOCS — preview-thumbnail shelf
//  Shown in place of the feature carousel whenever there are
//  recently-created files. Each entry renders a PDF page-0 preview
//  (rendered off the main thread) so the shelf never shows raw text.
// ═══════════════════════════════════════════════════════════
@Composable
private fun RecentDocsRow(docs: List<RecentDoc>, onOpen: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT FILES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(docs.take(6), key = { it.uri }) { doc ->
                RecentDocCard(doc = doc, onClick = { onOpen(doc.uri) })
            }
        }
    }
}

@Composable
private fun RecentDocCard(doc: RecentDoc, onClick: () -> Unit) {
    val thumbnail = rememberPdfThumbnail(doc.uri)
    Card(
        modifier = Modifier
            .width(86.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = doc.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        tint = PrecisionPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = doc.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

// Whether the underlying content URI still resolves to a live file.
@Suppress("SwallowedException")
private fun docExists(context: Context, uri: String): Boolean = try {
    context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
} catch (_: Exception) {
    false
}

// Renders PDF page 0 to a Bitmap off the main thread (using the OS renderer,
// no extra dependency). Null if the file is gone or the render fails.
@Composable
private fun rememberPdfThumbnail(uri: String): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                val fd = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")
                    ?: return@withContext null
                fd.use { parcel ->
                    PdfRenderer(parcel).use { renderer ->
                        if (renderer.pageCount < 1) return@withContext null
                        renderer.openPage(0).use { page ->
                            val w = page.width.coerceAtLeast(1)
                            val h = page.height.coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }
    return bitmap
}

private fun formatRecentTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
        diff < 604_800_000L -> "${diff / 86_400_000L} d ago"
        else -> SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(Date(timestamp))
    }
}

// ═══════════════════════════════════════════════════════════
//  HOME SCREEN — PRECISION TOOLS HUB
//  Domain-accent category grids, hero reader card, search,
//  recent docs, quiet dock + ad banner.
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onToolClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenRecentDoc: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    // rememberSaveable (not remember) so the browsed category survives navigating
    // to a tool and back — the saved value is restored on re-entry. The value is
    // ALSO persisted to DataStore every browse, so a cold relaunch returns the
    // user to the category they were on rather than resetting to "Create".
    var activeTab by rememberSaveable { mutableStateOf(0) }

    // ── Recent docs state ────────────────────────────────
    var recentDocs by remember { mutableStateOf<List<RecentDoc>>(emptyList()) }

    suspend fun loadRecents() {
        val all = prefsManager.recentDocs.first()
        val valid = all.filter { docExists(context, it.uri) }
        if (valid.size != all.size) {
            // Prune deleted files from DataStore so they don't linger.
            scope.launch { prefsManager.setRecentDocs(valid) }
        }
        recentDocs = valid
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { loadRecents() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val privacyUrl = Config.PRIVACY_POLICY_URL

    val tools = remember {
        listOf(
            ToolItem(
                title = "Image to PDF",
                subtitle = "JPG, PNG to PDF",
                icon = Icons.Rounded.Image,
                gradientColors = GradientImageToPdfVibrant,
                route = Screen.ImageToPdf.route,
                category = "Convert",
                badge = "Popular"
            ),
            ToolItem(
                title = "Word to PDF",
                subtitle = "Convert DOCX files",
                icon = Icons.Rounded.Description,
                gradientColors = GradientWordToPdfVibrant,
                route = Screen.WordToPdf.route,
                category = "Convert",
                badge = "DOCX"
            ),
            ToolItem(
                title = "Merge PDF",
                subtitle = "Combine 2+ PDFs",
                icon = Icons.AutoMirrored.Rounded.CallMerge,
                gradientColors = GradientMergeVibrant,
                route = Screen.MergePdf.route,
                category = "Organize",
                badge = "Fast"
            ),
            ToolItem(
                title = "Split PDF",
                subtitle = "Extract pages",
                icon = Icons.AutoMirrored.Rounded.CallSplit,
                gradientColors = GradientSplitVibrant,
                route = Screen.SplitPdf.route,
                category = "Organize",
                badge = "Extract"
            ),
            ToolItem(
                title = "Compress PDF",
                subtitle = "Reduce file size",
                icon = Icons.Rounded.Compress,
                gradientColors = GradientCompressVibrant,
                route = Screen.CompressPdf.route,
                category = "Optimize",
                badge = "Lite"
            ),
            ToolItem(
                title = "View & Read",
                subtitle = "Read PDF docs",
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                gradientColors = GradientViewVibrant,
                route = Screen.ViewPdf.route,
                category = "View",
                badge = "Reader"
            ),
            ToolItem(
                title = "Organize Pages",
                subtitle = "Rotate, reorder, extract",
                icon = Icons.Rounded.ViewAgenda,
                gradientColors = GradientOrganizeVibrant,
                route = Screen.OrganizePages.route,
                category = "Organize",
                badge = "Edit"
            ),
            ToolItem(
                title = "Watermark",
                subtitle = "Stamp DRAFT or text",
                icon = Icons.AutoMirrored.Rounded.BrandingWatermark,
                gradientColors = GradientWatermarkVibrant,
                route = Screen.WatermarkPdf.route,
                category = "Protect",
                badge = "Stamp"
            ),
            ToolItem(
                title = "PDF to Images",
                subtitle = "Export pages as JPG/PNG",
                icon = Icons.Rounded.PhotoLibrary,
                gradientColors = GradientExportImagesVibrant,
                route = Screen.ExportImages.route,
                category = "Convert",
                badge = "Export"
            ),
            ToolItem(
                title = "PDF to Word",
                subtitle = "Convert to editable .docx",
                icon = Icons.Rounded.Description,
                gradientColors = GradientWordVibrant,
                route = Screen.ExportWord.route,
                category = "Convert",
                badge = "Word"
            ),
            ToolItem(
                title = "OCR PDF",
                subtitle = "Make scans searchable",
                icon = Icons.Rounded.DocumentScanner,
                gradientColors = GradientOcrVibrant,
                route = Screen.Ocr.route,
                category = "Convert",
                badge = "Search"
            ),
            ToolItem(
                title = "Unlock PDF",
                subtitle = "Remove a password you own",
                icon = Icons.Rounded.LockOpen,
                gradientColors = GradientUnlockVibrant,
                route = Screen.UnlockPdf.route,
                category = "Protect",
                badge = "Access"
            )
        )
    }

    // The Workbench — quick-creating duo up front, catalogue grouped by document life.
    val quickCreateTools = remember(tools) {
        tools.filter {
            it.route == Screen.ImageToPdf.route || it.route == Screen.WordToPdf.route
        }
    }

    fun lifecycleGroup(vararg routes: String): List<ToolItem> =
        routes.mapNotNull { r -> tools.firstOrNull { it.route == r } }

    val lifecycleSections = remember(tools) {
        listOf(
            HomeSection("Create", PrecisionPrimary, lifecycleGroup(
                Screen.ExportWord.route, Screen.ExportImages.route, Screen.Ocr.route,
                Screen.CompressPdf.route
            )),
            HomeSection("Refine", DomainIndigo, lifecycleGroup(
                Screen.MergePdf.route, Screen.SplitPdf.route,
                Screen.OrganizePages.route, Screen.ViewPdf.route
            )),
            HomeSection("Protect", DomainEmerald, lifecycleGroup(
                Screen.WatermarkPdf.route, Screen.UnlockPdf.route
            ))
        )
    }

    // Swipeable catalogue — one page per segment; swiping updates the active tab.
    val pagerState = rememberPagerState(pageCount = { lifecycleSections.size })

    LaunchedEffect(lifecycleSections) {
        val openCount = prefsManager.appOpenCount.first()
        val hasRated = prefsManager.hasRated.first()
        val lastTool = prefsManager.lastUsedTool.first()
        val storedTab = prefsManager.homeCategory.first()

        // Restore the category the user was browsing. Priority:
        //   1. homeCategory — the segment they last swiped/selected to (persisted
        //      every browse, so a cold relaunch returns to it).
        //   2. Otherwise, if they last used a tool, its owning segment (useful on
        //      the first cold start after using a tool, before any browse).
        //   3. Otherwise fall back to the restored activeTab (back-navigation).
        val fallbackIdx = lifecycleSections.indexOfFirst { s ->
            s.tools.any { it.route == lastTool }
        }.takeIf { it >= 0 } ?: activeTab
        val target = storedTab ?: fallbackIdx
        try {
            pagerState.scrollToPage(target.coerceIn(0, lifecycleSections.size - 1))
        } catch (_: Exception) {
            // Pager not laid out yet — the sync below settles the page.
        }

        // Keep the active tab (and the persisted preference) in step with the
        // pager, so both swipes and tab-taps save where the user ends up.
        snapshotFlow { pagerState.currentPage }.collect {
            activeTab = it
            prefsManager.setHomeCategory(it)
        }

        if (!hasRated && openCount > 0 && openCount % 5 == 0) {
            showRateDialog = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Fixed upper content: header, showcase/recents, quick create,
                // and the segment tabs. The catalogue below wraps its own height.
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HubHeader(onSettingsClick = onSettingsClick)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Feature showcase — auto-animated, ad-like banner. It's the
                    // filler when there's nothing recent to show; the moment any
                    // docs come back, the recents shelf (with preview thumbnails)
                    // takes this band instead.
                    if (recentDocs.isEmpty()) {
                        FeatureShowcaseCarousel(onToolClick = onToolClick)
                    } else {
                        RecentDocsRow(docs = recentDocs, onOpen = { onOpenRecentDoc(it) })
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick create — the gateway
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(450, delayMillis = 90)) + slideInVertically(tween(450, easing = FastOutSlowInEasing)) { it / 6 },
                        label = "quickEnter"
                    ) {
                        Column {
                            QuickCreatePanel(tools = quickCreateTools, onToolClick = onToolClick)
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // Lifecycle catalogue — swipe between segments; the tab bubble
                    // tracks the live swipe, the content rises in like a bubble.
                    PrecisionSegmentTabs(
                        tabs = lifecycleSections.map { it.title },
                        selectedIndex = activeTab,
                        pagePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                        onSelect = { tab -> scope.launch { pagerState.animateScrollToPage(tab) } }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    pageSpacing = 0.dp
                ) { page ->
                    val sec = lifecycleSections.getOrElse(page) { lifecycleSections.first() }
                    // Native horizontal slide between segments — content wraps
                    // its intrinsic height instead of stretching to fill the viewport.
                    Column(Modifier.fillMaxWidth().wrapContentHeight()) {
                        PrecisionCategoryPill(text = sec.title, accent = sec.accent)
                        Spacer(modifier = Modifier.height(9.dp))
                        ToolGrid(matches = sec.tools, onToolClick = onToolClick)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Quiet Dock — pinned so it is available on every section ──
            HubDock(
                onRateClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                onShareClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Try PDF Toolkit for quick PDF tools: " +
                                    "https://play.google.com/store/apps/details?id=${context.packageName}"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                },
                onPrivacyClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                    context.startActivity(intent)
                },
                onSettingsClick = onSettingsClick
            )

            // ── Banner Ad ────────────────────────────────────
            if (Config.SHOW_ADS) {
                AdBanner()
            }
        }
    }

    if (showPrivacyDialog) {
        PrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showRateDialog) {
        RateAppDialog(
            onDismiss = { showRateDialog = false },
            onRated = {
                showRateDialog = false
                scope.launch { prefsManager.setHasRated() }
            }
        )
    }
}


// ═══════════════════════════════════════════════════════════
//  HUB HEADER — headline brand line + quiet settings glyph
// ═══════════════════════════════════════════════════════════
@Composable
private fun HubHeader(onSettingsClick: () -> Unit) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "PDF Toolkit",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            PrecisionBadge2(text = "Offline")
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun PrecisionBadge2(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SuccessGreen.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = SuccessGreen
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  HERO READER CARD — gradient surface, pill CTA → View & Read
// ═══════════════════════════════════════════════════════════
@Composable
private fun QuickCreatePanel(tools: List<ToolItem>, onToolClick: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            PrecisionPrimary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0f)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrecisionPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = PrecisionPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Create a PDF",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Make a document from images or Word",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    tools.forEach { tool ->
                        Box(modifier = Modifier.weight(1f)) {
                            PrecisionQuickTile(tool = tool, onClick = { onToolClick(tool.route) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrecisionQuickTile(tool: ToolItem, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bouncyScale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PrecisionPrimary.copy(alpha = 0.10f))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(12.dp)
    ) {
        Column {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                tint = PrecisionPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tool.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  TOOL GRID — 2-column cards, domain-washed
// ═══════════════════════════════════════════════════════════
@Composable
private fun ToolGrid(matches: List<ToolItem>, onToolClick: (String) -> Unit) {
    val chunked = matches.chunked(2)
    // Cards wrap their intrinsic height instead of stretching to fill all
    // available space — keeps compact, consistent sizing regardless of
    // how much vertical room the parent provides.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (rowItems in chunked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (tool in rowItems) {
                    Box(modifier = Modifier.weight(1f)) {
                        PrecisionToolCard(tool = tool, onClick = { onToolClick(tool.route) })
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PrecisionToolCard(tool: ToolItem, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bouncyScale"
    )

    val accent = categoryAccent(tool.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Domain-tinted icon chip
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        tint = accent,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                Column {
                    Text(
                        text = tool.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tool.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tool.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun categoryAccent(category: String): Color {
    return when (category) {
        "Convert" -> DomainAmber
        "Organize" -> DomainIndigo
        "Optimize", "Protect" -> DomainEmerald
        else -> PrecisionPrimary
    }
}




// ═══════════════════════════════════════════════════════════
//  DOCK — Rate / Share / Privacy / Settings
// ═══════════════════════════════════════════════════════════
@Composable
private fun HubDock(
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                icon = Icons.Outlined.Star,
                label = "Rate",
                onClick = onRateClick
            )
            DockItem(
                icon = Icons.Outlined.Share,
                label = "Share",
                onClick = onShareClick
            )
            DockItem(
                icon = Icons.Outlined.Shield,
                label = "Privacy",
                onClick = onPrivacyClick
            )
            DockItem(
                icon = Icons.Outlined.Tune,
                label = "Settings",
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ═══════════════════════════════════════════════════════════
//  BANNER AD
// ═══════════════════════════════════════════════════════════
@Composable
private fun AdBanner() {
    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-7057228007467461/7757028756"
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
    )
}


// ═══════════════════════════════════════════════════════════
//  PRIVACY DIALOG
// ═══════════════════════════════════════════════════════════
@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = PrecisionPrimary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                "Privacy policy",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                PrivacyBullet(
                    icon = Icons.Outlined.PhoneAndroid,
                    text = "This app does not collect personal information. Selected files remain on your device."
                )
                Spacer(modifier = Modifier.height(10.dp))
                PrivacyBullet(
                    icon = Icons.Outlined.Info,
                    text = "Google AdMob displays relevant advertisements without storing file data."
                )
                Spacer(modifier = Modifier.height(10.dp))
                PrivacyBullet(
                    icon = Icons.Outlined.CloudOff,
                    text = "All PDF operations run offline locally. No files are uploaded to any server."
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun PrivacyBullet(icon: ImageVector, text: String) {
    Row {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrecisionPrimary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}