package com.example.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser

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
