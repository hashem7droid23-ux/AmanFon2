package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.dao.AlertDao
import com.example.data.dao.ReportDao
import com.example.data.database.AppDatabase
import com.example.data.model.AlertEntity
import com.example.data.model.ReportEntity
import com.example.data.remote.FirestorePhoneService
import com.example.data.remote.FirestoreReport
import com.example.util.ImeiValidator
import com.example.util.ReportPhotos
import com.example.util.ReportPolicy
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PhoneTrackerRepository(private val reportDao: ReportDao, private val alertDao: AlertDao, private val context: Context) {
    private val firestoreService = FirestorePhoneService(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var syncJob: Job? = null
    private val auth = FirebaseAuth.getInstance()
    private val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        syncJob?.cancel(); syncJob = null
        if (firebaseAuth.currentUser != null) syncJob = scope.launch {
            while (isActive) {
                try {
                    firestoreService.observeReportSnapshots().collect { snapshot ->
                        lock.withLock {
                            AppDatabase.getDatabase(context).withTransaction {
                                snapshot.reports.forEach { upsertCloud(it, snapshot.authoritative) }
                                if (snapshot.authoritative) {
                                    val remoteIds = snapshot.reports.map { it.id }.toSet()
                                    reportDao.getCloudReports().filter { it.cloudId !in remoteIds }.forEach {
                                        reportDao.deleteReport(it.id)
                                        ReportPhotos.deleteLocal(context, it.imei1)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { android.util.Log.w("PhoneTrackerRepo", "Sync failed; retrying", e); delay(5000) }
            }
        }
    }
    init { auth.addAuthStateListener(listener) }
    fun close() { auth.removeAuthStateListener(listener); scope.cancel() }
    val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()
    val allAlerts: Flow<List<AlertEntity>> = alertDao.getAllAlerts()
    val unreadAlertsCount = alertDao.getUnreadAlertsCount()
    val reportsCount = reportDao.getReportsCount()
    val recoveredCount = reportDao.getRecoveredCount()
    val activeStolenCount = reportDao.getActiveStolenCount()
    private fun fromCloud(cr: FirestoreReport, localId: Long = 0) = ReportEntity(
        id = localId, cloudId = cr.id, userId = cr.userId, reportType = cr.reportType,
        brand = cr.brand, model = cr.model, imei1 = cr.imei1, imei2 = cr.imei2,
        serialNumber = cr.serialNumber, color = cr.color, distinctiveMarks = cr.distinctiveMarks,
        governorate = cr.governorate, district = cr.district, incidentLocation = cr.incidentLocation,
        incidentTimestamp = cr.createdAt?.toDate()?.time ?: 0L, contactName = cr.contactName,
        primaryPhone = cr.primaryPhone, whatsappNumber = cr.whatsappNumber, rewardAmount = cr.rewardAmount,
        policeReportNumber = cr.policeReportNumber, isRecovered = cr.isRecovered,
        additionalNotes = cr.additionalNotes, createdAt = cr.createdAt?.toDate()?.time ?: 0L
    )
    private suspend fun upsertCloud(cr: FirestoreReport, authoritative: Boolean = false): ReportEntity {
        val known = reportDao.getByCloudId(cr.id)
        val legacy = if (known == null) reportDao.getLegacyByImei(cr.imei1)?.takeIf {
            it.contactName == cr.contactName && it.primaryPhone == cr.primaryPhone && it.model == cr.model
        } else null
        val previous = known ?: legacy
        val entity = fromCloud(cr, previous?.id ?: 0L)
        val result = if (previous == null) entity.copy(id = reportDao.insertReport(entity)) else {
            reportDao.updateReport(entity); entity
        }
        if (authoritative && cr.createdAt != null) reportDao.removeLegacyCacheDuplicates(result.id, cr.imei1, cr.contactName, cr.primaryPhone, cr.model, cr.createdAt.toDate().time)
        return result
    }
    suspend fun insertReport(report: ReportEntity, notifyBroadcast: Boolean = true, publishToCloud: Boolean = true): Long {
        check(publishToCloud) { "البلاغات التجريبية معطلة" }
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("سجّل الدخول لنشر البلاغ")
        val cloudId = firestoreService.publishReportToCloud(report)
        val saved = lock.withLock {
            val existing = reportDao.getByCloudId(cloudId)
            val entity = report.copy(id = existing?.id ?: 0L, cloudId = cloudId, userId = uid)
            if (existing == null) entity.copy(id = reportDao.insertReport(entity)) else { reportDao.updateReport(entity); entity }
        }
        val alert = AlertEntity(
            reportId = saved.id, title = "بلاغ ${if (report.isStolen) "سرقة" else if (report.isLost) "فقدان" else "عثور"}: ${report.brand} ${report.model}",
            message = "${report.governorate} (${report.incidentLocation})", governorate = report.governorate,
            deviceModel = "${report.brand} ${report.model}", imeiSnippet = report.maskedImei,
            alertType = if (report.isStolen) "URGENT_THEFT" else "COMMUNITY_ALERT"
        )
        alertDao.insertAlert(alert)
        try { firestoreService.publishAlertToCloud(alert, cloudId) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { android.util.Log.w("PhoneTrackerRepo", "Report published, alert failed", e) }
        if (notifyBroadcast) com.example.util.InAppNotificationManager.checkAndNotifyIfMatches(saved, context)
        return saved.id
    }
    suspend fun insertSupervisorAlert(alert: AlertEntity) {
        firestoreService.publishAlertToCloud(alert, "supervisor_broadcast_${alert.timestamp}")
        alertDao.insertAlert(alert)
    }
    private suspend fun manageable(id: Long): ReportEntity {
        val report = reportDao.getReportById(id) ?: throw IllegalStateException("البلاغ غير موجود")
        check(ReportPolicy.canManage(report.userId, auth.currentUser?.uid, com.example.util.AdminManager.isAdmin.value)) { "لا تملك صلاحية تعديل هذا البلاغ" }
        check(!report.cloudId.isNullOrBlank()) { "انتظر اكتمال مزامنة هذا البلاغ القديم" }
        return report
    }
    suspend fun updateRecoveryStatus(id: Long, recovered: Boolean) {
        val report = manageable(id)
        firestoreService.updateRecoveryStatusInCloud(requireNotNull(report.cloudId), recovered)
        reportDao.updateRecoveryStatus(id, recovered)
    }
    suspend fun deleteReport(id: Long) {
        val report = manageable(id)
        firestoreService.deleteReportFromCloud(report)
        lock.withLock { reportDao.deleteReport(id) }
        ReportPhotos.deleteLocal(context, report.imei1)
    }
    fun getReportById(id: Long): Flow<ReportEntity?> = reportDao.getReportByIdFlow(id)
    suspend fun checkImei(rawImei: String): ReportEntity? {
        val remote = firestoreService.checkImeiOnServer(ImeiValidator.clean(rawImei)).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        return lock.withLock { remote.firstOrNull()?.let { upsertCloud(it, true) } }
    }
    fun searchReports(query: String) = reportDao.searchReports(query)
    suspend fun markAlertAsRead(id: Long) = alertDao.markAsRead(id)
    suspend fun markAllAlertsAsRead() = alertDao.markAllAsRead()
    suspend fun simulateBroadcastAlert(governorate: String, brand: String, model: String, imei: String) {
        throw IllegalStateException("تم تعطيل البلاغات الوهمية في جميع النسخ")
    }
}
