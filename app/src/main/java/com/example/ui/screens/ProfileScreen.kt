package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    posts: List<Post>,
    balance: Int,
    dailyBonusClaimed: Boolean,
    walletFilter: String,
    transactions: List<WalletTransaction>,
    storeItems: List<StoreItem>,
    onUpdateBio: (String) -> Unit,
    onUpdateProfile: (String, String, String) -> Unit,
    onToggleNotifications: () -> Unit,
    onTogglePrivacy: () -> Unit,
    onLogout: () -> Unit,
    onClaimDailyBonus: () -> Unit,
    onFilterChange: (String) -> Unit,
    onBuyItem: (StoreItem) -> Unit,
    onLikePost: (String) -> Unit,
    onCommentPost: (String) -> Unit,
    onSharePost: (Post) -> Unit,
    onNavigateToRecharge: () -> Unit = {}
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: النقاط والرصيد, 1: منشوراتي, 2: إعدادات الحساب
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditBioDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRechargeDialog by remember { mutableStateOf(false) }

    // User's own posts or activity
    val userPosts = posts.filter { 
        it.authorHandle == userProfile.handle || 
        it.authorHandle == "ID: ${userProfile.id}" || 
        it.authorHandle == "@user_me" || 
        it.id.startsWith("post_") ||
        it.id.startsWith("p_")
    }.take(6)
    val totalPostsCount = userPosts.size.coerceAtLeast(8)

    val filteredTransactions = transactions.filter { tx ->
        when (walletFilter) {
            "كسب (+)" -> tx.type == TransactionType.EARN
            "إنفاق (-)" -> tx.type == TransactionType.SPEND
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // -------------------------------------------------------------
        // 1. Profile Header (Avatar, Name, Bio, and Stats)
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_header_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar with Edit Badge
                    Box(
                        modifier = Modifier.size(86.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(MujtamaPrimary, MujtamaTeal, MujtamaGold)
                                    )
                                )
                                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = userProfile.avatarEmoji, fontSize = 42.sp)
                        }

                        // Edit avatar icon button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MujtamaGold)
                                .clickable { showEditProfileDialog = true }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل الصورة",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // User Name & Handle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = userProfile.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MujtamaPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "عضو نشط 🌟",
                                    color = MujtamaPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "ID: ${userProfile.id} • ${userProfile.joinDate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bio Box with Edit Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showEditBioDialog = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = userProfile.bio,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                lineHeight = 17.sp
                            )
                            IconButton(
                                onClick = { showEditBioDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "تعديل النبذة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Stats Grid Row (Posts, Followers, Following)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStatItem(title = "المنشورات", count = "$totalPostsCount")
                        VerticalDivider(modifier = Modifier.height(28.dp))
                        ProfileStatItem(title = "المتابعون", count = "${userProfile.followersCount}")
                        VerticalDivider(modifier = Modifier.height(28.dp))
                        ProfileStatItem(title = "يتابع", count = "${userProfile.followingCount}")
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. Profile Sub-Tabs Navigation
        // -------------------------------------------------------------
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                TabRow(
                    selectedTabIndex = selectedSubTab,
                    modifier = Modifier.padding(4.dp),
                    containerColor = Color.Transparent,
                    indicator = {}
                ) {
                    val subTabs = listOf(
                        Triple("النقاط والرصيد", Icons.Default.Stars, 0),
                        Triple("منشوراتي", Icons.Default.Article, 1),
                        Triple("إعدادات الحساب", Icons.Default.Settings, 2)
                    )

                    subTabs.forEach { (label, icon, index) ->
                        val isSelected = selectedSubTab == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedSubTab = index },
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .padding(vertical = 8.dp)
                                .testTag("profile_subtab_$index"),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. Sub-Tab 0: Points & Wallet Subsection
        // -------------------------------------------------------------
        if (selectedSubTab == 0) {
            // Virtual Points Balance Slim Bar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_points_balance_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Right side: Points Balance (Arabic RTL makes this natural right side)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MujtamaGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "النقاط",
                                    tint = MujtamaGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$balance",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "نقطة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MujtamaGold
                                )
                            }
                        }

                        // Left side: Recharge Button (زر "شحن")
                        Button(
                            onClick = {
                                onNavigateToRecharge()
                                showRechargeDialog = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MujtamaGold,
                                contentColor = Color(0xFF221500)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("recharge_points_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "شحن",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Points Transparency & Safe Gaming Policy
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MujtamaTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "ضمان الأمان والنزاهة للنقاط",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "• الرصيد يُشحن حصرياً من قبل إدارة التطبيق كمكافآت للمسابقات والنشاط.\n• لا يوجد أي سحب نقدي أو تحويل خارج التطبيق.\n• جميع الألعاب مجانية بدون أي رهان مالي بين المستخدمين.",
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Transactions Header & Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل عمليات النقاط (كسب / إنفاق) 📜",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("الكل", "كسب (+)", "إنفاق (-)").forEach { filter ->
                            FilterChip(
                                selected = walletFilter == filter,
                                onClick = { onFilterChange(filter) },
                                label = { Text(filter, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }

            // Transactions List Items
            items(filteredTransactions, key = { it.id }) { tx ->
                val isEarn = tx.type == TransactionType.EARN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isEarn) MujtamaOnlineGreen.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isEarn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isEarn) MujtamaOnlineGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${tx.note} • ${tx.date}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = if (isEarn) "+${tx.points}" else "-${tx.points}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = if (isEarn) MujtamaOnlineGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Store items preview in profile
            item {
                Text(
                    text = "متجر المكافآت والأوسمة الافتراضية 🎁",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(storeItems, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (item.isOwned) MujtamaTeal.copy(alpha = 0.2f) else MujtamaGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (item.category) {
                                    "إطارات" -> Icons.Default.FilterFrames
                                    "أوسمة" -> Icons.Default.MilitaryTech
                                    else -> Icons.Default.Palette
                                },
                                contentDescription = null,
                                tint = if (item.isOwned) MujtamaTeal else MujtamaGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = item.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${item.cost} نقطة", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MujtamaGold)
                        }

                        if (item.isOwned) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MujtamaTeal.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "مملوك لديك ✓",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MujtamaTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { onBuyItem(item) },
                                enabled = balance >= item.cost,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("استبدال", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 4. Sub-Tab 1: My Posts & Recent Activity
        // -------------------------------------------------------------
        if (selectedSubTab == 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "منشوراتي ونشاطي الأخير (${userPosts.size}) 📝",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (userPosts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, tint = MujtamaPrimary, modifier = Modifier.size(40.dp))
                            Text("لم تقم بنشر أي مشاركة بعد!", fontWeight = FontWeight.Bold)
                            Text("شارك أفكارك وتحدياتك مع أصدقائك في مجتمعنا الآن.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(userPosts, key = { it.id }) { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MujtamaPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(userProfile.avatarEmoji, fontSize = 18.sp)
                                    }
                                    Column {
                                        Text(text = userProfile.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = post.timeAgo, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (post.tag != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MujtamaPrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = post.tag,
                                            fontSize = 10.sp,
                                            color = MujtamaPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = post.content,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Interactions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onLikePost(post.id) }) {
                                    Icon(
                                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "إعجاب",
                                        tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${post.likesCount}", fontSize = 11.sp)
                                }

                                TextButton(onClick = { onCommentPost(post.id) }) {
                                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "تعليق", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${post.commentsCount}", fontSize = 11.sp)
                                }

                                TextButton(onClick = { onSharePost(post) }) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مشاركة", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 5. Sub-Tab 2: Account Settings
        // -------------------------------------------------------------
        if (selectedSubTab == 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "إعدادات الحساب والخصوصية ⚙️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        // 1. Edit Profile Item
                        SettingsActionRow(
                            icon = Icons.Default.Person,
                            title = "تعديل بيانات الملف الشخصي",
                            subtitle = "الاسم، النبذة، والصورة الرمزية",
                            onClick = { showEditProfileDialog = true }
                        )

                        HorizontalDivider()

                        // 2. Privacy Setting Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MujtamaTeal)
                                Column {
                                    Text(text = "خصوصية الحساب", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "الحالة الحالية: ${userProfile.privacyLevel}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            OutlinedButton(
                                onClick = onTogglePrivacy,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("تبديل", fontSize = 11.sp)
                            }
                        }

                        HorizontalDivider()

                        // 3. Notifications Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (userProfile.isNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (userProfile.isNotificationsEnabled) MujtamaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(text = "إشعارات التطبيق", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = if (userProfile.isNotificationsEnabled) "التنبيهات مفعلة لجميع الأنشطة" else "التنبيهات مكتومة",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = userProfile.isNotificationsEnabled,
                                onCheckedChange = { onToggleNotifications() }
                            )
                        }

                        HorizontalDivider()

                        // 4. Logout Button
                        Button(
                            onClick = { showLogoutDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("logout_button")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // DIALOGS
    // -------------------------------------------------------------

    // Edit Bio Dialog
    if (showEditBioDialog) {
        var bioText by remember { mutableStateOf(userProfile.bio) }
        AlertDialog(
            onDismissRequest = { showEditBioDialog = false },
            title = { Text("تعديل النبذة التعريفية ✏️") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اكتب نبذة مميزة تعبر عن اهتماماتك:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = bioText,
                        onValueChange = { bioText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateBio(bioText)
                        showEditBioDialog = false
                    },
                    enabled = bioText.isNotBlank()
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBioDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Full Edit Profile Dialog (Name, Bio, Emoji)
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(userProfile.name) }
        var bioInput by remember { mutableStateOf(userProfile.bio) }
        var emojiInput by remember { mutableStateOf(userProfile.avatarEmoji) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("تعديل الملف الشخصي 👤") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("الاسم المعروض") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("النبذة التعريفية") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Text("اختر رمز الصورة الشخصية:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("👨‍💻", "👩‍💻", "👑", "🛡️", "🎮", "🚀").forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (emojiInput == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { emojiInput = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(nameInput, bioInput, emojiInput)
                        showEditProfileDialog = false
                    },
                    enabled = nameInput.isNotBlank()
                ) {
                    Text("حفظ التغييرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("تأكيد تسجيل الخروج 🚪") },
            text = { Text("هل أنت متأكد من رغبتك في تسجيل الخروج من حسابك؟ ستظل بياناتك ورصيد نقاطك محفوظة بالكامل.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تسجيل الخروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Recharge Points Dialog (شحن الرصيد والمكافآت)
    if (showRechargeDialog) {
        AlertDialog(
            onDismissRequest = { showRechargeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = MujtamaGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("شحن رصيد النقاط والمكافآت ⚡")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "رصيدك الحالي: $balance نقطة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Daily Bonus Option
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("هدية الدخول اليومية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("+150 نقطة", fontWeight = FontWeight.Black, color = MujtamaGold, fontSize = 12.sp)
                            }
                            Text(
                                "احصل على مكافأة مجانية يومية فور تسجيل الدخول لدعم نشاطك.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    onClaimDailyBonus()
                                    showRechargeDialog = false
                                },
                                enabled = !dailyBonusClaimed,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MujtamaGold, contentColor = Color(0xFF221500)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (dailyBonusClaimed) "تم استلام هدية اليوم ✓" else "شحن هدية اليوم الآن (+150)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Activity & Challenges info
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MujtamaTeal.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MujtamaTeal, modifier = Modifier.size(20.dp))
                            Text(
                                text = "يمكنك أيضاً زيادة رصيدك بالمشاركة في تحديات الغرف الصوتية، الفوز بالألعاب، ومسابقات الفريق التفاعلية.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRechargeDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun ProfileStatItem(title: String, count: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = count,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MujtamaPrimary)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
