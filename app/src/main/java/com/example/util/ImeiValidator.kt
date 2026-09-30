package com.example.util

object ImeiValidator {
    /**
     * Cleans an IMEI string by keeping only ASCII digits accepted by Luhn and the backend.
     */
    fun clean(raw: String): String {
        return raw.filter { it in '0'..'9' }
    }

    /**
     * Checks if the IMEI satisfies the 15-digit Luhn algorithm (mod 10).
     */
    fun isValidLuhn(imei: String): Boolean {
        val clean = clean(imei)
        if (clean.length != 15) return false

        var sum = 0
        for (i in 0 until 15) {
            var digit = clean[i] - '0'
            // For standard 15-digit IMEI, double the digits at odd indices (0-indexed: 1, 3, 5, 7, 9, 11, 13)
            if (i % 2 != 0) {
                digit *= 2
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10)
                }
            }
            sum += digit
        }
        return sum % 10 == 0
    }

    /**
     * Validates whether string is 14-16 digits.
     */
    fun isFormatValid(imei: String): Boolean {
        val clean = clean(imei)
        return clean.length in 14..16
    }

    /**
     * Masks the IMEI for privacy when displaying to public users (e.g. 354892******891)
     */
    fun mask(imei: String): String {
        val clean = clean(imei)
        return if (clean.length >= 8) {
            val prefix = clean.take(4)
            val suffix = clean.takeLast(3)
            val maskedPart = "*".repeat((clean.length - 7).coerceAtLeast(3))
            "$prefix$maskedPart$suffix"
        } else {
            clean
        }
    }
}
