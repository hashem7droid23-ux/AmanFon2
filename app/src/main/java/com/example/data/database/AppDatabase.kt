package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AlertDao
import com.example.data.dao.ReportDao
import com.example.data.model.AlertEntity
import com.example.data.model.ReportEntity

@Database(entities = [ReportEntity::class, AlertEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun alertDao(): AlertDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE phone_reports ADD COLUMN cloudId TEXT")
                db.execSQL("ALTER TABLE phone_reports ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE phone_reports ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE UNIQUE INDEX index_phone_reports_cloudId ON phone_reports(cloudId)")
                // Hide only the exact bundled sample identities; never erase user reports.
                val samples = listOf(
                    "354892110485921" to "ياسر قائد الشميري",
                    "356789104812390" to "طارق عمر المحضار",
                    "869402058319204" to "مروان سعيد عبد الله",
                    "358910459201483" to "محل الأندلس لخدمات الجوال (المهندس عادل)",
                    "359102485910243" to "سالم باوزير"
                )
                samples.forEach { (imei, name) ->
                    db.execSQL("UPDATE phone_reports SET isDemo = 1 WHERE imei1 = ? AND contactName = ?", arrayOf(imei, name))
                }
                // Hide demo notifications without dropping any tables or real reports.
                db.execSQL("UPDATE broadcast_alerts SET isRead = 1 WHERE reportId IN (SELECT id FROM phone_reports WHERE isDemo = 1)")
            }
        }
        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "yemen_phone_tracker_db")
                .addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
