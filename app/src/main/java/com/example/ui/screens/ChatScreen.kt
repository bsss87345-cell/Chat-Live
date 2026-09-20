package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.theme.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.net.Uri
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversations: List<ChatConversation>,
    activeChatId: String?,
    chatFilter: String,
    pushNotificationsEnabled: Boolean,
    chatSubTab: String,
    chatRooms: List<ChatRoom>,
    activeRoomId: String?,
    roomCategoryFilter: String,
    onSubTabChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onTogglePushNotifications: () -> Unit,
    onOpenChat: (String) -> Unit,
    onCloseChat: () -> Unit,
    onSendMessage: (String, String, ChatMessageType) -> Unit,
    onStartGameInChat: (String, GameType) -> Unit,
    onNavigateToGames: (GameType?) -> Unit,
    onRoomCategoryChange: (String) -> Unit,
    onOpenRoom: (String) -> Unit,
    onCloseRoom: () -> Unit,
    onCreateRoom: (String, String, String, RoomAccessType, String?, Int, String, String?) -> Unit,
    onJoinRoom: (String, String) -> Boolean,
    onLeaveRoom: (String) -> Unit,
    onSendRoomMessage: (String, String, ChatMessageType) -> Unit,
    onPinRoomMessage: (String, String) -> Unit,
    onUnpinRoomMessage: (String) -> Unit,
    onDeleteRoomMessage: (String, String) -> Unit,
    onKickRoomMember: (String, String) -> Unit,
    onMuteRoomMember: (String, String) -> Unit,
    onChangeMemberRole: (String, String, RoomMemberRole) -> Unit,
    onUpdateRoomSettings: (String, String, String, Int) -> Unit,
    onStartInRoomGame: (String, GameType) -> Unit,
    onToggleLock: (String) -> Unit,
    onUpdateBackground: (String, String) -> Unit,
    onBlockMember: (String, String) -> Unit,
    onUnblockMember: (String, String) -> Unit,
    onRequestVoiceSeat: (Int) -> Unit = {},
    onRespondVoiceSeatRequest: (String, Boolean) -> Unit = { _, _ -> },
    onLeaveVoiceSeat: (Int) -> Unit = {},
    onMuteVoiceSeat: (Int) -> Unit = {},
    onToggleOwnerVoiceMute: () -> Unit = {},
    onTakeVoiceSeat: (Int) -> Unit = {},
    myAvatarUrl: String = ""
) {
    // 1. If an active direct conversation is open
    if (activeChatId != null) {
        val activeConv = conversations.find { it.id == activeChatId }
        if (activeConv != null) {
            ChatConversationView(
                conversation = activeConv,
                onBack = onCloseChat,
                onSendMessage = { text, type -> onSendMessage(activeConv.id, text, type) },
                onStartGame = { gameType -> onStartGameInChat(activeConv.id, gameType) },
                onPlayGameDirectly = onNavigateToGames,
                myAvatarUrl = myAvatarUrl
            )
            return
        }
    }

    // 2. If an active Chat Room is open
    if (activeRoomId != null) {
        val activeRoom = chatRooms.find { it.id == activeRoomId }
        if (activeRoom != null) {
            ChatRoomView(
                room = activeRoom,
                onBack = onCloseRoom,
                onSendMessage = { text, type -> onSendRoomMessage(activeRoom.id, text, type) },
                onPinMessage = { text -> onPinRoomMessage(activeRoom.id, text) },
                onUnpinMessage = { onUnpinRoomMessage(activeRoom.id) },
                onDeleteMessage = { msgId -> onDeleteRoomMessage(activeRoom.id, msgId) },
                onKickMember = { memId -> onKickRoomMember(activeRoom.id, memId) },
                onMuteMember = { memId -> onMuteRoomMember(activeRoom.id, memId) },
                onChangeMemberRole = { memId, newRole -> onChangeMemberRole(activeRoom.id, memId, newRole) },
                onUpdateSettings = { name, desc, max -> onUpdateRoomSettings(activeRoom.id, name, desc, max) },
                onLeaveRoom = { onLeaveRoom(activeRoom.id) },
                onToggleLock = { onToggleLock(activeRoom.id) },
                onUpdateBackground = { uri -> onUpdateBackground(activeRoom.id, uri) },
                onBlockMember = { memId -> onBlockMember(activeRoom.id, memId) },
                onUnblockMember = { memId -> onUnblockMember(activeRoom.id, memId) },
                onJoinRoom = { onJoinRoom(activeRoom.id, "") },
                onStartRoomGame = { gameType -> onStartInRoomGame(activeRoom.id, gameType) },
                onPlayGameDirectly = onNavigateToGames,
                myAvatarUrl = myAvatarUrl,
                onRequestVoiceSeat = onRequestVoiceSeat,
                onRespondVoiceSeatRequest = onRespondVoiceSeatRequest,
                onLeaveVoiceSeat = onLeaveVoiceSeat,
                onMuteVoiceSeat = onMuteVoiceSeat,
                onToggleOwnerMute = onToggleOwnerVoiceMute,
                onTakeVoiceSeat = onTakeVoiceSeat
            )
            return
        }
    }
    // 3. Main Chat View with SubTabs ("المحادثات الخاصة" / "غرف الدردشة")
    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-Tab Switcher
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            TabRow(
                selectedTabIndex = if (chatSubTab == "المحادثات الخاصة") 0 else 1,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(44.dp),
                containerColor = Color.Transparent,
                indicator = {},
                divider = {}
            ) {
                listOf("المحادثات الخاصة", "غرف الدردشة").forEachIndexed { index, title ->
                    val isSelected = (index == 0 && chatSubTab == "المحادثات الخاصة") ||
                            (index == 1 && chatSubTab == "غرف الدردشة")
                    Tab(
                        selected = isSelected,
                        onClick = { onSubTabChange(title) },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .heightIn(min = 0.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("chat_subtab_$index"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.ChatBubbleOutline else Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }
        }

        // SubTab Content
        if (chatSubTab == "المحادثات الخاصة") {
            ChatListView(
                conversations = conversations,
                chatFilter = chatFilter,
                pushNotificationsEnabled = pushNotificationsEnabled,
                onFilterChange = onFilterChange,
                onTogglePushNotifications = onTogglePushNotifications,
                onOpenChat = onOpenChat
            )
        } else {
            ExploreRoomsGridView(
                rooms = chatRooms,
                onOpenRoom = onOpenRoom,
                onJoinRoom = onJoinRoom,
                onCreateRoom = onCreateRoom
            )
        }
    }
}

// -------------------------------------------------------------
// LIST OF CHAT ROOMS (غرف الدردشة)
// -------------------------------------------------------------
@Composable
fun ChatRoomsListView(
    rooms: List<ChatRoom>,
    onOpenRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Boolean,
    onCreateRoom: (String, String, String, RoomAccessType, String?, Int, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    var passwordPromptRoom by remember { mutableStateOf<ChatRoom?>(null) }

    val filteredRooms = rooms.filter { room ->
        val query = searchQuery.trim()
        if (query.isBlank()) {
            true
        } else {
            // Search primarily by room ID, supporting raw ID (e.g. room_1), numeric portion (e.g. 1), invite code, or name
            room.id.contains(query, ignoreCase = true) ||
                    room.id.removePrefix("room_").equals(query, ignoreCase = true) ||
                    room.inviteCode.contains(query, ignoreCase = true) ||
                    room.name.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Actions Bar: Search + Create Room & Join via Code
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن ID الغرفة", fontSize = 12.sp) },
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

                // Create Room Button
                FilledTonalButton(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MujtamaPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("create_room_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إنشاء غرفة", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Join by Invite Code Button
                IconButton(
                    onClick = { showJoinByCodeDialog = true },
                    modifier = Modifier.testTag("join_by_code_button")
                ) {
                    Icon(Icons.Default.Link, contentDescription = "رمز دعوة", tint = MujtamaTeal)
                }
            }
        }

        // Empty state when search returns no rooms
        if (filteredRooms.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔍", fontSize = 32.sp)
                        Text(
                            text = "لم يتم العثور على غرفة بالمعرّف: $searchQuery",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Rooms List immediately follows search bar without any category filters
        items(filteredRooms, key = { it.id }) { room ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("room_item_${room.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (room.isJoined) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Emoji Avatar
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MujtamaPrimary.copy(alpha = 0.85f), MujtamaTeal)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = room.iconEmoji, fontSize = 22.sp)
                            }

                            Column {
                                Text(
                                    text = room.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "ID: ${room.id}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Access badge
                                    val accessColor = when (room.accessType) {
                                        RoomAccessType.PUBLIC -> MujtamaOnlineGreen
                                        RoomAccessType.PASSWORD -> MujtamaGold
                                        RoomAccessType.INVITE_ONLY -> MaterialTheme.colorScheme.error
                                    }
                                    Text(
                                        text = when (room.accessType) {
                                            RoomAccessType.PUBLIC -> "عامة 🌐"
                                            RoomAccessType.PASSWORD -> "بكلمة سر 🔒"
                                            RoomAccessType.INVITE_ONLY -> "دعوة فقط ✉️"
                                        },
                                        fontSize = 10.sp,
                                        color = accessColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Member Count
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${room.memberCount}/${room.maxMembers}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Description
                    Text(
                        text = room.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        lineHeight = 16.sp
                    )

                    // Pinned snippet if any
                    if (room.pinnedMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MujtamaGold.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = room.pinnedMessage,
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (room.isOwner) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MujtamaGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "أنت مالك الغرفة 👑",
                                    fontSize = 10.sp,
                                    color = MujtamaGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "رمز الدعوة: ${room.inviteCode}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (room.isJoined) {
                            Button(
                                onClick = { onOpenRoom(room.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MujtamaPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("دخول الغرفة 💬", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (room.accessType == RoomAccessType.PASSWORD) {
                                        passwordPromptRoom = room
                                    } else {
                                        onJoinRoom(room.id, "")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MujtamaTeal),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("انضمام للغرفة ➕", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Password Prompt Dialog
    if (passwordPromptRoom != null) {
        val targetRoom = passwordPromptRoom!!
        var passInput by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { passwordPromptRoom = null },
            title = { Text("الغرفة محمية بكلمة مرور 🔒") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل كلمة المرور للانضمام إلى '${targetRoom.name}':", fontSize = 13.sp)
                    OutlinedTextField(
                        value = passInput,
                        onValueChange = {
                            passInput = it
                            isError = false
                        },
                        placeholder = { Text("كلمة المرور") },
                        isError = isError,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (isError) {
                        Text("كلمة المرور غير صحيحة!", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onJoinRoom(targetRoom.id, passInput)
                        if (success) {
                            passwordPromptRoom = null
                        } else {
                            isError = true
                        }
                    }
                ) {
                    Text("تأكيد والانضمام")
                }
            },
            dismissButton = {
                TextButton(onClick = { passwordPromptRoom = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Join by Code Dialog
    if (showJoinByCodeDialog) {
        var codeInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showJoinByCodeDialog = false },
            title = { Text("الانضمام عبر رمز دعوة ✉️") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل رمز الدعوة الخاص بالغرفة:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            codeInput = it
                            errorMessage = null
                        },
                        placeholder = { Text("مثال: GAME-2026 أو ADAB2026") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val foundRoom = rooms.find { it.inviteCode.equals(codeInput.trim(), ignoreCase = true) }
                        if (foundRoom != null) {
                            onJoinRoom(foundRoom.id, "")
                            showJoinByCodeDialog = false
                        } else {
                            errorMessage = "لم يتم العثور على غرفة بهذا الرمز!"
                        }
                    },
                    enabled = codeInput.isNotBlank()
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

    // Create Room Dialog
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("عام") }
        var accessType by remember { mutableStateOf(RoomAccessType.PUBLIC) }
        var password by remember { mutableStateOf("") }
        var maxMembersText by remember { mutableStateOf("100") }
        var selectedEmoji by remember { mutableStateOf("💬") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("إنشاء غرفة دردشة جديدة 🎙️") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Name
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

                    // Emoji selector
                    item {
                        Text("أيقونة الغرفة التعبيرية:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("💬", "🎮", "⚽", "🛡️", "📚", "💡", "🚀", "🎨").forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedEmoji = emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    // Description
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("وصف مختصر للغرفة") },
                            placeholder = { Text("عن ماذا تتحدث هذه الغرفة؟") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }

                    // Category
                    item {
                        Text("التصنيف والاهتمام:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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

                    // Access Type (Public, Password, Invite)
                    item {
                        Text("نوع الوصول والصلاحية:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RoomAccessType.values().forEach { type ->
                                FilterChip(
                                    selected = accessType == type,
                                    onClick = { accessType = type },
                                    label = { Text(type.labelAr, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Password field if required
                    if (accessType == RoomAccessType.PASSWORD) {
                        item {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("كلمة المرور للغرفة 🔒") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Max Members
                    item {
                        OutlinedTextField(
                            value = maxMembersText,
                            onValueChange = { maxMembersText = it.filter { char -> char.isDigit() } },
                            label = { Text("الحد الأقصى للأعضاء (10 - 1000)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val maxCount = maxMembersText.toIntOrNull() ?: 100
                        onCreateRoom(
                            name,
                            description,
                            category,
                            accessType,
                            password.ifBlank { null },
                            maxCount,
                            selectedEmoji
                        )
                        showCreateDialog = false
                    },
                    enabled = name.isNotBlank() && description.isNotBlank()
                ) {
                    Text("إنشاء الغرفة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// INSIDE CHAT ROOM VIEW (شاشة غرفة الدردشة الحية)
// -------------------------------------------------------------
@Composable
fun ChatRoomView(
    room: ChatRoom,
    onBack: () -> Unit,
    onSendMessage: (String, ChatMessageType) -> Unit,
    onPinMessage: (String) -> Unit,
    onUnpinMessage: () -> Unit,
    onDeleteMessage: (String) -> Unit,
    onKickMember: (String) -> Unit,
    onMuteMember: (String) -> Unit,
    onChangeMemberRole: (String, RoomMemberRole) -> Unit,
    onUpdateSettings: (String, String, Int) -> Unit,
    onLeaveRoom: () -> Unit,
    onToggleLock: () -> Unit,
    onUpdateBackground: (String) -> Unit,
    onBlockMember: (String) -> Unit,
    onUnblockMember: (String) -> Unit,
    onJoinRoom: () -> Unit,
    onStartRoomGame: (GameType) -> Unit,
    onPlayGameDirectly: (GameType?) -> Unit,
    myAvatarUrl: String = "",
    onRequestVoiceSeat: (Int) -> Unit = {},
    onRespondVoiceSeatRequest: (String, Boolean) -> Unit = { _, _ -> },
    onLeaveVoiceSeat: (Int) -> Unit = {},
    onMuteVoiceSeat: (Int) -> Unit = {},
    onToggleOwnerMute: () -> Unit = {},
    onTakeVoiceSeat: (Int) -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var showMembersSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showGamePicker by remember { mutableStateOf(false) }
    var showMembersListDialog by remember { mutableStateOf(false) }
    var showAdminsListDialog by remember { mutableStateOf(false) }
    var showBannedListDialog by remember { mutableStateOf(false) }
    var showBackgroundPickerDialog by remember { mutableStateOf(false) }
    var showVoiceMicDialog by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                onUpdateBackground(uri.toString())
            }
        }
    )
    
    val currentMember = room.members.find { it.id == "me" }
    val isOwnerOrAdmin = room.isOwner || currentMember?.role == RoomMemberRole.ADMIN || currentMember?.role == RoomMemberRole.OWNER
    val isMuted = currentMember?.isMuted == true

    val roomContext = androidx.compose.ui.platform.LocalContext.current
    val musicPermission = if (android.os.Build.VERSION.SDK_INT >= 33) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val musicPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted: Boolean ->
            android.widget.Toast.makeText(
                roomContext,
                if (granted) "تم منح صلاحية ملفات الصوت" else "لم تُمنح صلاحية ملفات الصوت",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    )

    var roomMusicPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isMusicPlaying by remember { mutableStateOf(false) }
    var showMusicPage by remember { mutableStateOf(false) }
    var playingTrackIndex by remember { mutableStateOf(-1) }
    val roomMusicTracks = remember { androidx.compose.runtime.mutableStateListOf<Pair<String, Uri>>() }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            roomMusicPlayer?.release()
            roomMusicPlayer = null
        }
    }

    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null && roomMusicTracks.size < 10) {
                var trackName = "مقطع صوتي"
                try {
                    roomContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && cursor.moveToFirst()) {
                            trackName = cursor.getString(nameIndex) ?: trackName
                        }
                    }
                } catch (e: Exception) {
                }
                if (roomMusicTracks.any { it.second == uri || (trackName != "مقطع صوتي" && it.first == trackName) }) {
                    android.widget.Toast.makeText(
                        roomContext,
                        "الموسيقى موجودة",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    roomMusicTracks.add(Pair(trackName, uri))
                }
            }
        }
    )

    val stopMusic: () -> Unit = {
        roomMusicPlayer?.release()
        roomMusicPlayer = null
        isMusicPlaying = false
        playingTrackIndex = -1
    }

    val playTrack: (Int) -> Unit = { index ->
        try {
            roomMusicPlayer?.release()
            val player = android.media.MediaPlayer()
            player.setDataSource(roomContext, roomMusicTracks[index].second)
            player.isLooping = true
            player.setOnPreparedListener { it.start() }
            player.prepareAsync()
            roomMusicPlayer = player
            isMusicPlaying = true
            playingTrackIndex = index
        } catch (e: Exception) {
            roomMusicPlayer = null
            isMusicPlaying = false
            playingTrackIndex = -1
            android.widget.Toast.makeText(
                roomContext,
                "تعذر تشغيل الملف",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (showMusicPage) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showMusicPage = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showMusicPage = false }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                        }
                        Text(
                            text = "موسيقى الغرفة (${roomMusicTracks.size}/10)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (isOwnerOrAdmin) {
                            Button(
                                onClick = {
                                    if (roomMusicTracks.size >= 10) {
                                        android.widget.Toast.makeText(
                                            roomContext,
                                            "الحد الأقصى 10 مقاطع",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        musicPickerLauncher.launch(arrayOf("audio/*"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MujtamaGold)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                Text(text = "إضافة", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (roomMusicTracks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "لا توجد موسيقى في الغرفة بعد",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(roomMusicTracks.size) { index ->
                                val isThisPlaying = playingTrackIndex == index
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { if (isThisPlaying) stopMusic() else playTrack(index) }
                                        ) {
                                            Icon(
                                                imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = if (isThisPlaying) "إيقاف" else "تشغيل",
                                                tint = if (isThisPlaying) MujtamaGold else MujtamaTeal
                                            )
                                        }
                                        Text(
                                            text = roomMusicTracks[index].first,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isOwnerOrAdmin) {
                                            IconButton(
                                                onClick = {
                                                    if (playingTrackIndex == index) {
                                                        stopMusic()
                                                    } else if (playingTrackIndex > index) {
                                                        playingTrackIndex = playingTrackIndex - 1
                                                    }
                                                    roomMusicTracks.removeAt(index)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف الموسيقى",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Intercept system back button/gesture to leave room smoothly
    BackHandler(onBack = onBack)

Box(modifier = Modifier.fillMaxSize()) {
    if (!room.backgroundImageUrl.isNullOrBlank()) {
        AsyncImage(
            model = room.backgroundImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
// Room Top App Bar
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .heightIn(min = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                }

// Room Image (or first letter of room name if none was set)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (room.imageUrl.isNullOrBlank()) MujtamaPrimary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (!room.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = room.imageUrl,
                            contentDescription = room.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = room.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!room.isJoined && !room.isOwner) {
                            Text(
                                text = "انضمام",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onJoinRoom() }
                            )
                        }
                        Text(
                            text = room.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                    // معرّف الغرفة (Room ID) المكوّن من 8 أرقام
                    val displayId = if (room.id.filter { it.isDigit() }.length == 8) {
                        room.id.filter { it.isDigit() }
                    } else {
                        String.format("%08d", kotlin.math.abs(room.id.hashCode()) % 90000000 + 10000000)
                    }
                    Text(
                        text = "ID: $displayId",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("room_topbar_id")
                    )
                }

// Members List Button
                IconButton(
                    onClick = { showMembersSheet = true },
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("room_members_button")
                ) {
                    Icon(Icons.Default.Group, contentDescription = "الأعضاء", tint = MaterialTheme.colorScheme.primary)
                }

// Room Settings / More Menu (Owner only)
                if (room.isOwner) {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("room_settings_button")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات الغرفة")
                    }
                }
            }
        }

        // Voice Mics Section: مايك المالك في المنتصف وتحته 8 مايكات مرقمة (1-8)
        RoomVoiceStage(
            room = room,
            isCurrentUserOwner = room.isOwner,
            onToggleOwnerMute = onToggleOwnerMute,
            onLeaveVoiceSeat = onLeaveVoiceSeat,
            onMuteVoiceSeat = onMuteVoiceSeat
        )

        // 5. نقل الرسالة المثبتة (Pinned Message) لتظهر أسفل صف المايكات
        AnimatedVisibility(visible = room.pinnedMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MujtamaGold.copy(alpha = 0.15f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, tint = MujtamaGold, modifier = Modifier.size(16.dp))
                        Text(
                            text = room.pinnedMessage ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isOwnerOrAdmin) {
                        IconButton(
                            onClick = onUnpinMessage,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء التثبيت", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Muted status warning
        if (isMuted) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = "⚠️ أنت في وضع الكتم داخل هذه الغرفة، لا يمكنك إرسال رسائل حالياً.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Messages Stream
        val roomListState = androidx.compose.foundation.lazy.rememberLazyListState()
        androidx.compose.runtime.LaunchedEffect(room.messages.size) {
            if (room.messages.isNotEmpty()) {
                roomListState.animateScrollToItem(room.messages.lastIndex)
            }
        }
        LazyColumn(
            state = roomListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(room.messages, key = { it.id }) { message ->
                RoomMessageBubble(
                    message = message,
                    isOwnerOrAdmin = isOwnerOrAdmin,
                    onDeleteMessage = { onDeleteMessage(message.id) },
                    onPlayGame = { onPlayGameDirectly(findGameTypeFromTitle(message.gameTitle)) },
                    myAvatarUrl = myAvatarUrl
                )
            }
        }

        // Input Bar
        Surface(
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 6. استبدال أيقونة المايك بأيقونة صندوق الهدايا (Gift Box)
                IconButton(
                    onClick = { /* Placeholder: وظيفة صندوق الهدايا بدون منطق خلفي */ },
                    enabled = !isMuted,
                    modifier = Modifier.testTag("room_gift_box_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "صندوق الهدايا",
                        tint = MujtamaGold
                    )
                }

                // Room music button (Owner/Admin only)
                if (isOwnerOrAdmin) {
                    IconButton(
                        onClick = {
                            val alreadyGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                roomContext,
                                musicPermission
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (alreadyGranted) {
                                showMusicPage = true
                            } else {
                                musicPermissionLauncher.launch(musicPermission)
                            }
                        },
                        enabled = !isMuted,
                        modifier = Modifier.testTag("room_music_button")
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "الموسيقى", tint = if (isMusicPlaying) MujtamaGold else MujtamaTeal)
                    }
                }
                // Text Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isMuted) "أنت مكتوم في الغرفة..." else "شارك برأيك في الغرفة...",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("room_message_input"),
                    shape = RoundedCornerShape(22.dp),
                    enabled = !isMuted,
                    singleLine = true
                )

                // Voice mic request button (Badge = عدد الطلبات المعلّقة)
                Box {
                    IconButton(
                        onClick = { showVoiceMicDialog = true },
                        modifier = Modifier.testTag("room_voice_request_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "طلب مايك",
                            tint = MujtamaTeal
                        )
                    }
                    if (room.voiceSeatRequests.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${room.voiceSeatRequests.size}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Send Button
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText, ChatMessageType.TEXT)
                            inputText = ""
                        }
                    },
                    enabled = !isMuted && inputText.isNotBlank(),
                    shape = CircleShape,
                    modifier = Modifier.testTag("room_send_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "إرسال")
                }
            }
        }
    }

    // Members Bottom Sheet / Dialog
    if (showMembersSheet) {
        AlertDialog(
            onDismissRequest = { showMembersSheet = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("أعضاء الغرفة (${room.members.size})")
                    Text(
                        text = "${room.members.count { it.isOnline }} متصل",
                        fontSize = 11.sp,
                        color = MujtamaOnlineGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(room.members) { member ->
                        val isTargetSelf = member.id == "me"
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Avatar with online dot
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MujtamaPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val avatarBitmap = remember(myAvatarUrl) {
                                            if (isTargetSelf && myAvatarUrl.isNotEmpty()) {
                                                BitmapFactory.decodeFile(myAvatarUrl)?.asImageBitmap()
                                            } else null
                                        }
                                        if (avatarBitmap != null) {
                                            Image(
                                                bitmap = avatarBitmap,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(text = member.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (member.isOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(MujtamaOnlineGreen)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = member.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        if (member.isMuted) {
                                            Text("🔇", fontSize = 10.sp)
                                        }
                                    }
                                    val memberDisplayId = if (member.id.filter { it.isDigit() }.length == 8) {
                                        member.id.filter { it.isDigit() }
                                    } else {
                                        String.format("%08d", kotlin.math.abs(member.id.hashCode()) % 90000000 + 10000000)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "ID: $memberDisplayId",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = member.role.labelAr,
                                            fontSize = 10.sp,
                                            color = when (member.role) {
                                                RoomMemberRole.OWNER -> MujtamaGold
                                                RoomMemberRole.ADMIN -> MujtamaTeal
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Moderation controls (if I am Owner/Admin and target is not myself)
                                if (isOwnerOrAdmin && !isTargetSelf) {
                                    // Mute toggle
                                    IconButton(
                                        onClick = { onMuteMember(member.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (member.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = "كتم",
                                            tint = if (member.isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Kick member
                                    IconButton(
                                        onClick = { onKickMember(member.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonRemove,
                                            contentDescription = "طرد",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Block member (Owner only)
                                    if (room.isOwner && member.id != "me") {
                                        IconButton(
                                            onClick = { onBlockMember(member.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = "حظر",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Promote/Demote (Owner only)
                                    if (room.isOwner) {
                                        IconButton(
                                            onClick = {
                                                val nextRole = if (member.role == RoomMemberRole.ADMIN) RoomMemberRole.MEMBER else RoomMemberRole.ADMIN
                                                onChangeMemberRole(member.id, nextRole)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = "تعديل الرتبة",
                                                tint = MujtamaTeal,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMembersSheet = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

// Room Settings & Actions Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("إعدادات وإدارة الغرفة ⚙️") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            showSettingsDialog = false
                            showMembersListDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الأعضاء")
                    }

                    OutlinedButton(
                        onClick = {
                            showSettingsDialog = false
                            showAdminsListDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("المشرفون")
                    }

                    OutlinedButton(
                        onClick = {
                            showSettingsDialog = false
                            showBannedListDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("المحظورون")
                    }

                    OutlinedButton(
                        onClick = {
                            showSettingsDialog = false
                            showBackgroundPickerDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تغيير خلفية الدردشة")
                    }

OutlinedButton(
                        onClick = { onToggleLock() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (room.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (room.isLocked) "الغرفة مقفلة 🔒" else "قفل الغرفة")
}
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Members List Dialog
    if (showMembersListDialog) {
        AlertDialog(
            onDismissRequest = { showMembersListDialog = false },
            title = { Text("أعضاء الغرفة (${room.members.size})") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(room.members) { member ->
                        Text(text = member.name, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMembersListDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Admins List Dialog
    if (showAdminsListDialog) {
        val admins = room.members.filter { it.role == RoomMemberRole.ADMIN || it.role == RoomMemberRole.OWNER }
        AlertDialog(
            onDismissRequest = { showAdminsListDialog = false },
            title = { Text("مشرفو الغرفة (${admins.size})") },
            text = {
                if (admins.isEmpty()) {
                    Text("لا يوجد مشرفون حالياً.", fontSize = 13.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(admins) { member ->
                            Text(text = "${member.name} (${member.role.labelAr})", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAdminsListDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

// Banned List Dialog
    if (showBannedListDialog) {
        AlertDialog(
            onDismissRequest = { showBannedListDialog = false },
            title = { Text("المستخدمون المحظورون") },
            text = {
                if (room.blockedMembers.isEmpty()) {
                    Text("لا يوجد مستخدمون محظورون حالياً.", fontSize = 13.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        room.blockedMembers.forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(member.name, fontSize = 13.sp)
                                TextButton(onClick = { onUnblockMember(member.id) }) {
                                    Text("إلغاء الحظر", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBannedListDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

// Chat Background Picker Dialog
    if (showBackgroundPickerDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundPickerDialog = false },
            title = { Text("خلفية الدردشة") },
            text = {
                Text("اختر صورة من معرض هاتفك لتكون خلفية هذه الغرفة.")
            },
            confirmButton = {
                TextButton(onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    showBackgroundPickerDialog = false
                }) {
                    Text("اختيار صورة")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onUpdateBackground("")
                    showBackgroundPickerDialog = false
                }) {
                    Text("إعادة الافتراضي")
                }
            }
        )
    }
    // Voice Mic Request Dialog: مالك/مشرف = قائمة طلبات، عضو = اختيار مقعد فارغ
    if (showVoiceMicDialog) {
        if (isOwnerOrAdmin) {
            AlertDialog(
                onDismissRequest = { showVoiceMicDialog = false },
                title = { Text("طلبات المايك (${room.voiceSeatRequests.size})") },
                text = {
                    if (room.voiceSeatRequests.isEmpty()) {
                        Text("لا توجد طلبات حالياً.", fontSize = 13.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(room.voiceSeatRequests, key = { it.id }) { request ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                .background(MujtamaPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (request.requesterAvatarUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = request.requesterAvatarUrl,
                                                    contentDescription = request.requesterName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                )
                                            } else {
                                                Text(request.requesterName.take(1), color = Color.White, fontSize = 12.sp)
                                            }
                                        }
                                        Text(request.requesterName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { onRespondVoiceSeatRequest(request.id, true) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Check, contentDescription = "قبول", tint = MujtamaOnlineGreen, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { onRespondVoiceSeatRequest(request.id, false) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "رفض", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showVoiceMicDialog = false }) {
                        Text("إغلاق")
                    }
                }
            )
        } else {
            val emptySeats = room.voiceSeats.filter { it.occupantId.isNullOrBlank() }.map { it.seatNumber }
            AlertDialog(
                onDismissRequest = { showVoiceMicDialog = false },
                title = { Text("طلب مايك") },
                text = {
                    if (emptySeats.isEmpty()) {
                        Text("لا توجد مقاعد فارغة حالياً.", fontSize = 13.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            emptySeats.forEach { seatNum ->
                                OutlinedButton(
                                    onClick = {
                                        onRequestVoiceSeat(seatNum)
                                        showVoiceMicDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("مقعد رقم $seatNum")
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showVoiceMicDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    // Pin Message Dialog
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("تثبيت إعلان في الغرفة 📌") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اكتب الإعلان الذي ترغب في ظهوره لجميع أعضاء الغرفة في الأعلى:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        placeholder = { Text("مثال: التحدي سيبدأ الساعة 9 مساءً") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPinMessage(pinInput)
                        showPinDialog = false
                    },
                    enabled = pinInput.isNotBlank()
                ) {
                    Text("تثبيت الإعلان")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
// Game Picker Dialog
        if (showGamePicker) {
            GamePickerDialog(
                onDismiss = { showGamePicker = false },
                onSelectGame = {
                    showGamePicker = false
                    onStartRoomGame(it)
                }
            )
        }
    }
}
}

// -------------------------------------------------------------
// Voice Stage: مايك المالك في المنتصف وتحته 8 مايكات مرقمة من 1 إلى 8
// نمط زجاجي شفاف أنيق وعصري (Glassmorphism & Glowing Active States)
// -------------------------------------------------------------
@Composable
fun RoomVoiceStage(
    room: ChatRoom,
    isCurrentUserOwner: Boolean,
    onToggleOwnerMute: () -> Unit = {},
    onLeaveVoiceSeat: (Int) -> Unit = {},
    onMuteVoiceSeat: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var seatOptionsFor by remember { mutableStateOf<Int?>(null) }

    // حاوية المنصة الصوتية بنمط زجاجي شفاف ناعم وأنيق (Glassmorphism)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.50f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.06f)
                )
            )
        ),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // مايك المالك في منتصف الشاشة: بحجم أكبر (58dp) وتصميم زجاجي ذهبي مع هالة توهج
            OwnerVoiceSeat(
                isOwnerMuted = room.ownerVoiceSeat.isMuted,
                isSpeaking = !room.ownerVoiceSeat.isMuted,
                onClick = {
                    if (isCurrentUserOwner) {
                        onToggleOwnerMute()
                    }
                }
            )

            // فاصل زجاجي شفاف خفيف
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 28.dp),
                color = Color.White.copy(alpha = 0.12f)
            )

            // مايكات الأعضاء: 8 مايكات صوتية في شبكة منتظمة متساوية (4 × 2)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // الصف الأول: مايكات 1، 2، 3، 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..4).forEach { seatNum ->
                        val seat = room.voiceSeats.firstOrNull { it.seatNumber == seatNum }
                        val occupied = !seat?.occupantId.isNullOrBlank()
                        VoiceSeatItem(
                            seatNumber = seatNum,
                            isOccupied = occupied,
                            isSpeaking = occupied && seat?.isMuted == false,
                            occupantName = seat?.occupantName,
                            occupantAvatarUrl = seat?.occupantAvatarUrl,
                            onClick = {
                                if (seat?.occupantId == "me") {
                                    seatOptionsFor = seatNum
                                }
                            }
                        )
                    }
                }

                // الصف الثاني: مايكات 5، 6، 7، 8
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (5..8).forEach { seatNum ->
                        val seat = room.voiceSeats.firstOrNull { it.seatNumber == seatNum }
                        val occupied = !seat?.occupantId.isNullOrBlank()
                        VoiceSeatItem(
                            seatNumber = seatNum,
                            isOccupied = occupied,
                            isSpeaking = occupied && seat?.isMuted == false,
                            occupantName = seat?.occupantName,
                            occupantAvatarUrl = seat?.occupantAvatarUrl,
                            onClick = {
                                if (seat?.occupantId == "me") {
                                    seatOptionsFor = seatNum
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    val seatNum = seatOptionsFor
    if (seatNum != null) {
        val seat = room.voiceSeats.firstOrNull { it.seatNumber == seatNum }
        androidx.compose.ui.window.Dialog(onDismissRequest = { seatOptionsFor = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.TextButton(onClick = {
                        onLeaveVoiceSeat(seatNum)
                        seatOptionsFor = null
                    }) {
                        androidx.compose.material3.Text("نزول من المايك")
                    }
                    androidx.compose.material3.TextButton(onClick = {
                        onMuteVoiceSeat(seatNum)
                        seatOptionsFor = null
                    }) {
                        androidx.compose.material3.Text(
                            if (seat?.isMuted == true) "إلغاء الكتم" else "كتم نفسي"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerVoiceSeat(
    isOwnerMuted: Boolean,
    isSpeaking: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "owner_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "owner_glow_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "owner_glow_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("room_owner_mic")
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .scale(if (isSpeaking) glowScale else 1f)
                .clip(CircleShape)
                // خلفية شفافة زجاجية بنمط Glassmorphism ولمسة ذهبية فخمة
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MujtamaGold.copy(alpha = if (isSpeaking) 0.32f else 0.16f),
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
// حواف دائرية أوضح مع تأثير توهج حيوي عند التحدث
                .border(
                    width = if (isSpeaking) 3.dp else 2.5.dp,
                    brush = if (isSpeaking) {
                        Brush.linearGradient(
                            listOf(
                                MujtamaGold.copy(alpha = glowAlpha),
                                Color.White.copy(alpha = 0.95f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MujtamaGold.copy(alpha = 0.9f),
                                Color.White.copy(alpha = 0.4f)
                            )
                        )
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isOwnerMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "مايك المالك",
                tint = if (isOwnerMuted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MujtamaGold,
                modifier = Modifier.size(28.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("👑", fontSize = 11.sp)
            Text(
                text = "المالك",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MujtamaGold
            )
        }
    }
}

@Composable
private fun VoiceSeatItem(
    seatNumber: Int,
    isOccupied: Boolean,
    isSpeaking: Boolean,
    occupantName: String? = null,
    occupantAvatarUrl: String? = null,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "seat_pulse_$seatNumber")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "seat_glow_$seatNumber"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "seat_scale_$seatNumber"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("room_member_mic_$seatNumber")
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(if (isSpeaking) glowScale else 1f)
                .clip(CircleShape)
                .background(
                    when {
                        isSpeaking -> Brush.radialGradient(
                            listOf(
                                MujtamaTeal.copy(alpha = 0.32f),
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                        isOccupied -> Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                        else -> Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.05f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    }
                )
                .border(
                    width = if (isSpeaking) 3.dp else 2.dp,
                    brush = when {
                        isSpeaking -> Brush.linearGradient(
                            listOf(
                                MujtamaTeal.copy(alpha = glowAlpha),
                                Color.White.copy(alpha = 0.9f)
                            )
                        )
                        isOccupied -> Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                Color.White.copy(alpha = 0.5f)
                            )
                        )
                        else -> Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            )
                        )
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isOccupied && !occupantAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = occupantAvatarUrl,
                    contentDescription = occupantName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = if (isOccupied) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = "مايك $seatNumber",
                    tint = when {
                        isSpeaking -> MujtamaTeal
                        isOccupied -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = if (isOccupied && !occupantName.isNullOrBlank()) occupantName else "$seatNumber",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 44.dp),
            color = when {
                isSpeaking -> MujtamaTeal
                isOccupied -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            }
        )
    }
}
@Composable
fun RoomMessageBubble(
    message: ChatMessage,
    isOwnerOrAdmin: Boolean,
    onDeleteMessage: () -> Unit,
    onPlayGame: () -> Unit,
    myAvatarUrl: String = ""
) {
    val bubbleColor = if (message.isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isFromMe) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (message.isFromMe && myAvatarUrl.isNotEmpty()) {
                val myRoomMsgAvatarBitmap = remember(myAvatarUrl) {
                    BitmapFactory.decodeFile(myAvatarUrl)?.asImageBitmap()
                }
                if (myRoomMsgAvatarBitmap != null) {
                    Image(
                        bitmap = myRoomMsgAvatarBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )
                }
            }
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (message.senderName.isNotBlank()) {
                    Text(
                        text = message.senderName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromMe) 4.dp else 16.dp
                ),
                color = bubbleColor
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    when (message.type) {
                        ChatMessageType.TEXT -> {
                            Text(text = message.text, color = textColor, fontSize = 15.sp)
                        }
                        ChatMessageType.AUDIO -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (message.isFromMe) Color.White.copy(alpha = 0.25f) else MujtamaPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "تشغيل الصوت",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "رسالة صوتية في الغرفة",
                                        color = textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "0:${message.audioDurationSec.toString().padStart(2, '0')}",
                                        color = textColor.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        ChatMessageType.IMAGE -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = "صورة",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "صورة مرفقة في الغرفة",
                                    color = textColor,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        ChatMessageType.GAME_INVITE -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF261D42))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.SportsEsports,
                                            contentDescription = null,
                                            tint = MujtamaGold
                                        )
                                        Text(
                                            text = message.gameTitle ?: "تحدي جماعي لأعضاء الغرفة",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Text(
                                        text = "جائزة التحدي: +${message.gameReward} نقطة نظام 🌟",
                                        color = MujtamaGold,
                                        fontSize = 12.sp
                                    )
                                    Button(
                                        onClick = onPlayGame,
                                        colors = ButtonDefaults.buttonColors(containerColor = MujtamaGold),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "انضم للعب الآن",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
            
        }
    }
}

// -------------------------------------------------------------
// DIRECT / GROUP CHAT LIST (المحادثات الفردية والجماعية)
// -------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListView(
    conversations: List<ChatConversation>,
    chatFilter: String,
    pushNotificationsEnabled: Boolean,
    onFilterChange: (String) -> Unit,
    onTogglePushNotifications: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredConversations = conversations.filter { conv ->
        val matchesFilter = when (chatFilter) {
            "متصل الآن" -> conv.isOnline
            else -> true
        }
        val matchesSearch = if (searchQuery.isBlank()) true else conv.name.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Filter Chips Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("الكل", "متصل الآن").forEach { filter ->
                    FilterChip(
                        selected = chatFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter, fontSize = 12.sp) },
                        modifier = Modifier.testTag("chat_filter_${if (filter == "الكل") "all" else "online"}")
                    )
                }
            }
        }

        // Conversation List Items
        items(filteredConversations, key = { it.id }) { conv ->
            var showOptionsMenu by remember { mutableStateOf(false) }

            Box {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .combinedClickable(
                            onClick = { onOpenChat(conv.id) },
                            onLongClick = { showOptionsMenu = true }
                        )
                        .testTag("conversation_item_${conv.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar with online badge
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            if (conv.isGroup) listOf(MujtamaPrimary, MujtamaTeal)
                                            else listOf(MujtamaTeal, MujtamaGold)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (conv.isGroup) Icons.Default.Groups else Icons.Default.Person,
                                    contentDescription = conv.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            if (conv.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(MujtamaOnlineGreen)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }

                        // Texts
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conv.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = conv.time,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conv.lastMessage,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )

                                if (conv.unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ) {
                                        Text("${conv.unreadCount}")
                                    }
                                }
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("تثبيت المحادثة") },
                        onClick = { showOptionsMenu = false },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف المحادثة") },
                        onClick = { showOptionsMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("حظر المستخدم") },
                        onClick = { showOptionsMenu = false },
                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIRECT CONVERSATION DETAIL VIEW (المحادثة الفردية)
// -------------------------------------------------------------
@Composable
fun ChatConversationView(
    conversation: ChatConversation,
    onBack: () -> Unit,
    onSendMessage: (String, ChatMessageType) -> Unit,
    onStartGame: (GameType) -> Unit,
    onPlayGameDirectly: (GameType?) -> Unit,
    myAvatarUrl: String = ""
) {
    var inputText by remember { mutableStateOf("") }
    var showGamePicker by remember { mutableStateOf(false) }

Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Chat Header
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MujtamaPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (conversation.avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = conversation.avatarUrl,
                            contentDescription = conversation.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = conversation.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conversation.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (conversation.isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MujtamaOnlineGreen)
                            )
                            Text(
                                text = "متصل الآن",
                                fontSize = 11.sp,
                                color = MujtamaOnlineGreen
                            )
                        } else {
                            Text(
                                text = if (conversation.isGroup) "${conversation.memberCount} عضواً" else "غير متصل",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // In-Chat Game Launcher Button
                Button(
                    onClick = { showGamePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MujtamaGold),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("in_chat_launch_game_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "بدء لعبة",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "بدء لعبة",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Messages Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(conversation.messages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    onPlayGame = { onPlayGameDirectly(findGameTypeFromTitle(message.gameTitle)) },
                    myAvatarUrl = myAvatarUrl
                )
            }
        }

        // Chat Input Bar
        Surface(
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Voice note button
                IconButton(
                    onClick = {
                        onSendMessage("تسجيل صوتي", ChatMessageType.AUDIO)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "تسجيل صوتي",
                        tint = MujtamaPrimary
                    )
                }

                // Image attach button
                IconButton(
                    onClick = {
                        onSendMessage("صورة مرفقة", ChatMessageType.IMAGE)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "إرفاق صورة",
                        tint = MujtamaTeal
                    )
                }

                // Text Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("اكتب رسالتك...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                // Send Button
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText, ChatMessageType.TEXT)
                            inputText = ""
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال")
                }
            }
        }

        // In-Chat Game Picker Dialog
        if (showGamePicker) {
            GamePickerDialog(
                onDismiss = { showGamePicker = false },
                onSelectGame = {
                    showGamePicker = false
                    onStartGame(it)
                }
            )
        }
    }
}

// -------------------------------------------------------------
// DIRECT CHAT MESSAGE BUBBLE
// -------------------------------------------------------------
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onPlayGame: () -> Unit,
    myAvatarUrl: String = ""
) {
    val bubbleColor = if (message.isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isFromMe) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (message.isFromMe && myAvatarUrl.isNotEmpty()) {
                val myMsgAvatarBitmap = remember(myAvatarUrl) {
                    BitmapFactory.decodeFile(myAvatarUrl)?.asImageBitmap()
                }
                if (myMsgAvatarBitmap != null) {
                    Image(
                        bitmap = myMsgAvatarBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )
                }
            }
        Column(
            horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            if (!message.isFromMe) {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromMe) 4.dp else 16.dp
                ),
                color = bubbleColor
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    when (message.type) {
                        ChatMessageType.TEXT -> {
                            Text(text = message.text, color = textColor, fontSize = 14.sp)
                        }
                        ChatMessageType.AUDIO -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (message.isFromMe) Color.White.copy(alpha = 0.25f) else MujtamaPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "تشغيل الصوت",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "رسالة صوتية",
                                        color = textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "0:${message.audioDurationSec.toString().padStart(2, '0')}",
                                        color = textColor.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        ChatMessageType.IMAGE -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = "صورة",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "صورة مرفقة عبر المحادثة",
                                    color = textColor,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        ChatMessageType.GAME_INVITE -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF261D42))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.SportsEsports,
                                            contentDescription = null,
                                            tint = MujtamaGold
                                        )
                                        Text(
                                            text = message.gameTitle ?: "تحدي الألعاب المباشر",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Text(
                                        text = "جائزة الفوز: +${message.gameReward} نقطة نظام 🌟",
                                        color = MujtamaGold,
                                        fontSize = 12.sp
                                    )
                                    Button(
                                        onClick = onPlayGame,
                                        colors = ButtonDefaults.buttonColors(containerColor = MujtamaGold),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "العب التحدي الآن",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.timestamp,
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
        }
    }
}

// -------------------------------------------------------------
// HELPER: FIND GAME TYPE BY TITLE & GAME PICKER DIALOG
// -------------------------------------------------------------
fun findGameTypeFromTitle(title: String?): GameType {
    if (title == null) return GameType.LUDO
    return when {
        title.contains("جاكارو") -> GameType.JACKAROO
        title.contains("دومينو") -> GameType.DOMINO
        title.contains("سلم") -> GameType.SNAKES_AND_LADDERS
        title.contains("شطرنج") -> GameType.CHESS
        else -> GameType.LUDO
    }
}

@Composable
fun GamePickerDialog(
    onDismiss: () -> Unit,
    onSelectGame: (GameType) -> Unit
) {
    val games = listOf(
        GameType.LUDO,
        GameType.JACKAROO,
        GameType.DOMINO,
        GameType.SNAKES_AND_LADDERS,
        GameType.CHESS
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختر لعبة للتحدي 🎮",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "حدد إحدى الألعاب الخمس لإرسال دعوة التحدي وبدء اللعب مباشرة:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    games.forEach { game ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectGame(game) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(game.tagColorHex).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(game.iconEmoji, fontSize = 18.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(game.titleAr, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(game.subtitleAr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(game.tagColorHex).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = game.playersCount,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(game.tagColorHex),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
