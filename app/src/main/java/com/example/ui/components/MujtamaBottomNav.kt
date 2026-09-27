package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppTab
import com.example.ui.theme.GlowingMagenta
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
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
        BottomNavItem(tab = AppTab.FEED, icon = Icons.Outlined.Home),
        BottomNavItem(tab = AppTab.CHAT, icon = Icons.Outlined.ChatBubbleOutline),
        BottomNavItem(tab = AppTab.GAMES, icon = Icons.Outlined.SportsEsports),
        BottomNavItem(tab = AppTab.TEAM, icon = Icons.Outlined.Groups),
        BottomNavItem(tab = AppTab.PROFILE, icon = Icons.Outlined.Person)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.Center)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = NeonCyan,
                    spotColor = NeonPurple
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                    shape = RoundedCornerShape(50)
                ),
            shape = RoundedCornerShape(50),
            color = DarkSurface.copy(alpha = 0.92f),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentTab == item.tab
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .then(
                                if (isSelected) Modifier.shadow(
                                    elevation = 12.dp,
                                    shape = CircleShape,
                                    ambientColor = GlowingMagenta,
                                    spotColor = GlowingMagenta
                                ) else Modifier
                            )
                            .clip(CircleShape)
                            .background(if (isSelected) GlowingMagenta else Color.Transparent)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onTabSelected(item.tab) }
                            .testTag("tab_${item.tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.tab.titleAr,
                            tint = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
