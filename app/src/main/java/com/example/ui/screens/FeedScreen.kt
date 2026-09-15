package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    stories: List<Story>,
    posts: List<Post>,
    activeStory: Story?,
    activeCommentPostId: String?,
    onStoryClick: (Story) -> Unit,
    onCloseStory: () -> Unit,
    onAddStory: (String, String?, StoryMediaType, List<Long>) -> Unit = { _, _, _, _ -> },
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onCloseComments: () -> Unit,
    onAddComment: (String, String) -> Unit,
    onShareClick: (Post) -> Unit,
    onPublishPost: (String, String, PostMediaType) -> Unit,
    onFollowClick: (String) -> Unit = {},
    onEditPost: (String, String) -> Unit = { _, _ -> },
    onDeletePost: (String) -> Unit = {},
    onReportPost: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showStoryCreationScreen by remember { mutableStateOf(false) }
    var showCameraPermissionDeniedDialog by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var deletingPost by remember { mutableStateOf<Post?>(null) }
    var reportingPost by remember { mutableStateOf<Post?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showStoryCreationScreen = true
        } else {
            showCameraPermissionDeniedDialog = true
        }
    }

    val handleAddStoryClick = {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            showStoryCreationScreen = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stories Section
            item {
                StoriesBar(
                    stories = stories,
                    onStoryClick = onStoryClick,
                    onAddStoryClick = handleAddStoryClick
                )
            }

            // Quick Create Post Header
            item {
                QuickCreatePostCard(
                    onClick = { showCreatePostDialog = true }
                )
            }

            // Feed Posts
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onLikeClick = { onLikeClick(post.id) },
                    onCommentClick = { onCommentClick(post.id) },
                    onShareClick = { onShareClick(post) },
                    onFollowClick = { onFollowClick(post.id) },
                    onEditClick = { editingPost = post },
                    onDeleteClick = { deletingPost = post },
                    onReportClick = { reportingPost = post }
                )
            }
        }

