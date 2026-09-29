package com.example.data.repository

import android.content.Context
import com.example.data.dao.AlertDao
import com.example.data.dao.ReportDao
import com.example.data.model.AlertEntity
import com.example.data.model.ReportEntity
import com.example.util.ImeiValidator
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PhoneTrackerRepository(
    private val reportDao: ReportDao,
    private val alertDao: AlertDao,
    private val context: Context
) {
    private val firestoreService = com.example.data.remote.FirestorePhoneService(context)

    val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()
    val allAlerts: Flow<List<AlertEntity>> = alertDao.getAllAlerts()
    val unreadAlertsCount: Flow<Int> = alertDao.getUnreadAlertsCount()
    val reportsCount: Flow<Int> = reportDao.getReportsCount()
    val recoveredCount: Flow<Int> = reportDao.getRecoveredCount()
    val activeStolenCount: Flow<Int> = reportDao.getActiveStolenCount()

    init {
        // Pre-populate with realistic Yemen sample data if database is empty
        CoroutineScope(Dispatchers.IO).launch {
            val existing = reportDao.getAllReports().first()
            if (existing.isEmpty()) {
                seedInitialYemenReports()
            }
        }

        // Realtime sync from cloud when authenticated
        CoroutineScope(Dispatchers.IO).launch {
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
                if (auth.currentUser != null) {
                    launch {
                        firestoreService.observeCloudReports().collect { cloudReports ->
                            cloudReports.forEach { cr ->
                                val entity = ReportEntity(
                                    reportType = cr.reportType,
                                    brand = cr.brand,
                                    model = cr.model,
                                    imei1 = cr.imei1,
                                    imei2 = cr.imei2,
                                    serialNumber = cr.serialNumber,
                                    color = cr.color,
                                    distinctiveMarks = cr.distinctiveMarks,
                                    governorate = cr.governorate,
                                    district = cr.district,
                                    incidentLocation = cr.incidentLocation,
                                    incidentTimestamp = cr.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                                    contactName = cr.contactName,
                                    primaryPhone = cr.primaryPhone,
                                    whatsappNumber = cr.whatsappNumber,
                                    rewardAmount = cr.rewardAmount,
                                    policeReportNumber = cr.policeReportNumber,
                                    isRecovered = cr.isRecovered,
                                    additionalNotes = cr.additionalNotes,
                                    createdAt = cr.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                                )
                                reportDao.insertReport(entity)
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun insertReport(report: ReportEntity, notifyBroadcast: Boolean = true): Long {
        val id = reportDao.insertReport(report)

        val alertTitle = when (report.reportType) {
            "STOLEN" -> "🚨 بلاغ سرقة عاجل: ${report.brand} ${report.model}"
            "LOST" -> "⚠️ بلاغ فقدان هاتف: ${report.brand} ${report.model}"
            else -> "📢 تم العثور على هاتف: ${report.brand} ${report.model}"
        }

        val alertMessage = "المحافظة: ${report.governorate} (${report.incidentLocation}) - رقم البلاغ #$id"

        val alert = AlertEntity(
            reportId = id,
            title = alertTitle,
            message = alertMessage,
            governorate = report.governorate,
            deviceModel = "${report.brand} ${report.model}",
            imeiSnippet = report.maskedImei,
            alertType = if (report.reportType == "STOLEN") "URGENT_THEFT" else "COMMUNITY_ALERT"
        )
        alertDao.insertAlert(alert)

        // Publish to Firebase Cloud Firestore if signed in
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
            try {
                val cloudId = firestoreService.publishReportToCloud(report)
                firestoreService.publishAlertToCloud(alert, cloudId)
            } catch (e: Exception) {
                android.util.Log.w("PhoneTrackerRepo", "Cloud sync pending: ${e.message}")
            }
        }

        if (notifyBroadcast) {
            if (report.reportType == "STOLEN") {
                NotificationHelper.showUrgentTheftAlert(
                    context = context,
                    reportId = id,
                    title = "سرقة هاتف: ${report.brand} ${report.model}",
                    message = "تم الإبلاغ للتو في ${report.governorate}. اضغط للتحقق من IMEI والتفاصيل.",
                    governorate = report.governorate
                )
            } else {
                NotificationHelper.showGeneralAlert(
                    context = context,
                    id = id.toInt(),
                    title = alertTitle,
                    message = "${report.brand} ${report.model} - ${report.governorate} (${report.incidentLocation})"
                )
            }
        }
        return id
    }

    suspend fun updateRecoveryStatus(id: Long, recovered: Boolean) {
        reportDao.updateRecoveryStatus(id, recovered)
        val report = reportDao.getReportById(id)
        if (recovered && report != null) {
            val recoveryAlert = AlertEntity(
                reportId = id,
                title = "🎉 تم بحمد الله استرجاع الجهاز!",
                message = "تم استعادة هاتف ${report.brand} ${report.model} في ${report.governorate}",
                governorate = report.governorate,
                deviceModel = "${report.brand} ${report.model}",
                imeiSnippet = report.maskedImei,
                alertType = "RECOVERY"
            )
            alertDao.insertAlert(recoveryAlert)
            NotificationHelper.showGeneralAlert(
                context = context,
                id = (id + 1000).toInt(),
                title = "🎉 تم استرجاع الهاتف بنجاح",
                message = "هاتف ${report.brand} ${report.model} عاد لصاحبه في ${report.governorate}!"
            )
        }
    }

    suspend fun deleteReport(id: Long) {
        reportDao.deleteReport(id)
    }

    fun getReportById(id: Long): Flow<ReportEntity?> {
        return reportDao.getReportByIdFlow(id)
    }

    suspend fun checkImei(rawImei: String): ReportEntity? {
        val clean = ImeiValidator.clean(rawImei)
        return reportDao.findByImei(clean)
    }

    fun searchReports(query: String): Flow<List<ReportEntity>> {
        return reportDao.searchReports(query)
    }

    suspend fun markAlertAsRead(id: Long) {
        alertDao.markAsRead(id)
    }

    suspend fun markAllAlertsAsRead() {
        alertDao.markAllAsRead()
    }

    suspend fun simulateBroadcastAlert(
        governorate: String,
        brand: String,
        model: String,
        imei: String
    ) {
        val now = System.currentTimeMillis()
        val simulatedReport = ReportEntity(
            reportType = "STOLEN",
            brand = brand,
            model = model,
            imei1 = imei,
            color = "أسود",
            distinctiveMarks = "خدش واضح بجانب الكاميرا الخلفية",
            governorate = governorate,
            district = "المركز التجاري",
            incidentLocation = "سوق الهواتف الرئيسي",
            incidentTimestamp = now,
            contactName = "أحمد محمد الحاشدي",
            primaryPhone = "777450123",
            whatsappNumber = "777450123",
            rewardAmount = 100000,
            policeReportNumber = "ص/2026/892",
            createdAt = now
        )
        insertReport(simulatedReport, notifyBroadcast = true)
    }

    private suspend fun seedInitialYemenReports() {
        val now = System.currentTimeMillis()
        val oneHour = 3600 * 1000L
        val oneDay = 24 * oneHour

        val initialReports = listOf(
            ReportEntity(
                reportType = "STOLEN",
                brand = "Samsung (سامسونج)",
                model = "Galaxy S23 Ultra (512GB)",
                imei1 = "354892110485921",
                imei2 = "354892110485939",
                serialNumber = "R5CN40PZ7XA",
                color = "أخضر زيتي (Phantom Green)",
                distinctiveMarks = "كسر طفيف على حافة الكاميرا العلوية وكفر شفاف عليه ملصق علم اليمن",
                governorate = "صنعاء (الأمانة)",
                district = "التحرير",
                incidentLocation = "سوق باب السلام - أمام محلات البرق للهواتف",
                incidentTimestamp = now - (3 * oneHour),
                contactName = "ياسر قائد الشميري",
                primaryPhone = "777123987",
                whatsappNumber = "777123987",
                rewardAmount = 150000,
                policeReportNumber = "ب/1043 قسم باب السلام",
                isRecovered = false,
                additionalNotes = "الجهاز مقفل برقم سري وتم تفعيل وضع الفقدان عن بُعد. محلات الهواتف يرجى التحفظ عليه والاتصال فوراً.",
                createdAt = now - (3 * oneHour)
            ),
            ReportEntity(
                reportType = "STOLEN",
                brand = "Apple (آيفون)",
                model = "iPhone 15 Pro Max (256GB)",
                imei1 = "356789104812390",
                imei2 = "",
                serialNumber = "F2LWM9PQ0L",
                color = "تيتانيوم طبيعي",
                distinctiveMarks = "شاشة حماية زجاجية خصوصية (لقافة) مخدوشة من الأسفل",
                governorate = "عدن",
                district = "الشيخ عثمان",
                incidentLocation = "سوق الجوالات - جوار فرع بنك الكريمي",
                incidentTimestamp = now - (6 * oneHour),
                contactName = "طارق عمر المحضار",
                primaryPhone = "733456789",
                whatsappNumber = "733456789",
                rewardAmount = 200000,
                policeReportNumber = "عد/298 قسم الشيخ عثمان",
                isRecovered = false,
                additionalNotes = "يوجد حساب iCloud مقفل Lost Mode، مكافأة نقدية فورية لمن يعيده أو يدلي بمكانه.",
                createdAt = now - (6 * oneHour)
            ),
            ReportEntity(
                reportType = "LOST",
                brand = "Xiaomi / Redmi (شاومي)",
                model = "Redmi Note 13 Pro 4G",
                imei1 = "869402058319204",
                imei2 = "869402058319212",
                serialNumber = "319204XM",
                color = "أزرق ثلجي",
                distinctiveMarks = "جراب سيليكون أسود وفيه بطاقة شخصية في الظهر",
                governorate = "تعز",
                district = "صالة",
                incidentLocation = "شارع جمال - باص الأجرة المتجه للحوض",
                incidentTimestamp = now - (1 * oneDay),
                contactName = "مروان سعيد عبد الله",
                primaryPhone = "711987654",
                whatsappNumber = "711987654",
                rewardAmount = 50000,
                policeReportNumber = "",
                isRecovered = false,
                additionalNotes = "نسيته في باص أجرة، يحتوي على صور عائلية هامة ومستندات عمل.",
                createdAt = now - (1 * oneDay)
            ),
            ReportEntity(
                reportType = "FOUND",
                brand = "Infinix (إنفينيكس)",
                model = "Hot 40 Pro",
                imei1 = "358910459201483",
                imei2 = "",
                serialNumber = "INF40912",
                color = "ذهبي متدرج",
                distinctiveMarks = "شاشة سليمة بدون كفر وخلفية الشاشة صورة جامع الصالح",
                governorate = "إب",
                district = "المشنة",
                incidentLocation = "شارع العدين - تم العثور عليه في كافتيريا السلام",
                incidentTimestamp = now - (2 * oneDay),
                contactName = "محل الأندلس لخدمات الجوال (المهندس عادل)",
                primaryPhone = "773112233",
                whatsappNumber = "773112233",
                rewardAmount = 0,
                policeReportNumber = "",
                isRecovered = false,
                additionalNotes = "الجهاز مقفل بنمط شاشة، موجود في المحل ومن يثبت ملكيته بالكرتون أو الرمز يستلمه فوراً.",
                createdAt = now - (2 * oneDay)
            ),
            ReportEntity(
                reportType = "STOLEN",
                brand = "Samsung (سامسونج)",
                model = "Galaxy A54 5G",
                imei1 = "359102485910243",
                imei2 = "359102485910250",
                serialNumber = "SM-A546B-YEM",
                color = "بنفسجي رائع (Awesome Violet)",
                distinctiveMarks = "ملصق حماية حراري في الظهر مائل قليلاً",
                governorate = "حضرموت (المكلا)",
                district = "المكلا",
                incidentLocation = "الشرج - شارع هايبر المستهلك",
                incidentTimestamp = now - (4 * oneDay),
                contactName = "سالم باوزير",
                primaryPhone = "770987123",
                whatsappNumber = "770987123",
                rewardAmount = 80000,
                policeReportNumber = "حض/553",
                isRecovered = true, // Sample of recovered phone to inspire hope
                additionalNotes = "تم استرجاع الجهاز بحمد الله وتكاتف أصحاب المحلات في سوق الشرج!",
                createdAt = now - (4 * oneDay)
            )
        )

        reportDao.insertAll(initialReports)

        val initialAlerts = listOf(
            AlertEntity(
                reportId = 1,
                title = "🚨 بلاغ سرقة عاجل: Galaxy S23 Ultra",
                message = "صنعاء - سوق باب السلام (أمام محلات البرق). مكافأة 150,000 ر.ي",
                governorate = "صنعاء (الأمانة)",
                deviceModel = "Samsung Galaxy S23 Ultra",
                imeiSnippet = "3548****921",
                timestamp = now - (3 * oneHour),
                isRead = false,
                alertType = "URGENT_THEFT"
            ),
            AlertEntity(
                reportId = 2,
                title = "🚨 بلاغ سرقة عاجل: iPhone 15 Pro Max",
                message = "عدن - سوق الشيخ عثمان للجوالات. مكافأة 200,000 ر.ي",
                governorate = "عدن",
                deviceModel = "Apple iPhone 15 Pro Max",
                imeiSnippet = "3567****390",
                timestamp = now - (6 * oneHour),
                isRead = false,
                alertType = "URGENT_THEFT"
            ),
            AlertEntity(
                reportId = 4,
                title = "📢 تم العثور على جهاز: Infinix Hot 40 Pro",
                message = "إب - شارع العدين، موجود لدى محل الأندلس بانتظار صاحبه مع الكرتون",
                governorate = "إب",
                deviceModel = "Infinix Hot 40 Pro",
                imeiSnippet = "3589****483",
                timestamp = now - (2 * oneDay),
                isRead = true,
                alertType = "DEVICE_FOUND"
            ),
            AlertEntity(
                reportId = 5,
                title = "🎉 تم بحمد الله استرجاع الهاتف بنجاح!",
                message = "حضرموت (المكلا) - استعادة هاتف Galaxy A54 بفضل وعي أصحاب المحلات",
                governorate = "حضرموت (المكلا)",
                deviceModel = "Samsung Galaxy A54 5G",
                imeiSnippet = "3591****243",
                timestamp = now - (1 * oneDay),
                isRead = true,
                alertType = "RECOVERY"
            )
        )
        alertDao.insertAll(initialAlerts)
    }
}
