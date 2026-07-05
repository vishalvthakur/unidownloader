package com.example.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.ui.MainViewModel.DownloadState

class DownloadService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: DownloadRepository

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        const val EXTRA_URL = "EXTRA_URL"

        // Global flow of current download state from the Service
        private val _serviceDownloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
        val serviceDownloadState: StateFlow<DownloadState> = _serviceDownloadState.asStateFlow()

        fun updateServiceDownloadState(state: DownloadState) {
            _serviceDownloadState.value = state
        }

        fun resetState() {
            _serviceDownloadState.value = DownloadState.Idle
        }

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
                _serviceDownloadState.value = DownloadState.Downloading(0)
                startForeground(NOTIFICATION_ID, buildProgressNotification(0, 0L, 0L))
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
                onProgress = { progress, bytesRead, totalBytes ->
                    updateNotification(progress, bytesRead, totalBytes)
                    _serviceDownloadState.value = DownloadState.Downloading(progress)
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
                        _serviceDownloadState.value = DownloadState.Success(result.fileName)
                    }
                    is MediaDownloader.DownloadResult.Error -> {
                        Toast.makeText(
                            applicationContext,
                            "Download failed: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        showCompleteNotification(result.message, false)
                        _serviceDownloadState.value = DownloadState.Error(result.message)
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

    private fun buildProgressNotification(progress: Int, bytesRead: Long, totalBytes: Long): Notification {
        val contentText = when {
            totalBytes > 0 -> {
                val percentageStr = if (progress >= 0) "$progress%" else "Downloading..."
                "Downloading media: $percentageStr (${formatBytes(bytesRead)} / ${formatBytes(totalBytes)})"
            }
            bytesRead > 0 -> {
                "Downloading media: ${formatBytes(bytesRead)} completed"
            }
            else -> {
                "Starting download..."
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Universal Downloader")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, if (progress >= 0) progress else 0, progress < 0)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(progress: Int, bytesRead: Long, totalBytes: Long) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildProgressNotification(progress, bytesRead, totalBytes))
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return java.text.DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
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
