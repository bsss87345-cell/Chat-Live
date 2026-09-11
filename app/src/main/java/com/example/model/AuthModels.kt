package com.example.model

/**
 * مزودات تسجيل الدخول المدعومة
 */
enum class AuthProvider(val providerNameAr: String, val iconEmoji: String) {
    FACEBOOK("فيسبوك", "📘"),
    GOOGLE("جوجل", "🌐")
}

/**
 * بيانات الحساب المسترجعة بعد المصادقة ومنح الصلاحيات
 */
data class AuthUserAccount(
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val avatarEmoji: String = "👤",
    val provider: AuthProvider
)

/**
 * حالات نتيجة المصادقة
 */
sealed class AuthResult {
    data class Success(
        val account: AuthUserAccount,
        val generatedUserId: String
    ) : AuthResult()

    data class PermissionDenied(val message: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Cancelled : AuthResult()
}

/**
 * توليد معرّف رقمي فريد مكون من 8 أرقام للمستخدم تلقائياً بدلاً من اسم مستخدم إنجليزي
 */
fun generateUniqueUserId(): String {
    val randomNum = (10000000..99999999).random()
    return randomNum.toString()
}
