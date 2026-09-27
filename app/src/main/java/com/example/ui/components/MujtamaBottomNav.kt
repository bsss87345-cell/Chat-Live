package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppTab
import com.example.ui.theme.GlowingMagenta
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextSecondary

data class BottomNavItem(
    val tab: AppTab,
    val icon: ImageVector
)

@Composable
fun MujtamaBottomNav(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    unreadChatCount: Int = 2
) {
    val items = listOf(
        BottomNavItem(
            tab = AppTab.PROFILE,
            icon = Icons.Outlined.Person
        ),
        BottomNavItem(
            tab = AppTab.TEAM,
            icon = Icons.Outlined.Groups
        ),
        BottomNavItem(
            tab = AppTab.GAMES,
            icon = Icons.Outlined.SportsEsports
        ),
        BottomNavItem(
            tab = AppTab.CHAT,
            icon = Icons.Outlined.ChatBubbleOutline
        ),
        BottomNavItem(
            tab = AppTab.FEED,
            icon = Icons.Outlined.Home
        )
    )

    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.tab.titleAr,
                        modifier = Modifier
                            .size(24.dp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GlowingMagenta,
                    selectedTextColor = GlowingMagenta,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = GlowingMagenta.copy(alpha = 0.18f)
                ),
                modifier = Modifier.testTag("tab_${item.tab.name.lowercase()}")
            )
        }
    }
}
