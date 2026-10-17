package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.ReportEntity
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val reportId: Long,
    val title: String,
    val message: String,
    val brand: String,
    val model: String,
    val governorate: String,
    val color: String = "غير محدد",
    val matchedCriterion: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SearchWatchCriteria(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val brand: String = "",
    val model: String = "",
    val governorate: String = "",
    val imeiPrefix: String = "",
    val isEnabled: Boolean = true
)

object InAppNotificationManager {

    private val _currentInAppNotification = MutableStateFlow<InAppNotification?>(null)
    val currentInAppNotification: StateFlow<InAppNotification?> = _currentInAppNotification.asStateFlow()

    private val _notificationsHistory = MutableStateFlow<List<InAppNotification>>(emptyList())
    val notificationsHistory: StateFlow<List<InAppNotification>> = _notificationsHistory.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    // Default watch criteria preloaded for demonstration and immediate functionality
    private val _savedCriteriaList = MutableStateFlow<List<SearchWatchCriteria>>(
        listOf(
            SearchWatchCriteria(
                id = "watch_iphone_sanaa",
                label = "مراقبة هواتف آيفون (صنعاء)",
                brand = "Apple",
                model = "iPhone",
                governorate = "صنعاء",
                isEnabled = true
            ),
            SearchWatchCriteria(
                id = "watch_samsung_galaxy",
                label = "مراقبة هواتف سامسونج جالاكسي",
                brand = "Samsung",
                model = "Galaxy",
                governorate = "",
                isEnabled = true
            )
        )
    )
    val savedCriteriaList: StateFlow<List<SearchWatchCriteria>> = _savedCriteriaList.asStateFlow()

    init {
        // Initialize FCM Registration and subscribe to general alerts topic
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _fcmToken.value = task.result
                }
            }
            FirebaseMessaging.getInstance().subscribeToTopic("urgent_theft_yemen")
            FirebaseMessaging.getInstance().subscribeToTopic("all_reports_broadcast")
        } catch (_: Exception) {
            // Graceful fallback if Google Play services is absent in some emulator configurations
        }
    }

    fun updateFcmToken(token: String) {
        _fcmToken.value = token
    }

    fun addWatchCriteria(criteria: SearchWatchCriteria) {
        _savedCriteriaList.value = listOf(criteria) + _savedCriteriaList.value
        // Also subscribe to brand-specific FCM topic if available
        if (criteria.brand.isNotBlank()) {
            val topic = "brand_" + criteria.brand.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
            try {
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
            } catch (_: Exception) {}
        }
    }

    fun removeWatchCriteria(id: String) {
        _savedCriteriaList.value = _savedCriteriaList.value.filter { it.id != id }
    }

    fun toggleWatchCriteria(id: String) {
        _savedCriteriaList.value = _savedCriteriaList.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun triggerInAppNotification(notification: InAppNotification, context: Context? = null) {
        _currentInAppNotification.value = notification
        _notificationsHistory.value = listOf(notification) + _notificationsHistory.value

        // Vibrate to provide tactile feedback for the in-app alert
        context?.let { ctx ->
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.let { v ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 200), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(longArrayOf(0, 150, 100, 200), -1)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun dismissCurrent() {
        _currentInAppNotification.value = null
    }

    fun clearHistory() {
        _notificationsHistory.value = emptyList()
    }

    /**
     * Checks if a new report matches any saved watch criteria or current search keyword.
     * If matched, triggers the in-app notification banner and updates history.
     */
    fun checkAndNotifyIfMatches(
        report: ReportEntity,
        context: Context?,
        activeSearchQuery: String = ""
    ): Boolean {
        // 1. Check against active saved watch criteria
        for (criteria in _savedCriteriaList.value) {
            if (!criteria.isEnabled) continue

            val matchBrand = criteria.brand.isBlank() ||
                    report.brand.contains(criteria.brand, ignoreCase = true)
            val matchModel = criteria.model.isBlank() ||
                    report.model.contains(criteria.model, ignoreCase = true)
            val matchGov = criteria.governorate.isBlank() ||
                    report.governorate.equals(criteria.governorate, ignoreCase = true)
            val matchImei = criteria.imeiPrefix.isBlank() ||
                    report.imei1.startsWith(criteria.imeiPrefix)

            if (matchBrand && matchModel && matchGov && matchImei) {
                val notification = InAppNotification(
                    reportId = report.id,
                    title = "🚨 تطابق مواصفات: ${report.brand} ${report.model}",
                    message = "تم إضافة بلاغ جديد مطابق لمعيار المراقبة [${criteria.label}] في محافظة ${report.governorate}!",
                    brand = report.brand,
                    model = report.model,
                    governorate = report.governorate,
                    color = report.color,
                    matchedCriterion = criteria.label
                )
                triggerInAppNotification(notification, context)
                return true
            }
        }

        // 2. Check against active search query currently typed by the user in the app
        if (activeSearchQuery.isNotBlank() && activeSearchQuery.length >= 3) {
            val q = activeSearchQuery.trim()
            val matchesSearch = report.model.contains(q, ignoreCase = true) ||
                    report.brand.contains(q, ignoreCase = true) ||
                    report.imei1.contains(q)

            if (matchesSearch) {
                val notification = InAppNotification(
                    reportId = report.id,
                    title = "🔍 بلاغ مطابق لبحثك الحالي: ${report.brand} ${report.model}",
                    message = "تم إضافة بلاغ الآن يطابق كلمة البحث الحالية [${activeSearchQuery}] في محافظة ${report.governorate}",
                    brand = report.brand,
                    model = report.model,
                    governorate = report.governorate,
                    color = report.color,
                    matchedCriterion = "بحث: $activeSearchQuery"
                )
                triggerInAppNotification(notification, context)
                return true
            }
        }

        return false
    }

    /**
     * Simulation method allowing the user to immediately test and preview
     * the in-app notification banner as if received from Firebase Cloud Messaging.
     */
    fun simulateFcmMatchedNotification(context: Context?, customModel: String = "iPhone 15 Pro", customGov: String = "صنعاء") {
        val notification = InAppNotification(
            reportId = 101L,
            title = "🔔 إشعار FCM: تطابق مواصفات هاتف مراقب!",
            message = "وصل تنبيه سحابي عبر FCM بإضافة بلاغ لهاتف [$customModel] في محافظة [$customGov] مطابق لمواصفات بحثك!",
            brand = if (customModel.contains("iPhone", ignoreCase = true)) "Apple" else "Samsung",
            model = customModel,
            governorate = customGov,
            color = "تيتانيوم طبيعي",
            matchedCriterion = "تنبيه FCM السحابي المباشر"
        )
        triggerInAppNotification(notification, context)
    }
}
