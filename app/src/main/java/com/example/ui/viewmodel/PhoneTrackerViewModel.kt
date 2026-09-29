package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AlertEntity
import com.example.data.model.ReportEntity
import com.example.data.repository.PhoneTrackerRepository
import com.example.util.ImeiValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    LOGIN,
    FEED,
    CHECK_IMEI,
    NEW_REPORT,
    ALERTS,
    REPORT_DETAILS,
    SHOPS_GUIDE,
    ADMIN_DASHBOARD
}

sealed class ImeiCheckState {
    object Idle : ImeiCheckState()
    object Checking : ImeiCheckState()
    data class StolenAlert(val report: ReportEntity) : ImeiCheckState()
    data class Safe(val cleanImei: String, val isLuhnValid: Boolean) : ImeiCheckState()
    data class InvalidFormat(val reason: String) : ImeiCheckState()
}

data class AppStats(
    val totalReports: Int = 0,
    val activeStolen: Int = 0,
    val recovered: Int = 0
)

class PhoneTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhoneTrackerRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PhoneTrackerRepository(
            reportDao = database.reportDao(),
            alertDao = database.alertDao(),
            context = application.applicationContext
        )
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.LOGIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<AppScreen>()

    private val _selectedReportId = MutableStateFlow<Long?>(null)
    val selectedReportId: StateFlow<Long?> = _selectedReportId.asStateFlow()

    // Filters and Search
    val searchQuery = MutableStateFlow("")
    val filterGovernorate = MutableStateFlow<String?>(null)
    val filterType = MutableStateFlow<String?>(null) // STOLEN, LOST, FOUND

    // Data streams
    val allReports = repository.allReports
    val allAlerts = repository.allAlerts
    val unreadAlertsCount: StateFlow<Int> = repository.unreadAlertsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val stats: StateFlow<AppStats> = combine(
        repository.reportsCount,
        repository.activeStolenCount,
        repository.recoveredCount
    ) { total, stolen, recovered ->
        AppStats(totalReports = total, activeStolen = stolen, recovered = recovered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppStats())

    // Filtered Reports
    val filteredReports: StateFlow<List<ReportEntity>> = combine(
        allReports,
        searchQuery,
        filterGovernorate,
        filterType
    ) { reports, query, gov, type ->
        reports.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.model.contains(query, ignoreCase = true) ||
                    item.brand.contains(query, ignoreCase = true) ||
                    item.imei1.contains(query) ||
                    item.incidentLocation.contains(query, ignoreCase = true) ||
                    item.contactName.contains(query, ignoreCase = true) ||
                    item.color.contains(query, ignoreCase = true)

            val matchesGov = gov == null || item.governorate == gov
            val matchesType = type == null || item.reportType == type

            matchesQuery && matchesGov && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Report Detail Flow
    val selectedReport: StateFlow<ReportEntity?> = combine(
        allReports,
        _selectedReportId
    ) { reports, id ->
        if (id == null) null else reports.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // IMEI Checker State
    val imeiSearchInput = MutableStateFlow("")
    private val _imeiCheckResult = MutableStateFlow<ImeiCheckState>(ImeiCheckState.Idle)
    val imeiCheckResult: StateFlow<ImeiCheckState> = _imeiCheckResult.asStateFlow()

    // Submit Report Feedback
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submissionSuccessMessage = MutableStateFlow<String?>(null)
    val submissionSuccessMessage: StateFlow<String?> = _submissionSuccessMessage.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            _screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun openReportDetails(reportId: Long) {
        _selectedReportId.value = reportId
        navigateTo(AppScreen.REPORT_DETAILS)
    }

    fun handleBack(): Boolean {
        if (_screenHistory.isNotEmpty()) {
            val previous = _screenHistory.removeAt(_screenHistory.size - 1)
            _currentScreen.value = previous
            return true
        } else if (_currentScreen.value != AppScreen.FEED) {
            _currentScreen.value = AppScreen.FEED
            return true
        }
        return false
    }

    fun checkImei() {
        val raw = imeiSearchInput.value
        val clean = ImeiValidator.clean(raw)

        if (clean.length < 8) {
            _imeiCheckResult.value = ImeiCheckState.InvalidFormat("رقم IMEI يجب أن يتكون من 14 إلى 15 رقماً على الأقل")
            return
        }

        viewModelScope.launch {
            _imeiCheckResult.value = ImeiCheckState.Checking
            val found = repository.checkImei(clean)
            if (found != null) {
                _imeiCheckResult.value = ImeiCheckState.StolenAlert(found)
            } else {
                val isLuhnValid = ImeiValidator.isValidLuhn(clean)
                _imeiCheckResult.value = ImeiCheckState.Safe(clean, isLuhnValid)
            }
        }
    }

    fun testWithSampleImei(imei: String) {
        imeiSearchInput.value = imei
        checkImei()
    }

    fun clearImeiCheck() {
        imeiSearchInput.value = ""
        _imeiCheckResult.value = ImeiCheckState.Idle
    }

    fun submitReport(
        reportType: String,
        brand: String,
        model: String,
        imei1: String,
        imei2: String,
        color: String,
        distinctiveMarks: String,
        governorate: String,
        district: String,
        incidentLocation: String,
        contactName: String,
        primaryPhone: String,
        whatsappNumber: String,
        rewardAmountStr: String,
        policeReportNumber: String,
        additionalNotes: String,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanImei = ImeiValidator.clean(imei1)
        if (cleanImei.length < 10) {
            onError("يرجى إدخال رقم IMEI صحيح لا يقل عن 14-15 رقماً")
            return
        }

        if (brand.isBlank() || model.isBlank()) {
            onError("يرجى تحديد ماركة وموديل الجهاز")
            return
        }

        if (governorate.isBlank() || incidentLocation.isBlank()) {
            onError("يرجى تحديد المحافظة ومكان الفقدان/السرقة")
            return
        }

        if (contactName.isBlank() || primaryPhone.isBlank()) {
            onError("يرجى إدخال اسم صاحب البلاغ ورقم التواصل")
            return
        }

        // Admin Security Enforcement: Check if user or phone number is banned
        if (com.example.util.AdminManager.isBanned(primaryPhone) || 
            com.example.util.AdminManager.isBanned(whatsappNumber) || 
            com.example.util.AdminManager.isBanned(contactName)) {
            onError("⛔ هذا الحساب أو رقم الهاتف محظور من قبل المشرف العام (م. هاشم القديمي) لمخالفته شروط وسياسات المنظومة.")
            return
        }

        val rewardLong = rewardAmountStr.filter { it.isDigit() }.toLongOrNull() ?: 0L

        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val newReport = ReportEntity(
                    reportType = reportType,
                    brand = brand.trim(),
                    model = model.trim(),
                    imei1 = cleanImei,
                    imei2 = ImeiValidator.clean(imei2),
                    color = color.trim().ifBlank { "غير محدد" },
                    distinctiveMarks = distinctiveMarks.trim(),
                    governorate = governorate,
                    district = district.trim(),
                    incidentLocation = incidentLocation.trim(),
                    incidentTimestamp = System.currentTimeMillis(),
                    contactName = contactName.trim(),
                    primaryPhone = primaryPhone.trim(),
                    whatsappNumber = whatsappNumber.trim().ifBlank { primaryPhone.trim() },
                    rewardAmount = rewardLong,
                    policeReportNumber = policeReportNumber.trim(),
                    additionalNotes = additionalNotes.trim(),
                    createdAt = System.currentTimeMillis()
                )
                val id = repository.insertReport(newReport, notifyBroadcast = true)
                _isSubmitting.value = false
                _submissionSuccessMessage.value = "تم تسجيل البلاغ بنجاح وتعميمه فوراً برقم #$id"
                onSuccess(id)
            } catch (e: Exception) {
                _isSubmitting.value = false
                onError(e.message ?: "حدث خطأ أثناء حفظ البلاغ")
            }
        }
    }

    fun clearSubmissionMessage() {
        _submissionSuccessMessage.value = null
    }

    fun toggleRecovered(reportId: Long, isRecovered: Boolean) {
        viewModelScope.launch {
            repository.updateRecoveryStatus(reportId, isRecovered)
        }
    }

    fun deleteReport(reportId: Long) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
            if (_selectedReportId.value == reportId) {
                handleBack()
            }
        }
    }

    fun adminDeleteReport(reportId: Long) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
            if (_selectedReportId.value == reportId) {
                handleBack()
            }
        }
    }

    fun sendSupervisorBroadcast(title: String, message: String, isUrgent: Boolean, context: android.content.Context) {
        com.example.util.AdminManager.sendSupervisorBroadcast(title, message, isUrgent, context, repository)
    }

    fun markAlertRead(alertId: Long) {
        viewModelScope.launch {
            repository.markAlertAsRead(alertId)
        }
    }

    fun markAllAlertsRead() {
        viewModelScope.launch {
            repository.markAllAlertsAsRead()
        }
    }

    fun simulateTheftBroadcast() {
        viewModelScope.launch {
            val randomGovs = listOf("صنعاء (الأمانة)", "عدن", "تعز", "حضرموت (المكلا)", "مأرب", "الحديدة")
            val randomModels = listOf(
                Pair("Apple (آيفون)", "iPhone 14 Pro (128GB)"),
                Pair("Samsung (سامسونج)", "Galaxy S24 Plus"),
                Pair("Xiaomi / Redmi (شاومي)", "Redmi Note 13"),
                Pair("Honor (هونر)", "Honor X9b 5G")
            )
            val selectedGov = randomGovs.random()
            val selectedModel = randomModels.random()
            val simulatedImei = "35" + (1000000000000L..9999999999999L).random().toString()

            repository.simulateBroadcastAlert(
                governorate = selectedGov,
                brand = selectedModel.first,
                model = selectedModel.second,
                imei = simulatedImei
            )
        }
    }
}
