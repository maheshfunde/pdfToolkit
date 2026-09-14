package com.yashodatech.pdftoolkit

import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.components.PrecisionFileSelectCard
import com.yashodatech.pdftoolkit.components.PrecisionGradientButton
import com.yashodatech.pdftoolkit.theme.GradientMergeVibrant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.pdf.PdfMerger
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// ── Gradient constants ──────────────────────────────────
private val GradientMerge = listOf(Color(0xFFB23F1F), Color(0xFFE4572E))
private val GradientSuccess = listOf(Color(0xFFE4572E), Color(0xFFB23F1F))
private val AccentMerge = Color(0xFFE4572E)
private val SuccessColor = Color(0xFFE4572E)


// ── Data class for selected PDF ─────────────────────────
data class PdfFileInfo(
    val uri: Uri,
    val name: String,
    val size: String
)


// ── Auto generate name ──────────────────────────────────
private fun generateMergeName(): String {
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    return "Merged_${dateFormat.format(Date())}"
}


// ── Helper: Get file info ───────────────────────────────
private fun getFileInfo(context: Context, uri: Uri): PdfFileInfo {
    var name = "Unknown.pdf"
    var size = "Unknown size"

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

        if (cursor.moveToFirst()) {
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: "Unknown.pdf"
            if (sizeIndex >= 0) size = formatMergeSize(cursor.getLong(sizeIndex))
        }
    }

    return PdfFileInfo(uri = uri, name = name, size = size)
}

private fun formatMergeSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}


