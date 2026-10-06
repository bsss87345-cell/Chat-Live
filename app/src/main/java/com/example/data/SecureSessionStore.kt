package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * جلسة المستخدم — **مشفّرة بمفتاح داخل عتاد/نظام الجهاز (Android Keystore)**.
 *
 * المشكلة التي يحلها: `_isLoggedIn` كان يبدأ `false` دائماً ⇒ كل فتح للتطبيق
 * يعني تسجيل دخول من جديد. وحفظ العلم وحده في ملف نصي كان سيكشف الجلسة
 * (وبعد ربط Firebase: رمز الدخول) لأي تطبيق أو نسخة احتياطية تقرأ الملف.
 *
 * الطريقة: مفتاح AES-256 **لا يخرج من Keystore أبداً**، والتشفير GCM
 * (تشفير + تحقق من السلامة) ⇒ أي تعديل على الملف يُرفض ولا يُفكّ بنجاح.
 *
 * ⚠️ نطاق المسؤولية: هذا الملف **يحفظ الجلسة**، ولا يسجّل دخولاً.
 * عند ربط Firebase Auth: تُمرّر `uid` و`idToken` من الخادم بدل القيم المحلية.
 *
 * ⚠️ القياس: وحدة التشفير [SessionCipher] منفصلة عن Keystore لتُختبر فعلياً
 * على JVM (13 اختباراً في `session_test`) — لا يُقبل تعديلها بلا إعادة تشغيلها.
 */
object SecureSessionStore {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "chatlive_session_key_v1"
    private const val FILE_NAME = "session.enc"
    private const val GCM_TAG_BITS = 128
    private const val AAD = "chatlive.session.v1"

    /** جلسة محفوظة. */
    data class Session(
        val userId: String,
        val token: String,
        val provider: String,
        val createdAt: Long
    )

    private var appContext: Context? = null

    /** يُستدعى مرة واحدة من `MainActivity.onCreate` (نفس نمط `LocalStore.initialize`). */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun isReady(): Boolean = appContext != null

    private fun file(): File = File(appContext!!.filesDir, FILE_NAME)

    /** يوجد ملف جلسة قابل للقراءة؟ (يُستخدم وحده لو أردنا فحصاً سريعاً) */
    fun hasSession(): Boolean = load() != null

    /**
     * يحفظ الجلسة. يرجع false لو الجهاز لا يدعم Keystore أو فشل التشفير —
     * والنتيجة: التطبيق يشتغل بلا استمرارية جلسة بدل أن ينهار.
     */
    fun save(userId: String, token: String = "", provider: String = "local"): Boolean {
        if (!isReady() || userId.isBlank()) return false
        return try {
            val payload = SessionCipher.encode(userId, token, provider, System.currentTimeMillis())
            val blob = SessionCipher.encrypt(secretKey(), payload)
            val f = file()
            f.writeBytes(blob)
            // ملف خاص بالتطبيق فقط (زيادة على أن مسار filesDir خاص أصلاً)
            f.setReadable(false, false)
            f.setReadable(true, true)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** يقرأ الجلسة المحفوظة. يرجع null لو ما فيه جلسة أو الملف معدَّل/تالف. */
    fun load(): Session? {
        if (!isReady()) return null
        return try {
            val f = file()
            if (!f.exists()) return null
            val payload = SessionCipher.decrypt(secretKey(), f.readBytes()) ?: return null
            SessionCipher.decode(payload)
        } catch (e: Exception) {
            null
        }
    }

    /** يمحو الجلسة (تسجيل خروج / جلسة غير صالحة). */
    fun clear() {
        if (!isReady()) return
        try {
            file().delete()
        } catch (e: Exception) {
            // تجاهل: لا شي نعمله، والملف سيُستبدل عند أول حفظ
        }
    }

    /**
     * مفتاح AES-256 داخل Keystore الجهاز. يُنشأ مرة واحدة ويبقى ثابتاً
     * (ولذلك الجلسة تبقى صالحة بين تشغيلات التطبيق).
     */
    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}

/**
 * وحدة التشفير والمعرّفة — **مستقلة عن أندرويد** لتُختبر على JVM.
 *
 * الصيغة: `IV (12 بايت) + ciphertext + tag (16 بايت)` ككتلة واحدة.
 * الـIV عشوائي بكل عملية ⇒ تشفير نفس الجلسة مرتين يعطي ملفين مختلفين.
 */
internal object SessionCipher {

    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val AAD = "chatlive.session.v1"

    fun encrypt(key: SecretKey, plain: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        require(iv.size == IV_BYTES) { "IV غير متوقع: ${iv.size}" }
        return iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
    }

    /** يرجع null لو الملف معدَّل أو المفتاح مختلف (لا انهيار). */
    fun decrypt(key: SecretKey, blob: ByteArray): String? {
        if (blob.size <= IV_BYTES) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE, key,
                GCMParameterSpec(TAG_BITS, blob.copyOfRange(0, IV_BYTES))
            )
            cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
            String(cipher.doFinal(blob.copyOfRange(IV_BYTES, blob.size)), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /** `v1|userId|createdAt|provider|token` — الرمز قد يحوي `|` فنجمعه للآخر. */
    fun encode(userId: String, token: String, provider: String, createdAt: Long): String =
        "v1|$userId|$createdAt|$provider|$token"

    fun decode(payload: String): SecureSessionStore.Session? {
        val parts = payload.split("|")
        if (parts.size < 5 || parts[0] != "v1") return null
        val userId = parts[1]
        val createdAt = parts[2].toLongOrNull() ?: return null
        val provider = parts[3]
        val token = parts.drop(4).joinToString("|")
        return SecureSessionStore.Session(userId, token, provider, createdAt)
    }
}
