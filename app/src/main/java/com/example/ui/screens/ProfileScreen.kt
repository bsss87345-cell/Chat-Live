package com.example.ui.screens

import coil.compose.AsyncImage
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.view.WindowManager
import androidx.core.view.ViewCompat
import com.example.model.*
import com.example.ui.theme.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.shadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    posts: List<Post>,
    balance: Int,
    onUpdateBio: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenAccountSettings: () -> Unit = {},
    onUpdateAvatarImage: (String) -> Unit = {},
    followersList: List<FollowUser> = emptyList(),
    followingList: List<FollowUser> = emptyList(),
    isOnOwnProfile: Boolean = true
) {
    var showEditBioDialog by remember { mutableStateOf(false) }
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }
    var selectedProfileTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val avatarImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            saveAvatarToInternalStorage(context, it)?.let { path ->
                onUpdateAvatarImage(path)
            }
        }
    }

    // User's own posts or activity (used for the header stats count)
    val userPosts = posts.filter {
        it.authorHandle == userProfile.handle ||
        it.authorHandle == "ID: ${userProfile.id}" ||
        it.authorHandle == "@user_me" ||
        it.id.startsWith("post_") ||
        it.id.startsWith("p_")
    }.take(6)
    val totalPostsCount = userPosts.size

    Box(modifier = Modifier.fillMaxSize()) {
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
           Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_header_card")
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { 
                    // Menu icon (top-left corner in RTL) → opens the separate account-settings page
                    // Top bar: ID (right) + settings icons (left, own profile only)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ID: ${userProfile.id}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isOnOwnProfile) {
                            IconButton(
                                onClick = { settingsExpanded = !settingsExpanded },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("profile_settings_expand_button")
                            ) {
                                Icon(
                                    imageVector = if (settingsExpanded) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
                                    contentDescription = "توسيع الإعدادات",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(
                                onClick = { onOpenAccountSettings() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("profile_settings_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "القائمة",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            if (settingsExpanded) {
                                IconButton(
                                    onClick = { },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("profile_views_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveRedEye,
                                        contentDescription = "من شاهد الملف الشخصي",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("profile_add_friend_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "إضافة صديق",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("profile_swap_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "تحويل",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Avatar + Stats side-by-side, with the name floating above (no reserved space)
                    var avatarGlowStarted by remember { mutableStateOf(false) }
                    val avatarGlowAlpha by animateFloatAsState(
                        targetValue = if (avatarGlowStarted) 0f else 1f,
                        animationSpec = tween(durationMillis = 1200),
                        label = "avatarGlow"
                    )
                    LaunchedEffect(Unit) {
                        avatarGlowStarted = true
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                        Box(
                            modifier = Modifier.size(86.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                MujtamaGold.copy(alpha = 0.6f * avatarGlowAlpha),
                                                MujtamaTeal.copy(alpha = 0.3f * avatarGlowAlpha),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
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
                                if (userProfile.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = userProfile.avatarUrl,
                                        contentDescription = "صورة الملف الشخصي",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(text = userProfile.avatarEmoji, fontSize = 42.sp)
                                }
                            }

                            // Edit avatar icon button
                            if (isOnOwnProfile) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MujtamaGold)
                                        .clickable {
                                            avatarImageLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تغيير صورة الملف الشخصي",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                       }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileStatItem(title = "المنشورات", count = "$totalPostsCount")
                            ProfileStatItem(
                                title = "المتابعون",
                                count = "${userProfile.followersCount}",
                                onClick = { showFollowersDialog = true }
                            )
                            ProfileStatItem(
                                title = "يتابع",
                                count = "${userProfile.followingCount}",
                                onClick = { showFollowingDialog = true }
                            )
                        }
                    }
                        Text(
                            text = userProfile.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                                .offset(y = (-4).dp)
                        )
                    }

                    // Bio (plain text, no box — shown for own profile and others; placeholder for empty own bio)
                    if (userProfile.bio.isNotBlank() || isOnOwnProfile) {
                        Text(
                            text = if (userProfile.bio.isNotBlank()) userProfile.bio else "أضف نبذة تعريفية",
                            fontSize = 12.sp,
                            color = if (userProfile.bio.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .then(
                                    if (isOnOwnProfile) Modifier.clickable { showEditBioDialog = true }
                                    else Modifier
                                )
                        )
                    }
                    val isDarkTheme = isSystemInDarkTheme()
                    // Content type tabs (Posts / Video / Reuse) - selected = black (light) / white+shadow (dark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 24.dp, end = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = "المنشورات",
                            tint = if (isDarkTheme) Color.White.copy(alpha = if (selectedProfileTab == 0) 1f else 0.4f)
                                   else Color.Black.copy(alpha = if (selectedProfileTab == 0) 1f else 0.4f),
                            modifier = Modifier
                                .size(32.dp)
                                .then(
                                    if (isDarkTheme && selectedProfileTab == 0)
                                        Modifier.shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                                    else Modifier
                                )
                                .testTag("profile_tab_posts")
                                .clickable { selectedProfileTab = 0 }
                        )
                        Icon(
                            imageVector = Icons.Outlined.PlayCircleOutline,
                            contentDescription = "فيديو",
                            tint = if (isDarkTheme) Color.White.copy(alpha = if (selectedProfileTab == 1) 1f else 0.4f)
                                   else Color.Black.copy(alpha = if (selectedProfileTab == 1) 1f else 0.4f),
                            modifier = Modifier
                                .size(32.dp)
                                .then(
                                    if (isDarkTheme && selectedProfileTab == 1)
                                        Modifier.shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                                    else Modifier
                                )
                                .testTag("profile_tab_video")
                                .clickable { selectedProfileTab = 1 }
                        )
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = "إعادة استخدام",
                            tint = if (isDarkTheme) Color.White.copy(alpha = if (selectedProfileTab == 2) 1f else 0.4f)
                                   else Color.Black.copy(alpha = if (selectedProfileTab == 2) 1f else 0.4f),
                            modifier = Modifier
                                .size(32.dp)
                                .then(
                                    if (isDarkTheme && selectedProfileTab == 2)
                                        Modifier.shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                                    else Modifier
                                )
                                .testTag("profile_tab_reuse")
                                .clickable { selectedProfileTab = 2 }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (selectedProfileTab) {
                        0 -> {
                            userPosts.forEach { post ->
                                PostCard(
                                    post = post,
                                    onLikeClick = { },
                                    onCommentClick = { },
                                    onShareClick = { }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        1 -> {
                            // TODO: تبويب الفيديو - يُنفذ لاحقاً بطلب صريح من المستخدم
                        }
                        2 -> {
                            Text(
                                text = "لا يوجد إعادة استخدام بعد",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            )
                        }
                    }
           }
        }
    }

    // -------------------------------------------------------------
    // DIALOGS
    // -------------------------------------------------------------

    // Edit Bio Overlay - full-screen, animated, inside the same Box (no separate Dialog window)
    AnimatedVisibility(
        visible = showEditBioDialog,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        var bioText by remember { mutableStateOf(userProfile.bio) }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showEditBioDialog = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "أضف نبذة تعريفية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = {
                        onUpdateBio(bioText)
                        showEditBioDialog = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "حفظ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "${bioText.length}/150",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bioText,
                            onValueChange = { if (it.length <= 150) bioText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = { Text("اكتب نبذة مميزة تعبر عن اهتماماتك", fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    }
    }

    // Followers List - Full Screen
    if (showFollowersDialog) {
        FollowListFullScreen(
            title = "المتابعون",
            users = followersList,
            onDismiss = { showFollowersDialog = false }
        )
    }

    // Following List - Full Screen
    if (showFollowingDialog) {
        FollowListFullScreen(
            title = "يتابع",
            users = followingList,
            onDismiss = { showFollowingDialog = false }
        )
    }
}

private fun FollowUser.toUserProfile(): UserProfile {
    return UserProfile(
        id = id,
        name = name,
        handle = handle,
        avatarUrl = avatarUrl
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowListFullScreen(
    title: String,
    users: List<FollowUser>,
    onDismiss: () -> Unit
) {
    var selectedUser by remember { mutableStateOf<FollowUser?>(null) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setBackgroundDrawable(
                ColorDrawable(android.graphics.Color.WHITE)
            )
        }
        Surface(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                )
                if (users.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "لا يوجد أحد هنا بعد",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(users) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedUser = user }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (user.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = user.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            user.name.take(1),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        user.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        "ID: ${user.id}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedUser != null) {
        Dialog(
            onDismissRequest = { selectedUser = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val innerView = LocalView.current
            SideEffect {
                (innerView.parent as? DialogWindowProvider)?.window?.setBackgroundDrawable(
                    ColorDrawable(android.graphics.Color.WHITE)
                )
            }
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                ProfileScreen(
                    userProfile = selectedUser!!.toUserProfile(),
                    posts = emptyList(),
                    balance = 0,
                    onUpdateBio = {},
                    onLogout = {},
                    isOnOwnProfile = false
                )
            }
        }
    }
}

private fun saveAvatarToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        context.filesDir.listFiles()
            ?.filter { it.name.startsWith("avatar_") }
            ?.forEach { it.delete() }

        val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ProfileStatItem(title: String, count: String, onClick: (() -> Unit)? = null) {
    Column(
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
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
