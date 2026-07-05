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
    
    private val apiClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    sealed class DownloadResult {
        data class Success(val filePath: String, val fileName: String, val mediaType: String) : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

    private suspend fun fetchActiveInstances(): List<String> = withContext(Dispatchers.IO) {
        val defaultInstances = listOf(
            "https://dog.kittycat.boo/",
            "https://api.cobalt.liubquanti.click/",
            "https://rue-cobalt.xenon.zone/",
            "https://cobaltapi.cjs.nz/"
        )
        try {
            val request = Request.Builder()
                .url("https://cobalt.directory/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .get()
                .build()
            apiClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string()
                    if (!html.isNullOrEmpty()) {
                        // Regex to match "api":"something.domain.ext" or api:"something.domain.ext"
                        val regex = """(?:api["']?\s*:\s*["'])([a-zA-Z0-9.-]+)(?:["'])""".toRegex()
                        val matches = regex.findAll(html)
                        val list = mutableListOf<String>()
                        
                        // Extract unique matching domains
                        val uniqueHosts = matches.map { it.groupValues[1] }
                            .distinct()
                            .filter { host ->
                                if (host.isEmpty() || !host.contains(".")) return@filter false
                                if (host.contains("cloudflare", ignoreCase = true) ||
                                    host.contains("plausible", ignoreCase = true) ||
                                    host.contains("google", ignoreCase = true) ||
                                    host.contains("br0k3.me", ignoreCase = true)) { // often offline
                                    return@filter false
                                }
                                
                                // Check if this host is explicitly offline in the HTML context
                                val hostIndex = html.indexOf(host)
                                if (hostIndex != -1) {
                                    val start = (hostIndex - 150).coerceAtLeast(0)
                                    val end = (hostIndex + 150).coerceAtMost(html.length)
                                    val contextText = html.substring(start, end)
                                    if (contextText.contains("online:false") || 
                                        contextText.contains("\"online\":false") || 
                                        contextText.contains("\"online\": false") ||
                                        contextText.contains("online: false")) {
                                        Log.d(TAG, "Skipping offline host from cobalt.directory: $host")
                                        return@filter false
                                    }
                                }
                                true
                            }
                            .toList()

                        for (host in uniqueHosts) {
                            val url = "https://$host/"
                            if (!list.contains(url)) {
                                list.add(url)
                            }
                        }
                        
                        if (list.isNotEmpty()) {
                            Log.d(TAG, "Successfully extracted ${list.size} dynamic instances from cobalt.directory: $list")
                            // Prioritize our known tested working endpoints at the very front of the list,
                            // but append the others so we have massive fallbacks!
                            val combined = mutableListOf<String>()
                            for (def in defaultInstances) {
                                combined.add(def)
                            }
                            for (item in list) {
                                if (!combined.contains(item)) {
                                    combined.add(item)
                                }
                            }
                            return@withContext combined
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dynamically scrape cobalt.directory homepage, falling back to defaults", e)
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
        onProgress: (progress: Int, bytesRead: Long, totalBytes: Long) -> Unit = { _, _, _ -> }
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
        onProgress: (progress: Int, bytesRead: Long, totalBytes: Long) -> Unit
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
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Origin", "https://cobalt.tools")
                .header("Referer", "https://cobalt.tools/")
                .build()

            apiClient.newCall(request).execute().use { response ->
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
        onProgress: (progress: Int, bytesRead: Long, totalBytes: Long) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading direct stream URL: $fileUrl")
            val request = Request.Builder()
                .url(fileUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            downloadClient.newCall(request).execute().use { response ->
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
        onProgress: (progress: Int, bytesRead: Long, totalBytes: Long) -> Unit
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
                var lastProgress = -1
                var lastBytesUpdated = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    val progress = if (totalBytes > 0) {
                        ((totalBytesRead * 100) / totalBytes).toInt()
                    } else {
                        -1
                    }
                    if (totalBytes > 0) {
                        if (progress != lastProgress) {
                            lastProgress = progress
                            onProgress(progress, totalBytesRead, totalBytes)
                        }
                    } else {
                        // Update every 512 KB when content length is unknown
                        if (totalBytesRead - lastBytesUpdated >= 512 * 1024) {
                            lastBytesUpdated = totalBytesRead
                            onProgress(-1, totalBytesRead, totalBytes)
                        }
                    }
                }
                // Send final progress update
                val finalProgress = if (totalBytes > 0) 100 else -1
                onProgress(finalProgress, totalBytesRead, totalBytes)
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
