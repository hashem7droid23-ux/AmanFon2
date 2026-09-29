package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class FirestoreReport(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val reportType: String = "STOLEN",
    val brand: String = "",
    val model: String = "",
    val imei1: String = "",
    val imei2: String = "",
    val serialNumber: String = "",
    val color: String = "",
    val distinctiveMarks: String = "",
    val governorate: String = "",
    val district: String = "",
    val incidentLocation: String = "",
    val contactName: String = "",
    val primaryPhone: String = "",
    val whatsappNumber: String = "",
    val rewardAmount: Long = 0,
    val policeReportNumber: String = "",
    val isRecovered: Boolean = false,
    val additionalNotes: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class FirestoreAlert(
    @DocumentId
    val id: String = "",
    val authorId: String = "",
    val reportId: String = "",
    val title: String = "",
    val message: String = "",
    val governorate: String = "",
    val deviceModel: String = "",
    val imeiSnippet: String = "",
    val alertType: String = "URGENT_THEFT",
    @ServerTimestamp
    val timestamp: Timestamp? = null
)
