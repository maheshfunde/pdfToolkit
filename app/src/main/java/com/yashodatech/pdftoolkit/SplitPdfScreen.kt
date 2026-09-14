package com.yashodatech.pdftoolkit

import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.components.PrecisionFileSelectCard
import com.yashodatech.pdftoolkit.components.PrecisionGradientButton
import com.yashodatech.pdftoolkit.theme.GradientSplitVibrant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.yashodatech.pdftoolkit.pdf.PdfSplitter
import com.yashodatech.pdftoolkit.pdf.SplitRange
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// ── Gradient constants ──────────────────────────────────
private val GradientSplit = listOf(Color(0xFFB23F1F), Color(0xFFE4572E))
private val GradientSuccess = listOf(Color(0xFFE4572E), Color(0xFFB23F1F))
private val AccentSplit = Color(0xFFE4572E)
private val SuccessColor = Color(0xFFE4572E)


// ── Data classes ────────────────────────────────────────
data class SplitRangeState(val from: Int, val to: Int)

enum class SplitMode(val label: String, val description: String) {
    EVERY_PAGE("Every Page", "Split into individual pages"),
    CUSTOM_RANGES("Custom Ranges", "Define page ranges")
}


// ── Helper ──────────────────────────────────────────────
private fun formatSplitSize(bytes: Long): String {
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
fun SplitPdfScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Source PDF state
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var sourceSize by remember { mutableStateOf("") }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoadingInfo by remember { mutableStateOf(false) }

    // Split ranges
    val splitRanges = remember { mutableStateListOf<SplitRangeState>() }
    var showAddRangeDialog by remember { mutableStateOf(false) }

    // Split mode
    var splitMode by remember { mutableStateOf(SplitMode.CUSTOM_RANGES) }

    // Processing state
    var isSplitting by remember { mutableStateOf(false) }
    var savedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
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
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            sourceUri = it
            savedUris = emptyList()
            splitRanges.clear()
            showSuccessScreen = false

            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) sourceName = cursor.getString(nameIdx) ?: "Unknown.pdf"
                    if (sizeIdx >= 0) sourceSize = formatSplitSize(cursor.getLong(sizeIdx))
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

    // ── Helper: Split PDF ───────────────────────────
    fun splitPdf() {
        isSplitting = true

        scope.launch {
            try {
                val ranges = when (splitMode) {
                    SplitMode.EVERY_PAGE -> {
                        (1..totalPages).map { SplitRange(it, it) }
                    }
                    SplitMode.CUSTOM_RANGES -> {
                        splitRanges.map { SplitRange(it.from, it.to) }
                    }
                }

                val uris = withContext(Dispatchers.IO) {
                    PdfSplitter().splitPdf(
                        context = context,
                        sourceUri = sourceUri!!,
                        ranges = ranges,
                        baseFileName = sourceName.removeSuffix(".pdf")
                    )
                }

                savedUris = uris
                showSuccessScreen = true

                if (Config.SHOW_ADS) {
                    interstitialAd?.show(context as Activity)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.localizedMessage ?: "Failed to split PDF"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isSplitting = false
            }
        }
    }

    // ── Helper: Reset ───────────────────────────────
    fun resetForNewSplit() {
        sourceUri = null
        sourceName = ""
        sourceSize = ""
        totalPages = 0
        splitRanges.clear()
        savedUris = emptyList()
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
                title = "Split PDF",
                subtitle = "Extract pages or separate documents",
                icon = Icons.AutoMirrored.Rounded.CallSplit,
                gradientColors = GradientSplitVibrant,
                onBackClick = onBack,
                statusBadge = "Extract Pages"
            )

            ModernGlassLoader(
                isShowing = isSplitting,
                title = "Splitting PDF...",
                statusText = "Extracting selected page ranges & generating output files"
            )

            if (showSuccessScreen && savedUris.isNotEmpty()) {

                // ═════════════════════════════════════
                //  SUCCESS SCREEN
                // ═════════════════════════════════════
                SplitSuccessScreen(
                    fileCount = savedUris.size,
                    sourceName = sourceName,
                    onShareAll = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "application/pdf"
                                    putParcelableArrayListExtra(
                                        Intent.EXTRA_STREAM,
                                        ArrayList(savedUris)
                                    )
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                },
                                "Share Split PDFs"
                            )
                        )
                    },
                    onSplitAnother = { resetForNewSplit() },
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

                    SplitSelectButton(
                        hasFile = sourceUri != null,
                        onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }
                    )

                    Spacer(Modifier.height(20.dp))

                    if (sourceUri == null) {
                        SplitEmptyState()
                    } else if (isLoadingInfo) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentSplit, modifier = Modifier.size(48.dp))
                        }
                    } else {

                        // ── File Info ───────────────
                        SplitFileInfoCard(name = sourceName, size = sourceSize, totalPages = totalPages)

                        Spacer(Modifier.height(16.dp))

                        // ── Split Mode ──────────────
                        SplitModeSelector(
                            selected = splitMode,
                            onSelect = {
                                splitMode = it
                                splitRanges.clear()
                                savedUris = emptyList()
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        when (splitMode) {
                            SplitMode.EVERY_PAGE -> {
                                Surface(
                                    color = AccentSplit.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.Info, null,
                                            tint = AccentSplit,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "This will create $totalPages separate PDF files, one for each page.",
                                            fontSize = 13.sp,
                                            color = AccentSplit,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            SplitMode.CUSTOM_RANGES -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Page Ranges", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    TextButton(onClick = { showAddRangeDialog = true }) {
                                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Range")
                                    }
                                }

                                if (splitRanges.isEmpty()) {
                                    Surface(
                                        color = AccentSplit.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Outlined.ContentCut, null,
                                                tint = AccentSplit.copy(alpha = 0.4f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "No ranges added yet",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Tap \"Add Range\" to define page ranges",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        itemsIndexed(splitRanges) { index, range ->
                                            SplitRangeCard(
                                                range = range,
                                                index = index,
                                                onRemove = { splitRanges.removeAt(index) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Bottom Actions ──────────────────
                if (sourceUri != null && totalPages > 0 && !isLoadingInfo) {
                    val canSplit = when (splitMode) {
                        SplitMode.EVERY_PAGE -> true
                        SplitMode.CUSTOM_RANGES -> splitRanges.isNotEmpty()
                    }

                    if (canSplit) {
                        SplitBottomArea(onSplitClick = { splitPdf() })
                    }
                }
            }
        }

        // ── Add Range Dialog ────────────────────────
        if (showAddRangeDialog) {
            AddRangeDialog(
                totalPages = totalPages,
                onDismiss = { showAddRangeDialog = false },
                onAdd = { from, to ->
                    splitRanges.add(SplitRangeState(from, to))
                    showAddRangeDialog = false
                }
            )
        }

        // ── Splitting Dialog ────────────────────────
        if (isSplitting) {
            SplittingPdfDialog()
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  SUCCESS SCREEN
// ═══════════════════════════════════════════════════════════
@Composable
private fun SplitSuccessScreen(
    fileCount: Int,
    sourceName: String,
    onShareAll: () -> Unit,
    onSplitAnother: () -> Unit,
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
                    Icons.Rounded.CheckCircle, null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "PDF Split Successfully!",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        // ── Source File Badge ────────────────────────
        Surface(
            color = AccentSplit.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.ContentCut, null,
                    tint = AccentSplit,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    sourceName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentSplit,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "$fileCount PDF${if (fileCount != 1) "s" else ""} created • Saved to Downloads",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // ── Share All Button ────────────────────────
        OutlinedButton(
            onClick = onShareAll,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = Brush.horizontalGradient(GradientSplit)
            )
        ) {
            Icon(
                Icons.Outlined.Share, null,
                tint = AccentSplit,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Share All Split PDFs",
                color = AccentSplit,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.weight(0.3f))

        // ── Split Another ───────────────────────────
        PrecisionGradientButton(
            text = "Split Another PDF",
            onClick = onSplitAnother,
            gradient = GradientSplit,
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
//  SELECT PDF BUTTON
// ═══════════════════════════════════════════════════════════
@Composable
private fun SplitSelectButton(hasFile: Boolean, onClick: () -> Unit) {
    PrecisionFileSelectCard(
        title = if (hasFile) "Change PDF File" else "Select PDF File",
        subtitle = "Tap to choose a .pdf file",
        chipLabel = ".PDF",
        gradient = GradientSplitVibrant,
        icon = Icons.Rounded.FileOpen,
        onClick = onClick
    )
}


// ═══════════════════════════════════════════════════════════
//  EMPTY STATE
// ═══════════════════════════════════════════════════════════
// ═══════════════════════════════════════════════════════════
//  EMPTY STATE (continued)
// ═══════════════════════════════════════════════════════════
@Composable
private fun SplitEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(AccentSplit.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ContentCut, null,
                    tint = AccentSplit.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "No PDF selected",
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select a PDF file to get started",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  FILE INFO CARD
// ═══════════════════════════════════════════════════════════
@Composable
private fun SplitFileInfoCard(name: String, size: String, totalPages: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "File Name",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Rounded.Description, null,
                    tint = AccentSplit,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "File Size",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        size,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Total Pages",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        totalPages.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  SPLIT MODE SELECTOR
// ═══════════════════════════════════════════════════════════
@Composable
private fun SplitModeSelector(selected: SplitMode, onSelect: (SplitMode: SplitMode) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Split Mode",
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SplitMode.entries.forEach { mode ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(mode) },
                    color = if (selected == mode)
                        AccentSplit.copy(alpha = 0.15f)
                    else
                        Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            mode.label,
                            fontWeight = if (selected == mode) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (selected == mode) AccentSplit else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            mode.description,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  SPLIT RANGE CARD
// ═══════════════════════════════════════════════════════════
@Composable
private fun SplitRangeCard(range: SplitRangeState, index: Int, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = AccentSplit.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${index + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AccentSplit
                        )
                    }
                }

                Column {
                    Text(
                        "Range ${index + 1}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Page ${range.from}${if (range.from != range.to) " - ${range.to}" else ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.Delete, null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
//  BOTTOM ACTION AREA
// ═══════════════════════════════════════════════════════════
@Composable
private fun SplitBottomArea(onSplitClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        PrecisionGradientButton(
            text = "Split PDF",
            onClick = onSplitClick,
            gradient = GradientSplit,
            icon = Icons.Rounded.ContentCut
        )
    }
}


// ═══════════════════════════════════════════════════════════
//  ADD RANGE DIALOG
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRangeDialog(totalPages: Int, onDismiss: () -> Unit, onAdd: (Int, Int) -> Unit) {
    var fromText by remember { mutableStateOf("") }
    var toText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun validateAndAdd() {
        errorMessage = ""

        val from = fromText.toIntOrNull()
        val to = toText.toIntOrNull()

        when {
            from == null || to == null -> {
                errorMessage = "Please enter valid page numbers"
            }
            from < 1 || to < 1 -> {
                errorMessage = "Page numbers must be at least 1"
            }
            from > totalPages || to > totalPages -> {
                errorMessage = "Page numbers cannot exceed $totalPages"
            }
            from > to -> {
                errorMessage = "Start page must be less than or equal to end page"
            }
            else -> {
                onAdd(from, to)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Page Range", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Define the page range (1-$totalPages)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fromText,
                        onValueChange = { fromText = it },
                        label = { Text("From") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = toText,
                        onValueChange = { toText = it },
                        label = { Text("To") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFF6B6B).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            errorMessage,
                            fontSize = 12.sp,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndAdd() },
                colors = ButtonDefaults.buttonColors(containerColor = AccentSplit)
            ) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}


// ═══════════════════════════════════════════════════════════
//  SPLITTING PDF DIALOG
// ═══════════════════════════════════════════════════════════
@Composable
private fun SplittingPdfDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = AccentSplit,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Splitting PDF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Please wait while your PDF is being processed...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
        shape = RoundedCornerShape(16.dp),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}