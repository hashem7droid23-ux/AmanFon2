package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YemenGold
import kotlinx.coroutines.delay
import kotlin.random.Random

private val CyberCyan = Color(0xFF00E5FF)
private val DeepNavy = Color(0xFF030A16)

private data class SplashParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float
)

/** easeOutBack: gives the emblem a premium "settle" overshoot. */
private fun easeOutBack(p: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = p - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

/**
 * Motion graphics splash for "أمان فون" (v2)
 * Timeline (GPU-only, graphicsLayer + Canvas, no relayout):
 * 1. Starfield particles + ambient aurora
 * 2. Shield emblem 3D flip-in with overshoot, rotating gold/cyan orbit, radar ripples and scan beam
 * 3. Title reveal + gold underline sweep + typewriter tagline
 * 4. Mission card and staggered feature pills
 * 5. Developer card (م. هاشم القديمي - 714525890) + enter button
 */
@Composable
fun MotionSplashScreen(
    onFinishSplash: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var finished by remember { mutableStateOf(false) }
    val finish: () -> Unit = {
        if (!finished) {
            finished = true
            onFinishSplash()
        }
    }

    val master = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "SplashLoop")
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "Orbit"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "Pulse"
    )
    val scan by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Scan"
    )
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "Drift"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Glow"
    )

    val particles = remember {
        val rnd = Random(714525890)
        List(42) {
            SplashParticle(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                radius = 0.6f + rnd.nextFloat() * 1.8f,
                speed = 0.25f + rnd.nextFloat() * 0.75f,
                phase = rnd.nextFloat()
            )
        }
    }

    LaunchedEffect(Unit) {
        master.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2200,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        )
        delay(2600)
        finish()
    }

    val t = master.value
    fun phase(start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

    val emblemP = phase(0.00f, 0.40f)
    val emblemScale = 0.35f + 0.65f * easeOutBack(emblemP)
    val ringP = phase(0.15f, 0.50f)
    val titleP = phase(0.25f, 0.55f)
    val underlineP = phase(0.40f, 0.65f)
    val taglineP = phase(0.45f, 0.75f)
    val missionP = phase(0.50f, 0.80f)
    val devP = phase(0.62f, 0.92f)
    val footerP = phase(0.80f, 1.00f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF020611), Color(0xFF071A33), Color(0xFF0A2140), DeepNavy)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { finish() }
            .testTag("motion_splash_screen")
    ) {
        // Ambient layer: aurora + floating particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val auroraCenter = Offset(size.width * 0.5f, size.height * 0.2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = 0.16f * glow),
                        Color(0xFF0D47A1).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = auroraCenter,
                    radius = size.width * 0.75f
                ),
                center = auroraCenter,
                radius = size.width * 0.75f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(YemenGold.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.95f),
                    radius = size.width * 0.6f
                ),
                center = Offset(size.width * 0.85f, size.height * 0.95f),
                radius = size.width * 0.6f
            )
            particles.forEach { p ->
                var y = (p.y - drift * p.speed) % 1f
                if (y < 0f) y += 1f
                val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * kotlin.math.sin((drift * 40f + p.phase * 6.28f)))
                drawCircle(
                    color = (if (p.phase > 0.8f) YemenGold else CyberCyan).copy(alpha = 0.35f * twinkle),
                    radius = p.radius * density,
                    center = Offset(p.x * size.width, y * size.height)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(176.dp)
            ) {
                // Radar ripples
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = emblemP }
                ) {
                    val maxR = size.minDimension / 2f
                    for (i in 0 until 3) {
                        val p = (pulse + i / 3f) % 1f
                        drawCircle(
                            color = CyberCyan.copy(alpha = (1f - p) * 0.35f),
                            radius = maxR * (0.50f + p * 0.50f),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }

                // Rotating orbit ring with tick marks
                Canvas(
                    modifier = Modifier
                        .size(150.dp)
                        .rotate(orbit)
                        .graphicsLayer {
                            alpha = ringP
                            scaleX = 0.8f + 0.2f * ringP
                            scaleY = 0.8f + 0.2f * ringP
                        }
                ) {
                    val stroke = 2.4.dp.toPx()
                    val inset = stroke
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    val topLeft = Offset(inset, inset)
                    drawArc(
                        color = YemenGold,
                        startAngle = 0f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = CyberCyan.copy(alpha = 0.85f),
                        startAngle = 180f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    val tickInset = 9.dp.toPx()
                    val tickSize = Size(size.width - tickInset * 2, size.height - tickInset * 2)
                    for (i in 0 until 36) {
                        drawArc(
                            color = PureWhite.copy(alpha = if (i % 3 == 0) 0.35f else 0.12f),
                            startAngle = i * 10f,
                            sweepAngle = 1.6f,
                            useCenter = false,
                            topLeft = Offset(tickInset, tickInset),
                            size = tickSize,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                // Shield emblem (same vector as the launcher icon) with 3D flip-in
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "شعار أمان فون",
                    modifier = Modifier
                        .requiredSize(220.dp)
                        .graphicsLayer {
                            alpha = emblemP
                            scaleX = emblemScale
                            scaleY = emblemScale
                            rotationY = (1f - emblemP) * 90f
                            cameraDistance = 14f * density
                        }
                )

                // Security scan beam across the shield
                Canvas(
                    modifier = Modifier
                        .size(width = 76.dp, height = 104.dp)
                        .graphicsLayer { alpha = if (emblemP >= 1f) 0.9f else 0f }
                ) {
                    val y = size.height * (0.12f + 0.76f * scan)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, CyberCyan.copy(alpha = 0.18f), Color.Transparent),
                            startY = y - 14.dp.toPx(),
                            endY = y + 14.dp.toPx()
                        ),
                        topLeft = Offset(0f, y - 14.dp.toPx()),
                        size = Size(size.width, 28.dp.toPx())
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, CyberCyan, Color.Transparent)
                        ),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Title + underline + tagline
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "أمان",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite,
                        modifier = Modifier.graphicsLayer {
                            alpha = titleP
                            translationX = (1f - titleP) * 60f
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "فون",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = YemenGold,
                        modifier = Modifier.graphicsLayer {
                            alpha = titleP
                            translationX = -(1f - titleP) * 60f
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(underlineP.coerceAtLeast(0.001f))
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(listOf(Color.Transparent, YemenGold, CyberCyan, YemenGold, Color.Transparent)),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val tagline = "AMAN PHONE • REPUBLIC OF YEMEN"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0D253F),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                    modifier = Modifier.graphicsLayer { alpha = if (taglineP > 0f) 1f else 0f }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tagline.take((tagline.length * taglineP).toInt()).padEnd(tagline.length, ' '),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9),
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Mission card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC081B30)),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF1976D2).copy(alpha = 0.5f),
                            CyberCyan.copy(alpha = 0.45f * glow),
                            Color(0xFF1976D2).copy(alpha = 0.5f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = missionP
                        translationY = (1f - missionP) * 60f
                        scaleX = 0.94f + 0.06f * missionP
                        scaleY = 0.94f + 0.06f * missionP
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
                        color = PureWhite.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val p1 = phase(0.62f, 0.78f)
                        val p2 = phase(0.68f, 0.84f)
                        val p3 = phase(0.74f, 0.90f)
                        SplashPill(Icons.Default.QrCodeScanner, "فحص الـ IMEI", p1)
                        SplashPill(Icons.Default.PhoneIphone, "بلاغات فورية", p2)
                        SplashPill(Icons.Default.CheckCircle, "تعاميم المحلات", p3)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Developer card (same info as before)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE60C223A)),
                border = BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(YemenGold, CyberCyan.copy(alpha = glow), YemenGold))
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = devP
                        translationY = (1f - devP) * 70f
                    }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = YemenGold.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, YemenGold.copy(alpha = 0.6f))
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

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF061424),
                        border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Developer Phone", "714525890"))
                                Toast.makeText(context, "تم نسخ رقم المطور: 714525890", Toast.LENGTH_SHORT).show()
                                try {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:714525890")))
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

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SafeGreen.copy(alpha = 0.25f),
                                border = BorderStroke(0.8.dp, SafeGreen.copy(alpha = 0.7f))
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

            // 5. Enter button with breathing glow
            Surface(
                shape = RoundedCornerShape(25.dp),
                color = Color(0xFF091F38),
                border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(YemenGold, CyberCyan.copy(alpha = glow), YemenGold))),
                modifier = Modifier
                    .graphicsLayer {
                        alpha = footerP
                        scaleX = 0.9f + 0.1f * footerP
                        scaleY = 0.9f + 0.1f * footerP
                    }
                    .clickable { finish() }
                    .testTag("skip_splash_button")
            ) {
                Text(
                    text = "الدخول الآن ⚡",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SplashPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    progress: Float
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF061424),
        border = BorderStroke(0.8.dp, CyberCyan.copy(alpha = 0.35f)),
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 24f
            scaleX = 0.85f + 0.15f * progress
            scaleY = 0.85f + 0.15f * progress
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberCyan,
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
