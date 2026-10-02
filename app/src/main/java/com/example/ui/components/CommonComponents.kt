package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertEntity
import com.example.data.model.ReportEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberTint
import com.example.ui.theme.BlueTint
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandCyan
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
import java.text.NumberFormat
import java.util.Locale

/** Formats numbers with Western digits and thousands separators: 150,000 */
fun formatAmount(value: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(value)

/** "Samsung (سامسونج)" -> "Samsung" */
fun shortBrand(brand: String): String = brand.substringBefore(" (").substringBefore(" / ").trim().ifBlank { brand }

@Composable
fun StatusBadge(
    reportType: String,
    isRecovered: Boolean,
    modifier: Modifier = Modifier
) {
    val bg: Color
    val fg: Color
    val icon: ImageVector
    val label: String
    when {
        isRecovered -> { bg = GreenTint; fg = SuccessGreen; icon = Icons.Default.CheckCircle; label = "تم الاسترجاع" }
        reportType == "STOLEN" -> { bg = RedTint; fg = Color(0xFFFF8A8E); icon = Icons.Default.Security; label = "مسروق" }
        reportType == "LOST" -> { bg = AmberTint; fg = WarningAmber; icon = Icons.Default.Warning; label = "مفقود" }
        else -> { bg = BlueTint; fg = Color(0xFF7CC4FF); icon = Icons.Default.PhoneAndroid; label = "معثور عليه" }
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun YemenFlagPill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
    ) {
        Column {
            Box(modifier = Modifier.size(width = 20.dp, height = 4.5.dp).background(Color(0xFFCE1126)))
            Box(modifier = Modifier.size(width = 20.dp, height = 4.5.dp).background(Color.White))
            Box(modifier = Modifier.size(width = 20.dp, height = 4.5.dp).background(Color(0xFF000000)))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = BrandSurface,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondaryLight,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UrgentAlertTicker(
    latestAlert: AlertEntity?,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Only shown when there is something new to read
    AnimatedVisibility(
        visible = latestAlert != null && unreadCount > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (latestAlert != null) {
            val urgent = latestAlert.alertType == "URGENT_THEFT"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (urgent) listOf(Color(0xFF8E1B22), Color(0xFFC62B33))
                            else listOf(BrandSurfaceHigh, BrandSurface)
                        )
                    )
                    .clickable { onClick() }
                    .testTag("urgent_alert_ticker")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PureWhite.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = latestAlert.title,
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "📍 ${latestAlert.governorate}",
                            color = PureWhite.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(YemenGold)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$unreadCount جديد",
                            color = BrandInk,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItemCard(
    report: ReportEntity,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when {
        report.isRecovered -> SuccessGreen
        report.reportType == "STOLEN" -> AlertRed
        report.reportType == "LOST" -> WarningAmber
        else -> Color(0xFF7CC4FF)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("report_item_${report.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Device avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = shortBrand(report.brand),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = YemenGold
                        )
                        Text(
                            text = formatRelativeTime(report.createdAt),
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                    Text(
                        text = report.model,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 21.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusBadge(reportType = report.reportType, isRecovered = report.isRecovered)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BrandSurfaceHigh.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${report.governorate} • ${report.incidentLocation}",
                            fontSize = 12.sp,
                            color = PureWhite.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IMEI  ${report.maskedImei}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondaryLight
                        )
                        if (report.color.isNotBlank()) {
                            Text(
                                text = report.color.substringBefore(" ("),
                                fontSize = 11.sp,
                                color = TextSecondaryLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (report.rewardAmount > 0 && !report.isRecovered) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(YemenGold.copy(alpha = 0.14f))
                        .border(1.dp, YemenGold.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "💰 مكافأة ${formatAmount(report.rewardAmount.toLong())} ريال",
                        color = YemenGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionPill(
                    icon = Icons.Default.Call,
                    label = "اتصال",
                    container = Color(0xFF1E88E5),
                    tag = "call_button_${report.id}",
                    modifier = Modifier.weight(1f),
                    onClick = onCallClick
                )
                ActionPill(
                    icon = Icons.AutoMirrored.Outlined.Chat,
                    label = "واتساب",
                    container = SuccessGreen,
                    tag = "whatsapp_button_${report.id}",
                    modifier = Modifier.weight(1f),
                    onClick = onWhatsAppClick
                )
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandSurfaceHigh)
                        .testTag("share_button_${report.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    container: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container.copy(alpha = 0.16f))
            .border(1.dp, container.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = container, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = container)
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        hours < 24 -> "منذ $hours ساعة"
        days == 1L -> "أمس"
        days < 30 -> "منذ $days أيام"
        else -> "منذ فترة"
    }
}
