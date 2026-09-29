package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GoldAlert
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YemenGold
import kotlinx.coroutines.delay

/**
 * Animated Motion Graphics Splash Screen for "أمان فون"
 * Showcases:
 * 1. Animated App Logo & Name (أمان فون - Aman Phone)
 * 2. Official App Purpose & Functions (تتبع الهواتف وفحص IMEI والتعاميم)
 * 3. App Developer Credentials & Contact (م. هاشم القديمي - 714525890)
 * Automatically navigates to Login Screen after a few moments.
 */
@Composable
fun MotionSplashScreen(
    onFinishSplash: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Animation visibility phases
    var showLogo by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var showPurpose by remember { mutableStateOf(false) }
    var showDeveloper by remember { mutableStateOf(false) }
    var showFooter by remember { mutableStateOf(false) }

    // Progress animation for auto-transition (3.8 seconds total duration)
    val progress = remember { Animatable(0f) }

    // Infinite transitions for cyber motion effects
    val infiniteTransition = rememberInfiniteTransition(label = "MotionCyber")

    // Radar scan rotation
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )

    // Glowing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Cyber aura glow alpha
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraAlpha"
    )

    // Trigger staggered motion sequence
    LaunchedEffect(Unit) {
        delay(120)
        showLogo = true
        delay(350)
        showTitle = true
        delay(400)
        showPurpose = true
        delay(450)
        showDeveloper = true
        delay(200)
        showFooter = true

        // Progress bar smooth animation over 3.6 seconds
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3600, easing = LinearEasing)
        )
        // Transition to Login Screen
        onFinishSplash()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF020710),
                        Navy900,
                        Color(0xFF061426),
                        Color(0xFF030A14)
                    )
                )
            )
            .testTag("motion_splash_screen")
    ) {
        // Background Cyber Motion Graphics Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.28f)
            val maxRadius = size.width * 0.75f

            // Concentric radar scan rings
            drawCircle(
                color = Color(0xFF1E88E5).copy(alpha = 0.12f),
                radius = maxRadius * 0.45f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.08f),
                radius = maxRadius * 0.7f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = YemenGold.copy(alpha = 0.07f),
                radius = maxRadius * 0.95f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Dynamic rotating radar scanning ray
            val rayLength = maxRadius * 0.75f
            val rad = Math.toRadians(radarAngle.toDouble())
            val targetX = center.x + (rayLength * Math.cos(rad)).toFloat()
            val targetY = center.y + (rayLength * Math.sin(rad)).toFloat()

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.4f), Color.Transparent),
                    start = center,
                    end = Offset(targetX, targetY)
                ),
                start = center,
                end = Offset(targetX, targetY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Main Foreground Motion Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Logo & Glowing Shield Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = showLogo,
                    enter = scaleIn(
                        initialScale = 0.4f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f)
                    ) + fadeIn(tween(600))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                    ) {
                        // Ambient Cyber Glowing Aura
                        Box(
                            modifier = Modifier
                                .size(115.dp)
                                .alpha(auraAlpha)
                                .blur(22.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF00E5FF), YemenGold, Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                        )

                        // Central Shield Container
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0A1C32),
                            border = androidx.compose.foundation.BorderStroke(
                                2.5.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        YemenGold,
                                        Color(0xFF00E5FF),
                                        Color(0xFF1E88E5),
                                        YemenGold
                                    )
                                )
                            ),
                            shadowElevation = 14.dp,
                            modifier = Modifier.size(105.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "شعار أمان فون",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .scale(1.2f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. App Name & Typography
                AnimatedVisibility(
                    visible = showTitle,
                    enter = slideInVertically(
                        initialOffsetY = { 60 },
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(500))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "أمان",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = PureWhite,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "فون",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = YemenGold,
                                letterSpacing = 1.sp
                            )
                        }

                        // English subtitle badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Navy800.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E88E5).copy(alpha = 0.5f)),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "AMAN PHONE • YEMEN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64B5F6),
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Official App Purpose & Functions Card (وظيفة التطبيق)
            AnimatedVisibility(
                visible = showPurpose,
                enter = slideInVertically(
                    initialOffsetY = { 80 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(600))
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1F38).copy(alpha = 0.85f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = YemenGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "المنظومة الوطنية لحماية الأجهزة الذكية",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "منصة يمنية مركزية متخصصة في تتبع وبلاغات الهواتف المفقودة والمسروقة، وفحص سلامة الـ IMEI، وتنسيق التعاميم الفورية مع شبكة محلات الهواتف المعتمدة في كافة المحافظات.",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = PureWhite.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3 Quick Functional Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PurposeMiniChip(icon = Icons.Default.QrCodeScanner, text = "فحص IMEI")
                            PurposeMiniChip(icon = Icons.Default.PhoneIphone, text = "بلاغات فورية")
                            PurposeMiniChip(icon = Icons.Default.CheckCircle, text = "تعاميم المحلات")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Developer Presentation Card (اسم ورقم مطور التطبيق)
            AnimatedVisibility(
                visible = showDeveloper,
                enter = slideInVertically(
                    initialOffsetY = { 90 },
                    animationSpec = tween(650, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(650))
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2642)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(YemenGold, Color(0xFF00E5FF), YemenGold)
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Developer Header with Crown
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "👑", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تطوير وبرمجة مهندس المنظومة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = YemenGold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Developer Name
                        Text(
                            text = "م. هاشم القديمي",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = PureWhite
                        )
                        Text(
                            text = "Eng. Hashem Al-Qudaimi",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF90CAF9)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Phone & WhatsApp Contact Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF081729),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Copy phone number and offer call/whatsapp
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Developer Phone", "714525890")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ رقم المطور: 714525890", Toast.LENGTH_SHORT).show()

                                    // Open dialer
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:714525890"))
                                        context.startActivity(dialIntent)
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(SafeGreen.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = null,
                                            tint = SafeGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "هاتف وواتساب المطور:",
                                            fontSize = 10.5.sp,
                                            color = PureWhite.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "714525890 (967+)",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureWhite
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SafeGreen.copy(alpha = 0.25f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "نسخ",
                                            tint = SafeGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "اتصال / نسخ",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SafeGreen
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Email Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = PureWhite.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "hashem7droid23@gmail.com",
                                fontSize = 11.sp,
                                color = PureWhite.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Bottom Loading Indicator & Skip Button
            AnimatedVisibility(
                visible = showFooter,
                enter = fadeIn(tween(400))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated progress indicator bar
                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = YemenGold,
                        trackColor = Color(0xFF1E3553)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fast Skip / Enter Now Button
                    Button(
                        onClick = onFinishSplash,
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy700.copy(alpha = 0.9f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("skip_splash_button")
                    ) {
                        Text(
                            text = "الدخول لتسجيل الدخول ⚡",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PurposeMiniChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF071526),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF2196F3).copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PureWhite.copy(alpha = 0.9f)
            )
        }
    }
}
