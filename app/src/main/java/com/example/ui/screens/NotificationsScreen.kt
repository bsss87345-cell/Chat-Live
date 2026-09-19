package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppNotification
import com.example.model.NotificationType

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    isDarkMode: Boolean,
    onClose: () -> Unit
) {
    val bg = if (isDarkMode) Color(0xFF121212) else Color(0xFFFFFFFF)
    val textMain = if (isDarkMode) Color(0xFFF5F5F5) else Color(0xFF1A1A1A)
    val textSub = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val unreadBg = if (isDarkMode) Color(0xFF1E2A3A) else Color(0xFFEAF2FF)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = bg) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = textMain)
                    }
                    Text(
                        text = "الإشعارات",
                        color = textMain,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = textSub,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("لا توجد إشعارات", color = textSub, fontSize = 16.sp)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(notifications, key = { it.id }) { n ->
                            val accent = colorFor(n.type)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (n.isRead) Color.Transparent else unreadBg)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        iconFor(n.type),
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(n.text, color = textMain, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(timeAgoText(n.timeMillis), color = textSub, fontSize = 12.sp)
                                }
                                if (!n.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2196F3))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun iconFor(type: NotificationType): ImageVector = when (type) {
    NotificationType.LIKE -> Icons.Filled.Favorite
    NotificationType.COMMENT -> Icons.Filled.ChatBubble
    NotificationType.FOLLOW -> Icons.Filled.PersonAdd
    NotificationType.SYSTEM -> Icons.Filled.Info
}

private fun colorFor(type: NotificationType): Color = when (type) {
    NotificationType.LIKE -> Color(0xFFE53935)
    NotificationType.COMMENT -> Color(0xFF2196F3)
    NotificationType.FOLLOW -> Color(0xFF43A047)
    NotificationType.SYSTEM -> Color(0xFFFB8C00)
}

private fun timeAgoText(timeMillis: Long): String {
    val minutes = (System.currentTimeMillis() - timeMillis) / 60000
    return when {
        minutes < 1 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        minutes < 1440 -> "منذ ${minutes / 60} ساعة"
        else -> "منذ ${minutes / 1440} يوم"
    }
}
