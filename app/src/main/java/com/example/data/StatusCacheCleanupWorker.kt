package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import java.io.File

class StatusCacheCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "StatusCacheCleanup"
        const val WORK_NAME = "WhatsAppStatusCacheCleanupWork"

        fun schedule(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<StatusCacheCleanupWorker>(
                1, java.util.concurrent.TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun runOnce(context: Context) {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<StatusCacheCleanupWorker>()
                .build()
            androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting periodic WhatsApp status and cache cleanup task...")
        val context = applicationContext
        
        try {
            // 1. Clear database records and storage files older than 30 days
            val database = AppDatabase.getDatabase(context)
            val downloadDao = database.downloadDao()
            
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
            
            val whatsappDownloads = downloadDao.getDownloadsByPlatform("whatsapp")
            val oldDownloads = whatsappDownloads.filter { it.timestamp < thirtyDaysAgo }
            
            Log.i(TAG, "Found ${oldDownloads.size} WhatsApp downloads older than 30 days out of ${whatsappDownloads.size} total records.")
            
            var deletedFilesCount = 0
            for (download in oldDownloads) {
                try {
                    val path = download.filePath
                    if (path.startsWith("content://")) {
                        val deletedRows = context.contentResolver.delete(Uri.parse(path), null, null)
                        if (deletedRows > 0) {
                            deletedFilesCount++
                        }
                    } else {
                        val file = File(path)
                        if (file.exists() && file.delete()) {
                            deletedFilesCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete file for download: ${download.id} at path: ${download.filePath}", e)
                }
                
                // Delete the database record
                downloadDao.deleteDownload(download)
            }
            Log.i(TAG, "Successfully cleared $deletedFilesCount WhatsApp status files and updated the history database.")

            // 2. Clear general temporary app cache directories for files older than 30 days
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                val deletedCacheCount = deleteOldFiles(cacheDir, thirtyDaysAgo)
                Log.i(TAG, "Cleaned up $deletedCacheCount temporary cache files older than 30 days from cache directory.")
            }
            
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null && externalCacheDir.exists()) {
                val deletedExtCacheCount = deleteOldFiles(externalCacheDir, thirtyDaysAgo)
                Log.i(TAG, "Cleaned up $deletedExtCacheCount temporary cache files older than 30 days from external cache directory.")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error occurred during periodic cleanup", e)
            return Result.retry()
        }
    }

    private fun deleteOldFiles(directory: File, thresholdTime: Long): Int {
        var count = 0
        val files = directory.listFiles() ?: return count
        for (file in files) {
            if (file.isDirectory) {
                count += deleteOldFiles(file, thresholdTime)
                // Delete empty subdirectory
                if (file.listFiles()?.isEmpty() == true) {
                    file.delete()
                }
            } else if (file.isFile) {
                if (file.lastModified() < thresholdTime) {
                    if (file.delete()) {
                        count++
                    }
                }
            }
        }
        return count
    }
}
