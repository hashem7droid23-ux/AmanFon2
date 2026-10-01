package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.AlertEntity
import com.example.data.model.ReportEntity
import com.example.data.repository.PhoneTrackerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BannedAccount(
    val id: String = UUID.randomUUID().toString(),
    val identifier: String, // Phone number, email, or user name
    val reason: String,
    val bannedAt: Long = System.currentTimeMillis(),
    val bannedBy: String = "المشرف العام (م. هاشم القديمي)"
)

object AdminManager {

    const val SUPER_ADMIN_EMAIL = "hashem7droid23@gmail.com"
    private const val PREFS_NAME = "aman_admin_prefs"
    private const val KEY_BANNED_ACCOUNTS = "key_banned_accounts"
    private const val KEY_ADMIN_SIMULATION = "key_admin_simulation_active"

    // The core isAdmin state flow explicitly requested by the user
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    // Compatibility alias for isSuperAdmin
    val isSuperAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _bannedAccounts = MutableStateFlow<List<BannedAccount>>(emptyList())
    val bannedAccounts: StateFlow<List<BannedAccount>> = _bannedAccounts.asStateFlow()

    private var sharedPreferences: SharedPreferences? = null
    private var cachedAdminEmail: String = SUPER_ADMIN_EMAIL

    /**
     * Admin simulation is a developer-only tool. It is ignored completely in release builds
     * so nobody can unlock supervisor controls on a production install.
     */
    private fun isSimulationActive(): Boolean {
        if (!BuildConfig.DEBUG) return false
        return sharedPreferences?.getBoolean(KEY_ADMIN_SIMULATION, false) ?: false
    }

    /** Only verified emails (e.g. Google Sign-In) are trusted for admin access. */
    private fun trustedEmailOf(user: FirebaseUser?): String? {
        if (user == null || !user.isEmailVerified) return null
        return user.email?.trim()?.takeIf { it.isNotBlank() }
    }

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs

        // Make sure a stale simulation flag can never survive into a release build
        if (!BuildConfig.DEBUG && prefs.getBoolean(KEY_ADMIN_SIMULATION, false)) {
            prefs.edit().remove(KEY_ADMIN_SIMULATION).apply()
        }

        loadBannedAccounts(prefs)

        val firestoreService = com.example.data.remote.FirestorePhoneService(context)

