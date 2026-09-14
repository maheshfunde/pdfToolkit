package com.yashodatech.pdftoolkit

import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.components.PrecisionGradientButton
import com.yashodatech.pdftoolkit.theme.GradientImageToPdfVibrant

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix as AndroidMatrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Image as PdfImage
import com.itextpdf.layout.properties.AreaBreakType
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.RecentDoc
import com.yashodatech.pdftoolkit.pdf.DocumentProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ══════════════════════════════════════════════════════════════
//  Constants
// ══════════════════════════════════════════════════════════════

private val GradientImagePdf = listOf(Color(0xFFE4572E), Color(0xFFB23F1F))
private val GradientSuccess = listOf(Color(0xFFE4572E), Color(0xFFB23F1F))
private val AccentImagePdf = Color(0xFFE4572E)
private val SuccessColor = Color(0xFFE4572E)

private val CropHandleColor = Color(0xFFE4572E)
private val CropLineColor = Color.White
private val CropOverlayColor = Color.Black.copy(alpha = 0.55f)
private const val MIN_CROP_PX = 50f
private const val HANDLE_TOUCH_RADIUS = 48f
private const val CROP_INIT_PADDING = 16f

private enum class DragHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP_EDGE, BOTTOM_EDGE, LEFT_EDGE, RIGHT_EDGE, CENTER
}

data class ScannedImage(
    val id: String = UUID.randomUUID().toString(),
    val original: Bitmap,
    val display: Bitmap,
    val uri: Uri? = null,
    val name: String = "",
    val isEnhanced: Boolean = false
)

// ══════════════════════════════════════════════════════════════
//  Custom Camera Contract — Android 18+ fix
// ══════════════════════════════════════════════════════════════

private class TakePictureWithGrants : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, input)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("", input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}

// ══════════════════════════════════════════════════════════════
//  Utilities
// ══════════════════════════════════════════════════════════════

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun createCameraFileUri(context: Context): Uri? {
    return try {
        val imageDir = File(context.cacheDir, "camera_images").apply { mkdirs() }
        val imageFile = File(imageDir, "cam_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",  // ← Changed from .fileprovider to .provider
            imageFile
        )
    } catch (e: Exception) {
        Log.e("ImageToPdf", "Failed to create camera file URI", e)
        null
    }
}
private fun loadScaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    return try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        }
        val width = boundsOptions.outWidth
        val height = boundsOptions.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }
    } catch (e: Exception) {
        Log.e("ImageToPdf", "Failed to load bitmap", e)
        null
    }
}

