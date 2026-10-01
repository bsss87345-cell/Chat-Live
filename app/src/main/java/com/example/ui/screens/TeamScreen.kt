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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
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
import com.example.ui.theme.MujtamaOnlineGreen
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.MujtamaPrimary

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
fun NeonGlowBox(
    glowColor: Color,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    fill: Color = Color.Transparent,
    borderColor: Color = glowColor,
    borderWidth: Dp = 1.5.dp,
    outerReach: Dp = 12.dp,
    outerAlpha: Float = 0.26f,
    innerReach: Dp = 10.dp,
    innerAlpha: Float = 0.22f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val r = shape.topStart.toPx(size, this)
            val bw = borderWidth.toPx()

            // 1) توهج خارجي متدرج
            val outerN = 12
            val outerStep = outerReach.toPx() / outerN
            for (i in 0 until outerN) {
                val t = (i + 0.5f) / outerN
                val off = outerStep * (i + 0.5f)
                drawRoundRect(
                    color = glowColor.copy(alpha = outerAlpha * (1f - t) * (1f - t)),
                    topLeft = Offset(-off, -off),
                    size = Size(w + off * 2f, h + off * 2f),
                    cornerRadius = CornerRadius(r + off),
                    style = Stroke(width = outerStep)
                )
            }

            // 2) تعبئة الخلفية
            drawRoundRect(color = fill, cornerRadius = CornerRadius(r))

            // 3) توهج داخلي متدرج
            val innerN = 10
            val innerStep = innerReach.toPx() / innerN
            for (i in 0 until innerN) {
                val t = (i + 0.5f) / innerN
                val inset = innerStep * (i + 0.5f)
                val rw = w - inset * 2f
                val rh = h - inset * 2f
                if (rw > 0f && rh > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = innerAlpha * (1f - t) * (1f - t)),
                        topLeft = Offset(inset, inset),
                        size = Size(rw, rh),
                        cornerRadius = CornerRadius((r - inset).coerceAtLeast(0f)),
                        style = Stroke(width = innerStep)
                    )
                }
            }

            // 4) الحد الرفيع
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(bw / 2f, bw / 2f),
                size = Size(w - bw, h - bw),
                cornerRadius = CornerRadius((r - bw / 2f).coerceAtLeast(0f)),
                style = Stroke(width = bw)
            )
        }
        Box(
            modifier = Modifier
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
            content = content
        )
    }
}

