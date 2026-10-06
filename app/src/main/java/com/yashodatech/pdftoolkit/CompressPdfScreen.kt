package com.yashodatech.pdftoolkit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.yashodatech.pdftoolkit.components.*
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.RecentDoc
import com.yashodatech.pdftoolkit.pdf.CompressionLevel
import com.yashodatech.pdftoolkit.pdf.PdfCompressor
import com.yashodatech.pdftoolkit.pdf.PdfSplitter
import com.yashodatech.pdftoolkit.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

@Composable
fun CompressPdfScreen(
    onBack: () -> Unit,
    initialUri: Uri? = null,
    onOpenDocument: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var sourceSize by remember { mutableStateOf(0L) }
    var sourceSizeText by remember { mutableStateOf("") }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoadingInfo by remember { mutableStateOf(false) }

    var compressionLevel by remember { mutableStateOf(CompressionLevel.MEDIUM) }
    var isCompressing by remember { mutableStateOf(false) }
    var savedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var compressedSize by remember { mutableStateOf(0L) }
    var outputFileName by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

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

    fun loadPdf(uri: Uri) {
        sourceUri = uri
        savedPdfUri = null
        compressedSize = 0L
        showSuccess = false

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) sourceName = cursor.getString(nameIdx) ?: "document.pdf"
                if (sizeIdx >= 0) {
                    sourceSize = cursor.getLong(sizeIdx)
                    sourceSizeText = formatSize(sourceSize)
                }
            }
        }
        if (sourceName.isBlank()) {
            sourceName = uri.lastPathSegment ?: "document.pdf"
        }

        isLoadingInfo = true
        scope.launch {
            totalPages = withContext(Dispatchers.IO) {
                PdfSplitter().getPageCount(context, uri)
            }
            isLoadingInfo = false
        }
    }

    LaunchedEffect(initialUri) {
        initialUri?.let { loadPdf(it) }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            loadPdf(it)
        }
    }

    fun resetForNew() {
        sourceUri = null
        sourceName = ""
        sourceSize = 0L
        sourceSizeText = ""
        totalPages = 0
        savedPdfUri = null
        compressedSize = 0L
        outputFileName = ""
        showSuccess = false
        compressionLevel = CompressionLevel.MEDIUM
    }

    fun compressPdf() {
        if (sourceUri == null) return
        isCompressing = true
        scope.launch {
            try {
                val outName = "${sourceName.removeSuffix(".pdf")}_compressed.pdf"
                val result = withContext(Dispatchers.IO) {
                    PdfCompressor().compressPdf(
                        context = context,
                        sourceUri = sourceUri!!,
                        outputFileName = outName,
                        level = compressionLevel
                    )
                }

                savedPdfUri = Uri.parse(result.first)
                compressedSize = result.second
                outputFileName = outName

                val prefs = PreferencesManager(context)
                prefs.addRecentDoc(
                    RecentDoc(
                        name = outName,
                        uri = result.first,
                        tool = "Compress",
                        timestamp = System.currentTimeMillis()
                    )
                )

                showSuccess = true
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                if (Config.SHOW_ADS) {
                    interstitialAd?.show(context as Activity)
                }
            } catch (e: Throwable) {
                val msg = if (e is OutOfMemoryError) {
                    "Document is too large for device memory at this compression level. Try Low compression."
                } else {
                    e.localizedMessage ?: "Failed to compress PDF"
                }
                Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
            } finally {
                isCompressing = false
            }
        }
    }

    AdaptiveToolScaffold(
        title = "Compress PDF",
        subtitle = "Reduce document storage size up to 80%",
        icon = Icons.Rounded.Compress,
        accentColor = ToolCompressAccent,
        onBack = onBack,
        previewPane = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ModernFilePickerCard(
                    selectedFileUri = sourceUri,
                    fileName = sourceName,
                    fileSize = sourceSizeText,
                    pageCount = totalPages,
                    accentColor = ToolCompressAccent,
                    onPick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                    onClear = { resetForNew() },
                    emptyPrompt = "Select PDF to Compress",
                    emptySubtext = "Supports PDFs of any page count"
                )

                if (sourceSize > 50 * 1024 * 1024L) {
                    ModernLargeFileBanner()
                }
            }
        },
        controlsPane = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ModernActionCard(
                    title = "Compression Level",
                    accentColor = ToolCompressAccent
                ) {
                    val estLow = if (sourceSize > 0) "Est. ~" + formatSize((sourceSize * 0.70).toLong()) else null
                    val estMed = if (sourceSize > 0) "Est. ~" + formatSize((sourceSize * 0.45).toLong()) else null
                    val estHigh = if (sourceSize > 0) "Est. ~" + formatSize((sourceSize * 0.20).toLong()) else null

                    CompressionOptionRow(
                        title = "Low Compression",
                        subtitle = "High visual fidelity • Small reduction (~20-40%)",
                        isSelected = compressionLevel == CompressionLevel.LOW,
                        accentColor = ToolCompressAccent,
                        estimatedSize = estLow,
                        onClick = { compressionLevel = CompressionLevel.LOW }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CompressionOptionRow(
                        title = "Balanced Compression",
                        subtitle = "Balanced quality & size • Medium reduction (~50-70%)",
                        isSelected = compressionLevel == CompressionLevel.MEDIUM,
                        accentColor = ToolCompressAccent,
                        isBadge = "Recommended",
                        estimatedSize = estMed,
                        onClick = { compressionLevel = CompressionLevel.MEDIUM }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CompressionOptionRow(
                        title = "Extreme Compression",
                        subtitle = "Maximum file reduction • Standard fidelity (~75-90%)",
                        isSelected = compressionLevel == CompressionLevel.HIGH,
                        accentColor = ToolCompressAccent,
                        estimatedSize = estHigh,
                        onClick = { compressionLevel = CompressionLevel.HIGH }
                    )
                }
            }
        },
        bottomBar = {
            ModernStickyBottomBar {
                ModernPrimaryButton(
                    text = "Compress PDF",
                    icon = Icons.Rounded.Compress,
                    accentColor = ToolCompressAccent,
                    enabled = sourceUri != null && !isCompressing && !isLoadingInfo,
                    isLoading = isCompressing,
                    onClick = { compressPdf() }
                )
            }
        }
    )

    ModernGlassLoader(
        isShowing = isCompressing,
        title = "Compressing PDF...",
        subtitle = "Optimizing page streams and downsampling images",
        accentColor = ToolCompressAccent
    )

    if (showSuccess && savedPdfUri != null) {
        val pctSaved = if (sourceSize > 0 && compressedSize > 0) {
            val diff = sourceSize - compressedSize
            if (diff > 0) ((diff.toDouble() / sourceSize.toDouble()) * 100).toInt() else 0
        } else 0

        ModernSuccessSheet(
            fileName = outputFileName,
            originalSize = formatSize(sourceSize),
            newSize = formatSize(compressedSize),
            savingsText = if (pctSaved > 0) "Saved $pctSaved% of storage space" else null,
            onOpen = {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(savedPdfUri, "application/pdf")
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                    )
                } catch (_: Exception) {
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
            onDone = { resetForNew() }
        )
    }
}

@Composable
private fun CompressionOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    isBadge: String? = null,
    estimatedSize: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (estimatedSize != null) {
                            ModernBadge(
                                text = estimatedSize,
                                color = accentColor.copy(alpha = 0.12f),
                                textColor = accentColor
                            )
                        }
                        if (isBadge != null) {
                            ModernBadge(
                                text = isBadge,
                                color = accentColor.copy(alpha = 0.15f),
                                textColor = accentColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}