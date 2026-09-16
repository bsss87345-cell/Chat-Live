package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.ui.theme.MujtamaGold
import com.example.ui.theme.MujtamaOnlineGreen
import com.example.ui.theme.MujtamaPrimary
import com.example.ui.theme.MujtamaTeal

/**
 * شاشة استكشاف غرف الدردشة بتصميم شبكة مربعات حديثة (Grid Cards).
 * - خلفية المربع صورة الغرفة المرفوعة من المالك مع غطاء زجاجي وتدرج أنيق
 * - عداد الأعضاء الحاليين بارز (أيقونة أشخاص + رقم)
 * - زر زجاجي شفاف بعبارة "دخول" للانضمام المباشر للغرفة
 * - ترتيب المربعات تلقائياً حسب عدد الأعضاء من الأكثر إلى الأقل مع تحديث حي
 */
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
    val sortedRooms = remember(rooms, searchQuery) {
        val query = searchQuery.trim()
        val filtered = if (query.isBlank()) {
            rooms
        } else {
            rooms.filter { room ->
                room.id.contains(query, ignoreCase = true) ||
                        room.id.removePrefix("room_").equals(query, ignoreCase = true) ||
                        room.inviteCode.contains(query, ignoreCase = true) ||
                        room.name.contains(query, ignoreCase = true)
            }
        }
        filtered.sortedByDescending { it.memberCount }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_rooms_grid"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 84.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // شريط الإجراءات والبحث (يمتد على كامل العرض)
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        .weight(1f)
                        .testTag("rooms_search_input"),
                    singleLine = true
                )

                // زر إنشاء غرفة دردشة جديدة
                FilledTonalButton(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MujtamaPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("create_room_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إنشاء", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // زر الانضمام عبر كود دعوة
                IconButton(
                    onClick = { showJoinByCodeDialog = true },
                    modifier = Modifier.testTag("join_by_code_button")
                ) {
                    Icon(Icons.Default.Link, contentDescription = "رمز دعوة", tint = MujtamaTeal)
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
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${sortedRooms.size} غرفة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔍", fontSize = 32.sp)
                        Text(
                            text = "لم يتم العثور على غرفة تطابق البحث",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // قائمة بطاقات الغرف (مربعات شبكية بنمط أنيق وعصري)
        items(sortedRooms, key = { it.id }) { room ->
            RoomGridCard(
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

    // نافذة الانضمام عبر رمز الدعوة
    if (showJoinByCodeDialog) {
        var inviteCodeInput by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showJoinByCodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("✉️")
                    Text("الانضمام برمز الدعوة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل رمز الدعوة المخصص للغرفة الخاصة:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = {
                            inviteCodeInput = it
                            errorMsg = null
                        },
                        placeholder = { Text("مثال: GAME-2026") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = inviteCodeInput.trim()
                        val found = rooms.find { it.inviteCode.equals(trimmed, ignoreCase = true) }
                        if (found != null) {
                            onJoinRoom(found.id, "")
                            onOpenRoom(found.id)
                            showJoinByCodeDialog = false
                        } else {
                            errorMsg = "رمز الدعوة غير صحيح أو الغرفة غير موجودة!"
                        }
                    }
                ) {
                    Text("انضمام")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinByCodeDialog = false }) {
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
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("عام") }
    var accessType by remember { mutableStateOf(RoomAccessType.PUBLIC) }
    var password by remember { mutableStateOf("") }
    var maxMembersText by remember { mutableStateOf("100") }
    var selectedEmoji by remember { mutableStateOf("💬") }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

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
        title = { Text("إنشاء غرفة دردشة جديدة 🎙️", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
                        label = { Text("اسم الغرفة") },
                        placeholder = { Text("مثال: رواد التقنية والبرمجة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // رفع / اختيار صورة الغرفة التي تملأ خلفية المربع
                item {
                    Text("صورة خلفية الغرفة 🖼️:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    Text("اضغط لرفع صورة الغرفة من جهازك", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // نماذج صور سريعة مقترحة لسهولة التجربة
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("أو اختر نموذجاً سريعاً:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presets = listOf(
                            "🎮 ألعاب" to "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=800&auto=format&fit=crop",
                            "⚽ رياضة" to "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=800&auto=format&fit=crop",
                            "🛡️ فرق" to "https://images.unsplash.com/photo-1550745165-9bc0b252726f?q=80&w=800&auto=format&fit=crop",
                            "📚 ثقافة" to "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?q=80&w=800&auto=format&fit=crop",
                            "💡 تقنية" to "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=800&auto=format&fit=crop"
                        )
                        items(presets) { (title, url) ->
                            FilterChip(
                                selected = selectedImageUrl == url,
                                onClick = { selectedImageUrl = url },
                                label = { Text(title, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // وصف الغرفة
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("وصف مختصر للغرفة") },
                        placeholder = { Text("عن ماذا تتحدث هذه الغرفة؟") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                // التصنيف والاهتمام
                item {
                    Text("التصنيف:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("ألعاب", "رياضة", "فرق", "عام", "تقنية", "ثقافة")) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // نوع الدخول
                item {
                    Text("نوع الوصول:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = accessType == RoomAccessType.PUBLIC,
                            onClick = { accessType = RoomAccessType.PUBLIC },
                            label = { Text("عامة 🌐", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = accessType == RoomAccessType.PASSWORD,
                            onClick = { accessType = RoomAccessType.PASSWORD },
                            label = { Text("بكلمة سر 🔒", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = accessType == RoomAccessType.INVITE_ONLY,
                            onClick = { accessType = RoomAccessType.INVITE_ONLY },
                            label = { Text("دعوة ✉️", fontSize = 11.sp) }
                        )
                    }
                }

                if (accessType == RoomAccessType.PASSWORD) {
                    item {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور المطلوبة للدخول") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxCount = maxMembersText.toIntOrNull() ?: 100
                    onCreate(
                        name,
                        description,
                        category,
                        accessType,
                        password.ifBlank { null },
                        maxCount,
                        selectedEmoji,
                        selectedImageUrl
                    )
                },
                enabled = name.isNotBlank() && description.isNotBlank()
            ) {
                Text("إنشاء الغرفة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
