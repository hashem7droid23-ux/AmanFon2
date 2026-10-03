package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "phone_reports", indices = [Index(value = ["cloudId"], unique = true)])
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportType: String,
    val brand: String,
    val model: String,
    val imei1: String,
    val imei2: String = "",
    val serialNumber: String = "",
    val color: String,
    val distinctiveMarks: String = "",
    val governorate: String,
    val district: String = "",
    val incidentLocation: String,
    val incidentTimestamp: Long,
    val contactName: String,
    val primaryPhone: String,
    val whatsappNumber: String,
    val rewardAmount: Long = 0,
    val policeReportNumber: String = "",
    val isRecovered: Boolean = false,
    val additionalNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val cloudId: String? = null,
    val userId: String = "",
    val isDemo: Boolean = false
) {
    val isStolen get() = reportType == "STOLEN"
    val isLost get() = reportType == "LOST"
    val isFound get() = reportType == "FOUND"
    val maskedImei: String get() = if (imei1.length >= 8) {
        imei1.take(4) + "*".repeat((imei1.length - 7).coerceAtLeast(3)) + imei1.takeLast(3)
    } else imei1
}
