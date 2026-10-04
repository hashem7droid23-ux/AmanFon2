package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandBg
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.YemenGold
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.NotificationHelper
import com.example.util.AppUpdater

class MainActivity : ComponentActivity() {
    private val viewModel: PhoneTrackerViewModel by viewModels()
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.setupNotificationChannels(this)
        com.example.util.AdminManager.initialize(this)
        try { window.decorView.filterTouchesWhenObscured = true } catch (_: Exception) {}
        com.example.util.DeviceIntegrityChecker.performSecurityAudit(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasNotificationPermission(this)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        handleIntent(intent)
        val credentialManager = androidx.credentials.CredentialManager.create(this)
        lifecycleScope.launch {
            com.example.data.remote.FirebaseAuthManager.attemptAutoSignIn(this@MainActivity, credentialManager,
                onAuthSuccess = {}, onUnauthenticated = {}, scope = this)
        }
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { MainAppScaffold(viewModel) }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { com.example.data.remote.FirebaseAuthManager.refreshUser() }
        AppUpdater.check(this)
    }
    override fun onDestroy() { AppUpdater.release(this); super.onDestroy() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val targetScreen = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_SCREEN)
        val reportId = intent.getLongExtra(NotificationHelper.EXTRA_REPORT_ID, -1L)
        if (targetScreen == "details" && reportId != -1L) viewModel.openReportDetails(reportId)
        else if (targetScreen == "alerts") viewModel.navigateTo(AppScreen.ALERTS)
    }
}

