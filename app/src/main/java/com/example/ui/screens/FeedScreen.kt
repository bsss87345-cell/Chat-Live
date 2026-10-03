package com.example.ui.screens

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageCapture.OutputFileOptions
import java.io.File
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.core.CameraSelector
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.FocusMeteringAction
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.util.concurrent.TimeUnit
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    userProfile: UserProfile,
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
    onPublishPost: (String, String, PostMediaType, String) -> Unit,
    isUserFollowing: (String) -> Boolean = { false },
    onToggleFollow: (String) -> Unit = {},
    onEditPost: (String, String) -> Unit = { _, _ -> },
    onDeletePost: (String) -> Unit = {},
    onReportPost: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val mediaSaveScope = rememberCoroutineScope()
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showTextComposer by remember { mutableStateOf(false) }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val isVideoFile = context.contentResolver.getType(uri)?.startsWith("video") == true
            var durationMs = 0L
            if (isVideoFile) {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    durationMs = retriever
                        .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    retriever.release()
                } catch (e: Exception) {
                    durationMs = 0L
                }
            }
            if (durationMs > 60_000L) {
                android.widget.Toast.makeText(
                    context,
                    "مدة الفيديو أكثر من المسموح، حاول تقليل المدة",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                selectedMediaUri = uri
            }
        }
    }
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

    var startAnimation by remember { mutableStateOf(false) }
    val feedAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "feedFadeIn"
    )
    LaunchedEffect(Unit) {
        startAnimation = true
    }
    val feedListState = rememberLazyListState()
        val activeVideoId by remember(posts) {
        val videoIds = posts
            .filter { it.mediaType == PostMediaType.SHORT_VIDEO }
            .map { it.id }
            .toSet()
        derivedStateOf {
            val layout = feedListState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo
                .filter { info -> (info.key as? String) in videoIds }
                .minByOrNull { info -> kotlin.math.abs(info.offset + info.size / 2 - center) }
                ?.key as? String
                }
        }
    val isFeedScrolling by remember { derivedStateOf { feedListState.isScrollInProgress } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .alpha(feedAlpha)
    ) {
        FeedNeonBackground()
        LazyColumn(
            state = feedListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stories Section
            item {
                StoriesBar(
                    stories = stories,
                    onStoryClick = onStoryClick,
                    onAddStoryClick = handleAddStoryClick,
                    myAvatarUrl = userProfile.avatarUrl
                )
            }

            // Quick Create Post Header
            item {
                QuickCreatePostCard(
                    userProfile = userProfile,
                    onTextClick = { showTextComposer = true },
                    onGalleryClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )
            }

            // Feed Posts
                        items(posts, key = { it.id }) { post ->
                val hasRealMedia = remember(post.mediaType, post.mediaUri) {
                    post.mediaType != PostMediaType.NONE &&
                        post.mediaUri.isNotBlank() &&
                        File(post.mediaUri).exists()
                }
                if (hasRealMedia) {
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
                            post.id == activeVideoId,
                        canMountVideo = post.mediaType != PostMediaType.SHORT_VIDEO ||
                            !isFeedScrolling
                    )
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
                        onReportClick = { reportingPost = post },
                        // المنشور النصي ما فيه فيديو، فما يقرأ activeVideoId ولا isFeedScrolling
                        // وبهذا ما ينبّه نفسه مع كل تغيير لهما أثناء التمرير
                        isActiveVideo = false,
                        canMountVideo = true
                    )
                }
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

        // Full-screen text-only composer
        if (showTextComposer) {
            FullScreenTextComposer(
                onDismiss = { showTextComposer = false },
                onPublish = { text, tag ->
                    onPublishPost(text, tag, PostMediaType.NONE, "")
                    showTextComposer = false
                }
            )
        }

        // Full-screen media (image/video) composer
        selectedMediaUri?.let { uri ->
            FullScreenMediaComposer(
                mediaUri = uri,
                onDismiss = { selectedMediaUri = null },
                                onPublish = { caption, tag, mediaType ->
                val pickedUri = uri
                selectedMediaUri = null
                mediaSaveScope.launch {
                    val savedPath = withContext(Dispatchers.IO) {
                        try {
                            val ext = if (mediaType == PostMediaType.SHORT_VIDEO) "mp4" else "jpg"
                            val outFile = File(context.filesDir, "post_${System.currentTimeMillis()}.$ext")
                            context.contentResolver.openInputStream(pickedUri)?.use { input ->
                                outFile.outputStream().use { output -> input.copyTo(output) }
                            }
                                                        if (outFile.exists() && outFile.length() > 0L) {
                                val savedFilePath = outFile.absolutePath
                                // نحن أصلاً على خيط IO: نحسب نسبة الأبعاد ونخزنها بالكاش
                                // حتى يظهر المنشور بمقاسه الصحيح من أول إطار بلا أي قفزة
                                mediaRatioCache[savedFilePath] = readMediaAspectRatio(
                                    savedFilePath,
                                    mediaType == PostMediaType.SHORT_VIDEO
                                )
                                savedFilePath
                            } else ""
                        } catch (e: Exception) {
                            ""
                        }
                    }
                    if (savedPath.isNotEmpty()) {
                        onPublishPost(caption, tag, mediaType, savedPath)
                    } else {
                        android.widget.Toast.makeText(context, "تعذر حفظ الملف، حاول مرة ثانية", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
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
private fun FeedNeonBackground(modifier: Modifier = Modifier) {
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
fun StoriesBar(
    stories: List<Story>,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit,
    myAvatarUrl: String = ""
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // My Story Avatar (Add Story)
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onAddStoryClick)
                    .testTag("add_story_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = NeonCyan,
                            spotColor = NeonCyan
                        )
                        .border(width = 2.dp, color = NeonCyan, shape = CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (myAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = myAvatarUrl,
                            contentDescription = "إضافة قصة",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة قصة",
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "قصتي +",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Friends Stories
        items(stories.filter { !it.isCurrentUser }) { story ->
            val ringBrush = if (!story.isViewed) {
                Brush.sweepGradient(listOf(NeonCyan, NeonPurple, NeonCyan))
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
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (story.authorAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = story.authorAvatarUrl,
                            contentDescription = story.authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = story.authorName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = story.authorName.split(" ").firstOrNull() ?: story.authorName,
                    fontSize = 11.sp,
                    fontWeight = if (!story.isViewed) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    color = Color.White
                )
            }
        }
    }
}
@Composable
fun QuickCreatePostCard(
    userProfile: UserProfile,
    onTextClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .shadow(
                elevation = 10.dp,
                shape = cardShape,
                ambientColor = NeonPurple.copy(alpha = 0.4f),
                spotColor = NeonPurple.copy(alpha = 0.4f)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.25f), NeonPurple.copy(alpha = 0.7f))
                ),
                shape = cardShape
            )
            .testTag("quick_create_post_card"),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // أيقونة المعرض بإطار متوهج
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    onClick = onGalleryClick,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonPurple),
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = NeonPurple,
                            spotColor = NeonPurple
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "إرفاق صورة أو فيديو",
                            tint = NeonPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            // حقل النص كبسولة زجاجية بعرض كامل
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTextClick),
                shape = RoundedCornerShape(50),
                color = DarkBackground.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(NeonCyan.copy(alpha = 0.5f), NeonPurple.copy(alpha = 0.5f))
                    )
                )
            ) {
                Text(
                    text = "شارك أفكارك وتحدياتك مع المجتمع...",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

private val mediaRatioCache = java.util.concurrent.ConcurrentHashMap<String, Float>()

// ثوابت على مستوى الملف: تُنشأ مرة وحدة بعمر التطبيق بدل مرة لكل بطاقة بالقائمة.
// مهم: ثبات كائن الفرشاة يخلي كاش مسار الإطار داخل Modifier.border يشتغل فعلاً.
private val PostCardShape = RoundedCornerShape(18.dp)
private val PostCardGlowColor = NeonPurple.copy(alpha = 0.3f)
private val PostCardBorderBrush = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.6f))
)
/**
 * محمّل صور مخصص يعرف يفك ترميز أول لقطة من ملفات الفيديو.
 * محمّل Coil الافتراضي ما يدعم الفيديو إلا بتسجيل VideoFrameDecoder.
 */
