package com.yashodatech.pdftoolkit.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream

object FileHelper {

    // ═══════════════════════════════════════════
    // Save PDF to Downloads folder
    // ═══════════════════════════════════════════
    fun savePdfToDownloads(
        context: Context,
        tempFile: File,
        fileName: String
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ → Use MediaStore
                savePdfMediaStore(context, tempFile, fileName)
            } else {
                // Android 9 and below → Use legacy storage
                savePdfLegacy(context, tempFile, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ── Android 10+ (MediaStore) ────────────────
    private fun savePdfMediaStore(
        context: Context,
        tempFile: File,
        fileName: String
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${fileName}.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/PDFToolkit"
            )
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        )

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                FileInputStream(tempFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        // Delete temp file
        tempFile.delete()

        return uri
    }

    // ── Android 9 and below (Legacy) ────────────
    private fun savePdfLegacy(
        context: Context,
        tempFile: File,
        fileName: String
    ): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val pdfToolkitDir = File(downloadsDir, "PDFToolkit").apply { mkdirs() }
        val outputFile = File(pdfToolkitDir, "${fileName}.pdf")

        tempFile.copyTo(outputFile, overwrite = true)
        tempFile.delete()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            outputFile
        )
    }

    // ═══════════════════════════════════════════
    // Open PDF
    // ═══════════════════════════════════════════
    fun openPdf(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No PDF viewer app found. Install one from Play Store.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ═══════════════════════════════════════════
    // Share PDF
    // ═══════════════════════════════════════════
    fun sharePdf(context: Context, uri: Uri, fileName: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "Share PDF")
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share file", Toast.LENGTH_SHORT).show()
        }
    }

    // ═══════════════════════════════════════════
    // Get File URI (for FileProvider)
    // ═══════════════════════════════════════════
    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }
}