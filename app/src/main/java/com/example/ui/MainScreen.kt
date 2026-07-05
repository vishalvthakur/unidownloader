package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.DownloadEntity
import com.example.data.StatusMedia
import com.example.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            tint = ElegantAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UniDownloader",
                            fontWeight = FontWeight.Medium,
                            color = ElegantTextLight,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ElegantBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ElegantCard,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.currentTab.value = 0 },
                    icon = { Icon(Icons.Filled.Download, contentDescription = "Downloader") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElegantOnPrimary,
                        selectedTextColor = ElegantPrimary,
                        indicatorColor = ElegantPrimary,
                        unselectedIconColor = ElegantTextMuted,
                        unselectedTextColor = ElegantTextMuted
                    ),
                    modifier = Modifier.testTag("tab_downloader")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.currentTab.value = 1 },
                    icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = "WhatsApp") },
                    label = { Text("WhatsApp") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElegantOnPrimary,
                        selectedTextColor = ElegantPrimary,
                        indicatorColor = ElegantPrimary,
                        unselectedIconColor = ElegantTextMuted,
                        unselectedTextColor = ElegantTextMuted
                    ),
                    modifier = Modifier.testTag("tab_whatsapp")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.currentTab.value = 2 },
                    icon = { Icon(Icons.Filled.Folder, contentDescription = "Files") },
                    label = { Text("Files") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElegantOnPrimary,
                        selectedTextColor = ElegantPrimary,
                        indicatorColor = ElegantPrimary,
                        unselectedIconColor = ElegantTextMuted,
                        unselectedTextColor = ElegantTextMuted
                    ),
                    modifier = Modifier.testTag("tab_files")
                )
            }
        },
        containerColor = ElegantBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> DownloaderTab(viewModel)
                1 -> WhatsAppTab(viewModel)
                2 -> FileManagerTab(viewModel)
            }
        }
    }
}

