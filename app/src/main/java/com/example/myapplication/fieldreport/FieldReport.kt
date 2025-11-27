package com.example.myapplication.fieldreport

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Entity(tableName = "field_reports")
@TypeConverters(Converters::class)
data class FieldReport(
    @PrimaryKey
    val localId: String, // UUID for local tracking
    val serverId: String? = null, // For future backend sync
    val latitude: Double,
    val longitude: Double,
    val category: ReportCategory,
    val severity: ReportSeverity,
    val status: ReportStatus = ReportStatus.NEW,
    val title: String,
    val description: String,
    val photoUris: List<String>, // File paths to locally stored photos
    val timestamp: Long,
    val lastUpdated: Long = timestamp,
    val isSynced: Boolean = false, // For future backend sync
    val userId: String? = null // For future user authentication
)

enum class ReportCategory(val displayName: String, val icon: String) {
    DAMAGE_ASSESSMENT("Damage Assessment", "🏚️"),
    MEDICAL_NEED("Medical Need", "🏥"),
    SUPPLY_REQUEST("Supply Request", "📦"),
    HAZARD_ALERT("Hazard Alert", "⚠️"),
    EVACUATION("Evacuation Point", "🚶"),
    SHELTER("Shelter", "🏠"),
    WATER_SOURCE("Water Source", "💧"),
    INFRASTRUCTURE("Infrastructure", "🛣️"),
    OTHER("Other", "📍")
}

enum class ReportSeverity(val displayName: String, val color: Long) {
    CRITICAL(displayName = "Critical", color = 0xFFF44336),
    HIGH(displayName = "High", color = 0xFFFF9800),
    MEDIUM(displayName = "Medium", color = 0xFFFFEB3B),
    LOW(displayName = "Low", color = 0xFF4CAF50),
    INFO(displayName = "Info", color = 0xFF2196F3)
}

enum class ReportStatus(val displayName: String, val color: Long, val icon: String) {
    NEW(displayName = "New", color = 0xFF2196F3, icon = "🆕"),
    ASSESSED(displayName = "Assessed", color = 0xFF9C27B0, icon = "📋"),
    IN_PROGRESS(displayName = "In Progress", color = 0xFFFF9800, icon = "🔧"),
    NEEDS_SUPPLIES(displayName = "Needs Supplies", color = 0xFFFF5722, icon = "📦"),
    RESOLVED(displayName = "Resolved", color = 0xFF4CAF50, icon = "✅"),
    CLOSED(displayName = "Closed", color = 0xFF757575, icon = "🔒")
}

// Type converters for Room
class Converters {
    private val moshi = Moshi.Builder().build()
    private val listAdapter: JsonAdapter<List<String>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    @TypeConverter
    fun fromPhotoUriList(value: List<String>): String {
        return listAdapter.toJson(value)
    }

    @TypeConverter
    fun toPhotoUriList(value: String): List<String> {
        return listAdapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromReportCategory(value: ReportCategory): String {
        return value.name
    }

    @TypeConverter
    fun toReportCategory(value: String): ReportCategory {
        return ReportCategory.valueOf(value)
    }

    @TypeConverter
    fun fromReportSeverity(value: ReportSeverity): String {
        return value.name
    }

    @TypeConverter
    fun toReportSeverity(value: String): ReportSeverity {
        return ReportSeverity.valueOf(value)
    }

    @TypeConverter
    fun fromReportStatus(value: ReportStatus): String {
        return value.name
    }

    @TypeConverter
    fun toReportStatus(value: String): ReportStatus {
        return ReportStatus.valueOf(value)
    }

    @TypeConverter
    fun fromUpdateType(value: UpdateType): String {
        return value.name
    }

    @TypeConverter
    fun toUpdateType(value: String): UpdateType {
        return UpdateType.valueOf(value)
    }
}
