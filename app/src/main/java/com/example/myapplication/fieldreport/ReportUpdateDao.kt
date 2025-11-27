package com.example.myapplication.fieldreport

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportUpdateDao {
    @Query("SELECT * FROM report_updates WHERE reportId = :reportId ORDER BY timestamp DESC")
    fun getUpdatesForReport(reportId: String): Flow<List<ReportUpdate>>

    @Query("SELECT * FROM report_updates WHERE reportId = :reportId ORDER BY timestamp DESC")
    suspend fun getUpdatesForReportSync(reportId: String): List<ReportUpdate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdate(update: ReportUpdate)

    @Delete
    suspend fun deleteUpdate(update: ReportUpdate)

    @Query("DELETE FROM report_updates WHERE reportId = :reportId")
    suspend fun deleteUpdatesForReport(reportId: String)
}
