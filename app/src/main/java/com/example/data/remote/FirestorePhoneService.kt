package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AlertEntity
import com.example.data.model.ReportEntity
import com.example.util.ReportPolicy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID

data class CloudReportSnapshot(val reports: List<FirestoreReport>, val authoritative: Boolean)

class FirestorePhoneService(private val context: Context) {
    private val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    private val auth = FirebaseAuth.getInstance()
    private fun requireUserId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("سجّل الدخول واتصل بالإنترنت لإتمام العملية")

    fun observeReportSnapshots(): Flow<CloudReportSnapshot> = db.collection("reports")
        .orderBy("createdAt", Query.Direction.DESCENDING).snapshots().map {
            CloudReportSnapshot(it.toObjects(FirestoreReport::class.java), !it.metadata.isFromCache && !it.metadata.hasPendingWrites())
        }
    fun observeCloudReports(): Flow<List<FirestoreReport>> = observeReportSnapshots().map { it.reports }
    fun observeCloudAlerts(): Flow<List<FirestoreAlert>> = db.collection("alerts")
        .orderBy("timestamp", Query.Direction.DESCENDING).snapshots().map { it.toObjects(FirestoreAlert::class.java) }

    suspend fun publishReportToCloud(report: ReportEntity): String {
        val uid = requireUserId()
        require(ReportPolicy.isCompleteImei(report.imei1)) { "رقم IMEI يجب أن يكون 15 رقماً" }
        require(report.imei2.isBlank() || ReportPolicy.isCompleteImei(report.imei2)) { "رقم IMEI الثاني غير مكتمل" }
        // Verify connectivity before queuing a write; a failed write is never reported as published.
        withTimeout(15000) { db.collection("config").document("config").get(Source.SERVER).await() }
        val id = report.cloudId ?: UUID.randomUUID().toString()
        val payload = hashMapOf<String, Any>(
            "userId" to uid, "reportType" to report.reportType, "brand" to report.brand,
            "model" to report.model, "imei1" to report.imei1, "imei2" to report.imei2,
            "serialNumber" to report.serialNumber, "color" to report.color,
            "distinctiveMarks" to report.distinctiveMarks, "governorate" to report.governorate,
            "district" to report.district, "incidentLocation" to report.incidentLocation,
            "contactName" to report.contactName, "primaryPhone" to report.primaryPhone,
            "whatsappNumber" to report.whatsappNumber, "rewardAmount" to report.rewardAmount,
            "policeReportNumber" to report.policeReportNumber, "isRecovered" to report.isRecovered,
            "additionalNotes" to report.additionalNotes, "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("reports").document(id).set(payload).await()
        return id
    }

    suspend fun publishAlertToCloud(alert: AlertEntity, cloudReportId: String = "") {
        val uid = requireUserId()
        val payload = hashMapOf<String, Any>(
            "authorId" to uid, "reportId" to cloudReportId, "title" to alert.title,
            "message" to alert.message, "governorate" to alert.governorate,
            "deviceModel" to alert.deviceModel, "imeiSnippet" to alert.imeiSnippet,
            "alertType" to alert.alertType, "timestamp" to FieldValue.serverTimestamp()
        )
        // One initial alert per report. Recovery and broadcasts use their own deterministic IDs.
        val alertId = if (cloudReportId.isBlank()) UUID.randomUUID().toString() else "$cloudReportId-${alert.alertType}"
        db.collection("alerts").document(alertId).set(payload).await()
    }

    suspend fun updateRecoveryStatusInCloud(cloudDocId: String, isRecovered: Boolean) {
        requireUserId()
        db.collection("reports").document(cloudDocId).update(
            mapOf("isRecovered" to isRecovered, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun deleteReportFromCloud(report: ReportEntity) {
        requireUserId()
        val id = report.cloudId ?: throw IllegalStateException("هذا بلاغ قديم لم تكتمل مزامنته بعد")
        val batch = db.batch()
        val photoRef = db.collection("report_photos").document(report.imei1)
        val photo = photoRef.get(Source.SERVER).await()
        // Do not erase another report's photos when multiple reports share an IMEI.
        if (photo.exists() && (photo.getString("reportId") == id ||
            (photo.getString("reportId").isNullOrBlank() && photo.getString("userId") == report.userId))) {
            batch.delete(photoRef)
        }
        batch.delete(db.collection("reports").document(id))
        batch.commit().await()
    }

    suspend fun checkImeiOnServer(imei: String): List<FirestoreReport> {
        requireUserId()
        require(ReportPolicy.isCompleteImei(imei)) { "رقم IMEI يجب أن يكون 15 رقماً" }
        return withTimeout(15000) {
            // Two equality queries avoid an OR composite index dependency.
            val first = db.collection("reports").whereEqualTo("imei1", imei).get(Source.SERVER).await()
            val second = db.collection("reports").whereEqualTo("imei2", imei).get(Source.SERVER).await()
            (first.toObjects(FirestoreReport::class.java) + second.toObjects(FirestoreReport::class.java))
                .distinctBy { it.id }.filter { ReportPolicy.isActiveRisk(it.reportType, it.isRecovered) }
        }
    }

    suspend fun getOrInitAdminEmail(): String {
        val owner = "hashem7droid23@gmail.com"
        return try {
            val ref = db.collection("config").document("config")
            val snap = ref.get().await()
            val email = snap.getString("adminEmail")
            if (!email.isNullOrBlank()) email else {
                val user = auth.currentUser
                if (!snap.exists() && user != null && user.isEmailVerified && user.email.equals(owner, true)) {
                    ref.set(mapOf("adminEmail" to owner, "appName" to "أمان فون", "updatedAt" to FieldValue.serverTimestamp())).await()
                }
                owner
            }
        } catch (e: Exception) {
            Log.w("FirestorePhoneService", "Config check failed", e)
            owner
        }
    }
}
