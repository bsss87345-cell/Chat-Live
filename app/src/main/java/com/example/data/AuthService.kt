package com.example.data

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * خدمة الهوية — **Firebase Authentication عبر REST (Identity Toolkit)** بلا SDK.
 *
 * لماذا REST بلا SDK: مكتبات Firebase معلّقة بالمستودع بالكامل، والـREST يغطي
 * ما نحتاجه فعلاً (تسجيل · دخول · رمز جلسة) بلا أي مكتبة جديدة ولا تعديل على
 * `libs.versions.toml` — ونفس مفاتيح الخادم (لا معرّفات من جهة العميل).
 *
 * ⚠️ الحالة: **جاهزة وغير مفعّلة**. ما دام [API_KEY] فارغاً:
 *   • [isConfigured] = false
 *   • و[registerOrLogin] ترجع [Result.NotConfigured] **بلا أي اتصال شبكة**
 * ⇒ التطبيق يشتغل بسلوكه المحلي الحالي بالضبط، بلا أي سلوك زائف.
 *
 * طريقة التفعيل (بعد إنشاء مشروع Firebase — Spark مجاني):
 *   1) Firebase Console ← Project Settings ← Web API Key
 *   2) Authentication ← Sign-in method ← Email/Password ← تفعيل
 *   3) ضع المفتاح في [API_KEY] **أو** مرّره من سرّ الـCI (لا يُرفع للمستودع)
 *
 * ⚠️ لا تُستدعى هذه الدوال من الخيط الرئيسي — النداء يحجب (blocking) بقصد
 * ليُستدعى من `withContext(Dispatchers.IO)`.
 */
object AuthService {

    /**
     * مفتاح الويب لمشروع Firebase.
     * ⚠️ اتركه فارغاً بالمستودع واستخدم سرّ البناء — المفتاح ليس سرّاً خطيراً
     * لكن إبقاؤه خارج المستودع يمنع ربط مشاريع مجهولة بالتطبيق.
     */
    const val API_KEY: String = ""

    private const val SIGN_UP = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key="
    private const val SIGN_IN = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="

    val isConfigured: Boolean get() = API_KEY.isNotBlank()

    /** نتيجة عملية الهوية — بلا استثناءات تصل للواجهة. */
    sealed interface Result {
        data class Ok(val uid: String, val idToken: String, val refreshToken: String, val expiresIn: Long) : Result
        data class Error(val message: String) : Result
        data object NotConfigured : Result
    }

    /**
     * دخول إن وُجد الحساب، وإنشاء حساب إن لم يُوجد (بلا خطوتين للمستخدم).
     * يرجع [Result.NotConfigured] فوراً لو ما فيه مفتاح ⇒ صفر اتصال شبكة.
     */
    fun registerOrLogin(email: String, password: String): Result {
        if (!isConfigured) return Result.NotConfigured

        val mail = email.trim().lowercase()
        val inputProblem = AuthValidation.emailProblem(mail) ?: AuthValidation.passwordProblem(password)
        if (inputProblem != null) return Result.Error(inputProblem)

        val body = JSONObject().apply {
            put("email", mail)
            put("password", password)
            put("returnSecureToken", true)
        }.toString()

        // 1) محاولة الدخول
        val signIn = request(SIGN_IN + API_KEY, body)
        if (signIn is Result.Ok || (signIn as? Result.Error)?.let { !isMissingAccount(it.message) } == true) {
            return signIn
        }

        // 2) الحساب غير موجود ⇒ إنشاء
        val signUp = request(SIGN_UP + API_KEY, body)
        if (signUp is Result.Ok) return signUp

        // 3) تسابق نادر: أُنشئ الحساب من مكان آخر ⇒ نعيد محاولة الدخول
        if ((signUp as? Result.Error)?.let { it.message.contains("EMAIL_EXISTS") } == true) {
            return request(SIGN_IN + API_KEY, body)
        }
        return signUp
    }

