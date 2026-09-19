package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppTab
import com.example.ui.theme.MujtamaPrimary
import com.example.ui.theme.MujtamaTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MujtamaTopBar(
    currentTab: AppTab,
    walletBalance: Int = 0,
    isDarkMode: Boolean = false,
    onWalletClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    unreadCount: Int = 0,
    onSearchClick: () -> Unit = {},
    onToggleDarkMode: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Chat Live",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                )
            }
        },
        actions = {
            // Notification button
            Box(
                modifier = Modifier.padding(end = 4.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("top_bar_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "الإشعارات",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                // Unread dot indicator
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, end = 8.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
            }

            // Dark/Light mode toggle button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp)
            ) {
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("top_bar_theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.Brightness7 else Icons.Filled.Brightness4,
                        contentDescription = if (isDarkMode) "التبديل للوضع النهاري" else "التبديل للوضع الليلي",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
