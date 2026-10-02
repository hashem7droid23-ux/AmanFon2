package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.YemenFlagPill
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandBg
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandInk
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.YemenGold
import com.example.util.IntentHelper

private const val DEV_PHONE = "714525890"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YemenGuideScreen(
    onBack: () -> Unit,
    onNavigateToCheckImei: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        containerColor = BrandBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("دليل الأمان", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("للمحلات والمهندسين والمواطنين", color = TextSecondaryLight, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        YemenFlagPill()
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("guide_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== Hero charter =====
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF13304F), Color(0xFF0B1E35))))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(YemenGold.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Security, contentDescription = null, tint = YemenGold) }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "ميثاق أمان سوق الهواتف في اليمن",
                            color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp
                        )
                    }
                    Text(
                        "يقطع أمان فون الطريق على عصابات سرقة الهواتف بربط محلات البيع والصيانة بنظام تنبيهات فوري وقاعدة بيانات وطنية لأرقام IMEI.",
                        color = PureWhite.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 20.sp
                    )
                    Button(
                        onClick = onNavigateToCheckImei,
                        colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = BrandInk),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("افحص جهازاً الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ===== Emergency first: what people need fast =====
            SectionTitle("أرقام الطوارئ", "اضغط على الرقم للاتصال مباشرة")
            Surface(shape = RoundedCornerShape(20.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    EmergencyRow(Icons.Default.Security, AlertRed, "النجدة والشرطة", listOf("199", "194"))
                    RowDivider()
                    EmergencyRow(Icons.Default.Phone, WarningAmber, "البحث الجنائي", listOf("193"), note = "أو أقرب قسم شرطة")
                    RowDivider()
                    EmergencyRow(Icons.Default.SimCard, BrandCyan, "إيقاف شريحة يمن موبايل", listOf("188", "777777777"))
                    RowDivider()
                    EmergencyRow(Icons.Default.SimCard, BrandCyan, "إيقاف شريحة YOU", listOf("111"))
                    RowDivider()
                    EmergencyRow(Icons.Default.SimCard, BrandCyan, "إيقاف شريحة سبأفون", listOf("211"))
                }
            }

            SectionTitle("لأصحاب المحلات والمهندسين", null)
            GuideTipItem(Icons.Default.QrCodeScanner, YemenGold, "افحص IMEI قبل شراء أي جهاز مستخدم",
                "اطلب كود *#06# أمام البائع وافحص الرقم فوراً في التطبيق للتأكد من عدم وجود بلاغ سرقة.")
            GuideTipItem(Icons.Default.Store, BrandCyan, "اطلب الكرتون والفاتورة والبطاقة",
                "تجنب شراء الأجهزة بدون كرتون أو إثبات هوية، ودوّن اسم البائع ورقم بطاقته في سند الاستلام.")
            GuideTipItem(Icons.Default.Build, AlertRed, "احذر طلبات الفرمتة وتخطي الحسابات",
                "إذا طُلب منك فك قفل iCloud أو FRP لجهاز بلا إثبات ملكية، افحص IMEI واحتفظ برقم الشخص.")

            SectionTitle("للمواطنين", null)
            GuideTipItem(Icons.Default.CheckCircle, SuccessGreen, "دوّن أرقام IMEI فور الشراء",
                "احفظ IMEI 1 و IMEI 2 والسيريال في مكان آمن، واحتفظ بالكرتون والفاتورة.")
            GuideTipItem(Icons.Default.Lock, BrandCyan, "فعّل قفل الشاشة و Find My Device",
                "استخدم بصمة أو رمز PIN قوي، وتأكد من ربط الجهاز بحساب Google أو Apple ID.")
            GuideTipItem(Icons.Default.Warning, AlertRed, "عند السرقة: بلّغ فوراً",
                "انشر البلاغ في أمان فون ليصل تنبيه عاجل لكل المحلات خلال ثوانٍ، ثم بلّغ الشرطة.")

            // ===== Developer rights =====
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BrandSurface,
                border = BorderStroke(1.dp, YemenGold.copy(alpha = 0.5f))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("حقوق البرمجة والتصميم", color = YemenGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("المهندس: هاشم القديمي", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text("مطور ومصمم تطبيق أمان فون • الجمهورية اليمنية", color = TextSecondaryLight, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DevAction(Icons.Default.ThumbUp, "فيسبوك", Color(0xFF1877F2), Modifier.weight(1f)) {
                            IntentHelper.openFacebookProfile(context, "HashemAlQodimy")
                        }
                        DevAction(Icons.AutoMirrored.Outlined.Chat, "واتساب", SuccessGreen, Modifier.weight(1f)) {
                            IntentHelper.contactDeveloperWhatsApp(context, DEV_PHONE)
                        }
                        DevAction(Icons.Default.Call, "اتصال", BrandCyan, Modifier.weight(1f)) {
                            IntentHelper.makeCall(context, DEV_PHONE)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String?) {
    Column(Modifier.padding(top = 6.dp)) {
        Text(title, color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        if (subtitle != null) Text(subtitle, color = TextSecondaryLight, fontSize = 12.sp)
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(color = BrandBorder, modifier = Modifier.padding(horizontal = 14.dp))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmergencyRow(icon: ImageVector, tint: Color, title: String, numbers: List<String>, note: String? = null) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (note != null) Text(note, color = TextSecondaryLight, fontSize = 11.sp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            numbers.forEach { n ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SuccessGreen.copy(alpha = 0.16f))
                        .clickable { IntentHelper.makeCall(context, n) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("emergency_call_$n")
                ) {
                    Icon(Icons.Default.Call, contentDescription = "اتصال $n", tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(n, color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun DevAction(icon: ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GuideTipItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BrandSurface,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PureWhite)
                Spacer(Modifier.height(4.dp))
                Text(desc, fontSize = 12.sp, color = TextSecondaryLight, lineHeight = 19.sp)
            }
        }
    }
}
