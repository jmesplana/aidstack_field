package com.example.myapplication

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.myapplication.fieldreport.FieldReport
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ReportExporter {

    /**
     * Export reports to CSV format
     */
    fun exportToCSV(context: Context, reports: List<FieldReport>): File? {
        try {
            val fileName = "field_reports_${System.currentTimeMillis()}.csv"
            val file = File(context.filesDir, fileName)

            file.bufferedWriter().use { writer ->
                // Write CSV header
                writer.write("ID,Timestamp,Date/Time,Latitude,Longitude,Category,Severity,Title,Description,Photos,Synced\n")

                // Write report data
                reports.forEach { report ->
                    val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(report.timestamp))

                    val row = listOf(
                        report.localId,
                        report.timestamp.toString(),
                        dateTime,
                        report.latitude.toString(),
                        report.longitude.toString(),
                        report.category.displayName,
                        report.severity.displayName,
                        escapeCSV(report.title),
                        escapeCSV(report.description),
                        report.photoUris.size.toString(),
                        if (report.isSynced) "Yes" else "No"
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
