package com.yashodatech.pdftoolkit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.itextpdf.kernel.exceptions.BadPasswordException
import com.yashodatech.pdftoolkit.components.*
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.RecentDoc
import com.yashodatech.pdftoolkit.pdf.PdfUnlocker
import com.yashodatech.pdftoolkit.theme.*
import kotlinx.coroutines.launch

private fun formatUnlockBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

@Composable
fun UnlockPdfScreen(
    onBack: () -> Unit,
    onOpenDocument: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var sourceSize by remember { mutableStateOf("") }
    var lockState by remember { mutableStateOf<PdfUnlocker.LockState?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isUnlocking by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
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

    val unlocker = remember { PdfUnlocker(context) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            sourceUri = it
            password = ""
            passwordError = null
            lockState = null
            resultUri = null
            showSuccess = false

            context.contentResolver.query(it, null, null, null, null)?.use { c ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) sourceName = c.getString(nameIdx) ?: "document.pdf"
                    if (sizeIdx >= 0) sourceSize = formatUnlockBytes(c.getLong(sizeIdx))
                }
            }

            isLoading = true
            scope.launch {
                lockState = unlocker.probeLock(it)
                isLoading = false
            }
        }
    }

    fun runUnlock() {
        if (sourceUri == null) return
        isUnlocking = true
        passwordError = null
        scope.launch {
            try {
                val pw = if (lockState == PdfUnlocker.LockState.USER_PASSWORD) password else null
                val outUri = unlocker.unlockPdf(
                    sourceUri = sourceUri!!,
                    baseName = sourceName,
                    password = pw
                )
                resultUri = Uri.parse(outUri)
                val prefs = PreferencesManager(context)
                val outName = "${sourceName.removeSuffix(".pdf")}_unlocked.pdf"
                prefs.addRecentDoc(
                    RecentDoc(
                        name = outName,
                        uri = outUri,
                        tool = "Unlock",
                        timestamp = System.currentTimeMillis()
                    )
                )
                showSuccess = true
                if (Config.SHOW_ADS) interstitialAd?.show(context as Activity)
            } catch (_: BadPasswordException) {
                passwordError = "Incorrect password — please verify and try again."
            } catch (e: Exception) {
                android.util.Log.e("PDFToolkit", "Unlock failed", e)
                Toast.makeText(
                    context,
                    "Unlock failed: ${e.localizedMessage ?: "unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isUnlocking = false
            }
        }
    }

    fun resetForNew() {
        sourceUri = null
        sourceName = ""
        sourceSize = ""
        lockState = null
        password = ""
        passwordError = null
        resultUri = null
        showSuccess = false
    }

    AdaptiveToolScaffold(
        title = "Unlock PDF",
        subtitle = "Remove encryption or restrictions from a PDF you own",
        icon = Icons.Rounded.LockOpen,
        accentColor = ToolSecurityAccent,
        onBack = onBack,
        previewPane = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ModernFilePickerCard(
                    selectedFileUri = sourceUri,
                    fileName = sourceName,
                    fileSize = sourceSize,
                    accentColor = ToolSecurityAccent,
                    onPick = { launcher.launch(arrayOf("application/pdf")) },
                    onClear = { resetForNew() },
                    emptyPrompt = "Select Protected PDF",
                    emptySubtext = "Supports password-protected documents"
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ToolSecurityAccent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (lockState != null) {
                    when (lockState) {
                        PdfUnlocker.LockState.NOT_PROTECTED -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = ToolConvertAccent.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, ToolConvertAccent.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LockOpen,
                                        contentDescription = null,
                                        tint = ToolConvertAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "This document is already unprotected. No password needed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        PdfUnlocker.LockState.PERMISSIONS_ONLY -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = ToolOrganizeAccent.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, ToolOrganizeAccent.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LockOpen,
                                        contentDescription = null,
                                        tint = ToolOrganizeAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Document has owner permissions restrictions only. Can be unlocked without password.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        },
        controlsPane = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (lockState == PdfUnlocker.LockState.USER_PASSWORD) {
                    ModernActionCard(
                        title = "Document Password",
                        accentColor = ToolSecurityAccent
                    ) {
                        Text(
                            text = "Enter the current document password to generate an unlocked copy:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            singleLine = true,
                            isError = passwordError != null,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { runUnlock() }),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (passwordError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = passwordError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            val canUnlock = sourceUri != null && !isLoading && !isUnlocking &&
                    (lockState == PdfUnlocker.LockState.PERMISSIONS_ONLY ||
                            (lockState == PdfUnlocker.LockState.USER_PASSWORD && password.isNotBlank()) ||
                            lockState == PdfUnlocker.LockState.NOT_PROTECTED)

            ModernStickyBottomBar {
                ModernPrimaryButton(
                    text = "Unlock & Save PDF",
                    icon = Icons.Rounded.LockOpen,
                    accentColor = ToolSecurityAccent,
                    enabled = canUnlock,
                    isLoading = isUnlocking,
                    onClick = { runUnlock() }
                )
            }
        }
    )

    ModernGlassLoader(
        isShowing = isUnlocking,
        title = "Removing Lock...",
        subtitle = "Generating unlocked PDF document",
        accentColor = ToolSecurityAccent
    )

    if (showSuccess && resultUri != null) {
        val outName = "${sourceName.removeSuffix(".pdf")}_unlocked.pdf"
        ModernSuccessSheet(
            fileName = outName,
            onOpen = {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(resultUri, "application/pdf")
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
                            putExtra(Intent.EXTRA_STREAM, resultUri)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        },
                        "Share Unlocked PDF"
                    )
                )
            },
            onDone = { resetForNew() }
        )
    }
}