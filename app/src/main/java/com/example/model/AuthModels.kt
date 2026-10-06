package com.example.model

/**
 * بيانات الحساب بعد تسجيل الدخول أو إنشاء حساب جديد
 */
data class AuthUserAccount(
    val name: String,
    val email: String,
    val password: String = "",
    val avatarUrl: String? = null,
    val avatarEmoji: String = "👤"
)

/**
 * توليد معرّف رقمي من 8 أرقام للمستخدم.
 *
 * نطاق المستخدمين: 10000000 – 54999999
 * نطاق الغرف:      55000000 – 99999999
 * الفصل يضمن استحالة أن يحمل مستخدم وغرفة نفس الرقم.
 */
fun generateUniqueUserId(): String {
    // SecureRandom بدل kotlin.random: المعرّف السابق كان قابلاً للتوقّع تشفيرياً.
    // النطاق كما هو (8 أرقام: 10000000 – 54999999) فلا يتأثر أي حساب محفوظ.
    return (10_000_000 + userIdRandom.nextInt(45_000_000)).toString()
}

private val userIdRandom = java.security.SecureRandom()
