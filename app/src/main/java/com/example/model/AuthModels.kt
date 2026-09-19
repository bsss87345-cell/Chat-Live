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
 * توليد معرّف رقمي فريد مكون من 8 أرقام للمستخدم تلقائياً بدلاً من اسم مستخدم إنجليزي
 */
fun generateUniqueUserId(): String {
    val randomNum = (10000000..99999999).random()
    return randomNum.toString()
}
