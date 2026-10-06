package com.example.data

/**
 * تحقق الإدخال لشاشة الدخول والتسجيل.
 *
 * المشكلة التي يحلها: الفحص السابق كان `email.contains("@") && email.contains(".")`
 * (يقبل `@.` و`a@b.` و`@@..`) وكلمة سر من 6 أحرف بلا أي شرط تنوّع
 * ⇒ حسابات ضعيفة تنتقل كما هي لقاعدة البيانات.
 *
 * ⚠️ الرسلائل النصية هنا **مطابقة حرفياً** لنصوص الشاشة الحالية حتى لا يتغير
 * شكل الواجهة — والفرق أن الفحص صار أدق.
 *
 * ملف بلا أي اعتماد على أندرويد ⇒ مُختبَر فعلياً على JVM (انظر `validation_test`).
 */
object AuthValidation {

    const val MAX_NAME_CHARS = 60
    const val MIN_PASSWORD_CHARS = 8
    const val MAX_PASSWORD_CHARS = 128
    const val MAX_EMAIL_CHARS = 254

    /** بريد صحيح: نطاق + نقطة + امتداد حرفي — ويرفض `@.` و`a@b.` و`a@@b.com`. */
    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9]([A-Za-z0-9\\-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9\\-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean {
        val e = email.trim()
        return e.length in 6..MAX_EMAIL_CHARS &&
            !e.contains("..") &&
            EMAIL_REGEX.matches(e)
    }

    /** يرجع رسالة الخطأ، أو null لو البريد مقبول. */
    fun emailProblem(email: String): String? = when {
        email.isBlank() -> "يرجى إدخال البريد الإلكتروني."
        isValidEmail(email) -> null
        else -> "يرجى إدخال بريد إلكتروني صحيح."
    }

    /** يرجع رسالة الخطأ، أو null لو كلمة السر مقبولة. */
    fun passwordProblem(password: String): String? = when {
        password.isEmpty() -> "يرجى إدخال كلمة السر."
        password.length < MIN_PASSWORD_CHARS ->
            "يجب أن تكون كلمة السر $MIN_PASSWORD_CHARS أحرف على الأقل."
        password.length > MAX_PASSWORD_CHARS ->
            "كلمة السر طويلة جداً (الحد $MAX_PASSWORD_CHARS حرفاً)."
        password.any { it.isWhitespace() } -> "لا يمكن أن تحتوي كلمة السر على مسافات."
        password.none { it.isDigit() } -> "يجب أن تحتوي كلمة السر على رقم واحد على الأقل."
        password.none { it.isLetter() } -> "يجب أن تحتوي كلمة السر على حرف واحد على الأقل."
        isCommon(password) -> "كلمة السر شائعة جداً — اختر كلمة أقوى."
        else -> null
    }

    /** يرجع رسالة الخطأ، أو null لو الاسم مقبول. */
    fun nameProblem(name: String): String? {
        val n = name.trim()
        return when {
            n.length < 2 -> "يرجى إدخال اسم صحيح (حرفين على الأقل)."
            n.length > MAX_NAME_CHARS -> "الاسم طويل جداً (الحد $MAX_NAME_CHARS حرفاً)."
            n.any { it.isISOControl() } -> "الاسم يحوي رموزاً غير مسموحة."
            else -> null
        }
    }

    /** مُعرّف الدخول: بريد أو اسم مستخدم، غير فارغ وبحد أقصى معقول. */
    fun loginIdentifierProblem(identifier: String): String? =
        if (identifier.isBlank()) "يرجى إدخال اسم المستخدم أو البريد الإلكتروني وكلمة السر."
        else if (identifier.length > MAX_EMAIL_CHARS) "المُعرّف طويل جداً."
        else null

    /** كلمات سر شائعة (قائمة صغيرة — الفحص الحقيقي عند الخادم). */
    private val COMMON = setOf(
        "12345678", "123456789", "1234567890", "password", "password1", "qwerty123",
        "11111111", "00000000", "iloveyou", "admin123", "abcd1234", "aa123456"
    )

    private fun isCommon(password: String): Boolean = password.lowercase() in COMMON
}
