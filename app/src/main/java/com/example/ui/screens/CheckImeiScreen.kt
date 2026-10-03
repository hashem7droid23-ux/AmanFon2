package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.ImeiCheckState
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.ImeiValidator
import com.example.util.ImeiScanner
import com.example.util.IntentHelper
import com.example.util.ReportPolicy
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckImeiScreen(viewModel: PhoneTrackerViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val input by viewModel.imeiSearchInput.collectAsStateWithLifecycle()
    val state by viewModel.imeiCheckResult.collectAsStateWithLifecycle()
    val clean = ImeiValidator.clean(input)
    val complete = ReportPolicy.isCompleteImei(clean)
    fun check() { focus.clearFocus(); viewModel.checkImei() }
    fun setInput(value: String) { viewModel.clearImeiCheck(); viewModel.imeiSearchInput.value = ImeiValidator.clean(value).take(15) }
    Scaffold(modifier = modifier, containerColor = BrandBg, topBar = {
        TopAppBar(title = { Column {
            Text("فحص IMEI", color = PureWhite, fontWeight = FontWeight.ExtraBold)
            Text("تحقق من البلاغات قبل الشراء", color = TextSecondaryLight, fontSize = 11.sp)
        } }, navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("imei_back_button")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = PureWhite) }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = input, onValueChange = { setInput(it) }, label = { Text("رقم IMEI (15 رقماً)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { if (complete) check() }),
                        textStyle = androidx.compose.ui.text.TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 18.sp),
                        trailingIcon = { IconButton(onClick = {
                            if (input.isNotEmpty()) viewModel.clearImeiCheck() else clipboard.getText()?.text?.let { setInput(it) }
                        }) { Icon(if (input.isEmpty()) Icons.Default.ContentPaste else Icons.Default.Clear, "لصق أو مسح", tint = YemenGold) } },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YemenGold, unfocusedBorderColor = BrandBorder, focusedLabelColor = YemenGold, unfocusedLabelColor = TextSecondaryLight, cursorColor = YemenGold),
                        modifier = Modifier.fillMaxWidth().testTag("imei_input_field"))
                    Text("${clean.length}/15" + if (complete && !ImeiValidator.isValidLuhn(clean)) " • رقم غير قياسي، راجع الرقم" else "", color = TextSecondaryLight, fontSize = 12.sp)
                    OutlinedButton(onClick = { ImeiScanner.scan(context, { setInput(it) }, { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }) }, modifier = Modifier.fillMaxWidth().testTag("scan_check_imei_button")) {
                        Icon(Icons.Default.PhotoCamera, null, tint = YemenGold); Spacer(Modifier.width(8.dp)); Text("مسح باركود IMEI", color = PureWhite)
                    }
                    Button(onClick = { check() }, enabled = complete && state !is ImeiCheckState.Checking,
                        colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = BrandInk),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_check_imei_button")) {
                        if (state is ImeiCheckState.Checking) CircularProgressIndicator(Modifier.size(20.dp), color = BrandInk) else {
                            Icon(Icons.Default.Security, null); Spacer(Modifier.width(8.dp)); Text("فحص قاعدة البلاغات", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            when (val result = state) {
                ImeiCheckState.Idle -> {
                    Text("اطلب *#06# ثم أدخل الرقم أو امسح الباركود. الفحص يحتاج تسجيل الدخول واتصالاً بالإنترنت.", color = TextSecondaryLight)
                    Text("عدم وجود بلاغ ليس إثبات ملكية ولا ضماناً أن الجهاز غير مسروق.", color = WarningAmber)
                }
                ImeiCheckState.Checking -> Text("نتحقق من البيانات على الخادم…", color = TextSecondaryLight)
                is ImeiCheckState.InvalidFormat -> CheckBanner("تحقق من الرقم", result.reason, WarningAmber, "imei_result_banner")
                is ImeiCheckState.Unavailable -> CheckBanner("الفحص غير مكتمل", result.reason, WarningAmber, "unavailable_imei_result_card")
                is ImeiCheckState.Safe -> {
                    CheckBanner("لا يوجد بلاغ نشط لهذا الرقم", "لم نجد بلاغ سرقة أو فقدان نشطاً في قاعدة أمان فون وقت الفحص. هذا ليس ضماناً لسلامة الجهاز أو إثباتاً لملكيته.", WarningAmber, "safe_imei_result_card")
                    Text(result.cleanImei, color = PureWhite, fontFamily = FontFamily.Monospace)
                    if (!result.isLuhnValid) Text("الرقم لا يجتاز تحقق IMEI القياسي. طابقه مع الجهاز والعلبة.", color = WarningAmber)
                    Text("طابق الرقم في الجهاز والعلبة والفاتورة، واطلب إثبات الملكية قبل الدفع.", color = TextSecondaryLight)
                }
                is ImeiCheckState.StolenAlert -> {
                    val r = result.report
                    CheckBanner(if (r.isStolen) "يوجد بلاغ سرقة: لا تشترِ الجهاز" else "يوجد بلاغ فقدان: لا تشترِ الجهاز", "بلاغ مسجّل في أمان فون #${r.id}", AlertRed, "stolen_imei_result_card")
                    Surface(color = BrandSurface, shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${r.brand} ${r.model}", color = PureWhite, fontWeight = FontWeight.Bold)
                            Text("${r.governorate} • ${r.incidentLocation}", color = TextSecondaryLight)
                            Text("${r.color}\n${r.distinctiveMarks}\nصاحب البلاغ: ${r.contactName}", color = PureWhite)
                            if (r.policeReportNumber.isNotBlank()) Text("بلاغ الشرطة: ${r.policeReportNumber}", color = AlertRed)
                            if (r.rewardAmount > 0) Text("المكافأة: ${com.example.ui.components.formatAmount(r.rewardAmount)} ريال", color = YemenGold)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { IntentHelper.makeCall(context, r.primaryPhone) }, modifier = Modifier.weight(1f)) { Text("اتصال") }
                                Button(onClick = { IntentHelper.openWhatsApp(context, r.whatsappNumber, "السلام عليكم، أتواصل بخصوص بلاغ ${r.brand} ${r.model} عبر أمان فون") }, modifier = Modifier.weight(1f)) { Text("واتساب") }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun CheckBanner(title: String, message: String, accent: androidx.compose.ui.graphics.Color, tag: String) {
    Surface(color = BrandSurface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, accent), modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(message, color = PureWhite, fontSize = 13.sp)
        }
    }
}
