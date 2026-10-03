package com.example.util

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Google code scanner (Play services): no camera permission needed, the camera UI is provided by Google.
 * Reads the IMEI / serial barcodes printed on the phone box, the SIM-tray sticker or the *#06# screen.
 */
object ImeiScanner {
    private val IMEI_REGEX = Regex("(?<!\\d)\\d{15}(?!\\d)")
    private val SERIAL_PREFIX = Regex("^(S/?N|SERIAL(\\s*NO)?|SN)[\\s:#.-]*", RegexOption.IGNORE_CASE)

    private fun start(context: Context, onRaw: (String) -> Unit, onError: (String) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        try {
            GmsBarcodeScanning.getClient(context, options)
                .startScan()
                .addOnSuccessListener { barcode -> onRaw(barcode.rawValue.orEmpty()) }
                .addOnCanceledListener { /* user closed the scanner */ }
                .addOnFailureListener {
                    onError("تعذر تشغيل الماسح، تأكد من الإنترنت وحدّث خدمات Google Play ثم حاول مجدداً")
                }
        } catch (e: Exception) {
            onError("الماسح غير متاح على هذا الجهاز")
        }
    }

    /** Scans an IMEI barcode and returns the 15-digit IMEI. */
    fun scan(context: Context, onResult: (String) -> Unit, onError: (String) -> Unit) {
        start(context, { raw ->
            val imei = IMEI_REGEX.find(raw)?.value
                ?: raw.filter { it.isDigit() }.takeIf { it.length in 14..15 }
            if (imei != null) onResult(imei)
            else onError("هذا الباركود ليس رقم IMEI، صوّر الباركود المكتوب بجانبه IMEI")
        }, onError)
    }

    /** Scans the serial-number barcode (S/N) and returns it in upper case. */
    fun scanSerial(context: Context, onResult: (String) -> Unit, onError: (String) -> Unit) {
        start(context, { raw ->
            val cleaned = raw.trim().replace(SERIAL_PREFIX, "")
                .filter { it.isLetterOrDigit() }
                .uppercase()
            when {
                cleaned.length !in 5..24 -> onError("لم نتعرف على رقم تسلسلي في هذا الباركود")
                IMEI_REGEX.matches(cleaned) -> onError("هذا رقم IMEI وليس الرقم التسلسلي (S/N)")
                else -> onResult(cleaned)
            }
        }, onError)
    }
}
