package com.example.ui.screens

import androidx.compose.foundation.border
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
private val neonCardBorder = androidx.compose.foundation.BorderStroke(
    1.dp,
    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(NeonCyan, NeonPurple))
)

@Composable
fun AccountSettingsScreen(
    userProfile: UserProfile,
    posts: List<Post>,
    balance: Int,
    walletFilter: String,
    transactions: List<WalletTransaction>,
    storeItems: List<StoreItem>,
    onToggleNotifications: () -> Unit,
    onLogout: () -> Unit,
    onFilterChange: (String) -> Unit,
    onBuyItem: (StoreItem) -> Unit,
    onLikePost: (String) -> Unit,
    onCommentPost: (String) -> Unit,
    onSharePost: (Post) -> Unit,
    onNavigateToRecharge: () -> Unit,
    onUpdateProfile: (String, String, String) -> Unit,
    onUpdateBio: (String) -> Unit,
    onTogglePrivacy: () -> Unit = {},
    chatRooms: List<ChatRoom> = emptyList(),
    activeCommentPostId: String? = null,
    onCloseComments: () -> Unit = {},
    onAddComment: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    // null = showing the main vertical menu list; 0/1/2 = which full page is open
        var openPage by remember { mutableStateOf<Int?>(null) }

    // null = قائمة تصنيفات النشاط، أو عنوان التصنيف المفتوح حالياً
    var activityDetail by remember { mutableStateOf<String?>(null) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRechargeDialog by remember { mutableStateOf(false) }
    var selectedRechargePackage by remember { mutableStateOf<RechargePackage?>(null) }
    val rechargeClipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var showSupportChatDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf("العربية") }
    var showReportProblemDialog by remember { mutableStateOf(false) }
    var isPrivacyExpanded by remember { mutableStateOf(false) }
    var isSupportExpanded by remember { mutableStateOf(false) }
    var isGeneralExpanded by remember { mutableStateOf(false) }
    var showBlockedListDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var blockedUsersList by remember {
        mutableStateOf(emptyList<Triple<String, String, String>>())
    }

    val userPosts = posts.filter {
        it.authorHandle == userProfile.handle ||
        it.authorHandle == "ID: ${userProfile.id}" ||
        it.authorHandle == "@user_me" ||
        it.id.startsWith("post_") ||
                it.id.startsWith("p_")
    }.take(6)

    // --- منشورات النشاط ومنطق الفيديو النشط (نفس منطق الرئيسية) ---
    val activityLikedPosts = posts.filter { it.isLiked }
    val activityCommentedPosts = posts.filter { p ->
        p.commentsList.any { it.authorName == "أنت (أنا)" }
    }

    val settingsListState = rememberLazyListState()
    val activityDetailPosts = when (activityDetail) {
        "الإعجابات" -> activityLikedPosts
        "التعليقات" -> activityCommentedPosts
        else -> emptyList()
    }
    val activityVideoIds = activityDetailPosts
        .filter { it.mediaType == PostMediaType.SHORT_VIDEO }
        .map { it.id }
        .toSet()
    val activeActivityVideoId by remember(activityVideoIds) {
        derivedStateOf {
            val layout = settingsListState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo
                .filter { info -> (info.key as? String) in activityVideoIds }
                .minByOrNull { info -> kotlin.math.abs(info.offset + info.size / 2 - center) }
                ?.key as? String
        }
    }
    val isSettingsScrolling by remember { derivedStateOf { settingsListState.isScrollInProgress } }

    val filteredTransactions = transactions.filter { tx ->
        when (walletFilter) {
            "كسب (+)" -> tx.type == TransactionType.EARN
            "إنفاق (-)" -> tx.type == TransactionType.SPEND
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("account_settings_screen")
    ) {
        if (openPage == null) {
            // -------------------------------------------------------------
            // MAIN MENU (الإعدادات الأساسية) — unchanged
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("account_settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
tint = NeonCyan
                    )
                }
                Text(
                    text = "القائمة",
color = NeonCyan,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
    1.5.dp,
    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(NeonCyan, NeonPurple))
),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val menuItems = listOf(
                        Triple("الملف الشخصي", Icons.Default.Person, 3),
                        Triple("النشاط", Icons.Default.Article, 1),
                        Triple("النقاط والرصيد", Icons.Default.Stars, 0),
                        Triple("الإشعارات", Icons.Default.Notifications, 4),
                        Triple("الخصوصية والحظر", Icons.Default.Block, 5),
                        Triple("عام", Icons.Default.Settings, 6),
                        Triple("الدعم والمساعدة", Icons.Default.SupportAgent, 7),
                        Triple("تسجيل الخروج", Icons.Default.Logout, -1)
                    )

                    menuItems.forEachIndexed { pos, (label, icon, index) ->
                        val isLogout = index == -1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
.border(
    1.dp,
    if (isLogout) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    else if (pos % 2 == 0) NeonCyan.copy(alpha = 0.7f)
    else NeonPurple.copy(alpha = 0.7f),
    RoundedCornerShape(14.dp)
)
                                .clickable {
                                    if (isLogout) showLogoutDialog = true else openPage = index
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("account_settings_menu_item_$index"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isLogout) MaterialTheme.colorScheme.error else if (pos % 2 == 0) NeonCyan else NeonPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isLogout) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLogout) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (!isLogout) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // -------------------------------------------------------------
            // FULL-SCREEN SUB-PAGE — back button returns to the main menu
            // -------------------------------------------------------------
            val pageTitle = when (openPage) {
                3 -> "تعديل الملف الشخصي"
                2 -> "إعدادات الحساب"
                1 -> activityDetail ?: "النشاط"
                4 -> "الإشعارات"
                5 -> "الخصوصية والحظر"
                6 -> "عام"
                7 -> "الدعم والمساعدة"
                else -> "النقاط والرصيد"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                   IconButton(
                    onClick = {
                        // داخل صفحة نشاط تفصيلية؟ ارجع لقائمة النشاط أولاً، مو للقائمة الرئيسية
                        if (openPage == 1 && activityDetail != null) {
                            activityDetail = null
                        } else {
                            openPage = null
                        }
                    },
                    modifier = Modifier.testTag("account_settings_page_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
tint = NeonCyan
                    )
                }
                Text(
                    text = pageTitle,
color = NeonCyan,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

                        LazyColumn(
                state = settingsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("account_settings_content"),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // -------------------------------------------------------------
                // Page: تعديل الملف الشخصي
                // -------------------------------------------------------------
                if (openPage == 3) {
                    item {
                        var nameInput by remember { mutableStateOf(userProfile.name) }
                        var bioInput by remember { mutableStateOf(userProfile.bio) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_profile_card"),
                            border = neonCardBorder,
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("الاسم المعروض") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = NeonCyan.copy(alpha = 0.5f),
                                        focusedLabelColor = NeonCyan,
                                        cursorColor = NeonCyan
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = bioInput,
                                    onValueChange = { bioInput = it },
                                    label = { Text("النبذة التعريفية") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = NeonCyan.copy(alpha = 0.5f),
                                        focusedLabelColor = NeonCyan,
                                        cursorColor = NeonCyan
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4
                                )

                                Button(
                                    onClick = {
                                        onUpdateProfile(nameInput, bioInput, userProfile.avatarEmoji)
                                        onUpdateBio(bioInput)
                                        openPage = null
                                    },
                                    enabled = nameInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF00FF),
                                        contentColor = Color.Black,
                                        disabledContainerColor = Color(0xFFFF00FF).copy(alpha = 0.35f),
                                        disabledContentColor = Color.Black.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("حفظ التغييرات")
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // Page: النقاط والرصيد
                // -------------------------------------------------------------
                if (openPage == 0) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_points_balance_card"),
                            border = neonCardBorder,
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

                                Button(
                                                                        onClick = { showRechargeDialog = true },
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

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سجل عمليات النقاط (كسب / إنفاق) 📜",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("إنفاق (-)").forEach { filter ->
                                    FilterChip(
                                        selected = walletFilter == filter,
                                        onClick = { onFilterChange(filter) },
                                        label = { Text(filter, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }

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
                }

                // -------------------------------------------------------------
                // Page: منشوراتي
                // -------------------------------------------------------------
                if (openPage == 1) {
                    val likedPosts = activityLikedPosts
                    val commentedPosts = activityCommentedPosts
                    val joinedRooms = chatRooms.filter { it.isJoined }

                    // الأيقونة + العنوان + عدد العناصر لكل تصنيف
                    val activityCategories = listOf(
                        Triple(Icons.Default.Favorite, "الإعجابات", likedPosts.size),
                        Triple(Icons.Default.ChatBubbleOutline, "التعليقات", commentedPosts.size),
                        Triple(Icons.Default.Share, "المشاركات", 0),
                        Triple(Icons.Default.DeleteOutline, "المحتوى المحذوف", 0),
                        Triple(Icons.Default.MeetingRoom, "الغرف المنضم إليها", joinedRooms.size)
                    )

                    if (activityDetail == null) {
                        // ----------- قائمة التصنيفات -----------
                        item {
                            Text(
                                text = "النشاط 📋",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        items(activityCategories) { (icon, title, count) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activityDetail = title },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MujtamaPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MujtamaPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = if (count == 0) "لا يوجد نشاط بعد" else "$count عنصر",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // ----------- صفحة التصنيف المفتوح -----------
                        val detailPosts = when (activityDetail) {
                            "الإعجابات" -> likedPosts
                            "التعليقات" -> commentedPosts
                            else -> emptyList()
                        }

                        if (activityDetail == "الغرف المنضم إليها") {
                            if (joinedRooms.isEmpty()) {
                                item { ActivityEmptyState() }
                            } else {
                                items(joinedRooms, key = { it.id }) { room ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MujtamaPrimary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = room.iconEmoji, fontSize = 16.sp)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = room.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${room.memberCount} عضو · ${room.category}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (detailPosts.isEmpty()) {
                            item { ActivityEmptyState() }
                        } else {
                            // المنشورات تُعرض بنفس شكل الصفحة الرئيسية بالضبط
                            items(detailPosts, key = { it.id }) { post ->
                                val hasRealMedia = remember(post.mediaType, post.mediaUri) {
                                    post.mediaType != PostMediaType.NONE &&
                                        post.mediaUri.isNotBlank() &&
                                        java.io.File(post.mediaUri).exists()
                                }
                                                                if (hasRealMedia) {
                                    // نفس حل البروفايل: القائمة فيها هامش جانبي 12dp،
                                    // فنوسّع العنصر 24dp ونزيحه 12dp ليصير حافة لحافة
                                    Box(
                                        modifier = Modifier.layout { measurable, constraints ->
                                            val extraPx = 24.dp.roundToPx()
                                            val widened = constraints.maxWidth + extraPx
                                            val placeable = measurable.measure(
                                                constraints.copy(minWidth = widened, maxWidth = widened)
                                            )
                                            layout(placeable.width, placeable.height) {
                                                placeable.place(-extraPx / 2, 0)
                                            }
                                        }
                                    ) {
                                        MediaPostItem(
                                            post = post,
                                            onLikeClick = { onLikePost(post.id) },
                                            onCommentClick = { onCommentPost(post.id) },
                                            onShareClick = { onSharePost(post) },
                                            isActiveVideo = post.mediaType == PostMediaType.SHORT_VIDEO &&
                                                post.id == activeActivityVideoId,
                                            canMountVideo = post.mediaType != PostMediaType.SHORT_VIDEO ||
                                                !isSettingsScrolling
                                        )
                                    }
                                } else {
                                    PostCard(
                                        post = post,
                                        onLikeClick = { onLikePost(post.id) },
                                        onCommentClick = { onCommentPost(post.id) },
                                        onShareClick = { onSharePost(post) }
                                    )
                                }
                            }
                        }
                    }
                }

     
                // -------------------------------------------------------------
                // Page: الإشعارات
                // -------------------------------------------------------------
                if (openPage == 4) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notifications_card"),
                            border = neonCardBorder,
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (userProfile.isNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            contentDescription = null,
                                            tint = if (userProfile.isNotificationsEnabled) MujtamaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "الإشعارات",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (userProfile.isNotificationsEnabled) "التنبيهات مفعلة لجميع الأنشطة" else "التنبيهات معطلة",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = userProfile.isNotificationsEnabled,
                                        onCheckedChange = { onToggleNotifications() },
                                        modifier = Modifier.testTag("notifications_toggle_page")
                                    )
                                }
                            }
                        }
                    }
               }

                // -------------------------------------------------------------
                // Page: الخصوصية والحظر
                // -------------------------------------------------------------
                if (openPage == 5) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("privacy_level_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTogglePrivacy() }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "من يمكنه رؤية حسابي",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = userProfile.privacyLevel,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("privacy_card"),
                            border = neonCardBorder,
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showBlockedListDialog = true }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "قائمة الحظر",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${blockedUsersList.size} مستخدمين محظورين",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "${blockedUsersList.size}",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // Page: عام
                // -------------------------------------------------------------
                if (openPage == 6) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("general_card"),
                            border = neonCardBorder,
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showLanguageDialog = true }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = null,
                                            tint = MujtamaPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "تغيير اللغة",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "اللغة الحالية: $currentLanguage",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showPolicyDialog = true }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Policy,
                                            contentDescription = null,
                                            tint = MujtamaTeal,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "سياسة البرنامج",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "شروط الاستخدام والخصوصية",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "النسخة الحالية",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "الإصدار 1.2.0 (Build 104)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MujtamaOnlineGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "أحدث إصدار ✓",
                                            color = MujtamaOnlineGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // Page: الدعم والمساعدة
                // -------------------------------------------------------------
                                if (openPage == 7) {
                    selectedRechargePackage?.let { pack ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MujtamaGold.copy(alpha = 0.6f)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MujtamaGold.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "طلب شحن ${formatThousands(pack.points)} نقطة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MujtamaGold
                                    )
                                    Text(
                                        text = "حوّل مبلغ ${formatThousands(pack.iqd)} دينار عراقي إلى الرقم التالي من أي محفظة إلكترونية (زين كاش · آسيا حوالة · فاست باي · محفظة الناس)، ثم راسل الدعم برقم العملية ليتم شحن رصيدك.",
                                        fontSize = 12.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "07861890780",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = {
                                                rechargeClipboard.setText(
                                                    androidx.compose.ui.text.AnnotatedString("07861890780")
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "نسخ الرقم",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("نسخ", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_card"),
                            border = neonCardBorder,
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AccountSettingsActionRow(
                                    icon = Icons.Default.SupportAgent,
                                    title = "تواصل مباشر مع الدعم",
                                    subtitle = "محادثة فورية مع فريق الدعم الفني وخدمة العملاء",
                                                                        onClick = {
                                        selectedRechargePackage = null
                                        showSupportChatDialog = true
                                                                        }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                AccountSettingsActionRow(
                                    icon = Icons.Default.ReportProblem,
                                    title = "إبلاغ عن مشكلة",
                                    subtitle = "إرسال تقرير فني عن أي خلل أو عطل في التطبيق",
                                    onClick = { showReportProblemDialog = true }
                                )
                            }
                        }
                    }
                }
            }

        }
    }

    // Direct Support Chat Dialog (تواصل مباشر مع الدعم)
        if (showRechargeDialog) {
        RechargeDialog(
            onDismiss = { showRechargeDialog = false },
                        onSelectPackage = { pack ->
                selectedRechargePackage = pack
                showRechargeDialog = false
                showSupportChatDialog = true
                        }
        )
    }

    if (showSupportChatDialog) {
        val rechargePack = selectedRechargePackage
        var supportMessages by remember(rechargePack) {
            mutableStateOf(
                if (rechargePack != null) {
                    listOf(
                        Pair(
                            "فريق الدعم الفني 🎧",
                            "طلب شحن ${formatThousands(rechargePack.points)} نقطة بمبلغ ${formatThousands(rechargePack.iqd)} دينار عراقي.\n\n" +
                                "١) حوّل المبلغ إلى الرقم: 07861890780 من أي محفظة إلكترونية (زين كاش · آسيا حوالة · فاست باي · محفظة الناس).\n\n" +
                                "٢) أرفق هنا رقم البطاقة أو المحفظة التي حوّلت منها، مع صورة إشعار التحويل.\n\n" +
                                "سيتم إضافة الرصيد إلى حسابك خلال مدة لا تتجاوز ساعة ⏱️\nوفي حال تجاوز المدة يُرجى التواصل مع الدعم."
                        )
                    )
                } else {
                    listOf(
                        Pair("فريق الدعم الفني 🎧", "مرحباً بك في مركز الدعم الفني! كيف يمكننا مساعدتك اليوم؟")
                    )
                }
            )
        }
        var messageInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSupportChatDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MujtamaTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MujtamaTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تواصل مباشر مع الدعم",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MujtamaOnlineGreen)
                            )
                            Text(
                                text = "الفريق متصل الآن (24/7)",
                                fontSize = 10.sp,
                                color = MujtamaOnlineGreen
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("شحن الرصيد ⚡", "مشكلة في لعبة 🎲", "توثيق الحساب 🛡️").forEach { topic ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                modifier = Modifier.clickable {
                                    messageInput = topic
                                }
                            ) {
                                Text(
                                    text = topic,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                                                items(supportMessages) { (sender, text, imageUri) ->
                            val isMe = sender == "أنا"
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isMe) 12.dp else 2.dp,
                                        bottomEnd = if (isMe) 2.dp else 12.dp
                                    ),
                                    color = if (isMe) MujtamaPrimary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = sender,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = text,
                                            fontSize = 12.sp,
                                            color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                                        pendingImageUri?.let { previewUri ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AsyncImage(
                                model = previewUri,
                                contentDescription = "صورة التحويل المرفقة",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = "صورة التحويل مرفقة",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { pendingImageUri = null }) {
                                Text("إزالة", fontSize = 12.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                supportImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "إرفاق صورة التحويل",
                                tint = MujtamaTeal
                            )
                        }
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            placeholder = { Text("رقم البطاقة المحوّل منها...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        val canSend = messageInput.isNotBlank() || pendingImageUri != null
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    supportMessages = supportMessages + Triple(
                                        "أنا",
                                        messageInput.trim(),
                                        pendingImageUri
                                    )
                                    messageInput = ""
                                    pendingImageUri = null
                                    supportMessages = supportMessages + Triple(
                                        "فريق الدعم الفني 🎧",
                                        "تم استلام طلبك ✅ سيُضاف الرصيد خلال مدة لا تتجاوز ساعة. وفي حال تجاوز المدة تواصل معنا هنا.",
                                        null
                                    )
                                }
                            },
                            enabled = canSend
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "إرسال",
                                tint = if (canSend) MujtamaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupportChatDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

// Logout Confirmation Dialog (تأكيد تسجيل الخروج)
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك بتسجيل الخروج من حسابك؟",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
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

    // Change Language Dialog (تغيير اللغة)
    if (showLanguageDialog) {
        var tempSelectedLang by remember { mutableStateOf(currentLanguage) }
        val languages = listOf(
            Pair("العربية", "العربية (الافتراضية)"),
            Pair("English", "English (United States)"),
            Pair("Français", "Français (France)")
        )

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MujtamaPrimary
                    )
                    Text("تغيير لغة التطبيق 🌐", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اختر اللغة المفضلة لواجهة التطبيق:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    languages.forEach { (code, label) ->
                        val isSelected = tempSelectedLang == code
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempSelectedLang = code }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (code == "العربية") {
                                        Text(
                                            text = "اللغة الأساسية للمنصة",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentLanguage = tempSelectedLang
                        showLanguageDialog = false
                    }
                ) {
                    Text("تطبيق اللغة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Report Problem Dialog (إبلاغ عن مشكلة)
    if (showReportProblemDialog) {
        var issueCategory by remember { mutableStateOf("مشكلة تقنية عامة") }
        var issueDescription by remember { mutableStateOf("") }
        var isSubmitted by remember { mutableStateOf(false) }

        val categories = listOf("مشكلة في الصوت 🎙️", "خطأ في الألعاب 🎲", "شحن ونقاط ⚡", "مشكلة تقنية عامة ⚠️")

        AlertDialog(
            onDismissRequest = {
                showReportProblemDialog = false
                isSubmitted = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("إبلاغ عن مشكلة تقنية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                if (isSubmitted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MujtamaOnlineGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "تم إرسال البلاغ بنجاح! ✓",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "رقم التذكرة: #84920\nشكراً لمساعدتك في تحسين تجربتنا. سيقوم الفريق الفني بمراجعة البلاغ وحل المشكلة في أقرب وقت.",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "حدد نوع المشكلة التي تواجهك:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.take(2).forEach { cat ->
                                val selected = issueCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { issueCategory = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.drop(2).forEach { cat ->
                                val selected = issueCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { issueCategory = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Text(
                            text = "وصف المشكلة بالتفصيل:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = issueDescription,
                            onValueChange = { issueDescription = it },
                            placeholder = { Text("اذكر ما حدث معك بالتحديد...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = "سيتم ربط التقرير بمعرف حسابك (${userProfile.id}) للمتابعة.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (isSubmitted) {
                    Button(onClick = {
                        showReportProblemDialog = false
                        isSubmitted = false
                    }) {
                        Text("تم")
                    }
                } else {
                    Button(
                        onClick = { isSubmitted = true },
                        enabled = issueDescription.isNotBlank()
                    ) {
                        Text("إرسال البلاغ")
                    }
                }
            },
            dismissButton = {
                if (!isSubmitted) {
                    TextButton(onClick = { showReportProblemDialog = false }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }

    // Blocked Users Dialog (قائمة الحظر)
    if (showBlockedListDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedListDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("قائمة الحظر", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "المستخدمون المحظورون لا يمكنهم مراسلتك أو الانضمام لغرفك الخاصة:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (blockedUsersList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا يوجد أي مستخدم محظور حالياً 👍",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(blockedUsersList) { (id, name, date) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PersonOff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(text = "$date • ID: $id", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                blockedUsersList = blockedUsersList.filter { it.first != id }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("إلغاء الحظر", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedListDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // App Policy Dialog (سياسة البرنامج)
    if (showPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Policy,
                        contentDescription = null,
                        tint = MujtamaTeal
                    )
                    Text("سياسة البرنامج والخصوصية 📜", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "مرحباً بك في Chat Live! تسري هذه السياسة على جميع مستخدمي المنصة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Text(
                            text = "1. قواعد السلوك والاحترام المتبادل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "يُمنع منعاً باتاً نشر أي محتوى مسيء، ترويجي مزعج (سبام)، أو التعدي على خصوصية الأعضاء الآخرين في الغرف الصوتية والدردشات.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Text(
                            text = "2. حماية الخصوصية والبيانات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "نحن نحافظ على سرية بياناتك الشخصية ولا نشارك معرّف حسابك أو بريدك الإلكتروني مع أي أطراف ثالثة دون إذنك المسبق.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Text(
                            text = "3. نزاهة الألعاب والتحديات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "النقاط والمكافآت داخل التطبيق مخصصة للترفيه والمنافسة الشريفة. يُحظر استخدام أي برامج خارجية أو محاولات تلاعب بالنتائج.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Text(
                            text = "4. حقوق الأمان والإبلاغ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "يحق لكل مستخدم الإبلاغ عن أي انتهاك أو حظر أي حساب مسيء فوراً عبر أدوات الحظر المتاحة داخل التطبيق.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                    Button(onClick = { showPolicyDialog = false }) {
                    Text("فهمت وموافق")
                }
            }
        )
    }

    // ورقة التعليقات تُرسم هنا حتى تفتح داخل صفحة النشاط نفسها
    // بدل ما تنفتح بالبروفايل أو الرئيسية (الحالة بالـViewModel مشتركة بين الشاشات)
    if (activeCommentPostId != null) {
        val currentPost = posts.find { it.id == activeCommentPostId }
        if (currentPost != null) {
            CommentsBottomSheet(
                post = currentPost,
                onDismiss = onCloseComments,
                onAddComment = { commentText ->
                    onAddComment(currentPost.id, commentText)
                }
            )
        }
    }
}

@Composable
private fun AccountSettingsActionRow(
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

@Composable
private fun ActivityEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "لا يوجد نشاط بعد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                text = "تفاعلاتك راح تظهر هنا",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
