package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.remote.FirebaseAuthManager
import com.example.ui.components.YemenFlagPill
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.YemenGold
import com.example.util.IntentHelper
import kotlinx.coroutines.launch

enum class AuthTab {
    GOOGLE,
    EMAIL,
    PHONE
}

private val LoginCyan = Color(0xFF00E5FF)
private val GlassCard = Color(0xB30B1F38)
private val FieldBorder = Color(0xFF1E3A5F)
private const val DEVELOPER_PHONE = "714525890"

/**
 * Login screen v2: single full-height page (no scrolling needed on normal phones),
 * matching the new shield icon and the motion splash.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSkipGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedTab by remember { mutableStateOf(AuthTab.GOOGLE) }
    var isAuthenticating by remember { mutableStateOf(false) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val headerP by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "HeaderP"
    )
    val cardP by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "CardP"
    )
    val footerP by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "FooterP"
    )

    val infinite = rememberInfiniteTransition(label = "LoginLoop")
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)),
        label = "Orbit"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Glow"
    )

    val googleSignIn: () -> Unit = {
        isAuthenticating = true
        FirebaseAuthManager.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isAuthenticating = false
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                coroutineScope.launch {
                    val isMatch = com.example.util.AdminManager.verifyAndSetAdminState(
                        user = user,
                        firestoreService = com.example.data.remote.FirestorePhoneService(context)
                    )
                    if (isMatch) {
                        Toast.makeText(context, "👑 مرحباً بك يا باشمهندس هاشم! تم تفعيل وضع المشرف العام", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "مرحباً بك في أمان فون!", Toast.LENGTH_SHORT).show()
                    }
                    onLoginSuccess()
                }
            },
            onAuthError = { err ->
                isAuthenticating = false
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            },
            scope = coroutineScope,
            onAuthCancelled = { isAuthenticating = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF030A16)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF020611), Color(0xFF071A33), Color(0xFF0A2140), Color(0xFF030A16))
                    )
                )
        ) {
            // Ambient aurora
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height * 0.16f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(LoginCyan.copy(alpha = 0.14f * glow), Color(0xFF0D47A1).copy(alpha = 0.06f), Color.Transparent),
                        center = c,
                        radius = size.width * 0.8f
                    ),
                    center = c,
                    radius = size.width * 0.8f
                )
                val g = Offset(size.width * 0.1f, size.height)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(YemenGold.copy(alpha = 0.08f), Color.Transparent),
                        center = g,
                        radius = size.width * 0.7f
                    ),
                    center = g,
                    radius = size.width * 0.7f
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                val compact = maxHeight < 700.dp
                val emblemBox: Dp = if (compact) 92.dp else 116.dp
                val minHeight = maxHeight

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = minHeight)
                        .padding(horizontal = 20.dp, vertical = if (compact) 10.dp else 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // ---------- 1. Brand header ----------
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer {
                            alpha = headerP
                            translationY = (1f - headerP) * -40f
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(emblemBox)) {
                            Canvas(
                                modifier = Modifier
                                    .size(emblemBox)
                                    .rotate(orbit)
                            ) {
                                val s = 2.dp.toPx()
                                val arc = Size(size.width - s * 2, size.height - s * 2)
                                drawArc(
                                    color = YemenGold,
                                    startAngle = 0f,
                                    sweepAngle = 100f,
                                    useCenter = false,
                                    topLeft = Offset(s, s),
                                    size = arc,
                                    style = Stroke(width = s, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = LoginCyan.copy(alpha = 0.8f),
                                    startAngle = 180f,
                                    sweepAngle = 60f,
                                    useCenter = false,
                                    topLeft = Offset(s, s),
                                    size = arc,
                                    style = Stroke(width = s, cap = StrokeCap.Round)
                                )
                            }
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "شعار أمان فون",
                                modifier = Modifier.requiredSize(emblemBox * 1.55f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("أمان", color = PureWhite, fontSize = if (compact) 26.sp else 30.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فون", color = YemenGold, fontSize = if (compact) 26.sp else 30.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            YemenFlagPill()
                        }

                        Text(
                            text = "Aman Phone • المنظومة الوطنية لتتبع وحماية الهواتف",
                            color = YemenGold,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "فحص فوري لأرقام IMEI • بلاغات عاجلة لجميع المحلات • حماية مجتمعية متكاملة في اليمن",
                            color = PureWhite.copy(alpha = 0.7f),
                            fontSize = 10.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    // ---------- 2. Sign-in card ----------
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = GlassCard,
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(YemenGold.copy(alpha = 0.55f), LoginCyan.copy(alpha = 0.35f * glow), Color(0xFF1E3A5F))
                            )
                        ),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .graphicsLayer {
                                alpha = cardP
                                translationY = (1f - cardP) * 60f
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "مرحباً بك 👋",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureWhite
                                )
                                Text(
                                    text = "تسجيل الدخول إلى حسابك",
                                    fontSize = 12.sp,
                                    color = PureWhite.copy(alpha = 0.65f)
                                )
                            }

                            AuthSegmented(selected = selectedTab, onSelect = { selectedTab = it })

                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                                label = "AuthTab"
                            ) { tab ->
                                when (tab) {
                                    AuthTab.GOOGLE -> Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "المصادقة الآمنة السريعة عبر حساب Google المعتمد لمزامنة بلاغاتك وتنبيهاتك سحابياً في اليمن.",
                                            fontSize = 11.5.sp,
                                            color = PureWhite.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 17.sp
                                        )
                                        Button(
                                            onClick = googleSignIn,
                                            enabled = !isAuthenticating,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PureWhite,
                                                disabledContainerColor = PureWhite.copy(alpha = 0.8f)
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .testTag("google_login_primary_button")
                                        ) {
                                            if (isAuthenticating) {
                                                CircularProgressIndicator(
                                                    color = Color(0xFF071A33),
                                                    modifier = Modifier.size(22.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                                    contentDescription = "Google",
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "المتابعة باستخدام Google",
                                                    color = Color(0xFF1F1F1F),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }

                                    AuthTab.EMAIL -> Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LoginField(
                                            value = emailInput,
                                            onValueChange = { emailInput = it },
                                            label = "البريد الإلكتروني",
                                            icon = Icons.Default.Email,
                                            keyboardType = KeyboardType.Email,
                                            tag = "email_input_field"
                                        )
                                        LoginField(
                                            value = passwordInput,
                                            onValueChange = { passwordInput = it },
                                            label = "كلمة المرور",
                                            icon = Icons.Default.Lock,
                                            keyboardType = KeyboardType.Password,
                                            tag = "password_input_field",
                                            isPassword = true,
                                            passwordVisible = isPasswordVisible,
                                            onTogglePassword = { isPasswordVisible = !isPasswordVisible }
                                        )
                                        PrimaryAuthButton(
                                            text = "دخول بالبريد الإلكتروني",
                                            icon = Icons.AutoMirrored.Filled.Login,
                                            tag = "email_submit_button"
                                        ) {
                                            if (emailInput.isBlank() || passwordInput.isBlank()) {
                                                Toast.makeText(context, "يرجى كتابة البريد وكلمة المرور", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "تم تسجيل الدخول بنجاح بحسابك!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            }
                                        }
                                    }

                                    AuthTab.PHONE -> Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LoginField(
                                            value = phoneInput,
                                            onValueChange = { phoneInput = it },
                                            label = "رقم الهاتف اليمني (77/73/71/78/70)",
                                            icon = Icons.Default.Phone,
                                            keyboardType = KeyboardType.Phone,
                                            tag = "phone_input_field",
                                            prefix = "+967  "
                                        )
                                        if (isOtpSent) {
                                            LoginField(
                                                value = otpInput,
                                                onValueChange = { otpInput = it },
                                                label = "رمز التحقق (SMS)",
                                                icon = Icons.Default.Key,
                                                keyboardType = KeyboardType.Number,
                                                tag = "otp_input_field"
                                            )
                                        }
                                        PrimaryAuthButton(
                                            text = if (!isOtpSent) "إرسال رمز التحقق (SMS)" else "تأكيد الدخول برقم الهاتف",
                                            icon = if (!isOtpSent) Icons.AutoMirrored.Filled.Send else Icons.Default.Check,
                                            tag = "phone_submit_button"
                                        ) {
                                            if (phoneInput.length < 8) {
                                                Toast.makeText(context, "يرجى إدخال رقم هاتف يمني صحيح", Toast.LENGTH_SHORT).show()
                                            } else if (!isOtpSent) {
                                                isOtpSent = true
                                                Toast.makeText(context, "تم إرسال رمز التحقق إلى هاتفك", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "تم تأكيد رقم الهاتف بنجاح!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            }
                                        }
                                    }
                                }
                            }

                            // Guest mode
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSkipGuest() }
                                    .padding(vertical = 6.dp)
                                    .testTag("skip_guest_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = YemenGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تصفح كزائر لتفقد البلاغات وفحص IMEI ←",
                                    color = YemenGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ---------- 3. Developer credits (compact) ----------
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = footerP }
                            .testTag("developer_credits_card")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(1.dp)
                                    .background(Brush.horizontalGradient(listOf(Color.Transparent, YemenGold)))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = YemenGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "حقوق البرمجة والتصميم",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = YemenGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(1.dp)
                                    .background(Brush.horizontalGradient(listOf(YemenGold, Color.Transparent)))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "المهندس: هاشم القديمي",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = PureWhite
                        )
                        Text(
                            text = "مطور برمجيات وأنظمة أمان الهواتف • الجمهورية اليمنية",
                            fontSize = 10.5.sp,
                            color = PureWhite.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            ContactBubble(
                                icon = Icons.Outlined.ThumbUp,
                                label = "فيسبوك",
                                color = Color(0xFF1877F2),
                                tag = "developer_facebook_button"
                            ) { IntentHelper.openFacebookProfile(context, "HashemAlQodimy") }
                            ContactBubble(
                                icon = Icons.AutoMirrored.Outlined.Chat,
                                label = "واتساب",
                                color = SuccessGreen,
                                tag = "developer_whatsapp_button"
                            ) { IntentHelper.contactDeveloperWhatsApp(context, DEVELOPER_PHONE) }
                            ContactBubble(
                                icon = Icons.Default.Call,
                                label = "اتصال",
                                color = Color(0xFF1E88E5),
                                tag = "developer_call_button"
                            ) { IntentHelper.makeCall(context, DEVELOPER_PHONE) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthSegmented(selected: AuthTab, onSelect: (AuthTab) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF061424),
        border = BorderStroke(1.dp, FieldBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            SegmentItem(
                label = "Google",
                selected = selected == AuthTab.GOOGLE,
                tag = "tab_auth_google",
                leading = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            ) { onSelect(AuthTab.GOOGLE) }
            SegmentItem(
                label = "البريد",
                selected = selected == AuthTab.EMAIL,
                tag = "tab_auth_email",
                leading = {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = if (selected == AuthTab.EMAIL) Color(0xFF071A33) else YemenGold,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            ) { onSelect(AuthTab.EMAIL) }
            SegmentItem(
                label = "الهاتف",
                selected = selected == AuthTab.PHONE,
                tag = "tab_auth_phone",
                leading = {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (selected == AuthTab.PHONE) Color(0xFF071A33) else YemenGold,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            ) { onSelect(AuthTab.PHONE) }
        }
    }
}

@Composable
private fun SegmentItem(
    label: String,
    selected: Boolean,
    tag: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) YemenGold else Color.Transparent,
        animationSpec = tween(250),
        label = "SegBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color(0xFF071A33) else PureWhite.copy(alpha = 0.8f),
        animationSpec = tween(250),
        label = "SegFg"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 9.dp)
            .testTag(tag)
    ) {
        leading()
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType,
    tag: String,
    prefix: String? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = YemenGold) },
        prefix = if (prefix != null) {
            { Text(prefix, color = YemenGold, fontWeight = FontWeight.Bold) }
        } else null,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = PureWhite.copy(alpha = 0.7f)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = YemenGold,
            unfocusedBorderColor = FieldBorder,
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedLabelColor = YemenGold,
            unfocusedLabelColor = PureWhite.copy(alpha = 0.6f),
            cursorColor = YemenGold,
            focusedContainerColor = Color(0xFF061424),
            unfocusedContainerColor = Color(0xFF061424)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    )
}

@Composable
private fun PrimaryAuthButton(
    text: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(tag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFFE7A3), YemenGold, Color(0xFFB8860B))),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF071A33), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text, fontWeight = FontWeight.Black, color = Color(0xFF071A33), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ContactBubble(
    icon: ImageVector,
    label: String,
    color: Color,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(4.dp)
            .testTag(tag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
            ) {
                Icon(icon, contentDescription = label, tint = PureWhite, modifier = Modifier.size(17.dp))
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(label, fontSize = 10.sp, color = PureWhite.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
    }
}
