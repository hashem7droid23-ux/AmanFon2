package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AlertEntity
import com.example.data.model.ReportEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestorePhoneService(private val context: Context) {
    private val databaseId = context.getString(R.string.firestore_database_id)
    private val db = FirebaseFirestore.getInstance(databaseId)
    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeCloudReports(): Flow<List<FirestoreReport>> = flow {
        val path = "reports"
        emit(emptyList()) // Start with initial emission
        db.collection(path)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(FirestoreReport::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    fun observeCloudAlerts(): Flow<List<FirestoreAlert>> = flow {
        val path = "alerts"
        emit(emptyList())
        db.collection(path)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(FirestoreAlert::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun publishReportToCloud(report: ReportEntity): String {
        val uid = requireUserId()
        val path = "reports"
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "reportType" to report.reportType,
            "brand" to report.brand,
            "model" to report.model,
            "imei1" to report.imei1,
            "imei2" to report.imei2,
            "serialNumber" to report.serialNumber,
            "color" to report.color,
            "distinctiveMarks" to report.distinctiveMarks,
            "governorate" to report.governorate,
            "district" to report.district,
            "incidentLocation" to report.incidentLocation,
            "contactName" to report.contactName,
            "primaryPhone" to report.primaryPhone,
            "whatsappNumber" to report.whatsappNumber,
            "rewardAmount" to report.rewardAmount,
            "policeReportNumber" to report.policeReportNumber,
            "isRecovered" to report.isRecovered,
            "additionalNotes" to report.additionalNotes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val docRef = db.collection(path).add(payload).await()
            docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun publishAlertToCloud(alert: AlertEntity, cloudReportId: String = "") {
        val uid = requireUserId()
        val path = "alerts"
        val payload = hashMapOf<String, Any>(
            "authorId" to uid,
            "reportId" to cloudReportId,
            "title" to alert.title,
            "message" to alert.message,
            "governorate" to alert.governorate,
            "deviceModel" to alert.deviceModel,
            "imeiSnippet" to alert.imeiSnippet,
            "alertType" to alert.alertType,
            "timestamp" to FieldValue.serverTimestamp()
        )

        try {
            db.collection(path).add(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }

    suspend fun updateRecoveryStatusInCloud(cloudDocId: String, isRecovered: Boolean) {
        val path = "reports/$cloudDocId"
        try {
            db.collection("reports").document(cloudDocId).update(
                mapOf(
                    "isRecovered" to isRecovered,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    /**
     * Retrieves or initializes the 'config' document in Firestore with 'adminEmail' field.
     */
    suspend fun getOrInitAdminEmail(): String {
        val path = "config/config"
        return try {
            val docRef = db.collection("config").document("config")
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val email = snapshot.getString("adminEmail")
                if (!email.isNullOrBlank()) {
                    return email
                }
            }
            // Document doesn't exist, create it with adminEmail = hashem7droid23@gmail.com
            val initialConfig = hashMapOf<String, Any>(
                "adminEmail" to "hashem7droid23@gmail.com",
                "appName" to "أمان فون",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(initialConfig).await()
            "hashem7droid23@gmail.com"
        } catch (e: Exception) {
            Log.w("FirestorePhoneService", "Config check: ${e.message}")
            "hashem7droid23@gmail.com"
        }
    }
}
