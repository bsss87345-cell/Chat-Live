package com.example.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * تشفير كلمات مرور **الغرف** (لا كلمات مرور الحسابات — تلك مهمة Firebase Auth).
 *
 * • PBKDF2 مع salt عشوائي لكل غرفة (16 بايت) ⇒ لا جداول جاهزة ولا كشف جماعي.
 * • الخوارزمية تُخزَّن داخل النص نفسه ⇒ يعمل على الأجهزة القديمة (API 24/25) التي
 *   لا تدعم PBKDF2WithHmacSHA256، ويرقّي تلقائياً على الأحدث.
 * • الصيغة المخزنة:  pbkdf2$<algo>$<iterations>$<saltHex>$<hashHex>
 * • المقارنة ثابتة الزمن (MessageDigest.isEqual) ⇒ لا تسريب بالتوقيت.
 */
object PasswordHasher {

    /** بادئة تميّز القيم المشفّرة عن كلمات المرور النصية القديمة. */
    const val PREFIX = "pbkdf2\$"

    private const val ITERATIONS = 100_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    private val random = SecureRandom()

    /** أفضل خوارزمية متاحة على الجهاز (sha256 على API 26+، وإلا sha1). */
    private val algorithm: String by lazy {
        listOf("PBKDF2WithHmacSHA256", "PBKDF2WithHmacSHA1").firstOrNull { name ->
            try {
                SecretKeyFactory.getInstance(name)
                true
            } catch (e: Exception) {
                false
            }
        } ?: "PBKDF2WithHmacSHA1"
    }

    /** يُشفّر كلمة مرور جديدة. يرجع نصاً جاهزاً للتخزين. */
    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = derive(password, salt, ITERATIONS, algorithm)
        return PREFIX + algorithm + "$" + ITERATIONS + "$" + toHex(salt) + "$" + toHex(hash)
    }

    /** يتحقق من كلمة المرور مقابل قيمة **مشفّرة**. يرجع false لو القيمة ليست مشفّرة. */
    fun verify(password: String, stored: String?): Boolean {
        if (!isHashed(stored)) return false
        val parts = stored!!.split("$")
        if (parts.size != 5) return false
        val algo = parts[1]
        val iterations = parts[2].toIntOrNull() ?: return false
        val salt = fromHex(parts[3]) ?: return false
        val expected = fromHex(parts[4]) ?: return false
        val actual = try {
            derive(password, salt, iterations, algo)
        } catch (e: Exception) {
            return false
        }
        return MessageDigest.isEqual(expected, actual)
    }

    /** هل القيمة مشفّرة بهذه الصيغة؟ (وإلا فهي نص صريح قديم يحتاج ترقية) */
    fun isHashed(stored: String?): Boolean = stored != null && stored.startsWith(PREFIX)

    /**
     * يتحقق من كلمة مرور الغرفة مع دعم البيانات القديمة:
     * - قيمة **غير مشفّرة** (نص صريح من إصدار سابق) ⇒ مقارنة نصية (للتوافق فقط).
     */
    fun verifyOrLegacy(password: String, stored: String?): Boolean {
        if (stored == null) return false
        return if (isHashed(stored)) verify(password, stored) else stored == password
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int, algo: String): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(algo).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun toHex(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return out.toString()
    }

    private fun fromHex(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(hex[i * 2], 16)
            val lo = Character.digit(hex[i * 2 + 1], 16)
            if (hi < 0 || lo < 0) return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    private const val HEX = "0123456789abcdef"
}
