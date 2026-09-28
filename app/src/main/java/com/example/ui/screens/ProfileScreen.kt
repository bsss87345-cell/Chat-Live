package com.example.ui.screens

import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
    onLikeClick: (String) -> Unit = {},
    onShareClick: (Post) -> Unit = {},
    activeCommentPostId: String? = null,
    onCommentClick: (String) -> Unit = {},
    onCloseComments: () -> Unit = {},
    onAddComment: (String, String) -> Unit = { _, _ -> },
    onEditPost: (String, String) -> Unit = { _, _ -> },
    onDeletePost: (String) -> Unit = {},
    onReportPost: (String) -> Unit = {},
    followersList: List<FollowUser> = emptyList(),
    followingList: List<FollowUser> = emptyList(),
    isOnOwnProfile: Boolean = true,
    onLoadProfileFor: (String) -> UserProfile? = { null },
    onLoadFollowersFor: (String) -> List<FollowUser> = { emptyList() },
    onLoadFollowingFor: (String) -> List<FollowUser> = { emptyList() },
    onBackFromOtherProfile: () -> Unit = {},
    onLoadPostsFor: (String) -> List<Post> = { emptyList() },
    isUserFollowing: (String) -> Boolean = { false },
    onToggleFollow: (String) -> Unit = {}
) {
    var showEditBioDialog by remember { mutableStateOf(false) }
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var deletingPost by remember { mutableStateOf<Post?>(null) }
    var reportingPost by remember { mutableStateOf<Post?>(null) }
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

    // User's own posts (own profile) or another user's posts (other profile) - used for the header stats count and grid
    val userPosts = if (isOnOwnProfile) {
        posts.filter {
            it.authorHandle == userProfile.handle ||
            it.authorHandle == "ID: ${userProfile.id}" ||
            it.authorHandle == "@user_me" ||
            it.id.startsWith("post_") ||
            it.id.startsWith("p_")
        }.take(6)
    } else {
        onLoadPostsFor(userProfile.id)
    }
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
                    // Top bar: add-friend (physical right) + settings (physical left)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isOnOwnProfile) {
                            IconButton(
                                onClick = onBackFromOtherProfile,
                                modifier = Modifier.size(36.dp).testTag("profile_back_button")
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                        } else {
                            IconButton(
                                onClick = { },
                                modifier = Modifier.size(36.dp).testTag("profile_add_friend_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "إضافة صديق", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (isOnOwnProfile) {
                            IconButton(
                                onClick = { onOpenAccountSettings() },
                                modifier = Modifier.size(36.dp).testTag("profile_settings_menu_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = userProfile.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonCyan))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Online", fontSize = 13.sp, color = NeonCyan)
                        }
                    }

                    var avatarGlowStarted by remember { mutableStateOf(false) }
                    val avatarGlowAlpha by animateFloatAsState(
                        targetValue = if (avatarGlowStarted) 1f else 0f,
                        animationSpec = tween(durationMillis = 1200),
                        label = "avatarGlow"
                    )
                    LaunchedEffect(Unit) { avatarGlowStarted = true }
                    Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier.size(112.dp).clip(HexagonShape)
                                .background(NeonCyan.copy(alpha = 0.25f * avatarGlowAlpha))
                        )
                        Box(
                            modifier = Modifier.size(96.dp).clip(HexagonShape)
                                .background(DarkSurface)
                                .border(2.dp, NeonCyan, HexagonShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userProfile.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = userProfile.avatarUrl,
                                    contentDescription = "صورة الملف الشخصي",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(HexagonShape)
                                )
                            } else {
                                Text(text = userProfile.avatarEmoji, fontSize = 40.sp)
                            }
                        }
                        if (isOnOwnProfile) {
                            Box(
                                modifier = Modifier.align(Alignment.BottomEnd).size(28.dp)
                                    .clip(CircleShape).background(NeonCyan)
                                    .clickable {
                                        avatarImageLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تغيير صورة الملف الشخصي", tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = userProfile.handle, fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, contentDescription = "موثّق", tint = NeonCyan, modifier = Modifier.size(14.dp))
                    }

                    if (userProfile.bio.isNotBlank() || isOnOwnProfile) {
                        Text(
                            text = if (userProfile.bio.isNotBlank()) userProfile.bio else "أضف نبذة تعريفية",
                            fontSize = 13.sp,
                            color = if (userProfile.bio.isNotBlank()) TextPrimary else TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                .then(if (isOnOwnProfile) Modifier.clickable { showEditBioDialog = true } else Modifier)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileStatBox(Icons.Default.Groups, "${userProfile.followersCount}", "متابعون", NeonCyan, Modifier.weight(1f)) { showFollowersDialog = true }
                        ProfileStatBox(Icons.Default.Person, "${userProfile.followingCount}", "يتابع", NeonPurple, Modifier.weight(1f)) { showFollowingDialog = true }
                        ProfileStatBox(Icons.Default.Home, "$totalPostsCount", "غرفة", NeonCyan, Modifier.weight(1f), null)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showEditBioDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعديل الملف", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, NeonPurple),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPurple)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(50))
                                .background(NeonCyan).clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = "توثيق", tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ProfileHexIcon(Icons.Default.EmojiEvents, "إنجازات", NeonPurple)
                        ProfileHexIcon(Icons.Default.Movie, "مقاطع", NeonCyan)
                        ProfileHexIcon(Icons.Default.Groups, "الأصدقاء", NeonPurple)
                        ProfileHexIcon(Icons.Default.Home, "غرفي", NeonCyan)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 24.dp, end = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = "المنشورات",
                            tint = if (selectedProfileTab == 0) NeonPurple else TextSecondary,
                            modifier = Modifier.size(30.dp).testTag("profile_tab_posts").clickable { selectedProfileTab = 0 }
                        )
                        Icon(
                            imageVector = Icons.Outlined.PlayCircleOutline,
                            contentDescription = "فيديو",
                            tint = if (selectedProfileTab == 1) NeonPurple else TextSecondary,
                            modifier = Modifier.size(30.dp).testTag("profile_tab_video").clickable { selectedProfileTab = 1 }
                        )
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = "إعادة استخدام",
                            tint = if (selectedProfileTab == 2) NeonPurple else TextSecondary,
                            modifier = Modifier.size(30.dp).testTag("profile_tab_reuse").clickable { selectedProfileTab = 2 }
                        )
                    }
                    }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        when (selectedProfileTab) {
            0 -> {
                items(userPosts) { post ->
                    PostCard(
                        post = post,
                        isFollowing = isUserFollowing(post.authorId),
                        onLikeClick = { onLikeClick(post.id) },
                        onCommentClick = { onCommentClick(post.id) },
                        onShareClick = { onShareClick(post) },
                        onFollowClick = { onToggleFollow(post.authorId) },
                        onEditClick = { editingPost = post },
                        onDeleteClick = { deletingPost = post },
                        onReportClick = { reportingPost = post }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            1 -> {
                item {
                    // TODO: تبويب الفيديو - يُنفذ لاحقاً بطلب صريح من المستخدم
                }
            }
            2 -> {
                item {
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
            onDismiss = { showFollowersDialog = false },
            onLoadProfileFor = onLoadProfileFor,
            onLoadFollowersFor = onLoadFollowersFor,
            onLoadFollowingFor = onLoadFollowingFor,
            onLoadPostsFor = onLoadPostsFor,
            isUserFollowing = isUserFollowing,
            onToggleFollow = onToggleFollow
        )
    }

    // Following List - Full Screen
    if (showFollowingDialog) {
        FollowListFullScreen(
            title = "يتابع",
            users = followingList,
            onDismiss = { showFollowingDialog = false },
            onLoadProfileFor = onLoadProfileFor,
            onLoadFollowersFor = onLoadFollowersFor,
            onLoadFollowingFor = onLoadFollowingFor,
            onLoadPostsFor = onLoadPostsFor,
            isUserFollowing = isUserFollowing,
            onToggleFollow = onToggleFollow
        )
    }
    // Comments Bottom Sheet
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

    // Edit Post Dialog
    editingPost?.let { post ->
        EditPostDialog(
            post = post,
            onDismiss = { editingPost = null },
            onSave = { newContent ->
                onEditPost(post.id, newContent)
                editingPost = null
            }
        )
    }

    // Delete Post Confirmation Dialog
    deletingPost?.let { post ->
        DeletePostConfirmDialog(
            post = post,
            onDismiss = { deletingPost = null },
            onConfirm = {
                onDeletePost(post.id)
                deletingPost = null
            }
        )
    }

    // Report Post Dialog
    reportingPost?.let { post ->
        ReportPostDialog(
            post = post,
            onDismiss = { reportingPost = null },
            onConfirm = { reason ->
                onReportPost(post.id)
                reportingPost = null
            }
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
    onDismiss: () -> Unit,
    onLoadProfileFor: (String) -> UserProfile? = { null },
    onLoadFollowersFor: (String) -> List<FollowUser> = { emptyList() },
    onLoadFollowingFor: (String) -> List<FollowUser> = { emptyList() },
    onLoadPostsFor: (String) -> List<Post> = { emptyList() },
    isUserFollowing: (String) -> Boolean = { false },
    onToggleFollow: (String) -> Unit = {}
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
                            Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
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
                    userProfile = onLoadProfileFor(selectedUser!!.id) ?: selectedUser!!.toUserProfile(),
                    posts = emptyList(),
                    balance = 0,
                    onUpdateBio = {},
                    onLogout = {},
                    followersList = onLoadFollowersFor(selectedUser!!.id),
                    followingList = onLoadFollowingFor(selectedUser!!.id),
                    isOnOwnProfile = false,
                    onLoadProfileFor = onLoadProfileFor,
                    onLoadFollowersFor = onLoadFollowersFor,
                    onLoadFollowingFor = onLoadFollowingFor,
                    onBackFromOtherProfile = { selectedUser = null },
                    onLoadPostsFor = onLoadPostsFor,
                    isUserFollowing = isUserFollowing,
                    onToggleFollow = onToggleFollow
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

private val HexagonShape = androidx.compose.ui.graphics.GenericShape { size, _ ->
    val w = size.width; val h = size.height
    moveTo(w * 0.5f, 0f)
    lineTo(w, h * 0.25f)
    lineTo(w, h * 0.75f)
    lineTo(w * 0.5f, h)
    lineTo(0f, h * 0.75f)
    lineTo(0f, h * 0.25f)
    close()
}

@Composable
private fun ProfileStatBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    label: String,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(DarkSurface.copy(alpha = 0.4f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = borderColor, modifier = Modifier.size(18.dp))
        Text(count, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun ProfileHexIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier.size(58.dp).clip(HexagonShape)
                .background(DarkSurface).border(1.5.dp, color, HexagonShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Text(label, fontSize = 11.sp, color = Color.White)
    }
}
