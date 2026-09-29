package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedLight
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
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
    val imeiInput by viewModel.imeiSearchInput.collectAsStateWithLifecycle()
    val checkResult by viewModel.imeiCheckResult.collectAsStateWithLifecycle()

    val cleanInput = ImeiValidator.clean(imeiInput)
    val isLuhn = if (cleanInput.length == 15) ImeiValidator.isValidLuhn(cleanInput) else false

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "فحص رقم IMEI قبل الشراء",
                        color = PureWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("imei_back_button")
                    ) {
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
            // 1. Guidance Card on How to Get IMEI
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Navy700.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy700.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Navy700.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Navy700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "كيف تستخرج رقم IMEI من الهاتف؟",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "افتح لوحة الاتصال واضغط على *#06# وسيظهر لك فوراً الرقم التسلسلي المكون من 15 رقماً.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Input Box Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "أدخل رقم IMEI (15 رقماً)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = imeiInput,
                        onValueChange = { viewModel.imeiSearchInput.value = it },
                        placeholder = { Text("مثال: 354892110485921") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.checkImei() }),
                        leadingIcon = {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Navy700)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (imeiInput.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearImeiCheck() }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            viewModel.imeiSearchInput.value = clip
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = Navy700)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("imei_input_field")
                    )

                    // Digit Counter & Luhn Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الأرقام المدخلة: ${cleanInput.length} من 15",
                            fontSize = 11.sp,
                            color = if (cleanInput.length in 14..15) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (cleanInput.length == 15) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isLuhn) Icons.Default.Check else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isLuhn) SuccessGreen else WarningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLuhn) "خوارزمية Luhn متطابقة" else "الرقم غير قياسي",
                                    fontSize = 10.sp,
                                    color = if (isLuhn) SuccessGreen else WarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Check Button
                    Button(
                        onClick = { viewModel.checkImei() },
                        enabled = cleanInput.length >= 8,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy700),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_check_imei_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = PureWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فحص في القائمة السوداء الوطنية",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }

            // 3. Quick Sample Test Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "أو جرب الفحص بنماذج جاهزة:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.testWithSampleImei("354892110485921") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("sample_stolen_button")
                    ) {
                        Text("هاتف مسروق بصنعاء 🚨", fontSize = 11.sp, color = AlertRed)
                    }
                    OutlinedButton(
                        onClick = { viewModel.testWithSampleImei("359998877665544") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("sample_clean_button")
                    ) {
                        Text("هاتف سليم غير مسجل ✅", fontSize = 11.sp, color = SuccessGreen)
                    }
                }
            }

            // 4. Verification Results Banner
            when (val state = checkResult) {
                is ImeiCheckState.Idle -> {
                    // Nothing or friendly prompt
                }
                is ImeiCheckState.Checking -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Navy700)
                    }
                }
                is ImeiCheckState.InvalidFormat -> {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningAmberLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.reason,
                                color = Navy900,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                is ImeiCheckState.Safe -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreenLight),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, SuccessGreen),
                        modifier = Modifier.testTag("safe_imei_result_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "الجهاز سليم - لا توجد بلاغات",
                                        color = SuccessGreen,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "رقم IMEI: ${state.cleanImei}",
                                        color = Navy900,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Text(
                                text = "لم يتم العثور على أي بلاغ سرقة أو فقدان مسجل لهذا الرقم التسلسلي في قاعدة البيانات الوطنية بالجمهورية اليمنية.",
                                fontSize = 12.sp,
                                color = Navy900
                            )

                            Surface(
                                color = PureWhite,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 نصيحة أمان لمحلات الهواتف والمشترين:\nتأكد دائماً من مطابقة رقم IMEI المكتوب في النظام مع الرقم المحفور على درج الشريحة وعلبة الكرتون الأصلية.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
                is ImeiCheckState.StolenAlert -> {
                    val report = state.report
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AlertRedLight),
                        border = androidx.compose.foundation.BorderStroke(2.dp, AlertRed),
                        modifier = Modifier.testTag("stolen_imei_result_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(AlertRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "⚠️ تحذير: هذا الهاتف مسجل كـ [${if (report.isStolen) "مسروق" else "مفقود"}]",
                                        color = AlertRed,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "رقم البلاغ الرسمي: #${report.id}",
                                        color = Navy900,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Surface(
                                color = PureWhite,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = "📱 الجهاز: ${report.brand} ${report.model}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "📍 موقع الحادث: ${report.governorate} - ${report.incidentLocation}", fontSize = 12.sp)
                                    Text(text = "🎨 اللون والعلامات: ${report.color} (${report.distinctiveMarks.ifBlank { "لا توجد" }})", fontSize = 12.sp)
                                    Text(text = "👤 صاحب البلاغ: ${report.contactName}", fontSize = 12.sp)
                                    if (report.rewardAmount > 0) {
                                        Text(
                                            text = "💰 مكافأة مالية معلنة: ${"%,d".format(report.rewardAmount)} ريال يمني",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8A5A00),
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (report.policeReportNumber.isNotBlank()) {
                                        Text(text = "👮 بلاغ الشرطة: ${report.policeReportNumber}", fontSize = 12.sp, color = AlertRed)
                                    }
                                }
                            }

                            Text(
                                text = "تنبيه لجميع المحلات والمهندسين: الامتناع التام عن شراء هذا الجهاز أو عمل فورمات له، والتواصل فوراً مع صاحبه عبر الأزرار أدناه.",
                                fontSize = 11.sp,
                                color = AlertRed,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Contact Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { IntentHelper.makeCall(context, report.primaryPhone) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy700),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اتصال بصاحبه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val msg = "السلام عليكم، لقد تم فحص هاتفكم المسجل (${report.brand} ${report.model}) برقم IMEI: ${report.imei1}..."
                                        IntentHelper.openWhatsApp(context, report.whatsappNumber, msg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Chat, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
