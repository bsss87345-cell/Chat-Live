package com.example.data

import android.content.Context
import android.util.Log
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

/**
 * محرّك صوت الغرف الحيّة (Agora RTC) — طبقة رفيعة فوق RtcEngine.
 *
 * ⚠️ يُستدعى من VoiceRoomViewModel فقط، والواجهة لا تستدعيه مباشرة.
 *
 * قرارات التصميم:
 *  • CHANNEL_PROFILE_LIVE_BROADCASTING + دورين (مضيف/مستمع): المقاعد = مضيفون، البقية مستمعون.
 *  • AUDIO_SCENARIO_CHATROOM: السيناريو الصوتي للدردشة (القيمة 5 مؤكدة من الحزمة نفسها).
 *  • لا App ID ولا Token داخل الكود: يمرّرهما المستدعي (من BuildConfig وسيرفر التوكنات).
 *  • الدوال تُرجع رمز Agora (0 = نجاح). الاستثناءات تُلتقط داخلياً ولا تخرج للواجهة.
 *  • نداءات Agora تأتي من خيط أصلي ⇒ المستمع مسؤول عن النقل لخيط الواجهة.
 */
class AudioEngine(
    context: Context,
    private val appId: String,
    private val listener: Listener,
) {

    interface Listener {
        /** انضم المستخدم للقناة. [uid] هو المعرّف الفعلي (مهم إذا مرّرت 0). */
        fun onJoined(channel: String, uid: Int) {}

        /** مستخدم بعيد انضم للقناة. */
        fun onRemoteJoined(uid: Int) {}

        /** مستخدم بعيد غادر القناة. */
        fun onRemoteLeft(uid: Int) {}

        /** مستويات الصوت لكل متحدّث كما يرسلها SDK (uid → volume). تُستخدم لإضاءة المقعد. */
        fun onSpeakingLevels(levels: Map<Int, Int>) {}

        /** التوكن على وشك الانتهاء ⇒ اطلب توكناً جديداً من السيرفر ومرّره. */
        fun onTokenWillExpire(token: String) {}

        /** خطأ من SDK برمز Agora. */
        fun onError(code: Int) {}
    }

    private val appContext: Context = context.applicationContext
    private var engine: RtcEngine? = null

    private val handler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            listener.onJoined(channel.orEmpty(), uid)
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            listener.onRemoteJoined(uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            listener.onRemoteLeft(uid)
        }

        override fun onAudioVolumeIndication(
            speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
            totalVolume: Int,
        ) {
            val levels = HashMap<Int, Int>()
            speakers?.forEach { levels[it.uid] = it.volume }
            listener.onSpeakingLevels(levels)
        }

        override fun onTokenPrivilegeWillExpire(token: String?) {
            listener.onTokenWillExpire(token.orEmpty())
        }

        override fun onError(err: Int) {
            listener.onError(err)
        }
    }

    /**
     * يُنشئ المحرّك مرة واحدة (آمن للاستدعاء المتكرر).
     * يُرجع false إن فشل: مثلاً مكتبة أصلية غير متوفرة لمعمارية الجهاز.
     */
    fun init(): Boolean {
        if (engine != null) return true
        return try {
            val config = RtcEngineConfig().apply {
                mContext = appContext
                mAppId = appId
                mEventHandler = handler
                mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                mAudioScenario = Constants.AUDIO_SCENARIO_CHATROOM
            }
            val created = RtcEngine.create(config)
            created.enableAudio()
            engine = created
            true
        } catch (e: Exception) {
            Log.w(TAG, "فشل إنشاء محرّك Agora", e)
            false
        } catch (e: LinkageError) {
            // UnsatisfiedLinkError أو NoClassDefFoundError: مكتبة Agora الأصلية غير متوفرة لمعمارية الجهاز
            Log.w(TAG, "مكتبة Agora الأصلية غير متوفرة لهذه المعمارية", e)
            false
        }
    }

    /**
     * يدخل القناة. [asHost]=true ⇒ مضيف يتكلم (ينشر المايك)، false ⇒ مستمع.
     * [uid]=0 ⇒ يولّد SDK معرّفاً ويُعاد في onJoined.
     * يُرجع رمز Agora: 0 = نجاح، ERR_NOT_INITIALIZED إن لم تُستدعَ init.
     */
    fun join(token: String, channelName: String, uid: Int, asHost: Boolean): Int {
        val e = engine ?: return Constants.ERR_NOT_INITIALIZED
        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            clientRoleType = if (asHost) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
            publishMicrophoneTrack = asHost
            autoSubscribeAudio = true
        }
        e.enableAudioVolumeIndication(VOLUME_INTERVAL_MS, VOLUME_SMOOTH, false)
        return e.joinChannel(token, channelName, uid, options)
    }

    /** يغادر القناة الحالية، والمحرّك يبقى جاهزاً لدخول قناة أخرى. */
    fun leave(): Int = engine?.leaveChannel() ?: Constants.ERR_NOT_INITIALIZED

    /** يكتم المايك المحلي أو يفعّله. */
    fun setMicMuted(muted: Boolean): Int =
        engine?.muteLocalAudioStream(muted) ?: Constants.ERR_NOT_INITIALIZED

    /** true = السماعة الخارجية، false = سماعة الأذن. */
    fun setSpeakerOn(on: Boolean): Int =
        engine?.setEnableSpeakerphone(on) ?: Constants.ERR_NOT_INITIALIZED

    /**
     * يبدّل الدور داخل القناة الحالية بدون مغادرتها: true ⇒ متحدث (ينشر المايك)، false ⇒ مستمع.
     * التبديل إلى متحدث يحتاج توكناً بصلاحية نشر، فاستدعِ [renewToken] بتوكن المضيف قبله.
     * يُرجع رمز Agora: 0 = نجاح.
     */
    fun setRole(asHost: Boolean): Int {
        val e = engine ?: return Constants.ERR_NOT_INITIALIZED
        val role = if (asHost) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
        val roleResult = e.setClientRole(role)
        if (roleResult != 0) return roleResult
        val options = ChannelMediaOptions().apply {
            clientRoleType = if (asHost) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
            publishMicrophoneTrack = asHost
            autoSubscribeAudio = true
        }
        return e.updateChannelMediaOptions(options)
    }

    /** يجدّد توكن القناة الحالية (قبل انتهائه، أو قبل التبديل إلى متحدث). يُرجع 0 = نجاح. */
    fun renewToken(token: String): Int = engine?.renewToken(token) ?: Constants.ERR_NOT_INITIALIZED

    /** يغادر ويُتلف المحرّك نهائياً. يُستدعى عند إغلاق الغرفة. */
    fun release() {
        val e = engine ?: return
        e.leaveChannel()
        RtcEngine.destroy()
        engine = null
    }

    private companion object {
        const val TAG = "AudioEngine"
        const val VOLUME_INTERVAL_MS = 300
        const val VOLUME_SMOOTH = 3
    }
}
