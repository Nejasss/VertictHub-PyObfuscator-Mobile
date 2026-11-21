package com.verticthub.pyobfuscator.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

data class ImportedPythonFile(
    val filename: String,
    val source: String,
    val sizeBytes: Int,
    val encodingInfo: String,
    val lineEndingInfo: String,
)

class SafFileManager(private val context: Context) {
    fun readPythonOrZipFromUri(uri: Uri): Result<ImportedPythonFile> = runCatching {
        val displayName = queryDisplayName(uri) ?: "imported_script.py"
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Gagal membuka InputStream dari dokumen.")
        if (rawBytes.isEmpty()) throw IllegalArgumentException("File kosong (0 bytes).")
        if (displayName.endsWith(".zip", ignoreCase = true)) {
            extractFirstPythonFromZip(displayName, rawBytes)
        } else {
            decodeSourceBytes(displayName, rawBytes)
        }
    }

    private fun extractFirstPythonFromZip(zipName: String, zipBytes: ByteArray): ImportedPythonFile {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".py", ignoreCase = true)) {
                    return decodeSourceBytes(File(entry.name).name, zis.readBytes())
                }
                entry = zis.nextEntry
            }
        }
        throw IllegalArgumentException("Arsip ZIP '$zipName' tidak mengandung file .py.")
    }

    private fun decodeSourceBytes(filename: String, rawBytes: ByteArray): ImportedPythonFile {
        val hasUtf8Bom = rawBytes.size >= 3 && rawBytes[0] == 0xEF.toByte() && rawBytes[1] == 0xBB.toByte() && rawBytes[2] == 0xBF.toByte()
        val payload = if (hasUtf8Bom) rawBytes.copyOfRange(3, rawBytes.size) else rawBytes
        val text = String(payload, StandardCharsets.UTF_8)
        val hasCrlf = text.contains("\r\n")
        return ImportedPythonFile(
            filename = filename,
            source = text.replace("\r\n", "\n"),
            sizeBytes = rawBytes.size,
            encodingInfo = if (hasUtf8Bom) "UTF-8 (BOM stripped)" else "UTF-8",
            lineEndingInfo = if (hasCrlf) "CRLF -> LF" else "LF",
        )
    }

    fun writeTextToUri(uri: Uri, content: String): Result<Int> = runCatching {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: throw IllegalStateException("Gagal membuka OutputStream tujuan.")
        bytes.size
    }

    fun createShareIntent(filename: String, content: String, mimeType: String = "text/x-python"): Intent {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = filename.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val outFile = File(exportDir, safeName).apply { writeText(content, StandardCharsets.UTF_8) }
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, safeName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun clearTemporaryExports(): Long {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) return 0L
        var freed = 0L
        exportDir.listFiles()?.forEach { file -> freed += file.length(); file.delete() }
        return freed
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull()
}
