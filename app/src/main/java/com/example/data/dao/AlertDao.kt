package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM broadcast_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM broadcast_alerts WHERE governorate = :gov ORDER BY timestamp DESC")
    fun getAlertsByGovernorate(gov: String): Flow<List<AlertEntity>>

    @Query("SELECT COUNT(*) FROM broadcast_alerts WHERE isRead = 0")
    fun getUnreadAlertsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<AlertEntity>)

    @Query("UPDATE broadcast_alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE broadcast_alerts SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM broadcast_alerts WHERE id = :id")
    suspend fun deleteAlert(id: Long)

    @Query("DELETE FROM broadcast_alerts")
    suspend fun clearAll()
}
