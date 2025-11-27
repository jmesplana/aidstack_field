package com.example.myapplication.fieldreport

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldReportDao {
    @Query("SELECT * FROM field_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<FieldReport>>

    @Query("SELECT * FROM field_reports WHERE localId = :id")
    suspend fun getReportById(id: String): FieldReport?

    @Query("SELECT * FROM field_reports WHERE isSynced = 0")
    suspend fun getUnsyncedReports(): List<FieldReport>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: FieldReport)

    @Update
    suspend fun updateReport(report: FieldReport)

    @Delete
    suspend fun deleteReport(report: FieldReport)

    @Query("DELETE FROM field_reports WHERE localId = :id")
    suspend fun deleteReportById(id: String)

    @Query("SELECT * FROM field_reports WHERE " +
            "latitude BETWEEN :minLat AND :maxLat AND " +
            "longitude BETWEEN :minLon AND :maxLon")
    fun getReportsInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): Flow<List<FieldReport>>
}