@Composable
private fun rememberVideoFrameLoader(): coil.ImageLoader {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return remember(ctx) {
        coil.ImageLoader.Builder(ctx)
            .components { add(coil.decode.VideoFrameDecoder.Factory()) }
            .build()
    }
}
private fun readMediaAspectRatio(path: String, isVideo: Boolean): Float {
    var w = 0
    var h = 0
    try {
                if (isVideo) {
            val r = android.media.MediaMetadataRetriever()
            try {
                r.setDataSource(path)
                w = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                h = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rot = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (rot == 90 || rot == 270) {
                    val t = w
                    w = h
                    h = t
                }
            } finally {
                r.release()
            }
        } else {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            w = opts.outWidth
            h = opts.outHeight
            val orientation = android.media.ExifInterface(path)
                .getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)
            if (orientation == 6 || orientation == 8) {
                val t = w
                w = h
                h = t
            }
        }
    } catch (e: Exception) {
        return 1f
    }
    if (w <= 0 || h <= 0) return 1f
    return (w.toFloat() / h.toFloat()).coerceIn(0.8f, 1.91f)
}

@Composable
private fun PostVideoPlayer(
    filePath: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(true) }
    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(filePath))))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = false
            prepare()
        }
    }
        DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE -> isResumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(isActive, isResumed) { exoPlayer.playWhenReady = isActive && isResumed }
    LaunchedEffect(isMuted) { exoPlayer.volume = if (isMuted) 0f else 1f }

    Box(modifier = modifier) {
        AndroidView(
                        factory = { ctx ->
                (android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.view_post_player, null) as PlayerView).apply {
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = exoPlayer
                }
            },
            update = { it.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { isMuted = !isMuted }
        )
        Icon(
            imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
            contentDescription = if (isMuted) "الصوت مكتوم" else "الصوت شغال",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .size(20.dp)
        )
    }
}

