package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ReportPhotosGallery
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatAmount
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.shortBrand
import com.example.ui.theme.*
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.IntentHelper
import com.example.util.ReportPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(viewModel: PhoneTrackerViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler { onBack() }
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val report by viewModel.selectedReport.collectAsStateWithLifecycle()
    val isAdmin by com.example.util.AdminManager.isAdmin.collectAsStateWithLifecycle()
    val isSuperAdmin by com.example.util.AdminManager.isSuperAdmin.collectAsStateWithLifecycle()
    val user by com.example.data.remote.FirebaseAuthManager.currentUser.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmBan by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    fun feedback(message: String) { Toast.makeText(ctx, message, Toast.LENGTH_LONG).show() }
    Scaffold(modifier = modifier, containerColor = BrandBg, topBar = {
        TopAppBar(title = { Text(report?.let { "بلاغ #${it.id}" } ?: "تفاصيل البلاغ", color = PureWhite) },
            navigationIcon = { IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = PureWhite) } },
            actions = { report?.let { r -> IconButton(onClick = { IntentHelper.shareReport(ctx, r) }, modifier = Modifier.testTag("detail_share_button")) { Icon(Icons.Default.Share, "مشاركة", tint = PureWhite) } } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg))
    }, bottomBar = {
        report?.let { r -> Surface(color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { IntentHelper.makeCall(ctx, r.primaryPhone) }, colors = ButtonDefaults.buttonColors(containerColor = BrandSurfaceHigh), modifier = Modifier.weight(1f).height(52.dp).testTag("detail_call_button")) { Icon(Icons.Default.Call, null); Spacer(Modifier.width(6.dp)); Text("اتصال") }
                Button(onClick = { IntentHelper.openWhatsApp(ctx, r.whatsappNumber, "السلام عليكم، أتواصل بخصوص بلاغ الهاتف (${r.brand} ${r.model}) عبر أمان فون.") }, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), modifier = Modifier.weight(1f).height(52.dp).testTag("detail_whatsapp_button")) { Text("واتساب") }
            }
        } }
    }) { padding ->
        val item = report
        if (item == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                Text("لم يتم العثور على البلاغ", color = PureWhite, fontWeight = FontWeight.Bold)
                Text("ربما حُذف أو لم تتم مزامنته بعد", color = TextSecondaryLight)
            }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DetailSection("${shortBrand(item.brand)} ${item.model}") {
                StatusBadge(reportType = item.reportType, isRecovered = item.isRecovered)
                Text(formatRelativeTime(item.createdAt), color = TextSecondaryLight, fontSize = 12.sp)
                if (item.rewardAmount > 0 && !item.isRecovered) Text("💰 مكافأة ${formatAmount(item.rewardAmount)} ريال", color = YemenGold, fontWeight = FontWeight.Bold)
            }
            ReportPhotosGallery(imei = item.imei1)
            DetailSection("الأرقام التسلسلية") {
                fun copy(value: String) { clipboard.setText(AnnotatedString(value)); feedback("تم نسخ الرقم") }
                DetailNumber("IMEI 1", item.imei1) { copy(item.imei1) }
                if (item.imei2.isNotBlank()) DetailNumber("IMEI 2", item.imei2) { copy(item.imei2) }
                if (item.serialNumber.isNotBlank()) DetailNumber("S/N", item.serialNumber) { copy(item.serialNumber) }
                DetailValue("اللون", item.color)
                DetailValue("علامات فارقة", item.distinctiveMarks)
            }
            DetailSection("مكان الحادثة") {
                DetailValue("المحافظة", item.governorate); DetailValue("المديرية", item.district)
                DetailValue("المكان", item.incidentLocation); DetailValue("بلاغ الشرطة", item.policeReportNumber)
                DetailValue("ملاحظات", item.additionalNotes)
            }
            DetailSection("صاحب البلاغ") { DetailValue("الاسم", item.contactName); DetailValue("الهاتف", item.primaryPhone) }
            if (ReportPolicy.canManage(item.userId, user?.uid, isAdmin)) {
                Surface(color = BrandSurface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, YemenGold), modifier = Modifier.fillMaxWidth().testTag("admin_detail_actions_card")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (isAdmin) "أدوات الإشراف" else "إدارة بلاغك", color = YemenGold, fontWeight = FontWeight.Bold)
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Button(onClick = {
                            busy = true
                            viewModel.toggleRecovered(item.id, !item.isRecovered,
                                onSuccess = { busy = false; feedback("تم تأكيد تحديث حالة الجهاز على الخادم") },
                                onError = { busy = false; feedback(it) })
                        }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = if (item.isRecovered) BrandSurfaceHigh else SuccessGreen), modifier = Modifier.fillMaxWidth().testTag("admin_toggle_recovery_btn")) {
                            Text(if (item.isRecovered) "إلغاء وضع الاسترجاع" else "توثيق استرجاع الجهاز")
                        }
                        Button(onClick = { confirmDelete = true }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.18f), contentColor = AlertRed), modifier = Modifier.fillMaxWidth().testTag("admin_delete_report_button")) { Text("حذف البلاغ") }
                        if (isSuperAdmin) Button(onClick = { confirmBan = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("admin_ban_owner_button")) { Text("حظر الرقم") }
                    }
                }
            }
        }
        if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, containerColor = BrandSurface,
            title = { Text("حذف البلاغ نهائياً؟", color = PureWhite) },
            text = { Text("سيُحذف من الخادم مع الصور المرتبطة به. لا يمكن التراجع.", color = TextSecondaryLight) },
            confirmButton = { TextButton(onClick = {
                confirmDelete = false; busy = true
                viewModel.deleteReport(item.id,
                    onSuccess = { busy = false; feedback("تم تأكيد حذف البلاغ من الخادم") },
                    onError = { busy = false; feedback(it) })
            }) { Text("حذف", color = AlertRed) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("إلغاء", color = PureWhite) } })
        if (confirmBan) AlertDialog(onDismissRequest = { confirmBan = false }, containerColor = BrandSurface,
            title = { Text("حظر الرقم ${item.primaryPhone}؟", color = PureWhite) },
            text = { Text("لن يتمكن صاحب الرقم من نشر بلاغات جديدة بعد تأكيد الحظر على الخادم.", color = TextSecondaryLight) },
            confirmButton = { TextButton(onClick = {
                confirmBan = false
                com.example.util.AdminManager.banAccount(item.primaryPhone, "حظر بواسطة المشرف في البلاغ #${item.id}")
                feedback("تم إرسال طلب الحظر، راجع قائمة المحظورين للتأكد")
            }) { Text("حظر", color = AlertRed) } },
            dismissButton = { TextButton(onClick = { confirmBan = false }) { Text("إلغاء", color = PureWhite) } })
    }
}
@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = BrandSurface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, BrandBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            HorizontalDivider(color = BrandBorder)
            content()
        }
    }
}
@Composable
private fun DetailValue(label: String, value: String) {
    if (value.isNotBlank()) Row(Modifier.fillMaxWidth()) {
        Text(label, color = TextSecondaryLight, fontSize = 13.sp, modifier = Modifier.width(100.dp))
        Text(value, color = PureWhite, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}
@Composable
private fun DetailNumber(label: String, value: String, onCopy: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) { Text(label, color = TextSecondaryLight, fontSize = 11.sp); Text(value, color = PureWhite, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
        IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, "نسخ", tint = YemenGold) }
    }
}
