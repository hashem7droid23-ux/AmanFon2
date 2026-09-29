package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "broadcast_alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportId: Long,
    val title: String,
    val message: String,
    val governorate: String,
    val deviceModel: String,
    val imeiSnippet: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val alertType: String = "URGENT_THEFT" // URGENT_THEFT, DEVICE_FOUND, COMMUNITY_ALERT, RECOVERY
)
