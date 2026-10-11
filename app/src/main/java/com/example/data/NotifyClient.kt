package com.example.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 🔔 إرسال إشعارات Push (FCM) عبر خادم Cloudflare (POST /notify).
 *
 * يستدعى من SocialAppViewModel بعد إرسال رسالة جديدة.
 * فشل الإرسال لا يوقف التطبيق — ترجع false بهدوء.
 */
class NotifyClient(
    private val endpoint: String = DEFAULT_ENDPOINT,
) {

    /**
     * يرسل إشعاراً لمجموعة مستخدمين.
     * @param userIds معرّفات Firebase للمستلمين (بلا تكرار، بحد 200).
     * @param title عنوان الإشعار.
     * @param body نص الإشعار.
     * @param data بيانات إضافية (مثلاً roomId).
     * @return true إذا نجح الطلب.
     */
    suspend fun notifyUsers(
        userIds: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): Boolean {
        val targets = userIds.filter { it.isNotBlank() }.distinct().take(MAX_TARGETS)
        if (targets.isEmpty()) return false
        return withContext(Dispatchers.IO) {
            try {
                postNotify(targets, title, body, data)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun postNotify(userIds: List<String>, title: String, body: String, data: Map<String, String>) {
        val idToken = currentIdToken()
        if (idToken.isBlank()) return

        val payload = JSONObject()
            .put("userIds", JSONArray(userIds))
            .put("title", title.take(100))
            .put("body", body.take(500))
            .put("data", JSONObject(data))
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $idToken")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) return
        } finally {
            connection.disconnect()
        }
    }

    private fun currentIdToken(): String {
        val user = FirebaseAuth.getInstance().currentUser ?: return ""
        return try {
            Tasks.await(user.getIdToken(false)).token.orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        /** عنوان /notify على Cloudflare Worker. */
        const val DEFAULT_ENDPOINT = "https://chat-live-token.sweett4001.workers.dev/notify"
        private const val TIMEOUT_MS = 10_000
        private const val MAX_TARGETS = 200
    }
}
