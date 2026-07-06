package com.bearbones.kumaflow.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float, val downloadedMB: Float, val totalMB: Float) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object UpdateManager {

    private const val FILENAME = "KumaFlow_Update.apk"

    fun downloadApk(context: Context, urlString: String): Flow<DownloadState> = flow {
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FILENAME)
        
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                emit(DownloadState.Error("Server error: ${connection.responseCode}"))
                return@flow
            }

            val totalBytes = connection.contentLength
            val inputStream = connection.inputStream

            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val outputStream = FileOutputStream(destinationFile)
            val buffer = ByteArray(8 * 1024)
            var downloadedBytes = 0L
            var bytesRead: Int

            emit(DownloadState.Downloading(0f, 0f, totalBytes.toFloat() / (1024 * 1024)))

            var lastEmitTime = System.currentTimeMillis()

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!currentCoroutineContext().isActive) {
                            break
                        }
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        if (totalBytes > 0) {
                            // Artificial delay to make download take ~1 minute for cool visual progress
                            val delayMs = (60000L * bytesRead) / totalBytes
                            if (delayMs > 0) {
                                kotlinx.coroutines.delay(delayMs)
                            }

                            val currentTime = System.currentTimeMillis()
                            // Batasi emit progress agar tidak terlalu membebani UI (misal tiap 50ms)
                            if (currentTime - lastEmitTime > 50 || downloadedBytes == totalBytes.toLong()) {
                                val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                val downloadedMB = downloadedBytes.toFloat() / (1024 * 1024)
                                val totalMB = totalBytes.toFloat() / (1024 * 1024)
                                emit(DownloadState.Downloading(progress, downloadedMB, totalMB))
                                lastEmitTime = currentTime
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (!currentCoroutineContext().isActive) {
                if (destinationFile.exists()) destinationFile.delete()
                return@flow
            }

            emit(DownloadState.Success(destinationFile))

        } catch (e: CancellationException) {
            if (destinationFile.exists()) destinationFile.delete()
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            if (destinationFile.exists()) destinationFile.delete()
            emit(DownloadState.Error(e.message ?: "Gagal terhubung ke server"))
        }
    }.flowOn(Dispatchers.IO)

    fun installApk(context: Context, file: File) {
        try {
            if (file.exists()) {
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val install = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(install)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
