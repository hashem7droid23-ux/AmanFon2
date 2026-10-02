package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.model.AlertEntity
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandBg
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandInk
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandSurfaceHigh
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.YemenGold
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: PhoneTrackerViewModel,
    onBack: () -> Unit,
    onOpenReport: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val alerts by viewModel.allAlerts.collectAsStateWithLifecycle(initialValue = emptyList())
    val unreadCount by viewModel.unreadAlertsCount.collectAsStateWithLifecycle()
    val isAdmin by com.example.util.AdminManager.isAdmin.collectAsStateWithLifecycle()
    val canSendTest = isAdmin || BuildConfig.DEBUG

    var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    Scaffold(
        modifier = modifier,
        containerColor = BrandBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "التنبيهات",
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(YemenGold)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$unreadCount جديد",
                                    color = BrandInk,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("alerts_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = { viewModel.markAllAlertsRead() },
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "قراءة الكل",
                                tint = YemenGold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Notification permission
            if (!hasNotificationPermission) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = BrandSurface,
                        border = BorderStroke(1.dp, YemenGold.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(YemenGold.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = YemenGold, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "فعّل الإشعارات لتصلك التنبيهات فوراً",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PureWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "عند سرقة أو فقدان هاتف في محافظتك بننبهك مباشرة.",
                                fontSize = 12.sp,
                                color = TextSecondaryLight
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = YemenGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Text("تفعيل الإشعارات", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandInk)
                            }
                        }
                    }
                }
            }

            // 2. Test broadcast (admin / debug only)
            if (canSendTest) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = BrandSurface,
                        border = BorderStroke(1.dp, BrandBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AlertRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = AlertRed, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("إشعار تجريبي", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("للمشرف فقط • لا يُنشر للمستخدمين", color = TextSecondaryLight, fontSize = 10.sp)
                            }
                            Button(
                                onClick = {
                                    if (hasNotificationPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        viewModel.simulateTheftBroadcast()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp).testTag("send_test_alert_button")
                            ) {
                                Text("إرسال", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. List
            if (alerts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(BrandSurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = YemenGold, modifier = Modifier.size(34.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("لا توجد تنبيهات حالياً", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("بنبلغك أول ما ينزل بلاغ جديد", fontSize = 12.sp, color = TextSecondaryLight)
                    }
                }
            } else {
                item {
                    Text(
                        text = "السجل (${alerts.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryLight,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(alerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        onClick = {
                            viewModel.markAlertRead(alert.id)
                            if (alert.reportId > 0) {
                                onOpenReport(alert.reportId)
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun AlertItemCard(
    alert: AlertEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent: Color
    val icon = when (alert.alertType) {
        "RECOVERY" -> { accent = SuccessGreen; Icons.Default.CheckCircle }
        "URGENT_THEFT" -> { accent = AlertRed; Icons.Default.Security }
        "SUPERVISOR_BROADCAST" -> { accent = YemenGold; Icons.Default.Campaign }
        else -> { accent = BrandCyan; Icons.Default.PhoneAndroid }
    }
    val unread = !alert.isRead

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("alert_card_${alert.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (unread) BrandSurfaceHigh else BrandSurface,
        border = BorderStroke(1.dp, if (unread) accent.copy(alpha = 0.55f) else BrandBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (unread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = alert.title,
                        fontWeight = if (unread) FontWeight.Black else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = PureWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatRelativeTime(alert.timestamp),
                        fontSize = 10.sp,
                        color = TextSecondaryLight
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = alert.message,
                    fontSize = 12.sp,
                    color = PureWhite.copy(alpha = 0.78f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = BrandBg.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "📍 ${alert.governorate}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    if (alert.reportId > 0) {
                        Text(
                            text = "التفاصيل ←",
                            fontSize = 12.sp,
                            color = YemenGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
