package com.yashodatech.pdftoolkit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.components.LiquidGlassCard
import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.LiquidPrimaryButton
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.RecentDoc
import com.yashodatech.pdftoolkit.pdf.PageInfo
import com.yashodatech.pdftoolkit.pdf.PdfPageManager
import com.yashodatech.pdftoolkit.theme.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

// ═══════════════════════════════════════════════════════════
//  PAGE ORGANIZER — thumbnail list, reorder / rotate / delete /
//  duplicate / extract, then save the result as a new PDF.
// ═══════════════════════════════════════════════════════════
@Composable
fun OrganizePagesScreen(onBack: () -> Unit, onOpenDocument: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pageManager = remember { PdfPageManager(context) }
    DisposableEffect(Unit) { onDispose { pageManager.close() } }

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var sourceSize by remember { mutableStateOf("") }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }
    var savedName by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    val pages = remember { mutableStateListOf<PageInfo>() }
    val selected = remember { mutableStateListOf<Int>() }
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    LaunchedEffect(Unit) {
        if (Config.SHOW_ADS) {
            InterstitialAd.load(
                context,
                "ca-app-pub-7057228007467461/5314339450",
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                }
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            sourceUri = it
            savedUri = null
            showSuccess = false
            selected.clear()

            context.contentResolver.query(it, null, null, null, null)?.use { c ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) sourceName = c.getString(nameIdx) ?: "Unknown.pdf"
                    if (sizeIdx >= 0) sourceSize = formatBytes(c.getLong(sizeIdx))
                }
            }

            isLoading = true
            scope.launch {
                totalPages = withContext(Dispatchers.IO) {
                    pageManager.load(it).also { count ->
                        pages.clear()
                        for (i in 1..count) pages.add(PageInfo(sourcePage = i))
                    }
                }
                isLoading = false
            }
        }
    }

    fun save(pagesToSave: List<PageInfo>) {
        isSaving = true
        scope.launch {
            try {
                val isExtract = pagesToSave.size != totalPages
                val name = "${sourceName.removeSuffix(".pdf")}_${if (isExtract) "extracted" else "organized"}"
                val uriStr = withContext(Dispatchers.IO) {
                    pageManager.reorderAndSave(sourceUri!!, pagesToSave, name)
                }
                savedName = "$name.pdf"
                savedUri = Uri.parse(uriStr)
                PreferencesManager(context).addRecentDoc(
                    RecentDoc(
                        name = savedName,
                        uri = uriStr,
                        tool = if (isExtract) "Extract pages" else "Organize pages",
                        timestamp = System.currentTimeMillis()
                    )
                )
                showSuccess = true
                selected.clear()
                if (Config.SHOW_ADS) interstitialAd?.show(context as Activity)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.localizedMessage ?: "Failed to save PDF"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isSaving = false
            }
        }
    }

    fun resetForNew() {
        sourceUri = null
        sourceName = ""
        sourceSize = ""
        totalPages = 0
        pages.clear()
        selected.clear()
        savedUri = null
        savedName = ""
        showSuccess = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LiquidHeader(
                title = "Organize",
                subtitle = "Reorder, rotate, extract pages",
                icon = Icons.Rounded.ViewAgenda,
                gradientColors = GradientOrganizeVibrant,
                onBackClick = onBack,
                statusBadge = "Page Editor"
            )

            ModernGlassLoader(
                isShowing = isSaving,
                title = "Saving PDF...",
                statusText = "Writing pages in the new order"
            )

            if (showSuccess && savedUri != null) {
                OrganizeSuccess(
                    savedUri = savedUri!!,
                    displayName = savedName,
                    onChangeAnother = { resetForNew() },
                    onBack = onBack,
                    onOpenDocument = { onOpenDocument(savedUri!!.toString()) }
                )
            } else {
                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(16.dp))

                    SelectSourceButton(
                        hasFile = sourceUri != null,
                        onClick = { launcher.launch(arrayOf("application/pdf")) }
                    )

                    Spacer(Modifier.height(20.dp))

                    when {
                        sourceUri == null -> OrganizeEmptyState()
                        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Vermilion, modifier = Modifier.size(48.dp))
                        }
                        else -> {
                            PageSummary(name = sourceName, size = sourceSize, count = pages.size)

                            if (pages.size != totalPages) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${totalPages - pages.size} page${if (totalPages - pages.size != 1) "s" else ""} removed",
                                    fontSize = 11.5.sp, color = WarningAmber
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Text("Pages", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkFaint)
                            Spacer(Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                itemsIndexed(pages) { index, page ->
                                    val isSelected = selected.contains(index)
                                    PageRow(
                                        position = index + 1,
                                        page = page,
                                        manager = pageManager,
                                        isSelected = isSelected,
                                        canMoveUp = index > 0,
                                        canMoveDown = index < pages.size - 1,
                                        onToggleSelect = {
                                            if (isSelected) selected.remove(index) else selected.add(index)
                                        },
                                        onRotate = { pages[index] = page.copy(rotation = (page.rotation + 90) % 360) },
                                        onMoveUp = {
                                            if (index > 0) {
                                                val tmp = pages[index - 1]
                                                pages[index - 1] = page
                                                pages[index] = tmp
                                                selected.clear()
                                            }
                                        },
                                        onMoveDown = {
                                            if (index < pages.size - 1) {
                                                val tmp = pages[index + 1]
                                                pages[index + 1] = page
                                                pages[index] = tmp
                                                selected.clear()
                                            }
                                        },
                                        onDuplicate = {
                                            if (pages.size < 500) { pages.add(index + 1, page); selected.clear() }
                                        },
                                        onDelete = {
                                            if (pages.size > 1) { pages.removeAt(index); selected.clear() }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (sourceUri != null && pages.isNotEmpty() && !isLoading && !showSuccess) {
                    OrganizeBottomBar(
                        selectedCount = selected.size,
                        onExtractSelected = { if (selected.isNotEmpty()) save(selected.sorted().map { pages[it] }) },
                        onSaveAll = { save(pages.toList()) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Single page row
// ═══════════════════════════════════════════════════════════
@Composable
private fun PageRow(
    position: Int,
    page: PageInfo,
    manager: PdfPageManager,
    isSelected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleSelect: () -> Unit,
    onRotate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InkSurface)
            .border(
                1.dp,
                if (isSelected) Vermilion else InkBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onToggleSelect)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageThumb(manager = manager, sourcePage = page.sourcePage, rotation = page.rotation, size = 58.dp)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Page $position",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (isSelected) Vermilion else InkBone
            )
            Text("from page ${page.sourcePage}", fontSize = 10.5.sp, color = InkMuted)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically) {
            PageActionButton(Icons.AutoMirrored.Rounded.RotateRight, "Rotate", onRotate)
            PageActionButton(Icons.Rounded.KeyboardArrowUp, "Move up", onMoveUp, enabled = canMoveUp)
            PageActionButton(Icons.Rounded.KeyboardArrowDown, "Move down", onMoveDown, enabled = canMoveDown)
            PageActionButton(Icons.Rounded.ContentCopy, "Duplicate", onDuplicate)
            PageActionButton(Icons.Rounded.DeleteOutline, "Delete", onDelete)
        }
    }
}

@Composable
private fun PageActionButton(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(28.dp)) {
        Icon(
            icon,
            contentDescription = desc,
            tint = if (enabled) InkMuted else InkFaint,
            modifier = Modifier.size(17.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════
//  Lazy thumbnail (PdfRenderer allows one open page at a time)
// ═══════════════════════════════════════════════════════════
@Composable
private fun PageThumb(
    manager: PdfPageManager,
    sourcePage: Int,
    rotation: Int,
    size: Dp
) {
    val mutex = remember { Mutex() }
    val bitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null, key1 = sourcePage, key2 = rotation
    ) {
        value = withContext(Dispatchers.IO) {
            mutex.withLock { manager.renderPage(sourcePage - 1, 112, rotation) }
        }
    }
    Box(
        modifier = Modifier
            .size(width = size, height = size)
            .clip(RoundedCornerShape(8.dp))
            .background(InkSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Support UI
// ═══════════════════════════════════════════════════════════
@Composable
private fun SelectSourceButton(hasFile: Boolean, onClick: () -> Unit) {
    LiquidGlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (hasFile) Icons.Rounded.SwapHoriz else Icons.Rounded.FileOpen,
                contentDescription = null,
                tint = Vermilion,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (hasFile) "Change PDF File" else "Select PDF File",
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InkBone
            )
        }
    }
}

@Composable
private fun OrganizeEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape).background(Vermilion.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ViewAgenda, null, tint = Vermilion.copy(alpha = 0.5f), modifier = Modifier.size(46.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("No PDF selected", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = InkMuted)
            Spacer(Modifier.height(8.dp))
            Text("Select a PDF to organize its pages", fontSize = 13.sp, color = InkFaint)
        }
    }
}

@Composable
private fun PageSummary(name: String, size: String, count: Int) {
    LiquidGlassCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkBone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$size • organizing $count pages", fontSize = 11.sp, color = InkMuted)
            }
            Icon(Icons.Rounded.Description, null, tint = Vermilion, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun OrganizeBottomBar(selectedCount: Int, onExtractSelected: () -> Unit, onSaveAll: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (selectedCount > 0) {
            OutlinedButton(
                onClick = onExtractSelected,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Vermilion),
                border = BorderStroke(1.dp, Vermilion)
            ) {
                Icon(Icons.Rounded.ContentCut, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Extract $selectedCount selected", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        LiquidPrimaryButton(text = "Save PDF", onClick = onSaveAll)
    }
}

@Composable
private fun OrganizeSuccess(savedUri: Uri, displayName: String, onChangeAnother: () -> Unit, onBack: () -> Unit, onOpenDocument: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(0.25f))
        Box(Modifier.size(110.dp).clip(CircleShape).background(Vermilion.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(74.dp).clip(CircleShape).background(Vermilion), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CheckCircle, null, tint = InkBone, modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("PDF saved!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = InkBone)
        Spacer(Modifier.height(8.dp))
        Text("Saved to Downloads", fontSize = 13.sp, color = InkMuted)
        Text(displayName, fontSize = 12.sp, color = InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(28.dp))

        LiquidPrimaryButton(text = "Open Document", onClick = onOpenDocument)
        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, savedUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share PDF"))
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Vermilion),
            border = BorderStroke(1.dp, Vermilion)
        ) {
            Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Share PDF", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        Spacer(Modifier.weight(0.25f))

        LiquidPrimaryButton(text = "Organize Another PDF", onClick = onChangeAnother)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Back to Home", color = InkMuted)
        }
    }
}