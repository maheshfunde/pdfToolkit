package com.yashodatech.pdftoolkit

import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.components.PrecisionFileSelectCard
import com.yashodatech.pdftoolkit.components.PrecisionGradientButton
import com.yashodatech.pdftoolkit.theme.GradientCompressVibrant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.pdf.CompressionLevel
import com.yashodatech.pdftoolkit.pdf.PdfCompressor
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.yashodatech.pdftoolkit.pdf.PdfSplitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs


// ── Gradient constants ──────────────────────────────────
private val GradientCompress = listOf(Color(0xFFE4572E), Color(0xFFB23F1F))
private val GradientSuccess  = listOf(Color(0xFFE4572E), Color(0xFFB23F1F))
private val AccentCompress   = Color(0xFFE4572E)
private val SuccessColor     = Color(0xFFE4572E)


// ═══════════════════════════════════════════════════════════
//  MAIN SCREEN
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressPdfScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Source PDF state
    var sourceUri      by remember { mutableStateOf<Uri?>(null) }
    var sourceName     by remember { mutableStateOf("") }
    var sourceSize     by remember { mutableStateOf(0L) }
    var sourceSizeText by remember { mutableStateOf("") }
    var totalPages     by remember { mutableIntStateOf(0) }
    var isLoadingInfo  by remember { mutableStateOf(false) }

    // Compression settings
    var compressionLevel by remember { mutableStateOf(CompressionLevel.MEDIUM) }

    // Processing state
    var isCompressing  by remember { mutableStateOf(false) }
    var savedPdfUri    by remember { mutableStateOf<Uri?>(null) }
    var compressedSize by remember { mutableStateOf(0L) }
    var outputFileName by remember { mutableStateOf("") }
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    // Success screen state
    var showSuccessScreen by remember { mutableStateOf(false) }

    // ── Load interstitial ad ────────────────────────
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

    // ── PDF picker launcher ─────────────────────────
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            sourceUri = it
            savedPdfUri = null
            compressedSize = 0L
            showSuccessScreen = false

            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) sourceName = cursor.getString(nameIdx) ?: "Unknown.pdf"
                    if (sizeIdx >= 0) {
                        sourceSize = cursor.getLong(sizeIdx)
                        sourceSizeText = formatSize(sourceSize)
                    }
                }
            }

            isLoadingInfo = true
            scope.launch {
                totalPages = withContext(Dispatchers.IO) {
                    PdfSplitter().getPageCount(context, it)
                }
                isLoadingInfo = false
            }
        }
    }

    // ── Compress helper ─────────────────────────────
    fun compressPdf() {
        isCompressing = true
        scope.launch {
            try {
                val outName = "compressed_$sourceName"
                val result = withContext(Dispatchers.IO) {
                    PdfCompressor().compressPdf(
                        context = context,
                        sourceUri = sourceUri!!,
                        outputFileName = outName,
                        level = compressionLevel
                    )
                }

                savedPdfUri    = Uri.parse(result.first)
                compressedSize = result.second
                outputFileName = outName
                showSuccessScreen = true

                if (Config.SHOW_ADS) {
                    interstitialAd?.show(context as Activity)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.localizedMessage ?: "Failed to compress PDF"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isCompressing = false
            }
        }
    }

    // ── Reset helper ────────────────────────────────
    fun resetForNew() {
        sourceUri = null
        sourceName = ""
        sourceSize = 0L
        sourceSizeText = ""
        totalPages = 0
        savedPdfUri = null
        compressedSize = 0L
        outputFileName = ""
        showSuccessScreen = false
        compressionLevel = CompressionLevel.MEDIUM
    }

    // ═════════════════════════════════════════════════
    //  SCAFFOLD
    // ═════════════════════════════════════════════════
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            LiquidHeader(
                title = "Compress PDF",
                subtitle = "Reduce document storage size up to 80%",
                icon = Icons.Rounded.Compress,
                gradientColors = GradientCompressVibrant,
                onBackClick = onBack,
                statusBadge = "Fast Engine"
            )

            ModernGlassLoader(
                isShowing = isCompressing,
                title = "Compressing PDF...",
                statusText = "Optimizing pages, images & document streams"
            )

            if (showSuccessScreen && savedPdfUri != null) {

                // ═════════════════════════════════
                //  SUCCESS SCREEN
                // ═════════════════════════════════
                CompressSuccessScreen(
                    fileName = outputFileName,
                    originalSize = sourceSize,
                    compressedSize = compressedSize,
                    onOpen = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(savedPdfUri, "application/pdf")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to open PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShare = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, savedPdfUri)
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                },
                                "Share Compressed PDF"
                            )
                        )
                    },
                    onCompressAnother = { resetForNew() },
                    onBack = onBack
                )

            } else {

                // ═════════════════════════════════
                //  EDITOR SCREEN
                // ═════════════════════════════════
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {

                    Spacer(Modifier.height(16.dp))

                    CompressSelectButton(
                        hasFile = sourceUri != null,
                        onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }
                    )

                    Spacer(Modifier.height(20.dp))

                    if (sourceUri == null) {
                        CompressEmptyState()
                    } else if (isLoadingInfo) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AccentCompress,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else {

                        CompressFileInfoCard(
                            name = sourceName,
                            size = sourceSizeText,
                            totalPages = totalPages
                        )

                        Spacer(Modifier.height(20.dp))

                        CompressionLevelSelector(
                            selected = compressionLevel,
                            onSelect = {
                                compressionLevel = it
                                savedPdfUri = null
                                compressedSize = 0L
                            }
                        )

                        Spacer(Modifier.height(20.dp))

                        EstimatedSizeCard(
                            originalSize = sourceSize,
                            compressionLevel = compressionLevel
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                }

                // ── Bottom Compress Button ──────
                if (sourceUri != null && totalPages > 0 && !isLoadingInfo) {
                    CompressBottomArea(onCompressClick = { compressPdf() })
                }
            }
        }

        // ── Compressing Dialog ──────────────────
        if (isCompressing) {
            CompressingPdfDialog()
        }
    }
}


