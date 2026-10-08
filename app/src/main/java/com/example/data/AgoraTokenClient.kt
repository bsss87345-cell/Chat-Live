package com.example.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** دور المستخدم داخل الغرفة: متحدث (host) أو مستمع (audience). */
enum class VoiceRole(val wire: String) {
    HOST("host"),
    AUDIENCE("audience"),
}

/**
 * التوكن اللي يرجعه خادم Cloudflare، وجاهز لتمريره إلى AudioEngine.join().
 * لا تطبع هذا الكائن في السجل، لأنه يحتوي التوكن.
 */
data class VoiceToken(
    val token: String,
    val uid: Int,
    val channel: String,
    val expiresIn: Int,
)

/** خطأ واضح نعرضه للمستخدم. ما يحتوي أبداً على التوكن. */
class VoiceTokenException(
    message: String,
    val httpCode: Int? = null,
    cause: Throwable? = null,
) : IOException(message, cause)

/**
 * يطلب توكن غرفة صوتية من خادم Cloudflare.
 * يرسل توكن تسجيل دخول Firebase في الترويسة Authorization، ولا يحفظ أي شي.
 */
class AgoraTokenClient(
    private val endpoint: String = DEFAULT_ENDPOINT,
) {
    suspend fun fetchToken(channel: String, role: VoiceRole): VoiceToken {
        if (!CHANNEL_PATTERN.matches(channel)) {
            throw VoiceTokenException("اسم الغرفة غير صالح.")
        }
        val idToken = currentIdToken()
        return withContext(Dispatchers.IO) { postForToken(idToken, channel, role) }
    }

    /** يجيب توكن تسجيل الدخول الحالي. يشتغل على Dispatchers.IO لأن Tasks.await ما ينفع على الخيط الرئيسي. */
    private suspend fun currentIdToken(): String = withContext(Dispatchers.IO) {
        val user: FirebaseUser = authUserOrNull() ?: throw VoiceTokenException("لازم تسجّل دخول أول.")
        val token = try {
            Tasks.await(user.getIdToken(false)).token.orEmpty()
        } catch (e: Exception) {
            throw VoiceTokenException(TOKEN_FAILED, cause = e)
        }
        if (token.isBlank()) throw VoiceTokenException(TOKEN_FAILED)
        token
    }

    private fun authUserOrNull(): FirebaseUser? = try {
        FirebaseAuth.getInstance().currentUser
    } catch (e: Exception) {
        null
    }

    private fun postForToken(idToken: String, channel: String, role: VoiceRole): VoiceToken {
        val connection = try {
            URL(endpoint).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw VoiceTokenException(NETWORK_FAILED, cause = e)
        }
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $idToken")

            val body = JSONObject().put("channel", channel).put("role", role.wire).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            if (code !in 200..299) {
                throw VoiceTokenException(messageForCode(code), httpCode = code)
            }
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(text)
            return VoiceToken(
                token = json.getString("token"),
                uid = json.getInt("uid"),
                channel = json.getString("channel"),
                expiresIn = json.getInt("expiresIn"),
            )
        } catch (e: VoiceTokenException) {
            throw e
        } catch (e: JSONException) {
            throw VoiceTokenException(BAD_RESPONSE, cause = e)
        } catch (e: IOException) {
            throw VoiceTokenException(NETWORK_FAILED, cause = e)
        } finally {
            connection.disconnect()
        }
    }

    private fun messageForCode(code: Int): String = when (code) {
        400 -> "بيانات الغرفة غير صالحة."
        401 -> "انتهت جلسة الدخول. سجّل دخول من جديد."
        500 -> "خادم الصوت غير جاهز حالياً. جرّب لاحقاً."
        else -> "تعذّر الحصول على توكن الصوت (رمز $code). جرّب لاحقاً."
    }

    companion object {
        /** عنوان خادم التوكن على Cloudflare Worker. */
        const val DEFAULT_ENDPOINT = "https://chat-live-token.sweett4001.workers.dev/token"

        private const val TIMEOUT_MS = 10_000
        private val CHANNEL_PATTERN = Regex("[A-Za-z0-9_-]{1,64}")
        private const val TOKEN_FAILED = "ما قدرنا نجيب توكن الدخول. سجّل دخول من جديد وجرّب."
        private const val NETWORK_FAILED = "تعذّر الاتصال بالخادم. تأكد من الإنترنت وجرّب مرة ثانية."
        private const val BAD_RESPONSE = "رد الخادم غير متوقع. جرّب لاحقاً."
    }
}
