package com.example.myapplication.fieldreport

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "report_updates")
@TypeConverters(Converters::class)
data class ReportUpdate(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val reportId: String, // Foreign key to FieldReport
    val updateType: UpdateType,
    val text: String,
    val newStatus: ReportStatus? = null,
    val photoUris: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null // For future user authentication
)

enum class UpdateType(val displayName: String, val icon: String) {
    NOTE("Note", "📝"),
    STATUS_CHANGE("Status Changed", "🔄"),
    PHOTO("Photo Added", "📷"),
    ACTION("Action Taken", "✔️"),
    EDIT("Report Edited", "✏️")
}
