package com.example.myapplication.fieldreport

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.myapplication.ReportExporter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsListView(
    reports: List<FieldReport>,
    onReportClick: (FieldReport) -> Unit,
    onCreateReport: () -> Unit,
    onExportFull: () -> Unit = {},
    onExportDelta: () -> Unit = {},
    onExportPackage: () -> Unit = {}
) {
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }
    val lastExportTimestamp = ReportExporter.getLastExportTimestamp(context)
    val hasModifiedReports = reports.any { it.lastUpdated > lastExportTimestamp }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF8FAFC),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Field Reports",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A365D)
                    )
                    Text(
                        "${reports.size} report${if (reports.size != 1) "s" else ""}",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Export button (only show if there are reports)
                if (reports.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Export reports",
                                tint = Color(0xFF1A365D)
                            )
                        }

                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Full Export (Current State)") },
                                leadingIcon = {
                                    Icon(Icons.Default.FileDownload, null)
                                },
                                onClick = {
                                    showExportMenu = false
                                    onExportFull()
                                }
                            )
                            if (hasModifiedReports && lastExportTimestamp > 0) {
                                DropdownMenuItem(
                                    text = {
                                        val modifiedCount = reports.count { it.lastUpdated > lastExportTimestamp }
                                        Text("Export Changes Only ($modifiedCount)")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Update, null)
                                    },
                                    onClick = {
                                        showExportMenu = false
                                        onExportDelta()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Complete Package (+ Timeline)") },
                                leadingIcon = {
                                    Icon(Icons.Default.FolderZip, null)
                                },
                                onClick = {
                                    showExportMenu = false
                                    onExportPackage()
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Share Summary") },
                                leadingIcon = {
                                    Icon(Icons.Default.Description, null)
                                },
                                onClick = {
                                    showExportMenu = false
                                    val summary = ReportExporter.getReportSummary(reports)
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Field Reports Summary")
                                        putExtra(android.content.Intent.EXTRA_TEXT, summary)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Summary"))
                                }
                            )
                        }
                    }
                }
            }
        }

        if (reports.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF94A3B8)
                    )
                    Text(
                        "No reports yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        "Tap the + button to create your first field report",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            // Reports list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports) { report ->
                    ReportCard(
                        report = report,
                        onClick = { onReportClick(report) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportCard(
    report: FieldReport,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo thumbnail or category icon
            if (report.photoUris.isNotEmpty()) {
                AsyncImage(
                    model = report.photoUris.first(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            Modifier.padding(0.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(report.severity.color),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                report.category.icon,
                                fontSize = 36.sp
                            )
                        }
                    }
                }
            }

            // Report details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    report.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 2
                )

                // Category and severity
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(report.severity.color).copy(alpha = 0.2f)
                    ) {
                        Text(
                            report.severity.displayName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(report.severity.color)
                        )
                    }
                    Text(
                        report.category.displayName,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Time and photo count
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        getTimeAgo(report.timestamp),
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    if (report.photoUris.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF94A3B8)
                            )
                            Text(
                                "${report.photoUris.size}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Arrow icon
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = Color(0xFF94A3B8)
            )
        }
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        minutes > 0 -> "$minutes min${if (minutes > 1) "s" else ""} ago"
        else -> "Just now"
    }
}
