package com.example.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AgoraTokenClient
import com.example.data.AudioEngine
import com.example.data.VoiceRole
import com.example.data.VoiceTokenException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** مرحلة الغرفة الصوتية: خامل، يتصل، داخل الغرفة، أو خطأ. */
enum class VoicePhase { IDLE, CONNECTING, JOINED, ERROR }

/** الحالة اللي تعرضها شاشة الغرفة الصوتية. */
data class VoiceRoomUiState(
    val phase: VoicePhase = VoicePhase.IDLE,
    val channel: String? = null,
    val asHost: Boolean = false,
    val uid: Int? = null,
    val micMuted: Boolean = false,
    val speakerOn: Boolean = true,
    val remoteUids: Set<Int> = emptySet(),
    val speakingLevels: Map<Int, Int> = emptyMap(),
    val errorMessage: String? = null,
)

/**
 * يدير دخول الغرفة الصوتية: يجهّز المحرّك، يطلب التوكن من الخادم، ثم يدخل القناة.
 *
 * قواعد:
 *  • المضيف المباشر (asHost=true) يحتاج إذن الميكروفون، والشاشة تطلبه قبل الاستدعاء.
 *  • الدخول العادي كمستمع. [setHostRole] يرفع المستخدم إلى متحدث داخل نفس القناة بدون خروج.
 *  • تبديل الدور وتجديد التوكن يمرّان عبر قفل واحد، فلا يتعارضان على التوكن نفسه.
 *  • المحرّك يُنشأ ويُحفظ دون أي تعليق بينهما، فلا يضيع حتى لو أُلغي الدخول في منتصف الطريق.
 *  • نداءات Agora تأتي من خيط أصلي، والتحديث عبر update() آمن للخيوط.
 *  • المحرّك يُتلف عند إغلاق الـViewModel فقط.
 */
class VoiceRoomViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenClient = AgoraTokenClient()
    private var engine: AudioEngine? = null
    private var joinJob: Job? = null

    /** الدور المطلوب حالياً (من المقعد). يُطبَّق فوراً داخل الغرفة، أو عند اكتمال الدخول. */
    @Volatile
    private var wantHost = false

    /** يمنع تبديل الدور وتجديد التوكن في نفس الوقت. */
    private val roleMutex = Mutex()

    private val _state = MutableStateFlow(VoiceRoomUiState())
    val state: StateFlow<VoiceRoomUiState> = _state.asStateFlow()

    private val listener = object : AudioEngine.Listener {
        override fun onJoined(channel: String, uid: Int) {
            _state.update { it.copy(phase = VoicePhase.JOINED, channel = channel, uid = uid, errorMessage = null) }
            // لو طُلب دور المتحدث أثناء الاتصال، نطبّقه الآن.
            applyRoleAsync()
        }

        override fun onRemoteJoined(uid: Int) {
            _state.update { it.copy(remoteUids = it.remoteUids + uid) }
        }

        override fun onRemoteLeft(uid: Int) {
            _state.update {
                it.copy(remoteUids = it.remoteUids - uid, speakingLevels = it.speakingLevels - uid)
            }
        }

        override fun onSpeakingLevels(levels: Map<Int, Int>) {
            _state.update { it.copy(speakingLevels = levels) }
        }

        override fun onTokenWillExpire(token: String) {
            viewModelScope.launch { roleMutex.withLock { renewCurrentToken() } }
        }

        override fun onError(code: Int) {
            _state.update { current ->
                if (current.phase == VoicePhase.CONNECTING) {
                    current.copy(phase = VoicePhase.ERROR, errorMessage = "تعذّر دخول الغرفة (رمز $code).")
                } else {
                    current.copy(errorMessage = "خطأ في محرّك الصوت (رمز $code).")
                }
            }
        }
    }

    /**
     * يدخل الغرفة. المضيف المباشر (asHost=true) يحتاج إذن الميكروفون.
     * الجسر يدخل دائماً بـ asHost=false، ثم يستدعي [setHostRole] حسب المقعد.
     */
    fun join(channel: String, asHost: Boolean) {
        val phase = _state.value.phase
        if (phase == VoicePhase.CONNECTING || phase == VoicePhase.JOINED) return
        if (asHost && !hasMicPermission()) {
            fail(NEED_MIC)
            return
        }
        wantHost = asHost
        val kept = _state.value
        _state.value = VoiceRoomUiState(
            phase = VoicePhase.CONNECTING,
            channel = channel,
            asHost = asHost,
            micMuted = kept.micMuted,
            speakerOn = kept.speakerOn,
        )
        joinJob = viewModelScope.launch {
            try {
                val audio = ensureEngine()
                val token = tokenClient.fetchToken(channel, if (asHost) VoiceRole.HOST else VoiceRole.AUDIENCE)
                val rc = audio.join(token.token, channel, token.uid, asHost)
                if (rc != 0) {
                    fail("تعذّر دخول الغرفة (رمز $rc).")
                    return@launch
                }
                // ننتظر تأكيد Agora (onJoined أو onError)، وإلا نعلن المهلة بدل ما نبقى في حالة الاتصال للأبد.
                val settled = withTimeoutOrNull(JOIN_TIMEOUT_MS) {
                    _state.first { it.phase != VoicePhase.CONNECTING }
                }
                if (settled == null) {
                    // لو وصل تأكيد متأخر بعد المهلة، نغادر حتى لا نبقى داخل الغرفة بدون ما تعرف الواجهة.
                    engine?.leave()
                    fail(JOIN_TIMEOUT)
                }
            } catch (e: VoiceTokenException) {
                fail(e.message ?: JOIN_GENERIC)
            } catch (e: IllegalStateException) {
                fail(e.message ?: JOIN_GENERIC)
            }
        }
    }

    /** يغادر الغرفة ويُبقي المحرّك جاهزاً. تفضيلات المايك والسماعة تبقى كما هي. */
    fun leave() {
        joinJob?.cancel()
        joinJob = null
        wantHost = false
        engine?.leave()
        val kept = _state.value
        _state.value = VoiceRoomUiState(micMuted = kept.micMuted, speakerOn = kept.speakerOn)
    }

    /**
     * يرفع المستخدم إلى متحدث (true) أو ينزله إلى مستمع (false) داخل الغرفة الحالية، بدون خروج منها.
     * الجسر يستدعيها عند الجلوس على مقعد أو القيام منه. إن لم يكتمل الدخول بعد، يُطبَّق عند اكتماله.
     */
    fun setHostRole(asHost: Boolean) {
        if (asHost && !hasMicPermission()) {
            _state.update { it.copy(errorMessage = NEED_MIC) }
            return
        }
        wantHost = asHost
        applyRoleAsync()
    }

    /** يكتم المايك أو يفعّله. قبل الدخول يُحفظ الاختيار ويُطبّق عند إنشاء المحرّك. */
    fun setMicMuted(muted: Boolean) {
        val audio = engine
        if (audio == null) {
            _state.update { it.copy(micMuted = muted) }
            return
        }
        if (audio.setMicMuted(muted) == 0) _state.update { it.copy(micMuted = muted) }
    }

    /** السماعة الخارجية أو سماعة الأذن. نفس قاعدة الكتم. */
    fun setSpeakerOn(on: Boolean) {
        val audio = engine
        if (audio == null) {
            _state.update { it.copy(speakerOn = on) }
            return
        }
        if (audio.setSpeakerOn(on) == 0) _state.update { it.copy(speakerOn = on) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        joinJob?.cancel()
        wantHost = false
        engine?.release()
        engine = null
        super.onCleared()
    }

    /** يطبّق الدور المطلوب في الخلفية، تحت القفل، وبالترتيب. */
    private fun applyRoleAsync() {
        viewModelScope.launch { roleMutex.withLock { applyRole() } }
    }

    /** يطبّق الدور المطلوب مرة واحدة. يُستدعى داخل القفل فقط. */
    private suspend fun applyRole() {
        val target = wantHost
        val before = _state.value
        if (before.phase != VoicePhase.JOINED || before.asHost == target) return
        val channel = before.channel ?: return
        val audio = engine ?: return

        if (target) {
            val token = try {
                tokenClient.fetchToken(channel, VoiceRole.HOST)
            } catch (e: VoiceTokenException) {
                reportRoleError(e.message ?: ROLE_ON_FAILED)
                return
            }
            // بعد انتظار الشبكة قد يكون المستخدم غادر الغرفة، فلا نلمس المحرّك.
            if (_state.value.phase != VoicePhase.JOINED) return
            if (before.uid != null && token.uid != before.uid) {
                reportRoleError(ROLE_ON_FAILED)
                return
            }
            val renewRc = audio.renewToken(token.token)
            if (renewRc != 0) {
                reportRoleError("$ROLE_ON_FAILED (رمز $renewRc)")
                return
            }
            val roleRc = audio.setRole(true)
            if (roleRc != 0) {
                audio.setRole(false) // نضمن أن المحرّك بقي مستمعاً
                reportRoleError("$ROLE_ON_FAILED (رمز $roleRc)")
                return
            }
        } else {
            val rc = audio.setRole(false)
            if (rc != 0) {
                reportRoleError("$ROLE_OFF_FAILED (رمز $rc)")
                return
            }
        }
        _state.update { it.copy(asHost = target) }
    }

    /** يجدّد توكن الدور الحالي قبل انتهائه، ويُبقي الدور كما هو. يُستدعى داخل القفل فقط. */
    private suspend fun renewCurrentToken() {
        val current = _state.value
        if (current.phase != VoicePhase.JOINED) return
        val channel = current.channel ?: return
        val audio = engine ?: return
        val role = if (current.asHost) VoiceRole.HOST else VoiceRole.AUDIENCE
        val token = try {
            tokenClient.fetchToken(channel, role)
        } catch (e: VoiceTokenException) {
            reportRoleError(TOKEN_RENEW_FAILED)
            return
        }
        if (_state.value.phase != VoicePhase.JOINED) return
        val rc = audio.renewToken(token.token)
        if (rc != 0) reportRoleError("$TOKEN_RENEW_FAILED (رمز $rc)")
    }

    /** خطأ تغيير الدور لا يُخرج المستخدم من الغرفة. يبقى كما هو ويعرض الرسالة. */
    private fun reportRoleError(message: String) {
        _state.update { it.copy(errorMessage = message) }
    }

    /** يُنشئ المحرّك عند أول استعمال ويحفظه، ويطبّق التفضيلات الحالية عليه. */
    private fun ensureEngine(): AudioEngine {
        engine?.let { return it }
        val appId = BuildConfig.AGORA_APP_ID
        if (appId.isBlank()) throw IllegalStateException(NOT_CONFIGURED)
        val created = AudioEngine(getApplication<Application>(), appId, listener)
        if (!created.init()) throw IllegalStateException(NO_ENGINE)
        val current = _state.value
        created.setSpeakerOn(current.speakerOn)
        created.setMicMuted(current.micMuted)
        engine = created
        return created
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(getApplication<Application>(), Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun fail(message: String) {
        _state.update { it.copy(phase = VoicePhase.ERROR, errorMessage = message) }
    }

    private companion object {
        const val NEED_MIC = "نحتاج إذن الميكروفون عشان تتكلم في الغرفة."
        const val NOT_CONFIGURED = "صوت الغرف غير مهيّأ في هذا البناء."
        const val NO_ENGINE = "صوت الغرف غير مدعوم على هذا الجهاز."
        const val JOIN_GENERIC = "تعذّر دخول الغرفة. جرّب مرة ثانية."
        const val JOIN_TIMEOUT = "انتهت مهلة دخول الغرفة. جرّب مرة ثانية."
        const val JOIN_TIMEOUT_MS = 15_000L
        const val ROLE_ON_FAILED = "تعذّر تفعيل المايك للتحدّث. جرّب مرة ثانية."
        const val ROLE_OFF_FAILED = "تعذّر إيقاف المايك. اخرج من الغرفة وادخل من جديد."
        const val TOKEN_RENEW_FAILED = "تعذّر تجديد صلاحية الصوت. اخرج من الغرفة وادخل من جديد."
    }
}
