package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.remote.FirebaseAuthManager
import com.example.ui.components.YemenFlagPill
import com.example.ui.theme.AlertRed
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import com.example.util.IntentHelper

enum class AuthTab {
    GOOGLE,
    EMAIL,
    PHONE
}

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

    // Email tab fields
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Phone tab fields
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }

    // Slow-motion animation states
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    // Slow pulsing aura for security shield
    val infiniteTransition = rememberInfiniteTransition(label = "RadarAura")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraAlpha"
    )

    // Staggered entrance animations
    val headerAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "HeaderAlpha"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, delayMillis = 250, easing = FastOutSlowInEasing),
        label = "ContentAlpha"
    )
    val footerAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "FooterAlpha"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Navy900
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Slow-Motion Header with Animated Security Shield Emblem
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(headerAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    // Outer glowing pulsing ring
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        WarningAmber.copy(alpha = auraAlpha),
                                        Navy700.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Middle navy badge
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Navy800)
                            .border(2.dp, WarningAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "شعار أمان فون",
                            tint = WarningAmber,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "أمان فون",
                        color = PureWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    YemenFlagPill()
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Aman Phone • المنظومة الوطنية لتتبع وحماية الهواتف",
                    color = WarningAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "فحص فوري لأرقام IMEI • بلاغات عاجلة لجميع المحلات • حماية مجتمعية متكاملة في اليمن",
                    color = PureWhite.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 2. Interactive Sign-In Container Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Navy800),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, Navy700),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "تسجيل الدخول إلى حسابك",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )

                    // Navigation Tabs with Icons: Google / Email / Phone
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedTab == AuthTab.GOOGLE,
                            onClick = { selectedTab = AuthTab.GOOGLE },
                            leadingIcon = {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = { Text("Google", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningAmber,
                                selectedLabelColor = Navy900,
                                containerColor = Navy700.copy(alpha = 0.6f),
                                labelColor = PureWhite
                            ),
                            modifier = Modifier.weight(1f).testTag("tab_auth_google")
                        )
                        FilterChip(
                            selected = selectedTab == AuthTab.EMAIL,
                            onClick = { selectedTab = AuthTab.EMAIL },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (selectedTab == AuthTab.EMAIL) Navy900 else WarningAmber
                                )
                            },
                            label = { Text("البريد", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningAmber,
                                selectedLabelColor = Navy900,
                                containerColor = Navy700.copy(alpha = 0.6f),
                                labelColor = PureWhite
                            ),
                            modifier = Modifier.weight(1f).testTag("tab_auth_email")
                        )
                        FilterChip(
                            selected = selectedTab == AuthTab.PHONE,
                            onClick = { selectedTab = AuthTab.PHONE },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (selectedTab == AuthTab.PHONE) Navy900 else WarningAmber
                                )
                            },
                            label = { Text("الهاتف", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningAmber,
                                selectedLabelColor = Navy900,
                                containerColor = Navy700.copy(alpha = 0.6f),
                                labelColor = PureWhite
                            ),
                            modifier = Modifier.weight(1f).testTag("tab_auth_phone")
                        )
                    }

                    // Content of each selected tab with smooth transitions
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                        label = "AuthTabTransition"
                    ) { tab ->
                        when (tab) {
                            // 1. Google Tab
                            AuthTab.GOOGLE -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "المصادقة الآمنة السريعة عبر حساب Google المعتمد لمزامنة بلاغاتك وتنبيهاتك سحابياً في اليمن.",
                                        fontSize = 12.sp,
                                        color = PureWhite.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )

                                    Button(
                                        onClick = {
                                            isAuthenticating = true
                                            FirebaseAuthManager.onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onAuthSuccess = {
                                                    isAuthenticating = false
                                                    Toast.makeText(context, "مرحباً بك في أمان فون!", Toast.LENGTH_SHORT).show()
                                                    onLoginSuccess()
                                                },
                                                onAuthError = { err ->
                                                    isAuthenticating = false
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                },
                                                scope = coroutineScope,
                                                onAuthCancelled = { isAuthenticating = false }
                                            )
                                        },
                                        enabled = !isAuthenticating,
                                        shape = RoundedCornerShape(14.dp),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("google_login_primary_button")
                                    ) {
                                        if (isAuthenticating) {
                                            CircularProgressIndicator(
                                                color = Navy900,
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            androidx.compose.foundation.Image(
                                                painter = painterResource(id = R.drawable.ic_google_logo),
                                                contentDescription = "Google Logo",
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "تسجيل الدخول السريع عبر Google",
                                                color = Color(0xFF1F1F1F),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Email & Password Tab
                            AuthTab.EMAIL -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = { emailInput = it },
                                        label = { Text("البريد الإلكتروني", color = PureWhite.copy(alpha = 0.8f)) },
                                        placeholder = { Text("example@gmail.com") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = WarningAmber)
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarningAmber,
                                            unfocusedBorderColor = Navy700,
                                            focusedTextColor = PureWhite,
                                            unfocusedTextColor = PureWhite
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("email_input_field")
                                    )

                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        label = { Text("كلمة المرور", color = PureWhite.copy(alpha = 0.8f)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = WarningAmber)
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = PureWhite.copy(alpha = 0.7f)
                                                )
                                            }
                                        },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarningAmber,
                                            unfocusedBorderColor = Navy700,
                                            focusedTextColor = PureWhite,
                                            unfocusedTextColor = PureWhite
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("password_input_field")
                                    )

                                    Button(
                                        onClick = {
                                            if (emailInput.isBlank() || passwordInput.isBlank()) {
                                                Toast.makeText(context, "يرجى كتابة البريد وكلمة المرور", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "تم تسجيل الدخول بنجاح بحسابك!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("email_submit_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Login,
                                            contentDescription = null,
                                            tint = Navy900,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("دخول بالبريد الإلكتروني", fontWeight = FontWeight.Bold, color = Navy900)
                                    }
                                }
                            }

                            // 3. Phone Number Tab
                            AuthTab.PHONE -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = phoneInput,
                                        onValueChange = { phoneInput = it },
                                        label = { Text("رقم الهاتف اليمني (77/73/71/78/70)", color = PureWhite.copy(alpha = 0.8f)) },
                                        placeholder = { Text("77xxxxxxx") },
                                        prefix = { Text("+967  ", color = WarningAmber, fontWeight = FontWeight.Bold) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = WarningAmber)
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = WarningAmber,
                                            unfocusedBorderColor = Navy700,
                                            focusedTextColor = PureWhite,
                                            unfocusedTextColor = PureWhite
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("phone_input_field")
                                    )

                                    if (isOtpSent) {
                                        OutlinedTextField(
                                            value = otpInput,
                                            onValueChange = { otpInput = it },
                                            label = { Text("رمز التحقق (SMS)", color = PureWhite.copy(alpha = 0.8f)) },
                                            placeholder = { Text("أدخل رمز 4 أو 6 أرقام") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Key, contentDescription = null, tint = SuccessGreen)
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = SuccessGreen,
                                                unfocusedBorderColor = Navy700,
                                                focusedTextColor = PureWhite,
                                                unfocusedTextColor = PureWhite
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("otp_input_field")
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (phoneInput.length < 8) {
                                                Toast.makeText(context, "يرجى إدخال رقم هاتف يمني صحيح", Toast.LENGTH_SHORT).show()
                                            } else if (!isOtpSent) {
                                                isOtpSent = true
                                                Toast.makeText(context, "تم إرسال رمز التحقق إلى هاتفك", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "تم تأكيد رقم الهاتف بنجاح!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("phone_submit_button")
                                    ) {
                                        Icon(
                                            imageVector = if (!isOtpSent) Icons.Default.Send else Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PureWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (!isOtpSent) "إرسال رمز التحقق (SMS)" else "تأكيد الدخول برقم الهاتف",
                                            fontWeight = FontWeight.Bold,
                                            color = PureWhite
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Guest / Skip Mode as a sleek stylish card button
                    OutlinedButton(
                        onClick = onSkipGuest,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, WarningAmber.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("skip_guest_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تصفح أمان فون كزائر لتفقد البلاغات وفحص IMEI ←",
                            color = WarningAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Developer & Designer Rights Section (حقوق برمجة وتصميم التطبيق)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Navy800.copy(alpha = 0.9f)),
                border = BorderStroke(1.2.dp, WarningAmber.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(footerAlpha)
                    .testTag("developer_credits_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(WarningAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حقوق البرمجة والتصميم",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }

                    Text(
                        text = "تم تطوير وبرمجة وتصميم التطبيق بواسطة:",
                        fontSize = 11.sp,
                        color = PureWhite.copy(alpha = 0.75f)
                    )

                    Text(
                        text = "المهندس: هاشم القديمي",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PureWhite
                    )

                    Text(
                        text = "مطور برمجيات وأنظمة أمان الهواتف • الجمهورية اليمنية",
                        fontSize = 11.sp,
                        color = WarningAmber.copy(alpha = 0.9f)
                    )

                    // Contact Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Facebook Button
                        Button(
                            onClick = {
                                IntentHelper.openFacebookProfile(context, "HashemAlQodimy")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1.3f).testTag("developer_facebook_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ThumbUp,
                                contentDescription = "فيسبوك",
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("فيسبوك", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                                Text("HashemAlQodimy", fontSize = 8.sp, color = PureWhite.copy(alpha = 0.9f))
                            }
                        }

                        // WhatsApp Button
                        Button(
                            onClick = {
                                IntentHelper.contactDeveloperWhatsApp(context, "777450123")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("developer_whatsapp_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Chat,
                                contentDescription = "واتساب",
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        }

                        // Direct Call Button
                        Button(
                            onClick = {
                                IntentHelper.makeCall(context, "777450123")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy700),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(0.9f).testTag("developer_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "اتصال",
                                tint = PureWhite,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