@Composable
fun MainAppScaffold(viewModel: PhoneTrackerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadAlertsCount.collectAsStateWithLifecycle()
    val remoteConfig by com.example.data.remote.AdminCloud.config.collectAsStateWithLifecycle()
    val isAdmin by com.example.util.AdminManager.isAdmin.collectAsStateWithLifecycle()
    val pinned by viewModel.pinnedReport.collectAsStateWithLifecycle()
    val inMaintenance = remoteConfig.maintenanceMode && !isAdmin && currentScreen != AppScreen.SPLASH && currentScreen != AppScreen.LOGIN
    val showBottomBar = !inMaintenance && currentScreen in listOf(AppScreen.FEED, AppScreen.CHECK_IMEI, AppScreen.ALERTS, AppScreen.SHOPS_GUIDE)
    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = BrandBg,
        contentWindowInsets = if (currentScreen == AppScreen.SPLASH) WindowInsets.systemBars else WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) Column {
                AdminBroadcastStrip(remoteConfig.announcement, pinned) { viewModel.openReportDetails(it) }
                NavigationBar(containerColor = BrandSurface, tonalElevation = 0.dp, modifier = Modifier.testTag("main_navigation_bar")) {
                    BottomItem(currentScreen == AppScreen.FEED, Icons.Default.Home, "الرئيسية", "nav_tab_feed") { viewModel.navigateToTab(AppScreen.FEED) }
                    BottomItem(currentScreen == AppScreen.CHECK_IMEI, Icons.Default.QrCodeScanner, "فحص IMEI", "nav_tab_check_imei") { viewModel.navigateToTab(AppScreen.CHECK_IMEI) }
                    BottomItem(currentScreen == AppScreen.NEW_REPORT, Icons.Default.AddCircle, "بلاغ جديد", "nav_tab_new_report", AlertRed) { viewModel.navigateTo(AppScreen.NEW_REPORT) }
                    NavigationBarItem(selected = currentScreen == AppScreen.ALERTS, onClick = { viewModel.navigateToTab(AppScreen.ALERTS) },
                        icon = {
                            BadgedBox(badge = { if (unreadCount > 0) Badge(containerColor = AlertRed) { Text(if (unreadCount > 99) "99+" else "$unreadCount", color = PureWhite) } }) { Icon(Icons.Default.Notifications, "التنبيهات") }
                        }, label = { Text("التنبيهات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }, colors = navColors(YemenGold), modifier = Modifier.testTag("nav_tab_alerts"))
                    BottomItem(currentScreen == AppScreen.SHOPS_GUIDE, Icons.AutoMirrored.Filled.MenuBook, "دليل الأمان", "nav_tab_guide") { viewModel.navigateToTab(AppScreen.SHOPS_GUIDE) }
                }
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (inMaintenance) MaintenanceScreen(remoteConfig.maintenanceMessage) else {
                AnimatedContent(targetState = currentScreen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "ScreenTransition") { screen ->
                    when (screen) {
                        AppScreen.SPLASH -> MotionSplashScreen(onFinishSplash = { viewModel.finishSplash() })
                        AppScreen.LOGIN -> LoginScreen(onLoginSuccess = { viewModel.onEnteredApp() }, onSkipGuest = { viewModel.onEnteredApp() })
                        AppScreen.FEED -> HomeScreen(viewModel)
                        AppScreen.CHECK_IMEI -> CheckImeiScreen(viewModel, onBack = { viewModel.handleBack() })
                        AppScreen.NEW_REPORT -> NewReportScreen(viewModel, onBack = { viewModel.handleBack() }, onReportSubmitted = { viewModel.openReportReplacingCurrent(it) })
                        AppScreen.ALERTS -> AlertsScreen(viewModel, onBack = { viewModel.handleBack() }, onOpenReport = { viewModel.openReportDetails(it) })
                        AppScreen.REPORT_DETAILS -> ReportDetailScreen(viewModel, onBack = { viewModel.handleBack() })
                        AppScreen.SHOPS_GUIDE -> YemenGuideScreen(onBack = { viewModel.handleBack() }, onNavigateToCheckImei = { viewModel.navigateToTab(AppScreen.CHECK_IMEI) })
                        AppScreen.ADMIN_DASHBOARD -> AdminDashboardScreen(viewModel, onBack = { viewModel.handleBack() })
                        AppScreen.PROFILE -> ProfileScreen(viewModel, onBack = { viewModel.handleBack() })
                    }
                }
            }
        }
    }
}
@Composable
private fun navColors(accent: androidx.compose.ui.graphics.Color) = NavigationBarItemDefaults.colors(
    selectedIconColor = accent, selectedTextColor = accent, indicatorColor = accent.copy(alpha = 0.16f),
    unselectedIconColor = PureWhite.copy(alpha = 0.55f), unselectedTextColor = PureWhite.copy(alpha = 0.55f))
@Composable
private fun RowScope.BottomItem(selected: Boolean, icon: ImageVector, label: String, tag: String,
    accent: androidx.compose.ui.graphics.Color = YemenGold, onClick: () -> Unit) {
    NavigationBarItem(selected, onClick, icon = { Icon(icon, label) }, label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, colors = navColors(accent), modifier = Modifier.testTag(tag))
}
@Composable
private fun AdminBroadcastStrip(announcement: String, pinned: com.example.data.model.ReportEntity?, onOpenPinned: (Long) -> Unit) {
    if (announcement.isBlank() && pinned == null) return
    Column(Modifier.fillMaxWidth().background(com.example.ui.theme.BrandSurfaceHigh)) {
        if (announcement.isNotBlank()) Text("📢 $announcement", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp).testTag("admin_announcement_strip"))
        if (pinned != null) Text("📌 بلاغ مثبّت: ${pinned.brand.substringBefore(" (")} ${pinned.model} • ${pinned.governorate.substringBefore(" (")}  ←",
            color = YemenGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            modifier = Modifier.fillMaxWidth().clickable { onOpenPinned(pinned.id) }.padding(horizontal = 14.dp, vertical = 8.dp).testTag("admin_pinned_strip"))
    }
}
@Composable
private fun MaintenanceScreen(message: String) {
    Column(Modifier.fillMaxSize().background(BrandBg).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🛠️", fontSize = 56.sp); Spacer(Modifier.height(16.dp))
        Text("التطبيق تحت الصيانة", color = PureWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message.ifBlank { "نعمل على تحسين الخدمة، نرجع لكم قريباً." }, color = PureWhite.copy(alpha = 0.75f), fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp)); Text("إدارة منظومة أمان فون", color = YemenGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
