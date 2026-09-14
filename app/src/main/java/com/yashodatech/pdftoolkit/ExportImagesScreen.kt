package com.yashodatech.pdftoolkit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.components.LiquidGlassCard
import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.LiquidPrimaryButton
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.components.PrecisionFileSelectCard
import com.yashodatech.pdftoolkit.pdf.ExportFormat
import com.yashodatech.pdftoolkit.pdf.PdfImageExporter
import com.yashodatech.pdftoolkit.theme.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun formatImgBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

// ═══════════════════════════════════════════════════════════
//  PDF → IMAGES — render every page as a JPG / PNG in Pictures.
// ═══════════════════════════════════════════════════════════
@Composable
fun ExportImagesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var sourceSize by remember { mutableStateOf("") }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showSuccess by remember { mutableStateOf(false) }

    // Selected pages to export (0-based indices into the source PDF).
    val selectedPages = remember { mutableStateListOf<Int>() }
    // Rendered page previews (index-aligned with the source PDF). Caller recycles.
    var pageThumbs by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    // Options
    var format by remember { mutableStateOf(ExportFormat.JPG) }
    var quality by remember { mutableStateOf(92) }

    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    // Recycle preview bitmaps when this screen leaves composition.
    val currentThumbs by rememberUpdatedState(pageThumbs)
    DisposableEffect(Unit) {
        onDispose { currentThumbs.forEach { it.recycle() } }
    }

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
            exportedUris = emptyList()
            showSuccess = false
            selectedPages.clear()

            context.contentResolver.query(it, null, null, null, null)?.use { c ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) sourceName = c.getString(nameIdx) ?: "Unknown.pdf"
                    if (sizeIdx >= 0) sourceSize = formatImgBytes(c.getLong(sizeIdx))
                }
            }

            isLoading = true
            scope.launch {
                val exporter = PdfImageExporter(context)
                val (count, thumbs) = withContext(Dispatchers.IO) {
                    exporter.validate(it) to exporter.renderThumbnails(it)
                }
                totalPages = count
                pageThumbs.forEach { it.recycle() }
                pageThumbs = thumbs
                // Default: every page selected.
                selectedPages.clear()
                for (i in 0 until totalPages) selectedPages.add(i)
                isLoading = false
            }
        }
    }

    fun export() {
        if (sourceUri == null || selectedPages.isEmpty()) return
        isExporting = true
        scope.launch {
            try {
                val uris = withContext(Dispatchers.IO) {
                    PdfImageExporter(context).exportPages(
                        sourceUri = sourceUri!!,
                        baseName = sourceName,
                        pageIndices = selectedPages.sorted(),
                        format = format,
                        quality = quality
                    )
                }
                exportedUris = uris.map { Uri.parse(it) }
                showSuccess = true
                if (Config.SHOW_ADS) interstitialAd?.show(context as Activity)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.localizedMessage ?: "Export failed"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isExporting = false
            }
        }
    }

    fun resetForNew() {
        sourceUri = null
        sourceName = ""
        sourceSize = ""
        totalPages = 0
        selectedPages.clear()
        pageThumbs.forEach { it.recycle() }
        pageThumbs = emptyList()
        exportedUris = emptyList()
        showSuccess = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LiquidHeader(
                title = "PDF to Images",
                subtitle = "Export every page as JPG or PNG",
                icon = Icons.Rounded.PhotoLibrary,
                gradientColors = GradientExportImagesVibrant,
                onBackClick = onBack,
                statusBadge = "Export"
            )

            ModernGlassLoader(
                isShowing = isExporting,
                title = "Exporting images...",
                statusText = "Rendering each page"
            )

            if (showSuccess && exportedUris.isNotEmpty()) {
                ExportImagesSuccess(
                    count = exportedUris.size,
                    format = format,
                    onView = { openGallery(context, exportedUris) },
                    onExportAnother = { resetForNew() },
                    onBack = onBack
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(16.dp))

                    PrecisionFileSelectCard(
                        title = if (sourceUri != null) "Change PDF File" else "Select PDF File",
                        subtitle = "Tap to choose a .pdf file",
                        chipLabel = ".PDF",
                        gradient = GradientExportImagesVibrant,
                        icon = Icons.Rounded.FileOpen,
                        onClick = { launcher.launch(arrayOf("application/pdf")) }
                    )

                    if (sourceUri != null) {
                        Spacer(Modifier.height(16.dp))

                        if (isLoading) {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Vermilion, modifier = Modifier.size(44.dp))
                            }
                        } else {
                            // ── File + page info ──
                            LiquidGlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(sourceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkBone(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("$sourceSize • $totalPages page${if (totalPages != 1) "s" else ""}", fontSize = 11.sp, color = InkMuted())
                                    }
                                    Icon(Icons.Rounded.Description, null, tint = Vermilion, modifier = Modifier.size(24.dp))
                                }
                            }

                            // ── Pages ──
                            Spacer(Modifier.height(20.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pages to export", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkFaint(), modifier = Modifier.weight(1f))
                                Text(
                                    if (selectedPages.size == totalPages) "All $totalPages"
                                    else "${selectedPages.size} selected",
                                    fontSize = 12.sp, color = Vermilion, fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PageActionChip(label = "Select all", selected = selectedPages.size == totalPages, onClick = {
                                    selectedPages.clear()
                                    for (i in 0 until totalPages) selectedPages.add(i)
                                })
                                PageActionChip(label = "None", selected = selectedPages.isEmpty(), onClick = { selectedPages.clear() })
                            }
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 4.dp)
                            ) {
                                items(totalPages) { index ->
                                    val thumb = pageThumbs.getOrNull(index)?.asImageBitmap()
                                    PageSelectTile(
                                        pageNumber = index + 1,
                                        thumbnail = thumb,
                                        selected = selectedPages.contains(index),
                                        onClick = {
                                            if (selectedPages.contains(index)) selectedPages.remove(index)
                                            else selectedPages.add(index)
                                        }
                                    )
                                }
                            }

                            // ── Format ──
                            Spacer(Modifier.height(20.dp))
                            Text("Format", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkFaint())
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FormatChip(label = "JPG", desc = "smaller", selected = format == ExportFormat.JPG, onClick = { format = ExportFormat.JPG })
                                FormatChip(label = "PNG", desc = "lossless", selected = format == ExportFormat.PNG, onClick = { format = ExportFormat.PNG })
                            }

                            // ── Quality (JPG only) ──
                            if (format == ExportFormat.JPG) {
                                Spacer(Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Quality", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkFaint(), modifier = Modifier.weight(1f))
                                    Text("$quality%", fontSize = 12.sp, color = Vermilion, fontWeight = FontWeight.SemiBold)
                                }
                                Slider(
                                    value = quality.toFloat(),
                                    onValueChange = { quality = it.roundToInt() },
                                    valueRange = 50f..100f,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Vermilion,
                                        inactiveTrackColor = InkBorder,
                                        thumbColor = Vermilion
                                    )
                                )
                                Text("Saved to Pictures/PDF Toolkit at full page resolution", fontSize = 11.sp, color = InkMuted())
                            } else {
                                Spacer(Modifier.height(8.dp))
                                Text("PNG is saved lossless at full page resolution", fontSize = 11.sp, color = InkMuted())
                            }

                            Spacer(Modifier.height(20.dp))
                            val n = selectedPages.size
                            LiquidPrimaryButton(
                                text = if (n == totalPages) "Export $n Images"
                                        else "Export $n Image${if (n != 1) "s" else ""}",
                                onClick = { export() },
                                enabled = sourceUri != null && !isLoading && n > 0
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    } else {
                        Spacer(Modifier.height(120.dp))
                        ExportEmptyState()
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Format selector chip
// ═══════════════════════════════════════════════════════════
@Composable
private fun FormatChip(label: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Vermilion.copy(alpha = 0.18f) else InkSurface)
            .border(1.dp, if (selected) Vermilion else InkBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) Vermilion else InkBone()
        )
        Text(desc, fontSize = 10.sp, color = InkMuted())
    }
}

