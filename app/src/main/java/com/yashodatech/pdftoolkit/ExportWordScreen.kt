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
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.pdf.PdfWordExporter
import com.yashodatech.pdftoolkit.theme.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DOCX_MIME =
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

private fun formatWordBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

// ═══════════════════════════════════════════════════════════
//  PDF → WORD — pull the readable text out of a PDF into a .docx file.
// ═══════════════════════════════════════════════════════════
@Composable
fun ExportWordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var sourceSize by remember { mutableStateOf("") }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
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

            context.contentResolver.query(it, null, null, null, null)?.use { c ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) sourceName = c.getString(nameIdx) ?: "Unknown.pdf"
                    if (sizeIdx >= 0) sourceSize = formatWordBytes(c.getLong(sizeIdx))
                }
            }

            isLoading = true
            scope.launch {
                totalPages = withContext(Dispatchers.IO) {
                    PdfWordExporter(context).pageCount(it)
                }
                isLoading = false
            }
        }
    }

    fun export() {
        if (sourceUri == null) return
        isExporting = true
        scope.launch {
            try {
                val outUri = withContext(Dispatchers.IO) {
                    PdfWordExporter(context).exportDocx(sourceUri!!, sourceName)
                }
                resultUri = Uri.parse(outUri)
                showSuccess = true
                if (Config.SHOW_ADS) interstitialAd?.show(context as Activity)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    e.localizedMessage ?: "Conversion failed",
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
        resultUri = null
        showSuccess = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LiquidHeader(
                title = "PDF to Word",
                subtitle = "Convert to an editable .docx file",
                icon = Icons.Rounded.Description,
                gradientColors = GradientWordVibrant,
                onBackClick = onBack,
                statusBadge = "Word"
            )

            ModernGlassLoader(
                isShowing = isExporting,
                title = "Creating Word file...",
                statusText = "Extracting your text"
            )

            if (showSuccess && resultUri != null) {
                ExportWordSuccess(
                    displayName = derivedName(sourceName),
                    onShare = { shareDoc(context, resultUri!!) },
                    onView = { openDoc(context, resultUri!!) },
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

                    LiquidGlassCard(onClick = { launcher.launch(arrayOf("application/pdf")) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (sourceUri != null) Icons.Rounded.SwapHoriz else Icons.Rounded.FileOpen,
                                contentDescription = null,
                                tint = Vermilion,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (sourceUri != null) "Change PDF File" else "Select PDF File",
                                fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InkBone
                            )
                        }
                    }

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
                                        Text(sourceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkBone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("$sourceSize • $totalPages page${if (totalPages != 1) "s" else ""}", fontSize = 11.sp, color = InkMuted)
                                    }
                                    Icon(Icons.Rounded.Description, null, tint = Vermilion, modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // ── What's saved ──
                            LiquidGlassCard {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Info, null, tint = Vermilion, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Saved as .docx", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkBone)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Readable text from every page becomes paragraphs in a Word file, " +
                                            "saved to Downloads/PDF Toolkit. Image-only pages have no text to convert.",
                                        fontSize = 12.sp, lineHeight = 17.sp, color = InkMuted
                                    )
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            LiquidPrimaryButton(
                                text = "Convert to Word",
                                onClick = { export() },
                                enabled = sourceUri != null && !isLoading
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    } else {
                        Spacer(Modifier.height(120.dp))
                        WordExportEmptyState()
                    }
                }
            }
        }
    }
}

fun derivedName(baseName: String): String =
    baseName.removeSuffix(".pdf").substringBeforeLast(".").ifBlank { "document" } + "_document.docx"

// ═══════════════════════════════════════════════════════════
//  File opener + share + empty state + success
// ═══════════════════════════════════════════════════════════
private fun openDoc(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, DOCX_MIME)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No app found to open Word files", Toast.LENGTH_SHORT).show()
    }
}

private fun shareDoc(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = DOCX_MIME
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Word file"))
}

@Composable
private fun WordExportEmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(100.dp).clip(CircleShape).background(Vermilion.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Description, null, tint = Vermilion.copy(alpha = 0.5f), modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("No PDF selected", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = InkMuted)
        Spacer(Modifier.height(8.dp))
        Text("Select a PDF to convert to Word", fontSize = 13.sp, color = InkFaint, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ExportWordSuccess(
    displayName: String,
    onShare: () -> Unit,
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
                Icon(Icons.Rounded.Description, null, tint = InkBone, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Word file created!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = InkBone)
        Spacer(Modifier.height(8.dp))
        Text("Saved as $displayName", fontSize = 13.sp, color = InkMuted, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text("to Downloads/PDF Toolkit", fontSize = 12.sp, color = InkFaint)
        Spacer(Modifier.height(28.dp))

        LiquidPrimaryButton(text = "Share Word File", onClick = onShare)

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onView) {
            Text("Open File", color = Vermilion)
        }

        Spacer(Modifier.weight(0.25f))

        LiquidPrimaryButton(text = "Convert Another PDF", onClick = onExportAnother)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Back to Home", color = InkMuted)
        }
    }
}