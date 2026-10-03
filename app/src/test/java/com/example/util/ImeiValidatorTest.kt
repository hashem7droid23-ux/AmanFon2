package com.example.util

import org.junit.Assert.*
import org.junit.Test

class ImeiValidatorTest {
    @Test fun normalizesArabicPersianAndFormattedDigits() {
        assertEquals("354892110485921", ImeiValidator.clean("٣٥٤٨٩٢١١٠٤٨٥٩٢١"))
        assertEquals("354892110485921", ImeiValidator.clean("۳۵۴۸۹۲۱۱۰۴۸۵۹۲۱"))
        assertEquals("354892110485921", ImeiValidator.clean("35 4892-110485921"))
    }
    @Test fun luhnWorksAndShortFormatsAreRejected() {
        assertTrue(ImeiValidator.isValidLuhn("490154203237518"))
        assertTrue(ImeiValidator.isValidLuhn("٤٩٠١٥٤٢٠٣٢٣٧٥١٨"))
        assertFalse(ImeiValidator.isValidLuhn("490154203237519"))
        assertFalse(ImeiValidator.isFormatValid("49015420323751"))
        assertFalse(ImeiValidator.isFormatValid("4901542032375180"))
        assertTrue(ImeiValidator.isFormatValid("490154203237518"))
    }
}
