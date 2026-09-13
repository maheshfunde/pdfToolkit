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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.components.LiquidGlassCard
import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.LiquidPrimaryButton
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.pdf.PdfPageManager
import com.yashodatech.pdftoolkit.pdf.PdfWatermarker
import com.yashodatech.pdftoolkit.pdf.WatermarkConfig
import com.yashodatech.pdftoolkit.theme.*
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.RecentDoc
import com.google.android.gms.ads.*
import kotlin.math.roundToInt
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun formatWmBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

// ═══════════════════════════════════════════════════════════
//  WATERMARK — stamp text (DRAFT / CONFIDENTIAL / custom) onto
//  every page of a PDF, then save as a new file.
// ═══════════════════════════════════════════════════════════
@Composable
fun WatermarkPdfScreen(onBack: () -> Unit, onOpenDocument: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var sourceSize by remember { mutableStateOf("") }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var resultUri by remember { mutableStateOf<Uri?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Watermark options
    var watermarkText by remember { mutableStateOf("DRAFT") }
    var watermarkColor by remember { mutableStateOf(Vermilion) }
    var opacity by remember { mutableStateOf(0.25f) }
    var fontSizePt by remember { mutableStateOf(60f) }
    var diagonal by remember { mutableStateOf(true) }

    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    val previewManager = remember { PdfPageManager(context) }

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
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            sourceUri = it
            resultUri = null
            showSuccess = false
            previewBitmap = null

            context.contentResolver.query(it, null, null, null, null)?.use { c ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) sourceName = c.getString(nameIdx) ?: "Unknown.pdf"
                    if (sizeIdx >= 0) sourceSize = formatWmBytes(c.getLong(sizeIdx))
                }
            }

            isLoading = true
            scope.launch {
                withContext(Dispatchers.IO) {
                    totalPages = previewManager.load(it)
                    previewBitmap = previewManager.renderPage(0, 340)
                    previewManager.close()
                }
                isLoading = false
            }
        }
    }

    fun applyWatermark() {
        if (sourceUri == null) return
        isApplying = true
        scope.launch {
            try {
                val config = WatermarkConfig(
                    text = watermarkText,
                    fontSize = fontSizePt,
                    colorArgb = watermarkColor.toArgb(),
                    opacity = opacity,
                    rotation = if (diagonal) -45f else 0f
                )
                val uriStr = withContext(Dispatchers.IO) {
                    PdfWatermarker().addTextWatermark(
                        context, sourceUri!!, config,
                        "${sourceName.removeSuffix(".pdf")}_watermarked"
                    )
                }
                resultUri = Uri.parse(uriStr)
                PreferencesManager(context).addRecentDoc(
                    RecentDoc(
                        name = "${sourceName.removeSuffix(".pdf")}_watermarked.pdf",
                        uri = uriStr,
                        tool = "Watermark",
                        timestamp = System.currentTimeMillis()
                    )
                )
                showSuccess = true
                if (Config.SHOW_ADS) interstitialAd?.show(context as Activity)
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage ?: "Failed to add watermark"}", Toast.LENGTH_LONG).show()
            } finally {
                isApplying = false
            }
        }
    }

    fun resetForNew() {
        sourceUri = null
        sourceName = ""
        sourceSize = ""
        totalPages = 0
        previewBitmap = null
        resultUri = null
        showSuccess = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LiquidHeader(
                title = "Watermark",
                subtitle = "Stamp text across every page",
                icon = Icons.AutoMirrored.Rounded.BrandingWatermark,
                gradientColors = GradientWatermarkVibrant,
                onBackClick = onBack,
                statusBadge = "Protect"
            )

            ModernGlassLoader(
                isShowing = isApplying,
                title = "Applying watermark...",
                statusText = "Burning text onto each page"
            )

            if (showSuccess && resultUri != null) {
                WatermarkSuccess(
                    resultUri = resultUri!!,
                    displayName = "${sourceName.removeSuffix(".pdf")}_watermarked.pdf",
                    onApplyAnother = { resetForNew() },
                    onBack = onBack,
                    onOpenDocument = { onOpenDocument(resultUri!!.toString()) }
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
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Vermilion, modifier = Modifier.size(44.dp))
                            }
                        } else {
                            // ── File + page info ──
                            LiquidGlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(sourceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkBone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("$sourceSize • $totalPages page${if (totalPages != 1) "s" else ""}", fontSize = 11.sp, color = InkMuted)
                                    }
                                    Icon(Icons.Rounded.Description, null, tint = Vermilion, modifier = Modifier.size(24.dp))
                                }
                            }

                            // ── Watermark text ──
                            Spacer(Modifier.height(16.dp))
                            Text("Watermark text", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkFaint)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = watermarkText,
                                onValueChange = { watermarkText = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Vermilion,
                                    unfocusedBorderColor = InkBorder,
                                    focusedTextColor = InkBone,
                                    unfocusedTextColor = InkBone,
                                    cursorColor = Vermilion
                                )
                            )

                            // ── Color ──
                            Spacer(Modifier.height(16.dp))
                            Text("Color", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkFaint)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val palette = listOf(
                                    "Red" to Vermilion,
                                    "Slate" to InkMuted,
                                    "Deep" to InkFaint,
                                    "White" to Color.White
                                )
                                palette.forEach { (label, color) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .then(
                                                    if (watermarkColor == color)
                                                        Modifier.border(2.dp, InkBone, CircleShape)
                                                    else Modifier
                                                )
                                                .clickable { watermarkColor = color },
                                            contentAlignment = Alignment.Center
                                        ) {}
                                        Spacer(Modifier.height(4.dp))
                                        Text(label, fontSize = 10.sp, color = InkMuted)
                                    }
                                }
                            }

                            // ── Position ──
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Position", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkFaint, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PositionChip(label = "Diagonal", selected = diagonal, onClick = { diagonal = true })
                                    PositionChip(label = "Center", selected = !diagonal, onClick = { diagonal = false })
                                }
                            }

                            // ── Size ──
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Size", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkFaint, modifier = Modifier.weight(1f))
                                Text("${fontSizePt.roundToInt()}pt", fontSize = 12.sp, color = Vermilion, fontWeight = FontWeight.SemiBold)
                            }
                            Slider(
                                value = fontSizePt,
                                onValueChange = { fontSizePt = it },
                                valueRange = 24f..120f,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Vermilion,
                                    inactiveTrackColor = InkBorder,
                                    thumbColor = Vermilion
                                )
                            )

                            // ── Opacity ──
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Opacity", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkFaint, modifier = Modifier.weight(1f))
                                Text("${(opacity * 100).roundToInt()}%", fontSize = 12.sp, color = Vermilion, fontWeight = FontWeight.SemiBold)
                            }
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                valueRange = 0.05f..0.9f,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Vermilion,
                                    inactiveTrackColor = InkBorder,
                                    thumbColor = Vermilion
                                )
                            )

                            // ── Preview ──
                            Spacer(Modifier.height(8.dp))
                            Text("Preview", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkFaint)
                            Spacer(Modifier.height(8.dp))
                            PreviewCard(bitmap = previewBitmap, text = watermarkText, color = watermarkColor, opacity = opacity, diagonal = diagonal, fontSizePt = fontSizePt)

                            Spacer(Modifier.height(20.dp))
                            LiquidPrimaryButton(text = "Apply Watermark", onClick = { applyWatermark() }, enabled = sourceUri != null && !isLoading)
                            Spacer(Modifier.height(24.dp))
                        }
                    } else {
                        Spacer(Modifier.height(120.dp))
                        WatermarkEmptyState()
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Position selector chip
// ═══════════════════════════════════════════════════════════
@Composable
private fun PositionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Vermilion.copy(alpha = 0.18f) else InkSurface)
            .border(1.dp, if (selected) Vermilion else InkBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Vermilion else InkMuted
        )
    }
}

