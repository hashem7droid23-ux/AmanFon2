package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.YemenLocations
import com.example.ui.theme.AlertRed
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.ImeiValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReportScreen(
    viewModel: PhoneTrackerViewModel,
    onBack: () -> Unit,
    onReportSubmitted: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    var reportType by remember { mutableStateOf("STOLEN") }
    var selectedBrand by remember { mutableStateOf(YemenLocations.PHONE_BRANDS[0]) }
    var brandDropdownExpanded by remember { mutableStateOf(false) }

    var model by remember { mutableStateOf("") }
    var imei1 by remember { mutableStateOf("") }
    var imei2 by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var marks by remember { mutableStateOf("") }

    var selectedGov by remember { mutableStateOf(YemenLocations.GOVERNORATES[0]) }
    var govDropdownExpanded by remember { mutableStateOf(false) }
    var district by remember { mutableStateOf("") }
    var incidentLocation by remember { mutableStateOf("") }

    var contactName by remember { mutableStateOf("") }
    var primaryPhone by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var rewardAmount by remember { mutableStateOf("") }
    var policeReport by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var instantBroadcastEnabled by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تقديم بلاغ عن هاتف في اليمن",
                        color = PureWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("new_report_back_button")
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
            // 1. Report Type Selector
            Text(
                text = "نوع البلاغ:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = reportType == "STOLEN",
                    onClick = { reportType = "STOLEN" },
                    label = { Text("هاتف مسروق 🚨") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AlertRed,
                        selectedLabelColor = PureWhite
                    ),
                    modifier = Modifier.weight(1f).testTag("type_chip_stolen")
                )
                FilterChip(
                    selected = reportType == "LOST",
                    onClick = { reportType = "LOST" },
                    label = { Text("هاتف مفقود ⚠️") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WarningAmber,
                        selectedLabelColor = Navy900
                    ),
                    modifier = Modifier.weight(1f).testTag("type_chip_lost")
                )
                FilterChip(
                    selected = reportType == "FOUND",
                    onClick = { reportType = "FOUND" },
                    label = { Text("معثور عليه 📱") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SuccessGreen,
                        selectedLabelColor = PureWhite
                    ),
                    modifier = Modifier.weight(1f).testTag("type_chip_found")
                )
            }

            // 2. Device Information Section
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Navy700)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بيانات الهاتف ومواصفاته",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Brand Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedBrand,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ماركة الهاتف *") },
                            trailingIcon = {
                                IconButton(onClick = { brandDropdownExpanded = !brandDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("brand_dropdown_field")
                        )
                        DropdownMenu(
                            expanded = brandDropdownExpanded,
                            onDismissRequest = { brandDropdownExpanded = false }
                        ) {
                            YemenLocations.PHONE_BRANDS.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand) },
                                    onClick = {
                                        selectedBrand = brand
                                        brandDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Model Name
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("الموديل بالتحديد * (مثال: Galaxy S23 Ultra)") },
                        placeholder = { Text("اسم الموديل وسعة الذاكرة") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_input_field")
                    )

                    // IMEI 1
                    OutlinedTextField(
                        value = imei1,
                        onValueChange = { imei1 = it },
                        label = { Text("رقم IMEI الأول (15 رقماً) *") },
                        placeholder = { Text("اطلبه بـ *#06# أو من الكرتون") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) imei1 = clip
                                }
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = Navy700)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("imei1_input_field")
                    )

                    // IMEI 2
                    OutlinedTextField(
                        value = imei2,
                        onValueChange = { imei2 = it },
                        label = { Text("رقم IMEI الثاني (اختياري للشريحة 2)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Color and distinctive marks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = { Text("اللون") },
                            placeholder = { Text("أسود، أزرق..") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = marks,
                            onValueChange = { marks = it },
                            label = { Text("علامات فارقة") },
                            placeholder = { Text("خدش، كفر، ملصق..") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }

            // 3. Location in Yemen Section
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AlertRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "موقع الحادثة في اليمن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Governorate
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedGov,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المحافظة *") },
                            trailingIcon = {
                                IconButton(onClick = { govDropdownExpanded = !govDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("governorate_dropdown_field")
                        )
                        DropdownMenu(
                            expanded = govDropdownExpanded,
                            onDismissRequest = { govDropdownExpanded = false }
                        ) {
                            YemenLocations.GOVERNORATES.forEach { gov ->
                                DropdownMenuItem(
                                    text = { Text(gov) },
                                    onClick = {
                                        selectedGov = gov
                                        govDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // District
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("المديرية / المنطقة") },
                        placeholder = { Text("مثال: التحرير، الشيخ عثمان، صالة..") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Specific Location / Market
                    OutlinedTextField(
                        value = incidentLocation,
                        onValueChange = { incidentLocation = it },
                        label = { Text("المكان المحدد أو أقرب سوق هواتف *") },
                        placeholder = { Text("مثال: سوق باب السلام، شارع جمال، باص أجرة..") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_input_field")
                    )
                }
            }

            // 4. Contact and Reward Information Section
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Navy700)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بيانات التواصل والمكافأة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("اسم صاحب البلاغ *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_name_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = primaryPhone,
                            onValueChange = { primaryPhone = it },
                            label = { Text("رقم الاتصال *") },
                            placeholder = { Text("77xxxxxxx") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("primary_phone_field")
                        )
                        OutlinedTextField(
                            value = whatsappNumber,
                            onValueChange = { whatsappNumber = it },
                            label = { Text("رقم الواتساب") },
                            placeholder = { Text("77xxxxxxx") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Reward in YER
                    OutlinedTextField(
                        value = rewardAmount,
                        onValueChange = { rewardAmount = it },
                        label = { Text("مكافأة مالية تحفيزية (ريال يمني - اختياري)") },
                        placeholder = { Text("مثال: 50000 أو 100000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = WarningAmber)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Police Report
                    OutlinedTextField(
                        value = policeReport,
                        onValueChange = { policeReport = it },
                        label = { Text("رقم بلاغ قسم الشرطة أو البحث الجنائي (إن وجد)") },
                        placeholder = { Text("رقم المحضر / اسم القسم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Additional Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية") },
                        placeholder = { Text("أي تفاصيل تساعد في استرجاع الهاتف") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 5. Instant Notification Broadcast Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Navy800)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PureWhite.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "إرسال إشعار فوري لجميع المستخدمين",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "يصل البلاغ مباشرة لجميع محلات الهواتف والمستخدمين في اليمن",
                                color = PureWhite.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    Switch(
                        checked = instantBroadcastEnabled,
                        onCheckedChange = { instantBroadcastEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = WarningAmber
                        )
                    )
                }
            }

            // 6. Submit Button
            Button(
                onClick = {
                    viewModel.submitReport(
                        reportType = reportType,
                        brand = selectedBrand,
                        model = model,
                        imei1 = imei1,
                        imei2 = imei2,
                        color = color,
                        distinctiveMarks = marks,
                        governorate = selectedGov,
                        district = district,
                        incidentLocation = incidentLocation,
                        contactName = contactName,
                        primaryPhone = primaryPhone,
                        whatsappNumber = whatsappNumber,
                        rewardAmountStr = rewardAmount,
                        policeReportNumber = policeReport,
                        additionalNotes = notes,
                        onSuccess = { newId ->
                            Toast.makeText(context, "تم تسجيل البلاغ وبث التنبيه بنجاح!", Toast.LENGTH_LONG).show()
                            onReportSubmitted(newId)
                        },
                        onError = { errMsg ->
                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_report_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Security, contentDescription = null, tint = PureWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "نشر البلاغ وتعميم التنبيه الفوري",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
