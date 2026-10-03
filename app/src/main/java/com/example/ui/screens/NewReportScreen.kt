package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.YemenLocations
import com.example.ui.components.PhotoPickerSection
import com.example.ui.components.formatAmount
import com.example.ui.theme.*
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.ImeiScanner
import com.example.util.ImeiValidator

private val STEP_TITLES = listOf("الجهاز", "المكان", "التواصل")
private fun isYemeniMobile(s: String): Boolean {
    var d = ImeiValidator.clean(s).removePrefix("00967").removePrefix("967")
    if (d.length == 10) d = d.removePrefix("0")
    return d.length == 9 && d.startsWith("7")
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReportScreen(viewModel: PhoneTrackerViewModel, onBack: () -> Unit, onReportSubmitted: (Long) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val submitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var errors by rememberSaveable { mutableStateOf(false) }
    var type by rememberSaveable { mutableStateOf("STOLEN") }
    var brand by rememberSaveable { mutableStateOf(YemenLocations.PHONE_BRANDS[0]) }
    var model by rememberSaveable { mutableStateOf("") }
    var imei1 by rememberSaveable { mutableStateOf("") }
    var imei2 by rememberSaveable { mutableStateOf("") }
    var serial by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("") }
    var marks by rememberSaveable { mutableStateOf("") }
    var photosRaw by rememberSaveable { mutableStateOf("") }
    val photos = photosRaw.split("\n").filter { it.isNotBlank() }
    var gov by rememberSaveable { mutableStateOf(YemenLocations.GOVERNORATES[0]) }
    var district by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var wa by rememberSaveable { mutableStateOf("") }
    var reward by rememberSaveable { mutableStateOf("") }
    var police by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    val first = ImeiValidator.clean(imei1)
    val second = ImeiValidator.clean(imei2)
    val firstError = if (first.length != 15) "يجب أن يكون 15 رقماً (${first.length}/15)" else null
    val secondError = if (second.isNotEmpty() && second.length != 15) "يجب أن يكون 15 رقماً" else null
    val modelError = if (model.trim().length < 2) "اكتب الموديل" else null
    val locationError = if (location.trim().length < 2) "حدد المكان أو أقرب سوق" else null
    val nameError = if (name.trim().length < 2) "الاسم مطلوب" else null
    val phoneError = if (!isYemeniMobile(phone)) "رقم يمني من 9 أرقام يبدأ بـ 7" else null
    val waError = if (wa.isNotBlank() && !isYemeniMobile(wa)) "رقم غير صحيح" else null
    val warning = first.length == 15 && !ImeiValidator.isValidLuhn(first)
    val accent = when (type) { "STOLEN" -> AlertRed; "LOST" -> WarningAmber; else -> SuccessGreen }
    fun valid(s: Int) = when (s) { 0 -> modelError == null && firstError == null && secondError == null; 1 -> locationError == null; else -> nameError == null && phoneError == null && waError == null }
    fun back() { if (submitting) return; if (step > 0) { step--; errors = false } else onBack() }
    BackHandler { back() }
    val scanError: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    fun submit() {
        viewModel.submitReport(reportType = type, brand = brand, model = model.trim(), imei1 = first, imei2 = second,
            color = color.trim(), distinctiveMarks = marks.trim(), governorate = gov, district = district.trim(),
            incidentLocation = location.trim(), contactName = name.trim(), primaryPhone = ImeiValidator.clean(phone),
            whatsappNumber = ImeiValidator.clean(wa), rewardAmountStr = ImeiValidator.clean(reward),
            policeReportNumber = police.trim(), additionalNotes = notes.trim(), serialNumber = serial.trim(), photoUris = photos,
            onSuccess = { id ->
                Toast.makeText(context, viewModel.submissionSuccessMessage.value ?: "تم تأكيد نشر البلاغ", Toast.LENGTH_LONG).show()
                onReportSubmitted(id)
            }, onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() })
    }
    Scaffold(modifier = modifier, containerColor = BrandBg, topBar = {
        Column(Modifier.background(BrandBg)) {
            TopAppBar(title = { Column {
                Text("بلاغ جديد", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("الخطوة ${step + 1} من 3 • ${STEP_TITLES[step]}", color = TextSecondaryLight, fontSize = 11.sp)
            } }, navigationIcon = { IconButton(onClick = { back() }, enabled = !submitting, modifier = Modifier.testTag("new_report_back_button")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = PureWhite) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg))
            StepIndicator(step, accent)
        }
    }, bottomBar = {
        Surface(color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
            Row(Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) OutlinedButton(onClick = { back() }, enabled = !submitting, border = BorderStroke(1.dp, BrandBorder), shape = RoundedCornerShape(14.dp), modifier = Modifier.height(52.dp)) { Text("السابق", color = PureWhite) }
                Button(onClick = { focus.clearFocus(); if (!valid(step)) errors = true else if (step < 2) { errors = false; step++ } else submit() }, enabled = !submitting,
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (step == 2) AlertRed else YemenGold, contentColor = if (step == 2) PureWhite else BrandInk), modifier = Modifier.weight(1f).height(52.dp).testTag(if (step == 2) "submit_report_button" else "next_step_button")) {
                    if (submitting) CircularProgressIndicator(Modifier.size(22.dp), color = PureWhite, strokeWidth = 2.dp) else {
                        if (step == 2) { Icon(Icons.Default.Campaign, null); Spacer(Modifier.width(8.dp)) }
                        Text(if (step == 2) "نشر البلاغ" else "التالي", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }) { padding ->
        AnimatedContent(targetState = step, transitionSpec = { val dir = if (targetState > initialState) -1 else 1; (slideInHorizontally { dir * it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -dir * it / 3 } + fadeOut()) }, label = "reportStep", modifier = Modifier.padding(padding)) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (s) {
                    0 -> {
                        Label("نوع البلاغ")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TypeTile("مسروق", Icons.Default.Security, AlertRed, type == "STOLEN", Modifier.weight(1f).testTag("type_chip_stolen")) { type = "STOLEN" }
                            TypeTile("مفقود", Icons.Default.Warning, WarningAmber, type == "LOST", Modifier.weight(1f).testTag("type_chip_lost")) { type = "LOST" }
                            TypeTile("معثور عليه", Icons.Default.Search, SuccessGreen, type == "FOUND", Modifier.weight(1f).testTag("type_chip_found")) { type = "FOUND" }
                        }
                        FormCard {
                            PickerField("ماركة الهاتف", brand, YemenLocations.PHONE_BRANDS, "brand_dropdown_field") { brand = it }
                            Field(model, { model = it.take(100) }, "الموديل *", "مثال: Galaxy S23 Ultra 256GB", error = modelError.takeIf { errors }, tag = "model_input_field")
                            Field(imei1, { imei1 = ImeiValidator.clean(it).take(15) }, "IMEI الأول *", "صوّر الباركود أو اطلب *#06#", keyboard = KeyboardType.Number, mono = true,
                                error = firstError.takeIf { errors }, support = if (warning) "تنبيه: الرقم لا يطابق المعيار، تأكد منه" else "${first.length}/15", supportColor = if (warning) WarningAmber else TextSecondaryLight, tag = "imei1_input_field", trailing = {
                                    Row {
                                        IconButton(onClick = { ImeiScanner.scan(context, { imei1 = it }, scanError) }, modifier = Modifier.testTag("imei1_scan_button")) { Icon(Icons.Default.QrCodeScanner, "تصوير الباركود", tint = YemenGold) }
                                        IconButton(onClick = { clipboard.getText()?.text?.let { imei1 = ImeiValidator.clean(it).take(15) } }) { Icon(Icons.Default.ContentPaste, "لصق", tint = TextSecondaryLight) }
                                    }
                                })
                            Field(imei2, { imei2 = ImeiValidator.clean(it).take(15) }, "IMEI الثاني (اختياري)", null, keyboard = KeyboardType.Number, mono = true, error = secondError.takeIf { errors }, trailing = { IconButton(onClick = { ImeiScanner.scan(context, { imei2 = it }, scanError) }) { Icon(Icons.Default.QrCodeScanner, "تصوير الباركود", tint = YemenGold) } })
                            Field(serial, { serial = it.filter(Char::isLetterOrDigit).uppercase().take(24) }, "الرقم التسلسلي S/N (اختياري)", "صوّر باركود S/N من الكرتون", mono = true, tag = "serial_input_field", trailing = { IconButton(onClick = { ImeiScanner.scanSerial(context, { serial = it }, scanError) }, modifier = Modifier.testTag("serial_scan_button")) { Icon(Icons.Default.QrCodeScanner, "تصوير الباركود", tint = YemenGold) } })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field(color, { color = it.take(100) }, "اللون", "أسود، أزرق..", modifier = Modifier.weight(1f))
                                Field(marks, { marks = it.take(1000) }, "علامات فارقة", "خدش، كفر..", modifier = Modifier.weight(1.4f))
                            }
                        }
                        PhotoPickerSection(photos, { photosRaw = it.joinToString("\n") })
                    }
                    1 -> {
                        Label("أين حدث ذلك؟")
                        FormCard {
                            PickerField("المحافظة", gov, YemenLocations.GOVERNORATES, "governorate_dropdown_field") { gov = it }
                            Field(district, { district = it.take(100) }, "المديرية / المنطقة", "مثال: التحرير، الشيخ عثمان")
                            Field(location, { location = it.take(300) }, "المكان المحدد أو أقرب سوق *", "مثال: سوق باب السلام، باص أجرة..", error = locationError.takeIf { errors }, tag = "location_input_field")
                        }
                        HintCard("كلما كان المكان أدق، زادت فرص المحلات القريبة في التعرف على الجهاز.")
                    }
                    else -> {
                        Label("بيانات التواصل")
                        FormCard {
                            Field(name, { name = it.take(100) }, "اسم صاحب البلاغ *", null, error = nameError.takeIf { errors }, tag = "contact_name_field")
                            Field(phone, { phone = ImeiValidator.clean(it).take(14) }, "رقم الاتصال *", "77xxxxxxx", keyboard = KeyboardType.Phone, error = phoneError.takeIf { errors }, tag = "primary_phone_field")
                            Field(wa, { wa = ImeiValidator.clean(it).take(14) }, "رقم الواتساب (اختياري)", "إذا كان مختلفاً", keyboard = KeyboardType.Phone, error = waError.takeIf { errors })
                        }
                        Label("تفاصيل إضافية")
                        FormCard {
                            Field(reward, { reward = ImeiValidator.clean(it).take(9) }, "مكافأة (ريال يمني، اختياري)", "مثال: 50000", keyboard = KeyboardType.Number, support = reward.toLongOrNull()?.takeIf { it > 0 }?.let { "${formatAmount(it)} ريال" }, supportColor = YemenGold)
                            Field(police, { police = it.take(100) }, "رقم بلاغ الشرطة (إن وجد)", "رقم المحضر / اسم القسم")
                            Field(notes, { notes = it.take(2000) }, "ملاحظات", "أي تفاصيل تساعد في استرجاع الهاتف", singleLine = false)
                        }
                        Surface(shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = 0.10f), border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("مراجعة قبل النشر", color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                Text("${brand.substringBefore(" (")} ${model.trim()}", color = PureWhite, fontWeight = FontWeight.Bold)
                                Text("IMEI $first", color = TextSecondaryLight, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                if (serial.isNotBlank()) Text("S/N ${serial.trim()}", color = TextSecondaryLight, fontSize = 12.sp)
                                Text("$gov • ${location.trim()}", color = TextSecondaryLight, fontSize = 12.sp)
                                if (photos.isNotEmpty()) Text("📷 ${photos.size} صور للجهاز", color = TextSecondaryLight, fontSize = 12.sp)
                                HorizontalDivider(color = BrandBorder, modifier = Modifier.padding(vertical = 6.dp))
                                Text("سيُضاف البلاغ إلى قاعدة أمان فون بعد تأكيد الخادم. وصول التنبيهات يعتمد على الاتصال والمزامنة.", color = PureWhite.copy(alpha = 0.85f), fontSize = 11.sp)
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
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        STEP_TITLES.forEachIndexed { i, title ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(if (i <= step) YemenGold else BrandSurfaceHigh), contentAlignment = Alignment.Center) {
                    if (i < step) Icon(Icons.Default.Check, null, tint = BrandInk, modifier = Modifier.size(16.dp)) else Text("${i + 1}", color = if (i == step) BrandInk else TextSecondaryLight, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                Text(title, color = if (i == step) PureWhite else TextSecondaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (i < STEP_TITLES.lastIndex) {
                val p by animateFloatAsState(if (i < step) 1f else 0f, label = "bar")
                Box(Modifier.weight(1f).padding(horizontal = 6.dp).padding(bottom = 14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(BrandSurfaceHigh)) { Box(Modifier.fillMaxWidth(p).height(3.dp).background(YemenGold)) }
            }
        }
    }
}
@Composable private fun Label(text: String) = Text(text, color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
@Composable private fun FormCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } }
}
@Composable private fun HintCard(text: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BrandCyan.copy(alpha = 0.08f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, null, tint = BrandCyan, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(text, color = TextSecondaryLight, fontSize = 12.sp)
    }
}
@Composable private fun TypeTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(if (selected) accent.copy(alpha = 0.18f) else BrandSurface).border(if (selected) 1.5.dp else 1.dp, if (selected) accent else BrandBorder, RoundedCornerShape(16.dp)).clickable { onClick() }.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = if (selected) accent else TextSecondaryLight, modifier = Modifier.size(24.dp)); Spacer(Modifier.height(6.dp)); Text(label, color = if (selected) PureWhite else TextSecondaryLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = BrandBorder, focusedLabelColor = YemenGold, unfocusedLabelColor = TextSecondaryLight, cursorColor = YemenGold, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedContainerColor = BrandBg, unfocusedContainerColor = BrandBg, errorContainerColor = BrandBg)
@Composable private fun Field(value: String, onChange: (String) -> Unit, label: String, placeholder: String?, modifier: Modifier = Modifier.fillMaxWidth(), keyboard: KeyboardType = KeyboardType.Text, mono: Boolean = false, singleLine: Boolean = true, error: String? = null, support: String? = null, supportColor: Color = TextSecondaryLight, tag: String? = null, trailing: (@Composable () -> Unit)? = null) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, placeholder = if (placeholder != null) { { Text(placeholder, color = TextSecondaryLight.copy(alpha = 0.5f)) } } else null,
        singleLine = singleLine, minLines = if (singleLine) 1 else 3, isError = error != null,
        supportingText = when { error != null -> { { Text(error) } }; support != null -> { { Text(support, color = supportColor) } }; else -> null },
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default, fontSize = if (mono) 16.sp else 15.sp, letterSpacing = if (mono) 1.sp else 0.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), trailingIcon = trailing, shape = RoundedCornerShape(14.dp), colors = fieldColors(), modifier = if (tag != null) modifier.testTag(tag) else modifier)
}
@Composable private fun PickerField(label: String, value: String, options: List<String>, tag: String, onPick: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondaryLight) }, shape = RoundedCornerShape(14.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth().testTag(tag))
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)).clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(BrandSurfaceHigh)) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option, color = if (option == value) YemenGold else PureWhite) }, onClick = { onPick(option); expanded = false }) }
        }
    }
}
