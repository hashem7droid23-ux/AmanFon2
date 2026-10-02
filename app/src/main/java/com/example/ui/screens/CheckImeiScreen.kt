package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.formatAmount
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberTint
import com.example.ui.theme.BrandBg
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandInk
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandSurfaceHigh
import com.example.ui.theme.GreenTint
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RedTint
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.YemenGold
import com.example.ui.viewmodel.ImeiCheckState
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.ImeiValidator
import com.example.util.IntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckImeiScreen(
    viewModel: PhoneTrackerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val imeiInput by viewModel.imeiSearchInput.collectAsStateWithLifecycle()
    val checkResult by viewModel.imeiCheckResult.collectAsStateWithLifecycle()

    val cleanInput = ImeiValidator.clean(imeiInput)
    val isComplete = cleanInput.length == 15
    val isLuhn = if (isComplete) ImeiValidator.isValidLuhn(cleanInput) else false
    val canCheck = cleanInput.length >= 14

    fun runCheck() {
        focusManager.clearFocus()
        viewModel.checkImei()
    }

    Scaffold(
        modifier = modifier,
        containerColor = BrandBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("فحص IMEI", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("تحقق من الجهاز قبل الشراء", color = TextSecondaryLight, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("imei_back_button")) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== Input card =====
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BrandSurface,
                border = BorderStroke(1.dp, BrandBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = imeiInput,
                        onValueChange = { new -> viewModel.imeiSearchInput.value = new.filter { it.isDigit() }.take(15) },
                        label = { Text("رقم IMEI (15 رقماً)") },
                        placeholder = { Text("مثال: 354892110485921", color = TextSecondaryLight.copy(alpha = 0.5f)) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = PureWhite
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { if (canCheck) runCheck() }),
                        trailingIcon = {
                            if (imeiInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearImeiCheck() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextSecondaryLight)
                                }
                            } else {
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        viewModel.imeiSearchInput.value = clip.filter { it.isDigit() }.take(15)
                                    }
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = YemenGold)
                                }
                            }
                        },
                        isError = isComplete && !isLuhn,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YemenGold,
                            unfocusedBorderColor = BrandBorder,
                            focusedLabelColor = YemenGold,
                            unfocusedLabelColor = TextSecondaryLight,
                            cursorColor = YemenGold,
                            focusedContainerColor = BrandBg,
                            unfocusedContainerColor = BrandBg
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("imei_input_field")
                    )

                    // Progress + validation line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DigitProgress(count = cleanInput.length, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        val (txt, col) = when {
                            cleanInput.isEmpty() -> "0/15" to TextSecondaryLight
                            !isComplete -> "${cleanInput.length}/15" to TextSecondaryLight
                            isLuhn -> "✓ رقم صحيح" to SuccessGreen
                            else -> "رقم غير قياسي" to WarningAmber
                        }
                        Text(txt, color = col, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { runCheck() },
                        enabled = canCheck && checkResult !is ImeiCheckState.Checking,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YemenGold,
                            contentColor = BrandInk,
                            disabledContainerColor = BrandSurfaceHigh,
                            disabledContentColor = TextSecondaryLight
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_check_imei_button")
                    ) {
                        if (checkResult is ImeiCheckState.Checking) {
                            CircularProgressIndicator(color = BrandInk, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("فحص في القائمة السوداء الوطنية", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // ===== Result / idle =====
            AnimatedContent(
                targetState = checkResult,
                transitionSpec = { (fadeIn() + slideInVertically { it / 6 }) togetherWith fadeOut() },
                label = "imeiResult"
            ) { state ->
                when (state) {
                    is ImeiCheckState.Idle, is ImeiCheckState.Checking -> IdleGuide()
                    is ImeiCheckState.InvalidFormat -> ResultBanner(
                        tint = AmberTint, accent = WarningAmber, icon = Icons.Default.Warning,
                        title = "تحقق من الرقم", subtitle = state.reason
                    )
                    is ImeiCheckState.Safe -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ResultBanner(
                            tint = GreenTint, accent = SuccessGreen, icon = Icons.Default.VerifiedUser,
                            title = "الجهاز سليم", subtitle = "لا توجد بلاغات سرقة أو فقدان لهذا الرقم",
                            imei = state.cleanImei,
                            testTag = "safe_imei_result_card"
                        )
                        TipCard("طابق رقم IMEI في النظام مع الرقم المطبوع على درج الشريحة وعلبة الكرتون الأصلية قبل الدفع.")
                    }
                    is ImeiCheckState.StolenAlert -> {
                        val report = state.report
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ResultBanner(
                                tint = RedTint, accent = AlertRed, icon = Icons.Default.GppBad,
                                title = if (report.isStolen) "لا تشترِ هذا الجهاز: مسروق" else "لا تشترِ هذا الجهاز: مفقود",
                                subtitle = "بلاغ رسمي رقم #${report.id}",
                                imei = ImeiValidator.clean(report.imei1),
                                testTag = "stolen_imei_result_card"
                            )
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = BrandSurface,
                                border = BorderStroke(1.dp, BrandBorder)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    InfoLine("الجهاز", "${report.brand} ${report.model}")
                                    InfoLine("مكان الحادثة", "${report.governorate} • ${report.incidentLocation}")
                                    if (report.color.isNotBlank()) InfoLine("اللون", report.color)
                                    if (report.distinctiveMarks.isNotBlank()) InfoLine("علامات فارقة", report.distinctiveMarks)
                                    InfoLine("صاحب البلاغ", report.contactName)
                                    if (report.policeReportNumber.isNotBlank()) InfoLine("بلاغ الشرطة", report.policeReportNumber, AlertRed)
                                    if (report.rewardAmount > 0) {
                                        InfoLine("المكافأة", "${formatAmount(report.rewardAmount.toLong())} ريال يمني", YemenGold)
                                    }
                                    HorizontalDivider(color = BrandBorder)
                                    Text(
                                        "امتنع عن شراء الجهاز أو فرمتته، وتواصل مع صاحبه فوراً.",
                                        color = AlertRed, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { IntentHelper.makeCall(context, report.primaryPhone) },
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandSurfaceHigh, contentColor = PureWhite),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).height(46.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("اتصال", fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = {
                                                val msg = "السلام عليكم، تم فحص هاتفكم المسجل (${report.brand} ${report.model}) برقم IMEI: ${report.imei1} عبر تطبيق أمان فون."
                                                IntentHelper.openWhatsApp(context, report.whatsappNumber, msg)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = PureWhite),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).height(46.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("واتساب", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Demo samples: developer builds only
            if (com.example.BuildConfig.DEBUG) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.testWithSampleImei("354892110485921") },
                        border = BorderStroke(1.dp, BrandBorder),
                        modifier = Modifier.weight(1f).testTag("sample_stolen_button")
                    ) { Text("تجربة: مسروق", fontSize = 11.sp, color = AlertRed) }
                    OutlinedButton(
                        onClick = { viewModel.testWithSampleImei("359998877665544") },
                        border = BorderStroke(1.dp, BrandBorder),
                        modifier = Modifier.weight(1f).testTag("sample_clean_button")
                    ) { Text("تجربة: سليم", fontSize = 11.sp, color = SuccessGreen) }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DigitProgress(count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(15) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < count) YemenGold else BrandBorder)
            )
        }
    }
}

@Composable
private fun IdleGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("كيف تفحص الجهاز؟", color = PureWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        StepRow(1, Icons.Default.Dialpad, "اطلب *#06# من لوحة الاتصال", "يظهر رقم IMEI فوراً على شاشة الجهاز")
        StepRow(2, Icons.Default.ContentPaste, "أدخل أو الصق الرقم", "15 رقماً، ونتحقق من صحته تلقائياً")
        StepRow(3, Icons.Default.CheckCircle, "اقرأ النتيجة", "أخضر = سليم  •  أحمر = مسروق أو مفقود")
    }
}

@Composable
private fun StepRow(n: Int, icon: ImageVector, title: String, desc: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(YemenGold.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = YemenGold, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = PureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = TextSecondaryLight, fontSize = 12.sp)
            }
            Text("$n", color = YemenGold.copy(alpha = 0.6f), fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ResultBanner(
    tint: Color,
    accent: Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    imei: String? = null,
    testTag: String = "imei_result_banner"
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = tint,
        border = BorderStroke(1.5.dp, accent),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = PureWhite, modifier = Modifier.size(36.dp)) }
            Text(title, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(subtitle, color = PureWhite.copy(alpha = 0.85f), fontSize = 13.sp, textAlign = TextAlign.Center)
            if (imei != null) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color.Black.copy(alpha = 0.25f)) {
                    Text(
                        imei,
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = PureWhite) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = TextSecondaryLight, fontSize = 13.sp, modifier = Modifier.width(96.dp))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TipCard(text: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = YemenGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = TextSecondaryLight, fontSize = 12.sp)
        }
    }
}