/** خلفية الشاشة: شبكة خفيفة + سداسيات نيون (نفس خلفية بقية الأقسام) */
private fun DrawScope.drawTeamNeonBackground() {
    val gridColor = TextSecondary.copy(alpha = 0.05f)
    val step = 40.dp.toPx()

    var x = 0f
    while (x < size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }

    fun hexPath(cx: Float, cy: Float, r: Float): Path {
        return Path().apply {
            for (i in 0..5) {
                val angle = Math.toRadians((60 * i - 30).toDouble())
                val px = cx + r * kotlin.math.cos(angle).toFloat()
                val py = cy + r * kotlin.math.sin(angle).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
    }

    val hexColor = NeonCyan.copy(alpha = 0.08f)
    drawPath(hexPath(size.width * 0.85f, size.height * 0.12f, 60.dp.toPx()), color = hexColor, style = Stroke(width = 2f))
    drawPath(hexPath(size.width * 0.1f, size.height * 0.55f, 45.dp.toPx()), color = hexColor, style = Stroke(width = 2f))
    drawPath(hexPath(size.width * 0.2f, size.height * 0.65f, 30.dp.toPx()), color = hexColor, style = Stroke(width = 2f))
}

private val TeamGlowCyan = Color(0xFF37EBEC)
private val TeamGlowPurple = Color(0xFFAF8FF0)
private val TeamGlowMagenta = Color(0xFFCF42E9)
private val TeamGlowGold = Color(0xFFF4D899)

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
            .background(DarkBackground)
            .drawBehind { drawTeamNeonBackground() }
            .testTag("team_screen_container")
    ) {
        // --- 1. الهيدر: زر الرجوع (نيون) ثم العنوان ثم الوصف ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp)
        ) {
            NeonGlowBox(
                glowColor = TeamGlowCyan,
                shape = RoundedCornerShape(12.dp),
                fill = TeamGlowCyan.copy(alpha = 0.10f),
                borderColor = Color(0xFF45EAEE),
                outerReach = 8.dp,
                outerAlpha = 0.22f,
                innerReach = 8.dp,
                innerAlpha = 0.18f,
                onClick = onBackClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("team_back_button")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "رجوع",
                        tint = Color(0xFF45EAEE),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "قسم الفريق والمجتمع",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "إدارة العلاقات، الإحالات، ورسائل الأصدقاء",
                color = Color(0xFFBFBFC1),
                fontSize = 18.sp
            )
        }

        // --- 2. أزرار الأقسام الثلاثة (نيون): إضافة صديق، طلبات الصداقة، الإحالات ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TeamSubSection.values().reversed().forEach { section ->
                val isSelected = activeSubSection == section
                val glow = when (section) {
                    TeamSubSection.ADD_FRIEND -> TeamGlowCyan
                    TeamSubSection.FRIEND_REQUESTS -> TeamGlowPurple
                    TeamSubSection.REFERRALS -> TeamGlowMagenta
                }
                val sectionIcon = when (section) {
                    TeamSubSection.ADD_FRIEND -> Icons.Default.Add
                    TeamSubSection.FRIEND_REQUESTS -> Icons.Default.Group
                    TeamSubSection.REFERRALS -> Icons.Default.Link
                }
                val sectionWeight = when (section) {
                    TeamSubSection.ADD_FRIEND -> 185f
                    TeamSubSection.FRIEND_REQUESTS -> 205f
                    TeamSubSection.REFERRALS -> 167f
                }
                val baseFill = when (section) {
                    TeamSubSection.ADD_FRIEND -> 0.18f
                    TeamSubSection.FRIEND_REQUESTS -> 0.30f
                    TeamSubSection.REFERRALS -> 0.20f
                }

                NeonGlowBox(
                    glowColor = glow,
                    shape = RoundedCornerShape(12.dp),
                    fill = glow.copy(alpha = baseFill + (if (isSelected) 0.08f else 0f)),
                    onClick = { activeSubSection = section },
                    modifier = Modifier
                        .weight(sectionWeight)
                        .height(40.dp)
                        .testTag("team_tab_${section.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = sectionIcon,
                            contentDescription = null,
                            tint = glow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = section.titleAr,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = glow,
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
            NeonGlowBox(
                glowColor = Color(0xFFB44DF0),
                shape = RoundedCornerShape(18.dp),
                fill = Color(0xFF201E2B),
                borderColor = Color(0xFFD392F5),
                outerReach = 10.dp,
                outerAlpha = 0.20f,
                innerReach = 14.dp,
                innerAlpha = 0.35f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("referral_link_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "رابط الدعوة الخاص بك",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "ID: ${userProfile.id}",
                            color = Color(0xFF25EEF5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // حقل الرابط: زر النسخ يمين والرابط يسار
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0B0C12),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCopied) MujtamaOnlineGreen else Color(0xFF3A3D47)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeonGlowBox(
                                glowColor = if (isCopied) MujtamaOnlineGreen else Color(0xFF3FC7DD),
                                shape = RoundedCornerShape(10.dp),
                                fill = if (isCopied) MujtamaOnlineGreen.copy(alpha = 0.18f) else Color(0xFF1A2B38),
                                outerReach = 6.dp,
                                outerAlpha = 0.15f,
                                innerReach = 6.dp,
                                innerAlpha = 0.12f,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Mujtamana Referral Link", referralLink)
                                    clipboard.setPrimaryClip(clip)
                                    isCopied = true
                                },
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("copy_referral_link_btn")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isCopied) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MujtamaOnlineGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isCopied) "تم النسخ" else "نسخ",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCopied) MujtamaOnlineGreen else Color(0xFF3FC7DD)
                                    )
                                }
                            }

                            Text(
                                text = referralLink,
                                fontSize = 12.sp,
                                color = Color(0xFFB8B9BE),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = "شارك هذا الرابط مع أصدقائك؛ عند تسجيلهم ستحصل أنت وصديقك على مكافآت ونقاط محفظة فورية.",
                        fontSize = 14.sp,
                        color = Color(0xFFBCBDC2),
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // بطاقة عدد الإحالات الحالي (رقم بارز وواضح)
        item {
            NeonGlowBox(
                glowColor = TeamGlowGold,
                shape = RoundedCornerShape(16.dp),
                fill = Color(0xFF2D2E28),
                outerReach = 10.dp,
                outerAlpha = 0.14f,
                innerReach = 12.dp,
                innerAlpha = 0.22f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("referrals_count_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "عدد الإحالات الناجحة",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    NeonGlowBox(
                        glowColor = TeamGlowGold,
                        shape = RoundedCornerShape(12.dp),
                        fill = Color(0xFF54472C),
                        borderColor = Color(0xFFF7DC99),
                        outerReach = 5.dp,
                        outerAlpha = 0.14f,
                        innerReach = 6.dp,
                        innerAlpha = 0.16f
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Color(0xFFFFE6AD),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "${referrals.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFFDDB)
                            )
                        }
                    }
                }
            }
        }

        // عنوان قائمة المنضمين
        item {
            Text(
                text = "قائمة المنضمين عبر رابط الدعوة (${referrals.size})",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }
        // قائمة بأسماء المستخدمين الذين انضموا عبر رابط الدعوة (حقيقية بدون بيانات وهمية)
        if (referrals.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("referrals_empty_state"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // أيقونة نيون مع توهج دائري ناعم خلفها
                    Box(
                        modifier = Modifier.size(84.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF4AD7E9).copy(alpha = 0.30f),
                                        Color.Transparent
                                    )
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = null,
                            tint = Color(0xFF4AD7E9),
                            modifier = Modifier.size(68.dp)
                        )
                    }
                    Text(
                        text = "لا توجد إحالات مسجلة حتى الآن",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "شارك رابطك في الأعلى مع معارفك وأصدقائك وستظهر أسماؤهم هنا فور انضمامهم.",
                        fontSize = 15.sp,
                        color = Color(0xFFBDBEC0),
                        textAlign = TextAlign.Center,
                        lineHeight = 23.sp
                    )
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
                                color = Color.White,
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
 * عرض طلبات الصداقة المعلقة بتصميم نيون بنفسجي مع أزرار قبول/رفض.
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
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    TeamGlowPurple.copy(alpha = 0.30f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = TeamGlowPurple,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Text(
                    text = "لا توجد طلبات صداقة واردة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "عندما يرسل لك مستخدم آخر طلب صداقة، سيظهر هنا لتتمكن من قبوله أو رفضه.",
                    fontSize = 15.sp,
                    color = Color(0xFFBDBEC0),
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(requests, key = { it.id }) { req ->
                NeonGlowBox(
                    glowColor = TeamGlowPurple,
                    shape = RoundedCornerShape(16.dp),
                    fill = Color(0xFF1F1E2E),
                    outerReach = 10.dp,
                    outerAlpha = 0.18f,
                    innerReach = 12.dp,
                    innerAlpha = 0.28f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_request_${req.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TeamGlowPurple.copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                TeamGlowPurple.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(req.senderAvatar, fontSize = 18.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = req.senderName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "ID: ${req.senderHandle} • ${req.timeAgo}",
                                fontSize = 12.sp,
                                color = Color(0xFFBCBDC2),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // زر قبول
                        NeonGlowBox(
                            glowColor = MujtamaOnlineGreen,
                            shape = RoundedCornerShape(10.dp),
                            fill = MujtamaOnlineGreen.copy(alpha = 0.18f),
                            outerReach = 6.dp,
                            outerAlpha = 0.15f,
                            innerReach = 6.dp,
                            innerAlpha = 0.12f,
                            onClick = { onAccept(req.id) },
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قبول",
                                    color = MujtamaOnlineGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // زر رفض
                        NeonGlowBox(
                            glowColor = Color(0xFF8A8D99),
                            shape = RoundedCornerShape(10.dp),
                            fill = Color(0xFF14151B),
                            borderColor = Color(0xFF3A3D47),
                            outerReach = 4.dp,
                            outerAlpha = 0.08f,
                            innerReach = 4.dp,
                            innerAlpha = 0.06f,
                            onClick = { onReject(req.id) },
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "رفض",
                                    color = Color(0xFFBCBDC2),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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
            .padding(16.dp)
    ) {
        NeonGlowBox(
            glowColor = TeamGlowCyan,
            shape = RoundedCornerShape(18.dp),
            fill = Color(0xFF1A2428),
            outerReach = 10.dp,
            outerAlpha = 0.18f,
            innerReach = 14.dp,
            innerAlpha = 0.28f,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = TeamGlowCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "إرسال طلب صداقة مباشر",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                Text(
                    text = "اكتب معرف المستخدم الرقمي (User ID) أو اسمه لإرسال طلب صداقة فوري:",
                    fontSize = 14.sp,
                    color = Color(0xFFBCBDC2),
                    lineHeight = 22.sp
                )

                OutlinedTextField(
                    value = friendQuery,
                    onValueChange = {
                        friendQuery = it
                        submitted = false
                    },
                    placeholder = {
                        Text(
                            text = "مثال: 84920153 أو أحمد",
                            fontSize = 13.sp,
                            color = Color(0xFF8A8B93)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = TeamGlowCyan,
                        focusedBorderColor = TeamGlowCyan,
                        unfocusedBorderColor = Color(0xFF3A3D47),
                        focusedContainerColor = Color(0xFF0B0C12),
                        unfocusedContainerColor = Color(0xFF0B0C12)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_friend_input_field")
                )

                // زر الإرسال (نيون سماوي)
                NeonGlowBox(
                    glowColor = TeamGlowCyan,
                    shape = RoundedCornerShape(12.dp),
                    fill = TeamGlowCyan.copy(alpha = 0.18f),
                    onClick = {
                        if (friendQuery.isNotBlank()) {
                            onSendRequest(friendQuery)
                            submitted = true
                            friendQuery = ""
                        }
                    },
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
                            tint = TeamGlowCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إرسال طلب الصداقة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TeamGlowCyan
                        )
                    }
                }

                if (submitted) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MujtamaOnlineGreen.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MujtamaOnlineGreen.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MujtamaOnlineGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تم إرسال طلب الصداقة بنجاح إلى المستخدم!",
                                color = MujtamaOnlineGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
