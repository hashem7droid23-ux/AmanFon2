package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReportEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedLight
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.AdminManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: PhoneTrackerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val allReports by viewModel.allReports.collectAsStateWithLifecycle(initialValue = emptyList())
    val bannedAccounts by AdminManager.bannedAccounts.collectAsState()
    val isSuperAdmin by AdminManager.isSuperAdmin.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var reportToDelete by remember { mutableStateOf<ReportEntity?>(null) }
    var userToBan by remember { mutableStateOf<Pair<String, String>?>(null) } // identifier to reason

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "👑 لوحة تحكم المشرف العام",
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Executive Admin Verification Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(WarningAmber, SuccessGreen, WarningAmber))
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_identity_card")
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(Navy700.copy(alpha = 0.5f), Navy900)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(WarningAmber.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👑", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "المهندس المصمم: هاشم القديمي",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PureWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "مشرف موثق",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = AdminManager.SUPER_ADMIN_EMAIL,
                                    fontSize = 12.sp,
                                    color = WarningAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "حساب المشرف العام والمسؤول عن إدارة البلاغات والمستخدمين",
                                    fontSize = 10.sp,
                                    color = PureWhite.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats summary pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Navy800,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("إجمالي البلاغات", fontSize = 10.sp, color = PureWhite.copy(alpha = 0.7f))
                                    Text("${allReports.size}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Navy800,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("الحسابات المحظورة", fontSize = 10.sp, color = AlertRed)
                                    Text("${bannedAccounts.size}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Navy800,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("صلاحية المشرف", fontSize = 10.sp, color = SuccessGreen)
                                    Text(if (isSuperAdmin) "مفعلة ✅" else "غير نشطة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Tab Navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        label = { Text("الرقابة على البلاغات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarningAmber,
                            selectedLabelColor = Navy900,
                            containerColor = Navy800,
                            labelColor = PureWhite
                        ),
                        modifier = Modifier.weight(1f).testTag("tab_supervise_reports")
                    )
                    FilterChip(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        label = { Text("الحسابات المحظورة (${bannedAccounts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AlertRed,
                            selectedLabelColor = PureWhite,
                            containerColor = Navy800,
                            labelColor = PureWhite
                        ),
                        modifier = Modifier.weight(1.2f).testTag("tab_banned_accounts")
                    )
                    FilterChip(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        label = { Text("بث تعميم رسمي", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessGreen,
                            selectedLabelColor = PureWhite,
                            containerColor = Navy800,
                            labelColor = PureWhite
                        ),
                        modifier = Modifier.weight(1.1f).testTag("tab_broadcast_supervisor")
                    )
                }
            }

            // 3. Tab Contents
            when (selectedTabIndex) {
                0 -> {
                    // TAB 0: Supervise & Manage Reports
                    item {
                        Text(
                            text = "جميع البلاغات المسجلة في المنظومة (تحكم وحذف كامل للمشرف):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }

                    if (allReports.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Navy800),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("لا توجد بلاغات مسجلة حالياً", color = PureWhite.copy(alpha = 0.7f), fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(allReports, key = { it.id }) { report ->
                            AdminReportSupervisionCard(
                                report = report,
                                onDelete = { reportToDelete = report },
                                onBanOwner = {
                                    userToBan = Pair(report.primaryPhone, "مخالفة في البلاغ #${report.id} (${report.brand} ${report.model})")
                                },
                                onToggleRecovered = {
                                    viewModel.toggleRecovered(report.id, !report.isRecovered)
                                    Toast.makeText(context, "تم تحديث حالة البلاغ كمشرف", Toast.LENGTH_SHORT).show()
                                },
                                onOpenDetails = { viewModel.openReportDetails(report.id) }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: Banned Accounts & Phone Numbers
                    item {
                        AdminBannedAccountsSection(
                            bannedList = bannedAccounts,
                            onBanNew = { identifier, reason ->
                                AdminManager.banAccount(identifier, reason)
                                Toast.makeText(context, "تم إدراج [$identifier] في قائمة الحظر بنجاح 🚫", Toast.LENGTH_SHORT).show()
                            },
                            onUnban = { id, identifier ->
                                AdminManager.unbanAccount(id)
                                Toast.makeText(context, "تم فك الحظر عن [$identifier] بنجاح ✅", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                2 -> {
                    // TAB 2: Supervisor Broadcast Announcement
                    item {
                        AdminSupervisorBroadcastSection(
                            onSendBroadcast = { title, msg, isUrgent ->
                                viewModel.sendSupervisorBroadcast(title, msg, isUrgent, context)
                                Toast.makeText(context, "تم إرسال التعميم السحابي لكافة المستخدمين بنجاح! 📢", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Report Deletion
    if (reportToDelete != null) {
        val rep = reportToDelete!!
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = { Text("تأكيد حذف البلاغ كمشرف عام", fontWeight = FontWeight.Bold, color = AlertRed) },
            text = {
                Text(
                    text = "هل أنت متأكد يا باشمهندس هاشم من حذف البلاغ #${rep.id} نهائياً؟\n\nالجهاز: ${rep.brand} ${rep.model}\nصاحب البلاغ: ${rep.contactName} (${rep.primaryPhone})",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminDeleteReport(rep.id)
                        Toast.makeText(context, "تم حذف البلاغ #${rep.id} بنجاح", Toast.LENGTH_SHORT).show()
                        reportToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("حذف البلاغ فوراً", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirmation Dialog for User Ban
    if (userToBan != null) {
        val (ident, defaultReason) = userToBan!!
        var banReason by remember { mutableStateOf(defaultReason) }
        AlertDialog(
            onDismissRequest = { userToBan = null },
            title = { Text("حظر حساب / رقم هاتف", fontWeight = FontWeight.Bold, color = AlertRed) },
            text = {
                Column {
                    Text("أنت بصدد حظر [$ident] من استخدام وتنزيل البلاغات في المنظومة:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = banReason,
                        onValueChange = { banReason = it },
                        label = { Text("سبب الحظر") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AdminManager.banAccount(ident, banReason)
                        Toast.makeText(context, "تم حظر [$ident] بنجاح ومنعه من إضافة أي بلاغات", Toast.LENGTH_SHORT).show()
                        userToBan = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("تأكيد الحظر 🚫", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToBan = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AdminReportSupervisionCard(
    report: ReportEntity,
    onDelete: () -> Unit,
    onBanOwner: () -> Unit,
    onToggleRecovered: () -> Unit,
    onOpenDetails: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy800),
        border = BorderStroke(1.dp, Navy700),
        modifier = Modifier.fillMaxWidth().testTag("admin_report_card_${report.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (report.reportType == "STOLEN") AlertRed else WarningAmber)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (report.reportType == "STOLEN") "سرقة 🚨" else "فقدان ⚠️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#${report.id} - ${report.brand} ${report.model}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PureWhite
                    )
                }

                if (report.isRecovered) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SuccessGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "تم الاسترجاع ✅",
                            fontSize = 10.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "📍 المحافظة: ${report.governorate} (${report.incidentLocation})",
                fontSize = 11.sp,
                color = PureWhite.copy(alpha = 0.8f)
            )
            Text(
                text = "👤 صاحب البلاغ: ${report.contactName} | 📞 ${report.primaryPhone}",
                fontSize = 11.sp,
                color = WarningAmber
            )
            Text(
                text = "IMEI: ${report.maskedImei}",
                fontSize = 11.sp,
                color = PureWhite.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Admin Control Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp).testTag("admin_delete_btn_${report.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف البلاغ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onBanOwner,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp).testTag("admin_ban_btn_${report.id}")
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حظر الرقم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenDetails,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text("التفاصيل", fontSize = 11.sp, color = PureWhite)
                }
            }
        }
    }
}

@Composable
fun AdminBannedAccountsSection(
    bannedList: List<com.example.util.BannedAccount>,
    onBanNew: (String, String) -> Unit,
    onUnban: (String, String) -> Unit
) {
    var newIdentifier by remember { mutableStateOf("") }
    var newReason by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Form to Ban New
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Navy800),
            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🚫 حظر رقم هاتف أو حساب جديد:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlertRed
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newIdentifier,
                    onValueChange = { newIdentifier = it },
                    label = { Text("رقم الهاتف أو البريد أو اسم المستخدم") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AlertRed,
                        unfocusedBorderColor = Navy700,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("ban_identifier_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newReason,
                    onValueChange = { newReason = it },
                    label = { Text("سبب الحظر (مثال: بلاغات كاذبة أو تلاعب)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AlertRed,
                        unfocusedBorderColor = Navy700,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("ban_reason_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (newIdentifier.isNotBlank()) {
                            onBanNew(newIdentifier.trim(), newReason.trim())
                            newIdentifier = ""
                            newReason = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_ban_button")
                ) {
                    Icon(Icons.Default.PersonRemove, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إدراج في قائمة الحظر ومنعه من النشر", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Banned List
        Text(
            text = "قائمة الحسابات والأرقام المحظورة حالياً (${bannedList.size}):",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )

        if (bannedList.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Navy800,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد أرقام محظورة حالياً", color = PureWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        } else {
            bannedList.forEach { item ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(item.identifier, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PureWhite)
                            }
                            Text("السبب: ${item.reason}", fontSize = 11.sp, color = PureWhite.copy(alpha = 0.8f))
                            Text("بواسطة: ${item.bannedBy}", fontSize = 10.sp, color = WarningAmber)
                        }

                        Button(
                            onClick = { onUnban(item.id, item.identifier) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("unban_btn_${item.id}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فك الحظر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSupervisorBroadcastSection(
    onSendBroadcast: (String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("تعميم أمني عاجل من المشرف العام") }
    var message by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy800),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().testTag("supervisor_broadcast_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "بث تعميم أمني لكافة مستخدمي أمان فون",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PureWhite
                    )
                    Text(
                        text = "سيصل كإشعار فوري داخل التطبيق وعبر قنوات التنبيه",
                        fontSize = 10.sp,
                        color = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان التعميم") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarningAmber,
                    unfocusedBorderColor = Navy700,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                modifier = Modifier.fillMaxWidth().testTag("broadcast_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("نص الرسالة أو التعميم للمحلات والمستخدمين") },
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarningAmber,
                    unfocusedBorderColor = Navy700,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                modifier = Modifier.fillMaxWidth().testTag("broadcast_message_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isUrgent) AlertRed else WarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تعميم عاجل عالي الأهمية 🚨",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }

                Switch(
                    checked = isUrgent,
                    onCheckedChange = { isUrgent = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = AlertRed, checkedTrackColor = Navy900)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && message.isNotBlank()) {
                        onSendBroadcast(title.trim(), message.trim(), isUrgent)
                        message = ""
                    }
                },
                enabled = title.isNotBlank() && message.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("send_broadcast_button")
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📢 إرسال وبث التعميم فوراً لجميع المستخدمين",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PureWhite
                )
            }
        }
    }
}
