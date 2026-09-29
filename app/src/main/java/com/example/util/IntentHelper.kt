package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.ReportEntity
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object IntentHelper {

    fun normalizeYemenPhone(phone: String): String {
        var clean = phone.filter { it.isDigit() }
        if (clean.startsWith("00967")) {
            clean = clean.removePrefix("00967")
        } else if (clean.startsWith("967")) {
            clean = clean.removePrefix("967")
        } else if (clean.startsWith("0")) {
            clean = clean.removePrefix("0")
        }
        return "967$clean"
    }

    fun openWhatsApp(context: Context, phone: String, message: String) {
        val yemenNumber = normalizeYemenPhone(phone)
        val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        val url = "https://wa.me/$yemenNumber?text=$encodedMessage"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تطبيق واتساب غير مثبت على الجهاز", Toast.LENGTH_SHORT).show()
        }
    }

    fun makeCall(context: Context, phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الهاتف", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareReport(context: Context, report: ReportEntity) {
        val statusText = when (report.reportType) {
            "STOLEN" -> "🚨 بلاغ عن هاتف مسروق"
            "LOST" -> "⚠️ بلاغ عن هاتف مفقود"
            else -> "📢 إعلان عن هاتف تم العثور عليه"
        }

        val rewardText = if (report.rewardAmount > 0) {
            "\n💰 مكافأة مالية لمن يعثر عليه أو يدلي بمعلومات: ${"%,d".format(report.rewardAmount)} ريال يمني"
        } else ""

        val policeText = if (report.policeReportNumber.isNotBlank()) {
            "\n👮 رقم بلاغ الشرطة والبحث الجنائي: ${report.policeReportNumber}"
        } else ""

        val shareBody = """
            $statusText
            ----------------------------
            📱 الجهاز: ${report.brand} ${report.model}
            🎨 اللون: ${report.color}
            🔢 رقم IMEI الأول: ${report.maskedImei}
            📍 مكان الحادث: ${report.governorate} - ${report.district.ifBlank { "" }} (${report.incidentLocation})
            👤 صاحب البلاغ: ${report.contactName}
            📞 هاتف التواصل: ${report.primaryPhone}
            💬 واتساب: ${report.whatsappNumber}$rewardText$policeText
            📝 تفاصيل وعلامات: ${report.distinctiveMarks.ifBlank { "لا توجد علامات إضافية" }}
            
            ⚠️ تنبيه لجميع محلات الهواتف ومهندسي الصيانة في اليمن: يرجى فحص رقم IMEI والتأكد قبل الشراء أو الفرمتة.
            تم النشر عبر تطبيق: مفقود اليمن (المنظومة الوطنية لمكافحة سرقة الهواتف)
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareBody)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val shareIntent = Intent.createChooser(sendIntent, "مشاركة بلاغ الهاتف في اليمن")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }
}