// ═══════════════════════════════════════════════════════════
//  Live preview (base page + Compose text overlay)
// ═══════════════════════════════════════════════════════════
@Composable
private fun PreviewCard(
    bitmap: android.graphics.Bitmap?,
    text: String,
    color: Color,
    opacity: Float,
    diagonal: Boolean,
    fontSizePt: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InkSurface)
            .border(1.dp, InkBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No preview", fontSize = 13.sp, color = InkFaint)
            }
        }

        Text(
            text = text.ifBlank { "DRAFT" },
            color = color.copy(alpha = opacity),
            // Map PDF points (24-120) → preview sp (≈20-60)
            fontSize = (fontSizePt * 0.5f).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.rotate(if (diagonal) -22f else 0f)
        )
    }
}

// ═══════════════════════════════════════════════════════════
//  Empty state + success
// ═══════════════════════════════════════════════════════════
@Composable
private fun WatermarkEmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(100.dp).clip(CircleShape).background(Vermilion.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.BrandingWatermark, null, tint = Vermilion.copy(alpha = 0.5f), modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("No PDF selected", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = InkMuted)
        Spacer(Modifier.height(8.dp))
        Text("Select a PDF to add a watermark", fontSize = 13.sp, color = InkFaint)
    }
}

@Composable
private fun WatermarkSuccess(resultUri: Uri, displayName: String, onApplyAnother: () -> Unit, onBack: () -> Unit, onOpenDocument: () -> Unit) {
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
        Text("Watermark applied!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = InkBone)
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
                    putExtra(Intent.EXTRA_STREAM, resultUri)
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

        LiquidPrimaryButton(text = "Watermark Another PDF", onClick = onApplyAnother)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Back to Home", color = InkMuted)
        }
    }
}