@Composable
fun PostCard(
    post: Post,
    isFollowing: Boolean = post.isFollowing,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onFollowClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    isActiveVideo: Boolean = false,
    canMountVideo: Boolean = true
) {
    var menuExpanded by remember { mutableStateOf(false) }

        Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .shadow(
                elevation = 8.dp,
                shape = PostCardShape,
                ambientColor = PostCardGlowColor,
                spotColor = PostCardGlowColor
            )
            .border(
                width = 1.dp,
                brush = PostCardBorderBrush,
                shape = PostCardShape
            )
            .testTag("post_card_${post.id}"),
        shape = PostCardShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        .size(48.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            ambientColor = NeonCyan,
                            spotColor = NeonCyan
                        )
                        .border(width = 2.dp, color = NeonCyan, shape = CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.authorAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = post.authorName,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = post.authorName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
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
                            color = Color.White
                        )

                        // زر متابعة (Follow) فقط بجانب اسم المستخدم مباشرة (لغير صاحب المنشور)
                        if (!post.isAuthor) {
                            OutlinedButton(
                                onClick = onFollowClick,
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("follow_button_${post.id}"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(50),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isFollowing) NeonPurple.copy(alpha = 0.4f) else NeonPurple
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = if (isFollowing) TextSecondary else NeonPurple
                                )
                            ) {
                                Text(
                                    text = if (isFollowing) "اتابع" else "متابعة",
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
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "خيارات المنشور",
                            tint = TextSecondary
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
                    color = NeonPurple.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = post.tag,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = NeonPurple,
                    
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Post Media Representation
            if (post.mediaType != PostMediaType.NONE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val hasRealImage = post.mediaType == PostMediaType.IMAGE && post.mediaUri.isNotBlank()
                    val hasRealVideo = remember(post.mediaType, post.mediaUri) {
                        post.mediaType == PostMediaType.SHORT_VIDEO &&
                            post.mediaUri.isNotBlank() &&
                            File(post.mediaUri).exists()
                    }
                    val mediaRatio by produceState(
                        initialValue = mediaRatioCache[post.mediaUri] ?: 1f,
                        key1 = post.mediaUri
                    ) {
                        if (hasRealImage || hasRealVideo) {
                            val cached = mediaRatioCache[post.mediaUri]
                            value = cached ?: withContext(Dispatchers.IO) {
                                mediaRatioCache.getOrPut(post.mediaUri) {
                                    readMediaAspectRatio(post.mediaUri, hasRealVideo)
                                }
                            }
                        } else {
                            value = 1f
                        }
                                        }
                Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (hasRealImage || hasRealVideo) Modifier.aspectRatio(mediaRatio)
                                else Modifier.height(190.dp)
                            )
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
                                                        if (hasRealVideo) {
                            AsyncImage(
                                model = File(post.mediaUri),
                                imageLoader = rememberVideoFrameLoader(),
                                contentDescription = "لقطة الفيديو",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (canMountVideo) {
                                PostVideoPlayer(
                                    filePath = post.mediaUri,
                                    isActive = isActiveVideo,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else if (hasRealImage) {
                            AsyncImage(
                                model = File(post.mediaUri),
                                contentDescription = "صورة المنشور",
                                modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                            )
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
                        tint = if (post.isLiked) NeonPurple else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount}",
                        fontWeight = if (post.isLiked) FontWeight.Bold else FontWeight.Normal,
                        color = if (post.isLiked) NeonPurple else NeonCyan
                    )
                }

                // Comment Button
                val commentInteraction = remember { MutableInteractionSource() }
                val commentPressed by commentInteraction.collectIsPressedAsState()
                TextButton(
                    onClick = onCommentClick,
                    interactionSource = commentInteraction,
                    modifier = Modifier.testTag("post_comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "تعليق",
                        tint = if (commentPressed) NeonPurple else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount}",
                        color = if (commentPressed) NeonPurple else NeonCyan
                    )
                }

               // Share Button
                val shareInteraction = remember { MutableInteractionSource() }
                val sharePressed by shareInteraction.collectIsPressedAsState()
                TextButton(
                    onClick = onShareClick,
                    interactionSource = shareInteraction,
                    modifier = Modifier.testTag("post_share_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "مشاركة",
                        tint = if (sharePressed) NeonPurple else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.sharesCount}",
                        color = if (sharePressed) NeonPurple else NeonCyan
                    )
                } 
            }
        }
    }
}

