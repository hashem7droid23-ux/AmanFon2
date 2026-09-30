package com.example.service

import android.util.Log
import com.example.util.InAppNotification
import com.example.util.InAppNotificationManager
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AmanFirebaseMessagingService : FirebaseMessagingService() {

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token registered: $token")
        InAppNotificationManager.updateFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val reportId = data["report_id"]?.toLongOrNull() ?: 101L
        val brand = data["brand"] ?: "هاتف ذكي"
        val model = data["model"] ?: "موديل غير محدد"
        val governorate = data["governorate"] ?: "صنعاء"
        val color = data["color"] ?: "غير محدد"
        val title = remoteMessage.notification?.title ?: data["title"] ?: "🚨 تنبيه هاتف مطابق للمواصفات!"
        val body = remoteMessage.notification?.body ?: data["message"] ?: "تم إضافة بلاغ جديد لهاتف $brand $model في $governorate"

        // 1. Trigger the In-App Notification banner inside the application
        val inAppAlert = InAppNotification(
            reportId = reportId,
            title = title,
            message = body,
            brand = brand,
            model = model,
            governorate = governorate,
            color = color,
            matchedCriterion = "إشعار سحابي FCM"
        )
        InAppNotificationManager.triggerInAppNotification(inAppAlert, applicationContext)

        // 2. Also trigger the system tray notification via NotificationHelper
        NotificationHelper.showUrgentTheftAlert(
            context = applicationContext,
            reportId = reportId,
            title = title,
            message = body,
            governorate = governorate
        )
    }

    companion object {
        private const val TAG = "AmanFCMService"
    }
}
