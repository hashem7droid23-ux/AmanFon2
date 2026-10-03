package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandBg
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandSurfaceHigh
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.YemenGold
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.IntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    viewModel: PhoneTrackerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val report by viewModel.selectedReport.collectAsStateWithLifecycle()
    val isSuperAdmin by com.example.util.AdminManager.isSuperAdmin.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmBan by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = BrandBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = report?.let { "بلاغ #${it.id}" } ?: "تفاصيل البلاغ",
                        color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PureWhite)
                    }
                },
                actions = {
                    report?.let { r ->
                        IconButton(onClick = { IntentHelper.shareReport(context, r) }, modifier = Modifier.testTag("detail_share_button")) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = PureWhite)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg)
            )
        },
        bottomBar = {
            report?.let { item ->
                Surface(color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { IntentHelper.makeCall(context, item.primaryPhone) },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandSurfaceHigh, contentColor = PureWhite),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp).testTag("detail_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("اتصال", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                val msg = "السلام عليكم، أتواصل معك بخصوص بلاغ الهاتف (${item.brand} ${item.model}) عبر تطبيق أمان فون."
                                IntentHelper.openWhatsApp(context, item.whatsappNumber, msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = PureWhite),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp).testTag("detail_whatsapp_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("واتساب", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val item = report
        if (item == null) {
            Column(
                Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextSecondaryLight, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(10.dp))
                Text("لم يتم العثور على البلاغ", color = PureWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("ربما حُذف أو لم تتم مزامنته بعد", color = TextSecondaryLight, fontSize = 12.sp)
            }
            return@Scaffold
        }

        val accent = when {
            item.isRecovered -> SuccessGreen
            item.reportType == "STOLEN" -> AlertRed
            item.reportType == "LOST" -> WarningAmber
            else -> Color(0xFF7CC4FF)
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== Hero =====
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.28f), BrandSurface)))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(shortBrand(item.brand), color = YemenGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(item.model, color = PureWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(reportType = item.reportType, isRecovered = item.isRecovered)
                        Spacer(Modifier.width(8.dp))
                        Text(formatRelativeTime(item.createdAt), color = TextSecondaryLight, fontSize = 12.sp)
                    }
                    if (item.rewardAmount > 0 && !item.isRecovered) {
                        Surface(shape = RoundedCornerShape(50), color = YemenGold.copy(alpha = 0.16f), border = BorderStroke(1.dp, YemenGold.copy(alpha = 0.5f))) {
                            Text(
                                "💰 مكافأة ${formatAmount(item.rewardAmount.toLong())} ريال لمن يعثر عليه",
                                color = YemenGold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ===== Photos (if any) =====
            ReportPhotosGallery(imei = item.imei1)

            // ===== IMEI =====
            Section(Icons.Default.Shield, "الأرقام التسلسلية") {
                ImeiRow("IMEI 1", item.imei1, highlight = item.isStolen) {
                    clipboardManager.setText(AnnotatedString(item.imei1))
                    Toast.makeText(context, "تم نسخ رقم IMEI", Toast.LENGTH_SHORT).show()
                }
                if (item.imei2.isNotBlank()) {
                    ImeiRow("IMEI 2", item.imei2, highlight = false) {
                        clipboardManager.setText(AnnotatedString(item.imei2))
                        Toast.makeText(context, "تم نسخ رقم IMEI", Toast.LENGTH_SHORT).show()
                    }
                }
                if (item.serialNumber.isNotBlank()) {
                    ImeiRow("الرقم التسلسلي S/N", item.serialNumber, highlight = false) {
                        clipboardManager.setText(AnnotatedString(item.serialNumber))
                        Toast.makeText(context, "تم نسخ الرقم التسلسلي", Toast.LENGTH_SHORT).show()
                    }
                }
                if (item.color.isNotBlank()) KV("اللون", item.color)
                if (item.distinctiveMarks.isNotBlank()) KV("علامات فارقة", item.distinctiveMarks)
            }

            // ===== Location =====
            Section(Icons.Default.LocationOn, "مكان الحادثة") {
                KV("المحافظة", item.governorate)
                if (item.district.isNotBlank()) KV("المديرية", item.district)
                KV("المكان", item.incidentLocation)
                if (item.policeReportNumber.isNotBlank()) KV("بلاغ الشرطة", item.policeReportNumber, AlertRed)
            }

            // ===== Contact =====
            Section(Icons.Default.Person, "صاحب البلاغ") {
                KV("الاسم", item.contactName)
                KV("الهاتف", item.primaryPhone, mono = true)
            }

            // ===== Admin =====
            if (isSuperAdmin) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BrandSurface,
                    border = BorderStroke(1.dp, YemenGold.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("admin_detail_actions_card")
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("أدوات المشرف العام", color = YemenGold, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Button(
                            onClick = {
                                viewModel.toggleRecovered(item.id, !item.isRecovered)
                                Toast.makeText(context, if (!item.isRecovered) "تم توثيق استرجاع الجهاز ✅" else "تم إلغاء حالة الاسترجاع", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (item.isRecovered) BrandSurfaceHigh else SuccessGreen,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("admin_toggle_recovery_btn")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (item.isRecovered) "إلغاء وضع الاسترجاع" else "توثيق استرجاع الجهاز", fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { confirmDelete = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.18f), contentColor = AlertRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("admin_delete_report_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("حذف البلاغ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { confirmBan = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandSurfaceHigh, contentColor = PureWhite),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("admin_ban_owner_button")
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("حظر الرقم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                containerColor = BrandSurface,
                title = { Text("حذف البلاغ نهائياً؟", color = PureWhite, fontWeight = FontWeight.Bold) },
                text = { Text("لا يمكن التراجع عن هذا الإجراء.", color = TextSecondaryLight) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        viewModel.adminDeleteReport(item.id)
                        Toast.makeText(context, "تم حذف البلاغ", Toast.LENGTH_SHORT).show()
                    }) { Text("حذف", color = AlertRed, fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("إلغاء", color = PureWhite) } }
            )
        }
        if (confirmBan) {
            AlertDialog(
                onDismissRequest = { confirmBan = false },
                containerColor = BrandSurface,
                title = { Text("حظر الرقم ${item.primaryPhone}؟", color = PureWhite, fontWeight = FontWeight.Bold) },
                text = { Text("لن يتمكن صاحب هذا الرقم من نشر بلاغات جديدة.", color = TextSecondaryLight) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmBan = false
                        com.example.util.AdminManager.banAccount(item.primaryPhone, "حظر بواسطة المشرف في البلاغ #${item.id}")
                        Toast.makeText(context, "تم حظر الرقم 🚫", Toast.LENGTH_LONG).show()
                    }) { Text("حظر", color = AlertRed, fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { confirmBan = false }) { Text("إلغاء", color = PureWhite) } }
            )
        }
    }
}

@Composable
private fun Section(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            HorizontalDivider(color = BrandBorder)
            content()
        }
    }
}

@Composable
private fun KV(label: String, value: String, valueColor: Color = PureWhite, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = TextSecondaryLight, fontSize = 13.sp, modifier = Modifier.width(100.dp))
        Text(
            value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ImeiRow(label: String, imei: String, highlight: Boolean, onCopy: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandBg)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = TextSecondaryLight, fontSize = 11.sp)
            Text(
                imei, color = if (highlight) Color(0xFFFF8A8E) else PureWhite,
                fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
            )
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = YemenGold, modifier = Modifier.size(20.dp))
        }
    }
}
