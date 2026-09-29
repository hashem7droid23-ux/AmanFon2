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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.CheckImeiScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NewReportScreen
import com.example.ui.screens.ReportDetailScreen
import com.example.ui.screens.YemenGuideScreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.PureWhite
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

        // 1. Initialize Android Notification Channels
        NotificationHelper.setupNotificationChannels(this)

        // 2. Request Notification Permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 3. Handle Intent Extras if clicked from Notification
        handleIntent(intent)

        // 4. Silent Google Auto-Sign-In attempt
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
        AppScreen.NEW_REPORT,
        AppScreen.ALERTS,
        AppScreen.SHOPS_GUIDE
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Navy800,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.FEED,
                        onClick = { viewModel.navigateTo(AppScreen.FEED) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                        label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PureWhite,
                            selectedTextColor = PureWhite,
                            indicatorColor = Navy700,
                            unselectedIconColor = PureWhite.copy(alpha = 0.6f),
                            unselectedTextColor = PureWhite.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_feed")
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.CHECK_IMEI,
                        onClick = { viewModel.navigateTo(AppScreen.CHECK_IMEI) },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "فحص IMEI") },
                        label = { Text("فحص IMEI", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PureWhite,
                            selectedTextColor = PureWhite,
                            indicatorColor = Navy700,
                            unselectedIconColor = PureWhite.copy(alpha = 0.6f),
                            unselectedTextColor = PureWhite.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_check_imei")
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.NEW_REPORT,
                        onClick = { viewModel.navigateTo(AppScreen.NEW_REPORT) },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "تقديم بلاغ") },
                        label = { Text("بلاغ جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PureWhite,
                            selectedTextColor = PureWhite,
                            indicatorColor = AlertRed,
                            unselectedIconColor = PureWhite.copy(alpha = 0.6f),
                            unselectedTextColor = PureWhite.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_new_report")
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.ALERTS,
                        onClick = { viewModel.navigateTo(AppScreen.ALERTS) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(containerColor = AlertRed) {
                                            Text(text = "$unreadCount", color = PureWhite)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "التنبيهات")
                            }
                        },
                        label = { Text("التنبيهات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PureWhite,
                            selectedTextColor = PureWhite,
                            indicatorColor = Navy700,
                            unselectedIconColor = PureWhite.copy(alpha = 0.6f),
                            unselectedTextColor = PureWhite.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_alerts")
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.SHOPS_GUIDE,
                        onClick = { viewModel.navigateTo(AppScreen.SHOPS_GUIDE) },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = "دليل المحلات") },
                        label = { Text("دليل الأمان", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PureWhite,
                            selectedTextColor = PureWhite,
                            indicatorColor = Navy700,
                            unselectedIconColor = PureWhite.copy(alpha = 0.6f),
                            unselectedTextColor = PureWhite.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_guide")
                    )
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
                            viewModel.openReportDetails(newId)
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
                        onNavigateToCheckImei = { viewModel.navigateTo(AppScreen.CHECK_IMEI) }
                    )
                }
            }
        }
    }
}
