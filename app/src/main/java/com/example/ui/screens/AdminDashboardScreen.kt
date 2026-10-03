package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReportEntity
import com.example.data.remote.AdminCloud
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.shortBrand
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
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.YemenGold
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.AdminManager
import kotlinx.coroutines.launch

private data class AdminTab(val title: String, val ownerOnly: Boolean = false)

private val ADMIN_TABS = listOf(
    AdminTab("نظرة عامة"),
    AdminTab("البلاغات"),
    AdminTab("الحظر"),
    AdminTab("التعاميم"),
    AdminTab("الفريق", ownerOnly = true),
    AdminTab("التحكم"),
    AdminTab("السجل")
)

@Composable
private fun adminFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = YemenGold,
    unfocusedBorderColor = BrandBorder,
    focusedLabelColor = YemenGold,
    unfocusedLabelColor = TextSecondaryLight,
    focusedTextColor = PureWhite,
    unfocusedTextColor = PureWhite,
    cursorColor = YemenGold,
    focusedContainerColor = BrandBg,
    unfocusedContainerColor = BrandBg
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: PhoneTrackerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allReports by viewModel.allReports.collectAsStateWithLifecycle(initialValue = emptyList())
    val bans by AdminManager.bannedAccounts.collectAsStateWithLifecycle()
    val isAdmin by AdminManager.isAdmin.collectAsStateWithLifecycle()
    val isOwner by AdminManager.isOwner.collectAsStateWithLifecycle()
    val config by AdminCloud.config.collectAsStateWithLifecycle()
    val logs by AdminCloud.logs.collectAsStateWithLifecycle()

    val tabs = ADMIN_TABS.filter { !it.ownerOnly || isOwner }
    var tab by remember { mutableIntStateOf(0) }
    if (tab >= tabs.size) tab = 0

    var reportToDelete by remember { mutableStateOf<ReportEntity?>(null) }
    var banTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun toastResult(r: Result<Unit>, ok: String) {
        Toast.makeText(context, if (r.isSuccess) ok else (r.exceptionOrNull()?.message ?: "تعذر التنفيذ"), Toast.LENGTH_LONG).show()
    }
    fun launchOp(ok: String, block: suspend () -> Result<Unit>) {
        scope.launch { toastResult(block(), ok) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = BrandBg,
        topBar = {
            Column(Modifier.background(BrandBg)) {
                TopAppBar(
                    title = {
                        Column {
                            Text("لوحة الإدارة", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (isOwner) "المشرف العام • صلاحيات كاملة" else if (isAdmin) "مشرف • صلاحيات الرقابة" else "لا توجد صلاحية",
                                color = if (isAdmin) YemenGold else AlertRed, fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PureWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { i, t ->
                        FilterChip(
                            selected = tab == i,
                            onClick = { tab = i },
                            label = { Text(t.title, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = BrandSurface,
                                labelColor = PureWhite.copy(alpha = 0.8f),
                                selectedContainerColor = YemenGold,
                                selectedLabelColor = BrandInk
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = tab == i,
                                borderColor = BrandBorder, selectedBorderColor = YemenGold
                            ),
                            modifier = Modifier.testTag("admin_tab_$i")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (!isAdmin) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("هذه الصفحة للمشرفين فقط", color = TextSecondaryLight)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tabs[tab].title) {
                "نظرة عامة" -> item { OverviewTab(allReports, bans.size, config) }

                "البلاغات" -> {
                    item {
                        ReportsAdminList(
                            reports = allReports,
                            config = config,
                            onOpen = { viewModel.openReportDetails(it.id) },
                            onToggleRecovered = { r ->
                                viewModel.toggleRecovered(r.id, !r.isRecovered)
                                scope.launch { AdminCloud.log(if (r.isRecovered) "إلغاء الاسترجاع" else "توثيق استرجاع", "#${r.id} ${r.model}") }
                                Toast.makeText(context, "تم تحديث الحالة", Toast.LENGTH_SHORT).show()
                            },
                            onHide = { r, hide -> launchOp(if (hide) "تم إخفاء البلاغ عن المستخدمين" else "تم إظهار البلاغ") { AdminCloud.setHidden(r.imei1, hide, "#${r.id} ${r.model}") } },
                            onPin = { r, pin -> launchOp(if (pin) "تم تثبيت البلاغ أعلى التطبيق 📌" else "تم إلغاء التثبيت") { AdminCloud.setPinned(if (pin) r.imei1 else "", "#${r.id} ${r.model}") } },
                            onBan = { r -> banTarget = r.primaryPhone to "مخالفة في البلاغ #${r.id} (${shortBrand(r.brand)} ${r.model})" },
                            onDelete = { reportToDelete = it }
                        )
                    }
                }

                "الحظر" -> item {
                    BansTab(
                        bans = bans,
                        onBan = { id, reason -> banTarget = id to reason },
                        onUnban = { b ->
                            AdminManager.unbanAccount(b.id) { ok, err ->
                                scope.launch { Toast.makeText(context, if (ok) "تم فك الحظر عن ${b.identifier} ✅" else (err ?: "تعذر"), Toast.LENGTH_SHORT).show() }
                            }
                        }
                    )
                }

                "التعاميم" -> item {
                    BroadcastTab(onSend = { title, msg, urgent ->
                        viewModel.sendSupervisorBroadcast(title, msg, urgent, context)
                        Toast.makeText(context, "تم بث التعميم 📢", Toast.LENGTH_LONG).show()
                    })
                }

                "الفريق" -> item {
                    TeamTab(
                        moderators = config.moderators,
                        onAdd = { email -> launchOp("تمت إضافة $email كمشرف ✅") { AdminCloud.addModerator(email) } },
                        onRemove = { email -> launchOp("تمت إزالة $email من المشرفين") { AdminCloud.removeModerator(email) } }
                    )
                }

                "التحكم" -> item {
                    ControlTab(
                        isOwner = isOwner,
                        config = config,
                        onAnnouncement = { text -> launchOp(if (text.isBlank()) "تم حذف الإعلان" else "تم نشر الإعلان لكل المستخدمين") { AdminCloud.setAnnouncement(text) } },
                        onPause = { p -> launchOp(if (p) "تم إيقاف نشر البلاغات مؤقتاً" else "تم استئناف نشر البلاغات") { AdminCloud.setReportsPaused(p) } },
                        onMaintenance = { on, msg -> launchOp(if (on) "تم تفعيل وضع الصيانة" else "تم إيقاف وضع الصيانة") { AdminCloud.setMaintenance(on, msg) } }
                    )
                }

                "السجل" -> {
                    if (logs.isEmpty()) {
                        item { EmptyNote("لا توجد عمليات مسجلة بعد") }
                    } else {
                        items(logs, key = { it.id }) { log ->
                            Surface(shape = RoundedCornerShape(14.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(log.action, color = YemenGold, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        Text(if (log.at > 0) formatRelativeTime(log.at) else "الآن", color = TextSecondaryLight, fontSize = 11.sp)
                                    }
                                    Text(log.target, color = PureWhite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (log.details.isNotBlank()) Text(log.details, color = TextSecondaryLight, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("بواسطة: ${log.by}", color = BrandCyan, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reportToDelete?.let { rep ->
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            containerColor = BrandSurface,
            title = { Text("حذف البلاغ #${rep.id} نهائياً؟", color = PureWhite, fontWeight = FontWeight.Bold) },
            text = { Text("${shortBrand(rep.brand)} ${rep.model}\nصاحب البلاغ: ${rep.contactName} (${rep.primaryPhone})\n\nلا يمكن التراجع. إذا كان مشبوهاً فقط، استخدم الإخفاء بدلاً من الحذف.", color = TextSecondaryLight) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.adminDeleteReport(rep.id)
                    scope.launch { AdminCloud.log("حذف بلاغ", "#${rep.id} ${rep.model}", rep.primaryPhone) }
                    Toast.makeText(context, "تم حذف البلاغ", Toast.LENGTH_SHORT).show()
                    reportToDelete = null
                }) { Text("حذف", color = AlertRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { reportToDelete = null }) { Text("إلغاء", color = PureWhite) } }
        )
    }

    banTarget?.let { (ident, defaultReason) ->
        var reason by remember(ident) { mutableStateOf(defaultReason) }
        AlertDialog(
            onDismissRequest = { banTarget = null },
            containerColor = BrandSurface,
            title = { Text("حظر $ident", color = PureWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("سيُمنع من نشر أي بلاغ من كل الأجهزة.", color = TextSecondaryLight, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("سبب الحظر") }, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = ident
                    AdminManager.banAccount(target, reason) { ok, err ->
                        scope.launch { Toast.makeText(context, if (ok) "تم حظر $target 🚫" else (err ?: "تعذر الحظر"), Toast.LENGTH_LONG).show() }
                    }
                    banTarget = null
                }) { Text("حظر", color = AlertRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { banTarget = null }) { Text("إلغاء", color = PureWhite) } }
        )
    }
}

// ============================ Overview ============================

@Composable
private fun OverviewTab(reports: List<ReportEntity>, bansCount: Int, config: com.example.data.remote.AppRemoteConfig) {
    val now = System.currentTimeMillis()
    val day = 24 * 60 * 60 * 1000L
    val stolen = reports.count { it.reportType == "STOLEN" }
    val lost = reports.count { it.reportType == "LOST" }
    val found = reports.count { it.reportType == "FOUND" }
    val recovered = reports.count { it.isRecovered }
    val rate = if (reports.isEmpty()) 0 else (recovered * 100 / reports.size)
    val last7 = (6 downTo 0).map { d ->
        val start = now - (d + 1) * day
        val end = now - d * day
        reports.count { it.createdAt in start until end }
    }
    val byGov = reports.groupBy { it.governorate.substringBefore(" (") }.mapValues { it.value.size }
        .entries.sortedByDescending { it.value }.take(6)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (config.maintenanceMode || config.reportsPaused) {
            Surface(shape = RoundedCornerShape(14.dp), color = AlertRed.copy(alpha = 0.15f), border = BorderStroke(1.dp, AlertRed)) {
                Text(
                    buildString {
                        if (config.maintenanceMode) append("⚠️ وضع الصيانة مفعّل. ")
                        if (config.reportsPaused) append("⏸️ نشر البلاغات موقوف.")
                    },
                    color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard("البلاغات", "${reports.size}", BrandCyan, Modifier.weight(1f))
            KpiCard("الاسترجاع", "$rate%", SuccessGreen, Modifier.weight(1f))
            KpiCard("محظورون", "$bansCount", AlertRed, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard("مسروقة", "$stolen", AlertRed, Modifier.weight(1f))
            KpiCard("مفقودة", "$lost", WarningAmber, Modifier.weight(1f))
            KpiCard("معثور عليها", "$found", SuccessGreen, Modifier.weight(1f))
        }

        Panel("البلاغات آخر 7 أيام") {
            val max = (last7.maxOrNull() ?: 0).coerceAtLeast(1)
            Row(
                Modifier.fillMaxWidth().height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                last7.forEachIndexed { i, v ->
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$v", color = PureWhite, fontSize = 10.sp)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((70f * v / max).coerceAtLeast(3f).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(if (i == 6) YemenGold else BrandCyan.copy(alpha = 0.6f))
                        )
                        Text(if (i == 6) "اليوم" else "-${6 - i}", color = TextSecondaryLight, fontSize = 9.sp)
                    }
                }
            }
        }

        Panel("أكثر المحافظات بلاغات") {
            if (byGov.isEmpty()) Text("لا توجد بيانات", color = TextSecondaryLight, fontSize = 12.sp)
            val max = (byGov.firstOrNull()?.value ?: 1).coerceAtLeast(1)
            byGov.forEach { (gov, count) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(gov, color = PureWhite, fontSize = 12.sp, modifier = Modifier.width(80.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(BrandSurfaceHigh)) {
                        Box(Modifier.fillMaxWidth(count.toFloat() / max).height(10.dp).background(YemenGold))
                    }
                    Text("  $count", color = YemenGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun KpiCard(title: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(title, color = TextSecondaryLight, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Panel(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            content()
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TextSecondaryLight, fontSize = 13.sp)
    }
}

// ============================ Reports ============================

@Composable
private fun ReportsAdminList(
    reports: List<ReportEntity>,
    config: com.example.data.remote.AppRemoteConfig,
    onOpen: (ReportEntity) -> Unit,
    onToggleRecovered: (ReportEntity) -> Unit,
    onHide: (ReportEntity, Boolean) -> Unit,
    onPin: (ReportEntity, Boolean) -> Unit,
    onBan: (ReportEntity) -> Unit,
    onDelete: (ReportEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    val filters = listOf("ALL" to "الكل", "STOLEN" to "مسروقة", "LOST" to "مفقودة", "FOUND" to "معثور", "RECOVERED" to "مسترجعة", "HIDDEN" to "مخفية")

    val shown = reports.filter { r ->
        val q = query.trim()
        val mq = q.isBlank() || r.model.contains(q, true) || r.brand.contains(q, true) || r.imei1.contains(q) ||
            r.primaryPhone.contains(q) || r.contactName.contains(q, true) || r.governorate.contains(q) || "#${r.id}" == q
        val mf = when (filter) {
            "ALL" -> true
            "RECOVERED" -> r.isRecovered
            "HIDDEN" -> r.imei1 in config.hiddenImeis
            else -> r.reportType == filter
        }
        mq && mf
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("ابحث: IMEI، رقم، اسم، موديل، #رقم البلاغ", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = YemenGold) },
            singleLine = true, shape = RoundedCornerShape(14.dp), colors = adminFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("admin_reports_search")
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            filters.forEach { (k, label) ->
                FilterChip(
                    selected = filter == k, onClick = { filter = k },
                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = BrandSurface, labelColor = PureWhite.copy(alpha = 0.8f),
                        selectedContainerColor = BrandCyan, selectedLabelColor = BrandInk
                    )
                )
            }
        }
        Text("${shown.size} بلاغ", color = TextSecondaryLight, fontSize = 12.sp)

        if (shown.isEmpty()) EmptyNote("لا توجد بلاغات مطابقة")
        shown.take(200).forEach { r ->
            val hidden = r.imei1 in config.hiddenImeis
            val pinned = config.pinnedImei.isNotBlank() && r.imei1 == config.pinnedImei
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = BrandSurface,
                border = BorderStroke(1.dp, if (pinned) YemenGold else if (hidden) AlertRed.copy(alpha = 0.5f) else BrandBorder),
                modifier = Modifier.fillMaxWidth().testTag("admin_report_card_${r.id}")
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("#${r.id} • ${shortBrand(r.brand)}", color = YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(r.model, color = PureWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        StatusBadge(reportType = r.reportType, isRecovered = r.isRecovered)
                    }
                    if (pinned || hidden) {
                        Text(
                            listOfNotNull(if (pinned) "📌 مثبّت" else null, if (hidden) "🙈 مخفي عن المستخدمين" else null).joinToString("  •  "),
                            color = if (hidden) AlertRed else YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Text("📍 ${r.governorate} • ${r.incidentLocation}", color = TextSecondaryLight, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("👤 ${r.contactName} • ${r.primaryPhone}  •  IMEI ${r.imei1}", color = TextSecondaryLight, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    HorizontalDivider(color = BrandBorder)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip(Icons.Default.Search, "التفاصيل", BrandCyan) { onOpen(r) }
                        ActionChip(Icons.Default.CheckCircle, if (r.isRecovered) "إلغاء الاسترجاع" else "تم الاسترجاع", SuccessGreen) { onToggleRecovered(r) }
                        ActionChip(Icons.Default.PushPin, if (pinned) "إلغاء التثبيت" else "تثبيت", YemenGold) { onPin(r, !pinned) }
                        ActionChip(if (hidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, if (hidden) "إظهار" else "إخفاء", WarningAmber) { onHide(r, !hidden) }
                        ActionChip(Icons.Default.Block, "حظر الرقم", PureWhite) { onBan(r) }
                        ActionChip(Icons.Default.Delete, "حذف", AlertRed) { onDelete(r) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f), border = BorderStroke(1.dp, color.copy(alpha = 0.4f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ============================ Bans ============================

@Composable
private fun BansTab(
    bans: List<com.example.util.BannedAccount>,
    onBan: (String, String) -> Unit,
    onUnban: (com.example.util.BannedAccount) -> Unit
) {
    var ident by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Panel("حظر رقم أو بريد") {
            Text("الحظر سحابي: يطبّق على كل الأجهزة، وقاعدة البيانات ترفض أي بلاغ من المحظور.", color = TextSecondaryLight, fontSize = 12.sp)
            OutlinedTextField(value = ident, onValueChange = { ident = it }, label = { Text("رقم الهاتف أو البريد") }, singleLine = true, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("ban_identifier_input"))
            OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("السبب") }, singleLine = true, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("ban_reason_input"))
            Button(
                onClick = { if (ident.isNotBlank()) { onBan(ident.trim(), reason.trim()); ident = ""; reason = "" } },
                enabled = ident.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = PureWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("confirm_ban_button")
            ) {
                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("حظر", fontWeight = FontWeight.Bold)
            }
        }
        Text("المحظورون (${bans.size})", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        if (bans.isEmpty()) EmptyNote("لا يوجد محظورون")
        bans.forEach { b ->
            Surface(shape = RoundedCornerShape(14.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.identifier, color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(b.reason, color = TextSecondaryLight, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${b.bannedBy}${if (b.bannedAt > 0) " • " + formatRelativeTime(b.bannedAt) else ""}", color = BrandCyan, fontSize = 10.sp)
                    }
                    TextButton(onClick = { onUnban(b) }, modifier = Modifier.testTag("unban_btn_${b.id}")) {
                        Text("فك الحظر", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================ Broadcast ============================

@Composable
private fun BroadcastTab(onSend: (String, String, Boolean) -> Unit) {
    var title by remember { mutableStateOf("تعميم أمني من إدارة أمان فون") }
    var message by remember { mutableStateOf("") }
    var urgent by remember { mutableStateOf(true) }
    Panel("بث تعميم لكل المستخدمين") {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("العنوان") }, singleLine = true, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("broadcast_title_input"))
        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("نص التعميم") }, minLines = 3, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("broadcast_message_input"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("عاجل 🚨", color = PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(checked = urgent, onCheckedChange = { urgent = it }, colors = SwitchDefaults.colors(checkedTrackColor = AlertRed))
        }
        Button(
            onClick = { onSend(title.trim(), message.trim(), urgent); message = "" },
            enabled = title.isNotBlank() && message.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = BrandInk),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_broadcast_button")
        ) {
            Icon(Icons.Default.Campaign, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("بث التعميم الآن", fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ============================ Team (owner) ============================

@Composable
private fun TeamTab(moderators: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var toRemove by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Panel("إضافة مشرف") {
            Text(
                "المشرف يقدر يدير البلاغات (إخفاء، تثبيت، حذف، توثيق الاسترجاع)، ويحظر ويبث تعاميم وينشر إعلانات. " +
                    "إضافة المشرفين والصيانة وإيقاف البلاغات تبقى لك وحدك. لازم يدخل المشرف ببريد مفعّل.",
                color = TextSecondaryLight, fontSize = 12.sp, lineHeight = 18.sp
            )
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("بريد المشرف") }, singleLine = true, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("moderator_email_input"))
            Button(
                onClick = { onAdd(email.trim()); email = "" },
                enabled = email.contains("@"),
                colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = BrandInk),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("add_moderator_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("إضافة للفريق", fontWeight = FontWeight.Bold)
            }
        }
        Text("الفريق", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        TeamRow(AdminManager.SUPER_ADMIN_EMAIL, "المشرف العام", YemenGold, null)
        if (moderators.isEmpty()) Text("لا يوجد مشرفون إضافيون", color = TextSecondaryLight, fontSize = 12.sp)
        moderators.forEach { m -> TeamRow(m, "مشرف", BrandCyan) { toRemove = m } }
    }
    toRemove?.let { m ->
        AlertDialog(
            onDismissRequest = { toRemove = null },
            containerColor = BrandSurface,
            title = { Text("إزالة $m من المشرفين؟", color = PureWhite, fontWeight = FontWeight.Bold) },
            confirmButton = { TextButton(onClick = { onRemove(m); toRemove = null }) { Text("إزالة", color = AlertRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { toRemove = null }) { Text("إلغاء", color = PureWhite) } }
        )
    }
}

@Composable
private fun TeamRow(email: String, role: String, color: Color, onRemove: (() -> Unit)?) {
    Surface(shape = RoundedCornerShape(14.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(email.take(1).uppercase(), color = color, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(email, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(role, color = color, fontSize = 11.sp)
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) { Icon(Icons.Default.PersonRemove, contentDescription = "إزالة", tint = AlertRed) }
            }
        }
    }
}

// ============================ Control ============================

@Composable
private fun ControlTab(
    isOwner: Boolean,
    config: com.example.data.remote.AppRemoteConfig,
    onAnnouncement: (String) -> Unit,
    onPause: (Boolean) -> Unit,
    onMaintenance: (Boolean, String) -> Unit
) {
    var announcement by remember(config.announcement) { mutableStateOf(config.announcement) }
    var maintMsg by remember(config.maintenanceMessage) { mutableStateOf(config.maintenanceMessage.ifBlank { "التطبيق تحت الصيانة مؤقتاً، نرجع لكم قريباً." }) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel("📢 إعلان أعلى التطبيق") {
            Text("يظهر لكل المستخدمين فوق شريط التنقل.", color = TextSecondaryLight, fontSize = 12.sp)
            OutlinedTextField(value = announcement, onValueChange = { announcement = it.take(300) }, label = { Text("نص الإعلان") }, minLines = 2, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("announcement_input"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAnnouncement(announcement) },
                    enabled = announcement.isNotBlank() && announcement != config.announcement,
                    colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = BrandInk),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                ) { Text("نشر", fontWeight = FontWeight.Bold) }
                if (config.announcement.isNotBlank()) {
                    Button(
                        onClick = { announcement = ""; onAnnouncement("") },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandSurfaceHigh, contentColor = PureWhite),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                    ) { Text("حذف الإعلان") }
                }
            }
        }

        if (isOwner) {
            Panel("⏸️ إيقاف نشر البلاغات") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (config.reportsPaused) "النشر موقوف حالياً لكل المستخدمين" else "المستخدمون يقدرون ينشرون بلاغات",
                        color = if (config.reportsPaused) AlertRed else PureWhite, fontSize = 13.sp, modifier = Modifier.weight(1f)
                    )
                    Switch(checked = config.reportsPaused, onCheckedChange = onPause, colors = SwitchDefaults.colors(checkedTrackColor = AlertRed))
                }
            }
            Panel("🛠️ وضع الصيانة") {
                Text("يقفل التطبيق على كل المستخدمين ما عدا المشرفين.", color = TextSecondaryLight, fontSize = 12.sp)
                OutlinedTextField(value = maintMsg, onValueChange = { maintMsg = it.take(200) }, label = { Text("رسالة الصيانة") }, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (config.maintenanceMode) "مفعّل" else "غير مفعّل", color = if (config.maintenanceMode) AlertRed else PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(checked = config.maintenanceMode, onCheckedChange = { onMaintenance(it, maintMsg) }, colors = SwitchDefaults.colors(checkedTrackColor = AlertRed))
                }
            }
        } else {
            Text("الصيانة وإيقاف البلاغات متاحة للمشرف العام فقط.", color = TextSecondaryLight, fontSize = 12.sp)
        }
    }
}
