package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YemenGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ultra-smooth, high-performance Motion Graphics Splash Screen for "أمان فون"
 * Built with GPU-accelerated graphicsLayer transformations for 60/120 FPS fluidity:
 * - App Shield Emblem & Logo
 * - App Mission & Core Functions
 * - Executive Developer Profile (م. هاشم القديمي - 714525890)
 * - Automatic smooth transition without progress bar
 */
@Composable
fun MotionSplashScreen(
    onFinishSplash: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Master unified animation timeline (0.0f to 1.0f)
    // Avoids separate delay recompositions that cause jank and stutter
    val masterAnim = remember { Animatable(0f) }

    // Subtle, lightweight rotating accent ring for the logo (0 to 360 degrees)
    val infiniteTransition = rememberInfiniteTransition(label = "AuraRing")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    // Run master fluid timeline
    LaunchedEffect(Unit) {
        // Master entrance: smooth Material Emphasized Decelerate (1200ms)
        masterAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1100,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        )
        // Hold for the user to comfortably view the branding and developer identity
        delay(2500)
        // Smoothly proceed to Login Screen
        onFinishSplash()
    }

    // Helper functions to interpolate graphicsLayer values smoothly without layout passes
    fun phaseAlpha(start: Float, end: Float): Float {
        val t = masterAnim.value
        return if (t < start) 0f else if (t >= end) 1f else ((t - start) / (end - start))
    }

    fun phaseTranslationY(start: Float, end: Float, distance: Float): Float {
        val alpha = phaseAlpha(start, end)
        return (1f - alpha) * distance
    }

    fun phaseScale(start: Float, end: Float, fromScale: Float = 0.85f): Float {
        val alpha = phaseAlpha(start, end)
        return fromScale + (1f - fromScale) * alpha
    }

    val logoAlpha = phaseAlpha(0.00f, 0.45f)
    val logoScale = phaseScale(0.00f, 0.45f, 0.70f)

    val titleAlpha = phaseAlpha(0.20f, 0.60f)
    val titleY = phaseTranslationY(0.20f, 0.60f, 40f)

    val purposeAlpha = phaseAlpha(0.35f, 0.75f)
    val purposeY = phaseTranslationY(0.35f, 0.75f, 45f)

    val devAlpha = phaseAlpha(0.50f, 0.90f)
    val devY = phaseTranslationY(0.50f, 0.90f, 50f)

    val footerAlpha = phaseAlpha(0.70f, 1.00f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030812),
                        Color(0xFF071426),
                        Color(0xFF0A1E38),
                        Color(0xFF040A14)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tap anywhere to skip immediately
                onFinishSplash()
            }
            .testTag("motion_splash_screen")
    ) {
        // Subtle ambient cybersecurity background (Pure GPU drawn, zero layout impact)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.22f)
            
            // Soft radial ambient light behind the logo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.10f),
                        Color(0xFF0D47A1).copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.55f
                ),
                center = center,
                radius = size.width * 0.55f
            )

            // Very subtle outer grid line
            drawCircle(
                color = Color(0xFF1E88E5).copy(alpha = 0.08f),
                center = center,
                radius = size.width * 0.42f,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Main Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Logo & App Branding Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated Emblem Container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(124.dp)
                        .graphicsLayer {
                            alpha = logoAlpha
                            scaleX = logoScale
                            scaleY = logoScale
                        }
                ) {
                    // Outer rotating thin cyber ring
                    Canvas(
                        modifier = Modifier
                            .size(122.dp)
                            .rotate(ringRotation)
                    ) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    YemenGold.copy(alpha = 0.8f),
                                    Color(0xFF00E5FF).copy(alpha = 0.6f),
                                    Color.Transparent,
                                    YemenGold.copy(alpha = 0.8f)
                                )
                            ),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Luxury central badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF08182B),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            Brush.linearGradient(listOf(YemenGold, Color(0xFF00E5FF)))
                        ),
                        shadowElevation = 12.dp,
                        modifier = Modifier.size(102.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "شعار أمان فون",
                                modifier = Modifier
                                    .size(96.dp)
                                    .graphicsLayer {
                                        scaleX = 1.15f
                                        scaleY = 1.15f
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // App Title & Tagline
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAlpha
                        translationY = titleY
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "أمان",
                            fontSize = 35.sp,
                            fontWeight = FontWeight.Black,
                            color = PureWhite,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "فون",
                            fontSize = 35.sp,
                            fontWeight = FontWeight.Black,
                            color = YemenGold,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0D253F),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E88E5).copy(alpha = 0.4f)),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "AMAN PHONE • REPUBLIC OF YEMEN",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90CAF9),
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Official System Mission Card (وظيفة المنظومة)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF081B30)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF1976D2).copy(alpha = 0.5f),
                            Color(0xFF00E5FF).copy(alpha = 0.4f),
                            Color(0xFF1976D2).copy(alpha = 0.5f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = purposeAlpha
                        translationY = purposeY
                    }
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
                            text = "المنظومة الوطنية الموحدة لتتبع وحماية الأجهزة",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "منصة رقمية مركزية معتمدة للتحقق الفوري من سلامة الأجهزة الذكية عبر الـ IMEI، وتوثيق بلاغات الفقدان والسرقة، وتنسيق التعاميم اللحظية مع شبكة محلات الهواتف المعتمدة في كافة المحافظات اليمنية.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = PureWhite.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Functional Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ExecutivePill(icon = Icons.Default.QrCodeScanner, text = "فحص الـ IMEI")
                        ExecutivePill(icon = Icons.Default.PhoneIphone, text = "بلاغات فورية")
                        ExecutivePill(icon = Icons.Default.CheckCircle, text = "تعاميم المحلات")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Executive Developer Card (بطاقة مطور المنظومة - م. هاشم القديمي)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C223A)),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(
                        listOf(YemenGold, Color(0xFF00E5FF), YemenGold)
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = devAlpha
                        translationY = devY
                    }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Tag
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = YemenGold.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, YemenGold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                        ) {
                            Text(text = "👑", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "تطوير وبرمجة مهندس المنظومة",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = YemenGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Developer Name
                    Text(
                        text = "م. هاشم القديمي",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite
                    )
                    Text(
                        text = "Eng. Hashem Al-Qudaimi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF90CAF9)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contact & Action Row (Phone + Direct Call/Copy/WhatsApp)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF061424),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Copy phone number
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Developer Phone", "714525890")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ رقم المطور: 714525890", Toast.LENGTH_SHORT).show()

                                // Open dialer directly
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
                                        .size(34.dp)
                                        .background(SafeGreen.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = SafeGreen,
                                        modifier = Modifier.size(19.dp)
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
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PureWhite,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Quick Action Button (Dial & Copy)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SafeGreen.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, SafeGreen.copy(alpha = 0.7f))
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
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Email Reference
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

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Subtle, clean skip button (NO line or progress bar)
            Surface(
                shape = RoundedCornerShape(25.dp),
                color = Color(0xFF091F38),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                modifier = Modifier
                    .graphicsLayer { alpha = footerAlpha }
                    .clickable { onFinishSplash() }
                    .testTag("skip_splash_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الدخول الآن ⚡",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ExecutivePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF061424),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF1E88E5).copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
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
