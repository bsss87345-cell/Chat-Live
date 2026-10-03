package com.example.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.ArrowForward
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ChatRoom
import com.example.model.RoomAccessType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.MujtamaPrimary
import com.example.ui.theme.MujtamaTeal
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextSecondary
/**
 * شاشة استكشاف غرف الدردشة بتصميم شبكة مربعات حديثة (Grid Cards).
 * - خلفية المربع صورة الغرفة المرفوعة من المالك مع غطاء زجاجي وتدرج أنيق
 * - عداد الأعضاء الحاليين بارز (أيقونة أشخاص + رقم)
 * - زر زجاجي شفاف بعبارة "دخول" للانضمام المباشر للغرفة
 * - ترتيب المربعات تلقائياً حسب عدد الأعضاء من الأكثر إلى الأقل مع تحديث حي
 */
// خلفية زخرفية خاصة بشاشة غرف الدردشة: خطوط شبكة + أشكال هيكسجون شفافة
@Composable
private fun RoomsNeonBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
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
}

@Composable
fun ExploreRoomsGridView(
    rooms: List<ChatRoom>,
    onOpenRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Boolean,
    onCreateRoom: (String, String, String, RoomAccessType, String?, Int, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    var passwordPromptRoom by remember { mutableStateOf<ChatRoom?>(null) }
    var roomViewFilter by remember { mutableStateOf("العامة") } // "العامة" أو "الخاص بي"
    var showSearchBar by remember { mutableStateOf(false) }
    
    // ترتيب تلقائي حسب عدد الأعضاء من الأكثر إلى الأقل (الغرف الأكثر نشاطاً أولاً)
val sortedRooms = remember(rooms, searchQuery, roomViewFilter) {
        val query = searchQuery.trim()
        val byTab = if (roomViewFilter == "الخاص بي") {
            rooms.filter { it.isJoined }
        } else {
            rooms
        }
        val filtered = if (query.isBlank()) {
            byTab
        } else {
            byTab.filter { room ->
                room.id.contains(query, ignoreCase = true) ||
                        room.id.removePrefix("room_").equals(query, ignoreCase = true) ||
                        room.inviteCode.contains(query, ignoreCase = true) ||
                        room.name.contains(query, ignoreCase = true)
            }
        }
        filtered.sortedByDescending { it.memberCount }
}

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        RoomsNeonBackground(modifier = Modifier.fillMaxSize())

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .testTag("explore_rooms_grid"),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 84.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
// شريط التبويبات والإجراءات الجديد
        item(span = { GridItemSpan(2) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isPublicSelected = roomViewFilter == "العامة"
                        Surface(
                            onClick = { roomViewFilter = "العامة" },
                            modifier = Modifier.testTag("rooms_tab_public"),
                            shape = RoundedCornerShape(50),
                            color = if (isPublicSelected) TextSecondary.copy(alpha = 0.25f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPublicSelected) NeonPurple else NeonPurple.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = "العامة",
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        val isMineSelected = roomViewFilter == "الخاص بي"
                        Surface(
                            onClick = { roomViewFilter = "الخاص بي" },
                            modifier = Modifier.testTag("rooms_tab_mine"),
                            shape = RoundedCornerShape(50),
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isMineSelected) NeonCyan else NeonCyan.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = "الخاص بي",
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = {
                                val myRoom = rooms.find { it.isOwner }
                                if (myRoom != null) {
                                    onOpenRoom(myRoom.id)
                                } else {
                                    showCreateDialog = true
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("create_room_button"),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddHome,
                                    contentDescription = "غرفتي",
                                    tint = NeonPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("rooms_search_toggle"),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "بحث",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن ID أو اسم الغرفة", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .testTag("rooms_search_input"),
                        singleLine = true
                    )
                }
            }
        }

        // إشارة ترتيب وتنشيط
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🔥", fontSize = 14.sp)
                    Text(
                        text = "مرتّبة حسب النشاط وعدد الأعضاء",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TextSecondary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${sortedRooms.size} غرفة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // حالة عدم وجود نتائج بحث
        if (sortedRooms.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(
                                    elevation = 20.dp,
                                    shape = CircleShape,
                                    ambientColor = NeonCyan,
                                    spotColor = NeonCyan
                                )
                                .clip(CircleShape)
                                .background(DarkSurface)
                                .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "لم يتم العثور على غرفة تطابق البحث",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // قائمة بطاقات الغرف (مربعات شبكية بنمط أنيق وعصري)
        items(sortedRooms, key = { it.id }) { room ->
            RoomNeonCard(
                room = room,
                onEnterClick = {
                    if (room.isJoined) {
                        onOpenRoom(room.id)
                    } else if (room.accessType == RoomAccessType.PASSWORD) {
                        passwordPromptRoom = room
                    } else {
                        onJoinRoom(room.id, "")
                        onOpenRoom(room.id)
                    }
                }
            )
        }
        }
    }

    // نافذة إدخال كلمة المرور للغرف المغلقة
    if (passwordPromptRoom != null) {
        val targetRoom = passwordPromptRoom!!
        var passInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
                onDismissRequest = { passwordPromptRoom = null },
                title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🔒")
                    Text("غرفة محمية بكلمة مرور", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل كلمة المرور للانضمام إلى '${targetRoom.name}':", fontSize = 13.sp)
                    OutlinedTextField(
                        value = passInput,
                        onValueChange = {
                            passInput = it
                            errorMessage = null
                        },
                        placeholder = { Text("كلمة المرور") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage != null
                    )
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onJoinRoom(targetRoom.id, passInput)
                        if (success) {
                            passwordPromptRoom = null
                            onOpenRoom(targetRoom.id)
                        } else {
                            errorMessage = "كلمة المرور غير صحيحة!"
                        }
                    }
                ) {
                    Text("دخول")
                }
            },
            dismissButton = {
                TextButton(onClick = { passwordPromptRoom = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // نافذة إنشاء غرفة دردشة جديدة مع إمكانية رفع صورة الغرفة
    if (showCreateDialog) {
        CreateRoomDialogWithImage(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, cat, access, pass, max, emoji, imgUrl ->
                onCreateRoom(name, desc, cat, access, pass, max, emoji, imgUrl)
                showCreateDialog = false
            }
        )
    }
}

/**
 * تصميم مربع الغرفة (Grid Card):
 * - صورة الغرفة التي يرفعها مالك الغرفة تملأ خلفية المربع
 * - عدد أعضاء الغرفة الحاليين يظهر بوضوح (رقم + أيقونة أشخاص)
 * - زر شفاف بعبارة "دخول" داخل المربع للانضمام المباشر
 * - حواف دائرية ناعمة مع تدرج حماية للقراءة
 */
@Composable
fun RoomGridCard(
    room: ChatRoom,
    onEnterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onEnterClick)
            .testTag("room_item_${room.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. خلفية المربع: صورة الغرفة المرفوعة من المالك (أو تدرج ملون بديل متناسق)
            if (!room.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = room.imageUrl,
                    contentDescription = room.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // تدرج فني مميز حسب فئة الغرفة كبديل أنيق
                val fallbackColors = when (room.category) {
                    "ألعاب" -> listOf(Color(0xFF1F1C47), Color(0xFF4338CA), Color(0xFF06B6D4))
                    "رياضة" -> listOf(Color(0xFF064E3B), Color(0xFF059669), Color(0xFF10B981))
                    "فرق" -> listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFFBBF24))
                    "ثقافة", "عام" -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569))
                    else -> listOf(MujtamaPrimary, MujtamaTeal, Color(0xFF0F172A))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(fallbackColors))
                ) {
                    // رمز مائي جمالي في الخلفية
                    Text(
                        text = room.iconEmoji,
                        fontSize = 72.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 16.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    )
                }
            }

            // 2. طبقة تدرج لوني شبه شفاف (Scrim Gradient) لضمان وضوح النصوص والأزرار بالكامل
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // 3. المحتوى الداخلي للمربع
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // الجزء العلوي: فئة الغرفة + شارة عدد الأعضاء البارزة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // فئة الغرفة وأيقونة القفل إن وجدت
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(room.iconEmoji, fontSize = 11.sp)
                            if (room.accessType == RoomAccessType.PASSWORD) {
                                Text("🔒", fontSize = 10.sp)
                            }
                            Text(
                                text = room.category,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // شارة عدد أعضاء الغرفة الحاليين (أيقونة أشخاص + رقم واضح)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MujtamaOnlineGreen.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MujtamaOnlineGreen)
                            )
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${room.memberCount}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // الجزء الأوسط: اسم الغرفة والوصف المختصر
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = room.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )

                    Text(
                        text = room.description,
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // الجزء السفلي: زر زجاجي شفاف بعبارة "دخول" للانضمام المباشر
                Surface(
                    onClick = onEnterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("room_enter_btn_${room.id}"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.20f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.25f)
                            )
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (room.isJoined) "دخول" else "دخول الغرفة",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Login,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
/**
 * بطاقة غرفة بتصميم النيون الجديد (غير مستخدمة بعد).
 */
@Composable
fun RoomNeonCard(
    room: ChatRoom,
    onEnterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(18.dp)
    val borderBrush = Brush.verticalGradient(
        listOf(NeonCyan, NeonPurple, Color(0xFFFF2BFF))
    )
    val iconBrush = Brush.linearGradient(listOf(NeonCyan, NeonPurple))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(224.dp)
            .testTag("room_item_${room.id}")
    ) {
        // البطاقة الخلفية المزاحة
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp, bottom = 6.dp)
                .clip(cardShape)
                .background(Color(0xFF0A0D10))
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), cardShape)
        )

        // البطاقة الأمامية
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 8.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = cardShape,
                    ambientColor = NeonCyan,
                    spotColor = NeonPurple
                )
                .clip(cardShape)
                .background(Color(0xFF0B0F12))
                .border(1.5.dp, borderBrush, cardShape)
                .clickable(onClick = onEnterClick)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // كبسولة النوع
                    Row(
                        modifier = Modifier
                            .background(NeonCyan.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .border(1.5.dp, NeonCyan, RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = room.category,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (room.accessType == RoomAccessType.PASSWORD) {
                            Text("🔒", fontSize = 10.sp)
                        }
                    }

                    // عدد المتصلين + النقطة الخضراء
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MujtamaOnlineGreen)
                        )
                        Text(
                            text = "${room.memberCount}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // الوسط: صورة الغرفة إن وجدت، وإلا أيقونة الفقاعة
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (room.imageUrl.isNullOrBlank()) {
                        Canvas(modifier = Modifier.size(width = 64.dp, height = 58.dp)) {
                            val w = size.width
                            val h = size.height
                            val p = 4.dp.toPx()
                            val bodyH = h * 0.78f
                            val thin = 2.dp.toPx()
                            val outerSize = Size(w - 2 * p, bodyH - p)

                            drawRoundRect(
                                color = NeonCyan.copy(alpha = 0.18f),
                                topLeft = Offset(p, p),
                                size = outerSize,
                                cornerRadius = CornerRadius(12.dp.toPx()),
                                style = Stroke(width = 7.dp.toPx())
                            )
                            drawRoundRect(
                                brush = iconBrush,
                                topLeft = Offset(p, p),
                                size = outerSize,
                                cornerRadius = CornerRadius(12.dp.toPx()),
                                style = Stroke(width = thin)
                            )
                            val gap = 5.dp.toPx()
                            drawRoundRect(
                                brush = iconBrush,
                                topLeft = Offset(p + gap, p + gap),
                                size = Size(outerSize.width - 2 * gap, outerSize.height - 2 * gap),
                                cornerRadius = CornerRadius(8.dp.toPx()),
                                style = Stroke(width = thin * 0.75f)
                            )
                            val tail = Path().apply {
                                moveTo(w * 0.18f, bodyH)
                                lineTo(w * 0.10f, h - p)
                                lineTo(w * 0.34f, bodyH)
                            }
                            drawPath(path = tail, brush = iconBrush, style = Stroke(width = thin))
                            drawLine(
                                brush = iconBrush,
                                start = Offset(w * 0.28f, bodyH * 0.40f),
                                end = Offset(w * 0.72f, bodyH * 0.40f),
                                strokeWidth = thin,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                brush = iconBrush,
                                start = Offset(w * 0.28f, bodyH * 0.60f),
                                end = Offset(w * 0.56f, bodyH * 0.60f),
                                strokeWidth = thin,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // اسم الغرفة
                Text(
                    text = room.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // زر الدخول
                Surface(
                    onClick = onEnterClick,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(32.dp)
                        .testTag("room_enter_btn_${room.id}"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFF00FF)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (room.isJoined) "دخول" else "انضم للغرفة",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * نافذة إنشاء الغرفة تتيح لمالك الغرفة اختيار ورفع صورة الغرفة
 * سواء من وسائط الجهاز (Photo Picker) أو من نماذج الصور السريعة.
 */
@Composable
fun CreateRoomDialogWithImage(
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        description: String,
        category: String,
        accessType: RoomAccessType,
        password: String?,
        maxMembers: Int,
        iconEmoji: String,
        imageUrl: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    // القيم الافتراضية الثابتة بعد حذف الحقول من الواجهة
    val description = ""
    val category = "عام"
    val accessType = RoomAccessType.PUBLIC
    val password: String? = null
    val maxMembersText = "100"
    val selectedEmoji = "💬"

    // لاقط الصور بدون أذونات خارجية (Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                selectedImageUrl = uri.toString()
            }
        }
    )

    AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = DarkSurface.copy(alpha = 0.85f),
            modifier = Modifier
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = NeonCyan,
                    spotColor = NeonPurple
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                    shape = RoundedCornerShape(24.dp)
                ),
            title = {
            Text(
                "إنشاء غرفة دردشة جديدة 🎙️",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // اسم الغرفة
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الغرفة", color = TextSecondary) },
                        placeholder = { Text("مثال: رواد التقنية والبرمجة", color = TextSecondary.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = NeonPurple.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = NeonCyan
                        )
                    )
                }

                // رفع / اختيار صورة الغرفة التي تملأ خلفية المربع
                item {
                    Text("صورة خلفية الغرفة 🖼️:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .border(
                                width = 1.dp,
                                color = NeonCyan,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!selectedImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = selectedImageUrl,
                                contentDescription = "معاينة صورة الغرفة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text("تغيير الصورة ✏️", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                                Text("اضغط لرفع صورة الغرفة من جهازك", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Surface(
                onClick = {
                    val maxCount = maxMembersText.toIntOrNull() ?: 100
                    onCreate(
                        name,
                        description,
                        category,
                        accessType,
                        password,
                        maxCount,
                        selectedEmoji,
                        selectedImageUrl
                    )
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(50),
                color = if (name.isNotBlank()) NeonCyan else DarkBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = if (name.isNotBlank()) 1f else 0.4f))
            ) {
                Text(
                    "إنشاء الغرفة",
                    color = if (name.isNotBlank()) Color.Black else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        },
        dismissButton = {
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple)
            ) {
                Text(
                    "إلغاء",
                    color = NeonPurple,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    )
}
