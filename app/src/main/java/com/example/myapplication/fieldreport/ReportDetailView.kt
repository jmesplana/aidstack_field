package com.example.myapplication.fieldreport

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.ReportExporter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailView(
    report: FieldReport,
    updates: List<ReportUpdate> = emptyList(),
    onBack: () -> Unit,
    onShowOnMap: () -> Unit = {},
    onDelete: () -> Unit = {},
    onStatusChange: (ReportStatus) -> Unit = {},
    onAddNote: (String, List<android.net.Uri>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showShareMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShowOnMap) {
                        Icon(
                            Icons.Default.Map,
                            "Show on map",
                            tint = Color(0xFF1A365D)
                        )
                    }

                    // Share menu
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(
                                Icons.Default.Share,
                                "Share report",
                                tint = Color(0xFF1A365D)
                            )
                        }

                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Report") },
                                leadingIcon = { Icon(Icons.Default.Description, null) },
                                onClick = {
                                    showShareMenu = false
                                    val shareIntent = ReportExporter.shareReport(context, report)
                                    context.startActivity(shareIntent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Location") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                                onClick = {
                                    showShareMenu = false
                                    val shareIntent = ReportExporter.shareReportLocation(report)
                                    context.startActivity(shareIntent)
                                }
                            )
                        }
                    }

                    // Delete button
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            "Delete report",
                            tint = Color(0xFFEF4444)
                        )
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
            // Title and Category Icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Category icon
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(report.severity.color)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            report.category.icon,
                            fontSize = 32.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        report.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        report.category.displayName,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Severity Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(report.severity.color).copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PriorityHigh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(report.severity.color)
                    )
                    Text(
                        "${report.severity.displayName} Severity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(report.severity.color)
                    )
                }
            }

            // Status Badge (clickable to change)
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(report.status.color).copy(alpha = 0.2f),
                    modifier = Modifier.clickable { showStatusMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(report.status.icon, fontSize = 16.sp)
                        Text(
                            report.status.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(report.status.color)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(report.status.color)
                        )
                    }
                }

                // Status dropdown menu
                DropdownMenu(
                    expanded = showStatusMenu,
                    onDismissRequest = { showStatusMenu = false }
                ) {
                    ReportStatus.values().forEach { status ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(status.icon)
                                    Text(status.displayName)
                                }
                            },
                            onClick = {
                                onStatusChange(status)
                                showStatusMenu = false
                            }
                        )
                    }
                }
            }

            Divider()

            // Description
            if (report.description.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Description",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            report.description,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B),
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // Location Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF1A365D)
                        )
                        Text(
                            "Location",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LocationInfoRow(
                        label = "Latitude",
                        value = String.format("%.6f°", report.latitude)
                    )
                    LocationInfoRow(
                        label = "Longitude",
                        value = String.format("%.6f°", report.longitude)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Show on Map Button
                        OutlinedButton(
                            onClick = onShowOnMap,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF1A365D)
                            )
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View", fontSize = 14.sp)
                        }

                        // Navigate There Button
                        Button(
                            onClick = {
                                // Create geo URI for navigation
                                val geoUri = Uri.parse("google.navigation:q=${report.latitude},${report.longitude}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                mapIntent.setPackage("com.google.android.apps.maps")

                                // Try Google Maps first, fall back to generic map intent
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    // Fallback to generic geo intent
                                    val fallbackUri = Uri.parse("geo:${report.latitude},${report.longitude}?q=${report.latitude},${report.longitude}(${report.title})")
                                    val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                                    try {
                                        context.startActivity(fallbackIntent)
                                    } catch (e: Exception) {
                                        // Could show a toast here if needed
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B35)
                            )
                        ) {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Navigate", fontSize = 14.sp)
                        }
                    }
                }
            }

            // Timestamp
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF1A365D)
                    )
                    Column {
                        Text(
                            "Reported",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                                .format(Date(report.timestamp)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }

            // Photos
            if (report.photoUris.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color(0xFF1A365D)
                            )
                            Text(
                                "Photos (${report.photoUris.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Photo grid
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            report.photoUris.forEach { photoUri ->
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Report photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }

            // Activity Log
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ActivityLogView(
                        updates = updates,
                        onAddNote = onAddNote
                    )
                }
            }

            // Sync Status (for future reference)
            if (!report.isSynced) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF8E1)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFF57C00)
                        )
                        Text(
                            "Not synced to server",
                            fontSize = 14.sp,
                            color = Color(0xFFF57C00)
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Delete Report?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will permanently delete this report:")
                    Text(
                        "\"${report.title}\"",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    if (report.photoUris.isNotEmpty()) {
                        Text(
                            "This will also delete ${report.photoUris.size} photo${if (report.photoUris.size > 1) "s" else ""}.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        "This action cannot be undone.",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEF4444)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LocationInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
    }
}
