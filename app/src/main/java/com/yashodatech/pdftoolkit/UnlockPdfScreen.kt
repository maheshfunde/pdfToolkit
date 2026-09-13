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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.components.LiquidGlassCard
import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.components.LiquidPrimaryButton
import com.yashodatech.pdftoolkit.components.ModernGlassLoader
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.RecentDoc
import com.yashodatech.pdftoolkit.pdf.PdfUnlocker
import com.yashodatech.pdftoolkit.theme.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.itextpdf.kernel.exceptions.BadPasswordException
import kotlinx.coroutines.launch

private fun formatUnlockBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

// ═══════════════════════════════════════════════════════════
//  Unlock / remove password — drop protection from a PDF you own
// ═══════════════════════════════════════════════════════════
@Composable
fun UnlockPdfScreen(onBack: () -> Unit, onOpenDocument: (String) -> Unit = {}) {
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
                    if (nameIdx >= 0) sourceName = c.getString(nameIdx) ?: "Unknown.pdf"
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
                prefs.addRecentDoc(
                    RecentDoc(
                        name = sourceName.removeSuffix(".pdf") + "_unlocked.pdf",
                        uri = outUri,
                        tool = "Unlock",
                        timestamp = System.currentTimeMillis()
                    )
                )
                showSuccess = true
                if (Config.SHOW_ADS) interstitialAd?.show(context as Activity)
            } catch (e: BadPasswordException) {
                passwordError = "Incorrect password — try again."
            } catch (e: Exception) {
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            LiquidHeader(
                title = "Unlock PDF",
                subtitle = "Remove a password you own",
                icon = Icons.Rounded.LockOpen,
                gradientColors = GradientUnlockVibrant,
                onBackClick = onBack,
                statusBadge = "Access"
            )

            if (isUnlocking) {
                ModernGlassLoader(
                    isShowing = true,
                    title = "Removing lock...",
                    statusText = "Saving an unlocked copy"
                )
            }

            if (showSuccess && resultUri != null) {
                UnlockSuccess(
                    displayName = sourceName.removeSuffix(".pdf").substringBeforeLast(".")
                        .ifBlank { "document" } + "_unlocked.pdf",
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
                                if (sourceUri != null) "Change PDF File" else "Select Protected PDF",
                                fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InkBone
                            )
                        }
                    }

                    if (sourceUri != null) {
                        Spacer(Modifier.height(16.dp))

                        if (isLoading) {
                            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Vermilion, modifier = Modifier.size(44.dp))
                            }
                        } else {
                            // ── File info + lock status ──
                            LiquidGlassCard {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(sourceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkBone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("$sourceSize", fontSize = 11.sp, color = InkMuted)
                                        }
                                        LockStatusBadge(lockState)
                                    }
                                }
                            }

                            // ── Password field, only when one is required ──
                            if (lockState == PdfUnlocker.LockState.USER_PASSWORD) {
                                Spacer(Modifier.height(16.dp))
                                PasswordField(
                                    password = password,
                                    visible = passwordVisible,
                                    error = passwordError,
                                    onPasswordChange = {
                                        password = it
                                        passwordError = null
                                    },
                                    onToggleVisible = { passwordVisible = !passwordVisible }
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            // ── What happens ──
                            UnlockHint(lockState)

                            Spacer(Modifier.height(20.dp))

                            LiquidPrimaryButton(
                                text = if (lockState == PdfUnlocker.LockState.USER_PASSWORD) "Unlock & Save"
                                else "Save Unlocked Copy",
                                onClick = { runUnlock() },
                                enabled = sourceUri != null && !isUnlocking &&
                                    (lockState != PdfUnlocker.LockState.USER_PASSWORD || password.isNotBlank())
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    } else {
                        Spacer(Modifier.height(120.dp))
                        UnlockEmptyState()
                    }
                }
            }
        }
    }
}

@Composable
private fun LockStatusBadge(lockState: PdfUnlocker.LockState?) {
    val (label, tint) = when (lockState) {
        PdfUnlocker.LockState.NOT_PROTECTED -> "Not protected" to InkMuted
        PdfUnlocker.LockState.PERMISSIONS_ONLY -> "Restricted copy/print" to WarningAmber
        PdfUnlocker.LockState.USER_PASSWORD -> "Password protected" to Vermilion
        null -> "Checking…" to InkFaint
    }
    Text(
        label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun PasswordField(
    password: String,
    visible: Boolean,
    error: String?,
    onPasswordChange: (String) -> Unit,
    onToggleVisible: () -> Unit
) {
    Column {
        Text(
            if (error != null) "Password" else "Enter the PDF password",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = error?.let { ErrorRed } ?: InkMuted
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error != null,
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = InkBone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Vermilion,
                unfocusedBorderColor = InkBorder,
                errorBorderColor = ErrorRed,
                focusedTextColor = InkBone,
                unfocusedTextColor = InkBone,
                cursorColor = Vermilion,
                focusedContainerColor = InkSurface,
                unfocusedContainerColor = InkSurface
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
            ),
            visualTransformation = if (visible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisible) {
                    Icon(
                        if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (visible) "Hide password" else "Show password",
                        tint = InkMuted
                    )
                }
            }
        )
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, fontSize = 12.sp, color = ErrorRed)
        }
    }
}

@Composable
private fun UnlockHint(lockState: PdfUnlocker.LockState?) {
    LiquidGlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, tint = Vermilion, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("What happens", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkBone)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when (lockState) {
                PdfUnlocker.LockState.NOT_PROTECTED ->
                    "This PDF has no password — you can still save an unlocked copy if you like."
                PdfUnlocker.LockState.PERMISSIONS_ONLY ->
                    "Editing, copying and printing are restricted. Unlock to remove those limits."
                PdfUnlocker.LockState.USER_PASSWORD ->
                    "You'll need the password you set. The unlocked copy opens freely with no restictions."
                null -> ""
            },
            fontSize = 12.sp, lineHeight = 17.sp, color = InkMuted
        )
    }
}

@Composable
private fun UnlockEmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(100.dp).clip(CircleShape).background(Vermilion.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.LockOpen, null, tint = Vermilion.copy(alpha = 0.5f), modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("No PDF selected", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = InkMuted)
        Spacer(Modifier.height(8.dp))
        Text("Pick a password-protected PDF to unlock it", fontSize = 13.sp, color = InkFaint, textAlign = TextAlign.Center)
    }
}

@Composable
private fun UnlockSuccess(
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
                Icon(Icons.Rounded.LockOpen, null, tint = InkBone, modifier = Modifier.size(38.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Unlocked!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = InkBone)
        Spacer(Modifier.height(8.dp))
        Text("Saved as $displayName", fontSize = 13.sp, color = InkMuted, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text("to Downloads — it now opens without a password", fontSize = 12.sp, color = InkFaint)
        Spacer(Modifier.height(28.dp))

        LiquidPrimaryButton(text = "Open Document", onClick = onView)

        Spacer(Modifier.weight(0.25f))

        LiquidPrimaryButton(text = "Unlock Another PDF", onClick = onExportAnother)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Back to Home", color = InkMuted)
        }
    }
}