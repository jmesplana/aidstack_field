package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapsView(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val offlineMapManager = remember { OfflineMapManager(context) }
    val downloadState by offlineMapManager.downloadState.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(offlineMapManager.getCacheSize()) }
    var selectedQuality by remember { mutableStateOf("Medium") }

    val scrollState = rememberScrollState()

    // Quality levels (zoom)
    val qualityOptions = mapOf(
        "Low" to 12,     // Faster download, less detail
        "Medium" to 14,  // Balanced
        "High" to 16     // Slower download, more detail
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Maps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A365D)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Network warning
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFF57C00)
                    )
                    Column {
                        Text(
                            "Offline Maps",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57C00)
                        )
                        Text(
                            "Download maps for areas you'll visit. Use WiFi to avoid data charges.",
                            fontSize = 14.sp,
                            color = Color(0xFF795548)
                        )
                    }
                }
            }

            // Current cache status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Downloaded Maps",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                offlineMapManager.formatCacheSize(cacheSize),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A365D)
                            )
                        }
                        if (offlineMapManager.isCacheAvailable()) {
                            IconButton(
                                onClick = { showDeleteConfirmation = true }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete cached maps",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }

            // Download quality selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Map Quality",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    qualityOptions.forEach { (quality, _) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedQuality == quality,
                                onClick = { selectedQuality = quality }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                quality,
                                fontSize = 14.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                    Text(
                        "Higher quality = more storage needed",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Region selection
            Text(
                "Select Region to Download",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            OfflineMapManager.PREDEFINED_REGIONS.keys.sorted().forEach { regionName ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedRegion == regionName)
                            Color(0xFFE0F2FE) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                regionName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                "Est. ${offlineMapManager.estimateDownloadSize(regionName, qualityOptions[selectedQuality]!!)}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Button(
                            onClick = {
                                selectedRegion = regionName
                                scope.launch {
                                    offlineMapManager.downloadRegion(
                                        regionName,
                                        qualityOptions[selectedQuality]!!
                                    )
                                    cacheSize = offlineMapManager.getCacheSize()
                                }
                            },
                            enabled = downloadState is DownloadState.Idle || downloadState is DownloadState.Completed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A365D)
                            )
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontSize = 14.sp)
                        }
                    }
                }
            }

            // Download progress
            when (val state = downloadState) {
                is DownloadState.Preparing -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                "Preparing download...",
                                fontSize = 14.sp,
                                color = Color(0xFF0C4A6E)
                            )
                        }
                    }
                }
                is DownloadState.Downloading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Downloading $selectedRegion...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0C4A6E)
                                )
                                Text(
                                    "${state.progress}/${state.total}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = if (state.total > 0) state.progress.toFloat() / state.total.toFloat() else 0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                is DownloadState.Completed -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A)
                            )
                            Text(
                                "Download completed! Maps available offline.",
                                fontSize = 14.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
                is DownloadState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFDC2626)
                            )
                            Text(
                                "Error: ${state.message}",
                                fontSize = 14.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Offline Maps?") },
            text = {
                Text("This will delete all downloaded map tiles (${offlineMapManager.formatCacheSize(cacheSize)}). You'll need to download them again for offline use.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        offlineMapManager.clearCache()
                        cacheSize = 0
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFDC2626)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
