package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.*
import com.example.ui.components.MujtamaBottomNav
import com.example.ui.components.MujtamaTopBar
import com.example.ui.screens.*
import com.example.ui.screens.auth.AuthScreen
import com.example.viewmodel.SocialAppViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: SocialAppViewModel) {
    // Force full RTL layout for Arabic user experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

        if (!isLoggedIn) {
            AuthScreen(
                onAuthSuccess = { account, generatedId ->
                    viewModel.onAuthSuccess(account, generatedId)
                }
            )
        } else {
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
            val walletBalance by viewModel.walletBalance.collectAsStateWithLifecycle()
            val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
            val activeRoomId by viewModel.activeRoomId.collectAsStateWithLifecycle()
            val activeGameType by viewModel.activeGameType.collectAsStateWithLifecycle()
            val showAccountSettings by viewModel.showAccountSettings.collectAsStateWithLifecycle()

        // Hide top bar and bottom navigation when user is inside any chat room, active game, or the account settings page
        val isInsideRoom = currentTab == AppTab.CHAT && activeRoomId != null
        val isInsideGame = currentTab == AppTab.GAMES && activeGameType != null
        val hideBars = isInsideRoom || isInsideGame || showAccountSettings

        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        val showNotifications by viewModel.showNotifications.collectAsStateWithLifecycle()
        val notificationsList by viewModel.notifications.collectAsStateWithLifecycle()
        val notificationsDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
        if (showNotifications) {
            com.example.ui.screens.NotificationsScreen(
                notifications = notificationsList,
                isDarkMode = notificationsDark,
                onClose = { viewModel.closeNotifications() }
            )
        }

        LaunchedEffect(userMessage) {
            userMessage?.let { msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = msg,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.clearUserMessage()
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AnimatedVisibility(
                    visible = currentTab == AppTab.FEED,
                    enter = fadeIn(animationSpec = tween(durationMillis = 180)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 120))
                ) {
                    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
                    MujtamaTopBar(
                        currentTab = currentTab,
                        walletBalance = walletBalance,
                        isDarkMode = isDarkMode,
                        onWalletClick = { viewModel.setTab(AppTab.PROFILE) },
                        onNotificationsClick = { viewModel.openNotifications() },
                        unreadCount = viewModel.notifications.collectAsStateWithLifecycle().value.count { !it.isRead },
                        onSearchClick = { viewModel.setTab(AppTab.CHAT) },
                        onToggleDarkMode = { viewModel.toggleDarkMode() }
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !hideBars,
                    enter = fadeIn(animationSpec = tween(durationMillis = 180)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 120))
                ) {
                    MujtamaBottomNav(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.setTab(it) }
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showAccountSettings) {
                    val posts by viewModel.posts.collectAsStateWithLifecycle()
                    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                    val walletFilter by viewModel.walletFilter.collectAsStateWithLifecycle()
                    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                    val storeItems by viewModel.storeItems.collectAsStateWithLifecycle()

                    AccountSettingsScreen(
                        userProfile = userProfile,
                        posts = posts,
                        balance = walletBalance,
                        walletFilter = walletFilter,
                        transactions = transactions,
                        storeItems = storeItems,
                        onToggleNotifications = { viewModel.toggleProfileNotifications() },
                        onLogout = { viewModel.logoutUser() },
                        onFilterChange = { viewModel.setWalletFilter(it) },
                        onBuyItem = { viewModel.buyStoreItem(it) },
                        onLikePost = { viewModel.toggleLike(it) },
                        onCommentPost = { viewModel.openComments(it) },
                        onSharePost = { viewModel.sharePost(it) },
                        onNavigateToRecharge = {},
                        onUpdateProfile = { name, bio, emoji -> viewModel.updateUserProfile(name, bio, emoji) },
                        onUpdateBio = { viewModel.updateUserBio(it) },
                        onBack = { viewModel.closeAccountSettings() }
                    )
                } else {
                Crossfade(targetState = currentTab, label = "tab_transition") { tab ->
                    when (tab) {
                        AppTab.FEED -> {
                            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                            val stories by viewModel.stories.collectAsStateWithLifecycle()
                            val posts by viewModel.posts.collectAsStateWithLifecycle()
                            val activeStory by viewModel.activeStory.collectAsStateWithLifecycle()
                            val activeCommentPostId by viewModel.activeCommentPostId.collectAsStateWithLifecycle()

                            FeedScreen(
                                userProfile = userProfile,
                                stories = stories,
                                posts = posts,
                                activeStory = activeStory,
                                activeCommentPostId = activeCommentPostId,
                                onStoryClick = { viewModel.openStory(it) },
                                onCloseStory = { viewModel.closeStory() },
                                onAddStory = { text, mediaUri, mediaType, colors ->
                                    viewModel.addStory(text, mediaUri, mediaType, colors)
                                },
                                onLikeClick = { viewModel.toggleLike(it) },
                                onCommentClick = { viewModel.openComments(it) },
                                onCloseComments = { viewModel.closeComments() },
                                onAddComment = { postId, text -> viewModel.addComment(postId, text) },
                                onShareClick = { viewModel.sharePost(it) },
                                onPublishPost = { text, tag, type -> viewModel.publishPost(text, tag, type) },
                                onFollowClick = { viewModel.toggleFollowUser(it) },
                                onEditPost = { postId, newText -> viewModel.editPost(postId, newText) },
                                onDeletePost = { viewModel.deletePost(it) },
                                onReportPost = { viewModel.reportPost(it) }
                            )
                        }

                        AppTab.CHAT -> {
                            val conversations by viewModel.conversations.collectAsStateWithLifecycle()
                            val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
                            val chatFilter by viewModel.chatFilter.collectAsStateWithLifecycle()
                            val pushNotificationsEnabled by viewModel.pushNotificationsEnabled.collectAsStateWithLifecycle()
                            val chatSubTab by viewModel.chatSubTab.collectAsStateWithLifecycle()
                            val chatRooms by viewModel.chatRooms.collectAsStateWithLifecycle()
                            val activeRoomId by viewModel.activeRoomId.collectAsStateWithLifecycle()
                            val roomCategoryFilter by viewModel.roomCategoryFilter.collectAsStateWithLifecycle()
                            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

                            ChatScreen(
                                myAvatarUrl = userProfile.avatarUrl,
                                conversations = conversations,
                                activeChatId = activeChatId,
                                chatFilter = chatFilter,
                                pushNotificationsEnabled = pushNotificationsEnabled,
                                chatSubTab = chatSubTab,
                                chatRooms = chatRooms,
                                activeRoomId = activeRoomId,
                                roomCategoryFilter = roomCategoryFilter,
                                onSubTabChange = { viewModel.setChatSubTab(it) },
                                onFilterChange = { viewModel.setChatFilter(it) },
                                onTogglePushNotifications = { viewModel.togglePushNotifications() },
                                onOpenChat = { viewModel.openConversation(it) },
                                onCloseChat = { viewModel.closeConversation() },
                                onSendMessage = { convId, text, type -> viewModel.sendMessage(convId, text, type) },
                                onStartGameInChat = { convId, gameType ->
                                    viewModel.startInChatGame(convId, gameType)
                                    viewModel.launchGame(gameType, GameMatchMode.WITH_FRIEND, "صديقك")
                                    viewModel.setTab(AppTab.GAMES)
                                },
                                onNavigateToGames = { gameType ->
                                    if (gameType != null) {
                                        viewModel.launchGame(gameType, GameMatchMode.WITH_FRIEND, "صديقك")
                                    }
                                    viewModel.setTab(AppTab.GAMES)
                                },
                                onRoomCategoryChange = { viewModel.setRoomCategoryFilter(it) },
                                onOpenRoom = { viewModel.openRoom(it) },
                                onCloseRoom = { viewModel.closeRoom() },
                                onCreateRoom = { name, desc, cat, access, pass, max, emoji, imgUrl ->
                                    viewModel.createChatRoom(name, desc, cat, access, pass, max, emoji, imgUrl)
                                },
                                onJoinRoom = { roomId, pass -> viewModel.joinChatRoom(roomId, pass) },
                                onLeaveRoom = { viewModel.leaveChatRoom(it) },
                                onToggleLock = { viewModel.toggleRoomLock(it) },
                                onUpdateBackground = { roomId, uri -> viewModel.updateRoomBackground(roomId, uri) },
                                onBlockMember = { roomId, memId -> viewModel.blockRoomMember(roomId, memId) },
                                onUnblockMember = { roomId, memId -> viewModel.unblockRoomMember(roomId, memId) },
                                onSendRoomMessage = { roomId, text, type -> viewModel.sendRoomMessage(roomId, text, type) },
                                onPinRoomMessage = { roomId, text -> viewModel.pinRoomMessage(roomId, text) },
                                onUnpinRoomMessage = { viewModel.unpinRoomMessage(it) },
                                onDeleteRoomMessage = { roomId, msgId -> viewModel.deleteRoomMessage(roomId, msgId) },
                                onKickRoomMember = { roomId, memId -> viewModel.kickRoomMember(roomId, memId) },
                                onMuteRoomMember = { roomId, memId -> viewModel.muteRoomMember(roomId, memId) },
                                onChangeMemberRole = { roomId, memId, role -> viewModel.changeRoomMemberRole(roomId, memId, role) },
                                onUpdateRoomSettings = { roomId, name, desc, max -> viewModel.updateRoomSettings(roomId, name, desc, max) },
                                onStartInRoomGame = { roomId, gameType ->
                                    viewModel.startInRoomGame(roomId, gameType)
                                    viewModel.launchGame(gameType, GameMatchMode.WITH_FRIEND, "أعضاء الغرفة")
                                    viewModel.setTab(AppTab.GAMES)
                                }
                            )
                        }

                        AppTab.GAMES -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = "🎮", fontSize = 48.sp)
                                    Text(
                                        text = "سوف يتم إضافة محتوى لاحقاً",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }

                        AppTab.TEAM -> {
                            val myTeam by viewModel.myTeam.collectAsStateWithLifecycle()
                            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                            val referrals by viewModel.referrals.collectAsStateWithLifecycle()
                            val friendRequests by viewModel.friendRequests.collectAsStateWithLifecycle()

                            TeamScreen(
                                userProfile = userProfile,
                                team = myTeam,
                                referrals = referrals,
                                friendRequests = friendRequests,
                                onBackClick = { viewModel.setTab(AppTab.FEED) },
                                onSendFriendRequest = { viewModel.sendFriendRequest(it) },
                                onAcceptFriendRequest = { viewModel.acceptFriendRequest(it) },
                                onRejectFriendRequest = { viewModel.rejectFriendRequest(it) },
                                onSimulateReferralJoined = { viewModel.simulateNewReferralJoined(it) }
                            )
                        }

                        AppTab.PROFILE -> {
                            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                            val posts by viewModel.posts.collectAsStateWithLifecycle()
                            ProfileScreen(
                                userProfile = userProfile,
                                posts = posts,
                                balance = walletBalance,
                                onUpdateBio = { viewModel.updateUserBio(it) },
                                onToggleNotifications = { viewModel.toggleProfileNotifications() },
                                onTogglePrivacy = { viewModel.toggleProfilePrivacy() },
                                onLogout = { viewModel.logoutUser() },
                                onClaimDailyBonus = { viewModel.claimDailyBonus() },
                                onFilterChange = { viewModel.setWalletFilter(it) },
                                onBuyItem = { viewModel.buyStoreItem(it) },
                                onLikePost = { viewModel.toggleLike(it) },
                                onCommentPost = { viewModel.openComments(it) },
                                onSharePost = { viewModel.sharePost(it) },
                                onOpenAccountSettings = { viewModel.openAccountSettings() },
                                onUpdateAvatarImage = { viewModel.updateUserAvatarUrl(it) }
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
