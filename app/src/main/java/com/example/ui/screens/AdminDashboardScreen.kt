package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.remote.AppRemoteConfig
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.shortBrand
import com.example.ui.theme.*
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.AdminManager
import kotlinx.coroutines.launch

private data class AdminTab(val title: String, val ownerOnly: Boolean = false)
private val ADMIN_TABS = listOf(AdminTab("نظرة عامة"), AdminTab("البلاغات"), AdminTab("الحظر"), AdminTab("التعاميم"), AdminTab("الفريق", true), AdminTab("التحكم"), AdminTab("السجل"))
@Composable private fun adminFieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = BrandBorder, focusedLabelColor = YemenGold, unfocusedLabelColor = TextSecondaryLight, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, cursorColor = YemenGold, focusedContainerColor = BrandBg, unfocusedContainerColor = BrandBg)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: PhoneTrackerViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reports by viewModel.allReports.collectAsStateWithLifecycle(initialValue = emptyList())
    val bans by AdminManager.bannedAccounts.collectAsStateWithLifecycle()
    val admin by AdminManager.isAdmin.collectAsStateWithLifecycle()
    val owner by AdminManager.isOwner.collectAsStateWithLifecycle()
    val config by AdminCloud.config.collectAsStateWithLifecycle()
    val logs by AdminCloud.logs.collectAsStateWithLifecycle()
    val tabs = ADMIN_TABS.filter { !it.ownerOnly || owner }
    var tab by remember { mutableIntStateOf(0) }
    if (tab >= tabs.size) tab = 0
    var deleteTarget by remember { mutableStateOf<ReportEntity?>(null) }
    var banTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var operationPending by remember { mutableStateOf(false) }
    fun toast(message: String) { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    fun launchOp(ok: String, block: suspend () -> Result<Unit>) { scope.launch { val result = block(); toast(if (result.isSuccess) ok else result.exceptionOrNull()?.message ?: "تعذر التنفيذ") } }
    Scaffold(modifier = modifier, containerColor = BrandBg, topBar = {
        Column(Modifier.background(BrandBg)) {
            TopAppBar(title = { Column {
                Text("لوحة الإدارة", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (owner) "المشرف العام • صلاحيات كاملة" else if (admin) "مشرف • صلاحيات الرقابة" else "لا توجد صلاحية", color = if (admin) YemenGold else AlertRed, fontSize = 11.sp)
            } }, navigationIcon = { IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = PureWhite) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { i, t -> FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(t.title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }, shape = RoundedCornerShape(50), colors = FilterChipDefaults.filterChipColors(containerColor = BrandSurface, labelColor = PureWhite.copy(alpha = 0.8f), selectedContainerColor = YemenGold, selectedLabelColor = BrandInk), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = tab == i, borderColor = BrandBorder, selectedBorderColor = YemenGold), modifier = Modifier.testTag("admin_tab_$i")) }
            }
            if (operationPending) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }) { padding ->
        if (!admin) { Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("هذه الصفحة للمشرفين فقط", color = TextSecondaryLight) }; return@Scaffold }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (tabs[tab].title) {
                "نظرة عامة" -> item { OverviewTab(reports, bans.size, config) }
                "البلاغات" -> item {
                    ReportsAdminList(reports, config,
                        onOpen = { viewModel.openReportDetails(it.id) },
                        onToggleRecovered = { r ->
                            if (!operationPending) {
                                operationPending = true
                                viewModel.toggleRecovered(r.id, !r.isRecovered,
                                    onSuccess = {
                                        operationPending = false
                                        scope.launch { AdminCloud.log(if (r.isRecovered) "إلغاء الاسترجاع" else "توثيق استرجاع", "#${r.id} ${r.model}") }
                                        toast("تم تأكيد تحديث الحالة على الخادم")
                                    }, onError = { operationPending = false; toast(it) })
                            }
                        },
                        onHide = { r, hide -> launchOp(if (hide) "تم إخفاء البلاغ عن المستخدمين" else "تم إظهار البلاغ") { AdminCloud.setHidden(r.imei1, hide, "#${r.id} ${r.model}") } },
                        onPin = { r, pin -> launchOp(if (pin) "تم تثبيت البلاغ أعلى التطبيق 📌" else "تم إلغاء التثبيت") { AdminCloud.setPinned(if (pin) r.imei1 else "", "#${r.id} ${r.model}") } },
                        onBan = { r -> banTarget = r.primaryPhone to "مخالفة في البلاغ #${r.id} (${shortBrand(r.brand)} ${r.model})" },
                        onDelete = { if (!operationPending) deleteTarget = it })
                }
                "الحظر" -> item { BansTab(bans, { id, reason -> banTarget = id to reason }, { b -> AdminManager.unbanAccount(b.id) { ok, err -> scope.launch { toast(if (ok) "تم فك الحظر عن ${b.identifier} ✅" else err ?: "تعذر فك الحظر") } } }) }
                "التعاميم" -> item { BroadcastTab { title, message, urgent -> viewModel.sendSupervisorBroadcast(title, message, urgent, context); toast("بدأ إرسال التعميم؛ تحقق من قائمة التنبيهات بعد المزامنة") } }
                "الفريق" -> item { TeamTab(config.moderators, { email -> launchOp("تمت إضافة $email كمشرف ✅") { AdminCloud.addModerator(email) } }, { email -> launchOp("تمت إزالة $email من المشرفين") { AdminCloud.removeModerator(email) } }) }
                "التحكم" -> item { ControlTab(owner, config,
                    { text -> launchOp(if (text.isBlank()) "تم حذف الإعلان" else "تم نشر الإعلان لكل المستخدمين") { AdminCloud.setAnnouncement(text) } },
                    { pause -> launchOp(if (pause) "تم إيقاف نشر البلاغات مؤقتاً" else "تم استئناف نشر البلاغات") { AdminCloud.setReportsPaused(pause) } },
                    { enabled, message -> launchOp(if (enabled) "تم تفعيل وضع الصيانة" else "تم إيقاف وضع الصيانة") { AdminCloud.setMaintenance(enabled, message) } }) }
                "السجل" -> if (logs.isEmpty()) item { EmptyNote("لا توجد عمليات مسجلة بعد") } else items(logs, key = { it.id }) { log ->
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
    deleteTarget?.let { r -> AlertDialog(onDismissRequest = { deleteTarget = null }, containerColor = BrandSurface,
        title = { Text("حذف البلاغ #${r.id} نهائياً؟", color = PureWhite, fontWeight = FontWeight.Bold) },
        text = { Text("${shortBrand(r.brand)} ${r.model}\nصاحب البلاغ: ${r.contactName} (${r.primaryPhone})\n\nسيُحذف من الخادم مع صوره المرتبطة. لا يمكن التراجع؛ استخدم الإخفاء إذا كان مشبوهاً فقط.", color = TextSecondaryLight) },
        confirmButton = { TextButton(onClick = {
            deleteTarget = null; operationPending = true
            viewModel.adminDeleteReport(r.id,
                onSuccess = { operationPending = false; scope.launch { AdminCloud.log("حذف بلاغ", "#${r.id} ${r.model}", r.primaryPhone) }; toast("تم تأكيد حذف البلاغ من الخادم") },
                onError = { operationPending = false; toast(it) })
        }) { Text("حذف", color = AlertRed, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("إلغاء", color = PureWhite) } }) }
    banTarget?.let { (identifier, initialReason) ->
        var reason by remember(identifier) { mutableStateOf(initialReason) }
        AlertDialog(onDismissRequest = { banTarget = null }, containerColor = BrandSurface,
            title = { Text("حظر $identifier", color = PureWhite, fontWeight = FontWeight.Bold) },
            text = { Column {
                Text("سيُمنع من نشر أي بلاغ من كل الأجهزة.", color = TextSecondaryLight, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(reason, { reason = it }, label = { Text("سبب الحظر") }, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth())
            } }, confirmButton = { TextButton(onClick = { AdminManager.banAccount(identifier, reason) { ok, err -> scope.launch { toast(if (ok) "تم حظر $identifier 🚫" else err ?: "تعذر الحظر") } }; banTarget = null }) { Text("حظر", color = AlertRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { banTarget = null }) { Text("إلغاء", color = PureWhite) } })
    }
}

@Composable
private fun OverviewTab(reports: List<ReportEntity>, bansCount: Int, config: AppRemoteConfig) {
    val now = System.currentTimeMillis(); val day = 24 * 60 * 60 * 1000L
    val recovered = reports.count { it.isRecovered }; val rate = if (reports.isEmpty()) 0 else recovered * 100 / reports.size
    val last7 = (6 downTo 0).map { d -> reports.count { it.createdAt in (now - (d + 1) * day) until (now - d * day) } }
    val byGov = reports.groupBy { it.governorate.substringBefore(" (") }.mapValues { it.value.size }.entries.sortedByDescending { it.value }.take(6)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (config.maintenanceMode || config.reportsPaused) Surface(shape = RoundedCornerShape(14.dp), color = AlertRed.copy(alpha = 0.15f), border = BorderStroke(1.dp, AlertRed)) {
            Text((if (config.maintenanceMode) "⚠️ وضع الصيانة مفعّل. " else "") + if (config.reportsPaused) "⏸️ نشر البلاغات موقوف." else "", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("البلاغات", "${reports.size}", BrandCyan, Modifier.weight(1f)); KpiCard("الاسترجاع", "$rate%", SuccessGreen, Modifier.weight(1f)); KpiCard("محظورون", "$bansCount", AlertRed, Modifier.weight(1f)) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("مسروقة", "${reports.count { it.isStolen }}", AlertRed, Modifier.weight(1f)); KpiCard("مفقودة", "${reports.count { it.isLost }}", WarningAmber, Modifier.weight(1f)); KpiCard("معثور عليها", "${reports.count { it.isFound }}", SuccessGreen, Modifier.weight(1f)) }
        Panel("البلاغات آخر 7 أيام") {
            val maximum = (last7.maxOrNull() ?: 0).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                last7.forEachIndexed { i, value -> Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$value", color = PureWhite, fontSize = 10.sp)
                    Box(Modifier.fillMaxWidth().height((70f * value / maximum).coerceAtLeast(3f).dp).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(if (i == 6) YemenGold else BrandCyan.copy(alpha = 0.6f)))
                    Text(if (i == 6) "اليوم" else "-${6 - i}", color = TextSecondaryLight, fontSize = 9.sp)
                } }
            }
        }
        Panel("أكثر المحافظات بلاغات") {
            if (byGov.isEmpty()) Text("لا توجد بيانات", color = TextSecondaryLight, fontSize = 12.sp)
            val maximum = (byGov.firstOrNull()?.value ?: 1).coerceAtLeast(1)
            byGov.forEach { (gov, count) -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                Text(gov, color = PureWhite, fontSize = 12.sp, modifier = Modifier.width(80.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(BrandSurfaceHigh)) { Box(Modifier.fillMaxWidth(count.toFloat() / maximum).height(10.dp).background(YemenGold)) }
                Text("  $count", color = YemenGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } }
        }
    }
}
@Composable private fun KpiCard(title: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) { Column(Modifier.padding(12.dp)) { Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black); Text(title, color = TextSecondaryLight, fontSize = 11.sp) } }
}
@Composable private fun Panel(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp); content() } }
}
@Composable private fun EmptyNote(text: String) { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text(text, color = TextSecondaryLight, fontSize = 13.sp) } }

