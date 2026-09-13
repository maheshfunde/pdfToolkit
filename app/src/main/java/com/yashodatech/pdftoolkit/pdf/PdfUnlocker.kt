package com.yashodatech.pdftoolkit.pdf

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.kernel.exceptions.BadPasswordException
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.ReaderProperties
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Removes a password (and any copy/print restrictions) from a PDF.
 *
 * iText removes encryption automatically: open the [PdfReader] with the correct
 * password and write to a [PdfWriter] that declares no encryption — the output is
 * the same document, fully unlocked.
 */
class PdfUnlocker(private val context: Context) {

    /** What kind of lock (if any) a PDF carries. */
    enum class LockState { NOT_PROTECTED, USER_PASSWORD, PERMISSIONS_ONLY }

    /**
     * Determines the lock on [sourceUri].
     *  - NOT_PROTECTED  → opens with an empty password and is not encrypted.
     *  - PERMISSIONS_ONLY → encrypted only to restrict copy/print (no user password).
     *  - USER_PASSWORD  → a user password is required to open it.
     */
    suspend fun probeLock(sourceUri: Uri): LockState = withContext(Dispatchers.IO) {
        val src = copyToTemp(sourceUri)
        try {
            probeLockFile(src, null)
        } finally {
            src.delete()
        }
    }

    private fun probeLockFile(src: File, password: String?): LockState {
        val props = ReaderProperties()
        if (!password.isNullOrEmpty()) props.setPassword(password.toByteArray())
        return try {
            PdfReader(src.absolutePath, props).use { reader ->
                if (reader.isEncrypted) LockState.PERMISSIONS_ONLY
                else LockState.NOT_PROTECTED
            }
        } catch (_: BadPasswordException) {
            LockState.USER_PASSWORD
        } catch (_: Exception) {
            LockState.USER_PASSWORD
        }
    }

    /**
     * Unlocks [sourceUri]. [password] is required when the PDF has a user password;
     * passing a wrong password throws [BadPasswordException] and a null/blank one is
     * only allowed for PERMISSIONS_ONLY / NOT_PROTECTED files. Writes
     * [baseName]_unlocked.pdf to Downloads and returns its URI.
     */
    suspend fun unlockPdf(
        sourceUri: Uri,
        baseName: String,
        password: String?,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): String = withContext(Dispatchers.IO) {
        val src = copyToTemp(sourceUri)
        val outFile = File.createTempFile("unlock_out_", ".pdf", context.cacheDir)
        try {
            if (probeLockFile(src, null) == LockState.USER_PASSWORD && password.isNullOrEmpty()) {
                throw IOException("This PDF requires a password")
            }
            val props = ReaderProperties()
            if (!password.isNullOrEmpty()) props.setPassword(password.toByteArray())

            PdfReader(src.absolutePath, props).use { reader ->
                PdfDocument(reader, PdfWriter(FileOutputStream(outFile))).use { pdf ->
                    // Touch every page so the whole document is carried over and we
                    // can report progress; closing the document then writes it out
                    // fully decrypted.
                    val total = pdf.numberOfPages
                    for (i in 1..total) {
                        pdf.getPage(i)
                        onProgress(i, total)
                    }
                }
            }

            val cleanBase = baseName.removeSuffix(".pdf")
                .substringBeforeLast(".")
                .ifBlank { "document" }
            copyToDownloads(outFile, "${cleanBase}_unlocked.pdf", context)
        } finally {
            src.delete()
            outFile.delete()
        }
    }

    private fun copyToTemp(sourceUri: Uri): File {
        val f = File.createTempFile("unlock_src_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(sourceUri)?.use { i ->
            f.outputStream().use { i.copyTo(it) }
        }
        return f
    }

    private companion object {
        fun copyToDownloads(source: File, name: String, context: Context): String {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Files.getContentUri("external"), values
            ) ?: throw IOException("Failed to create output file")
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: throw IOException("Cannot open output stream")
            return uri.toString()
        }
    }
}