package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.components.SearchWatchDialog
import com.example.util.InAppNotificationManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.ReportItemCard
import com.example.ui.components.StatCard
import com.example.ui.components.UrgentAlertTicker
import com.example.ui.components.YemenFlagPill
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import com.example.data.remote.FirebaseAuthManager
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandBg
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandInk
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandSurfaceHigh
import com.example.ui.theme.GreenTint
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.YemenGold
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.IntentHelper
import com.google.firebase.auth.FirebaseUser

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

    var showSearchWatchDialog by remember { mutableStateOf(false) }
    val inAppNotification by InAppNotificationManager.currentInAppNotification.collectAsStateWithLifecycle()
    val isSuperAdmin by com.example.util.AdminManager.isSuperAdmin.collectAsStateWithLifecycle()
    val canSendTest = isSuperAdmin || BuildConfig.DEBUG

    val latestUrgentAlert = alerts.firstOrNull { !it.isRead && it.alertType == "URGENT_THEFT" }
        ?: alerts.firstOrNull { !it.isRead }

    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex < 2 } }

    Scaffold(
        modifier = modifier,
        containerColor = BrandBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.requiredSize(64.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("أمان", color = PureWhite, fontSize = 19.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فون", color = YemenGold, fontSize = 19.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                YemenFlagPill()
                            }
                            Text(
                                text = "المنظومة الوطنية لحماية الهواتف",
                                color = TextSecondaryLight,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    if (isSuperAdmin) {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) },
                            modifier = Modifier.testTag("admin_dashboard_top_button")
                        ) {
                            Text("👑", fontSize = 20.sp)
                        }
                    }
                    IconButton(
                        onClick = { showSearchWatchDialog = true },
                        modifier = Modifier.testTag("search_watch_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = "مراقبة المواصفات",
                            tint = YemenGold
                        )
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.LOGIN) },
                        modifier = Modifier.testTag("account_login_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BrandSurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "حسابي",
                                tint = PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.navigateTo(AppScreen.NEW_REPORT) },
                expanded = fabExpanded,
                icon = { Icon(Icons.Default.Add, contentDescription = "تقديم بلاغ") },
                text = { Text("تقديم بلاغ", fontWeight = FontWeight.Bold) },
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Search (primary action, like global apps)
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("ابحث برقم IMEI أو الموديل أو المنطقة", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = YemenGold) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextSecondaryLight)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BrandSurface,
                            unfocusedContainerColor = BrandSurface,
                            focusedBorderColor = YemenGold,
                            unfocusedBorderColor = BrandBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            cursorColor = YemenGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_field")
                    )
                }

                // 2. Urgent ticker (only when there is something unread)
                item {
                    UrgentAlertTicker(
                        latestAlert = latestUrgentAlert,
                        unreadCount = unreadCount,
                        onClick = { viewModel.navigateTo(AppScreen.ALERTS) }
                    )
                }

                // 3. Hero: IMEI check
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF14365C), Color(0xFF0D2138))
                                )
                            )
                            .testTag("quick_check_imei_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ناوي تشتري جوال؟", color = YemenGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("افحص رقم IMEI في ثواني", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text("تأكد إن الجهاز مو مبلّغ عنه مسروق أو مفقود", color = TextSecondaryLight, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.navigateTo(AppScreen.CHECK_IMEI) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = YemenGold),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = BrandInk, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("افحص الآن", color = BrandInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(YemenGold.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = YemenGold, modifier = Modifier.size(40.dp))
                            }
                        }
                    }
                }

                // 4. Stats
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "إجمالي البلاغات",
                            value = "${stats.totalReports}",
                            icon = Icons.Default.PhoneAndroid,
                            accentColor = BrandCyan,
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

                // 5. Account / cloud sync
                item {
                    FirebaseCloudAuthCard(onOpenLogin = { viewModel.navigateTo(AppScreen.LOGIN) })
                }

                // 6. Admin shortcut + test broadcast (admin / debug only)
                if (isSuperAdmin) {
                    item {
                        QuickRow(
                            icon = null,
                            emoji = "👑",
                            title = "لوحة تحكم المشرف العام",
                            subtitle = "إدارة البلاغات والحظر والتعاميم",
                            accent = YemenGold,
                            tag = "admin_quick_card"
                        ) { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) }
                    }
                }
                if (canSendTest) {
                    item {
                        QuickRow(
                            icon = Icons.Default.Campaign,
                            title = "إشعار تجريبي",
                            subtitle = "للمشرف فقط • لا يُنشر للمستخدمين",
                            accent = AlertRed,
                            tag = "quick_broadcast_alert_card"
                        ) { viewModel.simulateTheftBroadcast() }
                    }
                }

                // 7. Watch search (FCM)
                item {
                    QuickRow(
                        icon = Icons.Default.NotificationsActive,
                        title = if (searchQuery.isNotBlank()) "نبّهني عند ظهور: $searchQuery" else "راقب جهازاً معيّناً",
                        subtitle = "ننبهك أول ما يُضاف بلاغ مطابق للمواصفات",
                        accent = BrandCyan,
                        tag = "watch_search_chip_button"
                    ) { showSearchWatchDialog = true }
                }

                // 8. Filters
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrandChip("الكل", selectedType == null, "filter_all") { viewModel.filterType.value = null }
                            BrandChip("مسروقة", selectedType == "STOLEN", "filter_stolen", AlertRed) {
                                viewModel.filterType.value = if (selectedType == "STOLEN") null else "STOLEN"
                            }
                            BrandChip("مفقودة", selectedType == "LOST", "filter_lost", WarningAmber) {
                                viewModel.filterType.value = if (selectedType == "LOST") null else "LOST"
                            }
                            BrandChip("معثور عليها", selectedType == "FOUND", "filter_found", SuccessGreen) {
                                viewModel.filterType.value = if (selectedType == "FOUND") null else "FOUND"
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BrandChip("كل المحافظات", selectedGov == null, "filter_gov_all") { viewModel.filterGovernorate.value = null }
                            listOf("صنعاء (الأمانة)", "عدن", "تعز", "الحديدة", "إب", "حضرموت (المكلا)", "مأرب").forEach { gov ->
                                BrandChip(gov.substringBefore(" ("), selectedGov == gov, "filter_gov_$gov") {
                                    viewModel.filterGovernorate.value = if (selectedGov == gov) null else gov
                                }
                            }
                        }
                    }
                }

                // 9. Section header
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "آخر البلاغات",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(BrandSurfaceHigh)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("${reports.size}", color = YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 10. Reports
                if (reports.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(BrandSurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = YemenGold, modifier = Modifier.size(34.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("لا توجد بلاغات مطابقة", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                            Text("غيّر البحث أو الفلاتر وحاول مرة ثانية", fontSize = 12.sp, color = TextSecondaryLight)
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
            }

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
private fun QuickRow(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    accent: Color,
    tag: String,
    emoji: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = BrandSurface,
        border = BorderStroke(1.dp, BrandBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                } else if (emoji != null) {
                    Text(emoji, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = TextSecondaryLight, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("←", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrandChip(
    label: String,
    selected: Boolean,
    tag: String,
    accent: Color = YemenGold,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = BrandSurface,
            labelColor = PureWhite.copy(alpha = 0.8f),
            selectedContainerColor = accent,
            selectedLabelColor = BrandInk
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = BrandBorder,
            selectedBorderColor = accent
        ),
        modifier = Modifier.testTag(tag)
    )
}

/** How the current Firebase user signed in. */
private enum class SignInMethod { GOOGLE, EMAIL, PHONE, OTHER }

private fun FirebaseUser.signInMethod(): SignInMethod {
    val providers = providerData.map { it.providerId }
    return when {
        "google.com" in providers -> SignInMethod.GOOGLE
        "phone" in providers -> SignInMethod.PHONE
        "password" in providers -> SignInMethod.EMAIL
        !phoneNumber.isNullOrBlank() -> SignInMethod.PHONE
        !email.isNullOrBlank() -> SignInMethod.EMAIL
        else -> SignInMethod.OTHER
    }
}

/** +967712345678 -> +967 712 345 678 */
private fun formatYemeniPhone(raw: String?): String {
    val p = raw?.trim().orEmpty()
    if (p.startsWith("+967") && p.length == 13) {
        val local = p.substring(4)
        return "+967 ${local.substring(0, 3)} ${local.substring(3, 6)} ${local.substring(6)}"
    }
    return p
}

@Composable
fun FirebaseCloudAuthCard(
    modifier: Modifier = Modifier,
    onOpenLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser by FirebaseAuthManager.currentUser.collectAsStateWithLifecycle()

    val user = currentUser
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (user != null) GreenTint else BrandSurface,
        border = BorderStroke(1.dp, if (user != null) SuccessGreen.copy(alpha = 0.45f) else BrandBorder)
    ) {
        if (user != null) {
            val method = user.signInMethod()
            val title: String
            val subtitle: String
            when (method) {
                SignInMethod.GOOGLE -> {
                    title = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "حساب Google"
                    subtitle = if (!user.displayName.isNullOrBlank() && !user.email.isNullOrBlank()) "Google • ${user.email}" else "دخول عبر Google"
                }
                SignInMethod.EMAIL -> {
                    title = user.email ?: "حساب بريد إلكتروني"
                    subtitle = if (user.isEmailVerified) "البريد الإلكتروني • مفعّل ✅" else "البريد • بانتظار التفعيل ⚠️"
                }
                SignInMethod.PHONE -> {
                    title = formatYemeniPhone(user.phoneNumber).ifBlank { "رقم هاتف موثّق" }
                    subtitle = "رقم الهاتف • موثّق برمز SMS"
                }
                SignInMethod.OTHER -> {
                    title = user.displayName ?: "مستخدم أمان فون"
                    subtitle = "متصل بسحابة Firebase"
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (method == SignInMethod.GOOGLE) PureWhite else SuccessGreen),
                    contentAlignment = Alignment.Center
                ) {
                    when (method) {
                        SignInMethod.GOOGLE -> Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google",
                            modifier = Modifier.size(20.dp)
                        )
                        SignInMethod.EMAIL -> Icon(Icons.Default.Email, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                        SignInMethod.PHONE -> Icon(Icons.Default.Phone, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                        SignInMethod.OTHER -> Icon(Icons.Default.CloudDone, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, fontSize = 11.sp, color = PureWhite.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("المزامنة السحابية نشطة", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
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
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp).testTag("sign_out_button")
                ) {
                    Text("خروج", fontSize = 12.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(YemenGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = YemenGold, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("سجّل دخولك لمزامنة بلاغاتك", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PureWhite)
                    Text("Google أو البريد أو رقم الهاتف", fontSize = 11.sp, color = TextSecondaryLight)
                }
                Button(
                    onClick = onOpenLogin,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YemenGold),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp).testTag("google_sign_in_button")
                ) {
                    Text("دخول", fontSize = 12.sp, fontWeight = FontWeight.Black, color = BrandInk)
                }
            }
        }
    }
}
