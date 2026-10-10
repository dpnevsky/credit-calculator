package com.example.creditcalculatormobile.feature.contract

import androidx.annotation.RequiresApi
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.creditcalculatormobile.data.model.DownloadedDocument
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentDownloadManager(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext

    suspend fun saveDocument(document: DownloadedDocument): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val resolvedFileName = document.fileName?.substringAfterLast('/')?.substringAfterLast('\\')
                ?.takeIf { it.isNotBlank() }
                ?: "credit-contract-${System.currentTimeMillis()}.pdf"

            val mimeType = document.mimeType ?: "application/pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(
                    fileName = resolvedFileName,
                    mimeType = mimeType,
                    bytes = document.bytes,
                )
            } else {
                saveToAppSpecificDownloads(
                    fileName = resolvedFileName,
                    bytes = document.bytes,
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): String {
        val resolver = appContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/CreditCalculatorMobile")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Не удалось создать запись в Downloads")

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(bytes)
            } ?: error("Не удалось открыть поток для сохранения файла")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (exception: Exception) {
            resolver.delete(uri, null, null)
            throw exception
        }

        return uri.toString()
    }

    private fun saveToAppSpecificDownloads(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: error("Не удалось получить директорию Downloads")

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val targetFile = File(downloadsDir, fileName)
        FileOutputStream(targetFile).use { output ->
            output.write(bytes)
            output.flush()
        }

        return targetFile.absolutePath
    }
}