// Active Story Viewer Modal
        if (activeStory != null) {
            val userStories = stories.filter { it.authorName == activeStory.authorName }
            val startIndex = userStories.indexOfFirst { it.id == activeStory.id }.coerceAtLeast(0)
            StoryViewerDialog(
                stories = userStories,
                initialIndex = startIndex,
                onDismiss = onCloseStory
            )
        }

        // Camera Permission Denied Rationale Dialog
        if (showCameraPermissionDeniedDialog) {
            CameraPermissionRationaleDialog(
                onDismiss = { showCameraPermissionDeniedDialog = false },
                onOpenSettings = {
                    showCameraPermissionDeniedDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                onOpenTextStoryFallback = {
                    showCameraPermissionDeniedDialog = false
                    showStoryCreationScreen = true
                }
            )
        }

        // Add Story Screen (Camera Preview, Side Controls, Video/Photo/Text/Gallery)
        if (showStoryCreationScreen) {
            StoryCreationDialog(
                onDismiss = { showStoryCreationScreen = false },
                onPublishStory = { text, mediaUri, mediaType, colors ->
                    onAddStory(text, mediaUri, mediaType, colors)
                    showStoryCreationScreen = false
                }
            )
        }

        // Create Post Dialog
        if (showCreatePostDialog) {
            CreatePostDialog(
                onDismiss = { showCreatePostDialog = false },
                onPublish = { text, tag, mediaType ->
                    onPublishPost(text, tag, mediaType)
                    showCreatePostDialog = false
                }
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
}

@Composable
fun StoriesBar(
    stories: List<Story>,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add My Story
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(onClick = onAddStoryClick)
                        .testTag("add_story_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة قصة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "قصتي +",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Friends Stories
            items(stories.filter { !it.isCurrentUser }) { story ->
                val ringBrush = if (!story.isViewed) {
                    Brush.sweepGradient(listOf(MujtamaPrimary, MujtamaCoral, MujtamaTeal, MujtamaPrimary))
                } else {
                    Brush.sweepGradient(listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.5f)))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onStoryClick(story) }
                        .testTag("story_${story.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(width = 2.5.dp, brush = ringBrush, shape = CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MujtamaPrimary.copy(alpha = 0.8f), MujtamaTeal.copy(alpha = 0.8f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = story.authorName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = story.authorName.split(" ").firstOrNull() ?: story.authorName,
                        fontSize = 11.sp,
                        fontWeight = if (!story.isViewed) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun QuickCreatePostCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onClick)
            .testTag("quick_create_post_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MujtamaPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "أ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "شارك أفكارك وتحدياتك مع المجتمع...",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "إرفاق وسائط",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onFollowClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onReportClick: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. رأس المنشور (Header):
            // يظهر فقط: اسم المستخدم، تاريخ ووقت النشر
            // بجانب اسم المستخدم مباشرة: زر "متابعة" (Follow) فقط
            // إزالة أي شعارات أو أيقونات أخرى (شارات توثيق، أيقونات إضافية... إلخ)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // الأيقونة الرمزية للمستخدم (الحرف الأول)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MujtamaPrimary, MujtamaTeal)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.authorName.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // اسم المستخدم + زر المتابعة بجانبه مباشرة + تاريخ ووقت النشر فقط
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // زر متابعة (Follow) فقط بجانب اسم المستخدم مباشرة (لغير صاحب المنشور)
                        if (!post.isAuthor) {
                            FilledTonalButton(
                                onClick = onFollowClick,
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("follow_button_${post.id}"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (post.isFollowing)
                                        MaterialTheme.colorScheme.surfaceVariant
                                    else
                                        MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (post.isFollowing)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = if (post.isFollowing) "متابَع" else "متابعة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // تاريخ ووقت النشر فقط
                    Text(
                        text = post.timeAgo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }

                // 2. قائمة الثلاث نقاط (⋮):
                // تحتوي حصراً على ثلاثة خيارات فقط بهذا الترتيب:
                // 1. تعديل
                // 2. حذف المنشور
                // 3. إبلاغ عن مشكلة
                // خيارا التعديل والحذف يظهران فقط لصاحب المنشور، وخيار الإبلاغ لجميع المستخدمين الآخرين
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("post_menu_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات المنشور",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (post.isAuthor) {
                            // 1. تعديل (يظهر لصاحب المنشور فقط)
                            DropdownMenuItem(
                                text = { Text("تعديل", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "تعديل",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                },
                                modifier = Modifier.testTag("menu_edit_post_${post.id}")
                            )

                            // 2. حذف المنشور (يظهر لصاحب المنشور فقط)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "حذف المنشور",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "حذف المنشور",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                                modifier = Modifier.testTag("menu_delete_post_${post.id}")
                            )
                        } else {
                            // 3. إبلاغ عن مشكلة (يظهر للمستخدمين الآخرين)
                            DropdownMenuItem(
                                text = { Text("إبلاغ عن مشكلة", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.ReportProblem,
                                        contentDescription = "إبلاغ عن مشكلة",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onReportClick()
                                },
                                modifier = Modifier.testTag("menu_report_post_${post.id}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post Content Text
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            // Optional Tag
            if (post.tag != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MujtamaTeal.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = post.tag,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MujtamaTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Post Media Representation
            if (post.mediaType != PostMediaType.NONE) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF261D42),
                                    Color(0xFF161226)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (post.mediaType == PostMediaType.SHORT_VIDEO) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MujtamaCoral.copy(alpha = 0.9f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "تشغيل الفيديو",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = post.mediaCaption ?: "فيديو قصير مجتمعي",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = "صورة مرفقة",
                                tint = MujtamaGold,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = post.mediaCaption ?: "صورة المنشور المميزة",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Like, Comment, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                TextButton(
                    onClick = onLikeClick,
                    modifier = Modifier.testTag("post_like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "إعجاب",
                        tint = if (post.isLiked) MujtamaCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount}",
                        fontWeight = if (post.isLiked) FontWeight.Bold else FontWeight.Normal,
                        color = if (post.isLiked) MujtamaCoral else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Comment Button
                TextButton(
                    onClick = onCommentClick,
                    modifier = Modifier.testTag("post_comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "تعليق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Share Button
                TextButton(
                    onClick = onShareClick,
                    modifier = Modifier.testTag("post_share_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "مشاركة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.sharesCount}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "التعليقات (${post.commentsCount})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Comments List
            if (post.commentsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "كن أول من يعلق على هذا المنشور! 💬",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    post.commentsList.forEach { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MujtamaPrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = comment.authorName.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = MujtamaPrimary
                                )
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = comment.authorName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = comment.timeAgo,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment.text,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // New Comment Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("اكتب تعليقك هنا...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            onAddComment(newCommentText)
                            newCommentText = ""
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.testTag("send_comment_button")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال")
                }
            }
        }
    }
}

@Composable
fun StoryViewerDialog(
    stories: List<Story>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (stories.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialIndex) { stories.size }
    val coroutineScope = rememberCoroutineScope()

    // Auto-advance every 5 seconds, unless it's the last story
    LaunchedEffect(pagerState.currentPage) {
        delay(5000)
        if (pagerState.currentPage < stories.size - 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        } else {
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .testTag("story_viewer_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val story = stories[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF3F1968), Color(0xFF140D26))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Progress bars for all stories of this user
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            stories.forEachIndexed { index, _ ->
                                LinearProgressIndicator(
                                    progress = {
                                        when {
                                            index < pagerState.currentPage -> 1f
                                            index == pagerState.currentPage -> 1f
                                            else -> 0f
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MujtamaGold,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Author info
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
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MujtamaTeal),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = story.authorName.take(1),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = story.authorName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = story.timeAgo,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.White
                                )
                            }
                        }

                        // Story Media / Text / Visual Canvas
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!story.mediaUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = story.mediaUri,
                                    contentDescription = "محتوى القصة",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (story.mediaType == StoryMediaType.VIDEO) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "فيديو ستوري",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (story.mediaText.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (story.mediaUri.isNullOrBlank()) Color.Transparent else Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(if (story.mediaUri.isNullOrBlank()) Alignment.Center else Alignment.BottomCenter)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = story.mediaText,
                                        color = Color.White,
                                        fontSize = if (story.mediaUri.isNullOrBlank()) 22.sp else 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = if (story.mediaUri.isNullOrBlank()) 32.sp else 22.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        // Quick Story Reactions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("❤️", "🔥", "👏", "🏆", "🌟").forEach { emoji ->
                                Surface(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .clickable { onDismiss() },
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = emoji, fontSize = 20.sp)
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

                    // Quick Story Reactions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("❤️", "🔥", "👏", "🏆", "🌟").forEach { emoji ->
                            Surface(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { onDismiss() },
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog displayed when camera permission is denied by the user.
 * Provides clear explanation and a direct link to app settings as requested.
 */
@Composable
fun CameraPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTextStoryFallback: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.VideocamOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "صلاحية الكاميرا مطلوبة",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "يتطلب إنشاء القصة الوصول إلى الكاميرا لالتقاط الصور وتسجيل مقاطع الفيديو مباشرة.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "يمكنك منح الإذن من إعدادات التطبيق أو الاستمرار بإنشاء ستوري نصي فقط.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("open_settings_button")
            ) {
                Text("فتح إعدادات التطبيق")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = onOpenTextStoryFallback,
                    modifier = Modifier.testTag("text_story_fallback_button")
                ) {
                    Text("ستوري نصي")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_permission_dialog")
                ) {
                    Text("إلغاء")
                }
            }
        }
    )
}

enum class StoryCreationMode {
    PHOTO,  // Camera photo mode (default)
    VIDEO,  // Camera video mode
    TEXT    // Text story mode with gradient backgrounds
}

/**
 * Full-screen Story Creation Interface:
 * 1. Default UI is Camera Preview ready for still photo capture.
 * 2. Vertical mode buttons on the right side: "نص", "فيديو", "اختيار من المعرض".
 * 3. Seamless switching between modes without reopening or closing the screen.
 */
@Composable
fun StoryCreationDialog(
    onDismiss: () -> Unit,
    onPublishStory: (String, String?, StoryMediaType, List<Long>) -> Unit
) {
    var currentMode by remember { mutableStateOf(StoryCreationMode.PHOTO) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }

    // Media Review state (when photo captured, video recorded, or gallery item picked)
    var capturedMediaUri by remember { mutableStateOf<String?>(null) }
    var isReviewing by remember { mutableStateOf(false) }
    var isVideoStory by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("") }

    // Video recording state
    var isRecording by remember { mutableStateOf(false) }
    var recordDuration by remember { mutableIntStateOf(0) }

    // Text story state
    var textStoryContent by remember { mutableStateOf("") }
    val gradientPalettes = remember {
        listOf(
            listOf(0xFF673AB7, 0xFF00897B), // Purple Teal
            listOf(0xFFFF5722, 0xFFFFB300), // Sunset Orange
            listOf(0xFF00796B, 0xFF43A047), // Emerald Forest
            listOf(0xFF880E4F, 0xFFFF4081), // Magenta Rose
            listOf(0xFF1A237E, 0xFF00B0FF)  // Midnight Cyan
        )
    }
    var selectedGradientIndex by remember { mutableIntStateOf(0) }

    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordDuration = 0
            while (isRecording) {
                delay(1000)
                recordDuration++
                if (recordDuration >= 30) {
                    isRecording = false
                    isVideoStory = true
                    capturedMediaUri = "https://images.unsplash.com/photo-1579208575657-c595a05383b7?auto=format&fit=crop&w=800&q=80"
                    isReviewing = true
                    break
                }
            }
        }
    }

    // Google Play Policy compliant zero-permission media picker
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            capturedMediaUri = uri.toString()
            isVideoStory = false
            isReviewing = true
        }
    }

    Dialog(
        onDismissRequest = {
            if (isRecording) {
                isRecording = false
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Screen Body: Review / Text / Live Camera
            if (isReviewing && capturedMediaUri != null) {
                // Review Captured or Selected Media
                StoryReviewView(
                    mediaUri = capturedMediaUri!!,
                    isVideo = isVideoStory,
                    caption = captionText,
                    onCaptionChange = { captionText = it },
                    onRetake = {
                        isReviewing = false
                        capturedMediaUri = null
                        captionText = ""
                    },
                    onPublish = {
                        onPublishStory(
                            captionText,
                            capturedMediaUri,
                            if (isVideoStory) StoryMediaType.VIDEO else StoryMediaType.PHOTO,
                            gradientPalettes[selectedGradientIndex]
                        )
                    }
                )
            } else if (currentMode == StoryCreationMode.TEXT) {
                // Mode 1: Text Story (خلفية ملوّنة + نص)
                TextStoryView(
                    text = textStoryContent,
                    onTextChange = { textStoryContent = it },
                    gradientPalettes = gradientPalettes,
                    selectedGradientIndex = selectedGradientIndex,
                    onSelectGradient = { selectedGradientIndex = it },
                    onPublish = {
                        if (textStoryContent.isNotBlank()) {
                            onPublishStory(
                                textStoryContent,
                                null,
                                StoryMediaType.TEXT,
                                gradientPalettes[selectedGradientIndex]
                            )
                        }
                    },
                    onBackToCamera = {
                        currentMode = StoryCreationMode.PHOTO
                    }
                )
            } else {
                // Camera View (PHOTO or VIDEO mode)
                // 1. Live Camera Preview
                CameraPreviewView(
                    modifier = Modifier.fillMaxSize(),
                    lensFacing = lensFacing
                )

                // Top Controls: Close, Flash, Flip Camera, and Active Mode Badge
                StoryTopBar(
                    currentMode = currentMode,
                    isRecording = isRecording,
                    recordDuration = recordDuration,
                    isFlashOn = isFlashOn,
                    onToggleFlash = { isFlashOn = !isFlashOn },
                    onFlipCamera = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else
                            CameraSelector.LENS_FACING_BACK
                    },
                    onClose = onDismiss
                )

                // Bottom Shutter Controls
                StoryBottomShutterBar(
                    currentMode = currentMode,
                    isRecording = isRecording,
                    onCapturePhoto = {
                        capturedMediaUri = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80"
                        isVideoStory = false
                        isReviewing = true
                    },
                    onToggleRecordVideo = {
                        if (isRecording) {
                            isRecording = false
                            isVideoStory = true
                            capturedMediaUri = "https://images.unsplash.com/photo-1579208575657-c595a05383b7?auto=format&fit=crop&w=800&q=80"
                            isReviewing = true
                        } else {
                            isRecording = true
                        }
                    }
                )
            }

            // 2. Right Side Vertical Selection Buttons (strictly on the right side of the screen)
            if (!isReviewing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 16.dp, top = 95.dp),
                    contentAlignment = AbsoluteAlignment.TopRight
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        // 1. زر نص — للتبديل إلى وضع كتابة ستوري نصي (خلفية ملوّنة + نص)
                        StorySideToolButton(
                            icon = Icons.Outlined.TextFields,
                            label = "نص",
                            isSelected = (currentMode == StoryCreationMode.TEXT),
                            onClick = {
                                currentMode = if (currentMode == StoryCreationMode.TEXT) {
                                    StoryCreationMode.PHOTO
                                } else {
                                    StoryCreationMode.TEXT
                                }
                            },
                            testTag = "side_button_mode_text"
                        )

                        // 2. زر فيديو — لتفعيل وضع تسجيل فيديو بدلاً من صورة ثابتة
                        StorySideToolButton(
                            icon = Icons.Outlined.Videocam,
                            label = "فيديو",
                            isSelected = (currentMode == StoryCreationMode.VIDEO),
                            onClick = {
                                currentMode = if (currentMode == StoryCreationMode.VIDEO) {
                                    StoryCreationMode.PHOTO
                                } else {
                                    StoryCreationMode.VIDEO
                                }
                            },
                            testTag = "side_button_mode_video"
                        )

                        // 3. زر اختيار من المعرض — لفتح معرض الصور/الفيديوهات واختيار محتوى جاهز
                        StorySideToolButton(
                            icon = Icons.Outlined.PhotoLibrary,
                            label = "المعرض",
                            isSelected = false,
                            onClick = {
                                galleryPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            testTag = "side_button_mode_gallery"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StorySideToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MujtamaPrimary else Color.Black.copy(alpha = 0.5f)
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MujtamaGold else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MujtamaGold else Color.White
        )
    }
}

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isBound by remember { mutableStateOf(false) }
    var bindError by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (!bindError) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()
                            if (cameraProvider.hasCamera(cameraSelector)) {
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                                isBound = true
                            } else {
                                bindError = true
                            }
                        } catch (e: Exception) {
                            bindError = true
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (bindError || !isBound) {
            CameraSimulationView(modifier = Modifier.fillMaxSize())
        }

        CameraViewfinderOverlay(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun CameraSimulationView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF252835),
                        Color(0xFF151720),
                        Color(0xFF0B0C10)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(38.dp)
                )
            }
            Text(
                text = "عدسة الكاميرا المباشرة",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CameraViewfinderOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // Subtle center focus brackets
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.Center)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(10.dp)
                )
        )
    }
}

@Composable
fun StoryTopBar(
    currentMode: StoryCreationMode,
    isRecording: Boolean,
    recordDuration: Int,
    isFlashOn: Boolean,
    onToggleFlash: () -> Unit,
    onFlipCamera: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                .testTag("close_story_creation_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "إغلاق",
                tint = Color.White
            )
        }

        // Mode or Timer Badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isRecording) Color.Red.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "تسجيل 00:${if (recordDuration < 10) "0$recordDuration" else recordDuration}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = if (currentMode == StoryCreationMode.VIDEO) "وضع الفيديو 🎥" else "التقاط صورة 📸",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Action Icons (Flash, Flip Camera)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onToggleFlash,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Outlined.FlashOff,
                    contentDescription = "الفلاش",
                    tint = if (isFlashOn) MujtamaGold else Color.White
                )
            }

            IconButton(
                onClick = onFlipCamera,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FlipCameraAndroid,
                    contentDescription = "تبديل الكاميرا",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun StoryBottomShutterBar(
    currentMode: StoryCreationMode,
    isRecording: Boolean,
    onCapturePhoto: () -> Unit,
    onToggleRecordVideo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 44.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentMode == StoryCreationMode.PHOTO) {
                // Still Photo Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onCapturePhoto)
                        .testTag("camera_photo_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                }
                Text(
                    text = "اضغط للالتقاط",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            } else if (currentMode == StoryCreationMode.VIDEO) {
                // Video Recording Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, Color.Red, CircleShape)
                        .padding(if (isRecording) 18.dp else 6.dp)
                        .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                        .background(Color.Red)
                        .clickable(onClick = onToggleRecordVideo)
                        .testTag("camera_video_shutter_button")
                )
                Text(
                    text = if (isRecording) "اضغط لإيقاف التسجيل" else "اضغط لبدء تسجيل الفيديو",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StoryReviewView(
    mediaUri: String,
    isVideo: Boolean,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onRetake: () -> Unit,
    onPublish: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Media Preview
        AsyncImage(
            model = mediaUri,
            contentDescription = "معاينة القصة",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Subtle gradient overlay at top and bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        // Top Retake / Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onRetake,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("retake_media_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "إعادة",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("إعادة التصوير")
            }

            if (isVideo) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("مقطع فيديو", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Bottom Caption and Publish Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = caption,
                onValueChange = onCaptionChange,
                placeholder = { Text("أضف تعليقاً على القصة... ✍️", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("story_caption_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MujtamaGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.45f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp),
                maxLines = 3
            )

            Button(
                onClick = onPublish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_story_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MujtamaPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "نشر القصة الآن",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun TextStoryView(
    text: String,
    onTextChange: (String) -> Unit,
    gradientPalettes: List<List<Long>>,
    selectedGradientIndex: Int,
    onSelectGradient: (Int) -> Unit,
    onPublish: () -> Unit,
    onBackToCamera: () -> Unit
) {
    val currentColors = gradientPalettes[selectedGradientIndex].map { Color(it) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(currentColors))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToCamera,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "العودة للكاميرا",
                        tint = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = "ستوري نصي ✍️",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                FilledTonalButton(
                    onClick = onPublish,
                    enabled = text.isNotBlank(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MujtamaGold,
                        contentColor = MujtamaPrimaryDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("publish_text_story_button")
                ) {
                    Text("نشر", fontWeight = FontWeight.Bold)
                }
            }

            // Centered Story Text Input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = {
                        Text(
                            text = "اكتب ما يجول في خاطرك هنا... ✨",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("text_story_input"),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    minLines = 3,
                    maxLines = 8
                )
            }

            // Bottom Palette Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اختر لون الخلفية",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    gradientPalettes.forEachIndexed { index, palette ->
                        val isSelected = (index == selectedGradientIndex)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(palette.map { Color(it) }))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { onSelectGradient(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenTextComposer(
    onDismiss: () -> Unit,
    onPublish: (String, String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                    Text(
                        text = "منشور جديد",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("ماذا يدور في ذهنك؟ شارك أعضاء المجتمع...") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    )
                )

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("الوسم (اختياري)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )

                Button(
                    onClick = { onPublish(content, tag) },
                    enabled = content.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("نشر")
                }
            }
        }
    }
}

@Composable
fun FullScreenMediaComposer(
    mediaUri: Uri,
    onDismiss: () -> Unit,
    onPublish: (String, String, PostMediaType) -> Unit
) {
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    val isVideo = remember(mediaUri) {
        context.contentResolver.getType(mediaUri)?.startsWith("video") == true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                    Text(
                        text = "منشور جديد",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "فيديو",
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    } else {
                        AsyncImage(
                            model = mediaUri,
                            contentDescription = "الوسائط المختارة",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("أضف تعليقاً...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tag,
                        onValueChange = { tag = it },
                        label = { Text("الوسم (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onPublish(caption, tag, if (isVideo) PostMediaType.SHORT_VIDEO else PostMediaType.IMAGE)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("نشر")
                    }
                }
            }
        }
    }
}

@Composable
fun EditPostDialog(
    post: Post,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var contentText by remember { mutableStateOf(post.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعديل المنشور",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .testTag("edit_post_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(contentText) },
                enabled = contentText.isNotBlank(),
                modifier = Modifier.testTag("save_edit_post_button")
            ) {
                Text("حفظ التعديل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun DeletePostConfirmDialog(
    post: Post,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "حذف المنشور",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                text = "هل أنت متأكد من رغبتك في حذف هذا المنشور نهائياً؟ لا يمكن التراجع عن هذا الإجراء.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_post_button")
            ) {
                Text("حذف المنشور")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ReportPostDialog(
    post: Post,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val reportReasons = listOf(
        "محتوى غير لائق أو مسيء",
        "مضايقة أو إساءة موجهة",
        "معلومات مضللة أو احتيال",
        "محتوى مكرر أو سبام"
    )
    var selectedReason by remember { mutableStateOf(reportReasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إبلاغ عن مشكلة",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "يرجى اختيار سبب البلاغ لإرساله للمشرفين:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                reportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedReason == reason),
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = reason, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_report_post_button")
            ) {
                Text("إرسال البلاغ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