// ── Helper ──────────────────────────────────────────────
private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024          -> "$bytes B"
        bytes < 1024 * 1024   -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}


// ═══════════════════════════════════════════════════════════
//  SUCCESS SCREEN
// ═══════════════════════════════════════════════════════════
@Composable
private fun CompressSuccessScreen(
    fileName: String,
    originalSize: Long,
    compressedSize: Long,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCompressAnother: () -> Unit,
    onBack: () -> Unit
) {
    val savedBytes   = originalSize - compressedSize
    val savedPercent = if (originalSize > 0)
        ((savedBytes.toFloat() / originalSize) * 100).toInt() else 0
    val didReduce = compressedSize < originalSize

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(Modifier.weight(0.2f))

        // ── Success / Warning Icon ──────────────
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    if (didReduce) SuccessColor.copy(alpha = 0.1f)
                    else Color(0xFFFFA726).copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (didReduce) GradientSuccess
                            else listOf(Color(0xFFFFA726), Color(0xFFFF7043))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (didReduce) Icons.Rounded.CheckCircle
                    else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = if (didReduce) "File Compressed Successfully!"
            else "Compression Complete",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        // ── File Name Badge ─────────────────────
        Surface(
            color = AccentCompress.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Description, null,
                    tint = AccentCompress,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    fileName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentCompress,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Saved to Downloads",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        // ── Warning if not reduced ──────────────
        if (!didReduce) {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = Color(0xFFFFA726).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Info, null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "This PDF is already well-optimized. The compressed version may not be smaller. Try a higher compression level.",
                        fontSize = 12.sp,
                        color = Color(0xFFE65100),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Compression Stats ───────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (didReduce) SuccessColor.copy(alpha = 0.06f)
                else Color(0xFFFFA726).copy(alpha = 0.06f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompressStatItem(
                        label = "Original",
                        value = formatSize(originalSize),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                    )
                    CompressStatItem(
                        label = "Compressed",
                        value = formatSize(compressedSize),
                        color = if (didReduce) AccentCompress else Color(0xFFFFA726)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                    )
                    CompressStatItem(
                        label = if (didReduce) "Saved" else "Diff",
                        value = if (didReduce) "$savedPercent%"
                        else "+${abs(savedPercent)}%",
                        color = if (didReduce) SuccessColor else Color(0xFFFFA726)
                    )
                }

                Spacer(Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = {
                        if (originalSize > 0)
                            (compressedSize.toFloat() / originalSize).coerceIn(0f, 1f)
                        else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (didReduce) AccentCompress else Color(0xFFFFA726),
                    trackColor = if (didReduce) AccentCompress.copy(alpha = 0.15f)
                    else Color(0xFFFFA726).copy(alpha = 0.15f)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Open & Share ────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(GradientCompress)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew, null,
                    tint = AccentCompress, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Open File",
                    color = AccentCompress,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
            }

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(GradientCompress)
                )
            ) {
                Icon(
                    Icons.Outlined.Share, null,
                    tint = AccentCompress, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Share",
                    color = AccentCompress,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.weight(0.2f))

        // ── Compress Another ────────────────────
        PrecisionGradientButton(
            text = "Compress Another PDF",
            onClick = onCompressAnother,
            gradient = GradientCompress,
            icon = Icons.Rounded.Add
        )

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Back to Home", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun CompressStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color
        )
    }
}


