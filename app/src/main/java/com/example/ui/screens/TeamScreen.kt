package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class TeamSubSection(val titleAr: String) {
    REFERRALS("الإحالات"),
    FRIEND_REQUESTS("طلبات الصداقة"),
    ADD_FRIEND("إضافة صديق")
}

@Composable
fun TeamScreen(
    userProfile: UserProfile,
    team: Team,
    referrals: List<ReferredUser>,
    friendRequests: List<FriendRequest>,
    onBackClick: () -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onRejectFriendRequest: (String) -> Unit,
    onSimulateReferralJoined: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubSection by remember { mutableStateOf(TeamSubSection.REFERRALS) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- التوب بار ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // زر الرجوع يمين
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Surface(
                        onClick = onBackClick,
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, NeonCyan),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("team_back_button")
                            .shadow(8.dp, RoundedCornerShape(12.dp),
                                ambientColor = NeonCyan.copy(alpha = 0.4f),
                                spotColor = NeonCyan.copy(alpha = 0.4f))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "رجوع",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // العنوان في المنتصف
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "قسم الفريق والمجتمع",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "إدارة العلاقات، الإحالات، ورسائل الأصدقاء",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // --- سويتش التابات ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(TeamSubSection.REFERRALS, Icons.Default.Link, NeonPurple),
                    Triple(TeamSubSection.FRIEND_REQUESTS, Icons.Default.Group, NeonPurple),
                    Triple(TeamSubSection.ADD_FRIEND, Icons.Default.PersonAdd, NeonCyan)
                ).forEach { (section, icon, activeColor) ->
                    val isActive = activeSubSection == section
                    Surface(
                        onClick = { activeSubSection = section },
                        shape = RoundedCornerShape(50),
                        color = if (isActive) activeColor.copy(alpha = 0.18f) else DarkSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isActive) activeColor else NeonPurple.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isActive) Modifier.shadow(
                                    6.dp, RoundedCornerShape(50),
                                    ambientColor = activeColor.copy(alpha = 0.3f),
                                    spotColor = activeColor.copy(alpha = 0.3f)
                                ) else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isActive) activeColor else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = section.titleAr,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) activeColor else TextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // --- المحتوى ---
            AnimatedContent(
                targetState = activeSubSection,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.fillMaxSize(),
                label = "team_section_anim"
            ) { section ->
                when (section) {
                    TeamSubSection.REFERRALS -> ReferralsSection(
                        userProfile = userProfile,
                        referrals = referrals,
                        onSimulateReferralJoined = onSimulateReferralJoined
                    )
                    TeamSubSection.FRIEND_REQUESTS -> FriendRequestsSection(
                        requests = friendRequests,
                        onAccept = onAcceptFriendRequest,
                        onReject = onRejectFriendRequest
                    )
                    TeamSubSection.ADD_FRIEND -> AddFriendSection(
                        onSendRequest = onSendFriendRequest
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────
// قسم الإحالات
// ─────────────────────────────────────────
@Composable
fun ReferralsSection(
    userProfile: UserProfile,
    referrals: List<ReferredUser>,
    onSimulateReferralJoined: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    val referralLink = remember(userProfile.id) {
        "https://mujtamana.app/join?ref=${userProfile.id}"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("referrals_section_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // بطاقة رابط الدعوة
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        12.dp, RoundedCornerShape(18.dp),
                        ambientColor = NeonPurple.copy(alpha = 0.3f),
                        spotColor = NeonCyan.copy(alpha = 0.3f)
                    )
                    .background(DarkSurface, RoundedCornerShape(18.dp))
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "رابط الدعوة الخاص بك",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkBackground,
                            border = BorderStroke(0.6.dp, NeonPurple.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ID: ${userProfile.id}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGold
                            )
                        }
                    }

                    // حقل الرابط + زر نسخ
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkBackground,
                        border = BorderStroke(
                            1.dp,
                            if (isCopied) NeonCyan else NeonPurple.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = referralLink,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Referral Link", referralLink)
                                    clipboard.setPrimaryClip(clip)
                                    isCopied = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCopied) NeonCyan.copy(alpha = 0.15f) else NeonPurple.copy(alpha = 0.15f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCopied) NeonCyan else NeonPurple.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag("copy_referral_link_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "نسخ",
                                        tint = if (isCopied) NeonCyan else NeonPurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isCopied) "تم النسخ" else "نسخ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCopied) NeonCyan else NeonPurple
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "شارك هذا الرابط مع أصدقائك؛ عند تسجيلهم ستحصل أنت وصديقك على مكافآت ونقاط محفظة فورية.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // بطاقة عدد الإحالات
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        8.dp, RoundedCornerShape(16.dp),
                        ambientColor = NeonGold.copy(alpha = 0.25f),
                        spotColor = NeonGold.copy(alpha = 0.25f)
                    )
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, NeonGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "عدد الإحالات الناجحة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonGold.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NeonGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = NeonGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${referrals.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGold
                            )
                        }
                    }
                }
            }
        }

        // عنوان القائمة
        item {
            Text(
                text = "قائمة المنضمين عبر رابط الدعوة (${referrals.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // حالة فارغة أو القائمة
        if (referrals.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "لا توجد إحالات مسجلة حتى الآن",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "شارك رابطك في الأعلى مع معارفك وأصدقائك وستظهر أسماؤهم هنا فور انضمامهم.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            items(referrals, key = { it.id }) { ref ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(ref.avatar, fontSize = 16.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ref.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text(ref.joinedTimeAgo, fontSize = 10.sp, color = TextSecondary)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "✓ منضم",
                                fontSize = 10.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────
// قسم طلبات الصداقة
// ─────────────────────────────────────────
@Composable
fun FriendRequestsSection(
    requests: List<FriendRequest>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (requests.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "لا توجد طلبات صداقة واردة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "عندما يرسل لك مستخدم آخر طلب صداقة سيظهر هنا لتتمكن من قبوله أو رفضه.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(requests, key = { it.id }) { req ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            NeonPurple.copy(alpha = 0.3f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                        .testTag("friend_request_${req.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonPurple.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(req.senderAvatar, fontSize = 16.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.senderName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text(
                                "ID: ${req.senderHandle} • ${req.timeAgo}",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Surface(
                            onClick = { onAccept(req.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "قبول",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            onClick = { onReject(req.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = DarkBackground,
                            border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "رفض",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────
// قسم إضافة صديق
// ─────────────────────────────────────────
@Composable
fun AddFriendSection(
    onSendRequest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var friendQuery by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    12.dp, RoundedCornerShape(18.dp),
                    ambientColor = NeonCyan.copy(alpha = 0.2f),
                    spotColor = NeonPurple.copy(alpha = 0.2f)
                )
                .background(DarkSurface, RoundedCornerShape(18.dp))
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                    RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "إرسال طلب صداقة مباشر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "اكتب معرف المستخدم الرقمي (User ID) أو اسمه لإرسال طلب صداقة فوري:",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = friendQuery,
                    onValueChange = {
                        friendQuery = it
                        submitted = false
                    },
                    placeholder = {
                        Text("مثال: 84920153 أو أحمد", fontSize = 12.sp, color = TextSecondary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = NeonPurple.copy(alpha = 0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_friend_input_field")
                )
                Surface(
                    onClick = {
                        if (friendQuery.isNotBlank()) {
                            onSendRequest(friendQuery)
                            submitted = true
                            friendQuery = ""
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = NeonCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("submit_friend_request_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "إرسال طلب الصداقة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NeonCyan
                        )
                    }
                }
                if (submitted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonCyan.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "تم إرسال طلب الصداقة بنجاح إلى المستخدم!",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
