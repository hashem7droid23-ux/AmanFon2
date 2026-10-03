package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/** Remote, admin-controlled settings stored in Firestore config/config. */
data class AppRemoteConfig(
    val moderators: List<String> = emptyList(),
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val reportsPaused: Boolean = false,
    val announcement: String = "",
    val pinnedImei: String = "",
    val hiddenImeis: List<String> = emptyList()
)

data class CloudBan(
    val key: String,
    val identifier: String,
    val reason: String,
    val bannedBy: String,
    val bannedAt: Long
)

data class AdminLog(
    val id: String,
    val action: String,
    val target: String,
    val details: String,
    val by: String,
    val at: Long
)

/**
 * Cloud side of the supervisor tools: bans, moderators team, remote app control and audit log.
 * Every write here is also enforced server-side by firestore.rules.
 */
object AdminCloud {
    private const val TAG = "AdminCloud"
    private const val CONFIG = "config"
    private const val CONFIG_DOC = "config"

    private var db: FirebaseFirestore? = null

    private val _config = MutableStateFlow(AppRemoteConfig())
    val config: StateFlow<AppRemoteConfig> = _config.asStateFlow()

    private val _bans = MutableStateFlow<List<CloudBan>>(emptyList())
    val bans: StateFlow<List<CloudBan>> = _bans.asStateFlow()

    private val _logs = MutableStateFlow<List<AdminLog>>(emptyList())
    val logs: StateFlow<List<AdminLog>> = _logs.asStateFlow()

    private var configReg: ListenerRegistration? = null
    private var bansReg: ListenerRegistration? = null
    private var logsReg: ListenerRegistration? = null

