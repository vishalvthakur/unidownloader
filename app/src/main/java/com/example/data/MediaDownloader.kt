package com.example.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit

object MediaDownloader {
    private const val TAG = "MediaDownloader"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    sealed class DownloadResult {
        data class Success(val filePath: String, val fileName: String, val mediaType: String) : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

    private suspend fun fetchActiveInstances(): List<String> = withContext(Dispatchers.IO) {
        val defaultInstances = listOf(
            "https://api.cobalt.tools/",
            "https://cobalt.api.ryb.me/",
            "https://cobalt.chunky.club/",
            "https://api.cobalt.sh/",
            "https://cobalt-api.kuss.pub/"
        )
        try {
            val request = Request.Builder()
                .url("https://cobalt.directory/api/instances")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val jsonArray = org.json.JSONArray(body)
                        val list = mutableListOf<String>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val url = obj.optString("url", "")
                            val isUp = obj.optBoolean("up", true)
                            // Clean slash
                            if (url.isNotEmpty() && isUp) {
                                val cleanUrl = if (url.endsWith("/")) url else "$url/"
                                list.add(cleanUrl)
                            }
                        }
                        if (list.isNotEmpty()) return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch dynamic instances from cobalt.directory, using defaults", e)
        }
        return@withContext defaultInstances
    }

    /**
     * Extracts direct stream URL from Cobalt API and downloads it to the public gallery.
     */
    suspend fun downloadMedia(
        context: Context,
        url: String,
        repository: DownloadRepository,
        preferredEngineUrl: String = "auto",
        onProgress: (Int) -> Unit = {}
    ): DownloadResult = withContext(Dispatchers.IO) {
        // Resolve list of base instances to try
        val instances = mutableListOf<String>()
        if (preferredEngineUrl != "auto" && preferredEngineUrl.isNotEmpty()) {
            val cleanPref = if (preferredEngineUrl.endsWith("/")) preferredEngineUrl else "$preferredEngineUrl/"
            instances.add(cleanPref)
        }
        
        // Add dynamic/default fallback instances to the pool
        val fetched = fetchActiveInstances()
        for (f in fetched) {
            if (!instances.contains(f)) {
                instances.add(f)
            }
        }

        var lastError = "No working Cobalt server could process this download request."
        
        for (baseUrl in instances) {
            Log.d(TAG, "Attempting download using server: $baseUrl")
            
            // Try different payload formats for the current instance (from strict to compatible)
            for (payloadAttempt in 0..2) {
                val result = makeSingleRequest(context, baseUrl, url, repository, payloadAttempt, onProgress)
                when (result) {
                    is DownloadResult.Success -> {
                        return@withContext result
                    }
                    is DownloadResult.Error -> {
                        lastError = result.message
                        // If the error is NOT a 400 bad request, don't waste time with other payload attempts on this server; try next server
                        if (!result.message.contains("HTTP 400") && !result.message.contains("Server error: HTTP 400")) {
                            break
                        }
                    }
                }
            }
        }
        
        return@withContext DownloadResult.Error(lastError)
    }

    private suspend fun makeSingleRequest(
        context: Context,
        baseUrl: String,
        url: String,
        repository: DownloadRepository,
        payloadAttempt: Int,
        onProgress: (Int) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            val jsonBody = org.json.JSONObject().apply {
                put("url", url)
                when (payloadAttempt) {
                    0 -> {
                        // Classic v7 standard
                        put("videoQuality", "max")
                        put("downloadMode", "auto")
                    }
                    1 -> {
                        // Modern v10 standard
                        put("vQuality", "max")
                    }
                    2 -> {
                        // Absolute minimal parameters (max compatibility)
                    }
                }
            }

            val request = Request.Builder()
                .url(baseUrl)
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Server $baseUrl (attempt $payloadAttempt) returned error ${response.code}: $responseBodyStr")
                    
                    var errorDetails = "HTTP ${response.code}"
                    try {
                        val errJson = org.json.JSONObject(responseBodyStr)
                        val errText = errJson.optString("text", "")
                        if (errText.isNotEmpty()) {
                            errorDetails = errText
                        } else {
                            val errMsg = errJson.optString("message", "")
                            if (errMsg.isNotEmpty()) errorDetails = errMsg
                        }
                    } catch (_: Exception) {}
                    
                    return@withContext DownloadResult.Error("Server error: HTTP ${response.code} ($errorDetails)")
                }

                val jsonResponse = org.json.JSONObject(responseBodyStr)
                val status = jsonResponse.optString("status", "error")
                if (status == "error") {
                    val errorText = jsonResponse.optString("text", "Unknown API error")
                    return@withContext DownloadResult.Error("Server error text: $errorText")
                }

