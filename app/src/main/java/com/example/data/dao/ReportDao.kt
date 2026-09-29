package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM phone_reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM phone_reports WHERE reportType = :type ORDER BY createdAt DESC")
    fun getReportsByType(type: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM phone_reports WHERE governorate = :gov ORDER BY createdAt DESC")
    fun getReportsByGovernorate(gov: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM phone_reports WHERE id = :id")
    fun getReportByIdFlow(id: Long): Flow<ReportEntity?>

    @Query("SELECT * FROM phone_reports WHERE id = :id")
    suspend fun getReportById(id: Long): ReportEntity?

    @Query("SELECT * FROM phone_reports WHERE imei1 = :cleanImei OR imei2 = :cleanImei LIMIT 1")
    suspend fun findByImei(cleanImei: String): ReportEntity?

    @Query("""
        SELECT * FROM phone_reports 
        WHERE model LIKE '%' || :query || '%' 
           OR brand LIKE '%' || :query || '%' 
           OR imei1 LIKE '%' || :query || '%'
           OR incidentLocation LIKE '%' || :query || '%'
           OR contactName LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchReports(query: String): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<ReportEntity>)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Query("UPDATE phone_reports SET isRecovered = :recovered WHERE id = :id")
    suspend fun updateRecoveryStatus(id: Long, recovered: Boolean)

    @Query("DELETE FROM phone_reports WHERE id = :id")
    suspend fun deleteReport(id: Long)

    @Query("SELECT COUNT(*) FROM phone_reports")
    fun getReportsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM phone_reports WHERE isRecovered = 1")
    fun getRecoveredCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM phone_reports WHERE reportType = 'STOLEN' AND isRecovered = 0")
    fun getActiveStolenCount(): Flow<Int>
}
