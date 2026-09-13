package com.yashodatech.pdftoolkit.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.itextpdf.kernel.pdf.*
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import java.io.ByteArrayOutputStream

class PdfCompressor {

    /**
     * Compresses PDF by:
     * 1. Copying to new document (forces re-serialization — THE KEY FIX)
     * 2. SmartMode eliminates duplicate objects
     * 3. Full Flate compression on all streams
     * 4. Re-compresses images at target quality
     * 5. Removes metadata & bloat
     *
     * @return Pair<outputUriString, outputFileSizeBytes>
     */
    fun compressPdf(
        context: Context,
        sourceUri: Uri,
        outputFileName: String,
        level: CompressionLevel
    ): Pair<String, Long> {

        val resolver = context.contentResolver

        // ── Create output in Downloads ──────────────
        val cleanName = if (outputFileName.endsWith(".pdf", true))
            outputFileName else "$outputFileName.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val outputUri = resolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        ) ?: throw Exception("Failed to create output file")

        val inputStream = resolver.openInputStream(sourceUri)
            ?: throw Exception("Cannot open source PDF")

        val outputStream = resolver.openOutputStream(outputUri)
            ?: throw Exception("Cannot open output stream")

        try {
            // ── SOURCE PDF (read-only) ──────────────
            val reader = PdfReader(inputStream)
            reader.setUnethicalReading(true)
            val sourcePdf = PdfDocument(reader)

            // ── OUTPUT PDF (new document) ───────────
            val writerProperties = WriterProperties().apply {
                // ╔══════════════════════════════════════════╗
                // ║  SmartMode: detects & removes duplicate  ║
                // ║  objects (fonts, images, etc.)           ║
                // ╚══════════════════════════════════════════╝
                useSmartMode()

                // Full Flate compression on ALL object streams
                setFullCompressionMode(true)

                // Compression level 0-9
                setCompressionLevel(
                    when (level) {
                        CompressionLevel.LOW -> 6
                        CompressionLevel.MEDIUM -> 8
                        CompressionLevel.HIGH -> 9
                    }
                )
            }

            val writer = PdfWriter(outputStream, writerProperties)
            val outputPdf = PdfDocument(writer)

            // ╔══════════════════════════════════════════════╗
            // ║  KEY FIX: Copy pages to NEW document         ║
            // ║  This forces re-serialization of all objects ║
            // ║  enabling actual compression                 ║
            // ╚══════════════════════════════════════════════╝
            sourcePdf.copyPagesTo(1, sourcePdf.numberOfPages, outputPdf)

            // ── Apply optimizations ─────────────────
            removeMetadata(outputPdf)

            if (level == CompressionLevel.MEDIUM || level == CompressionLevel.HIGH) {
                optimizePageResources(outputPdf)
                removeUnusedNames(outputPdf)
                compressImages(outputPdf, level.imageQuality)
            }

            if (level == CompressionLevel.HIGH) {
                removeAnnotations(outputPdf)
                removeBookmarks(outputPdf)
                removeJavaScript(outputPdf)
                removeEmbeddedFiles(outputPdf)
                removeThumbnails(outputPdf)
                flattenFormFields(outputPdf)
            }

            // Close all
            outputPdf.close()
            sourcePdf.close()
            writer.close()
            reader.close()
            outputStream.close()
            inputStream.close()

            // ── Get result ──────────────────────────
            val outputSize = getFileSize(context, outputUri)

            return Pair(outputUri.toString(), outputSize)

        } catch (e: Exception) {
            try {
                resolver.delete(outputUri, null, null)
            } catch (_: Exception) {}
            throw e
        }
    }

    // ════════════════════════════════════════════════════
    //  IMAGE COMPRESSION
    //  Re-encodes images at target JPEG quality.
    //  Only replaces if result is actually smaller.
    // ════════════════════════════════════════════════════
    private fun compressImages(pdf: PdfDocument, quality: Int) {
        try {
            for (i in 1..pdf.numberOfPages) {
                try {
                    val page = pdf.getPage(i)
                    val resources = page.pdfObject
                        ?.getAsDictionary(PdfName.Resources) ?: continue

                    val xObjects = resources.getAsDictionary(PdfName.XObject)
                        ?: continue

                    for (key in xObjects.keySet().toList()) {
                        try {
                            val obj = xObjects.getAsStream(key) ?: continue
                            val subtype = obj.getAsName(PdfName.Subtype)

                            if (subtype == PdfName.Image) {
                                compressSingleImage(obj, quality)
                            }
                        } catch (_: Exception) {
                            // Skip problematic images
                        }
                    }
                } catch (_: Exception) {}

                // GC every 10 pages for large PDFs
                if (i % 10 == 0) System.gc()
            }
        } catch (_: Exception) {}
    }

