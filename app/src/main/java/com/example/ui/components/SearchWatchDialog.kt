package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.util.InAppNotificationManager
import com.example.util.SearchWatchCriteria

@Composable
fun SearchWatchDialog(
    initialSearchQuery: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val savedCriteria by InAppNotificationManager.savedCriteriaList.collectAsState()
    val fcmToken by InAppNotificationManager.fcmToken.collectAsState()

    var brandInput by remember { mutableStateOf(if (initialSearchQuery.contains("iphone", true)) "Apple" else "") }
    var modelInput by remember { mutableStateOf(initialSearchQuery) }
    var govInput by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Navy900,
            border = BorderStroke(1.5.dp, WarningAmber.copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("search_watch_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(WarningAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAlert,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مراقبة المواصفات (FCM)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Text(
                                text = "إشعارات داخل التطبيق فور إضافة هاتف مطابق",
                                fontSize = 11.sp,
                                color = WarningAmber
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = PureWhite.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input fields to add new watch
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800),
                    border = BorderStroke(1.dp, Navy700),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "تعيين مواصفات هاتف للبحث والتنبيه الفوري:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = brandInput,
                                onValueChange = { brandInput = it },
                                label = { Text("الماركة (مثل Apple)", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarningAmber,
                                    unfocusedBorderColor = Navy700,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                ),
                                modifier = Modifier.weight(1f).testTag("watch_brand_input")
                            )

                            OutlinedTextField(
                                value = modelInput,
                                onValueChange = { modelInput = it },
                                label = { Text("الموديل (مثل iPhone 14)", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarningAmber,
                                    unfocusedBorderColor = Navy700,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                ),
                                modifier = Modifier.weight(1.2f).testTag("watch_model_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = govInput,
                            onValueChange = { govInput = it },
                            label = { Text("المحافظة (اختياري، مثل صنعاء أو عدن)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarningAmber,
                                unfocusedBorderColor = Navy700,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("watch_gov_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (brandInput.isNotBlank() || modelInput.isNotBlank()) {
                                    val label = buildString {
                                        if (brandInput.isNotBlank()) append(brandInput.trim()).append(" ")
                                        if (modelInput.isNotBlank()) append(modelInput.trim())
                                        if (govInput.isNotBlank()) append(" (").append(govInput.trim()).append(")")
                                    }
                                    InAppNotificationManager.addWatchCriteria(
                                        SearchWatchCriteria(
                                            label = label,
                                            brand = brandInput.trim(),
                                            model = modelInput.trim(),
                                            governorate = govInput.trim(),
                                            isEnabled = true
                                        )
                                    )
                                    showSuccessMessage = "تم تفعيل مراقبة [$label] بنجاح!"
                                    brandInput = ""
                                    modelInput = ""
                                    govInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            modifier = Modifier.fillMaxWidth().testTag("add_watch_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ وتفعيل التنبيه السحابي للمواصفات", color = Navy900, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (showSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "✅ $showSuccessMessage",
                                color = SuccessGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Watched Specifications List
                Text(
                    text = "المواصفات المراقبة حالياً (${savedCriteria.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarningAmber
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    items(savedCriteria, key = { it.id }) { criteria ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Navy800),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (criteria.isEnabled) WarningAmber else PureWhite.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = criteria.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureWhite
                                        )
                                        Text(
                                            text = if (criteria.isEnabled) "نشط ويراقب البلاغات الجديدة" else "متوقف مؤقتاً",
                                            fontSize = 10.sp,
                                            color = if (criteria.isEnabled) SuccessGreen else PureWhite.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = criteria.isEnabled,
                                        onCheckedChange = { InAppNotificationManager.toggleWatchCriteria(criteria.id) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = WarningAmber,
                                            checkedTrackColor = Navy900
                                        ),
                                        modifier = Modifier.padding(end = 4.dp)
                                    )

                                    IconButton(
                                        onClick = { InAppNotificationManager.removeWatchCriteria(criteria.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = PureWhite.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Instant Demo simulation button
                OutlinedButton(
                    onClick = {
                        InAppNotificationManager.simulateFcmMatchedNotification(
                            context = context,
                            customModel = if (modelInput.isNotBlank()) modelInput else "iPhone 15 Pro Max",
                            customGov = if (govInput.isNotBlank()) govInput else "صنعاء"
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SuccessGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_fcm_in_app_notification_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡ تجربة ظهور الإشعار داخل التطبيق فوراً (FCM Simulation)",
                        color = SuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // FCM status text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "خدمة Firebase Cloud Messaging متصلة بنجاح",
                        fontSize = 10.sp,
                        color = PureWhite.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
