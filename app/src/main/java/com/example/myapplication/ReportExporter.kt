package com.example.myapplication

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.myapplication.fieldreport.FieldReport
import com.example.myapplication.fieldreport.ReportUpdate
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ReportExporter {

    /**
     * Export reports to CSV format optimized for QGIS import
     * QGIS can import this CSV directly using "Add Delimited Text Layer"
     * with Longitude as X field and Latitude as Y field
     */
    fun exportToCSV(context: Context, reports: List<FieldReport>): File? {
        try {
            val fileName = "field_reports_${System.currentTimeMillis()}.csv"
            val file = File(context.filesDir, fileName)

            file.bufferedWriter().use { writer ->
                // Write CSV header - QGIS friendly column names
                writer.write("ID,Timestamp,DateTime,Longitude,Latitude,Category,Severity,Status,Title,Description,PhotoCount,LastUpdated,Synced,WKT\n")

                // Write report data
                reports.forEach { report ->
                    val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(report.timestamp))

                    val lastUpdatedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(report.lastUpdated))

                    // WKT (Well-Known Text) format for point geometry - QGIS native format
                    val wkt = "POINT(${report.longitude} ${report.latitude})"

                    val row = listOf(
                        escapeCSV(report.localId),
                        report.timestamp.toString(),
                        escapeCSV(dateTime),
                        report.longitude.toString(),  // X coordinate (Longitude first for QGIS)
                        report.latitude.toString(),   // Y coordinate (Latitude second)
                        escapeCSV(report.category.displayName),
                        escapeCSV(report.severity.displayName),
                        escapeCSV(report.status.displayName),
                        escapeCSV(report.title),
                        escapeCSV(report.description),
                        report.photoUris.size.toString(),
                        escapeCSV(lastUpdatedTime),
                        if (report.isSynced) "Yes" else "No",
                        escapeCSV(wkt)
                    ).joinToString(",")

                    writer.write("$row\n")
                }
            }

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Share CSV file via Android share intent
     */
    fun shareCSV(context: Context, file: File): Intent? {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Field Reports Export")
                putExtra(Intent.EXTRA_TEXT, "Exported field reports from Aidstack Field")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            return Intent.createChooser(shareIntent, "Share Reports")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Share a single report as text
     */
    fun shareReport(context: Context, report: FieldReport): Intent {
        val dateTime = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            .format(Date(report.timestamp))

        val text = buildString {
            appendLine("📍 FIELD REPORT")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("${report.category.icon} ${report.title}")
            appendLine()
            appendLine("Category: ${report.category.displayName}")
            appendLine("Severity: ${report.severity.displayName}")
            appendLine()
            if (report.description.isNotEmpty()) {
                appendLine("Description:")
                appendLine(report.description)
                appendLine()
            }
            appendLine("Location:")
            appendLine("  Lat: ${String.format("%.6f", report.latitude)}°")
            appendLine("  Lon: ${String.format("%.6f", report.longitude)}°")
            appendLine()
            appendLine("Reported: $dateTime")
            if (report.photoUris.isNotEmpty()) {
                appendLine("Photos: ${report.photoUris.size}")
            }
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("Shared from Aidstack Field")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${report.category.icon} ${report.title}")
            putExtra(Intent.EXTRA_TEXT, text)
        }

        return Intent.createChooser(shareIntent, "Share Report")
    }

    /**
     * Share report with location (opens in maps)
     */
    fun shareReportLocation(report: FieldReport): Intent {
        val uri = android.net.Uri.parse(
            "geo:${report.latitude},${report.longitude}?q=${report.latitude},${report.longitude}(${report.title})"
        )

        val shareIntent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
        }

        return Intent.createChooser(shareIntent, "Open Location In")
    }

    /**
     * Get summary stats for export
     */
    fun getReportSummary(reports: List<FieldReport>): String {
        val totalReports = reports.size
        val bySeverity = reports.groupBy { it.severity }.mapValues { it.value.size }
        val byCategory = reports.groupBy { it.category }.mapValues { it.value.size }
        val unsynced = reports.count { !it.isSynced }

        return buildString {
            appendLine("FIELD REPORTS SUMMARY")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("Total Reports: $totalReports")
            appendLine("Unsynced: $unsynced")
            appendLine()
            appendLine("By Severity:")
            bySeverity.forEach { (severity, count) ->
                appendLine("  ${severity.displayName}: $count")
            }
            appendLine()
            appendLine("By Category:")
            byCategory.forEach { (category, count) ->
                appendLine("  ${category.icon} ${category.displayName}: $count")
            }
        }
    }

    /**
     * Export activity timeline to CSV format for tracking report evolution
     * Shows one row per update/status change with full history
     */
    fun exportActivityTimelineToCSV(
        context: Context,
        reports: List<FieldReport>,
        updates: Map<String, List<ReportUpdate>>
    ): File? {
        try {
            val fileName = "activity_timeline_${System.currentTimeMillis()}.csv"
            val file = File(context.filesDir, fileName)

            file.bufferedWriter().use { writer ->
                // Write CSV header
                writer.write("ReportID,ReportTitle,UpdateID,UpdateType,Timestamp,DateTime,Status,UpdateText,PhotoCount,Longitude,Latitude,Category,Severity\n")

                // Write activity data
                reports.forEach { report ->
                    val reportUpdates = updates[report.localId] ?: emptyList()

                    reportUpdates.forEach { update ->
                        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date(update.timestamp))

                        val row = listOf(
                            escapeCSV(report.localId),
                            escapeCSV(report.title),
                            escapeCSV(update.id),
                            escapeCSV(update.updateType.displayName),
                            update.timestamp.toString(),
                            escapeCSV(dateTime),
                            escapeCSV(update.newStatus?.displayName ?: report.status.displayName),
                            escapeCSV(update.text),
                            update.photoUris.size.toString(),
                            report.longitude.toString(),
                            report.latitude.toString(),
                            escapeCSV(report.category.displayName),
                            escapeCSV(report.severity.displayName)
                        ).joinToString(",")

                        writer.write("$row\n")
                    }
                }
            }

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Export reports modified since a given timestamp (delta export)
     */
    fun exportDeltaToCSV(context: Context, reports: List<FieldReport>, sinceTimestamp: Long): File? {
        val modifiedReports = reports.filter { it.lastUpdated > sinceTimestamp }
        return if (modifiedReports.isNotEmpty()) {
            exportToCSV(context, modifiedReports)
        } else {
            null
        }
    }

    /**
     * Export complete package: current state + activity timeline
     * Returns a list of files (reports CSV + activity CSV)
     */
    fun exportCompletePackage(
        context: Context,
        reports: List<FieldReport>,
        updates: Map<String, List<ReportUpdate>>
    ): List<File> {
        val files = mutableListOf<File>()

        // Export current state
        exportToCSV(context, reports)?.let { files.add(it) }

        // Export activity timeline
        exportActivityTimelineToCSV(context, reports, updates)?.let { files.add(it) }

        return files
    }

    /**
     * Share multiple CSV files via Android share intent
     */
    fun shareMultipleCSVs(context: Context, files: List<File>): Intent? {
        try {
            val uris = files.map { file ->
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "text/csv"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                putExtra(Intent.EXTRA_SUBJECT, "Field Reports Export Package")
                putExtra(Intent.EXTRA_TEXT, "Complete field reports package: current state + activity timeline")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            return Intent.createChooser(shareIntent, "Share Reports Package")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Get last export timestamp from SharedPreferences
     */
    fun getLastExportTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences("report_export", Context.MODE_PRIVATE)
        return prefs.getLong("last_export_timestamp", 0L)
    }

    /**
     * Update last export timestamp
     */
    fun updateLastExportTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences("report_export", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_export_timestamp", timestamp).apply()
    }

    /**
     * Escape special characters for CSV format
     */
    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
