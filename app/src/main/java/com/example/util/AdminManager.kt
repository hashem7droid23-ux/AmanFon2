package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.AlertEntity
import com.example.data.remote.AdminCloud
import com.example.data.repository.PhoneTrackerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

data class BannedAccount(
    val id: String = java.util.UUID.randomUUID().toString(), // cloud key (phone last 9 digits / lowercase email)
    val identifier: String,  // as typed by the admin
    val reason: String,
    val bannedAt: Long = System.currentTimeMillis(),
    val bannedBy: String = ""
)

object AdminManager {

    const val SUPER_ADMIN_EMAIL = "hashem7droid23@gmail.com"
    private const val PREFS_NAME = "aman_admin_prefs"
    private const val KEY_BANNED_ACCOUNTS = "key_banned_accounts"
    private const val KEY_ADMIN_SIMULATION = "key_admin_simulation_active"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Any supervisor: the owner or a moderator added by the owner. */
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    /** Compatibility alias used across the UI for supervisor tools. */
    val isSuperAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    /** Owner only (م. هاشم): team management, maintenance, pausing reports. */
    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _bannedAccounts = MutableStateFlow<List<BannedAccount>>(emptyList())
    val bannedAccounts: StateFlow<List<BannedAccount>> = _bannedAccounts.asStateFlow()

    private var sharedPreferences: SharedPreferences? = null
    private var initialized = false

    private fun isSimulationActive(): Boolean {
        if (!BuildConfig.DEBUG) return false
        return sharedPreferences?.getBoolean(KEY_ADMIN_SIMULATION, false) ?: false
    }

    /** Only verified emails are trusted for admin access (rules require the same). */
    private fun trustedEmailOf(user: FirebaseUser?): String? {
        if (user == null || !user.isEmailVerified) return null
        return user.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    }

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs
        if (!BuildConfig.DEBUG && prefs.getBoolean(KEY_ADMIN_SIMULATION, false)) {
            prefs.edit().remove(KEY_ADMIN_SIMULATION).apply()
        }

        AdminCloud.init(context.applicationContext)
        val firestoreService = com.example.data.remote.FirestorePhoneService(context.applicationContext)

