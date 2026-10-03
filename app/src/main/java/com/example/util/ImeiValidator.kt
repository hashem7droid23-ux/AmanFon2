package com.example.util

object ImeiValidator {
    /** Convert decimal digits, including Arabic and Persian, to the canonical ASCII IMEI. */
    fun clean(raw: String): String = buildString {
        raw.forEach { char ->
            val digit = Character.digit(char, 10)
            if (digit in 0..9) append(('0'.code + digit).toChar())
        }
    }
    fun isValidLuhn(imei: String): Boolean {
        val value = clean(imei)
        if (!ReportPolicy.isCompleteImei(value)) return false
        var sum = 0
        value.forEachIndexed { index, char ->
            var digit = char - '0'
            if (index % 2 != 0) { digit *= 2; if (digit > 9) digit -= 9 }
            sum += digit
        }
        return sum % 10 == 0
    }
    fun isFormatValid(imei: String): Boolean = ReportPolicy.isCompleteImei(clean(imei))
    fun mask(imei: String): String {
        val value = clean(imei)
        return if (value.length >= 8) value.take(4) + "*".repeat((value.length - 7).coerceAtLeast(3)) + value.takeLast(3) else value
    }
}
