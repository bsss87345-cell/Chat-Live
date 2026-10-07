package com.example.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * خدمة الهوية — **Firebase Authentication (SDK الرسمي)**.
 *
 * لماذا تحوّلنا من REST إلى SDK: مكتبة Firestore تحتاج **جلسة مصادقة حقيقية**
 * داخل التطبيق لتُرسل رمز الهوية مع كل طلب. الـSDK يدير الرمز وتجديده تلقائياً،
 * ويقرأ الإعدادات من `google-services.json` (الذي يكتبه الـCI من السرّ).
 *
 * القيم التي يقرأها الـSDK تأتي من `app/google-services.json` — **لا مفاتيح في الكود**.
 * ولو غاب الملف (بناء محلي بلا سرّ): [isConfigured] = false ⇒ التطبيق يعمل بسلوكه
 * المحلي تماماً بلا أي اتصال شبكة (نفس السلوك قبل الربط).
 *
 * ⚠️ لا تُستدعى هذه الدوال من الخيط الرئيسي — النداء يحجب (blocking) بقصد
 * ليُستدعى من `withContext(Dispatchers.IO)`.
 */
object AuthService {

    /** عمر رمز الهوية الافتراضي (يُجدَّد تلقائياً بـSDK) */
    private const val TOKEN_LIFETIME_SECONDS = 3600L

    /** جاهزية الخدمة: يوجد تطبيق Firebase مهيّأ (google-services.json موجود). */
    val isConfigured: Boolean get() = authOrNull() != null

    /** نتيجة عملية الهوية — بلا استثناءات تصل للواجهة. */
    sealed interface Result {
        data class Ok(val uid: String, val idToken: String, val refreshToken: String, val expiresIn: Long) : Result
        data class Error(val message: String) : Result
        data object NotConfigured : Result
    }

    /**
     * دخول إن وُجد الحساب، وإنشاء حساب إن لم يُوجد (بلا خطوتين للمستخدم).
     * يرجع [Result.NotConfigured] فوراً لو لم يكن هناك مشروع مهيّأ ⇒ صفر اتصال شبكة.
     */
    fun registerOrLogin(email: String, password: String): Result {
        val auth = authOrNull() ?: return Result.NotConfigured

        val mail = email.trim().lowercase()
        val inputProblem = AuthValidation.emailProblem(mail) ?: AuthValidation.passwordProblem(password)
        if (inputProblem != null) return Result.Error(inputProblem)

        // 1) محاولة الدخول
        val signIn = authCall { auth.signInWithEmailAndPassword(mail, password) }
        if (signIn is Result.Ok) return signIn
        if (!isMissingAccount((signIn as? Result.Error)?.message.orEmpty())) return signIn

        // 2) الحساب غير موجود ⇒ إنشاء
        val signUp = authCall { auth.createUserWithEmailAndPassword(mail, password) }
        if (signUp is Result.Ok) return signUp

        // 3) تسابق نادر: أُنشئ الحساب من مكان آخر ⇒ نعيد محاولة الدخول
        if ((signUp as? Result.Error)?.message?.contains("مسجّل بالفعل") == true) {
            return authCall { auth.signInWithEmailAndPassword(mail, password) }
        }
        return signUp
    }

    /** يعيد تعيين كلمة السر عبر البريد (Firebase يرسل الرابط بنفسه). */
    fun sendPasswordReset(email: String): Result {
        val auth = authOrNull() ?: return Result.NotConfigured
        val mail = email.trim().lowercase()
        AuthValidation.emailProblem(mail)?.let { return Result.Error(it) }
        return try {
            Tasks.await(auth.sendPasswordResetEmail(mail))
            Result.Ok("", "", "", 0)
        } catch (e: Exception) {
            Result.Error(humanError(e))
        }
    }

    /**
     * يتجدّد رمز الجلسة. الـSDK يدير التجديد بنفسه؛ هذه الدالة تجبره على إصدار رمز جديد.
     * (الوسيط `refreshToken` من النظام القديم — لم يعد مستخدماً، يُقبل للتوافق.)
     */
    fun refreshSession(refreshToken: String): Result {
        val auth = authOrNull() ?: return Result.NotConfigured
        val user = auth.currentUser ?: return Result.Error("لا توجد جلسة فعّالة — سجّل الدخول من جديد.")
        return try {
            val token = Tasks.await(user.getIdToken(true)).token.orEmpty()
            if (token.isBlank()) Result.Error("تعذّر تحديث الجلسة — جرّب لاحقاً.")
            else Result.Ok(user.uid, token, "", TOKEN_LIFETIME_SECONDS)
        } catch (e: Exception) {
            Result.Error(humanError(e))
        }
    }
    // ══════════════ دوال المصادقة الموسّعة (تسجيل دخول · حساب جديد · تأكيد بريد) ══════════════
    // أُضيفت لأن الشاشة تحتاج: دخولاً بلا إنشاء تلقائي · إنشاءً منفصلاً · تأكيد بريد حقيقي برسالة Gmail.

    /** تسجيل الدخول بحساب **موجود** فقط (بلا إنشاء تلقائي لحساب جديد). */
    fun signIn(email: String, password: String): Result {
        val auth = authOrNull() ?: return Result.NotConfigured
        val mail = email.trim().lowercase()
        val inputProblem = AuthValidation.emailProblem(mail) ?: AuthValidation.passwordProblem(password)
        if (inputProblem != null) return Result.Error(inputProblem)
        return authCall { auth.signInWithEmailAndPassword(mail, password) }
    }