@Composable
private fun ReportsAdminList(reports: List<ReportEntity>, config: AppRemoteConfig, onOpen: (ReportEntity) -> Unit, onToggleRecovered: (ReportEntity) -> Unit, onHide: (ReportEntity, Boolean) -> Unit, onPin: (ReportEntity, Boolean) -> Unit, onBan: (ReportEntity) -> Unit, onDelete: (ReportEntity) -> Unit) {
    var query by remember { mutableStateOf("") }; var filter by remember { mutableStateOf("ALL") }
    val filters = listOf("ALL" to "الكل", "STOLEN" to "مسروقة", "LOST" to "مفقودة", "FOUND" to "معثور", "RECOVERED" to "مسترجعة", "HIDDEN" to "مخفية")
    val shown = reports.filter { r -> val q = query.trim(); val match = q.isBlank() || r.model.contains(q, true) || r.brand.contains(q, true) || r.imei1.contains(q) || r.primaryPhone.contains(q) || r.contactName.contains(q, true) || r.governorate.contains(q) || "#${r.id}" == q
        match && when (filter) { "ALL" -> true; "RECOVERED" -> r.isRecovered; "HIDDEN" -> r.imei1 in config.hiddenImeis; else -> r.reportType == filter }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(query, { query = it }, placeholder = { Text("ابحث: IMEI، رقم، اسم، موديل، #رقم البلاغ", fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = YemenGold) }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("admin_reports_search"))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { filters.forEach { (key, label) -> FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, shape = RoundedCornerShape(50), colors = FilterChipDefaults.filterChipColors(containerColor = BrandSurface, labelColor = PureWhite.copy(alpha = 0.8f), selectedContainerColor = BrandCyan, selectedLabelColor = BrandInk)) } }
        Text("${shown.size} بلاغ", color = TextSecondaryLight, fontSize = 12.sp)
        if (shown.isEmpty()) EmptyNote("لا توجد بلاغات مطابقة")
        shown.take(200).forEach { r ->
            val hidden = r.imei1 in config.hiddenImeis; val pinned = config.pinnedImei.isNotBlank() && r.imei1 == config.pinnedImei
            Surface(shape = RoundedCornerShape(18.dp), color = BrandSurface, border = BorderStroke(1.dp, if (pinned) YemenGold else if (hidden) AlertRed.copy(alpha = 0.5f) else BrandBorder), modifier = Modifier.fillMaxWidth().testTag("admin_report_card_${r.id}")) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("#${r.id} • ${shortBrand(r.brand)}", color = YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(r.model, color = PureWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        StatusBadge(reportType = r.reportType, isRecovered = r.isRecovered)
                    }
                    if (pinned || hidden) Text(listOfNotNull(if (pinned) "📌 مثبّت" else null, if (hidden) "🙈 مخفي عن المستخدمين" else null).joinToString("  •  "), color = if (hidden) AlertRed else YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("📍 ${r.governorate} • ${r.incidentLocation}", color = TextSecondaryLight, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("👤 ${r.contactName} • ${r.primaryPhone}  •  IMEI ${r.imei1}", color = TextSecondaryLight, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    HorizontalDivider(color = BrandBorder)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip(Icons.Default.Search, "التفاصيل", BrandCyan) { onOpen(r) }
                        ActionChip(Icons.Default.CheckCircle, if (r.isRecovered) "إلغاء الاسترجاع" else "تم الاسترجاع", SuccessGreen) { onToggleRecovered(r) }
                        ActionChip(Icons.Default.PushPin, if (pinned) "إلغاء التثبيت" else "تثبيت", YemenGold) { onPin(r, !pinned) }
                        ActionChip(if (hidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, if (hidden) "إظهار" else "إخفاء", WarningAmber) { onHide(r, !hidden) }
                        ActionChip(Icons.Default.Block, "حظر الرقم", PureWhite) { onBan(r) }; ActionChip(Icons.Default.Delete, "حذف", AlertRed) { onDelete(r) }
                    }
                }
            }
        }
    }
}
@Composable private fun ActionChip(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(onClick, shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f), border = BorderStroke(1.dp, color.copy(alpha = 0.4f))) { Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = color, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
}
@Composable private fun BansTab(bans: List<com.example.util.BannedAccount>, onBan: (String, String) -> Unit, onUnban: (com.example.util.BannedAccount) -> Unit) {
    var identifier by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Panel("حظر رقم أو بريد") {
            Text("الحظر سحابي: يطبّق على كل الأجهزة، وقاعدة البيانات ترفض أي بلاغ من المحظور.", color = TextSecondaryLight, fontSize = 12.sp)
            OutlinedTextField(identifier, { identifier = it }, label = { Text("رقم الهاتف أو البريد") }, singleLine = true, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("ban_identifier_input"))
            OutlinedTextField(reason, { reason = it }, label = { Text("السبب") }, singleLine = true, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("ban_reason_input"))
            Button(onClick = { onBan(identifier.trim(), reason.trim()); identifier = ""; reason = "" }, enabled = identifier.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = PureWhite), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(46.dp).testTag("confirm_ban_button")) { Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("حظر", fontWeight = FontWeight.Bold) }
        }
        Text("المحظورون (${bans.size})", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        if (bans.isEmpty()) EmptyNote("لا يوجد محظورون")
        bans.forEach { b -> Surface(shape = RoundedCornerShape(14.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(b.identifier, color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(b.reason, color = TextSecondaryLight, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(b.bannedBy + if (b.bannedAt > 0) " • " + formatRelativeTime(b.bannedAt) else "", color = BrandCyan, fontSize = 10.sp) }
                TextButton(onClick = { onUnban(b) }, modifier = Modifier.testTag("unban_btn_${b.id}")) { Text("فك الحظر", color = SuccessGreen, fontWeight = FontWeight.Bold) }
            }
        } }
    }
}
@Composable private fun BroadcastTab(onSend: (String, String, Boolean) -> Unit) {
    var title by remember { mutableStateOf("تعميم أمني من إدارة أمان فون") }; var message by remember { mutableStateOf("") }; var urgent by remember { mutableStateOf(true) }
    Panel("بث تعميم لكل المستخدمين") {
        OutlinedTextField(title, { title = it.take(150) }, label = { Text("العنوان") }, singleLine = true, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("broadcast_title_input"))
        OutlinedTextField(message, { message = it.take(500) }, label = { Text("نص التعميم") }, minLines = 3, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("broadcast_message_input"))
        Row(verticalAlignment = Alignment.CenterVertically) { Text("عاجل 🚨", color = PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Switch(urgent, { urgent = it }, colors = SwitchDefaults.colors(checkedTrackColor = AlertRed)) }
        Button(onClick = { onSend(title.trim(), message.trim(), urgent); message = "" }, enabled = title.trim().length >= 3 && message.trim().length >= 3, colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = BrandInk), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_broadcast_button")) { Icon(Icons.Default.Campaign, null); Spacer(Modifier.width(6.dp)); Text("بث التعميم الآن", fontWeight = FontWeight.ExtraBold) }
    }
}
@Composable private fun TeamTab(moderators: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var email by remember { mutableStateOf("") }; var remove by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Panel("إضافة مشرف") {
            Text("المشرف يقدر يدير البلاغات ويحظر ويبث تعاميم وينشر إعلانات. إضافة المشرفين والصيانة وإيقاف البلاغات تبقى للمشرف العام وحده. لازم يدخل المشرف ببريد مفعّل.", color = TextSecondaryLight, fontSize = 12.sp, lineHeight = 18.sp)
            OutlinedTextField(email, { email = it }, label = { Text("بريد المشرف") }, singleLine = true, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("moderator_email_input"))
            Button(onClick = { onAdd(email.trim()); email = "" }, enabled = email.contains("@"), colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = BrandInk), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(46.dp).testTag("add_moderator_button")) { Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(6.dp)); Text("إضافة للفريق", fontWeight = FontWeight.Bold) }
        }
        Text("الفريق", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        TeamRow(AdminManager.SUPER_ADMIN_EMAIL, "المشرف العام", YemenGold, null)
        if (moderators.isEmpty()) Text("لا يوجد مشرفون إضافيون", color = TextSecondaryLight, fontSize = 12.sp)
        moderators.forEach { m -> TeamRow(m, "مشرف", BrandCyan) { remove = m } }
    }
    remove?.let { m -> AlertDialog(onDismissRequest = { remove = null }, containerColor = BrandSurface, title = { Text("إزالة $m من المشرفين؟", color = PureWhite, fontWeight = FontWeight.Bold) }, confirmButton = { TextButton(onClick = { onRemove(m); remove = null }) { Text("إزالة", color = AlertRed, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { remove = null }) { Text("إلغاء", color = PureWhite) } }) }
}
@Composable private fun TeamRow(email: String, role: String, color: Color, onRemove: (() -> Unit)?) {
    Surface(shape = RoundedCornerShape(14.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text(email.take(1).uppercase(), color = color, fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(email, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(role, color = color, fontSize = 11.sp) }
        if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Default.PersonRemove, "إزالة", tint = AlertRed) }
    } }
}
@Composable private fun ControlTab(isOwner: Boolean, config: AppRemoteConfig, onAnnouncement: (String) -> Unit, onPause: (Boolean) -> Unit, onMaintenance: (Boolean, String) -> Unit) {
    var announcement by remember(config.announcement) { mutableStateOf(config.announcement) }
    var maintenanceMessage by remember(config.maintenanceMessage) { mutableStateOf(config.maintenanceMessage.ifBlank { "التطبيق تحت الصيانة مؤقتاً، نرجع لكم قريباً." }) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel("📢 إعلان أعلى التطبيق") {
            Text("يظهر لكل المستخدمين فوق شريط التنقل.", color = TextSecondaryLight, fontSize = 12.sp)
            OutlinedTextField(announcement, { announcement = it.take(300) }, label = { Text("نص الإعلان") }, minLines = 2, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth().testTag("announcement_input"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAnnouncement(announcement) }, enabled = announcement.isNotBlank() && announcement != config.announcement, colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = BrandInk), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("نشر", fontWeight = FontWeight.Bold) }
                if (config.announcement.isNotBlank()) Button(onClick = { announcement = ""; onAnnouncement("") }, colors = ButtonDefaults.buttonColors(containerColor = BrandSurfaceHigh, contentColor = PureWhite), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("حذف الإعلان") }
            }
        }
        if (isOwner) {
            Panel("⏸️ إيقاف نشر البلاغات") { Row(verticalAlignment = Alignment.CenterVertically) { Text(if (config.reportsPaused) "النشر موقوف حالياً لكل المستخدمين" else "المستخدمون يقدرون ينشرون بلاغات", color = if (config.reportsPaused) AlertRed else PureWhite, fontSize = 13.sp, modifier = Modifier.weight(1f)); Switch(config.reportsPaused, onPause, colors = SwitchDefaults.colors(checkedTrackColor = AlertRed)) } }
            Panel("🛠️ وضع الصيانة") {
                Text("يقفل التطبيق على كل المستخدمين ما عدا المشرفين.", color = TextSecondaryLight, fontSize = 12.sp)
                OutlinedTextField(maintenanceMessage, { maintenanceMessage = it.take(200) }, label = { Text("رسالة الصيانة") }, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) { Text(if (config.maintenanceMode) "مفعّل" else "غير مفعّل", color = if (config.maintenanceMode) AlertRed else PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Switch(config.maintenanceMode, { onMaintenance(it, maintenanceMessage) }, colors = SwitchDefaults.colors(checkedTrackColor = AlertRed)) }
            }
        } else Text("الصيانة وإيقاف البلاغات متاحة للمشرف العام فقط.", color = TextSecondaryLight, fontSize = 12.sp)
    }
}
