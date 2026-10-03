package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.database.AppDatabase
import com.example.data.model.ReportEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DatabaseUpgradeTest {
    private fun report(cloudId: String? = null) = ReportEntity(
        reportType = "STOLEN", brand = "Samsung", model = "S24", imei1 = "354892110485921",
        color = "أسود", governorate = "عدن", incidentLocation = "السوق", incidentTimestamp = 1000,
        contactName = "صاحب حقيقي", primaryPhone = "777123456", whatsappNumber = "777123456", createdAt = 1000,
        cloudId = cloudId, userId = "owner"
    )
    @Test fun migrationPreservesRealReportsAndValidatesRoomSchema() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val name = "upgrade-regression.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE phone_reports (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, reportType TEXT NOT NULL, brand TEXT NOT NULL, model TEXT NOT NULL, imei1 TEXT NOT NULL, imei2 TEXT NOT NULL, serialNumber TEXT NOT NULL, color TEXT NOT NULL, distinctiveMarks TEXT NOT NULL, governorate TEXT NOT NULL, district TEXT NOT NULL, incidentLocation TEXT NOT NULL, incidentTimestamp INTEGER NOT NULL, contactName TEXT NOT NULL, primaryPhone TEXT NOT NULL, whatsappNumber TEXT NOT NULL, rewardAmount INTEGER NOT NULL, policeReportNumber TEXT NOT NULL, isRecovered INTEGER NOT NULL, additionalNotes TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE broadcast_alerts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, reportId INTEGER NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, governorate TEXT NOT NULL, deviceModel TEXT NOT NULL, imeiSnippet TEXT NOT NULL, timestamp INTEGER NOT NULL, isRead INTEGER NOT NULL, alertType TEXT NOT NULL)")
                    db.execSQL("INSERT INTO phone_reports VALUES (42,'STOLEN','Samsung','S24','354892110485921','','','أسود','','عدن','','السوق',1000,'صاحب حقيقي','777123456','777123456',0,'',0,'',1000)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Unexpected helper migration") }
            }).build())
        helper.writableDatabase
        helper.close()
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            val saved = room.reportDao().getReportById(42)
            assertNotNull(saved)
            assertEquals("صاحب حقيقي", saved!!.contactName)
            assertFalse(saved.isDemo)
            assertNull(saved.cloudId)
            assertEquals("", saved.userId)
        } finally { room.close(); context.deleteDatabase(name) }
    }
    @Test fun repeatedCloudUpdatesKeepStableLocalIdentityAndUniqueCloudId() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.reportDao()
            val id = dao.insertReport(report("cloud-1"))
            dao.updateReport(report("cloud-1").copy(id = id, isRecovered = true))
            assertEquals(id, dao.getByCloudId("cloud-1")!!.id)
            assertTrue(dao.getByCloudId("cloud-1")!!.isRecovered)
            assertEquals(1, dao.getCloudReports().size)
            var rejected = false
            try { dao.insertReport(report("cloud-1")) } catch (_: Exception) { rejected = true }
            assertTrue("Duplicate cloud IDs must be rejected", rejected)
            dao.deleteReport(id)
            assertNull(dao.getByCloudId("cloud-1"))
        } finally { db.close() }
    }
}
