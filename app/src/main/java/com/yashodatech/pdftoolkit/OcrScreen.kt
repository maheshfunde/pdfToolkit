package com.yashodatech.pdftoolkit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.components.LiquidGlassCard
import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.LiquidPrimaryButton
import com.yashodatech.pdftoolkit.pdf.PdfOcrExporter
import com.yashodatech.pdftoolkit.theme.*
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.RecentDoc
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.components.PrecisionFileSelectCard

private fun formatOcrBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

// ═══════════════════════════════════════════════════════════
//  OCR — make scanned / image-only PDFs searchable & selectable
// ═══════════════════════════════════════════════════════════
@Composable
fun OcrScreen(onBack: () -> Unit, onOpenDocument: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var sourceSize by remember { mutableStateOf("") }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isOcrIng by remember { mutableStateOf(false) }
    var ocrPage by remember { mutableIntStateOf(0) }
    var resultUri by remember { mutableStateOf<Uri?>(null) }
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

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            sourceUri = it
            resultUri = null
            showSuccess = false
            ocrPage = 0

            context.contentResolver.query(it, null, null, null, null)?.use { c ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) sourceName = c.getString(nameIdx) ?: "Unknown.pdf"
                    if (sizeIdx >= 0) sourceSize = formatOcrBytes(c.getLong(sizeIdx))
                }
            }

            isLoading = true
            scope.launch {
                totalPages = withContext(Dispatchers.IO) {
                    PdfOcrExporter(context).pageCount(it)
                }
                isLoading = false
            }
        }
    }

    fun runOcr() {
        if (sourceUri == null) return
        isOcrIng = true
        ocrPage = 0
        scope.launch {
            try {
                val outUri = withContext(Dispatchers.IO) {
                    PdfOcrExporter(context).ocrPdf(
                        sourceUri = sourceUri!!,
                        baseName = sourceName,
                        onProgress = { done, _ -> ocrPage = done }
                    )
                }
                resultUri = Uri.parse(outUri)
                val prefs = PreferencesManager(context)
                prefs.addRecentDoc(
                    RecentDoc(
                        name = sourceName.removeSuffix(".pdf") + "_ocr.pdf",
                        uri = outUri,
                        tool = "OCR",
                        timestamp = System.currentTimeMillis()
                    )
                )
                showSuccess = true
                if (Config.SHOW_ADS) interstitialAd?.show(context as Activity)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "OCR failed: ${e.localizedMessage ?: "unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isOcrIng = false
            }
        }
    }

    fun resetForNew() {
        sourceUri = null
        sourceName = ""
        sourceSize = ""
        totalPages = 0
        ocrPage = 0
        resultUri = null
        showSuccess = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LiquidHeader(
                title = "OCR PDF",
                subtitle = "Make scanned PDFs searchable",
                icon = Icons.Rounded.DocumentScanner,
                gradientColors = GradientOcrVibrant,
                onBackClick = onBack,
                statusBadge = "Search"
            )

            if (isOcrIng) {
                ModernGlassLoader(
                    isShowing = true,
                    title = "Recognizing text...",
                    statusText = "Page $ocrPage of $totalPages"
                )
            }

            if (showSuccess && resultUri != null) {
                OcrSuccess(
                    displayName = sourceName.removeSuffix(".pdf").substringBeforeLast(".")
                        .ifBlank { "document" } + "_ocr.pdf",
                    onView = { onOpenDocument(resultUri.toString()) },
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
                        title = if (sourceUri != null) "Change PDF File" else "Select Scanned PDF",
                        subtitle = "Tap to choose a .pdf file",
                        chipLabel = ".PDF",
                        gradient = GradientOcrVibrant,
                        icon = Icons.Rounded.FileOpen,
                        onClick = { launcher.launch(arrayOf("application/pdf")) }
                    )

                    if (sourceUri != null) {
                        Spacer(Modifier.height(16.dp))

                        if (isLoading) {
                            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Vermilion, modifier = Modifier.size(44.dp))
                            }
                        } else {
                            // ── File info ──
                            LiquidGlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(sourceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkBone(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("$sourceSize • $totalPages page${if (totalPages != 1) "s" else ""}", fontSize = 11.sp, color = InkMuted())
                                    }
                                    Icon(Icons.Rounded.DocumentScanner, null, tint = Vermilion, modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // ── What it does ──
                            LiquidGlassCard {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Info, null, tint = Vermilion, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("How it works", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkBone())
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Each page is scanned for text and a searchable text layer is added, " +
                                            "so you can select, copy and find words. The image itself is unchanged. " +
                                            "First run downloads the OCR model over the network.",
                                        fontSize = 12.sp, lineHeight = 17.sp, color = InkMuted()
                                    )
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // ── Progress ──
                            when {
                                isOcrIng -> {
                                    Text("Recognizing page $ocrPage of $totalPages…", fontSize = 12.sp, color = InkMuted())
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { if (totalPages > 0) ocrPage.toFloat() / totalPages else 0f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Vermilion,
                                        trackColor = InkBorder
                                    )
                                }
                                else -> {
                                    LiquidPrimaryButton(
                                        text = "Make Searchable",
                                        onClick = { runOcr() },
                                        enabled = sourceUri != null && !isLoading
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    } else {
                        Spacer(Modifier.height(120.dp))
                        OcrEmptyState()
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Empty state + success
// ═══════════════════════════════════════════════════════════
@Composable
private fun OcrEmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(100.dp).clip(CircleShape).background(Vermilion.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DocumentScanner, null, tint = Vermilion.copy(alpha = 0.5f), modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("No PDF selected", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = InkMuted())
        Spacer(Modifier.height(8.dp))
        Text("Pick a scanned PDF to make it searchable", fontSize = 13.sp, color = InkFaint(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun OcrSuccess(
    displayName: String,
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
                Icon(Icons.Rounded.DocumentScanner, null, tint = InkBone(), modifier = Modifier.size(38.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Now searchable!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = InkBone())
        Spacer(Modifier.height(8.dp))
        Text("Saved as $displayName", fontSize = 13.sp, color = InkMuted(), textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text("to Downloads — open it and try selecting any word", fontSize = 12.sp, color = InkFaint())
        Spacer(Modifier.height(28.dp))

        LiquidPrimaryButton(text = "Open Document", onClick = onView)

        Spacer(Modifier.weight(0.25f))

        LiquidPrimaryButton(text = "OCR Another PDF", onClick = onExportAnother)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Back to Home", color = InkMuted())
        }
    }
}