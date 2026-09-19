package com.example.ui.screens

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
import androidx.compose.ui.unit.dp
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
import com.example.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    posts: List<Post>,
    balance: Int,
    dailyBonusClaimed: Boolean,
    walletFilter: String,
    transactions: List<WalletTransaction>,
    storeItems: List<StoreItem>,
    onUpdateBio: (String) -> Unit,
    onUpdateProfile: (String, String, String) -> Unit,
    onToggleNotifications: () -> Unit,
    onTogglePrivacy: () -> Unit,
    onLogout: () -> Unit,
    onClaimDailyBonus: () -> Unit,
    onFilterChange: (String) -> Unit,
    onBuyItem: (StoreItem) -> Unit,
    onLikePost: (String) -> Unit,
    onCommentPost: (String) -> Unit,
    onSharePost: (Post) -> Unit,
    onNavigateToRecharge: () -> Unit = {},
    onOpenAccountSettings: () -> Unit = {},
    onUpdateAvatarImage: (Uri) -> Unit = {}
) {
    var showEditBioDialog by remember { mutableStateOf(false) }
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }

    val avatarImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onUpdateAvatarImage(it) }
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_header_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Menu icon (top-left corner in RTL) → opens the separate account-settings page
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onOpenAccountSettings() },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("profile_settings_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "القائمة",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Avatar with Edit Badge
                    Box(
                        modifier = Modifier.size(86.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
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
                            Text(text = userProfile.avatarEmoji, fontSize = 42.sp)
                        }

                        // Edit avatar icon button
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

                    // User Name & Handle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = userProfile.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "ID: ${userProfile.id} • ${userProfile.joinDate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bio Box with Edit Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showEditBioDialog = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = userProfile.bio,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                lineHeight = 17.sp
                            )
                            IconButton(
                                onClick = { showEditBioDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "تعديل النبذة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Stats Grid Row (Posts, Followers, Following)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStatItem(title = "المنشورات", count = "$totalPostsCount")
                        VerticalDivider(modifier = Modifier.height(28.dp))
                        ProfileStatItem(
                            title = "المتابعون",
                            count = "${userProfile.followersCount}",
                            onClick = { showFollowersDialog = true }
                        )
                        VerticalDivider(modifier = Modifier.height(28.dp))
                        ProfileStatItem(
                            title = "يتابع",
                            count = "${userProfile.followingCount}",
                            onClick = { showFollowingDialog = true }
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // DIALOGS
    // -------------------------------------------------------------

    // Edit Bio Dialog
    if (showEditBioDialog) {
        var bioText by remember { mutableStateOf(userProfile.bio) }
        AlertDialog(
            onDismissRequest = { showEditBioDialog = false },
            title = { Text("تعديل النبذة التعريفية ✏️") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اكتب نبذة مميزة تعبر عن اهتماماتك:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = bioText,
                        onValueChange = { bioText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateBio(bioText)
                        showEditBioDialog = false
                    },
                    enabled = bioText.isNotBlank()
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBioDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Followers List Dialog
    if (showFollowersDialog) {
        AlertDialog(
            onDismissRequest = { showFollowersDialog = false },
            title = { Text("المتابعون") },
            text = { Text("قائمة المتابعين ستظهر هنا قريباً.") },
            confirmButton = {
                TextButton(onClick = { showFollowersDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Following List Dialog
    if (showFollowingDialog) {
        AlertDialog(
            onDismissRequest = { showFollowingDialog = false },
            title = { Text("يتابع") },
            text = { Text("قائمة الحسابات التي تتابعها ستظهر هنا قريباً.") },
            confirmButton = {
                TextButton(onClick = { showFollowingDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
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