// ═══════════════════════════════════════════════════════════
//  SELECT PDF BUTTON
// ═══════════════════════════════════════════════════════════
@Composable
private fun CompressSelectButton(hasFile: Boolean, onClick: () -> Unit) {
    PrecisionFileSelectCard(
        title = if (hasFile) "Change PDF File" else "Select PDF File",
        subtitle = "Tap to choose a .pdf file",
        chipLabel = ".PDF",
        gradient = GradientCompressVibrant,
        icon = Icons.Rounded.FileOpen,
        onClick = onClick
    )
}


// ═══════════════════════════════════════════════════════════
//  EMPTY STATE
// ═══════════════════════════════════════════════════════════
@Composable
private fun CompressEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(AccentCompress.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Compress, null,
                    tint = AccentCompress.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "No PDF selected",
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select a PDF file to\nreduce its file size",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center, lineHeight = 20.sp
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  FILE INFO CARD
// ═══════════════════════════════════════════════════════════
@Composable
private fun CompressFileInfoCard(name: String, size: String, totalPages: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentCompress.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PictureAsPdf, null,
                    tint = AccentCompress,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompressInfoChip(Icons.Rounded.Storage, size)
                    CompressInfoChip(Icons.Rounded.Description, "$totalPages pages")
                }
            }
        }
    }
}

@Composable
private fun CompressInfoChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}


// ═══════════════════════════════════════════════════════════
//  COMPRESSION LEVEL SELECTOR
// ═══════════════════════════════════════════════════════════
@Composable
private fun CompressionLevelSelector(
    selected: CompressionLevel,
    onSelect: (CompressionLevel) -> Unit
) {
    Column {
        Text(
            "Compression Level",
            fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompressionLevel.entries.forEach { level ->
                CompressionLevelCard(
                    level = level,
                    isSelected = selected == level,
                    onClick = { onSelect(level) }
                )
            }
        }
    }
}

@Composable
private fun CompressionLevelCard(
    level: CompressionLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(GradientCompress),
                    shape = RoundedCornerShape(14.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AccentCompress.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Brush.horizontalGradient(GradientCompress)
                        else Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (level) {
                        CompressionLevel.LOW -> Icons.Rounded.CleaningServices
                        CompressionLevel.MEDIUM -> Icons.Rounded.Tune
                        CompressionLevel.HIGH -> Icons.Rounded.Compress
                    },
                    contentDescription = null,
                    tint = if (isSelected) Color.White
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    level.label,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (isSelected) AccentCompress
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    level.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Surface(
                color = if (isSelected) AccentCompress.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "~${level.reductionPercent}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) AccentCompress
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        when (level) {
                            CompressionLevel.LOW -> "lossless"
                            CompressionLevel.MEDIUM -> "quality ${level.imageQuality}%"
                            CompressionLevel.HIGH -> "quality ${level.imageQuality}%"
                        },
                        fontSize = 9.sp,
                        color = if (isSelected) AccentCompress.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  ESTIMATED SIZE CARD
// ═══════════════════════════════════════════════════════════
@Composable
private fun EstimatedSizeCard(originalSize: Long, compressionLevel: CompressionLevel) {
    val estimatedSize = (originalSize * (1 - compressionLevel.reductionPercent / 100f)).toLong()

    Surface(
        color = AccentCompress.copy(alpha = 0.06f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Estimated output size",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatSize(estimatedSize),
                    fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = AccentCompress
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Original",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatSize(originalSize),
                    fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  BOTTOM COMPRESS AREA
// ═══════════════════════════════════════════════════════════
@Composable
private fun CompressBottomArea(onCompressClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        PrecisionGradientButton(
            text = "Compress PDF",
            onClick = onCompressClick,
            gradient = GradientCompress,
            icon = Icons.Rounded.Compress
        )
    }
}


// ═══════════════════════════════════════════════════════════
//  COMPRESSING PDF DIALOG
// ═══════════════════════════════════════════════════════════
@Composable
private fun CompressingPdfDialog() {
    AlertDialog(
        onDismissRequest = { },
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Compressing PDF",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = AccentCompress,
                    strokeWidth = 4.dp
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Reducing file size…",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = { }
    )
}