package com.example.ui.screens

import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.layout
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
import com.example.ui.share.shareProfileExternally
import com.example.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
private fun ProfileNeonBackground(modifier: Modifier = Modifier) {
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    posts: List<Post>,
    balance: Int,
    onUpdateBio: (String) -> Unit,
        onUpdateHandle: (String) -> Unit = {},
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
    onToggleFollow: (String) -> Unit = {},
    chatRooms: List<ChatRoom> = emptyList(),
    onOpenRoom: (String) -> Unit = {}
) {
    val profileShareContext = LocalContext.current
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

    // المنشورات المعروضة حسب التبويب: 0 = منشوراتي · 3 = اللي أعجبت بها
    val displayedPosts = when (selectedProfileTab) {
        0 -> userPosts
        3 -> posts.filter { it.isLiked }
        else -> emptyList()
    }

    // نفس منطق الرئيسية: الفيديو الأقرب لمنتصف الشاشة هو اللي يشتغل
    val profileListState = rememberLazyListState()
    val profileVideoIds = displayedPosts
        .filter { it.mediaType == PostMediaType.SHORT_VIDEO }
        .map { it.id }
        .toSet()
    val activeProfileVideoId by remember(profileVideoIds) {
        derivedStateOf {
            val layout = profileListState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo
                .filter { info -> (info.key as? String) in profileVideoIds }
                .minByOrNull { info -> kotlin.math.abs(info.offset + info.size / 2 - center) }
                ?.key as? String
        }
    }
    val isProfileScrolling by remember { derivedStateOf { profileListState.isScrollInProgress } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
    ProfileNeonBackground()
        LazyColumn(
        state = profileListState,
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
                    // Top bar: add-friend (right) + name/Online (center) + settings gear (left)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isOnOwnProfile) {
                            IconButton(
                                onClick = onBackFromOtherProfile,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("profile_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "رجوع",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        if (isOnOwnProfile) {
                            IconButton(
                                onClick = { },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("profile_add_friend_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "إضافة صديق",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = userProfile.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            if (isOnOwnProfile) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Online",
                                        fontSize = 12.sp,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                        if (isOnOwnProfile) {
                            IconButton(
                                onClick = { onOpenAccountSettings() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("profile_settings_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "الإعدادات",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
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
                            val hexShape = remember {
    androidx.compose.foundation.shape.GenericShape { size, _ ->
        val r = minOf(size.width, size.height) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        for (i in 0..5) {
            val angle = Math.toRadians((60 * i - 90).toDouble())
            val px = cx + r * kotlin.math.cos(angle).toFloat()
            val py = cy + r * kotlin.math.sin(angle).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
}
Box(
    modifier = Modifier
        .fillMaxSize()
        .shadow(16.dp, hexShape, ambientColor = NeonCyan, spotColor = NeonPurple)
        .background(DarkSurface, hexShape)
        .border(3.dp, Brush.linearGradient(listOf(NeonCyan, NeonPurple)), hexShape)
        .clip(hexShape)
        .clickable(enabled = isOnOwnProfile) {
            avatarImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
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

                       }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
Column(
    modifier = Modifier
        .weight(1f)
        .padding(bottom = 4.dp),
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    Text(
        text = userProfile.name,
        fontWeight = FontWeight.Black,
        fontSize = 20.sp,
        color = Color.White,
        maxLines = 1
    )
    Text(
        text = userProfile.handle,
        fontSize = 13.sp,
        color = TextSecondary,
        maxLines = 1
    )
    Text(
        text = if (userProfile.bio.isNotBlank()) userProfile.bio else "أضف نبذة تعريفية",
        fontSize = 13.sp,
        color = if (userProfile.bio.isNotBlank()) Color.White.copy(alpha = 0.85f) else TextSecondary,
        lineHeight = 18.sp,
        maxLines = 3
    )
}

                    }
                        
                    }

                    Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
    ProfileNeonStatCard(
        title = "المنشورات",
        count = "$totalPostsCount",
        icon = Icons.Outlined.GridView,
        accent = NeonCyan,
        modifier = Modifier.weight(1f)
    )
    ProfileNeonStatCard(
        title = "يتابع",
        count = "${userProfile.followingCount}",
        icon = Icons.Default.Person,
        accent = NeonPurple,
        modifier = Modifier.weight(1f),
        onClick = { showFollowingDialog = true }
    )
    ProfileNeonStatCard(
        title = "المتابعون",
        count = "${userProfile.followersCount}",
        icon = Icons.Default.People,
        accent = NeonCyan,
        modifier = Modifier.weight(1f),
        onClick = { showFollowersDialog = true }
    )
                    }
                    Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
    if (isOnOwnProfile) {
                ProfileNeonActionButton(
            text = "مشاركة",
            icon = Icons.Default.Share,
            accent = NeonPurple,
            filled = false,
            modifier = Modifier.weight(1f),
            onClick = { shareProfileExternally(profileShareContext, userProfile) }
        )
        ProfileNeonActionButton(
                text = "تعديل الملف",
                icon = Icons.Default.Edit,
                accent = NeonCyan,
                filled = true,
                modifier = Modifier.weight(1f),
                onClick = { showEditBioDialog = true }
            )
    } else {
        ProfileNeonActionButton(
            text = if (isUserFollowing(userProfile.id)) "إلغاء المتابعة" else "متابعة",
            icon = Icons.Default.PersonAdd,
            accent = NeonCyan,
            filled = !isUserFollowing(userProfile.id),
            modifier = Modifier.weight(1f),
            onClick = { onToggleFollow(userProfile.id) }
        )
    }
}
                    Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp, start = 16.dp, end = 16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    listOf("المنشورات", "الغرف", "الوسائط", "الإعجابات").forEachIndexed { index, label ->
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { selectedProfileTab = index }
                .testTag("profile_tab_$index"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = if (selectedProfileTab == index) NeonCyan else TextSecondary,
                fontSize = 15.sp,
                fontWeight = if (selectedProfileTab == index) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(if (selectedProfileTab == index) NeonCyan else Color.Transparent)
            )
        }
    }
                    }
                    }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        when (selectedProfileTab) {
                        0, 3 -> {
                if (displayedPosts.isEmpty()) {
                    item {
                        Text(
                            text = if (selectedProfileTab == 0) "ما نشرت شي بعد"
                            else "ما أعجبت بأي منشور بعد",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    }
                }
                items(displayedPosts, key = { it.id }) { post ->
                    val hasRealMedia = remember(post.mediaType, post.mediaUri) {
                        post.mediaType != PostMediaType.NONE &&
                            post.mediaUri.isNotBlank() &&
                            File(post.mediaUri).exists()
                    }
                                        if (hasRealMedia) {
                        // القائمة فيها هامش جانبي 12dp، ومنشور الوسائط لازم يكون حافة لحافة.
                        // الهامش السالب ممنوع بـCompose، فنوسّع العنصر 24dp ونزيحه 12dp يساراً.
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
                                isFollowing = isUserFollowing(post.authorId),
                                onLikeClick = { onLikeClick(post.id) },
                                onCommentClick = { onCommentClick(post.id) },
                                onShareClick = { onShareClick(post) },
                                onFollowClick = { onToggleFollow(post.authorId) },
                                onEditClick = { editingPost = post },
                                onDeleteClick = { deletingPost = post },
                                onReportClick = { reportingPost = post },
                                isActiveVideo = post.mediaType == PostMediaType.SHORT_VIDEO &&
                                    post.id == activeProfileVideoId,
                                canMountVideo = post.mediaType != PostMediaType.SHORT_VIDEO ||
                                    !isProfileScrolling
                            )
                        }
                    } else {
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
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                                }
            }
                        1 -> {
                val joinedRooms = chatRooms.filter { it.isJoined }
                if (joinedRooms.isEmpty()) {
                    item {
                        Text(
                            text = "ما انضممت لأي غرفة بعد",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(joinedRooms, key = { it.id }) { room ->
                        RoomGridCard(
                            room = room,
                            onEnterClick = { onOpenRoom(room.id) }
                        )
                    }
                }
            }
            else -> {
    item {
        Text(
            text = "قريباً",
            fontSize = 13.sp,
            color = TextSecondary,
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
            var handleText by remember { mutableStateOf(userProfile.handle.removePrefix("@")) }
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
                        onUpdateHandle(handleText)
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "اسم المستخدم",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = handleText,
                    onValueChange = { if (it.length <= 50 && !it.contains(" ")) handleText = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("@", fontSize = 16.sp) },
                    singleLine = true,
                    placeholder = { Text("username", fontSize = 12.sp) }
                )
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
                                        user.handle.ifBlank { "@${user.id}" },
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
private fun ProfileNeonStatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cutShape = remember {
        androidx.compose.foundation.shape.GenericShape { size, _ ->
            val c = size.height * 0.22f
            moveTo(c, 0f)
            lineTo(size.width - c, 0f)
            lineTo(size.width, c)
            lineTo(size.width, size.height - c)
            lineTo(size.width - c, size.height)
            lineTo(c, size.height)
            lineTo(0f, size.height - c)
            lineTo(0f, c)
            close()
        }
    }
    Row(
        modifier = modifier
            .shadow(10.dp, cutShape, ambientColor = accent, spotColor = accent)
            .background(DarkSurface, cutShape)
            .border(1.5.dp, accent, cutShape)
            .clip(cutShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1
            )
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
    }
}
@Composable
private fun ProfileNeonActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .shadow(10.dp, shape, ambientColor = accent, spotColor = accent)
            .background(if (filled) accent else DarkSurface, shape)
            .border(1.5.dp, accent, shape)
            .clip(shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) Color.Black else accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (filled) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}