        // Cloud bans -> UI list
        scope.launch {
            AdminCloud.bans.collect { list ->
                _bannedAccounts.value = list.map {
                    BannedAccount(id = it.key, identifier = it.identifier, reason = it.reason, bannedAt = it.bannedAt, bannedBy = it.bannedBy)
                }
            }
        }
        // Moderators list changes -> re-evaluate
        scope.launch {
            AdminCloud.config.collect { evaluate(FirebaseAuth.getInstance().currentUser) }
        }

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            scope.launch { verifyAndSetAdminState(auth.currentUser, firestoreService) }
        }
        scope.launch { verifyAndSetAdminState(FirebaseAuth.getInstance().currentUser, firestoreService) }
    }

    private fun evaluate(user: FirebaseUser?) {
        if (isSimulationActive()) {
            _isOwner.value = true; _isAdmin.value = true
            AdminCloud.startAdminListeners()
            return
        }
        val email = trustedEmailOf(user)
        val owner = email != null && email == SUPER_ADMIN_EMAIL
        val moderator = email != null && email in AdminCloud.config.value.moderators
        _isOwner.value = owner
        _isAdmin.value = owner || moderator
        if (owner || moderator) AdminCloud.startAdminListeners() else AdminCloud.stopAdminListeners()
    }

    suspend fun verifyAndSetAdminState(
        user: FirebaseUser?,
        firestoreService: com.example.data.remote.FirestorePhoneService? = null
    ): Boolean {
        evaluate(user)
        if (_isOwner.value && !isSimulationActive()) {
            // Owner login: make sure config exists and migrate any old on-device bans to the cloud
            try { firestoreService?.getOrInitAdminEmail() } catch (_: Exception) {}
            migrateLocalBans()
        }
        return _isAdmin.value
    }

    private suspend fun migrateLocalBans() {
        val prefs = sharedPreferences ?: return
        val raw = prefs.getString(KEY_BANNED_ACCOUNTS, null) ?: return
        try {
            val json = if (raw.startsWith("[")) raw else AmanSecurityEngine.decrypt(raw)
            val arr = JSONArray(json)
            var allOk = true
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val r = AdminCloud.ban(o.optString("identifier"), o.optString("reason"))
                if (r.isFailure) allOk = false
            }
            if (allOk) prefs.edit().remove(KEY_BANNED_ACCOUNTS).apply()
        } catch (_: Exception) {
            prefs.edit().remove(KEY_BANNED_ACCOUNTS).apply()
        }
    }

    fun toggleAdminSimulation(enable: Boolean) {
        if (!BuildConfig.DEBUG) {
            sharedPreferences?.edit()?.remove(KEY_ADMIN_SIMULATION)?.apply()
            evaluate(FirebaseAuth.getInstance().currentUser)
            return
        }
        sharedPreferences?.edit()?.putBoolean(KEY_ADMIN_SIMULATION, enable)?.apply()
        evaluate(FirebaseAuth.getInstance().currentUser)
    }

    fun isUserAdmin(user: FirebaseUser? = FirebaseAuth.getInstance().currentUser): Boolean {
        if (_isAdmin.value) return true
        val email = trustedEmailOf(user) ?: return false
        return email == SUPER_ADMIN_EMAIL || email in AdminCloud.config.value.moderators
    }

    /** Phone (any format), email or name. Phones/emails are matched by the same key the rules use. */
    fun isBanned(identifier: String): Boolean {
        val clean = identifier.trim()
        if (clean.isBlank()) return false
        if (AdminCloud.isBannedLocal(clean)) return true
        return _bannedAccounts.value.any { it.identifier.equals(clean, ignoreCase = true) }
    }

    fun banAccount(identifier: String, reason: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (!isUserAdmin() || identifier.isBlank()) { onResult(false, "ليس لديك صلاحية"); return }
        scope.launch {
            val r = AdminCloud.ban(identifier, reason)
            onResult(r.isSuccess, r.exceptionOrNull()?.message)
        }
    }

    fun unbanAccount(id: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (!isUserAdmin()) { onResult(false, "ليس لديك صلاحية"); return }
        val ident = _bannedAccounts.value.firstOrNull { it.id == id }?.identifier ?: id
        scope.launch {
            val r = AdminCloud.unban(id, ident)
            onResult(r.isSuccess, r.exceptionOrNull()?.message)
        }
    }

    fun sendSupervisorBroadcast(
        title: String,
        message: String,
        isUrgent: Boolean,
        context: Context,
        repository: PhoneTrackerRepository
    ) {
        if (!isUserAdmin()) return

        val formattedTitle = "👑 تعميم المشرف العام: $title"
        val formattedMessage = "$message\n\n— إدارة منظومة أمان فون"

        val inApp = InAppNotification(
            reportId = 0L,
            title = formattedTitle,
            message = formattedMessage,
            brand = "منظومة أمان فون",
            model = "تعميم أمني رسمي",
            governorate = "الجمهورية اليمنية",
            color = "رسمي",
            matchedCriterion = "إشعار المشرف العام"
        )
        InAppNotificationManager.triggerInAppNotification(inApp, context)

        scope.launch {
            val alert = AlertEntity(
                reportId = 0L,
                title = formattedTitle,
                message = formattedMessage,
                governorate = "كافة المحافظات",
                deviceModel = "تعميم المشرف العام",
                imeiSnippet = "إدارة المنظومة",
                alertType = if (isUrgent) "URGENT_THEFT" else "SUPERVISOR_BROADCAST"
            )
            repository.insertSupervisorAlert(alert)
            AdminCloud.log("بث تعميم", title, message)
        }

        if (isUrgent) {
            NotificationHelper.showUrgentTheftAlert(
                context = context,
                reportId = 9999L,
                title = formattedTitle,
                message = formattedMessage,
                governorate = "اليمن"
            )
        } else {
            NotificationHelper.showGeneralAlert(
                context = context,
                id = 9999,
                title = formattedTitle,
                message = formattedMessage
            )
        }
    }
}
