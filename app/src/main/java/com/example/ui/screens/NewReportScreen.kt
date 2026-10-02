package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.YemenLocations
import com.example.ui.components.formatAmount
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
import com.example.util.ImeiValidator

private val STEP_TITLES = listOf("الجهاز", "المكان", "التواصل")

private fun isYemeniMobile(s: String): Boolean {
    val d = s.filter { it.isDigit() }.removePrefix("00967").removePrefix("967")
    return d.length == 9 && d.startsWith("7")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReportScreen(
    viewModel: PhoneTrackerViewModel,
    onBack: () -> Unit,
    onReportSubmitted: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var step by rememberSaveable { mutableIntStateOf(0) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    var reportType by rememberSaveable { mutableStateOf("STOLEN") }
    var selectedBrand by rememberSaveable { mutableStateOf(YemenLocations.PHONE_BRANDS[0]) }
    var model by rememberSaveable { mutableStateOf("") }
    var imei1 by rememberSaveable { mutableStateOf("") }
    var imei2 by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("") }
    var marks by rememberSaveable { mutableStateOf("") }

    var selectedGov by rememberSaveable { mutableStateOf(YemenLocations.GOVERNORATES[0]) }
    var district by rememberSaveable { mutableStateOf("") }
    var incidentLocation by rememberSaveable { mutableStateOf("") }

    var contactName by rememberSaveable { mutableStateOf("") }
    var primaryPhone by rememberSaveable { mutableStateOf("") }
    var whatsappNumber by rememberSaveable { mutableStateOf("") }
    var rewardAmount by rememberSaveable { mutableStateOf("") }
    var policeReport by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    // ---- validation ----
    val imei1Clean = ImeiValidator.clean(imei1)
    val imei2Clean = ImeiValidator.clean(imei2)
    val imei1Err = when {
        imei1Clean.isEmpty() -> "رقم IMEI مطلوب"
        imei1Clean.length != 15 -> "يجب أن يكون 15 رقماً (${imei1Clean.length}/15)"
        else -> null
    }
    val imei1Warn = imei1Clean.length == 15 && !ImeiValidator.isValidLuhn(imei1Clean)
    val imei2Err = if (imei2Clean.isNotEmpty() && imei2Clean.length != 15) "يجب أن يكون 15 رقماً" else null
    val modelErr = if (model.isBlank()) "اكتب الموديل" else null
    val locationErr = if (incidentLocation.isBlank()) "حدد المكان أو أقرب سوق" else null
    val nameErr = if (contactName.isBlank()) "الاسم مطلوب" else null
    val phoneErr = when {
        primaryPhone.isBlank() -> "رقم الاتصال مطلوب"
        !isYemeniMobile(primaryPhone) -> "رقم يمني من 9 أرقام يبدأ بـ 7"
        else -> null
    }
    val waErr = if (whatsappNumber.isNotBlank() && !isYemeniMobile(whatsappNumber)) "رقم غير صحيح" else null

    fun stepValid(s: Int) = when (s) {
        0 -> modelErr == null && imei1Err == null && imei2Err == null
        1 -> locationErr == null
        else -> nameErr == null && phoneErr == null && waErr == null
    }

    fun goBack() {
        if (step > 0) { step--; showErrors = false } else onBack()
    }
    BackHandler { goBack() }

    fun submit() {
        viewModel.submitReport(
            reportType = reportType,
            brand = selectedBrand,
            model = model.trim(),
            imei1 = imei1Clean,
            imei2 = imei2Clean,
            color = color.trim(),
            distinctiveMarks = marks.trim(),
            governorate = selectedGov,
            district = district.trim(),
            incidentLocation = incidentLocation.trim(),
            contactName = contactName.trim(),
            primaryPhone = primaryPhone.filter { it.isDigit() },
            whatsappNumber = whatsappNumber.filter { it.isDigit() },
            rewardAmountStr = rewardAmount.filter { it.isDigit() },
            policeReportNumber = policeReport.trim(),
            additionalNotes = notes.trim(),
            onSuccess = { newId ->
                Toast.makeText(context, "تم نشر البلاغ وتعميم التنبيه ✅", Toast.LENGTH_LONG).show()
                onReportSubmitted(newId)
            },
            onError = { errMsg -> Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show() }
        )
    }

    val typeAccent = when (reportType) {
        "STOLEN" -> AlertRed
        "LOST" -> WarningAmber
        else -> SuccessGreen
    }

    Scaffold(
        modifier = modifier,
        containerColor = BrandBg,
        topBar = {
            Column(Modifier.background(BrandBg)) {
                TopAppBar(
                    title = {
                        Column {
                            Text("بلاغ جديد", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("الخطوة ${step + 1} من 3 • ${STEP_TITLES[step]}", color = TextSecondaryLight, fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { goBack() }, modifier = Modifier.testTag("new_report_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PureWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg)
                )
                StepIndicator(step = step, accent = typeAccent)
            }
        },
        bottomBar = {
            Surface(color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = { goBack() },
                            border = BorderStroke(1.dp, BrandBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(52.dp)
                        ) { Text("السابق", color = PureWhite, fontWeight = FontWeight.Bold) }
                    }
                    val last = step == 2
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (!stepValid(step)) {
                                showErrors = true
                            } else if (!last) {
                                showErrors = false
                                step++
                            } else if (!isSubmitting) {
                                submit()
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (last) AlertRed else YemenGold,
                            contentColor = if (last) PureWhite else BrandInk
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag(if (last) "submit_report_button" else "next_step_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        } else if (last) {
                            Icon(Icons.Default.Campaign, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("نشر البلاغ وتعميم التنبيه", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        } else {
                            Text("التالي", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) -1 else 1 // RTL-friendly
                (slideInHorizontally { dir * it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -dir * it / 3 } + fadeOut())
            },
            label = "reportStep",
            modifier = Modifier.padding(innerPadding)
        ) { s ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (s) {
                    0 -> {
                        Label("نوع البلاغ")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TypeTile("مسروق", Icons.Default.Security, AlertRed, reportType == "STOLEN", Modifier.weight(1f).testTag("type_chip_stolen")) { reportType = "STOLEN" }
                            TypeTile("مفقود", Icons.Default.Warning, WarningAmber, reportType == "LOST", Modifier.weight(1f).testTag("type_chip_lost")) { reportType = "LOST" }
                            TypeTile("معثور عليه", Icons.Default.Search, SuccessGreen, reportType == "FOUND", Modifier.weight(1f).testTag("type_chip_found")) { reportType = "FOUND" }
                        }
                        FormCard {
                            PickerField("ماركة الهاتف", selectedBrand, YemenLocations.PHONE_BRANDS, "brand_dropdown_field") { selectedBrand = it }
                            Field(model, { model = it }, "الموديل *", "مثال: Galaxy S23 Ultra 256GB",
                                error = modelErr.takeIf { showErrors }, tag = "model_input_field")
                            Field(
                                imei1, { imei1 = it.filter(Char::isDigit).take(15) }, "IMEI الأول *", "اطلب *#06# أو من الكرتون",
                                keyboard = KeyboardType.Number, mono = true,
                                error = imei1Err.takeIf { showErrors || imei1Clean.length == 15 },
                                support = when {
                                    imei1Warn -> "تنبيه: الرقم لا يطابق المعيار، تأكد منه"
                                    imei1Clean.length == 15 -> "✓ رقم صحيح"
                                    else -> "${imei1Clean.length}/15"
                                },
                                supportColor = when {
                                    imei1Warn -> WarningAmber
                                    imei1Clean.length == 15 -> SuccessGreen
                                    else -> TextSecondaryLight
                                },
                                tag = "imei1_input_field",
                                trailing = {
                                    IconButton(onClick = {
                                        clipboardManager.getText()?.text?.let { c -> imei1 = c.filter(Char::isDigit).take(15) }
                                    }) { Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = YemenGold) }
                                }
                            )
                            Field(imei2, { imei2 = it.filter(Char::isDigit).take(15) }, "IMEI الثاني (اختياري)", null,
                                keyboard = KeyboardType.Number, mono = true, error = imei2Err.takeIf { showErrors })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field(color, { color = it }, "اللون", "أسود، أزرق..", modifier = Modifier.weight(1f))
                                Field(marks, { marks = it }, "علامات فارقة", "خدش، كفر..", modifier = Modifier.weight(1.4f))
                            }
                        }
                    }
                    1 -> {
                        Label("أين حدث ذلك؟")
                        FormCard {
                            PickerField("المحافظة", selectedGov, YemenLocations.GOVERNORATES, "governorate_dropdown_field") { selectedGov = it }
                            Field(district, { district = it }, "المديرية / المنطقة", "مثال: التحرير، الشيخ عثمان")
                            Field(incidentLocation, { incidentLocation = it }, "المكان المحدد أو أقرب سوق *",
                                "مثال: سوق باب السلام، باص أجرة..", error = locationErr.takeIf { showErrors }, tag = "location_input_field")
                        }
                        HintCard("كلما كان المكان أدق، زادت فرص المحلات القريبة في التعرف على الجهاز.")
                    }
                    else -> {
                        Label("بيانات التواصل")
                        FormCard {
                            Field(contactName, { contactName = it }, "اسم صاحب البلاغ *", null,
                                error = nameErr.takeIf { showErrors }, tag = "contact_name_field")
                            Field(primaryPhone, { primaryPhone = it.filter(Char::isDigit).take(12) }, "رقم الاتصال *", "77xxxxxxx",
                                keyboard = KeyboardType.Phone, error = phoneErr.takeIf { showErrors }, tag = "primary_phone_field")
                            Field(whatsappNumber, { whatsappNumber = it.filter(Char::isDigit).take(12) }, "رقم الواتساب (اختياري)", "إذا كان مختلفاً",
                                keyboard = KeyboardType.Phone, error = waErr.takeIf { showErrors })
                        }
                        Label("تفاصيل إضافية")
                        FormCard {
                            val rewardDigits = rewardAmount.filter(Char::isDigit)
                            Field(rewardAmount, { rewardAmount = it.filter(Char::isDigit).take(9) }, "مكافأة (ريال يمني، اختياري)", "مثال: 50000",
                                keyboard = KeyboardType.Number,
                                support = rewardDigits.toLongOrNull()?.takeIf { it > 0 }?.let { "${formatAmount(it)} ريال" },
                                supportColor = YemenGold)
                            Field(policeReport, { policeReport = it }, "رقم بلاغ الشرطة (إن وجد)", "رقم المحضر / اسم القسم")
                            Field(notes, { notes = it }, "ملاحظات", "أي تفاصيل تساعد في استرجاع الهاتف", singleLine = false)
                        }
                        // Summary
                        Surface(shape = RoundedCornerShape(18.dp), color = typeAccent.copy(alpha = 0.10f), border = BorderStroke(1.dp, typeAccent.copy(alpha = 0.5f))) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("مراجعة قبل النشر", color = typeAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                Text("${selectedBrand.substringBefore(" (")} ${model.trim()}", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("IMEI $imei1Clean", color = TextSecondaryLight, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                Text("$selectedGov • ${incidentLocation.trim()}", color = TextSecondaryLight, fontSize = 12.sp)
                                HorizontalDivider(color = BrandBorder, modifier = Modifier.padding(vertical = 6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = YemenGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("سيصل تنبيه فوري لكل المحلات والمستخدمين في اليمن", color = PureWhite.copy(alpha = 0.85f), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, accent: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        STEP_TITLES.forEachIndexed { i, title ->
            val done = i < step
            val active = i == step
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (done || active) YemenGold else BrandSurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) Icon(Icons.Default.Check, contentDescription = null, tint = BrandInk, modifier = Modifier.size(16.dp))
                    else Text("${i + 1}", color = if (active) BrandInk else TextSecondaryLight, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                Text(title, color = if (active) PureWhite else TextSecondaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (i < STEP_TITLES.lastIndex) {
                val p by animateFloatAsState(if (i < step) 1f else 0f, label = "bar")
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .padding(bottom = 14.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BrandSurfaceHigh)
                ) {
                    Box(Modifier.fillMaxWidth(p).height(3.dp).background(YemenGold))
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) = Text(text, color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun HintCard(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandCyan.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextSecondaryLight, fontSize = 12.sp)
    }
}

@Composable
private fun TypeTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else BrandSurface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) accent else BrandBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) accent else TextSecondaryLight, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (selected) PureWhite else TextSecondaryLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = YemenGold,
    unfocusedBorderColor = BrandBorder,
    focusedLabelColor = YemenGold,
    unfocusedLabelColor = TextSecondaryLight,
    cursorColor = YemenGold,
    focusedTextColor = PureWhite,
    unfocusedTextColor = PureWhite,
    focusedContainerColor = BrandBg,
    unfocusedContainerColor = BrandBg,
    errorContainerColor = BrandBg
)

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboard: KeyboardType = KeyboardType.Text,
    mono: Boolean = false,
    singleLine: Boolean = true,
    error: String? = null,
    support: String? = null,
    supportColor: Color = TextSecondaryLight,
    tag: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder, color = TextSecondaryLight.copy(alpha = 0.5f)) } } else null,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = error != null,
        supportingText = when {
            error != null -> { { Text(error) } }
            support != null -> { { Text(support, color = supportColor) } }
            else -> null
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontSize = if (mono) 16.sp else 15.sp,
            letterSpacing = if (mono) 1.sp else 0.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        trailingIcon = trailing,
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(),
        modifier = if (tag != null) modifier.testTag(tag) else modifier
    )
}

@Composable
private fun PickerField(label: String, value: String, options: List<String>, tag: String, onPick: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryLight) },
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().testTag(tag)
        )
        // Whole field is tappable
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)).clickable { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(BrandSurfaceHigh)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = if (opt == value) YemenGold else PureWhite) },
                    onClick = { onPick(opt); expanded = false }
                )
            }
        }
    }
}
