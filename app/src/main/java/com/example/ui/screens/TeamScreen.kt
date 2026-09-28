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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.MujtamaPrimary
import com.example.ui.theme.MujtamaTeal

/**
 * الأقسام الرئيسية لقسم الفريق:
 * 1- الإحالات
 * 2- طلبات الصداقة
 * 3- إضافة صديق
 */
enum class TeamSubSection(val titleAr: String, val iconEmoji: String) {
    REFERRALS("الإحالات", "🔗"),
    FRIEND_REQUESTS("طلبات الصداقة", "👥"),
    ADD_FRIEND("إضافة صديق", "➕")
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("team_screen_container")
    ) {
        // --- 1. الشريط العلوي مع سهم الرجوع في الزاوية اليمنى مع RTL ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // زر سهم الرجوع شبه الشفاف والمدمج في الزاوية
                Surface(
                    onClick = onBackClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("team_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        // في واجهة RTL سهم ArrowForward يشير إلى اليمين (الرجوع للخلف)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // عنوان القسم وشارة الفريق
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "قسم الفريق والمجتمع",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MujtamaGold.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "🛡️ ${team.name}",
                                color = MujtamaGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "إدارة العلاقات، الإحالات، ورسائل الأصدقاء",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // --- 2. شريط الأقسام الأربعة بتصميم شبه شفاف وزجاجي متناسق مع الواجهة ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TeamSubSection.values().forEach { section ->
                val isSelected = activeSubSection == section

                Surface(
                    onClick = { activeSubSection = section },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("team_tab_${section.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MujtamaPrimary.copy(alpha = 0.20f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MujtamaPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = section.iconEmoji,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = section.titleAr,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MujtamaPrimary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // --- 3. عرض المحتوى المخصص حسب القسم النشط ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = activeSubSection,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "team_section_transition"
            ) { section ->
                when (section) {
                    TeamSubSection.REFERRALS -> {
                        ReferralsSection(
                            userProfile = userProfile,
                            referrals = referrals,
                            onSimulateReferralJoined = onSimulateReferralJoined
                        )
                    }
                    TeamSubSection.FRIEND_REQUESTS -> {
                        FriendRequestsSection(
                            requests = friendRequests,
                            onAccept = onAcceptFriendRequest,
                            onReject = onRejectFriendRequest
                        )
                    }
                    TeamSubSection.ADD_FRIEND -> {
                        AddFriendSection(
                            onSendRequest = onSendFriendRequest
                        )
                    }
                }
            }
        }
    }
}

/**
 * محتوى قسم "الإحالات":
 * 1. رابط الإحالة متضمناً ID المستخدم الفعلي مع زر نسخ
 * 2. أسفله مباشرة يظهر عدد الإحالات (رقم بارز)
 * 3. أسفل رابط الدعوة تظهر قائمة بأسماء المستخدمين الذين انضموا عبر رابط الدعوة
 * مع عرض حالة فارغة حقيقية (Empty State) بدون أي بيانات وهمية.
 */
@Composable
fun ReferralsSection(
    userProfile: UserProfile,
    referrals: List<ReferredUser>,
    onSimulateReferralJoined: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }
    var testFriendName by remember { mutableStateOf("") }

    // رابط الإحالة مع ID المستخدم الحقيقي
    val referralLink = remember(userProfile.id) {
        "https://mujtamana.app/join?ref=${userProfile.id}"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("referrals_section_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // بطاقة رابط الدعوة مع ID المستخدم وزر النسخ
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("referral_link_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🔗", fontSize = 16.sp)
                            Text(
                                text = "رابط الدعوة الخاص بك",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // إظهار ID المستخدم بوضوح
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.6.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = "ID: ${userProfile.id}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // حقل الرابط الشفاف مع زر النسخ الشفاف
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCopied) MujtamaOnlineGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
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
                                tint = MujtamaTeal,
                                modifier = Modifier.size(18.dp)
                            )

                            Text(
                                text = referralLink,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // زر نسخ شفاف زجاجي
                            Surface(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Mujtamana Referral Link", referralLink)
                                    clipboard.setPrimaryClip(clip)
                                    isCopied = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCopied) MujtamaOnlineGreen.copy(alpha = 0.20f) else MujtamaPrimary.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCopied) MujtamaOnlineGreen else MujtamaPrimary.copy(alpha = 0.40f)
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
                                        tint = if (isCopied) MujtamaOnlineGreen else MujtamaPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isCopied) "تم النسخ" else "نسخ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCopied) MujtamaOnlineGreen else MujtamaPrimary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "شارك هذا الرابط مع أصدقائك؛ عند تسجيلهم ستحصل أنت وصديقك على مكافآت ونقاط محفظة فورية.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // بطاقة عدد الإحالات الحالي (رقم بارز وواضح)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("referrals_count_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "عدد الإحالات الناجحة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "الأصدقاء الذين انضموا بالفعل عبر كودك",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MujtamaGold.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MujtamaGold.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MujtamaGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${referrals.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MujtamaGold
                            )
                        }
                    }
                }
            }
        }

        // عنوان قائمة المنضمين
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قائمة المنضمين عبر رابط الدعوة (${referrals.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }

        // قائمة بأسماء المستخدمين الذين انضموا عبر رابط الدعوة (حقيقية بدون بيانات وهمية)
        if (referrals.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("referrals_empty_state"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📭", fontSize = 32.sp)
                        Text(
                            text = "لا توجد إحالات مسجلة حتى الآن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "شارك رابطك في الأعلى مع معارفك وأصدقائك وستظهر أسماؤهم هنا فور انضمامهم.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        // زر عملي لتجربة انضمام صديق فعلي
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = testFriendName,
                                onValueChange = { testFriendName = it },
                                placeholder = { Text("اكتب اسم صديق للتجربة...", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_referral_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Surface(
                                onClick = {
                                    onSimulateReferralJoined(testFriendName)
                                    testFriendName = ""
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MujtamaTeal.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MujtamaTeal),
                                modifier = Modifier.testTag("simulate_referral_btn")
                            ) {
                                Text(
                                    text = "تسجيل تجريبي",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MujtamaTeal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            items(referrals, key = { it.id }) { user ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("referred_user_${user.id}"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // أفاتار شبه شفاف
                        Surface(
                            shape = CircleShape,
                            color = MujtamaPrimary.copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MujtamaPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(user.avatarEmoji, fontSize = 16.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "ID: ${user.handle} • انضم: ${user.joinedDate}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MujtamaOnlineGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, MujtamaOnlineGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "+50 نقطة",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MujtamaOnlineGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * محتوى قسم "طلبات الصداقة":
 * عرض طلبات الصداقة المعلقة مع أزرار قبول/رفض شفافة وزجاجية.
 */
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
                Text("👥", fontSize = 36.sp)
                Text("لا توجد طلبات صداقة واردة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "عندما يرسل لك مستخدم آخر طلب صداقة، سيظهر هنا لتتمكن من قبوله أو رفضه.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(requests, key = { it.id }) { req ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_request_${req.id}")
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
                            color = MujtamaPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(req.senderAvatar, fontSize = 16.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.senderName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "ID: ${req.senderHandle} • ${req.timeAgo}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // زر قبول شفاف زجاجي
                        Surface(
                            onClick = { onAccept(req.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = MujtamaOnlineGreen.copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MujtamaOnlineGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "قبول",
                                color = MujtamaOnlineGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // زر رفض شفاف
                        Surface(
                            onClick = { onReject(req.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.6.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "رفض",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/**
 * محتوى قسم "إضافة صديق":
 * إرسال طلب صداقة فوري بالبحث عن اسم أو معرّف الصديق (User ID / Handle).
 */
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("➕", fontSize = 18.sp)
                    Text(
                        text = "إرسال طلب صداقة مباشر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "اكتب معرف المستخدم الرقمي (User ID) أو اسمه لإرسال طلب صداقة فوري:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = friendQuery,
                    onValueChange = {
                        friendQuery = it
                        submitted = false
                    },
                    placeholder = { Text("مثال: 84920153 أو أحمد", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_friend_input_field")
                )

                // زر شفاف زجاجي للإرسال
                Surface(
                    onClick = {
                        if (friendQuery.isNotBlank()) {
                            onSendRequest(friendQuery)
                            submitted = true
                            friendQuery = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MujtamaPrimary.copy(alpha = 0.20f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MujtamaPrimary.copy(alpha = 0.60f)
                    ),
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
                            tint = MujtamaPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إرسال طلب الصداقة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MujtamaPrimary
                        )
                    }
                }

                if (submitted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MujtamaOnlineGreen.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("✅", fontSize = 12.sp)
                            Text(
                                text = "تم إرسال طلب الصداقة بنجاح إلى المستخدم!",
                                color = MujtamaOnlineGreen,
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