@Composable
fun MediaPostItem(
    post: Post,
    isFollowing: Boolean = post.isFollowing,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onFollowClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    isActiveVideo: Boolean = false,
    canMountVideo: Boolean = true
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isVideo = post.mediaType == PostMediaType.SHORT_VIDEO
    val mediaRatio by produceState(
        initialValue = mediaRatioCache[post.mediaUri] ?: 1f,
        key1 = post.mediaUri
    ) {
                val cached = mediaRatioCache[post.mediaUri]
        value = cached ?: withContext(Dispatchers.IO) {
            mediaRatioCache.getOrPut(post.mediaUri) {
                readMediaAspectRatio(post.mediaUri, isVideo)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}")
    ) {
        // ===== الوسائط بعرض الشاشة كامل =====
                Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(mediaRatio)
                .clipToBounds()
                .background(Color.Black)
        ) {
                if (isVideo) {
                // أول لقطة من الفيديو تبقى ظاهرة دائماً خلف المشغّل،
                // فما تظهر شاشة سوداء أثناء التمرير ولا أثناء تجهيز المشغّل
                AsyncImage(
                    model = File(post.mediaUri),
                    imageLoader = rememberVideoFrameLoader(),
                    contentDescription = "لقطة الفيديو",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (canMountVideo) {
                    PostVideoPlayer(
                        filePath = post.mediaUri,
                        isActive = isActiveVideo,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                AsyncImage(
                    model = File(post.mediaUri),
                    contentDescription = "صورة المنشور",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // تدرج أسود خفيف أعلى الوسائط لوضوح الاسم
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            )

            // الأفاتار + الاسم + متابعة + النقاط الثلاث (داخل الوسائط)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(width = 2.dp, color = NeonCyan, shape = CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.authorAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = post.authorName,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = post.authorName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )

                        if (!post.isAuthor) {
                            OutlinedButton(
                                onClick = onFollowClick,
                                modifier = Modifier
                                    .height(26.dp)
                                    .testTag("follow_button_${post.id}"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(50),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isFollowing) Color.White.copy(alpha = 0.5f) else Color.White
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = if (isFollowing) "اتابع" else "متابعة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Text(
                        text = post.timeAgo,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("post_menu_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "خيارات المنشور",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (post.isAuthor) {
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
        }

        // ===== أزرار الإعجاب والتعليق والمشاركة (أسفل الوسائط) =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onLikeClick,
                modifier = Modifier.testTag("post_like_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "إعجاب",
                    tint = if (post.isLiked) NeonPurple else NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${post.likesCount}",
                    fontWeight = if (post.isLiked) FontWeight.Bold else FontWeight.Normal,
                    color = if (post.isLiked) NeonPurple else NeonCyan
                )
            }

            val commentInteraction = remember { MutableInteractionSource() }
            val commentPressed by commentInteraction.collectIsPressedAsState()
            TextButton(
                onClick = onCommentClick,
                interactionSource = commentInteraction,
                modifier = Modifier.testTag("post_comment_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "تعليق",
                    tint = if (commentPressed) NeonPurple else NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${post.commentsCount}",
                    color = if (commentPressed) NeonPurple else NeonCyan
                )
            }

            val shareInteraction = remember { MutableInteractionSource() }
            val sharePressed by shareInteraction.collectIsPressedAsState()
            TextButton(
                onClick = onShareClick,
                interactionSource = shareInteraction,
                modifier = Modifier.testTag("post_share_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "مشاركة",
                    tint = if (sharePressed) NeonPurple else NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${post.sharesCount}",
                    color = if (sharePressed) NeonPurple else NeonCyan
                )
            }
        }

        // ===== النص والوسم (أسفل الأزرار) =====
        if (post.content.isNotBlank() || post.tag != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            ) {
                if (post.content.isNotBlank()) {
                    Text(
                        text = post.content,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color.White
                    )
                }
                if (post.tag != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonPurple.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = post.tag,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = NeonPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

    fun goToNextStory() {
        if (pagerState.currentPage < stories.size - 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        } else {
            onDismiss()
        }
    }

    val currentStoryForTimer = stories.getOrNull(pagerState.currentPage)
    val isCurrentVideo = currentStoryForTimer?.mediaType == StoryMediaType.VIDEO &&
        !currentStoryForTimer.mediaUri.isNullOrBlank()

    // صورة: تقدّم تلقائي بعد 5 ثوانٍ. فيديو: التقدّم يحصل عند اكتمال التشغيل الفعلي أدناه
    LaunchedEffect(pagerState.currentPage, isCurrentVideo) {
        if (!isCurrentVideo) {
            delay(5000)
            goToNextStory()
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
                                    if (!story.authorAvatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = story.authorAvatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = story.authorName.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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
                                if (story.mediaType == StoryMediaType.VIDEO) {
                                    val videoViewRef = remember(page) { mutableStateOf<android.widget.VideoView?>(null) }
                                    AndroidView(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp)),
                                        factory = { ctx ->
                                            object : android.widget.VideoView(ctx) {
                                                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                                                    setMeasuredDimension(
                                                        android.view.View.MeasureSpec.getSize(widthMeasureSpec),
                                                        android.view.View.MeasureSpec.getSize(heightMeasureSpec)
                                                    )
                                                }
                                            }.apply {
                                                setVideoPath(story.mediaUri)
                                                setOnPreparedListener { mp ->
                                                    mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                                }
                                                setOnCompletionListener {
                                                    if (page == pagerState.currentPage) {
                                                        goToNextStory()
                                                    }
                                                }
                                                videoViewRef.value = this
                                            }
                                        },
                                        update = { view ->
                                            if (page == pagerState.currentPage) {
                                                if (!view.isPlaying) view.start()
                                            } else {
                                                if (view.isPlaying) view.pause()
                                            }
                                        }
                                    )
                                } else {
                                    AsyncImage(
                                        model = story.mediaUri,
                                        contentDescription = "محتوى القصة",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
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

   enum class FlashMode { OFF, ON, AUTO }

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
    val context = LocalContext.current
        val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    val videoCapture = remember {
        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        VideoCapture.withOutput(recorder)
    }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var currentMode by remember { mutableStateOf(StoryCreationMode.PHOTO) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var isScreenFlashing by remember { mutableStateOf(false) }
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

    // Auto-stop recording after 30 seconds & track duration
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordDuration = 0
            while (isRecording && recordDuration < 30) {
                delay(1000)
                recordDuration++
            }
            if (isRecording) {
                activeRecording?.stop()
                isRecording = false
            }
        } else {
            recordDuration = 0
        }
    }

    // Request microphone permission once when the story creator opens
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
        val dialogView = LocalView.current
        SideEffect {
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
            dialogWindow?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            dialogWindow?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
            dialogWindow?.let { w ->
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, false)
                w.addFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
                w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                w.navigationBarColor = android.graphics.Color.BLACK
            }
        }
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
                    lensFacing = lensFacing,
                    torchEnabled = currentMode == StoryCreationMode.VIDEO && isRecording && flashMode == FlashMode.ON,
                    imageCapture = imageCapture,
                    videoCapture = videoCapture
                )
                // Top Controls: Close, Flash, Flip Camera, and Active Mode Badge
                LaunchedEffect(flashMode) {
                    imageCapture.flashMode = when (flashMode) {
                        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                    }
                }

                               StoryCameraControlsOverlayV2(
    currentMode = currentMode,
    isRecording = isRecording,
    recordDuration = recordDuration,
    flashMode = flashMode,
    onToggleFlash = {
        flashMode = when (flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
    },
    onClose = onDismiss,
    onSelectMode = { currentMode = it }, 
                    onFlipCamera = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else
                            CameraSelector.LENS_FACING_BACK
                    },
                    onOpenGallery = {
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onCapturePhoto = {
                        val photoFile = File(
                            context.filesDir,
                            "story_${System.currentTimeMillis()}.jpg"
                        )
                        val metadata = ImageCapture.Metadata().apply {
                            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                        }
                        val outputOptions = OutputFileOptions.Builder(photoFile)
                            .setMetadata(metadata)
                            .build()
                        if (lensFacing == CameraSelector.LENS_FACING_FRONT && flashMode != FlashMode.OFF) {
                            isScreenFlashing = true
                        }
                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    isScreenFlashing = false
                                    capturedMediaUri = photoFile.absolutePath
                                    isVideoStory = false
                                    isReviewing = true
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    isScreenFlashing = false
                                }
                            }
                        )
                    },
                    onToggleRecordVideo = {
                        if (isRecording) {
                            activeRecording?.stop()
                            isRecording = false
                        } else {
                            val videoFile = File(
                                context.filesDir,
                                "story_${System.currentTimeMillis()}.mp4"
                            )
                            val outputOptions = FileOutputOptions.Builder(videoFile).build()
                            val hasAudioPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            val pendingRecording =
                                videoCapture.output.prepareRecording(context, outputOptions)
                            val recordingToStart = if (hasAudioPermission) {
                                pendingRecording.withAudioEnabled()
                            } else {
                                pendingRecording
                            }
                            activeRecording = recordingToStart.start(
                                ContextCompat.getMainExecutor(context)
                            ) { event ->
                                if (event is VideoRecordEvent.Finalize) {
                                    if (!event.hasError()) {
                                        capturedMediaUri = videoFile.absolutePath
                                        isVideoStory = true
                                        isReviewing = true
                                    }
                                    activeRecording = null
                                }
                            }
                            isRecording = true
                        }
                    }
                                )

                // Front Camera Screen Flash (White Full-Screen Burst)
                if (isScreenFlashing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
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
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
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
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MujtamaGold else Color.White
        )
    }
}

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    torchEnabled: Boolean = false,
    imageCapture: ImageCapture? = null,
    videoCapture: VideoCapture<Recorder>? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isBound by remember(lensFacing) { mutableStateOf(false) }
    var bindError by remember(lensFacing) { mutableStateOf(false) }
    var camera by remember(lensFacing) { mutableStateOf<Camera?>(null) }
    
    var tapOffset by remember { mutableStateOf<Offset?>(null) }
    val previewView = remember(lensFacing) { PreviewView(context) }

    LaunchedEffect(torchEnabled, camera) {
        val cam = camera ?: return@LaunchedEffect
        if (cam.cameraInfo.hasFlashUnit()) {
            cam.cameraControl.enableTorch(torchEnabled)
        }
    }
    
    LaunchedEffect(tapOffset) {
        if (tapOffset != null) {
            delay(2000)
            tapOffset = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black)
            .pointerInput(camera, previewView) {
                detectTapGestures(
                    onTap = { offset ->
                        val cam = camera ?: return@detectTapGestures
                        tapOffset = offset
                        
                        val factory = previewView.meteringPointFactory
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build()
                        
                        cam.cameraControl.startFocusAndMetering(action)
                    }
                )
            }
    ) {
        if (!bindError) {
            key(lensFacing) {
                AndroidView(
                    factory = { ctx ->
                        previewView.apply {
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
                                    val preview = Preview.Builder()
                                        .setResolutionSelector(
                                            ResolutionSelector.Builder()
                                                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                                                .build()
                                        )
                                        .build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                    cameraProvider.unbindAll()

                                    val viewPort = previewView.viewPort
                                    val useCaseGroupBuilder = UseCaseGroup.Builder()
                                        .addUseCase(preview)
                                    
                                    imageCapture?.let { useCaseGroupBuilder.addUseCase(it) }
                                    videoCapture?.let { useCaseGroupBuilder.addUseCase(it) }
                                    
                                    viewPort?.let { useCaseGroupBuilder.setViewPort(it) }

                                    camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        useCaseGroupBuilder.build()
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
        }

        if (bindError || !isBound) {
            CameraSimulationView(modifier = Modifier.fillMaxSize())
        }

        CameraViewfinderOverlay(modifier = Modifier.fillMaxSize())
        
        tapOffset?.let { offset ->
            val animatedSize by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300),
                label = "focusScale"
            )
            val animatedAlpha by animateFloatAsState(
                targetValue = if (animatedSize > 0.9f) 0.8f else 1f,
                animationSpec = tween(durationMillis = 500),
                label = "focusAlpha"
            )
            
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = animatedAlpha),
                        radius = (100f * animatedSize),
                        center = offset,
                        style = Stroke(width = 4f)
                    )
                }
            }
        }
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
    flashMode: FlashMode,
    onToggleFlash: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Close Button (Left)
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterStart).testTag("close_story_creation_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "إغلاق",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Flash Button (Center)
        IconButton(
            onClick = onToggleFlash,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = when (flashMode) {
                    FlashMode.OFF -> Icons.Outlined.FlashOff
                    FlashMode.ON -> Icons.Filled.FlashOn
                    FlashMode.AUTO -> Icons.Filled.FlashAuto
                },
                contentDescription = "الفلاش",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Settings Button (Right - Visual only)
        IconButton(
            onClick = { },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "إعدادات",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // Optional: Timer Badge if recording
        if (isRecording) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Red.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(top = 40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                    Text(
                        text = "00:${if (recordDuration < 10) "0$recordDuration" else recordDuration}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StoryBottomShutterBar(
    currentMode: StoryCreationMode,
    isRecording: Boolean,
    onCapturePhoto: () -> Unit,
    onToggleRecordVideo: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenGallery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "0.5",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .background(Color.White, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "1x",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "2",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 12.dp, top = 6.dp, bottom = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenGallery) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = "المعرض",
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .border(
                            5.dp,
                            if (isRecording && currentMode == StoryCreationMode.VIDEO) Color.Red else Color.White,
                            CircleShape
                        )
                        .padding(4.dp)
                        .clip(
                            if (isRecording && currentMode == StoryCreationMode.VIDEO) {
                                RoundedCornerShape(12.dp)
                            } else {
                                CircleShape
                            }
                        )
                        .background(
                            if (isRecording && currentMode == StoryCreationMode.VIDEO) Color.Red else Color.White
                        )
                        .clickable {
                            if (currentMode == StoryCreationMode.VIDEO) {
                                onToggleRecordVideo()
                            } else {
                                onCapturePhoto()
                            }
                        }
                )

                IconButton(
                    onClick = onFlipCamera,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FlipCameraAndroid,
                        contentDescription = "تبديل الكاميرا",
                        tint = Color.White
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("بث مباشر", color = Color.Gray, fontSize = 14.sp)
                Text("صورة", color = Color.Gray, fontSize = 14.sp)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White
                ) {
                    Text(
                        text = "قصة",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Text("فيديو", color = Color.Gray, fontSize = 14.sp)
                Text("نص", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}
@Composable
fun StoryCameraControlsOverlay(
    currentMode: StoryCreationMode,
    isRecording: Boolean,
    recordDuration: Int,
    flashMode: FlashMode,
    onToggleFlash: () -> Unit,
    onClose: () -> Unit,
    onCapturePhoto: () -> Unit,
    onToggleRecordVideo: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onSelectMode: (StoryCreationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soon: () -> Unit = {
        android.widget.Toast.makeText(context, "قريباً", android.widget.Toast.LENGTH_SHORT).show()
    }
    val isVideoRec = isRecording && currentMode == StoryCreationMode.VIDEO
    val tabs: List<Pair<String, StoryCreationMode?>> = listOf(
        "بث مباشر" to null,
        "قصة" to StoryCreationMode.PHOTO,
        "فيديو" to StoryCreationMode.VIDEO,
        "نص" to StoryCreationMode.TEXT
    )
    val shutterBrush: Brush = if (isVideoRec) {
        Brush.linearGradient(listOf(Color.Red, Color.Red))
    } else {
        Brush.radialGradient(listOf(Color.White, Color(0xFFDDE6FF)))
    }

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        Box(modifier = modifier.fillMaxSize()) {

            // ── الشريط العلوي: إغلاق يسار، فلاش وإعدادات يمين
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleFlash, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = when (flashMode) {
                                FlashMode.OFF -> Icons.Outlined.FlashOff
                                FlashMode.ON -> Icons.Filled.FlashOn
                                FlashMode.AUTO -> Icons.Filled.FlashAuto
                            },
                            contentDescription = "الفلاش",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "الإعدادات",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // ── مؤقت التسجيل
            if (isRecording) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Red.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                        Text(
                            text = "00:${if (recordDuration < 10) "0$recordDuration" else recordDuration}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── عمود الأدوات الأيسر
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = (-110).dp)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                listOf(
                    Icons.Outlined.MusicNote to "الموسيقى",
                    Icons.Outlined.Timer to "المدة",
                    Icons.Outlined.GridView to "الشبكة",
                    Icons.Outlined.AutoAwesome to "التجميل"
                ).forEach { (icon, label) ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { soon() }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = label, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // ── الجزء السفلي: زوم، صف الغالق، شريط الأوضاع
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // كبسولة الزوم (شكلية كما هي حالياً)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Text("0.5", color = Color.White, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1x", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Text("2", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // صف الغالق: معرض (يسار) - غالق - تبديل الكاميرا (يمين)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.width(72.dp).clickable(onClick = onOpenGallery),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.DarkGray.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = "المعرض",
                                tint = MujtamaGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("المعرض", color = Color.White, fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .border(3.dp, if (isVideoRec) Color.Red else Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(if (isVideoRec) RoundedCornerShape(12.dp) else CircleShape)
                            .background(brush = shutterBrush)
                            .clickable {
                                if (currentMode == StoryCreationMode.VIDEO) {
                                    onToggleRecordVideo()
                                } else {
                                    onCapturePhoto()
                                }
                            }
                    )

                    Column(
                        modifier = Modifier.width(72.dp).clickable(onClick = onFlipCamera),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FlipCameraAndroid,
                                contentDescription = "تبديل الكاميرا",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("تبديل الكاميرا", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // شريط الأوضاع: بث مباشر - قصة - فيديو - نص
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { (label, mode) ->
                            val selected = mode != null && mode == currentMode
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(if (selected) Color.White else Color.Transparent)
                                        .clickable {
                                            if (!isRecording) {
                                                if (mode == null) soon() else onSelectMode(mode)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) Color.Black else Color.White.copy(alpha = 0.6f),
                                        fontSize = 15.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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

@Composable
fun StoryCameraControlsOverlayV2(
    currentMode: StoryCreationMode,
    isRecording: Boolean,
    recordDuration: Int,
    flashMode: FlashMode,
    onToggleFlash: () -> Unit,
    onClose: () -> Unit,
    onCapturePhoto: () -> Unit,
    onToggleRecordVideo: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onSelectMode: (StoryCreationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soon: () -> Unit = {
        android.widget.Toast.makeText(context, "قريباً", android.widget.Toast.LENGTH_SHORT).show()
    }
    val isVideoRec = isRecording && currentMode == StoryCreationMode.VIDEO
    val tabs: List<Pair<String, StoryCreationMode?>> = listOf(
        "بث مباشر" to null,
        "قصة" to StoryCreationMode.PHOTO,
        "فيديو" to StoryCreationMode.VIDEO,
        "نص" to StoryCreationMode.TEXT
    )
    val shutterBrush: Brush = if (isVideoRec) {
        Brush.linearGradient(listOf(Color.Red, Color.Red))
    } else {
        Brush.radialGradient(listOf(Color.White, Color(0xFFDDE6FF)))
    }
    val stripHeight = 64.dp
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        Box(modifier = modifier.fillMaxSize()) {

            // الشريط العلوي: إغلاق يسار، فلاش وإعدادات يمين
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleFlash, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = when (flashMode) {
                                FlashMode.OFF -> Icons.Outlined.FlashOff
                                FlashMode.ON -> Icons.Filled.FlashOn
                                FlashMode.AUTO -> Icons.Filled.FlashAuto
                            },
                            contentDescription = "الفلاش",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "الإعدادات",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // مؤقت التسجيل
            if (isRecording) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Red.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                        Text(
                            text = "00:${if (recordDuration < 10) "0$recordDuration" else recordDuration}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // عمود الأدوات الأيسر
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = (-110).dp)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                listOf(
                    Icons.Outlined.MusicNote to "الموسيقى",
                    Icons.Outlined.Timer to "المدة",
                    Icons.Outlined.GridView to "الشبكة",
                    Icons.Outlined.AutoAwesome to "التجميل"
                ).forEach { (icon, label) ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { soon() }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = label, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // الزوم + صف الغالق (فوق الشريط الأسود)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = stripHeight + navBottom + 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Text("0.5", color = Color.White, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1x", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Text("2", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.width(72.dp).clickable(onClick = onOpenGallery),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.DarkGray.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = "المعرض",
                                tint = MujtamaGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("المعرض", color = Color.White, fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .border(3.dp, if (isVideoRec) Color.Red else Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(if (isVideoRec) RoundedCornerShape(12.dp) else CircleShape)
                            .background(brush = shutterBrush)
                            .clickable {
                                if (currentMode == StoryCreationMode.VIDEO) {
                                    onToggleRecordVideo()
                                } else {
                                    onCapturePhoto()
                                }
                            }
                    )

                    Column(
                        modifier = Modifier.width(72.dp).clickable(onClick = onFlipCamera),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FlipCameraAndroid,
                                contentDescription = "تبديل الكاميرا",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("تبديل الكاميرا", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // الشريط الأسود السفلي: فيه الأوضاع فقط، ويغطي منطقة أزرار النظام
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Magenta)
                    .navigationBarsPadding()
                    .height(stripHeight),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { (label, mode) ->
                        val selected = mode != null && mode == currentMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable {
                                    if (!isRecording) {
                                        if (mode == null) soon() else onSelectMode(mode)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.Black else Color.White.copy(alpha = 0.7f),
                                fontSize = 15.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
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
    var showMoreMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090E)) // خلفية سوداء قريبة من الانستقرام
    ) {
        // ── 1. CENTER MEDIA CARD (المستطيل الوسطي للصورة) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .align(Alignment.TopCenter)
                .padding(top = 100.dp, bottom = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            if (isVideo) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        object : android.widget.VideoView(ctx) {
                            override fun onMeasure(w: Int, h: Int) {
                                setMeasuredDimension(
                                    android.view.View.MeasureSpec.getSize(w),
                                    android.view.View.MeasureSpec.getSize(h)
                                )
                            }
                        }.apply {
                            setVideoPath(mediaUri)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                mp.setVideoScalingMode(
                                    android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                                )
                                start()
                            }
                        }
                    }
                )
            } else {
                AsyncImage(
                    model = mediaUri,
                    contentDescription = "معاينة القصة",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // علامة "قصتك +" داخل الصورة في الأسفل يساراً
            Surface(
                modifier = Modifier
                    .align(AbsoluteAlignment.BottomLeft)
                    .padding(start = 14.dp, bottom = 14.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text("قصتك +", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── 2. TOP ICONS (الأزرار العلوية خارج الصورة) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Back Button (رجوع)
            IconButton(
                onClick = onRetake,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            // Right Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aa", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { }, modifier = Modifier.size(42.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) {
                    Icon(Icons.Outlined.EmojiEmotions, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { }, modifier = Modifier.size(42.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Box {
                    IconButton(
                        onClick = { showMoreMenu = !showMoreMenu },
                        modifier = Modifier.size(42.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp)).width(210.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("حفظ", color = Color.White) }, 
                            onClick = { showMoreMenu = false }, 
                            leadingIcon = { Icon(Icons.Outlined.SaveAlt, null, tint = Color.White) }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف", color = Color.Red) }, 
                            onClick = { showMoreMenu = false; onRetake() }, 
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Color.Red) }
                        )
                    }
                }
            }
        }

        // ── 3. BOTTOM BAR (الشريط السفلي) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { },
                modifier = Modifier.height(52.dp).weight(0.85f),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.3f), contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.SaveAlt, contentDescription = "حفظ", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("حفظ", fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = { },
                modifier = Modifier.height(52.dp).weight(1.3f),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.3f), contentColor = Color.White)
            ) {
                Box(modifier = Modifier.size(22.dp).background(Color(0xFF34C759), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("الأصدقاء المقربون", fontSize = 13.sp)
            }

            Button(
                onClick = onPublish,
                modifier = Modifier.height(52.dp).weight(1.5f),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF), contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.Send, contentDescription = "إرسال", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال إلى", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// أيقونة أداة صغيرة (يمين الشاشة) — private لهذه الشاشة فقط
@Composable
private fun StoryReviewToolButton(
    icon: ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)
                ) {
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
                }

                Button(
                    onClick = { onPublish(content, tag) },
                    enabled = content.isNotBlank(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .imePadding()
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)
                ) {
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
                            .padding(horizontal = 16.dp),
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
                    }
                }

                Button(
                    onClick = {
                        onPublish(caption, tag, if (isVideo) PostMediaType.SHORT_VIDEO else PostMediaType.IMAGE)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .imePadding()
                ) {
                    Text("نشر")
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
    var content by remember { mutableStateOf(post.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل المنشور") },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(content) },
                enabled = content.isNotBlank()
            ) {
                Text("حفظ")
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