    /** إنشاء حساب جديد فقط (يفشل لو البريد مستخدم بالفعل). */
    fun signUp(email: String, password: String): Result {
        val auth = authOrNull() ?: return Result.NotConfigured
        val mail = email.trim().lowercase()
        val inputProblem = AuthValidation.emailProblem(mail) ?: AuthValidation.passwordProblem(password)
        if (inputProblem != null) return Result.Error(inputProblem)
        return authCall { auth.createUserWithEmailAndPassword(mail, password) }
    }

    /** معرّف الحساب الحالي من Firebase، أو null لو لا جلسة. */
    fun currentUid(): String? = try {
        authOrNull()?.currentUser?.uid
    } catch (e: Exception) {
        null
    }

    /** اسم العرض المحفوظ للحساب الحالي (يُضبط عند إنشاء الحساب). */
    fun displayNameOfCurrentUser(): String = try {
        authOrNull()?.currentUser?.displayName.orEmpty()
    } catch (e: Exception) {
        ""
    }

    /** يضبط اسم العرض للحساب الحالي (يظهر في رسائل Firebase ولوحة التحكم). */
    fun setDisplayName(name: String): Boolean {
        val user = authOrNull()?.currentUser ?: return false
        val clean = name.trim().take(60)
        if (clean.isBlank()) return false
        return try {
            val request = UserProfileChangeRequest.Builder().setDisplayName(clean).build()
            Tasks.await(user.updateProfile(request))
            true
        } catch (e: Exception) {
            false
        }
    }

    /** يرسل **رابط تأكيد البريد** إلى بريد الحساب الحالي (رسالة من Firebase تصل إلى Gmail). */
    fun sendVerificationEmail(): Boolean {
        val user = authOrNull()?.currentUser ?: return false
        return try {
            Tasks.await(user.sendEmailVerification())
            true
        } catch (e: Exception) {
            false
        }
    }

    // ────────────────────────── التنفيذ ──────────────────────────

    /** يجلب نسخة FirebaseAuth أو null لو المشروع غير مهيّأ (بلا google-services.json). */
    private fun authOrNull(): FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    /** ينفّذ نداء مصادقة ويعيد [Result] — بلا استثناءات تصل للمستدعي. */
    private fun authCall(block: () -> com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>): Result =
        try {
            val user: FirebaseUser? = Tasks.await(block()).user
            if (user == null) {
                Result.Error("لم يرجع الخادم حساباً صالحاً.")
            } else {
                val token = Tasks.await(user.getIdToken(false)).token.orEmpty()
                Result.Ok(user.uid, token, "", TOKEN_LIFETIME_SECONDS)
            }
        } catch (e: Exception) {
            Result.Error(humanError(e))
        }

    /** يستخرج رمز الخطأ من سلسلة الاستثناءات (Tasks.await يلفّ الاستثناء الأصلي). */
    private fun errorCodeOf(e: Throwable): String {
        var current: Throwable? = e
        while (current != null) {
            if (current is FirebaseAuthException) return current.errorCode
            current = current.cause
        }
        return e.message.orEmpty()
    }

    /** يحوّل رموز خطأ Firebase لرسائل عربية مفهومة. */
    private fun humanError(e: Throwable): String {
        val code = errorCodeOf(e)
        return when {
            code.contains("ERROR_NETWORK_REQUEST_FAILED") ||
                code.contains("Unable to resolve host") ||
                code.contains("Unable to resolve host") -> "تعذّر الاتصال بالخدمة — تحقق من الإنترنت."
            code.contains("ERROR_EMAIL_ALREADY_IN_USE") -> "هذا البريد الإلكتروني مسجّل بالفعل."
            code.contains("ERROR_USER_NOT_FOUND") -> "لا يوجد حساب بهذا البريد."
            code.contains("ERROR_WRONG_PASSWORD") ||
                code.contains("ERROR_INVALID_CREDENTIAL") ||
                code.contains("ERROR_INVALID_LOGIN_CREDENTIALS") ->
                "بيانات الدخول غير صحيحة، أو لا يوجد حساب بهذه البيانات بعد."
            code.contains("ERROR_WEAK_PASSWORD") -> "كلمة السر ضعيفة — اختر كلمة أقوى."
            code.contains("ERROR_INVALID_EMAIL") -> "يرجى إدخال بريد إلكتروني صحيح."
            code.contains("ERROR_TOO_MANY_REQUESTS") -> "محاولات كثيرة — جرّب بعد قليل."
            code.contains("ERROR_OPERATION_NOT_ALLOWED") -> "تسجيل الدخول بالبريد غير مفعّل في إعدادات المشروع."
            code.contains("ERROR_USER_DISABLED") -> "هذا الحساب معطَّل — راجع الدعم."
            code.contains("API key not valid") || code.contains("API_KEY_INVALID") -> "مفتاح المشروع غير صالح."
            code.contains("PERMISSION_DENIED") -> "الخدمة مرفوضة — راجع إعدادات المشروع."
            else -> "تعذّر إكمال العملية. جرّب مرة أخرى."
        }
    }

    /** الرسائل التي تعني «الحساب غير موجود بعد» ⇒ ننتقل لإنشائه. */
    private fun isMissingAccount(message: String): Boolean =
        message.contains("لا يوجد حساب") || message.contains("بيانات الدخول غير صحيحة")
}
