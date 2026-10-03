package com.example.util

/** Shared, Android-free checks used by publication and authoritative IMEI queries. */
object ReportPolicy {
    fun isCompleteImei(value: String): Boolean = value.length == 15 && value.all { it in '0'..'9' }
    fun isActiveRisk(type: String, recovered: Boolean): Boolean = !recovered && type in setOf("STOLEN", "LOST")
    fun canManage(ownerId: String, currentId: String?, isAdmin: Boolean): Boolean =
        !currentId.isNullOrBlank() && (isAdmin || (ownerId.isNotBlank() && ownerId == currentId))
}