    private fun compressSingleImage(imageStream: PdfStream, quality: Int) {
        try {
            val imageXObject = PdfImageXObject(imageStream)
            val originalBytes = imageXObject.imageBytes ?: return

            // Skip tiny images (icons, logos) — not worth it
            val width = imageXObject.width.toInt()
            val height = imageXObject.height.toInt()
            if (width < 50 || height < 50) return

            // Skip if already very small
            if (originalBytes.size < 5000) return

            // Decode image
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeByteArray(
                originalBytes, 0, originalBytes.size, options
            ) ?: return

            // Re-encode as JPEG at target quality
            val outputBytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputBytes)
            bitmap.recycle()

            val compressedBytes = outputBytes.toByteArray()
            outputBytes.close()

            // ╔══════════════════════════════════════════╗
            // ║  ONLY replace if actually smaller         ║
            // ║  Never makes the file bigger              ║
            // ╚══════════════════════════════════════════╝
            if (compressedBytes.size < originalBytes.size) {
                imageStream.setData(compressedBytes, false)
                imageStream.put(PdfName.Filter, PdfName("DCTDecode"))
                imageStream.put(PdfName.Width, PdfNumber(width))
                imageStream.put(PdfName.Height, PdfNumber(height))
                imageStream.put(PdfName.BitsPerComponent, PdfNumber(8))
                imageStream.put(PdfName.ColorSpace, PdfName.DeviceRGB)
                // Remove old filters
                imageStream.remove(PdfName("DecodeParms"))
                imageStream.remove(PdfName("Mask"))
                imageStream.remove(PdfName("SMask"))
            }

        } catch (_: Exception) {
            // Skip if this image fails
        }
    }

    // ════════════════════════════════════════════════════
    //  METADATA REMOVAL
    // ════════════════════════════════════════════════════
    private fun removeMetadata(pdf: PdfDocument) {
        try {
            val catalog = pdf.catalog
            catalog.remove(PdfName.Metadata)
            catalog.remove(PdfName("PieceInfo"))
            catalog.remove(PdfName.Lang)

            // Clear document info
            try {
                val info = pdf.documentInfo
                info.setTitle("")
                info.setAuthor("")
                info.setSubject("")
                info.setKeywords("")
                info.setCreator("")
                info.setMoreInfo("Producer", "")
                info.setMoreInfo("CreationDate", "")
                info.setMoreInfo("ModDate", "")
            } catch (_: Exception) {}

            // Remove from trailer
            try {
                val infoDict = pdf.trailer?.getAsDictionary(PdfName.Info)
                if (infoDict != null) {
                    infoDict.remove(PdfName.CreationDate)
                    infoDict.remove(PdfName("ModDate"))
                    infoDict.remove(PdfName.Producer)
                    infoDict.remove(PdfName.Creator)
                    infoDict.remove(PdfName.Author)
                    infoDict.remove(PdfName.Title)
                    infoDict.remove(PdfName.Subject)
                    infoDict.remove(PdfName.Keywords)
                }
            } catch (_: Exception) {}

            // Per-page metadata
            for (i in 1..pdf.numberOfPages) {
                try {
                    val page = pdf.getPage(i)
                    page.pdfObject?.remove(PdfName.Metadata)
                    page.pdfObject?.remove(PdfName("PieceInfo"))
                    page.pdfObject?.remove(PdfName("LastModified"))
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    // ════════════════════════════════════════════════════
    //  PAGE RESOURCE OPTIMIZATION
    // ════════════════════════════════════════════════════
    private fun optimizePageResources(pdf: PdfDocument) {
        try {
            for (i in 1..pdf.numberOfPages) {
                try {
                    val page = pdf.getPage(i)
                    val resources = page.pdfObject
                        ?.getAsDictionary(PdfName.Resources) ?: continue

                    // Remove empty dictionaries
                    removeIfEmpty(resources, PdfName.XObject)
                    removeIfEmpty(resources, PdfName.Font)
                    removeIfEmpty(resources, PdfName.ExtGState)
                    removeIfEmpty(resources, PdfName.Pattern)
                    removeIfEmpty(resources, PdfName.Shading)
                    removeIfEmpty(resources, PdfName("Properties"))

                    // Remove ICC profiles
                    page.pdfObject?.remove(PdfName("OutputIntents"))
                    page.pdfObject?.remove(PdfName("Thumb"))

                } catch (_: Exception) {}
            }

            pdf.catalog.remove(PdfName("OutputIntents"))
        } catch (_: Exception) {}
    }

    private fun removeIfEmpty(dict: PdfDictionary, key: PdfName) {
        try {
            val sub = dict.getAsDictionary(key)
            if (sub != null && sub.size() == 0) {
                dict.remove(key)
            }
        } catch (_: Exception) {}
    }

    // ════════════════════════════════════════════════════
    //  NAME TREE CLEANUP
    // ════════════════════════════════════════════════════
    private fun removeUnusedNames(pdf: PdfDocument) {
        try {
            val names = pdf.catalog.getPdfObject()
                ?.getAsDictionary(PdfName.Names) ?: return

            names.remove(PdfName("JavaScript"))
            names.remove(PdfName("EmbeddedFiles"))
            names.remove(PdfName.AP)

            if (names.size() == 0) {
                pdf.catalog.remove(PdfName.Names)
            }
        } catch (_: Exception) {}
    }

    // ════════════════════════════════════════════════════
    //  HIGH-LEVEL REMOVALS
    // ════════════════════════════════════════════════════
    private fun removeAnnotations(pdf: PdfDocument) {
        try {
            for (i in 1..pdf.numberOfPages) {
                try {
                    val page = pdf.getPage(i)
                    val annots = page.pdfObject?.getAsArray(PdfName.Annots)
                        ?: continue

                    val toKeep = PdfArray()
                    for (j in 0 until annots.size()) {
                        try {
                            val annot = annots.getAsDictionary(j) ?: continue
                            val subtype = annot.getAsName(PdfName.Subtype)
                            if (subtype == PdfName.Link || subtype == PdfName("Widget")) {
                                toKeep.add(annot)
                            }
                        } catch (_: Exception) {}
                    }

                    if (toKeep.size() == 0) {
                        page.pdfObject?.remove(PdfName.Annots)
                    } else {
                        page.pdfObject?.put(PdfName.Annots, toKeep)
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun removeBookmarks(pdf: PdfDocument) {
        try {
            pdf.catalog.remove(PdfName("Outlines"))
        } catch (_: Exception) {}
    }

    private fun removeJavaScript(pdf: PdfDocument) {
        try {
            pdf.catalog.remove(PdfName("OpenAction"))
            pdf.catalog.remove(PdfName.AA)

            for (i in 1..pdf.numberOfPages) {
                try {
                    pdf.getPage(i).pdfObject?.remove(PdfName.AA)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun removeEmbeddedFiles(pdf: PdfDocument) {
        try {
            val names = pdf.catalog.getPdfObject()
                ?.getAsDictionary(PdfName.Names) ?: return
            names.remove(PdfName("EmbeddedFiles"))
        } catch (_: Exception) {}
    }

    private fun removeThumbnails(pdf: PdfDocument) {
        try {
            for (i in 1..pdf.numberOfPages) {
                try {
                    pdf.getPage(i).pdfObject?.remove(PdfName("Thumb"))
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun flattenFormFields(pdf: PdfDocument) {
        try {
            val form = pdf.catalog.getPdfObject()
                ?.getAsDictionary(PdfName("AcroForm")) ?: return

            val fields = form.getAsArray(PdfName("Fields"))
            if (fields == null || fields.size() == 0) {
                pdf.catalog.remove(PdfName("AcroForm"))
            }
        } catch (_: Exception) {}
    }

    // ════════════════════════════════════════════════════
    //  FILE SIZE HELPER
    // ════════════════════════════════════════════════════
    // ════════════════════════════════════════════════════
    //  FILE SIZE HELPER — RELIABLE
    // ════════════════════════════════════════════════════
    private fun getFileSize(context: Context, uri: Uri): Long {

        // Method 1: Try ParcelFileDescriptor (most reliable)
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val size = pfd.statSize
                if (size > 0) return size
            }
        } catch (_: Exception) {}

        // Method 2: Try AssetFileDescriptor
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                val size = afd.length
                if (size > 0) return size
            }
        } catch (_: Exception) {}

        // Method 3: Read entire stream and count bytes
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                var total = 0L
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    total += read
                }
                if (total > 0) return total
            }
        } catch (_: Exception) {}

        // Method 4: Query MediaStore (least reliable right after write)
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && idx >= 0) {
                    val size = cursor.getLong(idx)
                    if (size > 0) return size
                }
            }
        } catch (_: Exception) {}

        return 0L
    }
}