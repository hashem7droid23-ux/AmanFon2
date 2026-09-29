package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phone_reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportType: String, // STOLEN, LOST, FOUND
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
    val rewardAmount: Long = 0, // YER
    val policeReportNumber: String = "",
    val isRecovered: Boolean = false,
    val additionalNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isStolen: Boolean
        get() = reportType == "STOLEN"

    val isLost: Boolean
        get() = reportType == "LOST"

    val isFound: Boolean
        get() = reportType == "FOUND"

    val maskedImei: String
        get() = if (imei1.length >= 8) {
            val prefix = imei1.take(4)
            val suffix = imei1.takeLast(3)
            val maskedCount = (imei1.length - 7).coerceAtLeast(3)
            prefix + "*".repeat(maskedCount) + suffix
        } else {
            imei1
        }
}