    fun init(context: Context) {
        if (db != null) return
        db = try {
            FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        } catch (e: Exception) {
            Log.w(TAG, "Firestore unavailable: ${e.message}")
            null
        }
        listenConfig()
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) listenBans() else {
                bansReg?.remove(); bansReg = null; _bans.value = emptyList()
                stopAdminListeners()
            }
        }
    }

    private fun listenConfig() {
        val d = db ?: return
        configReg?.remove()
        configReg = d.collection(CONFIG).document(CONFIG_DOC).addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            _config.value = AppRemoteConfig(
                moderators = (snap.get("moderators") as? List<*>)?.mapNotNull { (it as? String)?.trim()?.lowercase() } ?: emptyList(),
                maintenanceMode = snap.getBoolean("maintenanceMode") ?: false,
                maintenanceMessage = snap.getString("maintenanceMessage") ?: "",
                reportsPaused = snap.getBoolean("reportsPaused") ?: false,
                announcement = snap.getString("announcement") ?: "",
                pinnedImei = snap.getString("pinnedImei") ?: "",
                hiddenImeis = (snap.get("hiddenImeis") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )
        }
    }

    private fun listenBans() {
        val d = db ?: return
        if (bansReg != null) return
        bansReg = d.collection("bans").addSnapshotListener { snap, e ->
            if (e != null || snap == null) return@addSnapshotListener
            _bans.value = snap.documents.map { doc ->
                CloudBan(
                    key = doc.id,
                    identifier = doc.getString("identifier") ?: doc.id,
                    reason = doc.getString("reason") ?: "",
                    bannedBy = doc.getString("bannedBy") ?: "",
                    bannedAt = doc.getTimestamp("bannedAt")?.toDate()?.time ?: 0L
                )
            }.sortedByDescending { it.bannedAt }
        }
    }

    /** Audit log is visible to admins only (rules), so only start it for them. */
    fun startAdminListeners() {
        val d = db ?: return
        if (logsReg != null) return
        logsReg = d.collection("admin_logs")
            .orderBy("at", Query.Direction.DESCENDING)
            .limit(150)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener
                _logs.value = snap.documents.map { doc ->
                    AdminLog(
                        id = doc.id,
                        action = doc.getString("action") ?: "",
                        target = doc.getString("target") ?: "",
                        details = doc.getString("details") ?: "",
                        by = doc.getString("by") ?: "",
                        at = doc.getTimestamp("at")?.toDate()?.time ?: 0L
                    )
                }
            }
    }

    fun stopAdminListeners() {
        logsReg?.remove(); logsReg = null; _logs.value = emptyList()
    }

    /** Phone -> last 9 digits, email -> lowercase. Same key is checked by firestore.rules. */
    fun banKey(raw: String): String {
        val t = raw.trim()
        if (t.contains("@")) return t.lowercase()
        val digits = t.filter { it.isDigit() }
        if (digits.length >= 9) return digits.takeLast(9)
        return t.lowercase().replace("/", "_").ifBlank { "_" }
    }

    private fun me(): String = FirebaseAuth.getInstance().currentUser?.email?.lowercase() ?: "unknown"

    private fun requireDb(): FirebaseFirestore = db ?: throw IllegalStateException("قاعدة البيانات غير متاحة")

    private fun friendly(e: Throwable): String = when {
        e.message?.contains("PERMISSION_DENIED", true) == true ->
            "رفضت قاعدة البيانات العملية. تأكد إن بريدك مفعّل وإن قواعد Firebase الجديدة منشورة"
        e.message?.contains("UNAVAILABLE", true) == true -> "لا يوجد اتصال بالإنترنت"
        else -> e.message ?: "تعذر تنفيذ العملية"
    }

    private suspend fun op(block: suspend () -> Unit): Result<Unit> = try {
        block(); Result.success(Unit)
    } catch (e: Exception) {
        Log.w(TAG, "admin op failed", e)
        Result.failure(Exception(friendly(e)))
    }

    suspend fun log(action: String, target: String, details: String = "") {
        val d = db ?: return
        try {
            d.collection("admin_logs").add(
                hashMapOf(
                    "action" to action,
                    "target" to target.take(200),
                    "details" to details.take(500),
                    "by" to me(),
                    "at" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "log failed: ${e.message}")
        }
    }

    // ----- Bans -----
    suspend fun ban(identifier: String, reason: String): Result<Unit> = op {
        val key = banKey(identifier)
        requireDb().collection("bans").document(key).set(
            hashMapOf(
                "identifier" to identifier.trim(),
                "reason" to reason.ifBlank { "مخالفة سياسات منظومة أمان فون" },
                "bannedBy" to me(),
                "bannedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        log("حظر", identifier.trim(), reason)
    }

    suspend fun unban(key: String, identifier: String = key): Result<Unit> = op {
        requireDb().collection("bans").document(key).delete().await()
        log("فك حظر", identifier)
    }

    fun isBannedLocal(identifier: String): Boolean {
        if (identifier.isBlank()) return false
        val key = banKey(identifier)
        return _bans.value.any { it.key == key }
    }

    // ----- Config -----
    private suspend fun mergeConfig(values: Map<String, Any>) {
        val data = HashMap(values)
        data["updatedAt"] = FieldValue.serverTimestamp()
        requireDb().collection(CONFIG).document(CONFIG_DOC).set(data, SetOptions.merge()).await()
    }

    suspend fun addModerator(email: String): Result<Unit> = op {
        val e = email.trim().lowercase()
        require(e.contains("@")) { "اكتب بريداً صحيحاً" }
        mergeConfig(mapOf("moderators" to FieldValue.arrayUnion(e)))
        log("إضافة مشرف", e)
    }

    suspend fun removeModerator(email: String): Result<Unit> = op {
        mergeConfig(mapOf("moderators" to FieldValue.arrayRemove(email.trim().lowercase())))
        log("إزالة مشرف", email)
    }

    suspend fun setMaintenance(enabled: Boolean, message: String): Result<Unit> = op {
        mergeConfig(mapOf("maintenanceMode" to enabled, "maintenanceMessage" to message.trim()))
        log(if (enabled) "تفعيل وضع الصيانة" else "إيقاف وضع الصيانة", "التطبيق", message)
    }

    suspend fun setReportsPaused(paused: Boolean): Result<Unit> = op {
        mergeConfig(mapOf("reportsPaused" to paused))
        log(if (paused) "إيقاف نشر البلاغات" else "استئناف نشر البلاغات", "البلاغات")
    }

    suspend fun setAnnouncement(text: String): Result<Unit> = op {
        mergeConfig(mapOf("announcement" to text.trim().take(300)))
        log(if (text.isBlank()) "حذف الإعلان" else "نشر إعلان", "الرئيسية", text)
    }

    suspend fun setPinned(imei: String, label: String): Result<Unit> = op {
        mergeConfig(mapOf("pinnedImei" to imei))
        log(if (imei.isBlank()) "إلغاء التثبيت" else "تثبيت بلاغ", label)
    }

    suspend fun setHidden(imei: String, hidden: Boolean, label: String): Result<Unit> = op {
        mergeConfig(mapOf("hiddenImeis" to if (hidden) FieldValue.arrayUnion(imei) else FieldValue.arrayRemove(imei)))
        log(if (hidden) "إخفاء بلاغ" else "إظهار بلاغ", label)
    }
}
