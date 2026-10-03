package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"

    private val _currentUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        Firebase.auth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
        }
        // Arabic SMS / emails from Firebase
        try {
            Firebase.auth.setLanguageCode("ar")
        } catch (_: Exception) {}
    }

    private fun getDefaultWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) {
            try {
                context.getString(resId).ifBlank { null }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    // ---------------------------------------------------------------------
    // Google
    // ---------------------------------------------------------------------

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = getDefaultWebClientId(context)
        if (clientId == null) {
            Log.w(TAG, "default_web_client_id not found")
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    private fun isUserCancel(e: Exception): Boolean {
        val m = e.message.orEmpty()
        return m.contains("cancelled by the user", true) || m.contains("canceled by the user", true)
    }

    private suspend fun finishGoogle(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            Firebase.auth.signInWithCredential(authCredential).await()
        } else {
            throw IllegalStateException("نوع بيانات الاعتماد غير متوافق")
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = getDefaultWebClientId(context)
        if (clientId == null) {
            onAuthError("إعدادات تسجيل الدخول عبر Google غير متوفرة في بيئة البناء الحالية")
            return
        }

        val activity = context as? Activity
        if (activity == null) {
            onAuthError("Context is not an Activity")
            return
        }

        val primary = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId = clientId).build())
            .build()
        // Fallback flow (bottom sheet) for devices where the button flow fails with reauth errors
        val fallback = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        scope.launch {
            try {
                val result = try {
                    credentialManager.getCredential(activity, primary)
                } catch (e: GetCredentialCancellationException) {
                    if (isUserCancel(e)) throw e
                    Log.w(TAG, "Primary Google flow failed (${e.message}), trying fallback")
                    credentialManager.getCredential(activity, fallback)
                }
                finishGoogle(result.credential)
                onAuthSuccess()
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled: ${e.message}", e)
                if (isUserCancel(e)) {
                    onAuthCancelled()
                } else {
                    // Typically "[16] Account reauth failed": the APK signature (SHA-1) is not
                    // registered for this package in the Firebase / Google Cloud project.
                    Toast.makeText(
                        activity,
                        "❌ رفض Google الدخول: بصمة التطبيق غير مسجّلة لمشروع Firebase.\nالتفاصيل: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    onAuthCancelled()
                }
            } catch (e: NoCredentialException) {
                Log.e(TAG, "No Google account", e)
                onAuthError("لا يوجد حساب Google على الجهاز. أضف حساباً من الإعدادات ثم حاول مجدداً")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                Toast.makeText(
                    activity,
                    "❌ فشل الدخول عبر Google: ${mapAuthError(e)}",
                    Toast.LENGTH_LONG
                ).show()
                onAuthCancelled()
            }
        }
    }

    // ---------------------------------------------------------------------
    // Email & password (Firebase Auth)
    // ---------------------------------------------------------------------

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /** Returns null when valid, otherwise an Arabic error message. */
    fun validateEmailForm(email: String, password: String, isRegister: Boolean): String? {
        val e = email.trim()
        if (e.isBlank() || password.isBlank()) return "يرجى كتابة البريد وكلمة المرور"
        if (!EMAIL_REGEX.matches(e)) return "صيغة البريد الإلكتروني غير صحيحة"
        if (isRegister && password.length < 6) return "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
        return null
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = Firebase.auth.signInWithEmailAndPassword(email.trim(), password).await()
        result.user ?: throw IllegalStateException("no user")
    }.recoverCatching { throw Exception(mapAuthError(it)) }

    suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = Firebase.auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("no user")
        try {
            user.sendEmailVerification().await()
        } catch (e: Exception) {
            Log.w(TAG, "Verification email not sent: ${e.message}")
        }
        user
    }.recoverCatching { throw Exception(mapAuthError(it)) }

    /** Re-sends the activation link to the signed-in email user. */
    suspend fun resendVerificationEmail(): Result<Unit> = runCatching {
        val user = Firebase.auth.currentUser ?: throw IllegalStateException("سجّل الدخول أولاً")
        user.reload().await()
        if (user.isEmailVerified) return@runCatching Unit
        user.sendEmailVerification().await()
        Unit
    }.recoverCatching { throw Exception(mapAuthError(it)) }

    /**
     * Reloads the user from Firebase (picks up a clicked activation link) and refreshes the ID token
     * so Firestore rules see email_verified = true. Returns true when the email is verified.
     */
    suspend fun refreshUser(): Boolean {
        val user = Firebase.auth.currentUser ?: return false
        return try {
            val wasVerified = user.isEmailVerified
            user.reload().await()
            val fresh = Firebase.auth.currentUser ?: return false
            if (fresh.isEmailVerified && !wasVerified) {
                fresh.getIdToken(true).await()
                // Same FirebaseUser instance: force observers (UI cards, admin checks) to re-read it
                _currentUser.value = null
                _currentUser.value = fresh
            }
            fresh.isEmailVerified
        } catch (e: Exception) {
            Log.w(TAG, "refreshUser failed: ${e.message}")
            false
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        Firebase.auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }.recoverCatching { throw Exception(mapAuthError(it)) }

    // ---------------------------------------------------------------------
    // Phone number + SMS OTP (Firebase Auth)
    // ---------------------------------------------------------------------

    /**
     * Normalizes Yemeni mobile numbers to E.164 (+9677XXXXXXXX).
     * Accepts: 7XXXXXXXX, 07XXXXXXXX, 9677XXXXXXXX, +9677XXXXXXXX, 009677XXXXXXXX
     */
    fun normalizeYemeniPhone(raw: String): String? {
        var digits = raw.filter { it.isDigit() }
        if (digits.startsWith("00967")) digits = digits.removePrefix("00967")
        else if (digits.startsWith("967")) digits = digits.removePrefix("967")
        if (digits.startsWith("0")) digits = digits.removePrefix("0")
        return if (Regex("^7[01378]\\d{7}$").matches(digits)) "+967$digits" else null
    }

    fun startPhoneVerification(
        activity: Activity,
        phoneE164: String,
        resendToken: PhoneAuthProvider.ForceResendingToken?,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onAutoVerified: () -> Unit,
        onError: (String) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Instant verification / SMS auto-retrieval
                Firebase.auth.signInWithCredential(credential)
                    .addOnSuccessListener { onAutoVerified() }
                    .addOnFailureListener { onError(mapAuthError(it)) }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(TAG, "Phone verification failed", e)
                onError(mapAuthError(e))
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                onCodeSent(verificationId, token)
            }
        }

        val builder = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (resendToken != null) builder.setForceResendingToken(resendToken)

        try {
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        } catch (e: Exception) {
            onError(mapAuthError(e))
        }
    }

    suspend fun verifyPhoneCode(verificationId: String, code: String): Result<FirebaseUser> = runCatching {
        val credential = PhoneAuthProvider.getCredential(verificationId, code.trim())
        val result = Firebase.auth.signInWithCredential(credential).await()
        result.user ?: throw IllegalStateException("no user")
    }.recoverCatching { throw Exception(mapAuthError(it)) }

    // ---------------------------------------------------------------------

    fun mapAuthError(e: Throwable): String {
        val code = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode ?: ""
        return when {
            code == "ERROR_OPERATION_NOT_ALLOWED" ||
                e.message?.contains("sign-in provider is disabled", ignoreCase = true) == true ->
                "طريقة الدخول هذه غير مفعّلة حالياً في النظام"
            e is FirebaseNetworkException -> "لا يوجد اتصال بالإنترنت، تحقق من الشبكة وحاول مجدداً"
            e is FirebaseTooManyRequestsException -> "محاولات كثيرة، يرجى الانتظار قليلاً ثم المحاولة"
            e is FirebaseAuthWeakPasswordException -> "كلمة المرور ضعيفة، استخدم 6 أحرف على الأقل"
            e is FirebaseAuthUserCollisionException -> "هذا البريد مسجل مسبقاً، سجّل الدخول بدلاً من إنشاء حساب"
            e is FirebaseAuthInvalidUserException -> when (code) {
                "ERROR_USER_DISABLED" -> "تم إيقاف هذا الحساب من قبل الإدارة"
                else -> "لا يوجد حساب بهذا البريد، أنشئ حساباً جديداً"
            }
            e is FirebaseAuthInvalidCredentialsException -> when (code) {
                "ERROR_INVALID_EMAIL" -> "صيغة البريد الإلكتروني غير صحيحة"
                "ERROR_INVALID_VERIFICATION_CODE" -> "رمز التحقق غير صحيح"
                "ERROR_SESSION_EXPIRED" -> "انتهت صلاحية الرمز، اطلب رمزاً جديداً"
                "ERROR_INVALID_PHONE_NUMBER" -> "رقم الهاتف غير صحيح"
                "ERROR_INVALID_CREDENTIAL" -> "بيانات Google مرفوضة من Firebase (تأكد من تفعيل مزوّد Google)"
                else -> "البريد أو كلمة المرور غير صحيحة"
            }
            e.message?.contains("BILLING_NOT_ENABLED", ignoreCase = true) == true ->
                "خدمة الرسائل غير مفعلة في مشروع Firebase حالياً"
            else -> e.localizedMessage ?: "حدث خطأ غير متوقع، حاول مجدداً"
        }
    }

    fun signOut(
        context: Context,
        credentialManager: CredentialManager,
        onSignOutComplete: () -> Unit,
        scope: CoroutineScope
    ) {
        Firebase.auth.signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state", e)
            } finally {
                onSignOutComplete()
            }
        }
    }
}
