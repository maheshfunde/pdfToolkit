package com.yashodatech.pdftoolkit

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.theme.*


@Composable
fun RateAppDialog(
    onDismiss: () -> Unit,
    onRated: () -> Unit
) {
    val context = LocalContext.current
    val packageName = context.packageName

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                // ── App Icon ────────────────────────
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.horizontalGradient(ButtonSapphireGradient),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Stars ───────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(5) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Enjoying PDF Toolkit?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "If you love our app, please take a moment to rate it on the Play Store. " +
                            "Your feedback helps us improve!",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(24.dp))

                // ── Rate Now Button ─────────────────
                Button(
                    onClick = {
                        // ✅ Try Play Store app first, fallback to browser
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$packageName")
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                            )
                            context.startActivity(intent)
                        }
                        onRated()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(ButtonSapphireGradient),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Star,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Rate 5 Stars",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Share Button ────────────────────
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "PDF Toolkit")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "📄 Check out PDF Toolkit! " +
                                        "Merge, Split, Compress PDFs & Convert Images to PDF.\n\n" +
                                        "Download: https://play.google.com/store/apps/details?id=$packageName"
                            )
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share via")
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Vermilion
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Share with Friends",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Maybe Later Button ──────────────
                TextButton(onClick = onDismiss) {
                    Text(
                        "Maybe Later",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // ── Never Ask Button ────────────────
                TextButton(onClick = onRated) {
                    Text(
                        "Don't Ask Again",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = { }
    )
}