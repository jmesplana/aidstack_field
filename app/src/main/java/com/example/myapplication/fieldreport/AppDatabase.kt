package com.example.myapplication.fieldreport

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FieldReport::class, ReportUpdate::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fieldReportDao(): FieldReportDao
    abstract fun reportUpdateDao(): ReportUpdateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to field_reports table
                database.execSQL("ALTER TABLE field_reports ADD COLUMN status TEXT NOT NULL DEFAULT 'NEW'")
                database.execSQL("ALTER TABLE field_reports ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0")

                // Create report_updates table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS report_updates (
                        id TEXT PRIMARY KEY NOT NULL,
                        reportId TEXT NOT NULL,
                        updateType TEXT NOT NULL,
                        text TEXT NOT NULL,
                        newStatus TEXT,
                        photoUris TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        userId TEXT
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aidstack_field_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration() // For development - remove for production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