        // Listen to Firebase Auth state
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            CoroutineScope(Dispatchers.IO).launch {
                verifyAndSetAdminState(auth.currentUser, firestoreService)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            verifyAndSetAdminState(FirebaseAuth.getInstance().currentUser, firestoreService)
        }
    }

    /**
     * Checks current user's email against the 'adminEmail' field from Firestore 'config' document.
     * If matched, sets 'isAdmin' state to true to enable supervisor controls across the app.
     * Note: the real enforcement happens in firestore.rules; this only controls the UI.
     */
    suspend fun verifyAndSetAdminState(
        user: FirebaseUser?,
        firestoreService: com.example.data.remote.FirestorePhoneService? = null
    ): Boolean {
        if (isSimulationActive()) {
            _isAdmin.value = true
            return true
        }

        val userEmail = trustedEmailOf(user)
        if (userEmail == null) {
            _isAdmin.value = false
            return false
        }

        val configuredEmail = try {
            firestoreService?.getOrInitAdminEmail() ?: cachedAdminEmail
        } catch (_: Exception) {
            cachedAdminEmail
        }
        cachedAdminEmail = configuredEmail

        val isMatch = userEmail.equals(configuredEmail, ignoreCase = true) ||
                      userEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)

        _isAdmin.value = isMatch
        return isMatch
    }

    private fun evaluateAdminStatus(user: FirebaseUser?) {
        val email = trustedEmailOf(user)
        val isEmailMatch = email != null && (
            email.equals(cachedAdminEmail, ignoreCase = true) ||
            email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
        )
        _isAdmin.value = isEmailMatch || isSimulationActive()
    }

    /**
     * Enables admin simulation mode for testing or demonstration when Google Play services
     * login is not available on an emulator. DEBUG builds only.
     */
    fun toggleAdminSimulation(enable: Boolean) {
        if (!BuildConfig.DEBUG) {
            sharedPreferences?.edit()?.remove(KEY_ADMIN_SIMULATION)?.apply()
            evaluateAdminStatus(FirebaseAuth.getInstance().currentUser)
            return
        }
        sharedPreferences?.edit()?.putBoolean(KEY_ADMIN_SIMULATION, enable)?.apply()
        _isAdmin.value = enable
    }

    fun isUserAdmin(user: FirebaseUser? = FirebaseAuth.getInstance().currentUser): Boolean {
        if (_isAdmin.value) return true
        val email = trustedEmailOf(user) ?: return false
        return email.equals(cachedAdminEmail, ignoreCase = true) || email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
    }

    private fun loadBannedAccounts(prefs: SharedPreferences) {
        val rawData = prefs.getString(KEY_BANNED_ACCOUNTS, null)
        if (rawData != null) {
            try {
                // فك التشفير العتادي للبيانات المخزنة
                val jsonStr = if (rawData.startsWith("[") || rawData.startsWith("{")) {
                    rawData // Plaintext legacy migration
                } else {
                    AmanSecurityEngine.decrypt(rawData)
                }
                val array = JSONArray(jsonStr)
                val list = mutableListOf<BannedAccount>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        BannedAccount(
                            id = obj.getString("id"),
                            identifier = obj.getString("identifier"),
                            reason = obj.getString("reason"),
                            bannedAt = obj.getLong("bannedAt"),
                            bannedBy = obj.getString("bannedBy")
                        )
                    )
                }
                _bannedAccounts.value = list
                return
            } catch (_: Exception) {}
        }

        // Default empty list or sample demo ban if pristine
        _bannedAccounts.value = emptyList()
    }

    private fun saveBannedAccounts() {
        val prefs = sharedPreferences ?: return
        try {
            val array = JSONArray()
            for (item in _bannedAccounts.value) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("identifier", item.identifier)
                    put("reason", item.reason)
                    put("bannedAt", item.bannedAt)
                    put("bannedBy", item.bannedBy)
                }
                array.put(obj)
            }
            // تشفير محتوى الحسابات المحظورة بنظام AES-256 قبل الحفظ في القرص
            val encryptedPayload = AmanSecurityEngine.encrypt(array.toString())
            prefs.edit().putString(KEY_BANNED_ACCOUNTS, encryptedPayload).apply()
        } catch (_: Exception) {}
    }

    /**
     * Checks if a phone number, email, or contact name has been banned by the admin.
     */
    fun isBanned(identifier: String): Boolean {
        val clean = identifier.trim()
        if (clean.isBlank()) return false
        return _bannedAccounts.value.any {
            it.identifier.equals(clean, ignoreCase = true) ||
            clean.contains(it.identifier, ignoreCase = true)
        }
    }

    /**
     * Bans a user account or phone number.
     */
    fun banAccount(identifier: String, reason: String) {
        if (!isUserAdmin()) return
        val clean = identifier.trim()
        if (clean.isBlank()) return
        val newEntry = BannedAccount(
            identifier = clean,
            reason = reason.ifBlank { "مخالفة سياسات وضوابط منظومة أمان فون" }
        )
        _bannedAccounts.value = listOf(newEntry) + _bannedAccounts.value.filter { it.identifier != clean }
        saveBannedAccounts()
    }

    /**
     * Unbans an account or phone number.
     */
    fun unbanAccount(id: String) {
        if (!isUserAdmin()) return
        _bannedAccounts.value = _bannedAccounts.value.filter { it.id != id }
        saveBannedAccounts()
    }

    /**
     * Broadcasts an official supervisor announcement to all users across the app.
     */
    fun sendSupervisorBroadcast(
        title: String,
        message: String,
        isUrgent: Boolean,
        context: Context,
        repository: PhoneTrackerRepository
    ) {
        if (!isUserAdmin()) return

        val formattedTitle = "👑 تعميم المشرف العام: $title"
        val formattedMessage = "$message\n\n— المهندس هاشم القديمي (المشرف العام)"

        // 1. Trigger the in-app notification banner
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

        // 2. Insert into the local and cloud alerts feed
        CoroutineScope(Dispatchers.IO).launch {
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
        }

        // 3. Trigger system notification
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