    /** يعيد تعيين كلمة السر عبر البريد (Firebase يرسل الرابط بنفسه). */
    fun sendPasswordReset(email: String): Result {
        if (!isConfigured) return Result.NotConfigured
        val mail = email.trim().lowercase()
        AuthValidation.emailProblem(mail)?.let { return Result.Error(it) }
        val body = JSONObject().apply {
            put("requestType", "PASSWORD_RESET")
            put("email", mail)
        }.toString()
        val http = request("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=$API_KEY", body)
        return if (http is Result.Ok) Result.Ok("", "", "", 0) else http
    }

    /** يتجدّد رمز الجلسة المنتهي (idToken عمره ساعة). */
    fun refreshSession(refreshToken: String): Result {
        if (!isConfigured) return Result.NotConfigured
        if (refreshToken.isBlank()) return Result.Error("لا يوجد رمز تجديد للجلسة.")
        val body = "grant_type=refresh_token&refresh_token=$refreshToken"
        val raw = post(
            "https://securetoken.googleapis.com/v1/token?key=$API_KEY",
            body,
            "application/x-www-form-urlencoded"
        ) ?: return Result.Error("تعذّر تحديث الجلسة — تحقق من الاتصال.")
        return parse(raw, idField = "id_token", uidField = "user_id", refreshField = "refresh_token")
    }

    // ────────────────────────── التنفيذ ──────────────────────────

    private fun request(url: String, jsonBody: String): Result {
        val raw = post(url, jsonBody, "application/json")
            ?: return Result.Error("تعذّر الاتصال بالخدمة — تحقق من الإنترنت.")
        return parse(raw, "idToken", "localId", "refreshToken")
    }

    /** POST يرجع جسم الاستجابة، أو null لو فشل الاتصال أصلاً. */
    private fun post(url: String, body: String, contentType: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", contentType)
                setRequestProperty("Accept", "application/json")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: ""
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** يفكّ استجابة Firebase ويحوّل أكواد الخطأ لرسائل عربية مفهومة. */
    private fun parse(raw: String, idField: String, uidField: String, refreshField: String): Result {
        val json = try {
            JSONObject(raw)
        } catch (e: Exception) {
            return Result.Error("استجابة غير مفهومة من الخادم.")
        }
        if (json.has("error")) {
            val message = json.optJSONObject("error")?.optString("message").orEmpty()
            return Result.Error(humanError(message))
        }
        val uid = json.optString(uidField)
        val token = json.optString(idField)
        if (uid.isBlank()) return Result.Error("لم يرجع الخادم معرّفاً صالحاً.")
        return Result.Ok(
            uid = uid,
            idToken = token,
            refreshToken = json.optString(refreshField),
            expiresIn = json.optString("expiresIn", "3600").toLongOrNull() ?: 3600L
        )
    }

    private fun humanError(code: String): String = when {
        code.contains("EMAIL_EXISTS") -> "هذا البريد الإلكتروني مسجّل بالفعل."
        code.contains("EMAIL_NOT_FOUND") -> "لا يوجد حساب بهذا البريد."
        code.contains("INVALID_PASSWORD") -> "بيانات الدخول غير صحيحة، أو لا يوجد حساب بهذه البيانات بعد."
        code.contains("INVALID_LOGIN_CREDENTIALS") -> "بيانات الدخول غير صحيحة، أو لا يوجد حساب بهذه البيانات بعد."
        code.contains("WEAK_PASSWORD") -> "كلمة السر ضعيفة — اختر كلمة أقوى."
        code.contains("INVALID_EMAIL") -> "يرجى إدخال بريد إلكتروني صحيح."
        code.contains("TOO_MANY_ATTEMPTS") -> "محاولات كثيرة — جرّب بعد قليل."
        code.contains("OPERATION_NOT_ALLOWED") -> "تسجيل الدخول بالبريد غير مفعّل في إعدادات المشروع."
        code.contains("API_KEY_INVALID") || code.contains("API key not valid") -> "مفتاح المشروع غير صالح."
        code.contains("PERMISSION_DENIED") -> "الخدمة مرفوضة — راجع إعدادات المشروع."
        else -> "تعذّر إكمال العملية. جرّب مرة أخرى."
    }

    private fun isMissingAccount(message: String): Boolean =
        message.contains("EMAIL_NOT_FOUND") ||
            message.contains("INVALID_LOGIN_CREDENTIALS") ||
            message.contains("INVALID_PASSWORD")
}
