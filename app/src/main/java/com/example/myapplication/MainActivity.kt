package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.fieldreport.FieldReport
import com.example.myapplication.fieldreport.FieldReportViewModel
import com.example.myapplication.fieldreport.ReportDetailView
import com.example.myapplication.fieldreport.ReportView
import com.example.myapplication.fieldreport.ReportsListView
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                LocationScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = viewModel(),
    disasterViewModel: DisasterViewModel = viewModel(),
    fieldReportViewModel: FieldReportViewModel = viewModel()
) {
    val context = LocalContext.current
    val locationData by viewModel.locationData.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val disasterState by disasterViewModel.state.collectAsState()
    val fieldReports by fieldReportViewModel.reports.collectAsState()
    val reportUpdates by fieldReportViewModel.currentReportUpdates.collectAsState()
    val settingsManager = remember { SettingsManager(context) }
    val useMetric by settingsManager.useMetric.collectAsState(initial = true)
    val networkState by remember { NetworkUtils.observeNetworkState(context) }.collectAsState(initial = NetworkState.Available)
    val scope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    var showOfflineMaps by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showCreateReportDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<FieldReport?>(null) }
    var mapTargetLatitude by remember { mutableStateOf<Double?>(null) }
    var mapTargetLongitude by remember { mutableStateOf<Double?>(null) }
    var navigationTrigger by remember { mutableStateOf(0) }

    // Load updates when a report is selected
    LaunchedEffect(selectedReport) {
        selectedReport?.let {
            fieldReportViewModel.loadUpdatesForReport(it.localId)
        }
    }

    // Update nearby disasters when location changes
    LaunchedEffect(locationData.latitude, locationData.longitude) {
        if (!locationData.isLoading) {
            disasterViewModel.updateNearbyDisasters(locationData.latitude, locationData.longitude)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.checkPermission(context)
            viewModel.startLocationUpdates(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermission(context)
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.startLocationUpdates(context)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLocationUpdates()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Aidstack ",
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFF1A365D)
                        )
                        Text(
                            "Field",
                            fontWeight = FontWeight.Medium,
                            color = androidx.compose.ui.graphics.Color(0xFF475569)
                        )

                        // Offline indicator
                        if (networkState is NetworkState.Unavailable) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                color = androidx.compose.ui.graphics.Color(0xFFFFEBEE)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CloudOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = androidx.compose.ui.graphics.Color(0xFFD32F2F)
                                    )
                                    Text(
                                        "Offline",
                                        fontSize = 12.sp,
                                        color = androidx.compose.ui.graphics.Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    titleContentColor = androidx.compose.ui.graphics.Color(0xFF1A365D)
                ),
                actions = {
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(
                            Icons.Default.Settings,
                            "Settings",
                            tint = androidx.compose.ui.graphics.Color(0xFF1A365D)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.LocationOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Location permission required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This app needs location access to show your position and altitude",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) {
                    Text("Grant Permission")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Content behind tabs
                Column(modifier = Modifier.fillMaxSize()) {
                    // Spacer for tab height
                    Spacer(modifier = Modifier.height(64.dp))

                    // Content based on selected tab
                    when (selectedTab) {
                        0 -> {
                            // Map View
                            if (locationData.isLoading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Waiting for GPS signal...")
                                    }
                                }
                            } else {
                                var recenterTrigger by remember { mutableStateOf(0) }

                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    MapViewComposable(
                                        latitude = locationData.latitude,
                                        longitude = locationData.longitude,
                                        disasters = disasterViewModel.getFilteredDisasters(),
                                        fieldReports = fieldReports,
                                        modifier = Modifier.fillMaxSize(),
                                        recenterTrigger = recenterTrigger,
                                        targetLatitude = mapTargetLatitude,
                                        targetLongitude = mapTargetLongitude,
                                        navigationTrigger = navigationTrigger
                                    )

                                    // Recenter button (top right)
                                    FloatingActionButton(
                                        onClick = {
                                            recenterTrigger++
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp),
                                        containerColor = androidx.compose.ui.graphics.Color.White,
                                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MyLocation,
                                            contentDescription = "Center on my location",
                                            tint = androidx.compose.ui.graphics.Color(0xFF1A365D)
                                        )
                                    }

                                    // Floating info card
                                    Card(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        elevation = CardDefaults.cardElevation(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                QuickInfoItem(
                                                    icon = Icons.Default.Speed,
                                                    label = "Speed",
                                                    value = if (useMetric)
                                                        "%.1f km/h".format(locationData.speed * 3.6f)
                                                    else
                                                        "%.1f mph".format(locationData.speed * 2.237f)
                                                )
                                                QuickInfoItem(
                                                    icon = Icons.Default.Terrain,
                                                    label = "Altitude",
                                                    value = if (useMetric)
                                                        "%.0f m".format(locationData.altitude)
                                                    else
                                                        "%.0f ft".format(locationData.altitude * 3.281f)
                                                )
                                                QuickInfoItem(
                                                    icon = Icons.Default.Navigation,
                                                    label = "Bearing",
                                                    value = locationData.getBearingDirection()
                                                )
                                            }
                                        }
                                    }

                                    // Create Report FAB (top right, below recenter button)
                                    FloatingActionButton(
                                        onClick = {
                                            showCreateReportDialog = true
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 80.dp, end = 16.dp),
                                        containerColor = androidx.compose.ui.graphics.Color(0xFFFF6B35),
                                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Create field report",
                                            tint = androidx.compose.ui.graphics.Color.White
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Disasters Tab
                            DisastersView(
                                disasterState = disasterState,
                                userLat = locationData.latitude,
                                userLon = locationData.longitude,
                                useMetric = useMetric,
                                onRefresh = { disasterViewModel.loadDisasters() }
                            )
                        }
                        2 -> {
                            // Reports Tab - List or Detail View
                            if (selectedReport != null) {
                                ReportDetailView(
                                    report = selectedReport!!,
                                    updates = reportUpdates,
                                    onBack = { selectedReport = null },
                                    onShowOnMap = {
                                        // Set target coordinates and switch to map tab
                                        mapTargetLatitude = selectedReport!!.latitude
                                        mapTargetLongitude = selectedReport!!.longitude
                                        navigationTrigger++
                                        selectedTab = 0 // Switch to Map tab
                                        selectedReport = null // Clear selection
                                    },
                                    onDelete = {
                                        // Delete the report
                                        fieldReportViewModel.deleteReport(selectedReport!!)
                                        selectedReport = null // Go back to list
                                    },
                                    onStatusChange = { newStatus ->
                                        // Change report status
                                        fieldReportViewModel.changeReportStatus(selectedReport!!, newStatus)
                                    },
                                    onAddNote = { text, photos ->
                                        // Add note to activity log
                                        fieldReportViewModel.addUpdateToReport(
                                            selectedReport!!,
                                            com.example.myapplication.fieldreport.UpdateType.NOTE,
                                            text,
                                            photos
                                        )
                                    }
                                )
                            } else {
                                ReportsListView(
                                    reports = fieldReports,
                                    onReportClick = { report ->
                                        selectedReport = report
                                    },
                                    onCreateReport = {
                                        showCreateReportDialog = true
                                    },
                                    onExportFull = {
                                        scope.launch {
                                            val csvFile = ReportExporter.exportToCSV(context, fieldReports)
                                            csvFile?.let { file ->
                                                ReportExporter.updateLastExportTimestamp(context)
                                                val shareIntent = ReportExporter.shareCSV(context, file)
                                                shareIntent?.let { context.startActivity(it) }
                                            }
                                        }
                                    },
                                    onExportDelta = {
                                        scope.launch {
                                            val lastExport = ReportExporter.getLastExportTimestamp(context)
                                            val csvFile = ReportExporter.exportDeltaToCSV(context, fieldReports, lastExport)
                                            csvFile?.let { file ->
                                                ReportExporter.updateLastExportTimestamp(context)
                                                val shareIntent = ReportExporter.shareCSV(context, file)
                                                shareIntent?.let { context.startActivity(it) }
                                            }
                                        }
                                    },
                                    onExportPackage = {
                                        scope.launch {
                                            val updatesMap = fieldReportViewModel.getAllUpdatesMap()
                                            val files = ReportExporter.exportCompletePackage(context, fieldReports, updatesMap)
                                            if (files.isNotEmpty()) {
                                                ReportExporter.updateLastExportTimestamp(context)
                                                val shareIntent = ReportExporter.shareMultipleCSVs(context, files)
                                                shareIntent?.let { context.startActivity(it) }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Tab Row on top (in front) - wrapped in Surface for elevation
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter),
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp
                ) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Map", maxLines = 1) },
                            icon = { Icon(Icons.Default.Map, null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Disasters", maxLines = 1) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (disasterState.nearbyDisasters.isNotEmpty()) {
                                            Badge { Text("${disasterState.nearbyDisasters.size}") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Warning, null)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Reports", maxLines = 1) },
                            icon = { Icon(Icons.Default.Assignment, null) }
                        )
                    }
                }
            }
        }

        // Settings Dialog
        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Settings") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Use Metric Units")
                            Switch(
                                checked = useMetric,
                                onCheckedChange = {
                                    scope.launch {
                                        settingsManager.setUseMetric(it)
                                    }
                                }
                            )
                        }

                        Divider()

                        // Offline Maps button
                        TextButton(
                            onClick = {
                                showSettings = false
                                showOfflineMaps = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudDownload, "Offline Maps")
                                    Text("Offline Maps")
                                }
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettings = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Offline Maps Screen
        if (showOfflineMaps) {
            OfflineMapsView(
                onBack = { showOfflineMaps = false }
            )
        }

        // Create Report Dialog
        if (showCreateReportDialog) {
            AlertDialog(
                onDismissRequest = { showCreateReportDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        // Header with close button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Create Report",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFF1A365D)
                            )
                            IconButton(onClick = { showCreateReportDialog = false }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }
                        Divider()

                        // Report form
                        ReportView(
                            viewModel = fieldReportViewModel,
                            currentLatitude = locationData.latitude,
                            currentLongitude = locationData.longitude,
                            onReportSubmitted = {
                                showCreateReportDialog = false
                                selectedReport = null // Clear any selected report
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}