                when (status) {
                    "stream", "redirect" -> {
                        val streamUrl = jsonResponse.getString("url")
                        val filename = jsonResponse.optString("filename", "download_${System.currentTimeMillis()}")
                        return@withContext downloadDirectFile(context, streamUrl, filename, url, repository, onProgress)
                    }
                    "picker" -> {
                        val pickerArray = jsonResponse.optJSONArray("picker")
                        if (pickerArray != null && pickerArray.length() > 0) {
                            var firstSuccess: DownloadResult? = null
                            for (i in 0 until pickerArray.length()) {
                                val item = pickerArray.getJSONObject(i)
                                val itemUrl = item.getString("url")
                                val type = item.optString("type", "photo")
                                val ext = if (type == "video") "mp4" else "jpg"
                                val filename = "slide_${i + 1}_${System.currentTimeMillis()}.$ext"
                                val res = downloadDirectFile(context, itemUrl, filename, url, repository, onProgress)
                                if (i == 0) firstSuccess = res
                            }
                            return@withContext firstSuccess ?: DownloadResult.Error("Failed to download picker slides")
                        } else {
                            return@withContext DownloadResult.Error("Picker mode returned empty list")
                        }
                    }
                    else -> {
                        return@withContext DownloadResult.Error("Unsupported Cobalt response status: $status")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in makeSingleRequest on $baseUrl (attempt $payloadAttempt)", e)
            return@withContext DownloadResult.Error(e.localizedMessage ?: "Network communication error on $baseUrl")
        }
    }

    private suspend fun downloadDirectFile(
        context: Context,
        fileUrl: String,
        suggestedFilename: String,
        originalUrl: String,
        repository: DownloadRepository,
        onProgress: (Int) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading direct stream URL: $fileUrl")
            val request = Request.Builder().url(fileUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadResult.Error("Failed to fetch stream content: ${response.code}")
                }

                val responseBody = response.body ?: return@withContext DownloadResult.Error("Direct stream was empty")
                val totalBytes = responseBody.contentLength()
                val inputStream = responseBody.byteStream()

                // Deduce content format
                val contentType = responseBody.contentType()?.toString() ?: ""
                val isVideo = contentType.contains("video", ignoreCase = true) || suggestedFilename.endsWith(".mp4", ignoreCase = true)
                val isAudio = contentType.contains("audio", ignoreCase = true) || suggestedFilename.endsWith(".mp3", ignoreCase = true)
                val mediaCategory = when {
                    isVideo -> "video"
                    isAudio -> "audio"
                    else -> "image"
                }

                // Determine appropriate gallery path
                val ext = when (mediaCategory) {
                    "video" -> "mp4"
                    "audio" -> "mp3"
                    else -> "jpg"
                }

                val cleanFilename = if (suggestedFilename.contains(".")) {
                    suggestedFilename
                } else {
                    "$suggestedFilename.$ext"
                }

                val savedUri = saveToMediaStore(context, inputStream, cleanFilename, mediaCategory, totalBytes, onProgress)
                    ?: return@withContext DownloadResult.Error("Failed to write file to storage")

                val actualPath = savedUri.toString()
                
                // Deduce Platform
                val platform = when {
                    originalUrl.contains("youtube", ignoreCase = true) || originalUrl.contains("youtu.be", ignoreCase = true) -> "youtube"
                    originalUrl.contains("instagram", ignoreCase = true) -> "instagram"
                    else -> "other"
                }

                // Save to Room DB
                val downloadRecord = DownloadEntity(
                    title = cleanFilename,
                    url = originalUrl,
                    filePath = actualPath,
                    fileSize = if (totalBytes > 0) totalBytes else 0L,
                    mediaType = mediaCategory,
                    platform = platform
                )
                repository.insertDownload(downloadRecord)

                return@withContext DownloadResult.Success(actualPath, cleanFilename, mediaCategory)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inside direct file downloader", e)
            return@withContext DownloadResult.Error("Failed to download file payload: ${e.localizedMessage}")
        }
    }

    private fun saveToMediaStore(
        context: Context,
        inputStream: InputStream,
        filename: String,
        mediaCategory: String,
        totalBytes: Long,
        onProgress: (Int) -> Unit
    ): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.TITLE, filename)
            
            when (mediaCategory) {
                "video" -> {
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/UniversalDownloader")
                    }
                }
                "audio" -> {
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/UniversalDownloader")
                    }
                }
                else -> {
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/UniversalDownloader")
                    }
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collectionUri = when (mediaCategory) {
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collectionUri, contentValues) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (totalBytes > 0) {
                        val progress = ((totalBytesRead * 100) / totalBytes).toInt()
                        onProgress(progress)
                    }
                }
                outputStream.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } else {
                // On older versions, scan file
                val path = getRealPathFromURI(context, uri)
                if (path != null) {
                    MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
                }
            }
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bytes to MediaStore", e)
            resolver.delete(uri, null, null)
            return null
        }
    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor = context.contentResolver.query(contentUri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(MediaStore.MediaColumns.DATA)
                if (idx > -1) it.getString(idx) else null
            } else null
        }
    }
}