private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    val matrix = AndroidMatrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}
// ══════════════════════════════════════════════════════════════
//  Main Screen
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val processor = remember { DocumentProcessor() }

    var images by remember { mutableStateOf<List<ScannedImage>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var scanMode by remember { mutableStateOf(ScanMode.ORIGINAL) }

    var savedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var savedPdfName by remember { mutableStateOf("") }
    var showSuccessScreen by remember { mutableStateOf(false) }

    var showCropScreen by remember { mutableStateOf(false) }
    var cropTargetIndex by remember { mutableIntStateOf(-1) }

    var showReorderSheet by remember { mutableStateOf(false) }

    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        if (Config.SHOW_ADS) {
            InterstitialAd.load(
                context, "ca-app-pub-7057228007467461/5314339450",
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("ImageToPdf", "Ad failed: ${error.message}")
                        interstitialAd = null
                    }
                }
            )
        }
    }

    val scannerOptions = remember {
        Builder()
            .setGalleryImportAllowed(true).setPageLimit(20)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setScannerMode(SCANNER_MODE_FULL).build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.let { pages ->
                isProcessing = true
                processingMessage = "Loading scanned pages…"
                scope.launch {
                    val newImgs = mutableListOf<ScannedImage>()
                    var c = images.size
                    for (page in pages) {
                        try {
                            val bmp = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(page.imageUri)
                                    ?.use { BitmapFactory.decodeStream(it) }
                            } ?: continue
                            val dsp = if (scanMode != ScanMode.ORIGINAL)
                                withContext(Dispatchers.IO) { processor.enhance(bmp, scanMode) }
                            else bmp.copy(Bitmap.Config.ARGB_8888, true)
                            c++
                            newImgs += ScannedImage(
                                original = bmp, display = dsp,
                                uri = page.imageUri, name = "Scan_$c",
                                isEnhanced = scanMode != ScanMode.ORIGINAL
                            )
                        } catch (e: Exception) {
                            Log.e("ImageToPdf", "Failed to load scanned page", e)
                        }
                    }
                    images = images + newImgs
                    isProcessing = false
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            processingMessage = "Loading images…"
            scope.launch {
                val newImgs = mutableListOf<ScannedImage>()
                var c = images.size
                for (uri in uris) {
                    try {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: SecurityException) {
                            Log.w("ImageToPdf", "Cannot persist URI permission", e)
                        }
                        val bmp = withContext(Dispatchers.IO) {
                            loadScaledBitmap(context, uri, 2048)
                        } ?: continue
                        var name = "Image_${c + 1}"
                        context.contentResolver.query(uri, null, null, null, null)?.use { cur ->
                            val idx = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cur.moveToFirst() && idx >= 0) name = cur.getString(idx) ?: name
                        }
                        c++
                        newImgs += ScannedImage(
                            original = bmp,
                            display = bmp.copy(Bitmap.Config.ARGB_8888, true),
                            uri = uri, name = name, isEnhanced = false
                        )
                    } catch (e: Exception) {
                        Log.e("ImageToPdf", "Failed to load gallery image", e)
                    }
                }
                images = images + newImgs
                isProcessing = false
            }
        }
    }

    // Camera launcher — custom contract with explicit URI grants
    val cameraLauncher = rememberLauncherForActivityResult(
        TakePictureWithGrants()
    ) { success ->
        val capturedUri = cameraImageUri
        if (success && capturedUri != null) {
            isProcessing = true
            processingMessage = "Loading photo…"
            scope.launch {
                try {
                    val bmp = withContext(Dispatchers.IO) {
                        loadScaledBitmap(context, capturedUri, 2048)
                    } ?: return@launch
                    images = images + ScannedImage(
                        original = bmp,
                        display = bmp.copy(Bitmap.Config.ARGB_8888, true),
                        name = "Camera_${images.size + 1}",
                        isEnhanced = false
                    )
                } catch (e: Exception) {
                    Log.e("ImageToPdf", "Failed to load camera photo", e)
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val uri = createCameraFileUri(context)
                if (uri != null) {
                    cameraImageUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    Toast.makeText(context, "Camera error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ImageToPdf", "Camera launch error after permission", e)
                Toast.makeText(context, "Camera error", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Helper functions ──────────────────────────────────

    fun launchCamera() {
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                try {
                    val uri = createCameraFileUri(context)
                    if (uri != null) {
                        cameraImageUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        Toast.makeText(context, "Camera error", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ImageToPdf", "Camera error", e)
                    Toast.makeText(context, "Camera error", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    fun applyEnhancement(newMode: ScanMode) {
        if (images.isEmpty()) return
        isProcessing = true
        processingMessage = "Applying ${newMode.label}…"
        scope.launch {
            val oldDisplays = mutableListOf<Bitmap>()
            images = images.mapNotNull { img ->
                try {
                    val d = if (newMode != ScanMode.ORIGINAL)
                        withContext(Dispatchers.IO) { processor.enhance(img.original, newMode) }
                    else img.original.copy(Bitmap.Config.ARGB_8888, true)
                    oldDisplays.add(img.display)
                    img.copy(display = d, isEnhanced = newMode != ScanMode.ORIGINAL)
                } catch (e: Exception) {
                    Log.e("ImageToPdf", "Enhancement failed", e)
                    null
                }
            }
            withContext(Dispatchers.IO) {
                oldDisplays.forEach { it.recycle() }
            }
            isProcessing = false
        }
    }

    fun moveUp(index: Int) {
        if (index > 0 && index < images.size) {
            val m = images.toMutableList()
            val item = m.removeAt(index)
            m.add(index - 1, item)
            images = m.toList()
        }
    }

    fun moveDown(index: Int) {
        if (index >= 0 && index < images.size - 1) {
            val m = images.toMutableList()
            val item = m.removeAt(index)
            m.add(index + 1, item)
            images = m.toList()
        }
    }

    fun applyCrop(index: Int, cropRect: Rect, imageBounds: Rect) {
        if (index !in images.indices) return
        isProcessing = true
        processingMessage = "Cropping…"
        scope.launch {
            try {
                val img = images[index]
                val bmp = img.original

                val scaleX = bmp.width.toFloat() / imageBounds.width
                val scaleY = bmp.height.toFloat() / imageBounds.height

                val normalizedRect = Rect(
                    min(cropRect.left, cropRect.right),
                    min(cropRect.top, cropRect.bottom),
                    max(cropRect.left, cropRect.right),
                    max(cropRect.top, cropRect.bottom)
                )

                val relLeft = normalizedRect.left - imageBounds.left
                val relTop = normalizedRect.top - imageBounds.top

                val x = (relLeft * scaleX).toInt().coerceIn(0, bmp.width - 1)
                val y = (relTop * scaleY).toInt().coerceIn(0, bmp.height - 1)
                val w = (normalizedRect.width * scaleX).toInt().coerceIn(1, bmp.width - x)
                val h = (normalizedRect.height * scaleY).toInt().coerceIn(1, bmp.height - y)

                val (cropped, display) = withContext(Dispatchers.IO) {
                    val croppedBmp = Bitmap.createBitmap(bmp, x, y, w, h)
                    val displayBmp = if (scanMode != ScanMode.ORIGINAL)
                        processor.enhance(croppedBmp, scanMode)
                    else croppedBmp.copy(Bitmap.Config.ARGB_8888, true)
                    croppedBmp to displayBmp
                }

                val oldDisplay = img.display
                val m = images.toMutableList()
                m[index] = img.copy(
                    original = cropped, display = display,
                    isEnhanced = scanMode != ScanMode.ORIGINAL
                )
                images = m.toList()
                withContext(Dispatchers.IO) { oldDisplay.recycle() }
            } catch (e: Exception) {
                Log.e("ImageToPdf", "Crop failed", e)
            } finally {
                isProcessing = false
            }
        }
    }

    fun rotateImage(index: Int) {
        if (index !in images.indices) return
        isProcessing = true
        processingMessage = "Rotating…"
        scope.launch {
            try {
                val img = images[index]
                val (rO, rD) = withContext(Dispatchers.IO) {
                    rotateBitmap(img.original, 90f) to rotateBitmap(img.display, 90f)
                }
                val oldOriginal = img.original
                val oldDisplay = img.display
                val m = images.toMutableList()
                m[index] = img.copy(original = rO, display = rD)
                images = m.toList()
                withContext(Dispatchers.IO) {
                    oldOriginal.recycle()
                    oldDisplay.recycle()
                }
            } catch (e: Exception) {
                Log.e("ImageToPdf", "Rotate failed", e)
            }
            isProcessing = false
        }
    }

    fun createPdf() {
        if (images.isEmpty()) return
        isProcessing = true
        processingMessage = "Creating PDF…"
        scope.launch {
            try {
                val pdfName = "scanned_document_${System.currentTimeMillis()}.pdf"
                val uri = withContext(Dispatchers.IO) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, pdfName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val outUri = context.contentResolver.insert(
                        MediaStore.Files.getContentUri("external"), values
                    ) ?: throw Exception("Failed to create file")
                    val os = context.contentResolver.openOutputStream(outUri)
                        ?: throw Exception("Failed to open stream")
                    os.use { outputStream ->
                        val writer = PdfWriter(outputStream)
                        val pdfDoc = PdfDocument(writer)
                        val doc = Document(pdfDoc, PageSize.A4)
                        doc.setMargins(20f, 20f, 20f, 20f)
                        images.forEachIndexed { i, img ->
                            if (i > 0) doc.add(AreaBreak(AreaBreakType.NEXT_PAGE))
                            val stream = ByteArrayOutputStream()
                            img.display.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                            val imgData = ImageDataFactory.create(stream.toByteArray())
                            stream.close()
                            val pdfImg = PdfImage(imgData)
                            val ps = PageSize.A4
                            pdfImg.scaleToFit(ps.width - 40f, ps.height - 40f)
                            pdfImg.setFixedPosition(
                                (ps.width - pdfImg.imageScaledWidth) / 2f,
                                (ps.height - pdfImg.imageScaledHeight) / 2f
                            )
                            doc.add(pdfImg)
                        }
                        doc.close()
                    }
                    outUri
                }
                savedPdfUri = uri
                savedPdfName = pdfName
                showSuccessScreen = true
                // Surface the freshly created PDF on the Home recents shelf.
                PreferencesManager(context).addRecentDoc(
                    RecentDoc(
                        name = pdfName,
                        uri = uri.toString(),
                        tool = "Image→PDF",
                        timestamp = System.currentTimeMillis()
                    )
                )
                if (Config.SHOW_ADS) {
                    context.findActivity()?.let { activity ->
                        interstitialAd?.show(activity)
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageToPdf", "PDF creation failed", e)
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isProcessing = false
            }
        }
    }

    fun reset() {
        val toRecycle = images.toList()
        images = emptyList()
        savedPdfUri = null
        savedPdfName = ""
        showSuccessScreen = false
        scanMode = ScanMode.ORIGINAL
        scope.launch(Dispatchers.IO) {
            toRecycle.forEach {
                it.original.recycle()
                it.display.recycle()
            }
        }
    }
    // ── Full-screen crop ────────────────────────────
    if (showCropScreen && cropTargetIndex in images.indices) {
        BackHandler { showCropScreen = false }
        TouchCropScreen(
            bitmap = images[cropTargetIndex].original,
            onApply = { cropRect, imageBounds ->
                applyCrop(cropTargetIndex, cropRect, imageBounds)
                showCropScreen = false
            },
            onCancel = { showCropScreen = false }
        )
        return
    }

    // ── Reorder bottom sheet ────────────────────────
    if (showReorderSheet) {
        ReorderBottomSheet(
            images = images,
            onMoveUp = { moveUp(it) },
            onMoveDown = { moveDown(it) },
            onDismiss = { showReorderSheet = false }
        )
    }

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
                title = "Image to PDF",
                subtitle = "Convert photos, scans, and images into PDF",
                icon = Icons.Rounded.Collections,
                gradientColors = GradientImageToPdfVibrant,
                onBackClick = onBack,
                statusBadge = "HD Conversion"
            )

            ModernGlassLoader(
                isShowing = isProcessing,
                title = processingMessage.ifBlank { "Processing Images..." },
                statusText = "Optimizing image resolution & rendering pages"
            )
            if (showSuccessScreen && savedPdfUri != null) {
                ImagePdfSuccessScreen(
                    savedPdfName, images.size,
                    onOpen = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(savedPdfUri, "application/pdf")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            })
                        } catch (e: Exception) {
                            Log.e("ImageToPdf", "Open failed", e)
                            Toast.makeText(context, "No PDF viewer", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShare = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, savedPdfUri)
                                clipData = ClipData.newRawUri("", savedPdfUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Share PDF")
                            )
                        } catch (e: Exception) {
                            Log.e("ImageToPdf", "Share failed", e)
                            Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCreateAnother = { reset() },
                    onBack = onBack
                )
            } else {
                Column(Modifier.weight(1f)) {
                    // Source buttons
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SourceButton(
                            Icons.Rounded.DocumentScanner, "Scan",
                            listOf(Color(0xFFE4572E), Color(0xFFB23F1F)), Modifier.weight(1f)
                        ) {
                            val activity = context.findActivity()
                            if (activity != null) {
                                scanner.getStartScanIntent(activity)
                                    .addOnSuccessListener {
                                        scannerLauncher.launch(
                                            IntentSenderRequest.Builder(it).build()
                                        )
                                    }
                                    .addOnFailureListener {
                                        Log.e("ImageToPdf", "Scanner unavailable", it)
                                        Toast.makeText(
                                            context, "Scanner unavailable", Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else {
                                Toast.makeText(
                                    context, "Cannot access scanner", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        SourceButton(
                            Icons.Rounded.PhotoLibrary, "Gallery",
                            listOf(Color(0xFFE4572E), Color(0xFFB23F1F)), Modifier.weight(1f)
                        ) {
                            galleryLauncher.launch(arrayOf("image/*"))
                        }
                        SourceButton(
                            Icons.Rounded.CameraAlt, "Camera",
                            listOf(Color(0xFFE4572E), Color(0xFFB23F1F)), Modifier.weight(1f)
                        ) {
                            launchCamera()
                        }
                    }

                    if (images.isNotEmpty()) {
                        ScanModeSelector(
                            scanMode,
                            { scanMode = it; applyEnhancement(it) },
                            Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    if (images.isEmpty()) {
                        ImagePdfEmptyState()
                    } else {
                        // Page count + hint
                        Surface(
                            color = AccentImagePdf.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Collections, null,
                                    tint = AccentImagePdf, modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    buildString {
                                        append("${images.size} page${if (images.size != 1) "s" else ""}")
                                        if (scanMode != ScanMode.ORIGINAL) append(" • ${scanMode.label}")
                                        if (images.size > 1) append(" • Tap ⇅ to reorder")
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AccentImagePdf
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            gridItemsIndexed(
                                items = images,
                                key = { _, img -> img.id }
                            ) { index, image ->
                                ImageCard(
                                    image = image,
                                    index = index,
                                    totalCount = images.size,
                                    onRemove = {
                                        val m = images.toMutableList()
                                        val r = m.removeAt(index)
                                        images = m.toList()
                                        scope.launch(Dispatchers.IO) {
                                            r.original.recycle()
                                            r.display.recycle()
                                        }
                                    },
                                    onMoveUp = { moveUp(index) },
                                    onMoveDown = { moveDown(index) },
                                    onCrop = {
                                        cropTargetIndex = index
                                        showCropScreen = true
                                    },
                                    onRotate = { rotateImage(index) }
                                )
                            }
                        }
                    }
                }

                if (images.isNotEmpty()) CreatePdfBottomBar { createPdf() }
            }
        }
        if (isProcessing) ProcessingDialog(processingMessage)
    }
}
// ══════════════════════════════════════════════════════════════
//  REORDER BOTTOM SHEET
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReorderBottomSheet(
    images: List<ScannedImage>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Reorder Pages",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Tap arrows to move pages up or down",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    items = images,
                    key = { _, img -> img.id }
                ) { index, image ->
                    ReorderItem(
                        image = image,
                        index = index,
                        totalCount = images.size,
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentImagePdf)
            ) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ReorderItem(
    image: ScannedImage,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            ) {
                Image(
                    bitmap = image.display.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(GradientImagePdf))
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Page ${index + 1}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    image.name,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onMoveUp,
                enabled = index > 0,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    "Move Up",
                    tint = if (index > 0) AccentImagePdf else Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = index < totalCount - 1,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    "Move Down",
                    tint = if (index < totalCount - 1) AccentImagePdf
                    else Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
// ══════════════════════════════════════════════════════════════
//  TOUCH CROP SCREEN
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TouchCropScreen(
    bitmap: Bitmap,
    onApply: (Rect, Rect) -> Unit,
    onCancel: () -> Unit
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageBounds by remember { mutableStateOf(Rect.Zero) }
    var cropInitialized by remember { mutableStateOf(false) }

    var cropLeft by remember { mutableFloatStateOf(-1f) }
    var cropTop by remember { mutableFloatStateOf(-1f) }
    var cropRight by remember { mutableFloatStateOf(-1f) }
    var cropBottom by remember { mutableFloatStateOf(-1f) }

    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartRect by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(containerSize) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            val cw = containerSize.width.toFloat()
            val ch = containerSize.height.toFloat()
            val bw = bitmap.width.toFloat()
            val bh = bitmap.height.toFloat()

            val scale = min(cw / bw, ch / bh)
            val scaledW = bw * scale
            val scaledH = bh * scale
            val offsetX = (cw - scaledW) / 2f
            val offsetY = (ch - scaledH) / 2f

            imageBounds = Rect(offsetX, offsetY, offsetX + scaledW, offsetY + scaledH)

            if (!cropInitialized) {
                cropLeft = imageBounds.left + CROP_INIT_PADDING
                cropTop = imageBounds.top + CROP_INIT_PADDING
                cropRight = imageBounds.right - CROP_INIT_PADDING
                cropBottom = imageBounds.bottom - CROP_INIT_PADDING
                cropInitialized = true
            }
        }
    }

    fun clampToImage(x: Float, y: Float): Offset {
        return Offset(
            x.coerceIn(imageBounds.left, imageBounds.right),
            y.coerceIn(imageBounds.top, imageBounds.bottom)
        )
    }

    fun findHandle(pos: Offset): DragHandle {
        val corners = listOf(
            DragHandle.TOP_LEFT to Offset(cropLeft, cropTop),
            DragHandle.TOP_RIGHT to Offset(cropRight, cropTop),
            DragHandle.BOTTOM_LEFT to Offset(cropLeft, cropBottom),
            DragHandle.BOTTOM_RIGHT to Offset(cropRight, cropBottom),
        )
        for ((handle, point) in corners) {
            if ((pos - point).getDistance() < HANDLE_TOUCH_RADIUS) return handle
        }
        val edgeTol = HANDLE_TOUCH_RADIUS * 0.8f
        if (abs(pos.y - cropTop) < edgeTol && pos.x in (cropLeft - edgeTol)..(cropRight + edgeTol))
            return DragHandle.TOP_EDGE
        if (abs(pos.y - cropBottom) < edgeTol && pos.x in (cropLeft - edgeTol)..(cropRight + edgeTol))
            return DragHandle.BOTTOM_EDGE
        if (abs(pos.x - cropLeft) < edgeTol && pos.y in (cropTop - edgeTol)..(cropBottom + edgeTol))
            return DragHandle.LEFT_EDGE
        if (abs(pos.x - cropRight) < edgeTol && pos.y in (cropTop - edgeTol)..(cropBottom + edgeTol))
            return DragHandle.RIGHT_EDGE
        if (pos.x in cropLeft..cropRight && pos.y in cropTop..cropBottom)
            return DragHandle.CENTER
        return DragHandle.NONE
    }

    fun resetCropBounds() {
        cropLeft = imageBounds.left + CROP_INIT_PADDING
        cropTop = imageBounds.top + CROP_INIT_PADDING
        cropRight = imageBounds.right - CROP_INIT_PADDING
        cropBottom = imageBounds.bottom - CROP_INIT_PADDING
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop Image", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF151210),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(color = Color(0xFF151210), tonalElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { resetCropBounds() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt, null,
                            tint = Color.White, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Reset", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val rect = Rect(
                                min(cropLeft, cropRight), min(cropTop, cropBottom),
                                max(cropLeft, cropRight), max(cropTop, cropBottom)
                            )
                            onApply(rect, imageBounds)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(GradientImagePdf),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Crop, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Apply Crop", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF151210)
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Drag corners or edges to crop",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 32.dp)
                    .onGloballyPositioned { containerSize = it.size }
                    .pointerInput(imageBounds) {
                        if (imageBounds == Rect.Zero) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                activeHandle = findHandle(offset)
                                dragStartOffset = offset
                                dragStartRect = Rect(cropLeft, cropTop, cropRight, cropBottom)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val pos = change.position
                                val clamped = clampToImage(pos.x, pos.y)

                                when (activeHandle) {
                                    DragHandle.TOP_LEFT -> {
                                        cropLeft = clamped.x.coerceAtMost(cropRight - MIN_CROP_PX)
                                        cropTop = clamped.y.coerceAtMost(cropBottom - MIN_CROP_PX)
                                    }
                                    DragHandle.TOP_RIGHT -> {
                                        cropRight = clamped.x.coerceAtLeast(cropLeft + MIN_CROP_PX)
                                        cropTop = clamped.y.coerceAtMost(cropBottom - MIN_CROP_PX)
                                    }
                                    DragHandle.BOTTOM_LEFT -> {
                                        cropLeft = clamped.x.coerceAtMost(cropRight - MIN_CROP_PX)
                                        cropBottom = clamped.y.coerceAtLeast(cropTop + MIN_CROP_PX)
                                    }
                                    DragHandle.BOTTOM_RIGHT -> {
                                        cropRight = clamped.x.coerceAtLeast(cropLeft + MIN_CROP_PX)
                                        cropBottom = clamped.y.coerceAtLeast(cropTop + MIN_CROP_PX)
                                    }
                                    DragHandle.TOP_EDGE -> {
                                        cropTop = clamped.y.coerceAtMost(cropBottom - MIN_CROP_PX)
                                    }
                                    DragHandle.BOTTOM_EDGE -> {
                                        cropBottom = clamped.y.coerceAtLeast(cropTop + MIN_CROP_PX)
                                    }
                                    DragHandle.LEFT_EDGE -> {
                                        cropLeft = clamped.x.coerceAtMost(cropRight - MIN_CROP_PX)
                                    }
                                    DragHandle.RIGHT_EDGE -> {
                                        cropRight = clamped.x.coerceAtLeast(cropLeft + MIN_CROP_PX)
                                    }
                                    DragHandle.CENTER -> {
                                        val dx = pos.x - dragStartOffset.x
                                        val dy = pos.y - dragStartOffset.y
                                        val w = dragStartRect.width
                                        val h = dragStartRect.height
                                        var nL = dragStartRect.left + dx
                                        var nT = dragStartRect.top + dy
                                        nL = nL.coerceIn(imageBounds.left, imageBounds.right - w)
                                        nT = nT.coerceIn(imageBounds.top, imageBounds.bottom - h)
                                        cropLeft = nL
                                        cropTop = nT
                                        cropRight = nL + w
                                        cropBottom = nT + h
                                    }
                                    DragHandle.NONE -> {}
                                }
                            },
                            onDragEnd = { activeHandle = DragHandle.NONE }
                        )
                    }
                    .drawWithContent {
                        drawContent()
                        if (cropLeft < 0) return@drawWithContent
                        val cr = Rect(cropLeft, cropTop, cropRight, cropBottom)

                        // Overlay
                        drawRect(CropOverlayColor, Offset.Zero, Size(size.width, cr.top))
                        drawRect(CropOverlayColor, Offset(0f, cr.bottom), Size(size.width, size.height - cr.bottom))
                        drawRect(CropOverlayColor, Offset(0f, cr.top), Size(cr.left, cr.height))
                        drawRect(CropOverlayColor, Offset(cr.right, cr.top), Size(size.width - cr.right, cr.height))

                        // Border
                        drawRect(CropLineColor, Offset(cr.left, cr.top), Size(cr.width, cr.height), style = Stroke(2.dp.toPx()))

                        // Grid
                        val tw = cr.width / 3f
                        val th = cr.height / 3f
                        for (i in 1..2) {
                            drawLine(CropLineColor.copy(alpha = 0.25f), Offset(cr.left + tw * i, cr.top), Offset(cr.left + tw * i, cr.bottom), 1.dp.toPx())
                            drawLine(CropLineColor.copy(alpha = 0.25f), Offset(cr.left, cr.top + th * i), Offset(cr.right, cr.top + th * i), 1.dp.toPx())
                        }

                        // Corner L-brackets
                        val bLen = 28.dp.toPx()
                        val bW = 4.dp.toPx()
                        listOf(
                            Triple(Offset(cr.left, cr.top), 1f, 1f),
                            Triple(Offset(cr.right, cr.top), -1f, 1f),
                            Triple(Offset(cr.left, cr.bottom), 1f, -1f),
                            Triple(Offset(cr.right, cr.bottom), -1f, -1f),
                        ).forEach { (corner, dx, dy) ->
                            drawLine(CropHandleColor, corner, Offset(corner.x + bLen * dx, corner.y), bW)
                            drawLine(CropHandleColor, corner, Offset(corner.x, corner.y + bLen * dy), bW)
                        }

                        // Corner dots
                        val cR = 10.dp.toPx()
                        listOf(
                            Offset(cr.left, cropTop), Offset(cropRight, cropTop),
                            Offset(cropLeft, cropBottom), Offset(cropRight, cropBottom),
                        ).forEach { drawCircle(CropHandleColor, cR, it) }

                        // Edge midpoints
                        val mR = 6.dp.toPx()
                        listOf(
                            Offset(cr.left + cr.width / 2, cr.top),
                            Offset(cr.left + cr.width / 2, cr.bottom),
                            Offset(cr.left, cr.top + cr.height / 2),
                            Offset(cr.right, cr.top + cr.height / 2),
                        ).forEach { drawCircle(CropHandleColor, mR, it) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Crop preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Size indicator
            if (imageBounds != Rect.Zero && cropLeft >= 0) {
                val cW = ((abs(cropRight - cropLeft) / imageBounds.width) * bitmap.width).toInt()
                val cH = ((abs(cropBottom - cropTop) / imageBounds.height) * bitmap.height).toInt()
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        "$cW × $cH px",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
// ══════════════════════════════════════════════════════════════
//  IMAGE CARD
// ══════════════════════════════════════════════════════════════

@Composable
private fun ImageCard(
    image: ScannedImage,
    index: Int,
    totalCount: Int,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onCrop: () -> Unit,
    onRotate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.65f),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(
                bitmap = image.display.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Page number
            Box(
                Modifier
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(GradientImagePdf))
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }

            // Remove
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Rounded.Close, "Remove",
                    tint = Color.White, modifier = Modifier.size(14.dp)
                )
            }

            // Bottom action bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMoveUp, enabled = index > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp, "Up",
                        tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown, enabled = index < totalCount - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown, "Down",
                        tint = if (index < totalCount - 1) Color.White
                        else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onCrop, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Crop, "Crop",
                        tint = Color.White, modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onRotate, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.RotateRight, "Rotate",
                        tint = Color.White, modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (image.isEnhanced) {
                Surface(
                    color = AccentImagePdf.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                ) {
                    Text(
                        "Enhanced", color = Color.White, fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════
//  SCAN MODE SELECTOR
// ══════════════════════════════════════════════════════════════

@Composable
private fun ScanModeSelector(
    selected: ScanMode,
    onSelect: (ScanMode) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ScanMode.entries.size) { i ->
            val mode = ScanMode.entries[i]
            val sel = selected == mode
            val chipColor = if (mode == ScanMode.SCAN) Color(0xFF00897B) else AccentImagePdf

            FilterChip(
                onClick = { onSelect(mode) },
                label = {
                    Text(
                        mode.label, fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                selected = sel,
                leadingIcon = {
                    if (sel) {
                        Icon(
                            if (mode == ScanMode.SCAN) Icons.Rounded.DocumentScanner
                            else Icons.Rounded.Check,
                            null, Modifier.size(16.dp)
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor.copy(alpha = 0.15f),
                    selectedLabelColor = chipColor,
                    selectedLeadingIconColor = chipColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = sel,
                    selectedBorderColor = chipColor,
                    selectedBorderWidth = 1.5.dp
                )
            )
        }
    }
}


// ══════════════════════════════════════════════════════════════
//  SOURCE BUTTON
// ══════════════════════════════════════════════════════════════

@Composable
private fun SourceButton(
    icon: ImageVector, label: String, gradient: List<Color>,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(70.dp)
            .shadow(
                4.dp, RoundedCornerShape(14.dp),
                ambientColor = gradient.first().copy(alpha = 0.3f),
                spotColor = gradient.first().copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    label, color = Color.White,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════
//  EMPTY STATE
// ══════════════════════════════════════════════════════════════

@Composable
private fun ImagePdfEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(AccentImagePdf.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.DocumentScanner, null,
                    tint = AccentImagePdf.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "No images added",
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Use Scan for auto edge detection\nor pick from Gallery / Camera",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center, lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                FeatureRow(Icons.Rounded.DocumentScanner, "Auto edge detection (Scan)")
                Spacer(Modifier.height(6.dp))
                FeatureRow(Icons.Rounded.Crop, "Touch-based crop")
                Spacer(Modifier.height(6.dp))
                FeatureRow(Icons.Rounded.SwapVert, "Reorder pages")
                Spacer(Modifier.height(6.dp))
                FeatureRow(Icons.Rounded.Tune, "Enhancement filters")
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, null, tint = AccentImagePdf.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text, fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}


// ══════════════════════════════════════════════════════════════
//  CREATE PDF BOTTOM BAR
// ══════════════════════════════════════════════════════════════

@Composable
private fun CreatePdfBottomBar(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        PrecisionGradientButton(
            text = "Create PDF",
            onClick = onClick,
            gradient = GradientImagePdf,
            icon = Icons.Rounded.PictureAsPdf
        )
    }
}


// ══════════════════════════════════════════════════════════════
//  SUCCESS SCREEN
// ══════════════════════════════════════════════════════════════

@Composable
private fun ImagePdfSuccessScreen(
    fileName: String, pageCount: Int, onOpen: () -> Unit,
    onShare: () -> Unit, onCreateAnother: () -> Unit, onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(0.2f))
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(SuccessColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(GradientSuccess)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CheckCircle, null,
                    tint = Color.White, modifier = Modifier.size(44.dp)
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "PDF Created Successfully!",
            fontWeight = FontWeight.Bold, fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            color = AccentImagePdf.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.PictureAsPdf, null,
                    tint = AccentImagePdf, modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    fileName, fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, color = AccentImagePdf,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "$pageCount page${if (pageCount != 1) "s" else ""} • Saved to Downloads",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(GradientImagePdf)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew, null,
                    tint = AccentImagePdf, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Open File", color = AccentImagePdf,
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
                    brush = Brush.horizontalGradient(GradientImagePdf)
                )
            ) {
                Icon(
                    Icons.Outlined.Share, null,
                    tint = AccentImagePdf, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Share", color = AccentImagePdf,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
            }
        }
        Spacer(Modifier.weight(0.2f))
        PrecisionGradientButton(
            text = "Create Another PDF",
            onClick = onCreateAnother,
            gradient = GradientImagePdf,
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
            Text(
                "Back to Home",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}


// ══════════════════════════════════════════════════════════════
//  PROCESSING DIALOG
// ══════════════════════════════════════════════════════════════

@Composable
private fun ProcessingDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(24.dp),
        title = null,
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(
                    Modifier.size(48.dp),
                    color = AccentImagePdf, strokeWidth = 4.dp
                )
                Spacer(Modifier.height(20.dp))
                Text("Processing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    message, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {}
    )
}