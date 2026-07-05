package com.example.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class DownloadService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: DownloadRepository

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        const val EXTRA_URL = "EXTRA_URL"

        fun startDownload(context: Context, url: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_URL, url)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = DownloadRepository(database.downloadDao())
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_DOWNLOAD) {
            val url = intent.getStringExtra(EXTRA_URL)
            if (!url.isNullOrEmpty()) {
                startForeground(NOTIFICATION_ID, buildProgressNotification(0))
                performDownload(url)
            } else {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun performDownload(url: String) {
        serviceScope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Starting original quality download...", Toast.LENGTH_SHORT).show()
            }

            val prefs = applicationContext.getSharedPreferences("downloader_prefs", Context.MODE_PRIVATE)
            val preferredEngineUrl = prefs.getString("cobalt_engine_url", "auto") ?: "auto"

            val result = MediaDownloader.downloadMedia(
                context = applicationContext,
                url = url,
                repository = repository,
                preferredEngineUrl = preferredEngineUrl,
                onProgress = { progress ->
                    updateNotification(progress)
                }
            )

            withContext(Dispatchers.Main) {
                when (result) {
                    is MediaDownloader.DownloadResult.Success -> {
                        Toast.makeText(
                            applicationContext,
                            "Download completed: ${result.fileName}",
                            Toast.LENGTH_LONG
                        ).show()
                        showCompleteNotification(result.fileName, true)
                    }
                    is MediaDownloader.DownloadResult.Error -> {
                        Toast.makeText(
                            applicationContext,
                            "Download failed: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        showCompleteNotification(result.message, false)
                    }
                }
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of background downloader"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildProgressNotification(progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Universal Downloader")
            .setContentText("Downloading media in original quality... $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(progress: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildProgressNotification(progress))
    }

    private fun showCompleteNotification(message: String, success: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = if (success) "Download Finished!" else "Download Failed"
        val icon = if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(2002, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
