package com.example.util

import org.junit.Assert.*
import org.junit.Test

class ReportPolicyTest {
    @Test fun imeiMustHaveExactlyFifteenAsciiDigits() {
        assertTrue(ReportPolicy.isCompleteImei("354892110485921"))
        assertFalse(ReportPolicy.isCompleteImei("35489211048592"))
        assertFalse(ReportPolicy.isCompleteImei("3548921104859210"))
        assertFalse(ReportPolicy.isCompleteImei("35489211048592x"))
        assertFalse(ReportPolicy.isCompleteImei("٣٥٤٨٩٢١١٠٤٨٥٩٢١"))
    }
    @Test fun foundAndRecoveredAreNotActiveTheftWarnings() {
        assertTrue(ReportPolicy.isActiveRisk("STOLEN", false))
        assertTrue(ReportPolicy.isActiveRisk("LOST", false))
        assertFalse(ReportPolicy.isActiveRisk("FOUND", false))
        assertFalse(ReportPolicy.isActiveRisk("STOLEN", true))
        assertFalse(ReportPolicy.isActiveRisk("LOST", true))
    }
    @Test fun managementRequiresSignedInOwnerOrAdmin() {
        assertTrue(ReportPolicy.canManage("owner", "owner", false))
        assertTrue(ReportPolicy.canManage("owner", "admin", true))
        assertFalse(ReportPolicy.canManage("owner", "stranger", false))
        assertFalse(ReportPolicy.canManage("", "owner", false))
        assertFalse(ReportPolicy.canManage("owner", null, true))
    }
}
