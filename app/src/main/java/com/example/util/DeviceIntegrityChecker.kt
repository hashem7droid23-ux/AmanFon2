package com.example.util

import android.content.Context
import android.os.Build
import java.io.File

/**
 * فاحص النزاهة والحماية من الاختراق والتجذير (Root & Tamper Detection)
 * يكشف إذا كان الجهاز مكسور الحماية (Rooted) أو تحت تأثير أدوات التجسس والاعتراض (Hooking).
 */
object DeviceIntegrityChecker {

    data class IntegrityReport(
        val isDeviceCompromised: Boolean,
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val hasHookingFramework: Boolean,
        val securityStatusMessage: String
    )

    private val KNOWN_ROOT_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/system/xbin/magisk",
        "/sbin/magisk"
    )

    private val DANGEROUS_PACKAGES = arrayOf(
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "de.robv.android.xposed.installer"
    )

    /**
     * فحص شامل وسريع لنزاهة الجهاز وأمانه.
     */
    fun performSecurityAudit(context: Context): IntegrityReport {
        val rootFilesFound = checkRootBinaries()
        val testKeysDetected = checkTestKeys()
        val dangerousPackages = checkDangerousPackages(context)
        val hookingDetected = checkHookingFrameworks()

        val isRooted = rootFilesFound || testKeysDetected || dangerousPackages
        val isCompromised = isRooted || hookingDetected

        val message = when {
            hookingDetected -> "⚠️ تحذير أمني: تم رصد برمجيات تعديل واعتراض للذاكرة (Hooking)!"
            isRooted -> "⚠️ تنبيه: تم رصد صلاحيات الروت (SuperUser) على الهاتف. يُنصح بعدم حفظ بيانات حساسة."
            else -> "🛡️ نظام الهاتف آمن وخالٍ من برمجيات التجسس أو التجذير غير المصرح بها."
        }

        return IntegrityReport(
            isDeviceCompromised = isCompromised,
            isRooted = isRooted,
            isEmulator = isRunningOnEmulator(),
            hasHookingFramework = hookingDetected,
            securityStatusMessage = message
        )
    }

    private fun checkRootBinaries(): Boolean {
        return try {
            for (path in KNOWN_ROOT_PATHS) {
                if (File(path).exists()) return true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun checkTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkDangerousPackages(context: Context): Boolean {
        val pm = context.packageManager
        for (pkg in DANGEROUS_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (_: Exception) {}
        }
        return false
    }

    private fun checkHookingFrameworks(): Boolean {
        return try {
            // التحقق من وجود Frida أو Xposed بالذاكرة
            val stackTrace = Thread.currentThread().stackTrace
            for (element in stackTrace) {
                val className = element.className.lowercase()
                if (className.contains("xposed") || className.contains("frida") || className.contains("substrate")) {
                    return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun isRunningOnEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }
}
