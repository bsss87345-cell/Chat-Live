package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.data.AuthValidation
import com.example.model.AuthUserAccount
import com.example.model.generateUniqueUserId
import com.example.ui.theme.*

/**
 * خطوات إنشاء حساب جديد، كل خطوة تُعرض في شاشة مستقلة (بنمط تدفق إنستجرام):
 * الاسم -> البريد الإلكتروني -> كلمة السر -> رمز التأكيد
 */
private enum class SignupStep {
    NAME, EMAIL, PASSWORD, OTP
}

/**
 * شاشة تسجيل الدخول وإنشاء الحساب لتطبيق «Chat Live»:
 * - وضع تسجيل الدخول: شاشة واحدة (بريد/يوزر + كلمة سر).
 * - وضع إنشاء حساب: تدفق خطوات منفصلة (اسم -> بريد -> كلمة سر -> رمز تأكيد وهمي).
 * - التخزين مؤقت بالذاكرة فقط (بلا قاعدة بيانات حتى الآن).
 */
@Composable
fun AuthScreen(
    onAuthSuccess: (AuthUserAccount, String) -> Unit,
    serverLogin: (String, String, (Boolean, String?) -> Unit) -> Unit = { _, _, done -> done(false, "الخدمة غير مهيأة") },
    serverResetPassword: (String, (Boolean, String?) -> Unit) -> Unit = { _, done -> done(false, "الخدمة غير مهيأة") },
    serverSignup: (String, String, String, (Boolean, String?) -> Unit) -> Unit = { _, _, _, done -> done(false, "الخدمة غير مهيأة") },
    serverConfirmSignup: (String, String, String, Boolean, (Boolean, String?) -> Unit) -> Unit = { _, _, _, _, done -> done(false, "الخدمة غير مهيأة") },
    serverResendVerification: ((Boolean, String?) -> Unit) -> Unit = { done -> done(false, "الخدمة غير مهيأة") },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // وضع الشاشة الحالي: true = تسجيل الدخول, false = إنشاء حساب
    var isLoginMode by remember { mutableStateOf(true) }

    // قائمة الحسابات "المسجّلة" مؤقتاً بذاكرة الجلسة الحالية فقط
    val registeredAccounts = remember { mutableStateListOf<AuthUserAccount>() }

    // -------------------- حقول تسجيل الدخول --------------------
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // -------------------- حقول إنشاء حساب --------------------
    var signupStep by remember { mutableStateOf(SignupStep.NAME) }
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }
    var signupOtpInput by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loginBusy by remember { mutableStateOf(false) }
    var forgotOpen by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotStatus by remember { mutableStateOf<String?>(null) }
    var resetBusy by remember { mutableStateOf(false) }
    var signupBusy by remember { mutableStateOf(false) }

    fun resetSignupState() {
        signupStep = SignupStep.NAME
        signupName = ""
        signupEmail = ""
        signupPassword = ""
        signupOtpInput = ""
        generatedOtp = ""
        errorMessage = null
    }

    fun switchMode(toLogin: Boolean) {
        errorMessage = null
        isLoginMode = toLogin
        if (!toLogin) resetSignupState()
    }

    fun handleLogin() {
        errorMessage = null
        val identifier = loginIdentifier.trim()
                // فحص أدق: مُعرّف فارغ فقط (البريد/اسم المستخدم) + كلمة السر مطلوبة
        AuthValidation.loginIdentifierProblem(identifier)?.let {
            errorMessage = it
            return
        }
        if (loginPassword.isEmpty()) {
            errorMessage = "يرجى إدخال اسم المستخدم أو البريد الإلكتروني وكلمة السر."
            return
        }
                if (loginBusy) return
        loginBusy = true
        // ☁️ الدخول الحقيقي عبر Firebase — يعمل من أي جهاز بهذا الحساب
        //    (onAuthSuccess يُستدعى داخل الـViewModel عند النجاح ⇒ لا نستدعيه هنا)
        serverLogin(identifier, loginPassword) { ok, message ->
            loginBusy = false
            if (!ok) {
                errorMessage = message ?: "تعذّر تسجيل الدخول — تحقق من الاتصال وحاول مجدداً."
            } else if (message != null) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun goToNextSignupStep() {
        errorMessage = null
        when (signupStep) {
            SignupStep.NAME -> {
                                AuthValidation.nameProblem(signupName)?.let {
                    errorMessage = it
                    return
                                }
                signupStep = SignupStep.EMAIL
            }
            SignupStep.EMAIL -> {
                                // كان يقبل «@.» و«a@b.» — الآن فحص حقيقي (نطاق + امتداد حرفي)
                AuthValidation.emailProblem(signupEmail)?.let {
                    errorMessage = it
                    return
                }
                if (registeredAccounts.any { it.email.equals(signupEmail.trim(), ignoreCase = true) }) {
                    errorMessage = "هذا البريد الإلكتروني مسجّل بالفعل."
                    return
                }
                signupStep = SignupStep.PASSWORD
            }
                        SignupStep.PASSWORD -> {
                AuthValidation.passwordProblem(signupPassword)?.let {
                    errorMessage = it
                    return
                }
                if (signupBusy) return
                signupBusy = true
                // ☁️ إنشاء الحساب فعلياً في Firebase ثم إرسال رابط تأكيد البريد (رسالة Gmail)
                serverSignup(signupName.trim(), signupEmail.trim(), signupPassword) { ok, message ->
                    signupBusy = false
                    if (ok) {
                        signupStep = SignupStep.OTP
                    } else {
                        errorMessage = message ?: "تعذّر إنشاء الحساب — جرّب مرة أخرى."
                    }
                }
            }
                        SignupStep.OTP -> {
                if (signupBusy) return
                signupBusy = true
                // ☁️ فحص تأكيد البريد من الخادم؛ وعند النجاح يكتمل الدخول داخل الViewModel
                serverConfirmSignup(signupEmail.trim(), signupPassword, signupName.trim(), false) { ok, message ->
                    signupBusy = false
                    if (!ok) {
                        errorMessage = message ?: "لم نتحقق من بريدك بعد — افتح رسالة Gmail واضغط الرابط."
                    }
                }
            }
        }
    }

        /** «أكمل لاحقاً»: يكمل الدخول بلا انتظار تأكيد البريد. */
    fun skipVerification() {
        errorMessage = null
        if (signupBusy) return
        signupBusy = true
        serverConfirmSignup(signupEmail.trim(), signupPassword, signupName.trim(), true) { ok, message ->
            signupBusy = false
            if (!ok) errorMessage = message ?: "تعذّر إكمال التسجيل — جرّب مرة أخرى."
        }
    }

    /** يعيد إرسال رابط تأكيد البريد. */
    fun resendVerification() {
        serverResendVerification { ok, message ->
            Toast.makeText(
                context,
                message ?: if (ok) "أُرسل رابط التأكيد ✅" else "تعذّر الإرسال — تحقق من الاتصال.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun goToPreviousSignupStep() {
        errorMessage = null
        signupStep = when (signupStep) {
            SignupStep.NAME -> SignupStep.NAME
            SignupStep.EMAIL -> SignupStep.NAME
            SignupStep.PASSWORD -> SignupStep.EMAIL
            SignupStep.OTP -> SignupStep.PASSWORD
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MujtamaDarkBackground,
                        Color(0xFF1B1638),
                        Color(0xFF0F0C1E)
                    )
                )
            )
            .testTag(if (isLoginMode) "login_screen" else "signup_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // -------------------------------------------------------------
            // سهم علوي: رجوع لخطوة سابقة (وضع التسجيل فقط بعد الخطوة الأولى)
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                if (!isLoginMode && signupStep != SignupStep.NAME) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "رجوع",
                        tint = MujtamaDarkTextMuted,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable { goToPreviousSignupStep() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoginMode) {
                LoginContent(
                    identifier = loginIdentifier,
                    onIdentifierChange = { loginIdentifier = it },
                    password = loginPassword,
                    onPasswordChange = { loginPassword = it },
                    passwordVisible = loginPasswordVisible,
                    onTogglePasswordVisible = { loginPasswordVisible = !loginPasswordVisible },
                    errorMessage = errorMessage,
                    onDismissError = { errorMessage = null },
                    onLoginClick = { handleLogin() },
                    onSwitchToSignup = { switchMode(false) }
                )
            } else {
                SignupContent(
                    step = signupStep,
                    name = signupName,
                    onNameChange = { signupName = it },
                    email = signupEmail,
                    onEmailChange = { signupEmail = it },
                    password = signupPassword,
                    onPasswordChange = { signupPassword = it },
                    passwordVisible = signupPasswordVisible,
                    onTogglePasswordVisible = { signupPasswordVisible = !signupPasswordVisible },
                    otpInput = signupOtpInput,
                    onOtpInputChange = { signupOtpInput = it },
                    errorMessage = errorMessage,
                    onDismissError = { errorMessage = null },
                    onSkipVerification = { skipVerification() },
                    onResendVerification = { resendVerification() },
                    onNextClick = { goToNextSignupStep() },
                    onSwitchToLogin = { switchMode(true) }
                )
            }
        }
    }
}

@Composable
private fun LoginContent(
    identifier: String,
    onIdentifierChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onLoginClick: () -> Unit,
    onSwitchToSignup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        AppBrandHeader(modifier = Modifier.padding(bottom = 24.dp))

        AuthErrorBanner(message = errorMessage, onDismiss = onDismissError)

        AuthPillTextField(
            value = identifier,
            onValueChange = onIdentifierChange,
            placeholder = "اسم المستخدم أو البريد الإلكتروني",
            modifier = Modifier.testTag("login_identifier_field")
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuthPillTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "كلمة السر",
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisible = onTogglePasswordVisible,
            modifier = Modifier.testTag("login_password_field")
        )

        Spacer(modifier = Modifier.height(22.dp))

        AuthPrimaryButton(
            text = "تسجيل الدخول",
            onClick = onLoginClick,
            modifier = Modifier.testTag("login_button")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "هل نسيت كلمة السر؟",
            fontSize = 13.sp,
            color = MujtamaDarkTextMuted,
            modifier = Modifier.clickable { /* شكلي حالياً، غير مفعّل بعد */ }
        )

        Spacer(modifier = Modifier.height(28.dp))

        AuthSecondaryOutlinedButton(
            text = "إنشاء حساب جديد",
            onClick = onSwitchToSignup,
            modifier = Modifier.testTag("switch_to_signup_button")
        )

        Spacer(modifier = Modifier.height(24.dp))
        AuthPrivacyBadge()
    }
}
@Composable
private fun SignupContent(
    step: SignupStep,
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    otpInput: String,
    onOtpInputChange: (String) -> Unit,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onNextClick: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onSkipVerification: () -> Unit,
    onResendVerification: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val (title, description) = when (step) {
            SignupStep.NAME -> "ما اسمك؟" to "هذا هو الاسم الذي سيظهر لأصدقائك داخل التطبيق."
            SignupStep.EMAIL -> "ما بريدك الإلكتروني؟" to "سنستخدم هذا البريد لتسجيل الدخول ومساعدتك على تأمين حسابك."
            SignupStep.PASSWORD -> "إنشاء كلمة سر" to "يرجى إنشاء كلمة سر مكونة من 6 أحرف أو أرقام على الأقل، ويجب أن يتعذر على الآخرين تخمينها."
            SignupStep.OTP -> "تأكيد البريد الإلكتروني" to "أرسلنا رابط تأكيد إلى بريدك من Firebase. افتح Gmail واضغط الرابط، ثم اضغط الزر بالأسفل. لم تجد الرسالة؟ جرّب مجلد Spam أو اضغط «إعادة إرسال الرابط»."
        }

        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = description,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            color = MujtamaDarkTextMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthErrorBanner(message = errorMessage, onDismiss = onDismissError)

        when (step) {
            SignupStep.NAME -> AuthPillTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = "الاسم الكامل",
                modifier = Modifier.testTag("signup_name_field")
            )
            SignupStep.EMAIL -> AuthPillTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = "البريد الإلكتروني",
                keyboardType = KeyboardType.Email,
                modifier = Modifier.testTag("signup_email_field")
            )
            SignupStep.PASSWORD -> AuthPillTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "كلمة السر",
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePasswordVisible = onTogglePasswordVisible,
                modifier = Modifier.testTag("signup_password_field")
            )
                SignupStep.OTP -> Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📧", fontSize = 34.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "بانتظار تأكيد بريدك…",
                    fontSize = 13.5.sp,
                    color = MujtamaDarkTextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "إعادة إرسال الرابط",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MujtamaGold,
                    modifier = Modifier
                        .clickable { onResendVerification() }
                        .testTag("resend_verification_button")
                )
                        }
        }

        Spacer(modifier = Modifier.height(22.dp))

        AuthPrimaryButton(
            text = if (step == SignupStep.OTP) "تحققت من بريدي — أكمل" else "التالي",
            onClick = onNextClick,
            modifier = Modifier.testTag("signup_next_button")
        )

              if (step == SignupStep.OTP) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "أكمل لاحقاً بدون تأكيد",
                fontSize = 13.sp,
                color = MujtamaDarkTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSkipVerification() }
                    .testTag("skip_verification_button")
            )
        }  
        
        if (step == SignupStep.NAME) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "لديّ حساب بالفعل",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MujtamaGold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchToLogin() }
                    .testTag("switch_to_login_button")
            )
        }
    }
}
/**
 * حقل إدخال موحّد بحواف دائرية (Pill) يُستخدم في كل خطوات الدخول والتسجيل
 */
@Composable
private fun AuthPillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisible: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, color = MujtamaDarkTextMuted, fontSize = 14.sp) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword && onTogglePasswordVisible != null) {
            {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MujtamaDarkTextMuted,
                    modifier = Modifier.clickable { onTogglePasswordVisible() }
                )
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MujtamaPrimary,
            unfocusedBorderColor = MujtamaDarkTextMuted.copy(alpha = 0.35f),
            focusedContainerColor = MujtamaDarkSurface.copy(alpha = 0.5f),
            unfocusedContainerColor = MujtamaDarkSurface.copy(alpha = 0.35f),
            cursorColor = MujtamaPrimary,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    )
}

@Composable
private fun AuthPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MujtamaPrimary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AuthSecondaryOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MujtamaTeal),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, MujtamaTeal),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AuthErrorBanner(message: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF4A1010),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFF5252)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("auth_error_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF8A80),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message ?: "",
                    color = Color(0xFFFFEBEE),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕",
                    color = Color(0xFFFFCDD2),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthPrivacyBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MujtamaTeal,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = "بياناتك محمية ومشفرة وفق معايير الخصوصية والأمان",
            fontSize = 11.sp,
            color = MujtamaDarkTextMuted.copy(alpha = 0.7f)
        )
    }
}
