package com.yashodatech.pdftoolkit

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.RecentDoc
import com.yashodatech.pdftoolkit.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

// ═══════════════════════════════════════════════════════════
//  HOME SCREEN — EDITORIAL INK DASHBOARD
//  Icon-led tools, one vermilion stamp, quiet hairline surfaces.
// ═══════════════════════════════════════════════════════════
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
    var lastUsedTool by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var recentDocs by remember { mutableStateOf<List<RecentDoc>>(emptyList()) }

    val privacyUrl = Config.PRIVACY_POLICY_URL

    LaunchedEffect(Unit) {
        val openCount = prefsManager.appOpenCount.first()
        val hasRated = prefsManager.hasRated.first()
        lastUsedTool = prefsManager.lastUsedTool.first()
        recentDocs = prefsManager.recentDocs.first()

        if (!hasRated && openCount > 0 && openCount % 5 == 0) {
            showRateDialog = true
        }
    }

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

    val filteredTools = remember(searchQuery, tools) {
        if (searchQuery.isBlank()) tools
        else tools.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true)
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
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // ── Editorial Header & Search ──────────────────────
            EditorialHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSettingsClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Recently edited ───────────────────────────────
            if (recentDocs.isNotEmpty() && searchQuery.isBlank()) {
                RecentDocsRow(docs = recentDocs, onOpen = { onOpenRecentDoc(it) })
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ── Tool Grid ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filteredTools.isEmpty()) {
                    EmptyStateBox(searchQuery = searchQuery)
                } else {
                    // Scrollable grid with fixed-height rows so the icon AND the
                    // title/subtitle/category text always fit inside every card.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val chunked = filteredTools.chunked(2)
                        for (rowItems in chunked) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(148.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (tool in rowItems) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        EditorialToolCard(
                                            tool = tool,
                                            onClick = { onToolClick(tool.route) }
                                        )
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Quiet Hairline Dock ───────────────────────────
            EditorialDock(
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

            // ── Banner Ad ───────────────────────────────────
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
//  EDITORIAL HEADER — big wordmark, hairline search, quiet settings
// ═══════════════════════════════════════════════════════════
@Composable
private fun EditorialHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = greeting,
                    color = InkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "PDF Toolkit",
                    color = InkBone,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Offline status — quiet, functional
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(InkSurface)
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
                            text = "Offline",
                            color = InkMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Settings — quiet glyph
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(InkSurface)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = InkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Search — hairline-bordered field, not a glass pill
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = InkSurface,
            border = BorderStroke(1.dp, InkBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = InkMuted,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Search tools — merge, compress…",
                            color = InkFaint,
                            fontSize = 13.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = InkBone,
                        unfocusedTextColor = InkBone,
                        cursorColor = Vermilion
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = InkMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  EDITORIAL TOOL CARD — icon-led, one mono chip, one vermilion arrow
// ═══════════════════════════════════════════════════════════
@Composable
private fun EditorialToolCard(tool: ToolItem, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bouncyScale"
    )

    Card(
        modifier = Modifier
            .fillMaxSize()
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
                    onTap = {
                        onClick()
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, InkBorder),
        colors = CardDefaults.cardColors(
            containerColor = InkSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Mono icon chip on a raised tile
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = InkBone,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & subtitle live right under the icon so they are always
            // visible even in a short card.
            Column {
                Text(
                    text = tool.title,
                    color = Color(0xFFF7F4EE),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.subtitle,
                    color = InkMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // One action affordance — category label + vermilion chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tool.category,
                    color = InkFaint,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Vermilion,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  EMPTY STATE
// ═══════════════════════════════════════════════════════════
@Composable
private fun EmptyStateBox(searchQuery: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = InkFaint,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No tools matching “$searchQuery”",
                fontSize = 13.sp,
                color = InkMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  RECENTLY EDITED DOCS — horizontal shelf, tap to reopen in-app
// ═══════════════════════════════════════════════════════════
@Composable
private fun RecentDocsRow(docs: List<RecentDoc>, onOpen: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently edited",
                color = InkFaint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${docs.size} doc${if (docs.size != 1) "s" else ""}",
                color = InkFaint,
                fontSize = 10.5.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(docs) { doc ->
                RecentDocCard(doc = doc, onClick = { onOpen(doc.uri) })
            }
        }
    }
}

@Composable
private fun RecentDocCard(doc: RecentDoc, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, InkBorder),
        colors = CardDefaults.cardColors(containerColor = InkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = Vermilion,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.name,
                    color = InkBone,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${doc.tool} • ${formatRecentTime(doc.timestamp)}",
                    color = InkMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = InkFaint,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun formatRecentTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    val min = diff / 60_000
    return when {
        min < 1 -> "just now"
        min < 60 -> "${min}m ago"
        min < 1440 -> "${min / 60}h ago"
        else -> "${min / 1440}d ago"
    }
}


// ═══════════════════════════════════════════════════════════
//  QUIET HAIRLINE DOCK
// ═══════════════════════════════════════════════════════════
@Composable
private fun EditorialDock(
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
        color = InkSurface,
        border = BorderStroke(1.dp, InkBorder)
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
                tint = InkMuted,
                onClick = onRateClick
            )
            DockItem(
                icon = Icons.Outlined.Share,
                label = "Share",
                tint = InkMuted,
                onClick = onShareClick
            )
            DockItem(
                icon = Icons.Outlined.Shield,
                label = "Privacy",
                tint = InkMuted,
                onClick = onPrivacyClick
            )
            DockItem(
                icon = Icons.Outlined.Tune,
                label = "Settings",
                tint = InkMuted,
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    tint: Color,
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
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = InkMuted,
            fontWeight = FontWeight.SemiBold
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
        containerColor = InkSurface,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = Vermilion,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text("Privacy policy", color = InkBone, fontWeight = FontWeight.Bold)
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
            tint = Vermilion,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = InkMuted
        )
    }
}