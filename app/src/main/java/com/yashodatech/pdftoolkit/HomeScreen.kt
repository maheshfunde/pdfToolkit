package com.yashodatech.pdftoolkit

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.yashodatech.pdftoolkit.components.ModernSecurityBadge
import com.yashodatech.pdftoolkit.components.PrimaryBottomNavigation
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yashodatech.pdftoolkit.data.RecentDoc
import java.util.Calendar

data class ToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val route: String,
    val category: String
)

private data class ToolCategorySection(
    val name: String,
    val tools: List<ToolItem>
)

data class RecentDocDisplay(
    val title: String,
    val metadata: String,
    val uri: String? = null
)

// ═══════════════════════════════════════════════════════════
//  HOME SCREEN — PRECISION PDF TOOLKIT
//  Matches Exact Design Mockup:
//  - One Unified Natural Vertical Scroll
//  - 4 Quick Actions (Merge, Compress, Convert, Open)
//  - Recent Documents with "See all ->"
//  - Tools Section with 4 Category Pills (Create, Organize, Optimize, Secure)
//  - 2-Column Responsive Tool Grid with Icon Container & Chevrons
//  - Bottom Navigation with Center Scan (Active, Never Disabling)
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onToolClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenRecentDoc: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = surfaceColor == DarkBackground || surfaceColor == DarkPrimarySurface || surfaceColor.luminance() < 0.5f

    // Dynamic primary color (Mockup Purple #A78BFA on dark, Red #DC2626 on light)
    val primaryAccent = if (isDark) AccentPrimaryScan else LightAccentRed
    val cardBackground = if (isDark) DarkPrimarySurface else Color.White
    val cardBorder = if (isDark) DarkBorder else Color(0xFFE5E7EB)
    val textPrimary = if (isDark) DarkTextPrimary else Color(0xFF111827)
    val textSecondary = if (isDark) DarkTextSecondary else Color(0xFF4B5563)
    val textMuted = if (isDark) DarkTextMuted else Color(0xFF6B7280)

    var selectedNavTab by rememberSaveable { mutableStateOf("home") }
    var selectedCategoryIndex by rememberSaveable { mutableStateOf(0) }
    var showToolsSheet by rememberSaveable { mutableStateOf(false) }
    var showRecentSheet by rememberSaveable { mutableStateOf(false) }

    // ── Direct Document Scanner ──────────────────────────
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(50)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pdfUri = scanResult?.pdf?.uri
            if (pdfUri != null) {
                scope.launch {
                    val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val name = "Scan_$timeStr.pdf"
                    prefsManager.addRecentDoc(
                        RecentDoc(
                            name = name,
                            uri = pdfUri.toString(),
                            tool = "Scan PDF",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                onOpenRecentDoc(pdfUri.toString())
            } else if (scanResult?.pages?.isNotEmpty() == true) {
                onToolClick(Screen.ImageToPdf.route)
            }
        }
    }

    val launchScanner: () -> Unit = {
        val activity = context as? Activity
        if (activity != null) {
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
                .addOnFailureListener {
                    onToolClick(Screen.ImageToPdf.route)
                }
        } else {
            onToolClick(Screen.ImageToPdf.route)
        }
    }

    // ── Recent docs state ────────────────────────────────
    var recentDocs by remember { mutableStateOf<List<RecentDoc>>(emptyList()) }

    suspend fun loadRecents() {
        val all = prefsManager.recentDocs.first()
        val valid = all.filter { docExists(context, it.uri) }
        if (valid.size != all.size) {
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

    val handleToolClick: (String) -> Unit = { route ->
        if (route == "direct_scanner") {
            launchScanner()
        } else {
            onToolClick(route)
        }
    }

    // ── Tool Catalogue matching mockup palette ───────────
    val allTools = remember {
        listOf(
            // ── CREATE (4 tools - 2x2 grid) ───────────────────
            ToolItem(
                title = "Scan PDF",
                subtitle = "Scan pages with camera",
                icon = Icons.Rounded.DocumentScanner,
                accent = LightAccentRed,
                route = "direct_scanner",
                category = "Create"
            ),
            ToolItem(
                title = "Image to PDF",
                subtitle = "Convert images to PDF",
                icon = Icons.Outlined.Image,
                accent = AccentConvertCreate, // #34D399
                route = Screen.ImageToPdf.route,
                category = "Create"
            ),
            ToolItem(
                title = "Word to PDF",
                subtitle = "Convert DOC/DOCX",
                icon = Icons.Rounded.Description,
                accent = AccentOrganizeEdit, // #F472B6
                route = Screen.WordToPdf.route,
                category = "Create"
            ),
            ToolItem(
                title = "View & Read",
                subtitle = "Read PDF docs",
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                accent = if (isDark) DarkTextPrimary else LightAccentRed,
                route = Screen.ViewPdf.route,
                category = "Create"
            ),

            // ── ORGANIZE (4 tools - 2x2 grid) ──────────────────
            ToolItem(
                title = "Merge PDF",
                subtitle = "Combine multiple files",
                icon = Icons.AutoMirrored.Rounded.CallMerge,
                accent = AccentPrimaryScan, // #A78BFA
                route = Screen.MergePdf.route,
                category = "Organize"
            ),
            ToolItem(
                title = "Split PDF",
                subtitle = "Extract pages",
                icon = Icons.AutoMirrored.Rounded.CallSplit,
                accent = AccentOrganizeEdit, // #F472B6
                route = Screen.SplitPdf.createRoute(),
                category = "Organize"
            ),
            ToolItem(
                title = "Organize Pages",
                subtitle = "Reorder, rotate, delete",
                icon = Icons.Rounded.ViewAgenda,
                accent = AccentCompressOptimize, // #FBBF24
                route = Screen.OrganizePages.createRoute(),
                category = "Organize"
            ),
            ToolItem(
                title = "Watermark",
                subtitle = "Stamp DRAFT or text",
                icon = Icons.AutoMirrored.Rounded.BrandingWatermark,
                accent = AccentConvertCreate, // #34D399
                route = Screen.WatermarkPdf.createRoute(),
                category = "Organize"
            ),

            // ── OPTIMIZE (4 tools - 2x2 grid) ──────────────────
            ToolItem(
                title = "Compress PDF",
                subtitle = "Reduce file size",
                icon = Icons.Rounded.Compress,
                accent = AccentCompressOptimize, // #FBBF24
                route = Screen.CompressPdf.createRoute(),
                category = "Optimize"
            ),
            ToolItem(
                title = "PDF to Word",
                subtitle = "Convert to editable DOCX",
                icon = Icons.Rounded.Description,
                accent = AccentOrganizeEdit, // #F472B6
                route = Screen.ExportWord.route,
                category = "Optimize"
            ),
            ToolItem(
                title = "PDF to Images",
                subtitle = "Export pages as JPG/PNG",
                icon = Icons.Rounded.PhotoLibrary,
                accent = AccentConvertCreate, // #34D399
                route = Screen.ExportImages.route,
                category = "Optimize"
            ),
            ToolItem(
                title = "Extract Text (OCR)",
                subtitle = "Make PDF searchable",
                icon = Icons.Outlined.DocumentScanner,
                accent = AccentPrimaryScan, // #A78BFA
                route = Screen.Ocr.route,
                category = "Optimize"
            ),

            // ── SECURE (2 tools - 1x2 grid) ────────────────────
            ToolItem(
                title = "Unlock PDF",
                subtitle = "Remove password",
                icon = Icons.Rounded.LockOpen,
                accent = AccentSecurityUnlock, // #F87171
                route = Screen.UnlockPdf.route,
                category = "Secure"
            ),
            ToolItem(
                title = "Watermark PDF",
                subtitle = "Add copyright stamp",
                icon = Icons.AutoMirrored.Rounded.BrandingWatermark,
                accent = AccentConvertCreate, // #34D399
                route = Screen.WatermarkPdf.createRoute(),
                category = "Secure"
            )
        )
    }

    val categorySections = remember(allTools) {
        listOf(
            ToolCategorySection("Create", allTools.filter { it.category == "Create" }),
            ToolCategorySection("Organize", allTools.filter { it.category == "Organize" }),
            ToolCategorySection("Optimize", allTools.filter { it.category == "Optimize" }),
            ToolCategorySection("Secure", allTools.filter { it.category == "Secure" })
        )
    }

    val categoryPagerState = rememberPagerState(pageCount = { categorySections.size })

    // Keep active category in step with pager
    LaunchedEffect(categoryPagerState) {
        snapshotFlow { categoryPagerState.currentPage }.collect {
            selectedCategoryIndex = it
        }
    }

    Scaffold(
        bottomBar = {
            PrimaryBottomNavigation(
                selectedRoute = selectedNavTab,
                onHomeClick = {
                    selectedNavTab = "home"
                    scope.launch { listState.animateScrollToItem(0) }
                },
                onToolsClick = {
                    selectedNavTab = "tools"
                    showToolsSheet = true
                },
                onScanClick = launchScanner,
                onRecentClick = {
                    selectedNavTab = "recent"
                    showRecentSheet = true
                },
                onSettingsClick = onSettingsClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // ONE PRIMARY VERTICAL SCROLL LAZYCOLUMN
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. HEADER ───────────────────────────────────────
            item(key = "header") {
                HomeHeader(
                    onSearchClick = { showToolsSheet = true },
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    isDark = isDark
                )
            }

            // ── 2. OPEN / IMPORT PDF HERO CARD ───────────────────
            item(key = "hero_open") {
                HeroOpenCard(
                    onClick = { onToolClick(Screen.ViewPdf.route) },
                    accent = primaryAccent,
                    cardBg = cardBackground,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            // ── 3. QUICK ACTIONS ROW ────────────────────────────
            item(key = "quick_actions") {
                QuickActionsSection(
                    onToolClick = handleToolClick,
                    cardBg = cardBackground,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    isDark = isDark
                )
            }

            // ── 4. TOOLS DIRECTORY (3-Column View) ──────────────
            item(key = "tools_directory") {
                ToolsSection(
                    categories = categorySections,
                    pagerState = categoryPagerState,
                    selectedIndex = selectedCategoryIndex,
                    onSelectCategory = { index ->
                        scope.launch { categoryPagerState.animateScrollToPage(index) }
                    },
                    onToolClick = handleToolClick,
                    primaryAccent = primaryAccent,
                    cardBg = cardBackground,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    textMuted = textMuted,
                    isDark = isDark
                )
            }

            // ── 5. AD BANNER (IF ENABLED) ───────────────────────
            if (Config.SHOW_ADS) {
                item(key = "ad_banner") {
                    AdBanner()
                }
            }
        }
    }

    // ── TOOLS CARD OPENING FROM NAVBAR UPWARDS ──────────────
    if (showToolsSheet) {
        ToolsBottomSheet(
            onDismiss = {
                showToolsSheet = false
                selectedNavTab = "home"
            },
            categories = categorySections,
            allTools = allTools,
            onToolClick = handleToolClick,
            cardBg = cardBackground,
            cardBorder = cardBorder,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textMuted = textMuted,
            primaryAccent = primaryAccent,
            isDark = isDark
        )
    }

    // ── RECENT DOCUMENTS CARD OPENING FROM NAVBAR UPWARDS ───
    if (showRecentSheet) {
        RecentDocumentsBottomSheet(
            onDismiss = {
                showRecentSheet = false
                selectedNavTab = "home"
            },
            recentDocs = recentDocs,
            onOpenDoc = { uri ->
                onOpenRecentDoc(uri)
            },
            onRemoveDoc = { uriToRemove ->
                scope.launch {
                    val updated = recentDocs.filter { it.uri != uriToRemove }
                    prefsManager.setRecentDocs(updated)
                    recentDocs = updated
                }
            },
            onClearAll = {
                scope.launch {
                    prefsManager.setRecentDocs(emptyList())
                    recentDocs = emptyList()
                }
            },
            onBrowseClick = {
                onToolClick(Screen.ViewPdf.route)
            },
            cardBg = cardBackground,
            cardBorder = cardBorder,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textMuted = textMuted,
            primaryAccent = primaryAccent,
            isDark = isDark
        )
    }
}

// ═══════════════════════════════════════════════════════════
//  HOME HEADER
// ═══════════════════════════════════════════════════════════
@Composable
private fun HomeHeader(
    onSearchClick: () -> Unit,
    textPrimary: Color,
    textMuted: Color,
    isDark: Boolean
) {
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
                color = textMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "PDF Toolkit",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ModernSecurityBadge()
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSearchClick),
                shape = CircleShape,
                color = if (isDark) DarkElevatedSurface else LightPrimarySurface,
                border = BorderStroke(1.dp, if (isDark) DarkBorder else LightBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search tools",
                        tint = if (isDark) DarkTextSecondary else LightTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  HERO OPEN / IMPORT CARD
// ═══════════════════════════════════════════════════════════
@Composable
private fun HeroOpenCard(
    onClick: () -> Unit,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = "Open Document",
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Open / Import PDF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "View, read, print or edit any PDF document",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(50),
                color = accent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Browse",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  QUICK ACTIONS ROW (Merge PDF, Compress, Convert, Open PDF)
// ═══════════════════════════════════════════════════════════
@Composable
private fun QuickActionsSection(
    onToolClick: (String) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textMuted: Color,
    isDark: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelSmall,
            color = textMuted,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionTile(
                title = "Merge",
                icon = Icons.AutoMirrored.Rounded.CallMerge,
                accent = if (isDark) AccentPrimaryScan else LightAccentRed,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onToolClick(Screen.MergePdf.route) }
            )
            QuickActionTile(
                title = "Split",
                icon = Icons.AutoMirrored.Rounded.CallSplit,
                accent = if (isDark) AccentOrganizeEdit else LightAccentRed,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onToolClick(Screen.SplitPdf.createRoute()) }
            )
            QuickActionTile(
                title = "Compress",
                icon = Icons.Rounded.Compress,
                accent = if (isDark) AccentCompressOptimize else LightAccentRed,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onToolClick(Screen.CompressPdf.createRoute()) }
            )
            QuickActionTile(
                title = "Convert",
                icon = Icons.Rounded.Description,
                accent = if (isDark) AccentConvertCreate else LightAccentRed,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onToolClick(Screen.WordToPdf.route) }
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val iconBg = if (isDark) accent.copy(alpha = 0.16f) else Color(0xFFFEF2F2)
            val iconColor = if (isDark) accent else LightAccentRed

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  RECENT DOCUMENTS BOTTOM SHEET (Navbar -> Slide Up)
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentDocumentsBottomSheet(
    onDismiss: () -> Unit,
    recentDocs: List<RecentDoc>,
    onOpenDoc: (String) -> Unit,
    onRemoveDoc: (String) -> Unit,
    onClearAll: () -> Unit,
    onBrowseClick: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    primaryAccent: Color,
    isDark: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) DarkPrimarySurface else Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Documents",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                if (recentDocs.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = "Clear all",
                            color = primaryAccent,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recentDocs.isEmpty()) {
                // Empty state when no documents opened yet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isDark) DarkElevatedSurface else Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = if (isDark) textMuted else LightAccentRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No recent documents opened",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PDFs you view, scan, or create will appear here for quick access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onBrowseClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open a PDF")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentDocs, key = { it.uri }) { doc ->
                        var menuExpanded by remember { mutableStateOf(false) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onDismiss()
                                    onOpenDoc(doc.uri)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) DarkElevatedSurface else Color.White,
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatRecentTime(doc.timestamp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.MoreVert,
                                            contentDescription = "Options",
                                            tint = textMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(if (isDark) DarkElevatedSurface else Color.White)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Open", color = textPrimary) },
                                            onClick = {
                                                menuExpanded = false
                                                onDismiss()
                                                onOpenDoc(doc.uri)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = primaryAccent)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Remove from recent", color = textPrimary) },
                                            onClick = {
                                                menuExpanded = false
                                                onRemoveDoc(doc.uri)
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = textMuted)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  ALL TOOLS BOTTOM SHEET (Navbar -> Slide Up)
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsBottomSheet(
    onDismiss: () -> Unit,
    categories: List<ToolCategorySection>,
    allTools: List<ToolItem>,
    onToolClick: (String) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    primaryAccent: Color,
    isDark: Boolean
) {
    var selectedCat by rememberSaveable { mutableStateOf("All") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val displayedTools = remember(selectedCat, allTools) {
        if (selectedCat == "All") allTools
        else allTools.filter { it.category == selectedCat }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) DarkPrimarySurface else Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Tools",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Pills: All | Create | Organize | Optimize | Secure
            val filterTabs = remember { listOf("All", "Create", "Organize", "Optimize", "Secure") }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterTabs) { tab ->
                    val isSelected = selectedCat == tab
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedCat = tab },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) primaryAccent
                        else if (isDark) DarkElevatedSurface else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) primaryAccent
                            else cardBorder
                        )
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textMuted,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2-Column Tool Grid inside Bottom Sheet on phones
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val chunked = displayedTools.chunked(2)
                items(chunked) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (tool in rowItems) {
                            Box(modifier = Modifier.weight(1f)) {
                                ToolCard(
                                    tool = tool,
                                    onClick = {
                                        onDismiss()
                                        onToolClick(tool.route)
                                    },
                                    cardBg = if (isDark) DarkElevatedSurface else Color.White,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    textMuted = textMuted,
                                    isDark = isDark
                                )
                            }
                        }
                        val missing = 2 - rowItems.size
                        if (missing > 0) {
                            for (i in 0 until missing) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  TOOLS SECTION (Category Pills & 3-Column Tool Grid)
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolsSection(
    categories: List<ToolCategorySection>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    selectedIndex: Int,
    onSelectCategory: (Int) -> Unit,
    onToolClick: (String) -> Unit,
    primaryAccent: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    isDark: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TOOLS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textMuted,
            letterSpacing = 0.6.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4 Category Pills: Create | Organize | Optimize | Secure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEachIndexed { index, cat ->
                val isSelected = selectedIndex == index
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectCategory(index) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) primaryAccent
                    else if (isDark) DarkElevatedSurface else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) primaryAccent
                        else cardBorder
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Category Content with full horizontal swiping
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            pageSpacing = 0.dp
        ) { page ->
            val cat = categories.getOrElse(page) { categories.first() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 240.dp)
            ) {
                ToolGrid(
                    tools = cat.tools,
                    onToolClick = onToolClick,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    textMuted = textMuted,
                    isDark = isDark
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  TOOL GRID (Responsive 2-Column on Phone, 3-Column on Tablet)
// ═══════════════════════════════════════════════════════════
@Composable
private fun ToolGrid(
    tools: List<ToolItem>,
    onToolClick: (String) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    isDark: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        val chunked = tools.chunked(columns)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (rowItems in chunked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (tool in rowItems) {
                        Box(modifier = Modifier.weight(1f)) {
                            ToolCard(
                                tool = tool,
                                onClick = { onToolClick(tool.route) },
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                textMuted = textMuted,
                                isDark = isDark
                            )
                        }
                    }
                    val missing = columns - rowItems.size
                    if (missing > 0) {
                        for (i in 0 until missing) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    tool: ToolItem,
    onClick: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    isDark: Boolean
) {
    val iconTint = if (isDark) tool.accent else LightAccentRed
    val iconBg = if (isDark) tool.accent.copy(alpha = 0.16f) else Color(0xFFFEF2F2)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorder),
        color = cardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Square icon container
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = tool.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  HELPERS
// ═══════════════════════════════════════════════════════════
private fun formatRecentTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
        diff < 604_800_000L -> "${diff / 86_400_000L} d ago"
        else -> "${diff / 86_400_000L} d ago"
    }
}

@Suppress("SwallowedException")
private fun docExists(context: Context, uri: String): Boolean = try {
    context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
} catch (_: Exception) {
    false
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