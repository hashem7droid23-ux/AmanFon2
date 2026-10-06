package com.example.ui.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AlertEntity
import com.example.data.model.ReportEntity
import com.example.data.repository.PhoneTrackerRepository
import com.example.util.ImeiValidator
import com.example.util.ReportPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

 enum class AppScreen { SPLASH, LOGIN, FEED, CHECK_IMEI, NEW_REPORT, ALERTS, REPORT_DETAILS, SHOPS_GUIDE, ADMIN_DASHBOARD, PROFILE }
sealed class ImeiCheckState {
    object Idle : ImeiCheckState()
    object Checking : ImeiCheckState()
    data class StolenAlert(val report: ReportEntity) : ImeiCheckState()
    data class Safe(val cleanImei: String, val isLuhnValid: Boolean) : ImeiCheckState()
    data class InvalidFormat(val reason: String) : ImeiCheckState()
    data class Unavailable(val reason: String) : ImeiCheckState()
}
data class AppStats(val totalReports: Int = 0, val activeStolen: Int = 0, val recovered: Int = 0)

class PhoneTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = PhoneTrackerRepository(database.reportDao(), database.alertDao(), application.applicationContext)
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen = _currentScreen.asStateFlow()
    private val _screenHistory = mutableListOf<AppScreen>()
    private val _selectedReportId = MutableStateFlow<Long?>(null)
    val selectedReportId = _selectedReportId.asStateFlow()
    val searchQuery = MutableStateFlow("")
    val filterGovernorate = MutableStateFlow<String?>(null)
    val filterType = MutableStateFlow<String?>(null)
    val allReports = repository.allReports
    val allAlerts = repository.allAlerts
    val unreadAlertsCount = repository.unreadAlertsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val stats = combine(repository.reportsCount, repository.activeStolenCount, repository.recoveredCount) { total, stolen, recovered ->
        AppStats(total, stolen, recovered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppStats())
    private val visibleReports = combine(allReports, com.example.data.remote.AdminCloud.config, com.example.util.AdminManager.isAdmin) { reports, cfg, admin ->
        if (admin) reports else reports.filter { it.imei1 !in cfg.hiddenImeis }
    }
    val pinnedReport = combine(visibleReports, com.example.data.remote.AdminCloud.config) { reports, cfg ->
        reports.firstOrNull { cfg.pinnedImei.isNotBlank() && it.imei1 == cfg.pinnedImei }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val filteredReports = combine(visibleReports, searchQuery, filterGovernorate, filterType) { reports, query, gov, type ->
        reports.filter { r ->
            val matches = query.isBlank() || listOf(r.model, r.brand, r.imei1, r.imei2, r.serialNumber, r.incidentLocation, r.contactName, r.color).any { it.contains(query, true) }
            matches && (gov == null || r.governorate == gov) && (type == null || r.reportType == type)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedReport = combine(allReports, _selectedReportId) { reports, id -> reports.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val imeiSearchInput = MutableStateFlow("")
    private val _imeiCheckResult = MutableStateFlow<ImeiCheckState>(ImeiCheckState.Idle)
    val imeiCheckResult = _imeiCheckResult.asStateFlow()
    private var checkJob: kotlinx.coroutines.Job? = null
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()
    private val _submissionSuccessMessage = MutableStateFlow<String?>(null)
    val submissionSuccessMessage = _submissionSuccessMessage.asStateFlow()

    fun finishSplash() {
        _currentScreen.value = if (com.example.data.remote.FirebaseAuthManager.currentUser.value != null) AppScreen.FEED else AppScreen.LOGIN
    }
    fun navigateTo(screen: AppScreen) {
        val target = if (screen == AppScreen.LOGIN && com.example.data.remote.FirebaseAuthManager.currentUser.value != null && _currentScreen.value != AppScreen.SPLASH) AppScreen.PROFILE else screen
        if (_currentScreen.value != target) { _screenHistory.add(_currentScreen.value); _currentScreen.value = target }
    }
    fun navigateToTab(screen: AppScreen) {
        _screenHistory.clear()
        if (screen != AppScreen.FEED) _screenHistory.add(AppScreen.FEED)
        _currentScreen.value = screen
    }
    fun onEnteredApp() { _screenHistory.clear(); _currentScreen.value = AppScreen.FEED }
    fun openReportReplacingCurrent(reportId: Long) { _selectedReportId.value = reportId; _currentScreen.value = AppScreen.REPORT_DETAILS }
    fun openReportDetails(reportId: Long) { _selectedReportId.value = reportId; navigateTo(AppScreen.REPORT_DETAILS) }
    fun handleBack(): Boolean {
        while (_screenHistory.isNotEmpty()) {
            val previous = _screenHistory.removeAt(_screenHistory.lastIndex)
            if (previous !in listOf(AppScreen.SPLASH, AppScreen.LOGIN, _currentScreen.value)) { _currentScreen.value = previous; return true }
        }
        if (_currentScreen.value != AppScreen.FEED) { _currentScreen.value = AppScreen.FEED; return true }
        return false
    }
    fun checkImei() {
        checkJob?.cancel()
        val clean = ImeiValidator.clean(imeiSearchInput.value)
        if (!ReportPolicy.isCompleteImei(clean)) {
            _imeiCheckResult.value = ImeiCheckState.InvalidFormat("رقم IMEI يتكون من 15 رقماً، تأكد منه بطلب *#06#")
            return
        }
        checkJob = viewModelScope.launch {
            _imeiCheckResult.value = ImeiCheckState.Checking
            try {
                val found = repository.checkImei(clean)
                _imeiCheckResult.value = if (found == null) ImeiCheckState.Safe(clean, ImeiValidator.isValidLuhn(clean)) else ImeiCheckState.StolenAlert(found)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _imeiCheckResult.value = ImeiCheckState.Unavailable("تعذر التحقق من قاعدة البلاغات على الخادم. سجّل الدخول وتحقق من الإنترنت ثم أعد المحاولة؛ لا تعتبر الجهاز آمناً.")
            }
        }
    }
    fun testWithSampleImei(imei: String) { imeiSearchInput.value = imei; checkImei() }
    fun clearImeiCheck() { checkJob?.cancel(); imeiSearchInput.value = ""; _imeiCheckResult.value = ImeiCheckState.Idle }

    fun submitReport(
        reportType: String, brand: String, model: String, imei1: String, imei2: String,
        color: String, distinctiveMarks: String, governorate: String, district: String,
        incidentLocation: String, contactName: String, primaryPhone: String, whatsappNumber: String,
        rewardAmountStr: String, policeReportNumber: String, additionalNotes: String,
        serialNumber: String = "", photoUris: List<String> = emptyList(),
        onSuccess: (Long) -> Unit, onError: (String) -> Unit
    ) {
        if (_isSubmitting.value) return
        val clean = ImeiValidator.clean(imei1)
        val second = ImeiValidator.clean(imei2)
        if (!ReportPolicy.isCompleteImei(clean) || (second.isNotBlank() && !ReportPolicy.isCompleteImei(second))) { onError("يرجى إدخال IMEI من 15 رقماً لكل رقم غير فارغ"); return }
        if (brand.trim().length < 2 || model.trim().length < 2) { onError("يرجى تحديد ماركة وموديل الجهاز"); return }
        if (governorate.trim().length < 2 || incidentLocation.trim().length < 2) { onError("يرجى تحديد المحافظة ومكان الحادثة"); return }
        if (contactName.trim().length < 2 || primaryPhone.isBlank()) { onError("يرجى إدخال اسم صاحب البلاغ ورقم التواصل"); return }
        if (com.example.data.remote.AdminCloud.config.value.reportsPaused && !com.example.util.AdminManager.isAdmin.value) { onError("نشر البلاغات موقوف مؤقتاً"); return }
        if (com.example.util.AdminManager.isBanned(primaryPhone) || com.example.util.AdminManager.isBanned(whatsappNumber) || com.example.util.AdminManager.isBanned(contactName)) { onError("هذا الحساب أو رقم الهاتف محظور"); return }
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val report = ReportEntity(
                    reportType = reportType, brand = brand.trim(), model = model.trim(), imei1 = clean, imei2 = second,
                    serialNumber = serialNumber.trim().uppercase().take(40), color = color.trim().ifBlank { "غير محدد" },
                    distinctiveMarks = distinctiveMarks.trim(), governorate = governorate, district = district.trim(),
                    incidentLocation = incidentLocation.trim(), incidentTimestamp = System.currentTimeMillis(),
                    contactName = contactName.trim(), primaryPhone = normalizeLocalPhone(primaryPhone),
                    whatsappNumber = normalizeLocalPhone(whatsappNumber.ifBlank { primaryPhone }),
                    rewardAmount = rewardAmountStr.filter { it.isDigit() }.toLongOrNull() ?: 0L,
                    policeReportNumber = policeReportNumber.trim(), additionalNotes = additionalNotes.trim()
                )
                val id = repository.insertReport(report)
                var photoWarning = ""
                if (photoUris.isNotEmpty()) {
                    try {
                        val images = com.example.util.ReportPhotos.saveLocal(getApplication(), clean, photoUris)
                        val cloudId = database.reportDao().getReportById(id)?.cloudId
                        val uploaded = cloudId != null && images.size == photoUris.size &&
                            com.example.util.ReportPhotos.upload(getApplication(), clean, images, cloudId)
                        if (!uploaded) photoWarning = "\nالبلاغ منشور، لكن الصور لم تُرفع. احتفظ بالصور وأعد المحاولة بعد تحديث قواعد Firebase."
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { photoWarning = "\nالبلاغ منشور، لكن حفظ أو رفع الصور لم يكتمل." }
                }
                _submissionSuccessMessage.value = "تم نشر البلاغ في قاعدة البيانات برقم #$id$photoWarning"
                if (photoWarning.isNotEmpty()) Toast.makeText(getApplication(), _submissionSuccessMessage.value, Toast.LENGTH_LONG).show()
                onSuccess(id)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { onError("لم يتم تأكيد نشر البلاغ. تحقق من الإنترنت والصلاحيات ثم افحص قائمة البلاغات قبل إعادة الإرسال.\n${e.message.orEmpty()}") }
            finally { _isSubmitting.value = false }
        }
    }
    private fun normalizeLocalPhone(raw: String): String {
        var d = raw.filter { it.isDigit() }
        if (d.startsWith("00967")) d = d.removePrefix("00967") else if (d.startsWith("967") && d.length > 9) d = d.removePrefix("967")
        if (d.startsWith("0") && d.length == 10) d = d.removePrefix("0")
        return d.ifBlank { raw.trim() }
    }
    fun clearSubmissionMessage() { _submissionSuccessMessage.value = null }
    fun toggleRecovered(reportId: Long, isRecovered: Boolean, onSuccess: () -> Unit = {}, onError: (String) -> Unit = { feedback(it) }) {
        viewModelScope.launch {
            try { repository.updateRecoveryStatus(reportId, isRecovered); onSuccess() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { onError(e.message ?: "لم يتم تأكيد تحديث البلاغ") }
        }
    }
    fun deleteReport(reportId: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = { feedback(it) }) {
        viewModelScope.launch {
            try {
                repository.deleteReport(reportId)
                if (_selectedReportId.value == reportId) handleBack()
                onSuccess()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { onError(e.message ?: "لم يتم تأكيد حذف البلاغ") }
        }
    }
    fun adminDeleteReport(reportId: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = { feedback(it) }) = deleteReport(reportId, onSuccess, onError)
    private fun feedback(message: String) { Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show() }
    fun sendSupervisorBroadcast(title: String, message: String, isUrgent: Boolean, context: android.content.Context) {
        com.example.util.AdminManager.sendSupervisorBroadcast(title, message, isUrgent, context, repository)
    }
    fun markAlertRead(alertId: Long) { viewModelScope.launch { repository.markAlertAsRead(alertId) } }
    fun markAllAlertsRead() { viewModelScope.launch { repository.markAllAlertsAsRead() } }
    fun simulateTheftBroadcast() { feedback("تم تعطيل البلاغات الوهمية") }
    override fun onCleared() { repository.close(); super.onCleared() }
}
