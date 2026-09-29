package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.YemenFlagPill
import com.example.ui.theme.AlertRed
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "دليل وتوعية محلات ومستخدمي الهواتف",
                            color = PureWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        YemenFlagPill()
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("guide_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy800)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Navy700)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ميثاق أمان سوق الهواتف في الجمهورية اليمنية",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = "يهدف تطبيق أمان فون لقطع الطريق على عصابات سرقة الهواتف من خلال ربط محلات الصيانة والبيع بنظام تنبيهات فوري موحد وقاعدة بيانات وطنية لـ IMEI.",
                        color = PureWhite.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // 2. Guidelines for Phone Shops & Technicians
            Text(
                text = "إرشادات أصحاب محلات ومهندسي الجوالات",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            GuideTipItem(
                icon = Icons.Default.QrCodeScanner,
                iconTint = WarningAmber,
                title = "1. فحص IMEI قبل شراء أي جهاز مستخدم",
                desc = "اطلب كود *#06# أمام البائع، وافحص الرقم فوراً في خانة 'فحص IMEI' بالتطبيق للتأكد من عدم وجود بلاغ سرقة رسمي مسجل."
            )

            GuideTipItem(
                icon = Icons.Default.Store,
                iconTint = Navy700,
                title = "2. طلب الكرتون والفاتورة والبطاقة الشخصية",
                desc = "تجنب شراء الأجهزة بدون كرتون أو بدون إثبات هوية صريح. قم بتدوين الاسم الكامل ورقم البطاقة الشخصية للبائع في سند الاستلام."
            )

            GuideTipItem(
                icon = Icons.Default.Build,
                iconTint = AlertRed,
                title = "3. الحذر من طلبات تخطي الحسابات والفرمتة المشبوهة",
                desc = "إذا جاء شخص يطلب فورمات أو فك قفل شاشة/iCloud/FRP لجهاز لا يملك كرتونه أو إثباته، تحقق من IMEI واحتفظ برقم هاتفه."
            )

            // 3. Citizen Protection Advice
            Text(
                text = "نصائح وإجراءات وقائية للمواطنين",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            GuideTipItem(
                icon = Icons.Default.CheckCircle,
                iconTint = SuccessGreen,
                title = "1. دوّن أرقام IMEI فور شراء هاتفك",
                desc = "اكتب رقمي IMEI 1 و IMEI 2 ورقم السيريال في دفتر خاص أو في حسابك السحابي، واحتفظ بالكرتون وفاتورة المحل."
            )

            GuideTipItem(
                icon = Icons.Default.Lock,
                iconTint = Navy700,
                title = "2. تفعيل قفل الشاشة وحسابات الأمان",
                desc = "استخدم بصمة الإصبع أو رمز PIN قوي، وتأكد من تفعيل خدمة (العثور على جهازي Find My Device) المرتبطة بحساب Google أو Apple ID."
            )

            GuideTipItem(
                icon = Icons.Default.Warning,
                iconTint = AlertRed,
                title = "3. عند السرقة: سارع بتقديم البلاغ في التطبيق والشرطة",
                desc = "انشر البلاغ فوراً في تطبيق أمان فون ليصل إشعار عاجل لجميع المحلات في محافظتك وبقية المحافظات في غضون ثوانٍ."
            )

            // 4. Emergency Numbers in Yemen
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = AlertRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "أرقام الطوارئ والبلاغات الأمنية في اليمن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    EmergencyRow(title = "عمليات النجدة والشرطة:", number = "199 أو 194")
                    EmergencyRow(title = "البحث الجنائي ومكافحة السرقات:", number = "193 أو زيارة أقرب قسم شرطة")
                    EmergencyRow(title = "إيقاف الشريحة (يمن موبايل):", number = "188 أو 777777777")
                    EmergencyRow(title = "إيقاف الشريحة (يو YOU):", number = "111")
                    EmergencyRow(title = "إيقاف الشريحة (سبأفون):", number = "211")
                }
            }

            // 5. Developer & Designer Rights Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Navy800),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, WarningAmber)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "حقوق البرمجة والتصميم",
                        color = WarningAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "المهندس: هاشم القديمي",
                        color = PureWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "مطور ومصمم تطبيق أمان فون • الجمهورية اليمنية",
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { com.example.util.IntentHelper.openFacebookProfile(context, "HashemAlQodimy") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("فيسبوك", fontSize = 11.sp, color = PureWhite)
                        }
                        Button(
                            onClick = { com.example.util.IntentHelper.contactDeveloperWhatsApp(context, "777450123") },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("واتساب", fontSize = 11.sp, color = PureWhite)
                        }
                        Button(
                            onClick = { com.example.util.IntentHelper.makeCall(context, "777450123") },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy700),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("اتصال", fontSize = 11.sp, color = PureWhite)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
fun EmergencyRow(title: String, number: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Surface(
            color = Navy700.copy(alpha = 0.08f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = number,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Navy700,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
