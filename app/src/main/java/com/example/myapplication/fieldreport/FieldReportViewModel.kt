package com.example.myapplication.fieldreport

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ImageCompressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

class FieldReportViewModel(application: Application) : AndroidViewModel(application) {
    private val reportDao = AppDatabase.getDatabase(application).fieldReportDao()
    private val updateDao = AppDatabase.getDatabase(application).reportUpdateDao()

    private val _reports = MutableStateFlow<List<FieldReport>>(emptyList())
    val reports: StateFlow<List<FieldReport>> = _reports.asStateFlow()

    private val _currentReportUpdates = MutableStateFlow<List<ReportUpdate>>(emptyList())
    val currentReportUpdates: StateFlow<List<ReportUpdate>> = _currentReportUpdates.asStateFlow()

    init {
        viewModelScope.launch {
            reportDao.getAllReports().collect { reportList ->
                _reports.value = reportList
            }
        }
    }

    fun loadUpdatesForReport(reportId: String) {
        viewModelScope.launch {
            updateDao.getUpdatesForReport(reportId).collect { updates ->
                _currentReportUpdates.value = updates
            }
        }
    }

    fun createReport(
        latitude: Double,
        longitude: Double,
        category: ReportCategory,
        severity: ReportSeverity,
        title: String,
        description: String,
        photoUris: List<Uri>
    ) {
        viewModelScope.launch {
            // Save photos to internal storage and get file paths
            val savedPhotoPaths = photoUris.map { uri ->
                savePhotoToInternalStorage(getApplication<Application>().applicationContext, uri)
            }.filterNotNull()

            val report = FieldReport(
                localId = UUID.randomUUID().toString(),
                latitude = latitude,
                longitude = longitude,
                category = category,
                severity = severity,
                title = title,
                description = description,
                photoUris = savedPhotoPaths,
                timestamp = System.currentTimeMillis(),
                isSynced = false
            )

            reportDao.insertReport(report)
        }
    }

    fun changeReportStatus(report: FieldReport, newStatus: ReportStatus) {
        viewModelScope.launch {
            val updatedReport = report.copy(
                status = newStatus,
                lastUpdated = System.currentTimeMillis(),
                isSynced = false
            )
            reportDao.updateReport(updatedReport)

            // Add status change to activity log
            val statusUpdate = ReportUpdate(
                reportId = report.localId,
                updateType = UpdateType.STATUS_CHANGE,
                text = "Status changed to ${newStatus.displayName}",
                newStatus = newStatus
            )
            updateDao.insertUpdate(statusUpdate)
        }
    }

    fun addUpdateToReport(
        report: FieldReport,
        updateType: UpdateType,
        text: String,
        photoUris: List<Uri> = emptyList()
    ) {
        viewModelScope.launch {
            // Save new photos
            val newPhotoPaths = photoUris.map { uri ->
                savePhotoToInternalStorage(getApplication<Application>().applicationContext, uri)
            }.filterNotNull()

            // Create update
            val update = ReportUpdate(
                reportId = report.localId,
                updateType = updateType,
                text = text,
                photoUris = newPhotoPaths
            )
            updateDao.insertUpdate(update)

            // Update report's lastUpdated timestamp
            val updatedReport = report.copy(
                lastUpdated = System.currentTimeMillis(),
                isSynced = false
            )
            reportDao.updateReport(updatedReport)
        }
    }

    fun updateReport(
        report: FieldReport,
        newTitle: String,
        newDescription: String,
        newSeverity: ReportSeverity,
        newCategory: ReportCategory,
        additionalPhotoUris: List<Uri> = emptyList()
    ) {
        viewModelScope.launch {
            // Save new photos to internal storage
            val newPhotoPaths = additionalPhotoUris.map { uri ->
                savePhotoToInternalStorage(getApplication<Application>().applicationContext, uri)
            }.filterNotNull()

            // Combine existing and new photo paths
            val allPhotoPaths = report.photoUris + newPhotoPaths

            // Create updated report
            val updatedReport = report.copy(
                title = newTitle,
                description = newDescription,
                severity = newSeverity,
                category = newCategory,
                photoUris = allPhotoPaths,
                isSynced = false // Mark as unsynced since it was modified
            )

            reportDao.updateReport(updatedReport)

            // Add edit to activity log
            val editUpdate = ReportUpdate(
                reportId = report.localId,
                updateType = UpdateType.EDIT,
                text = "Report details updated"
            )
            updateDao.insertUpdate(editUpdate)
        }
    }

    fun deleteReport(report: FieldReport) {
        viewModelScope.launch {
            // Delete associated photos
            report.photoUris.forEach { path ->
                try {
                    File(path).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Delete all updates for this report
            updateDao.deleteUpdatesForReport(report.localId)

            // Delete the report
            reportDao.deleteReport(report)
        }
    }

    fun deletePhotoFromReport(report: FieldReport, photoPath: String) {
        viewModelScope.launch {
            // Delete the photo file
            try {
                File(photoPath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Update report without this photo
            val updatedPhotos = report.photoUris.filter { it != photoPath }
            val updatedReport = report.copy(
                photoUris = updatedPhotos,
                lastUpdated = System.currentTimeMillis(),
                isSynced = false
            )
            reportDao.updateReport(updatedReport)
        }
    }

    private fun savePhotoToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val fileName = "photo_${UUID.randomUUID()}.jpg"
            // Use ImageCompressor to compress and save the photo
            ImageCompressor.compressImage(context, uri, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUnsyncedReportsCount(): Int {
        return _reports.value.count { !it.isSynced }
    }

    suspend fun getAllUpdatesMap(): Map<String, List<ReportUpdate>> {
        val allUpdates = updateDao.getAllUpdatesSync()
        return allUpdates.groupBy { it.reportId }
    }
}
