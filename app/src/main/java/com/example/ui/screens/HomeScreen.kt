package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.components.SearchWatchDialog
import com.example.util.InAppNotificationManager
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReportEntity
import com.example.data.model.YemenLocations
import com.example.ui.components.ReportItemCard
import com.example.ui.components.StatCard
import com.example.ui.components.UrgentAlertTicker
import com.example.ui.components.YemenFlagPill
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import com.example.data.remote.FirebaseAuthManager
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.AlertRed
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.IntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PhoneTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reports by viewModel.filteredReports.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val alerts by viewModel.allAlerts.collectAsStateWithLifecycle(initialValue = emptyList())
    val unreadCount by viewModel.unreadAlertsCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedType by viewModel.filterType.collectAsStateWithLifecycle()
    val selectedGov by viewModel.filterGovernorate.collectAsStateWithLifecycle()

    var showGovDialog by remember { mutableStateOf(false) }
    var showSearchWatchDialog by remember { mutableStateOf(false) }
    val inAppNotification by InAppNotificationManager.currentInAppNotification.collectAsStateWithLifecycle()
    val isSuperAdmin by com.example.util.AdminManager.isSuperAdmin.collectAsStateWithLifecycle()

    val latestUrgentAlert = alerts.firstOrNull { it.alertType == "URGENT_THEFT" } ?: alerts.firstOrNull()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "أمان فون",
                                    color = PureWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                YemenFlagPill()
                            }
                            Text(
                                text = "المنظومة الوطنية لحماية وتتبع الهواتف",
                                color = PureWhite.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Admin Crown shortcut (Exclusive to Hashem)
                    if (isSuperAdmin) {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) },
                            modifier = Modifier.testTag("admin_dashboard_top_button")
                        ) {
                            Text("👑", fontSize = 20.sp)
                        }
                    }

                    // Search Watch & FCM In-App Alerts shortcut
                    IconButton(
                        onClick = { showSearchWatchDialog = true },
                        modifier = Modifier.testTag("search_watch_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = "مراقبة المواصفات وتنبيهات FCM",
                            tint = WarningAmber
                        )
                    }

                    // Profile / Login shortcut
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.LOGIN) },
                        modifier = Modifier.testTag("account_login_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "تسجيل الدخول وحقوق المطور",
                            tint = PureWhite
                        )
                    }
                    // Guide for shops & users
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SHOPS_GUIDE) },
                        modifier = Modifier.testTag("guide_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "دليل المحلات",
                            tint = PureWhite
                        )
                    }

                    // Alerts Bell with Badge
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.ALERTS) },
                        modifier = Modifier.testTag("alerts_icon_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = AlertRed,
                                        contentColor = PureWhite
                                    ) {
                                        Text(text = "$unreadCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "التنبيهات العاجلة",
                                tint = PureWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy800)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.navigateTo(AppScreen.NEW_REPORT) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("تقديم بلاغ جديد", fontWeight = FontWeight.Bold) },
                containerColor = AlertRed,
                contentColor = PureWhite,
                modifier = Modifier.testTag("new_report_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // 1. Urgent Alert Ticker
            item {
                UrgentAlertTicker(
                    latestAlert = latestUrgentAlert,
                    unreadCount = unreadCount,
                    onClick = { viewModel.navigateTo(AppScreen.ALERTS) }
                )
            }

            // 1.2. Admin Executive Quick Access Banner (Visible ONLY to Hashem)
            if (isSuperAdmin) {
                item {
                    Card(
                        onClick = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Navy800),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, WarningAmber),
                        modifier = Modifier.fillMaxWidth().testTag("admin_quick_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("👑", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "لوحة تحكم المشرف العام (المهندس هاشم القديمي)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = WarningAmber
                                    )
                                    Text(
                                        text = "صلاحيات كاملة: حذف أي بلاغ، حظر الأرقام، وبث التعاميم",
                                        fontSize = 10.sp,
                                        color = PureWhite.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Text("لوحة التحكم ←", fontSize = 11.sp, color = WarningAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1.5. Firebase Google Cloud Sync & Auth Banner
            item {
                FirebaseCloudAuthCard()
            }

            // 2. Statistics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "إجمالي البلاغات",
                        value = "${stats.totalReports}",
                        icon = Icons.Default.PhoneAndroid,
                        accentColor = Navy700,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "سرقات نشطة",
                        value = "${stats.activeStolen}",
                        icon = Icons.Default.Security,
                        accentColor = AlertRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "تم استرجاعها",
                        value = "${stats.recovered}",
                        icon = Icons.Default.CheckCircle,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Quick Action Feature Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Check IMEI Quick Button
                    Card(
                        onClick = { viewModel.navigateTo(AppScreen.CHECK_IMEI) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_check_imei_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Navy700)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "فحص IMEI فوري",
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "قبل شراء أي هاتف",
                                    color = PureWhite.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Emergency Broadcast Simulator / Alert Trigger
                    Card(
                        onClick = { viewModel.simulateTheftBroadcast() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_broadcast_alert_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "بث تنبيه تجريبي",
                                    color = AlertRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تجربة إشعار فوري",
                                    color = AlertRed.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Search Bar with FCM Watch Trigger
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("ابحث برقم IMEI، الموديل، الموقع، أو الاسم...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Navy700)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_field")
                    )

                    // Quick Action: Watch this search
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Navy800.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "تفعيل تنبيه سحابي لبحث: [$searchQuery]" else "مراقبة مواصفات معينة وتنبيهي عند إضافتها (FCM)",
                                fontSize = 11.sp,
                                color = PureWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { showSearchWatchDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("watch_search_chip_button")
                        ) {
                            Text(
                                text = "تفعيل المراقبة 🔔",
                                color = Navy900,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 5. Filter Chips Row
            item {
                Column {
                    // Type Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { viewModel.filterType.value = null },
                            label = { Text("جميع البلاغات") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Navy700,
                                selectedLabelColor = PureWhite
                            ),
                            modifier = Modifier.testTag("filter_all")
                        )
                        FilterChip(
                            selected = selectedType == "STOLEN",
                            onClick = {
                                viewModel.filterType.value = if (selectedType == "STOLEN") null else "STOLEN"
                            },
                            label = { Text("مسروقة 🚨") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AlertRed,
                                selectedLabelColor = PureWhite
                            ),
                            modifier = Modifier.testTag("filter_stolen")
                        )
                        FilterChip(
                            selected = selectedType == "LOST",
                            onClick = {
                                viewModel.filterType.value = if (selectedType == "LOST") null else "LOST"
                            },
                            label = { Text("مفقودة ⚠️") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningAmber,
                                selectedLabelColor = Navy900
                            ),
                            modifier = Modifier.testTag("filter_lost")
                        )
                        FilterChip(
                            selected = selectedType == "FOUND",
                            onClick = {
                                viewModel.filterType.value = if (selectedType == "FOUND") null else "FOUND"
                            },
                            label = { Text("معثور عليها 📱") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SuccessGreen,
                                selectedLabelColor = PureWhite
                            ),
                            modifier = Modifier.testTag("filter_found")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Governorate selection chip scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المحافظة:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = selectedGov == null,
                            onClick = { viewModel.filterGovernorate.value = null },
                            label = { Text("كل محافظات اليمن") }
                        )
                        listOf("صنعاء (الأمانة)", "عدن", "تعز", "الحديدة", "إب", "حضرموت (المكلا)", "مأرب").forEach { gov ->
                            FilterChip(
                                selected = selectedGov == gov,
                                onClick = {
                                    viewModel.filterGovernorate.value = if (selectedGov == gov) null else gov
                                },
                                label = { Text(gov) }
                            )
                        }
                    }
                }
            }

            // 6. Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخر بلاغات الأجهزة المفقودة والمسروقة (${reports.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 7. Empty State or Reports List
            if (reports.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد بلاغات مطابقة لبحثك",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "جرب تغيير معايير البحث أو اختيار محافظة أخرى",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(reports, key = { it.id }) { report ->
                    ReportItemCard(
                        report = report,
                        onClick = { viewModel.openReportDetails(report.id) },
                        onCallClick = { IntentHelper.makeCall(context, report.primaryPhone) },
                        onWhatsAppClick = {
                            val msg = "السلام عليكم، بخصوص بلاغ الهاتف ${report.brand} ${report.model} في تطبيق أمان فون..."
                            IntentHelper.openWhatsApp(context, report.whatsappNumber, msg)
                        },
                        onShareClick = { IntentHelper.shareReport(context, report) }
                    )
                }
            }

            // Bottom Spacing for FAB
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        // Floating in-app notification banner at top of screen
        InAppNotificationBanner(
            notification = inAppNotification,
            onDismiss = { InAppNotificationManager.dismissCurrent() },
            onOpenReport = { reportId -> viewModel.openReportDetails(reportId) },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showSearchWatchDialog) {
        SearchWatchDialog(
            initialSearchQuery = searchQuery,
            onDismiss = { showSearchWatchDialog = false }
        )
    }
}
}

@Composable
fun FirebaseCloudAuthCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser by FirebaseAuthManager.currentUser.collectAsStateWithLifecycle()
    var isLoading by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (currentUser != null) SuccessGreenLight else Navy700.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (currentUser != null) SuccessGreen.copy(alpha = 0.4f) else Navy700.copy(alpha = 0.2f)
        )
    ) {
        if (currentUser != null) {
            val user = currentUser!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = user.displayName ?: "مستخدم جوجل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "متصل بسحابة Firebase ومزامنة البلاغات نشطة",
                            fontSize = 10.sp,
                            color = SuccessGreen
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        FirebaseAuthManager.signOut(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = {
                                Toast.makeText(context, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
                            },
                            scope = coroutineScope
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("خروج", fontSize = 11.sp)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Navy700.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Navy700,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مزامنة سحابية فورية (Firebase)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "سجل الدخول بحساب Google لمزامنة البلاغات",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        isLoading = true
                        FirebaseAuthManager.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                Toast.makeText(context, "تم تسجيل الدخول بنجاح عبر Google!", Toast.LENGTH_SHORT).show()
                            },
                            onAuthError = { err ->
                                isLoading = false
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            },
                            scope = coroutineScope,
                            onAuthCancelled = { isLoading = false }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy700),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PureWhite,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("دخول Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

