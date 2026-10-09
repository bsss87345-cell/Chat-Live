package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.SocialAppViewModel
import com.example.viewmodel.VoiceRoomViewModel

/**
 * 🌉 جسر خفي يربط مقاعد الغرفة الصوتية (ChatScreen) بمحرك الصوت (Agora).
 *
 * يُستدعى مرة واحدة من MainScreen عند تسجيل الدخول، ولا يرسم أي واجهة:
 *  • فتح غرفة دردشة → دخول قناة Agora كمستمع (يسمع الغرفة مباشرة).
 *  • إغلاق الغرفة / مغادرتها / تسجيل الخروج / فقد الاتصال → مغادرة القناة.
 *  • الجلوس على مقعد صوتي → طلب إذن الميكروفون ورفع المستخدم إلى متحدث.
 *  • القيام من المقعد → تنزيل المستخدم إلى مستمع.
 *  • الكتم يتبع حالة المقعد (isMuted في مقعد المستخدم).
 *  • أخطاء الصوت تظهر كـ Toast (إشعار Snackbar يحتاج تعديل SocialAppViewModel لاحقاً).
 *
 * ⚠️ "me" هو معرّف المستخدم المحلي في SocialAppViewModel. عند مزامنة المقاعد
 * مع Firestore يجب استبداله بمعرّف Firebase الحقيقي للمستخدم.
 */
@Composable
fun VoiceSeatBridge(socialViewModel: SocialAppViewModel) {
    val context = LocalContext.current
    val voiceViewModel: VoiceRoomViewModel = viewModel()

    val activeRoomId by socialViewModel.activeRoomId.collectAsStateWithLifecycle()
    val chatRooms by socialViewModel.chatRooms.collectAsStateWithLifecycle()
    val voiceState by voiceViewModel.state.collectAsStateWithLifecycle()

    // الغرفة الحالية، وهل المستخدم جالس على مقعد صوتي، وحالة كتمه
    val room = remember(activeRoomId, chatRooms) {
        chatRooms.firstOrNull { it.id == activeRoomId }
    }
    val isOnSeat = remember(room) {
        val r = room ?: return@remember false
        r.ownerVoiceSeat.occupantId == "me" || r.voiceSeats.any { it.occupantId == "me" }
    }
    val mySeatMuted = remember(room) {
        val r = room ?: return@remember false
        if (r.ownerVoiceSeat.occupantId == "me") r.ownerVoiceSeat.isMuted
        else r.voiceSeats.firstOrNull { it.occupantId == "me" }?.isMuted ?: false
    }

    // إذن الميكروفون (نفس نمط ChatScreen)
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val isOnSeatState = rememberUpdatedState(isOnSeat)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (!granted && isOnSeatState.value) {
            Toast.makeText(
                context,
                "نحتاج إذن الميكروفون للتحدّث في المقاعد الصوتية 🎙️",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // دخول/خروج القناة مع فتح/إغلاق الغرفة.
    // نغادر القناة الحالية دائماً أولاً، لضمان نظافة الحالة عند تبديل الغرف.
    LaunchedEffect(activeRoomId) {
        voiceViewModel.leave()
        val roomId = activeRoomId
        if (!roomId.isNullOrBlank()) voiceViewModel.join(roomId, asHost = false)
    }

    // الدور يتبع المقعد: جالس → متحدث (بعد الإذن)، قائم → مستمع.
    LaunchedEffect(isOnSeat, micGranted) {
        when {
            !isOnSeat -> voiceViewModel.setHostRole(false)
            micGranted -> voiceViewModel.setHostRole(true)
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // الكتم يتبع حالة المقعد
    LaunchedEffect(isOnSeat, mySeatMuted) {
        if (isOnSeat) voiceViewModel.setMicMuted(mySeatMuted)
    }

    // أخطاء الصوت → Toast
    val errorMessage = voiceState.errorMessage
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            voiceViewModel.clearError()
        }
    }

    // عند الخروج من MainScreen (تسجيل الخروج أو شاشة عدم الاتصال) اقطع الصوت
    DisposableEffect(Unit) {
        onDispose { voiceViewModel.leave() }
    }
}
