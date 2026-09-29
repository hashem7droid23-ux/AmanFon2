package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_URGENT_THEFT = "channel_urgent_theft"
    const val CHANNEL_FOUND_DEVICE = "channel_found_device"
    const val CHANNEL_COMMUNITY_BROADCAST = "channel_community_broadcast"

    const val EXTRA_TARGET_SCREEN = "extra_target_screen"
    const val EXTRA_REPORT_ID = "extra_report_id"

    fun setupNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Urgent Theft Alerts Channel
            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT_THEFT,
                "تنبيهات السرقات العاجلة (اليمن)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية بالهواتف المسروقة حديثاً في منطقتك ومحلات الهواتف"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
            }

            // 2. Found Devices Channel
            val foundChannel = NotificationChannel(
                CHANNEL_FOUND_DEVICE,
                "أجهزة تم العثور عليها",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات بالأجهزة المعثور عليها بانتظار أصحابها"
                enableLights(true)
                lightColor = Color.GREEN
            }

            // 3. Community Broadcasts Channel
            val communityChannel = NotificationChannel(
                CHANNEL_COMMUNITY_BROADCAST,
                "تنبيهات مجتمع الهواتف والتعاميم",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تعاميم أمنية وتنبيهات عامة لمحلات الهواتف والمستخدمين"
            }

            notificationManager.createNotificationChannels(
                listOf(urgentChannel, foundChannel, communityChannel)
            )
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showUrgentTheftAlert(
        context: Context,
        reportId: Long,
        title: String,
        message: String,
        governorate: String
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_SCREEN, "details")
            putExtra(EXTRA_REPORT_ID, reportId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reportId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_URGENT_THEFT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 $title")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n📍 المحافظة: $governorate\n⚡ تحذير: يرجى من جميع المحلات والمواطنين الامتناع عن شراء هذا الجهاز.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(0xFFD32F2F.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(reportId.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission might be revoked at runtime
        }
    }

    fun showGeneralAlert(
        context: Context,
        id: Int,
        title: String,
        message: String,
        channelId: String = CHANNEL_COMMUNITY_BROADCAST
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_SCREEN, "alerts")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(0xFF1E3E62.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        }
    }
}
