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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.CheckImeiScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NewReportScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportDetailScreen
import com.example.ui.screens.YemenGuideScreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandBg
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.YemenGold
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val viewModel: PhoneTrackerViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission status handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Android Notification Channels & Admin Manager
        NotificationHelper.setupNotificationChannels(this)
        com.example.util.AdminManager.initialize(this)

        // 2. Hardware Security & Anti-Tapjacking Shield
        try {
            window.decorView.filterTouchesWhenObscured = true
        } catch (_: Exception) {}

        // 3. Device Integrity & Anti-Tamper Check
        val integrity = com.example.util.DeviceIntegrityChecker.performSecurityAudit(this)
        if (integrity.isDeviceCompromised) {
            // Environment alert registered
        }

        // 4. Request Notification Permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 5. Handle Intent Extras if clicked from Notification
        handleIntent(intent)

        // 6. Silent Google Auto-Sign-In attempt
        val credentialManager = androidx.credentials.CredentialManager.create(this)
        lifecycleScope.launch {
            com.example.data.remote.FirebaseAuthManager.attemptAutoSignIn(
                context = this@MainActivity,
                credentialManager = credentialManager,
                onAuthSuccess = { /* Logged in */ },
                onUnauthenticated = { /* Continue */ },
                scope = this
            )
        }

        setContent {
            MyApplicationTheme {
                // Ensure RTL layout for Arabic
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val targetScreen = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_SCREEN)
        val reportId = intent.getLongExtra(NotificationHelper.EXTRA_REPORT_ID, -1L)

        if (targetScreen == "details" && reportId != -1L) {
            viewModel.openReportDetails(reportId)
        } else if (targetScreen == "alerts") {
            viewModel.navigateTo(AppScreen.ALERTS)
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: PhoneTrackerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadAlertsCount.collectAsStateWithLifecycle()

    val showBottomBar = currentScreen in listOf(
        AppScreen.FEED,
        AppScreen.CHECK_IMEI,
        AppScreen.ALERTS,
        AppScreen.SHOPS_GUIDE
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BrandBg,
        // Each screen's own TopAppBar / Scaffold handles the status bar.
        // Only the splash (no scaffold of its own) needs the outer insets.
        contentWindowInsets = if (currentScreen == AppScreen.SPLASH) WindowInsets.systemBars else WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = BrandSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    BottomItem(
                        selected = currentScreen == AppScreen.FEED,
                        icon = Icons.Default.Home,
                        label = "الرئيسية",
                        tag = "nav_tab_feed"
                    ) { viewModel.navigateToTab(AppScreen.FEED) }

                    BottomItem(
                        selected = currentScreen == AppScreen.CHECK_IMEI,
                        icon = Icons.Default.QrCodeScanner,
                        label = "فحص IMEI",
                        tag = "nav_tab_check_imei"
                    ) { viewModel.navigateToTab(AppScreen.CHECK_IMEI) }

                    BottomItem(
                        selected = currentScreen == AppScreen.NEW_REPORT,
                        icon = Icons.Default.AddCircle,
                        label = "بلاغ جديد",
                        tag = "nav_tab_new_report",
                        accent = AlertRed
                    ) { viewModel.navigateTo(AppScreen.NEW_REPORT) }

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.ALERTS,
                        onClick = { viewModel.navigateToTab(AppScreen.ALERTS) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(containerColor = AlertRed) {
                                            Text(text = if (unreadCount > 99) "99+" else "$unreadCount", color = PureWhite)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "التنبيهات")
                            }
                        },
                        label = { Text("التنبيهات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = navColors(YemenGold),
                        modifier = Modifier.testTag("nav_tab_alerts")
                    )

                    BottomItem(
                        selected = currentScreen == AppScreen.SHOPS_GUIDE,
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        label = "دليل الأمان",
                        tag = "nav_tab_guide"
                    ) { viewModel.navigateToTab(AppScreen.SHOPS_GUIDE) }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.SPLASH -> com.example.ui.screens.MotionSplashScreen(
                        onFinishSplash = { viewModel.finishSplash() }
                    )
                    AppScreen.LOGIN -> com.example.ui.screens.LoginScreen(
                        onLoginSuccess = { viewModel.onEnteredApp() },
                        onSkipGuest = { viewModel.onEnteredApp() }
                    )
                    AppScreen.FEED -> HomeScreen(
                        viewModel = viewModel
                    )
                    AppScreen.CHECK_IMEI -> CheckImeiScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.handleBack() }
                    )
                    AppScreen.NEW_REPORT -> NewReportScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.handleBack() },
                        onReportSubmitted = { newId ->
                            viewModel.openReportReplacingCurrent(newId)
                        }
                    )
                    AppScreen.ALERTS -> AlertsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.handleBack() },
                        onOpenReport = { reportId ->
                            viewModel.openReportDetails(reportId)
                        }
                    )
                    AppScreen.REPORT_DETAILS -> ReportDetailScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.handleBack() }
                    )
                    AppScreen.SHOPS_GUIDE -> YemenGuideScreen(
                        onBack = { viewModel.handleBack() },
                        onNavigateToCheckImei = { viewModel.navigateToTab(AppScreen.CHECK_IMEI) }
                    )
                    AppScreen.ADMIN_DASHBOARD -> com.example.ui.screens.AdminDashboardScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.handleBack() }
                    )
                    AppScreen.PROFILE -> ProfileScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.handleBack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun navColors(accent: androidx.compose.ui.graphics.Color) = NavigationBarItemDefaults.colors(
    selectedIconColor = accent,
    selectedTextColor = accent,
    indicatorColor = accent.copy(alpha = 0.16f),
    unselectedIconColor = PureWhite.copy(alpha = 0.55f),
    unselectedTextColor = PureWhite.copy(alpha = 0.55f)
)

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    tag: String,
    accent: androidx.compose.ui.graphics.Color = YemenGold,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = navColors(accent),
        modifier = Modifier.testTag(tag)
    )
}
