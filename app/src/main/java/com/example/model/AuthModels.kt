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
    val randomNum = (10000000..54999999).random()
    return randomNum.toString()
}
