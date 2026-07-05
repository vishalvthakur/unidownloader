package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val repository: DownloadRepository

    // Persistence of WhatsApp Tree URI
    private val prefs = context.getSharedPreferences("downloader_prefs", Context.MODE_PRIVATE)

    init {
        val database = AppDatabase.getDatabase(context)
        repository = DownloadRepository(database.downloadDao())
    }

    // Tab state
    var currentTab = MutableStateFlow(0) // 0: Downloader, 1: WhatsApp, 2: File Manager

    // Manual Download state
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        data class Success(val fileName: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    private val _manualDownloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val manualDownloadState: StateFlow<DownloadState> = _manualDownloadState.asStateFlow()

    // Smart File Manager download history list
    val allDownloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // File Manager filter & sort
    val searchQuery = MutableStateFlow("")
    val selectedMediaType = MutableStateFlow("all") // "all", "video", "image", "audio"
    val sortBy = MutableStateFlow("newest") // "newest", "size_desc", "size_asc"

    val filteredDownloads: StateFlow<List<DownloadEntity>> = combine(
        allDownloads,
        searchQuery,
        selectedMediaType,
        sortBy
    ) { list, query, type, sort ->
        var result = list
        if (query.isNotEmpty()) {
            result = result.filter { it.title.contains(query, ignoreCase = true) }
        }
        if (type != "all") {
            result = result.filter { it.mediaType == type }
        }
        result = when (sort) {
            "size_desc" -> result.sortedByDescending { it.fileSize }
            "size_asc" -> result.sortedBy { it.fileSize }
            else -> result.sortedByDescending { it.timestamp }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // WhatsApp Status state
    val whatsappTreeUri = MutableStateFlow<Uri?>(null)
    private val _whatsappStatuses = MutableStateFlow<List<StatusMedia>>(emptyList())
    val whatsappStatuses: StateFlow<List<StatusMedia>> = _whatsappStatuses.asStateFlow()

    private val _loadingStatuses = MutableStateFlow(false)
    val loadingStatuses: StateFlow<Boolean> = _loadingStatuses.asStateFlow()

    init {
        // Load stored Uri if exists
        val savedUriStr = prefs.getString("whatsapp_saf_uri", null)
        if (!savedUriStr.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(savedUriStr)
                whatsappTreeUri.value = uri
                loadWhatsAppStatuses(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startManualDownload(url: String) {
        if (url.isEmpty()) {
            _manualDownloadState.value = DownloadState.Error("Please enter a valid URL")
            return
        }
        _manualDownloadState.value = DownloadState.Downloading(0)
        viewModelScope.launch {
            val result = MediaDownloader.downloadMedia(
                context = context,
                url = url,
                repository = repository,
                onProgress = { progress ->
                    _manualDownloadState.value = DownloadState.Downloading(progress)
                }
            )
            when (result) {
                is MediaDownloader.DownloadResult.Success -> {
                    _manualDownloadState.value = DownloadState.Success(result.fileName)
                }
                is MediaDownloader.DownloadResult.Error -> {
                    _manualDownloadState.value = DownloadState.Error(result.message)
                }
            }
        }
    }

    fun resetDownloadState() {
        _manualDownloadState.value = DownloadState.Idle
    }

    // WhatsApp Status Management
    fun setWhatsAppTreeUri(uri: Uri) {
        whatsappTreeUri.value = uri
        prefs.edit().putString("whatsapp_saf_uri", uri.toString()).apply()
        loadWhatsAppStatuses(uri)
    }

    fun loadWhatsAppStatuses(uri: Uri? = whatsappTreeUri.value) {
        if (uri == null) return
        _loadingStatuses.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val list = WhatsAppStatusHelper.getStatusesFromTree(context, uri)
            _whatsappStatuses.value = list
            _loadingStatuses.value = false
        }
    }

    fun saveStatus(status: StatusMedia) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = WhatsAppStatusHelper.saveStatusToGallery(context, status)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Saved status to Gallery successfully!", Toast.LENGTH_SHORT).show()
                    // Record downloaded WhatsApp item in DB as well!
                    saveWhatsAppDownloadToHistory(status)
                } else {
                    Toast.makeText(context, "Failed to save status to gallery", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun saveWhatsAppDownloadToHistory(status: StatusMedia) = withContext(Dispatchers.IO) {
        val downloadRecord = DownloadEntity(
            title = "WhatsApp_Status_" + status.name,
            url = status.uri.toString(),
            filePath = status.uri.toString(),
            fileSize = status.size,
            mediaType = if (status.isVideo) "video" else "image",
            platform = "whatsapp"
        )
        repository.insertDownload(downloadRecord)
    }

    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete record
            repository.deleteDownload(download)
            // Note: If possible, we could delete from storage, but media store content can be read-only unless we ask for permissions.
            // Removing from list and history database works cleanly.
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Deleted from history", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
