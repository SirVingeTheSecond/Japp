package com.japp.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.japp.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AttachmentDownloadHelper {

    /**
     * Downloads an attachment and opens it .
     * Returns true if successful, false otherwise.
     */
    suspend fun downloadAndOpen(
        context: Context,
        attachmentId: Int,
        fileName: String,
        mimeType: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.attachmentService.downloadAttachment(attachmentId)

            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure(
                    Exception("Failed to download attachment: ${response.code()}")
                )
            }

            val downloadsDir = File(context.cacheDir, "downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)

            response.body()!!.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            withContext(Dispatchers.Main) {
                openFile(context, file, mimeType)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Opens a file with the appropriate app using FileProvider.
     */
    private fun openFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if app can handle this (maybe just use generic viewer?)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback: try generic viewer
                val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
            }
        } catch (_: Exception) {
            // If opening fails we simply ignore it as file is downloaded?
        }
    }

    /**
     * Formats file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}