@Composable
fun DownloaderTab(viewModel: MainViewModel) {
    var urlInput by remember { mutableStateOf("") }
    val downloadState by viewModel.manualDownloadState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantCard),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElegantCard, ElegantBackground)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "BACKGROUND SERVICE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantAccent,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "System Active",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light,
                            color = ElegantTextLight
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Ready to intercept links from Instagram, YouTube, and TikTok. Tap Share in any app or paste a link below to begin.",
                            fontSize = 14.sp,
                            color = ElegantTextMuted,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Input panel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Insert Media Link",
                    fontWeight = FontWeight.SemiBold,
                    color = ElegantTextLight,
                    fontSize = 16.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("https://www.youtube.com/watch?v=...", color = ElegantTextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("downloader_url_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ElegantTextLight,
                            unfocusedTextColor = ElegantTextLight,
                            focusedBorderColor = ElegantAccent,
                            unfocusedBorderColor = ElegantOutline,
                            focusedContainerColor = ElegantSecondaryCard,
                            unfocusedContainerColor = ElegantSecondaryCard
                        ),
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear text", tint = ElegantTextMuted)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.startManualDownload(urlInput)
                            }
                        )
                    )

                    Button(
                        onClick = { viewModel.startManualDownload(urlInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElegantAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("downloader_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = "Download",
                            tint = ElegantOnPrimary
                        )
                    }
                }
            }
        }

        // Download Status / Progress
        item {
            AnimatedContent(targetState = downloadState, label = "download_status") { state ->
                when (state) {
                    is MainViewModel.DownloadState.Downloading -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ElegantSecondaryCard),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(ElegantAccent, ElegantPrimary)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Downloading...",
                                        fontWeight = FontWeight.Bold,
                                        color = ElegantTextLight
                                    )
                                    Text(
                                        text = "${state.progress}%",
                                        fontWeight = FontWeight.Bold,
                                        color = ElegantAccent
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = ElegantAccent,
                                    trackColor = ElegantOutline,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Analyzing metadata and pulling stream chunks...",
                                    fontSize = 12.sp,
                                    color = ElegantTextMuted
                                )
                            }
                        }
                    }
                    is MainViewModel.DownloadState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ElegantSecondaryCard)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Success",
                                    tint = ElegantAccent,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Completed successfully!",
                                    fontWeight = FontWeight.Bold,
                                    color = ElegantTextLight,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.fileName,
                                    fontSize = 13.sp,
                                    color = ElegantTextMuted,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(
                                    onClick = { viewModel.resetDownloadState() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = ElegantAccent)
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                    is MainViewModel.DownloadState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ElegantSecondaryCard)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = "Error",
                                    tint = Color.Red,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Download Failed",
                                    fontWeight = FontWeight.Bold,
                                    color = ElegantTextLight,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.message,
                                    fontSize = 13.sp,
                                    color = ElegantTextMuted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(
                                    onClick = { viewModel.resetDownloadState() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = ElegantAccent)
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Live Diagnostics Panel
        item {
            val lastLogs by viewModel.lastDownloadLogs.collectAsStateWithLifecycle()
            if (downloadState !is MainViewModel.DownloadState.Idle && lastLogs.isNotEmpty()) {
                var logsExpanded by remember { mutableStateOf(true) }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = ElegantCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { logsExpanded = !logsExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = ElegantAccent, modifier = Modifier.size(18.dp))
                                Text("Live Execution Log", color = ElegantTextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Icon(
                                imageVector = if (logsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = ElegantTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        if (logsExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .background(ElegantSecondaryCard, RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, ElegantOutline.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                LaunchedEffect(lastLogs.size) {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(scrollState)
                                        .fillMaxWidth()
                                ) {
                                    lastLogs.forEach { log ->
                                        Text(
                                            text = log,
                                            color = if (log.contains("FAILED") || log.contains("Error") || log.contains("exception", ignoreCase = true)) Color(0xFFF2B8B5) else ElegantTextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Help guides
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Background Auto-Download Feature",
                    fontWeight = FontWeight.SemiBold,
                    color = ElegantTextLight,
                    fontSize = 16.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ElegantCard)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ElegantSecondaryCard, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = ElegantAccent)
                        }
                        Column {
                            Text(
                                text = "How to operate invisibly:",
                                fontWeight = FontWeight.Bold,
                                color = ElegantTextLight,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Open YouTube or Instagram on your device.\n" +
                                        "2. Choose any media and tap \"Share\".\n" +
                                        "3. Choose this App's icon from the Share Sheet.\n" +
                                        "4. The app intercepts the link, starts downloading, and saves to your Gallery invisibly in the background!",
                                fontSize = 13.sp,
                                color = ElegantTextMuted,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Advanced Engine Settings
        item {
            var expanded by remember { mutableStateOf(false) }
            val currentEngineUrl by viewModel.cobaltEngineUrl.collectAsStateWithLifecycle()
            val latencies by viewModel.serverLatencies.collectAsStateWithLifecycle()
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                colors = CardDefaults.cardColors(containerColor = ElegantCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ElegantAccent)
                            Column {
                                Text(
                                    "Advanced Engine Settings",
                                    fontWeight = FontWeight.Bold,
                                    color = ElegantTextLight,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Configure download server engines",
                                    color = ElegantTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = ElegantTextMuted
                        )
                    }
                    
                    if (expanded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Preferred Cobalt Instance:",
                                color = ElegantTextLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            TextButton(
                                onClick = { viewModel.testServerLatencies() },
                                colors = ButtonDefaults.textButtonColors(contentColor = ElegantAccent),
                                modifier = Modifier.height(28.dp).padding(0.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Ping", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ping Engines", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val standardInstances = listOf(
                            "Auto-Fallback (Recommended)" to "auto",
                            "Kittycat Boo (Tested)" to "https://dog.kittycat.boo/",
                            "Liubquanti Click (Tested)" to "https://api.cobalt.liubquanti.click/",
                            "Rue Xenon Zone (Tested)" to "https://rue-cobalt.xenon.zone/",
                            "CJS NZ (Tested)" to "https://cobaltapi.cjs.nz/",
                            "Official Server (JWT Required)" to "https://api.cobalt.tools/"
                        )
                        
                        standardInstances.forEach { (name, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setCobaltEngineUrl(value) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentEngineUrl == value,
                                    onClick = { viewModel.setCobaltEngineUrl(value) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ElegantAccent,
                                        unselectedColor = ElegantOutline
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(name, color = ElegantTextLight, fontSize = 13.sp)
                                
                                val latency = latencies[value]
                                if (latency != null) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    val badgeColor = when {
                                        latency == "Testing..." -> ElegantTextMuted
                                        latency == "Offline" || latency.startsWith("HTTP") -> Color(0xFFF2B8B5)
                                        else -> ElegantAccent
                                    }
                                    Text(
                                        text = latency,
                                        color = badgeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(ElegantSecondaryCard, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        var customInput by remember {
                            mutableStateOf(
                                if (currentEngineUrl != "auto" && standardInstances.none { it.second == currentEngineUrl }) {
                                    currentEngineUrl
                                } else {
                                    ""
                                }
                            )
                        }
                        
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { 
                                customInput = it
                                if (it.isNotEmpty() && it.startsWith("http")) {
                                    viewModel.setCobaltEngineUrl(it)
                                }
                            },
                            label = { Text("Custom Server URL", color = ElegantTextMuted) },
                            placeholder = { Text("https://api.example.com/", color = ElegantTextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ElegantTextLight,
                                unfocusedTextColor = ElegantTextLight,
                                focusedBorderColor = ElegantAccent,
                                unfocusedBorderColor = ElegantOutline,
                                focusedContainerColor = ElegantSecondaryCard,
                                unfocusedContainerColor = ElegantSecondaryCard
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "If the active server experiences issues or blocks downloads, Auto-Fallback mode will dynamically search and use working public instances to ensure your download succeeds.",
                            fontSize = 11.sp,
                            color = ElegantTextMuted,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = ElegantOutline.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // HTTP 403 Forbidden Bypassing options (User-Agent and Cookies)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "Security Bypass", tint = ElegantAccent, modifier = Modifier.size(18.dp))
                            Text(
                                "HTTP 403 / CDN Bypass Options",
                                fontWeight = FontWeight.Bold,
                                color = ElegantTextLight,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Spoof client headers to bypass security rules or rate-limits on hosting platforms:",
                            fontSize = 11.sp,
                            color = ElegantTextMuted,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val currentUaPreset by viewModel.userAgentPreset.collectAsStateWithLifecycle()
                        val currentCustomUa by viewModel.customUserAgent.collectAsStateWithLifecycle()
                        val currentCookies by viewModel.customCookies.collectAsStateWithLifecycle()

                        Text(
                            "User-Agent Spoofing Preset:",
                            color = ElegantTextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val uaPresets = listOf(
                            "Standard Browser (Chrome)" to "default",
                            "iOS Safari Mobile" to "mobile_safari",
                            "Instagram App Spoof" to "instagram_app",
                            "TikTok App Spoof" to "tiktok_app",
                            "Search Engine Bot" to "googlebot",
                            "Custom User-Agent String" to "custom"
                        )

                        uaPresets.forEach { (name, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setUserAgentPreset(value) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentUaPreset == value,
                                    onClick = { viewModel.setUserAgentPreset(value) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ElegantAccent,
                                        unselectedColor = ElegantOutline
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(name, color = ElegantTextLight, fontSize = 12.sp)
                            }
                        }

                        if (currentUaPreset == "custom") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = currentCustomUa,
                                onValueChange = { viewModel.setCustomUserAgent(it) },
                                label = { Text("Custom User-Agent Header", color = ElegantTextMuted) },
                                placeholder = { Text("Mozilla/5.0 ...", color = ElegantTextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = ElegantTextLight,
                                    unfocusedTextColor = ElegantTextLight,
                                    focusedBorderColor = ElegantAccent,
                                    unfocusedBorderColor = ElegantOutline,
                                    focusedContainerColor = ElegantSecondaryCard,
                                    unfocusedContainerColor = ElegantSecondaryCard
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Custom Session Cookies (Optional):",
                            color = ElegantTextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Supply active session cookies (e.g. key=value; key2=value2) to bypass age verification or platform geo-restrictions.",
                            fontSize = 11.sp,
                            color = ElegantTextMuted,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = currentCookies,
                            onValueChange = { viewModel.setCustomCookies(it) },
                            label = { Text("Session Cookies", color = ElegantTextMuted) },
                            placeholder = { Text("session_id=xyz; auth_token=abc", color = ElegantTextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ElegantTextLight,
                                unfocusedTextColor = ElegantTextLight,
                                focusedBorderColor = ElegantAccent,
                                unfocusedBorderColor = ElegantOutline,
                                focusedContainerColor = ElegantSecondaryCard,
                                unfocusedContainerColor = ElegantSecondaryCard
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppTab(viewModel: MainViewModel) {
    val treeUri by viewModel.whatsappTreeUri.collectAsStateWithLifecycle()
    val statuses by viewModel.whatsappStatuses.collectAsStateWithLifecycle()
    val isLoading by viewModel.loadingStatuses.collectAsStateWithLifecycle()
    val detectedFolders by viewModel.detectedFolders.collectAsStateWithLifecycle()
    val selectedStatusUris by viewModel.selectedStatusUris.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Register SAF open tree intent launcher
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Take persistable permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.setWhatsAppTreeUri(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (treeUri == null && statuses.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderSpecial,
                contentDescription = null,
                tint = ElegantAccent,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hidden WhatsApp Status Saver",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantTextLight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Android restricts direct access to WhatsApp's media folders for privacy. Grant access once using the Storage Access Framework to save viewed statuses.",
                fontSize = 14.sp,
                color = ElegantTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Detected Folders list
            if (detectedFolders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ElegantCard.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, ElegantAccent.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Detected Installed Storage:",
                            fontWeight = FontWeight.Bold,
                            color = ElegantTextLight,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            detectedFolders.forEach { folder ->
                                Box(
                                    modifier = Modifier
                                        .background(ElegantAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(BorderStroke(1.dp, ElegantAccent.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(folder, color = ElegantAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Onboarding Guide Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElegantCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Steps to authorize:", fontWeight = FontWeight.Bold, color = ElegantTextLight, fontSize = 14.sp)
                    Text("1. Tap the Grant Access button below.", color = ElegantTextMuted, fontSize = 13.sp)
                    Text("2. Navigate to: Android -> media -> com.whatsapp -> WhatsApp -> Media -> .Statuses", color = ElegantTextMuted, fontSize = 13.sp)
                    Text("3. Tap 'Use This Folder' and confirm 'Allow'.", color = ElegantTextMuted, fontSize = 13.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { safLauncher.launch(null) },
                colors = ButtonDefaults.buttonColors(containerColor = ElegantAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("whatsapp_grant_button")
            ) {
                Text("Grant WhatsApp Access", color = ElegantOnPrimary, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.testTag("exit_selection_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Exit selection", tint = ElegantTextLight)
                        }
                        Column {
                            Text(
                                text = "${selectedStatusUris.size} Selected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ElegantTextLight
                            )
                            Text(
                                text = "Tap items to select",
                                fontSize = 11.sp,
                                color = ElegantAccent
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val allSelected = statuses.isNotEmpty() && selectedStatusUris.size == statuses.size
                        IconButton(
                            onClick = {
                                if (allSelected) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAllStatuses()
                                }
                            },
                            modifier = Modifier.testTag("select_all_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Filled.Deselect else Icons.Filled.SelectAll,
                                contentDescription = if (allSelected) "Deselect All" else "Select All",
                                tint = ElegantAccent
                            )
                        }
                        IconButton(
                            onClick = { viewModel.saveSelectedStatuses() },
                            enabled = selectedStatusUris.isNotEmpty(),
                            modifier = Modifier.testTag("batch_save_header_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDownward,
                                contentDescription = "Save Selected",
                                tint = if (selectedStatusUris.isNotEmpty()) ElegantAccent else ElegantTextMuted
                            )
                        }
                    }
                } else {
                    Column {
                        Text(
                            text = "WhatsApp Statuses",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ElegantTextLight
                        )
                        Text(
                            text = if (treeUri != null) "Folder: Custom SAF Path" else "Auto-Scanned Local Folder",
                            fontSize = 11.sp,
                            color = if (treeUri != null) ElegantTextMuted else ElegantAccent
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.toggleSelectionMode() },
                            modifier = Modifier.testTag("toggle_selection_mode_button")
                        ) {
                            Icon(Icons.Filled.DoneAll, contentDescription = "Select Multiple", tint = ElegantTextMuted)
                        }
                        if (treeUri != null) {
                            IconButton(onClick = { viewModel.clearWhatsAppTreeUri() }) {
                                Icon(Icons.Filled.FolderOff, contentDescription = "Clear custom folder", tint = ElegantTextMuted)
                            }
                        } else {
                            IconButton(onClick = { safLauncher.launch(null) }) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = "Grant folder permission", tint = ElegantTextMuted)
                            }
                        }
                        IconButton(onClick = { viewModel.loadWhatsAppStatuses() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = ElegantAccent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElegantAccent)
                }
            } else if (statuses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = ElegantTextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No statuses found.", color = ElegantTextLight, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("View statuses on WhatsApp first!", color = ElegantTextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(statuses) { status ->
                            val isSelected = selectedStatusUris.contains(status.uri)
                            StatusItemCard(
                                status = status,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onToggleSelect = { viewModel.toggleStatusSelection(status) },
                                onSaveClick = { viewModel.saveStatus(status) }
                            )
                        }
                    }

                    // Bottom Floating Multi-Save Control Bar
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSelectionMode,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = ElegantCard),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            border = BorderStroke(1.dp, ElegantAccent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedStatusUris.size} Selected",
                                    color = ElegantTextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { viewModel.clearSelection() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = ElegantTextMuted
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("multi_cancel_button")
                                    ) {
                                        Text("Cancel", fontSize = 13.sp)
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.saveSelectedStatuses() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElegantAccent,
                                            contentColor = ElegantOnPrimary
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("multi_save_button"),
                                        enabled = selectedStatusUris.isNotEmpty()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowDownward,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("Save Selected", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusItemCard(
    status: StatusMedia,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onSaveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .testTag("status_item_card_${status.name}")
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        // Enter selection mode and select item on single click for best UX
                        onToggleSelect()
                    }
                },
                onLongClick = {
                    onToggleSelect()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElegantCard),
        border = if (isSelected) BorderStroke(2.dp, ElegantAccent) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Preview thumbnail using Coil AsyncImage
            AsyncImage(
                model = status.uri,
                contentDescription = status.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Category tag (Video or Photo)
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (status.isVideo) Icons.Filled.PlayArrow else Icons.Filled.Photo,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (status.isVideo) "VIDEO" else "IMAGE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Selection indicator overlay (Top End)
            if (isSelectionMode || isSelected) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Not selected",
                        tint = if (isSelected) ElegantAccent else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Bottom descriptive overlay with "Save to Gallery" button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = status.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(status.size),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    
                    // Styled "Save to Gallery" button (only visible when not in selection mode to avoid visual clutter)
                    if (!isSelectionMode) {
                        Button(
                            onClick = onSaveClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElegantAccent,
                                contentColor = ElegantOnPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("save_to_gallery_button_${status.name}"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDownward,
                                    contentDescription = "Save to Gallery",
                                    tint = ElegantOnPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Save",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileManagerTab(viewModel: MainViewModel) {
    val downloads by viewModel.filteredDownloads.collectAsStateWithLifecycle()
    val rawDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val mediaType by viewModel.selectedMediaType.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Stats calculations
    val totalSizeStr = remember(rawDownloads) {
        val bytes = rawDownloads.sumOf { it.fileSize }
        formatFileSize(bytes)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ElegantCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Organized Storage", color = ElegantTextMuted, fontSize = 12.sp)
                    Text("$totalSizeStr Used", color = ElegantTextLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .background(ElegantAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${rawDownloads.size} files",
                        color = ElegantAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ElegantCard.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ElegantOutline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Auto Cleanup Status",
                        tint = ElegantAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Auto-Cleanup Active",
                            color = ElegantTextLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clears WhatsApp and cache files older than 30 days automatically.",
                            color = ElegantTextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
                Button(
                    onClick = {
                        com.example.data.StatusCacheCleanupWorker.runOnce(context)
                        Toast.makeText(context, "Cleanup task enqueued successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElegantAccent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("manual_cleanup_button")
                ) {
                    Text(
                        text = "Clean Now",
                        color = ElegantOnPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search downloaded files...", color = ElegantTextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("file_manager_search"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElegantTextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ElegantTextLight,
                unfocusedTextColor = ElegantTextLight,
                focusedBorderColor = ElegantAccent,
                unfocusedBorderColor = ElegantOutline,
                focusedContainerColor = ElegantSecondaryCard,
                unfocusedContainerColor = ElegantSecondaryCard
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("all", "video", "image", "audio").forEach { type ->
                val isSelected = mediaType == type
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) ElegantAccent else ElegantSecondaryCard,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.selectedMediaType.value = type }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = type.uppercase(),
                        color = if (isSelected) ElegantOnPrimary else ElegantTextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Items list
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = ElegantTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No matching downloaded files.", color = ElegantTextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads) { item ->
                    FileItemRow(item = item, onDelete = { viewModel.deleteDownload(item) })
                }
            }
        }
    }
}

@Composable
fun FileItemRow(item: DownloadEntity, onDelete: () -> Unit) {
    val context = LocalContext.current
    val formattedDate = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }
    val formattedSize = remember(item.fileSize) {
        formatFileSize(item.fileSize)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ElegantCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Media Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(ElegantSecondaryCard, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.mediaType) {
                        "video" -> Icons.Filled.VideoLibrary
                        "audio" -> Icons.Filled.MusicNote
                        else -> Icons.Filled.Image
                    },
                    contentDescription = null,
                    tint = ElegantAccent
                )
            }

            // Media Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = ElegantTextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.platform.uppercase(),
                        color = ElegantAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = formattedSize,
                        color = ElegantTextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "•",
                        color = ElegantTextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formattedDate,
                        color = ElegantTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Actions row (Share / View / Delete)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    shareFileIntent(context, item)
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = ElegantTextMuted, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = {
                    openFileIntent(context, item)
                }) {
                    Icon(Icons.Filled.Visibility, contentDescription = "Open", tint = ElegantTextMuted, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

private fun openFileIntent(context: Context, item: DownloadEntity) {
    try {
        val uri = Uri.parse(item.filePath)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (item.mediaType == "video") "video/*" else if (item.mediaType == "audio") "audio/*" else "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file: No suitable viewer found", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFileIntent(context: Context, item: DownloadEntity) {
    try {
        val uri = Uri.parse(item.filePath)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.mediaType == "video") "video/*" else if (item.mediaType == "audio") "audio/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share media"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot share file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
