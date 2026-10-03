package com.example.data.dao

import androidx.room.*
import com.example.data.model.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM phone_reports WHERE isDemo = 0 ORDER BY createdAt DESC") fun getAllReports(): Flow<List<ReportEntity>>
    @Query("SELECT * FROM phone_reports WHERE isDemo = 0 AND reportType = :type ORDER BY createdAt DESC") fun getReportsByType(type: String): Flow<List<ReportEntity>>
    @Query("SELECT * FROM phone_reports WHERE isDemo = 0 AND governorate = :gov ORDER BY createdAt DESC") fun getReportsByGovernorate(gov: String): Flow<List<ReportEntity>>
    @Query("SELECT * FROM phone_reports WHERE id = :id AND isDemo = 0") fun getReportByIdFlow(id: Long): Flow<ReportEntity?>
    @Query("SELECT * FROM phone_reports WHERE id = :id AND isDemo = 0") suspend fun getReportById(id: Long): ReportEntity?
    @Query("SELECT * FROM phone_reports WHERE cloudId = :cloudId LIMIT 1") suspend fun getByCloudId(cloudId: String): ReportEntity?
    @Query("SELECT * FROM phone_reports WHERE cloudId IS NOT NULL AND isDemo = 0") suspend fun getCloudReports(): List<ReportEntity>
    @Query("SELECT * FROM phone_reports WHERE cloudId IS NULL AND isDemo = 0 AND imei1 = :imei ORDER BY id LIMIT 1") suspend fun getLegacyByImei(imei: String): ReportEntity?
    // Only exact server-import cache duplicates are discarded; unmatched local records are retained.
    @Query("DELETE FROM phone_reports WHERE cloudId IS NULL AND isDemo = 0 AND id != :keepId AND imei1 = :imei AND contactName = :name AND primaryPhone = :phone AND model = :model AND createdAt = :timestamp")
    suspend fun removeLegacyCacheDuplicates(keepId: Long, imei: String, name: String, phone: String, model: String, timestamp: Long)
    @Query("SELECT * FROM phone_reports WHERE isDemo = 0 AND isRecovered = 0 AND reportType IN ('STOLEN','LOST') AND (imei1 = :cleanImei OR imei2 = :cleanImei) LIMIT 1") suspend fun findByImei(cleanImei: String): ReportEntity?
    @Query("SELECT * FROM phone_reports WHERE isDemo = 0 AND (model LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR imei1 LIKE '%' || :query || '%' OR incidentLocation LIKE '%' || :query || '%' OR contactName LIKE '%' || :query || '%') ORDER BY createdAt DESC") fun searchReports(query: String): Flow<List<ReportEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertReport(report: ReportEntity): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(reports: List<ReportEntity>)
    @Update suspend fun updateReport(report: ReportEntity)
    @Query("UPDATE phone_reports SET isRecovered = :recovered WHERE id = :id") suspend fun updateRecoveryStatus(id: Long, recovered: Boolean)
    @Query("DELETE FROM phone_reports WHERE id = :id") suspend fun deleteReport(id: Long)
    @Query("SELECT COUNT(*) FROM phone_reports WHERE isDemo = 0") fun getReportsCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM phone_reports WHERE isDemo = 0 AND isRecovered = 1") fun getRecoveredCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM phone_reports WHERE isDemo = 0 AND reportType = 'STOLEN' AND isRecovered = 0") fun getActiveStolenCount(): Flow<Int>
}
