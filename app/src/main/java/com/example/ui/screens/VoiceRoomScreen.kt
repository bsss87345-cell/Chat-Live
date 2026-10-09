package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.VoicePhase
import com.example.viewmodel.VoiceRoomUiState
import com.example.viewmodel.VoiceRoomViewModel

/** حد تقريبي لاعتبار المستخدم يتكلم الآن. يُضبط بالتجربة على الجهاز. */
private const val SPEAKING_LEVEL = 10

/**
 * شاشة الغرفة الصوتية (نسخة مبدئية: بدون مقاعد ولا أسماء).
 *  • المتحدث يطلب إذن الميكروفون قبل الدخول. إذا رفضه يدخل كمستمع.
 *  • المستمع يدخل مباشرة بدون إذن.
 *  • زر "خروج" وزر الرجوع يغادران الغرفة ثم يغلقان الشاشة.
 */
@Composable
fun VoiceRoomScreen(
    channel: String,
    asHost: Boolean,
    onClose: () -> Unit,
    viewModel: VoiceRoomViewModel = viewModel(),
) {
    val context = LocalContext.current
    val ui by viewModel.state.collectAsStateWithLifecycle()
    var micDenied by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.join(channel, asHost = true)
        } else {
            micDenied = true
            viewModel.join(channel, asHost = false)
        }
    }

    fun enter() {
        when {
            !asHost -> viewModel.join(channel, asHost = false)
            hasMicPermission(context) -> viewModel.join(channel, asHost = true)
            else -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun leaveAndClose() {
        viewModel.leave()
        onClose()
    }

    LaunchedEffect(channel, asHost) { enter() }
    BackHandler { leaveAndClose() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = channel, style = MaterialTheme.typography.titleLarge)
        Text(text = statusText(ui), style = MaterialTheme.typography.bodyMedium)

        if (micDenied) {
            Text(text = "ما فعّلنا الميكروفون، دخلت كمستمع.", color = MaterialTheme.colorScheme.error)
        }
        ui.errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }
        if (ui.phase == VoicePhase.ERROR) {
            TextButton(onClick = {
                viewModel.clearError()
                enter()
            }) {
                Text("حاول مرة ثانية")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ui.remoteUids.toList()) { uid ->
                ParticipantRow(uid = uid, speaking = (ui.speakingLevels[uid] ?: 0) >= SPEAKING_LEVEL)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ui.asHost && ui.phase == VoicePhase.JOINED) {
                Button(
                    onClick = { viewModel.setMicMuted(!ui.micMuted) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (ui.micMuted) "شغّل المايك" else "كتم المايك")
                }
            }
            Button(
                onClick = { viewModel.setSpeakerOn(!ui.speakerOn) },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (ui.speakerOn) "سماعة خارجية" else "سماعة الأذن")
            }
            OutlinedButton(
                onClick = { leaveAndClose() },
                modifier = Modifier.weight(1f),
            ) {
                Text("خروج")
            }
        }
    }
}

@Composable
private fun ParticipantRow(uid: Int, speaking: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (speaking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (speaking) "مستخدم $uid · يتكلم الآن" else "مستخدم $uid",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun statusText(ui: VoiceRoomUiState): String = when (ui.phase) {
    VoicePhase.IDLE -> "غير متصل"
    VoicePhase.CONNECTING -> "جاري الاتصال..."
    VoicePhase.JOINED -> if (ui.asHost) "متصل كمتحدث" else "متصل كمستمع"
    VoicePhase.ERROR -> "تعذّر الاتصال"
}

private fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