// ═══════════════════════════════════════════════════════════
//  Page selection tiles
// ═══════════════════════════════════════════════════════════
@Composable
private fun PageActionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Vermilion.copy(alpha = 0.18f) else InkSurface)
            .border(1.dp, if (selected) Vermilion else InkBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Vermilion else InkMuted()
        )
    }
}

@Composable
private fun PageSelectTile(
    pageNumber: Int,
    thumbnail: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val height = 92.dp
    val aspect = if (thumbnail != null && thumbnail.height > 0)
        thumbnail.width.toFloat() / thumbnail.height else 0.7f
    Box(
        modifier = Modifier
            .width(height * aspect)
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(InkSurface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Vermilion else InkBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = "Page $pageNumber preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
        // Selected tint over the preview
        if (selected) {
            Box(Modifier.fillMaxSize().background(Vermilion.copy(alpha = 0.30f)))
        }
        // Page-number badge, top-left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text("$pageNumber", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        // Check mark, top-right when selected
        if (selected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Vermilion,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Gallery opener + empty + success
// ═══════════════════════════════════════════════════════════
fun openGallery(context: android.content.Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uris.first(), context.contentResolver.getType(uris.first()) ?: "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No image viewer found", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ExportEmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(100.dp).clip(CircleShape).background(Vermilion.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PhotoLibrary, null, tint = Vermilion.copy(alpha = 0.5f), modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("No PDF selected", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = InkMuted())
        Spacer(Modifier.height(8.dp))
        Text("Select a PDF to export its pages as images", fontSize = 13.sp, color = InkFaint(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ExportImagesSuccess(
    count: Int,
    format: ExportFormat,
    onView: () -> Unit,
    onExportAnother: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(0.25f))
        Box(Modifier.size(110.dp).clip(CircleShape).background(Vermilion.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(74.dp).clip(CircleShape).background(Vermilion), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PhotoLibrary, null, tint = InkBone(), modifier = Modifier.size(38.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Images exported!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = InkBone())
        Spacer(Modifier.height(8.dp))
        Text("$count ${format.ext.uppercase()} saved to Pictures/PDF Toolkit", fontSize = 13.sp, color = InkMuted())
        Spacer(Modifier.height(28.dp))

        LiquidPrimaryButton(text = "View Images", onClick = onView)

        Spacer(Modifier.weight(0.25f))

        LiquidPrimaryButton(text = "Export Another PDF", onClick = onExportAnother)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Back to Home", color = InkMuted())
        }
    }
}