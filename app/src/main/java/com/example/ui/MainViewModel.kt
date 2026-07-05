package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

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

    // Cobalt custom engine URL configuration
    val cobaltEngineUrl = MutableStateFlow(prefs.getString("cobalt_engine_url", "auto") ?: "auto")

    fun setCobaltEngineUrl(url: String) {
        cobaltEngineUrl.value = url
        prefs.edit().putString("cobalt_engine_url", url).apply()
    }

    // User-Agent and Session Cookie settings state flows
    val userAgentPreset = MutableStateFlow(prefs.getString("user_agent_preset", "default") ?: "default")
    val customUserAgent = MutableStateFlow(prefs.getString("custom_user_agent", "") ?: "")
    val customCookies = MutableStateFlow(prefs.getString("custom_cookies", "") ?: "")

    fun setUserAgentPreset(preset: String) {
        userAgentPreset.value = preset
        prefs.edit().putString("user_agent_preset", preset).apply()
    }

    fun setCustomUserAgent(ua: String) {
        customUserAgent.value = ua
        prefs.edit().putString("custom_user_agent", ua).apply()
    }

    fun setCustomCookies(cookies: String) {
        customCookies.value = cookies
        prefs.edit().putString("custom_cookies", cookies).apply()
    }

    // Manual Download state
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        data class Success(val fileName: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    val manualDownloadState: StateFlow<DownloadState> = DownloadService.serviceDownloadState

    // Live diagnostics logs of last download
    val lastDownloadLogs: StateFlow<List<String>> = MediaDownloader.lastDownloadLogs

    // Server health and latency monitor
    private val _serverLatencies = MutableStateFlow<Map<String, String>>(emptyMap())
    val serverLatencies: StateFlow<Map<String, String>> = _serverLatencies.asStateFlow()

    fun testServerLatencies() {
        viewModelScope.launch(Dispatchers.IO) {
            val servers = listOf(
                "https://dog.kittycat.boo/",
                "https://api.cobalt.liubquanti.click/",
                "https://rue-cobalt.xenon.zone/",
                "https://cobaltapi.cjs.nz/",
                "https://api.cobalt.tools/"
            )
            val current = _serverLatencies.value.toMutableMap()
            servers.forEach { current[it] = "Testing..." }
            _serverLatencies.value = current

            servers.forEach { serverUrl ->
                viewModelScope.launch(Dispatchers.IO) {
                    val start = System.currentTimeMillis()
                    val status = try {
                        val client = okhttp3.OkHttpClient.Builder()
                            .connectTimeout(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .readTimeout(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .build()
                        val req = okhttp3.Request.Builder()
                            .url(serverUrl)
                            .get()
                            .header("Accept", "application/json")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .build()
                        client.newCall(req).execute().use { res ->
                            if (res.isSuccessful || res.code in listOf(405, 400, 401, 403)) {
                                val delay = System.currentTimeMillis() - start
                                "${delay}ms"
                            } else {
                                "HTTP ${res.code}"
                            }
                        }
                    } catch (e: Exception) {
                        "Offline"
                    }
                    val updated = _serverLatencies.value.toMutableMap()
                    updated[serverUrl] = status
                    _serverLatencies.value = updated
                }
            }
        }
    }

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

    private val _detectedFolders = MutableStateFlow<List<String>>(emptyList())
    val detectedFolders: StateFlow<List<String>> = _detectedFolders.asStateFlow()

    private val _selectedStatusUris = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedStatusUris: StateFlow<Set<Uri>> = _selectedStatusUris.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    init {
        // Scan for detected folders in the background
        viewModelScope.launch(Dispatchers.IO) {
            _detectedFolders.value = WhatsAppStatusHelper.getDetectedWhatsAppFolders()
        }

        // Load stored Uri if exists, otherwise attempt local file scan
        val savedUriStr = prefs.getString("whatsapp_saf_uri", null)
        if (!savedUriStr.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(savedUriStr)
                whatsappTreeUri.value = uri
                loadWhatsAppStatuses(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                loadWhatsAppStatuses(null)
            }
        } else {
            loadWhatsAppStatuses(null)
        }
    }

    fun startManualDownload(url: String) {
        if (url.isEmpty()) {
            DownloadService.updateServiceDownloadState(DownloadState.Error("Please enter a valid URL"))
            return
        }
        DownloadService.startDownload(context, url)
    }

    fun resetDownloadState() {
        DownloadService.resetState()
    }

    // WhatsApp Status Management
    fun setWhatsAppTreeUri(uri: Uri) {
        whatsappTreeUri.value = uri
        prefs.edit().putString("whatsapp_saf_uri", uri.toString()).apply()
        loadWhatsAppStatuses(uri)
    }

    fun clearWhatsAppTreeUri() {
        whatsappTreeUri.value = null
        prefs.edit().remove("whatsapp_saf_uri").apply()
        loadWhatsAppStatuses(null)
    }

    fun toggleStatusSelection(status: StatusMedia) {
        val current = _selectedStatusUris.value.toMutableSet()
        if (current.contains(status.uri)) {
            current.remove(status.uri)
        } else {
            current.add(status.uri)
        }
        _selectedStatusUris.value = current
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        } else {
            _isSelectionMode.value = true
        }
    }

    fun selectAllStatuses() {
        val allUris = _whatsappStatuses.value.map { it.uri }.toSet()
        _selectedStatusUris.value = allUris
        if (allUris.isNotEmpty()) {
            _isSelectionMode.value = true
        }
    }

    fun clearSelection() {
        _selectedStatusUris.value = emptySet()
        _isSelectionMode.value = false
    }

    fun toggleSelectionMode() {
        val wasInSelectionMode = _isSelectionMode.value
        if (wasInSelectionMode) {
            clearSelection()
        } else {
            _isSelectionMode.value = true
        }
    }

    fun saveSelectedStatuses() {
        val selectedUris = _selectedStatusUris.value
        if (selectedUris.isEmpty()) return

        val statusesToSave = _whatsappStatuses.value.filter { selectedUris.contains(it.uri) }
        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            for (status in statusesToSave) {
                val success = WhatsAppStatusHelper.saveStatusToGallery(context, status)
                if (success) {
                    successCount++
                    saveWhatsAppDownloadToHistory(status)
                }
            }
            withContext(Dispatchers.Main) {
                if (successCount == statusesToSave.size) {
                    Toast.makeText(context, "Saved $successCount statuses to Gallery successfully!", Toast.LENGTH_SHORT).show()
                } else if (successCount > 0) {
                    Toast.makeText(context, "Saved $successCount of ${statusesToSave.size} statuses to Gallery successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save selected statuses to gallery", Toast.LENGTH_SHORT).show()
                }
                clearSelection()
            }
        }
    }

    fun loadWhatsAppStatuses(uri: Uri? = whatsappTreeUri.value) {
        _loadingStatuses.value = true
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            val list = if (uri != null) {
                WhatsAppStatusHelper.getStatusesFromTree(context, uri)
            } else {
                WhatsAppStatusHelper.scanLocalStatuses()
            }
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