// ═══════════════════════════════════════════════════════════
//  MAIN SCREEN
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val pdfFiles = remember { mutableStateListOf<PdfFileInfo>() }
    var showNameDialog by remember { mutableStateOf(false) }
    var pdfName by remember { mutableStateOf("") }
    var isMerging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var savedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var savedPdfName by remember { mutableStateOf("") }
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
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }
                }
            )
        }
    }

    // ── PDF picker launcher ─────────────────────────
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newFiles = uris.map { uri ->
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                getFileInfo(context, uri)
            }
            pdfFiles.addAll(newFiles)
            savedPdfUri = null
            showSuccessScreen = false
        }
    }

    // ── Helper: reorder ─────────────────────────────
    fun moveUp(index: Int) {
        if (index > 0) {
            val temp = pdfFiles[index]
            pdfFiles[index] = pdfFiles[index - 1]
            pdfFiles[index - 1] = temp
        }
    }

    fun moveDown(index: Int) {
        if (index < pdfFiles.size - 1) {
            val temp = pdfFiles[index]
            pdfFiles[index] = pdfFiles[index + 1]
            pdfFiles[index + 1] = temp
        }
    }

    // ── Helper: Merge PDFs ──────────────────────────
    fun mergePdfs(fileName: String) {
        val finalName = fileName.ifBlank { generateMergeName() }
        val fullFileName = "$finalName.pdf"

        isMerging = true
        showNameDialog = false

        scope.launch {
            try {
                val uriString = withContext(Dispatchers.IO) {
                    PdfMerger().mergePdfs(
                        context = context,
                        pdfUris = pdfFiles.map { it.uri },
                        outputFileName = fullFileName
                    )
                }

                savedPdfUri = Uri.parse(uriString)
                savedPdfName = fullFileName
                showSuccessScreen = true

                if (Config.SHOW_ADS) {
                    interstitialAd?.show(context as Activity)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.localizedMessage ?: "Failed to merge PDFs"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isMerging = false
            }
        }
    }

    // ── Helper: Reset ───────────────────────────────
    fun resetForNewMerge() {
        pdfFiles.clear()
        savedPdfUri = null
        savedPdfName = ""
        pdfName = ""
        showSuccessScreen = false
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
                title = "Merge PDF",
                subtitle = "Combine multiple PDF documents into 1 file",
                icon = Icons.AutoMirrored.Rounded.CallMerge,
                gradientColors = GradientMergeVibrant,
                onBackClick = onBack,
                statusBadge = "Multi-file"
            )

            ModernGlassLoader(
                isShowing = isMerging,
                title = "Merging PDFs...",
                statusText = "Combining document pages & structural trees"
            )

            if (showSuccessScreen && savedPdfUri != null) {

                // ═════════════════════════════════════
                //  SUCCESS SCREEN
                // ═════════════════════════════════════
                MergeSuccessScreen(
                    pdfName = savedPdfName,
                    pdfUri = savedPdfUri!!,
                    fileCount = pdfFiles.size,
                    onOpenPdf = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(savedPdfUri, "application/pdf")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                        )
                    },
                    onSharePdf = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, savedPdfUri)
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                },
                                "Share PDF"
                            )
                        )
                    },
                    onMergeAnother = { resetForNewMerge() },
                    onBack = onBack
                )

            } else {

                // ═════════════════════════════════════
                //  EDITOR SCREEN
                // ═════════════════════════════════════
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {

                    Spacer(Modifier.height(16.dp))

                    // ── Add PDFs Button ─────────────
                    MergeAddButton(
                        onClick = {
                            pdfLauncher.launch(arrayOf("application/pdf"))
                        }
                    )

                    Spacer(Modifier.height(20.dp))

                    if (pdfFiles.isEmpty()) {
                        MergeEmptyState()
                    } else {

                        // ── Count Header ────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.PictureAsPdf,
                                    contentDescription = null,
                                    tint = AccentMerge,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${pdfFiles.size} PDF${if (pdfFiles.size != 1) "s" else ""} selected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(onClick = {
                                pdfFiles.clear()
                                savedPdfUri = null
                            }) {
                                Icon(
                                    Icons.Rounded.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Clear All")
                            }
                        }

                        // ── Tip Chip ────────────────
                        Surface(
                            color = AccentMerge.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = AccentMerge,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "PDFs will be merged in the order shown. Use arrows to reorder.",
                                    fontSize = 12.sp,
                                    color = AccentMerge,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // ── PDF File List ───────────
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            itemsIndexed(
                                pdfFiles,
                                key = { i, file -> "${file.uri}_$i" }
                            ) { index, file ->
                                PdfFileCard(
                                    file = file,
                                    index = index,
                                    totalCount = pdfFiles.size,
                                    onMoveUp = { moveUp(index) },
                                    onMoveDown = { moveDown(index) },
                                    onRemove = { pdfFiles.removeAt(index) }
                                )
                            }
                        }
                    }
                }

                // ── Bottom Actions ──────────────────
                if (pdfFiles.size >= 2) {
                    MergeBottomArea(
                        onQuickMerge = { mergePdfs("") },
                        onNamedMerge = { showNameDialog = true }
                    )
                } else if (pdfFiles.size == 1) {
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = AccentMerge.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Select at least 2 PDFs to merge",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Name Dialog ─────────────────────────────
        if (showNameDialog) {
            MergeNameDialog(
                pdfName = pdfName,
                onNameChange = { pdfName = it },
                autoName = generateMergeName(),
                fileCount = pdfFiles.size,
                onDismiss = { showNameDialog = false },
                onConfirm = { mergePdfs(pdfName) }
            )
        }

        // ── Loading Dialog ──────────────────────────
        if (isMerging) {
            MergingPdfDialog()
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  SUCCESS SCREEN
// ═══════════════════════════════════════════════════════════
@Composable
private fun MergeSuccessScreen(
    pdfName: String,
    pdfUri: Uri,
    fileCount: Int,
    onOpenPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onMergeAnother: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(Modifier.weight(0.3f))

        // ── Success Icon ────────────────────────────
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(SuccessColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(GradientSuccess)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "PDFs Merged Successfully!",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        // ── File Name Badge ─────────────────────────
        Surface(
            color = AccentMerge.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = AccentMerge,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = pdfName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentMerge
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "$fileCount PDFs merged • Saved to Downloads",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // ── Open & Share ────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onOpenPdf,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(GradientMerge)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew, null,
                    tint = AccentMerge,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Open PDF", color = AccentMerge, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = onSharePdf,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(GradientMerge)
                )
            ) {
                Icon(
                    Icons.Outlined.Share, null,
                    tint = AccentMerge,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Share PDF", color = AccentMerge, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.weight(0.3f))

        // ── Merge Another ───────────────────────────
        PrecisionGradientButton(
            text = "Merge Another PDF",
            onClick = onMergeAnother,
            gradient = GradientMerge,
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


// ═══════════════════════════════════════════════════════════
//  ADD PDF BUTTON
// ═══════════════════════════════════════════════════════════
@Composable
private fun MergeAddButton(onClick: () -> Unit) {
    PrecisionFileSelectCard(
        title = "Select PDF Files",
        subtitle = "Tap to choose one or more .pdf files",
        chipLabel = ".PDF",
        gradient = GradientMergeVibrant,
        icon = Icons.AutoMirrored.Rounded.NoteAdd,
        onClick = onClick
    )
}


// ═══════════════════════════════════════════════════════════
//  EMPTY STATE
// ═══════════════════════════════════════════════════════════
@Composable
private fun MergeEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(AccentMerge.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.MergeType, null,
                    tint = AccentMerge.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "No PDFs selected",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select two or more PDF files\nto merge them into one",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  PDF FILE CARD
// ═══════════════════════════════════════════════════════════
@Composable
private fun PdfFileCard(
    file: PdfFileInfo,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(GradientMerge)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    file.size,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp, "Move Up",
                        tint = if (index > 0) AccentMerge
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown, "Move Down",
                        tint = if (index < totalCount - 1) AccentMerge
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Close, "Remove",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  BOTTOM ACTION AREA (Quick Merge + Named Merge)
// ═══════════════════════════════════════════════════════════
@Composable
private fun MergeBottomArea(
    onQuickMerge: () -> Unit,
    onNamedMerge: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ── Quick Merge ─────────────────────────
            PrecisionGradientButton(
                text = "Quick Merge",
                onClick = onQuickMerge,
                height = 52.dp,
                cornerRadius = 14.dp,
                gradient = GradientMerge,
                icon = Icons.Rounded.Bolt
            )

            Spacer(Modifier.height(10.dp))

            // ── Named Merge ─────────────────────────
            OutlinedButton(
                onClick = onNamedMerge,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(GradientMerge)
                )
            ) {
                Icon(
                    Icons.Rounded.Edit, null,
                    tint = AccentMerge,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Merge with Custom Name",
                    color = AccentMerge,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  MERGE NAME DIALOG (Updated)
// ═══════════════════════════════════════════════════════════
@Composable
private fun MergeNameDialog(
    pdfName: String,
    onNameChange: (String) -> Unit,
    autoName: String,
    fileCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentMerge.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.MergeType, null,
                    tint = AccentMerge,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Name merged PDF",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = pdfName,
                    onValueChange = onNameChange,
                    label = { Text("File name (optional)") },
                    placeholder = { Text(autoName) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Outlined.Description, null, tint = AccentMerge)
                    },
                    suffix = {
                        Text(
                            ".pdf",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (pdfName.isBlank()) "Will be saved as: $autoName.pdf"
                    else "Will be saved as: $pdfName.pdf",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontStyle = FontStyle.Italic
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    color = AccentMerge.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Info, null,
                            tint = AccentMerge,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$fileCount PDFs will be merged in the selected order",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentMerge)
            ) {
                Icon(Icons.AutoMirrored.Rounded.MergeType, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Merge", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


// ═══════════════════════════════════════════════════════════
//  MERGING DIALOG
// ═══════════════════════════════════════════════════════════
@Composable
private fun MergingPdfDialog() {
    AlertDialog(
        onDismissRequest = { },
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Merging PDFs",
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
                    color = AccentMerge,
                    strokeWidth = 4.dp
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Combining your PDF files…",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = { }
    )
}