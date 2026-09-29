package com.example

import com.example.data.model.YemenLocations
import com.example.util.ImeiValidator
import com.example.util.IntentHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testImeiCleaner() {
        assertEquals("354892110485921", ImeiValidator.clean("354-892 11048-5921"))
    }

    @Test
    fun testImeiMasking() {
        val masked = ImeiValidator.mask("354892110485921")
        assertTrue(masked.startsWith("3548"))
        assertTrue(masked.endsWith("921"))
        assertTrue(masked.contains("*"))
    }

    @Test
    fun testYemenPhoneNormalization() {
        assertEquals("967777123456", IntentHelper.normalizeYemenPhone("777123456"))
        assertEquals("967777123456", IntentHelper.normalizeYemenPhone("0777123456"))
        assertEquals("967777123456", IntentHelper.normalizeYemenPhone("+967777123456"))
    }

    @Test
    fun testYemenGovernoratesListNotEmpty() {
        assertTrue(YemenLocations.GOVERNORATES.contains("صنعاء (الأمانة)"))
        assertTrue(YemenLocations.GOVERNORATES.contains("عدن"))
        assertTrue(YemenLocations.GOVERNORATES.contains("تعز"))
        assertTrue(YemenLocations.GOVERNORATES.contains("حضرموت (المكلا)"))
    }
}
