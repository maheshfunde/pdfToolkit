package com.yashodatech.pdftoolkit

import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.theme.GradientWordToPdfVibrant

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.pdf.WordToPdfConverter
import com.yashodatech.pdftoolkit.utils.FileHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordToPdfScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val converter = remember { WordToPdfConverter(context) }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableStateOf("") }
    var outputFileName by remember { mutableStateOf("") }
    var isConverting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var conversionSuccess by remember { mutableStateOf<Boolean?>(null) }
    var resultMessage by remember { mutableStateOf("") }

    // ✅ Store the saved URI (not File)
    var savedPdfUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            conversionSuccess = null
            savedPdfUri = null

            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()

                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "document.docx"
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L

                selectedFileName = name
                selectedFileSize = formatFileSize(size)
                outputFileName = name.substringBeforeLast(".")
            }
        }
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
                title = "Word to PDF",
                subtitle = "Convert DOCX documents to PDF format",
                icon = Icons.Rounded.Description,
                gradientColors = GradientWordToPdfVibrant,
                onBackClick = onBack,
                statusBadge = "DOCX Engine"
            )

            ModernGlassLoader(
                isShowing = isConverting,
                title = "Converting Word Document...",
                statusText = "Parsing DOCX layout & generating PDF pages"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {

            // ════════════════════════════════
            // File Selection Card
            // ════════════════════════════════
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/msword"
                            )
                        )
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedFileUri == null) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(GradientWordToPdfVibrant)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Select Word Document", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap to choose a .docx or .doc file",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormatChip(".DOCX")
                            FormatChip(".DOC")
                        }

                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(GradientWordToPdfVibrant)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("W", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    selectedFileName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(selectedFileSize, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            IconButton(
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "application/msword"
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, "Change file", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ════════════════════════════════
            // Output File Name
            // ════════════════════════════════
            AnimatedVisibility(
                visible = selectedFileUri != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    OutlinedTextField(
                        value = outputFileName,
                        onValueChange = { outputFileName = it },
                        label = { Text("Output PDF Name") },
                        placeholder = { Text("Enter file name") },
                        leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, null) },
                        trailingIcon = {
                            Text(
                                ".pdf",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            // ════════════════════════════════
            // Convert Button
            // ════════════════════════════════
            AnimatedVisibility(
                visible = selectedFileUri != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Button(
                    onClick = {
                        if (outputFileName.isBlank()) {
                            Toast.makeText(context, "Enter output file name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            isConverting = true
                            progress = 0f
                            conversionSuccess = null
                            savedPdfUri = null

                            // Step 1: Convert to PDF (saved in cache)
                            val result = converter.convert(
                                inputUri = selectedFileUri!!,
                                outputFileName = outputFileName,
                                onProgress = { progress = it * 0.8f }  // 80% for conversion
                            )

                            if (result.success && result.tempFile != null) {

                                // Step 2: Save to Downloads folder
                                val uri = FileHelper.savePdfToDownloads(
                                    context = context,
                                    tempFile = result.tempFile,
                                    fileName = outputFileName
                                )

                                if (uri != null) {
                                    savedPdfUri = uri
                                    conversionSuccess = true
                                    resultMessage = "Saved to Downloads/PDFToolkit\n${result.pageCount} page(s)"
                                    progress = 1f
                                } else {
                                    conversionSuccess = false
                                    resultMessage = "Converted but failed to save to Downloads"
                                }

                            } else {
                                conversionSuccess = false
                                resultMessage = result.error ?: "Conversion failed"
                            }

                            isConverting = false
                        }
                    },
                    enabled = !isConverting && selectedFileUri != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(GradientWordToPdfVibrant),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isConverting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    "Converting... ${(progress * 100).toInt()}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Transform, null, tint = Color.White)
                                Text(
                                    "Convert to PDF",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // ════════════════════════════════
            // Progress Bar
            // ════════════════════════════════
            AnimatedVisibility(visible = isConverting) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ════════════════════════════════
            // Result Card
            // ════════════════════════════════
            AnimatedVisibility(
                visible = conversionSuccess != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (conversionSuccess == true)
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else
                            Color(0xFFF44336).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Status Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (conversionSuccess == true)
                                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    else
                                        Color(0xFFF44336).copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (conversionSuccess == true)
                                    Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                contentDescription = null,
                                tint = if (conversionSuccess == true)
                                    Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = if (conversionSuccess == true) "Conversion Successful!" else "Conversion Failed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = resultMessage,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        // ✅ Action Buttons - Using URI
                        if (conversionSuccess == true && savedPdfUri != null) {
                            Spacer(Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // ✅ Open PDF
                                OutlinedButton(
                                    onClick = {
                                        FileHelper.openPdf(context, savedPdfUri!!)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open")
                                }

                                // ✅ Share PDF
                                OutlinedButton(
                                    onClick = {
                                        FileHelper.sharePdf(
                                            context,
                                            savedPdfUri!!,
                                            outputFileName
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ════════════════════════════════
            // Features Info Card
            // ════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Features",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FeatureItem(Icons.Outlined.TextFormat, "Preserves Bold, Italic, Underline")
                    FeatureItem(Icons.Outlined.FormatSize, "Maintains heading styles & sizes")
                    FeatureItem(Icons.Outlined.FormatListBulleted, "Supports bullet & numbered lists")
                    FeatureItem(Icons.Outlined.SaveAlt, "Saves to Downloads/PDFToolkit folder")
                    FeatureItem(Icons.Outlined.Security, "All processing done locally")
                }
                }
            }
        }
    }
}

@Composable
private fun FormatChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "%.1f MB".format(size / (1024.0 * 1024.0))
    }
}