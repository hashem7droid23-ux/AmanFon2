package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.remote.FirebaseAuthManager
import com.example.ui.components.StatusBadge
import com.example.ui.components.shortBrand
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandBg
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandInk
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.YemenGold
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PhoneTrackerViewModel
import com.example.util.IntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: PhoneTrackerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val user by FirebaseAuthManager.currentUser.collectAsStateWithLifecycle()
    val isSuperAdmin by com.example.util.AdminManager.isSuperAdmin.collectAsStateWithLifecycle()
    val reports by viewModel.allReports.collectAsStateWithLifecycle(initialValue = emptyList())
    var confirmSignOut by remember { mutableStateOf(false) }

    val u = user
    val providers = u?.providerData?.map { it.providerId }.orEmpty()
    val isGoogle = "google.com" in providers
    val isPhone = "phone" in providers || (!u?.phoneNumber.isNullOrBlank() && u?.email.isNullOrBlank())

    val displayTitle = when {
        u == null -> "زائر"
        !u.displayName.isNullOrBlank() -> u.displayName!!
        isPhone -> u.phoneNumber ?: "رقم موثّق"
        else -> u.email ?: "مستخدم أمان فون"
    }
    val methodLabel = when {
        u == null -> "غير مسجّل الدخول"
        isGoogle -> "Google • ${u.email.orEmpty()}"
        isPhone -> "رقم الهاتف • موثّق برمز SMS"
        u.isEmailVerified -> "البريد الإلكتروني • مفعّل"
        else -> "البريد الإلكتروني • بانتظار التفعيل"
    }

    // "My reports": match by the verified phone number (last 9 digits)
    val myDigits = u?.phoneNumber?.filter { it.isDigit() }?.takeLast(9)
    val myReports = if (myDigits.isNullOrBlank() || myDigits.length < 9) emptyList()
    else reports.filter { it.primaryPhone.filter(Char::isDigit).takeLast(9) == myDigits }

    Scaffold(
        modifier = modifier,
        containerColor = BrandBg,
        topBar = {
            TopAppBar(
                title = { Text("حسابي", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBg)
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== Identity card =====
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF14365C), BrandSurface)))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(if (isGoogle) PureWhite else YemenGold.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            u == null -> Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = YemenGold, modifier = Modifier.size(36.dp))
                            isGoogle -> Image(painterResource(R.drawable.ic_google_logo), contentDescription = null, modifier = Modifier.size(34.dp))
                            isPhone -> Icon(Icons.Default.Phone, contentDescription = null, tint = YemenGold, modifier = Modifier.size(34.dp))
                            else -> Icon(Icons.Default.Email, contentDescription = null, tint = YemenGold, modifier = Modifier.size(34.dp))
                        }
                    }
                    Text(displayTitle, color = PureWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(methodLabel, color = TextSecondaryLight, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isSuperAdmin) {
                        Surface(shape = RoundedCornerShape(50), color = YemenGold.copy(alpha = 0.18f)) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = YemenGold, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("المشرف العام", color = YemenGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (u == null) {
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.LOGIN) },
                            colors = ButtonDefaults.buttonColors(containerColor = YemenGold, contentColor = BrandInk),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("profile_login_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تسجيل الدخول", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // ===== My reports =====
            if (u != null && myDigits != null && myDigits.length == 9) {
                Text("بلاغاتي (${myReports.size})", color = PureWhite, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                if (myReports.isEmpty()) {
                    Text("لم تنشر أي بلاغ برقمك بعد", color = TextSecondaryLight, fontSize = 12.sp)
                } else {
                    Surface(shape = RoundedCornerShape(20.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
                        Column {
                            myReports.forEachIndexed { i, r ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openReportDetails(r.id) }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${shortBrand(r.brand)} ${r.model}", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(r.governorate, color = TextSecondaryLight, fontSize = 11.sp)
                                    }
                                    StatusBadge(reportType = r.reportType, isRecovered = r.isRecovered)
                                }
                                if (i < myReports.lastIndex) HorizontalDivider(color = BrandBorder)
                            }
                        }
                    }
                }
            }

            // ===== Menu =====
            Surface(shape = RoundedCornerShape(20.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
                Column {
                    if (isSuperAdmin) {
                        MenuRow(Icons.Default.AdminPanelSettings, "لوحة تحكم المشرف", YemenGold, "profile_admin_row") {
                            viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD)
                        }
                        HorizontalDivider(color = BrandBorder)
                    }
                    MenuRow(Icons.AutoMirrored.Filled.MenuBook, "دليل الأمان وأرقام الطوارئ", BrandCyan, "profile_guide_row") {
                        viewModel.navigateTo(AppScreen.SHOPS_GUIDE)
                    }
                    HorizontalDivider(color = BrandBorder)
                    MenuRow(Icons.Default.SupportAgent, "تواصل مع المطور", SuccessGreen, "profile_support_row") {
                        IntentHelper.contactDeveloperWhatsApp(context, "714525890")
                    }
                    if (u != null) {
                        HorizontalDivider(color = BrandBorder)
                        MenuRow(Icons.AutoMirrored.Filled.Logout, "تسجيل الخروج", AlertRed, "profile_sign_out_row") {
                            confirmSignOut = true
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondaryLight, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("أمان فون • الإصدار ${BuildConfig.VERSION_NAME} • م. هاشم القديمي", color = TextSecondaryLight, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        if (confirmSignOut) {
            AlertDialog(
                onDismissRequest = { confirmSignOut = false },
                containerColor = BrandSurface,
                title = { Text("تسجيل الخروج؟", color = PureWhite, fontWeight = FontWeight.Bold) },
                text = { Text("يمكنك الدخول مجدداً في أي وقت.", color = TextSecondaryLight) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmSignOut = false
                        FirebaseAuthManager.signOut(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = {
                                Toast.makeText(context, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
                                viewModel.navigateToTab(AppScreen.FEED)
                            },
                            scope = scope
                        )
                    }) { Text("خروج", color = AlertRed, fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("إلغاء", color = PureWhite) } }
            )
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, tint: Color, tag: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(title, color = PureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("←", color = TextSecondaryLight, fontSize = 16.sp)
    }
}
