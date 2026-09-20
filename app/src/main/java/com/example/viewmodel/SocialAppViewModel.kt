package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SocialAppViewModel : ViewModel() {

    // --- Authentication State (الشاشة التي تظهر عند فتح التطبيق لأول مرة) ---
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _currentTab = MutableStateFlow(AppTab.FEED)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // --- Stories State ---
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _activeStory = MutableStateFlow<Story?>(null)
    val activeStory: StateFlow<Story?> = _activeStory.asStateFlow()

    // --- Feed Posts State ---
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _activeCommentPostId = MutableStateFlow<String?>(null)
    val activeCommentPostId: StateFlow<String?> = _activeCommentPostId.asStateFlow()

    // --- Chat State ---
    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _chatFilter = MutableStateFlow("الكل") // الكل، متصل الآن
    val chatFilter: StateFlow<String> = _chatFilter.asStateFlow()

    private val _pushNotificationsEnabled = MutableStateFlow(true)
    val pushNotificationsEnabled: StateFlow<Boolean> = _pushNotificationsEnabled.asStateFlow()

    // --- In-app Notifications Center ---
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    fun addNotification(type: NotificationType, text: String) {
        val item = AppNotification(
            id = java.util.UUID.randomUUID().toString(),
            type = type,
            text = text
        )
        _notifications.value = listOf(item) + _notifications.value
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    private val _showNotifications = MutableStateFlow(false)
    val showNotifications: StateFlow<Boolean> = _showNotifications.asStateFlow()

    fun openNotifications() {
        _showNotifications.value = true
    }

    fun closeNotifications() {
        _showNotifications.value = false
        markAllNotificationsRead()
    }

    // --- Chat Rooms State ---
    private val _chatSubTab = MutableStateFlow("المحادثات الخاصة") // "المحادثات الخاصة" أو "غرف الدردشة"
    val chatSubTab: StateFlow<String> = _chatSubTab.asStateFlow()

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _roomCategoryFilter = MutableStateFlow("الكل")
    val roomCategoryFilter: StateFlow<String> = _roomCategoryFilter.asStateFlow()

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    init {
        startLiveRoomUpdates()
    }

    private fun startLiveRoomUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(12000)
                _chatRooms.update { currentRooms ->
                    if (currentRooms.isEmpty()) currentRooms
                    else {
                        currentRooms.map { room ->
                            val delta = listOf(-1, 0, 1, 1).random()
                            val newCount = (room.memberCount + delta).coerceIn(1, room.maxMembers)
                            if (newCount != room.memberCount) {
                                room.copy(memberCount = newCount)
                            } else {
                                room
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Account Settings Page State ---
    private val _showAccountSettings = MutableStateFlow(false)
    val showAccountSettings: StateFlow<Boolean> = _showAccountSettings.asStateFlow()

    fun openAccountSettings() {
        _showAccountSettings.value = true
    }

    fun closeAccountSettings() {
        _showAccountSettings.value = false
    }

    // --- Games State ---
    private val _activeGameType = MutableStateFlow<GameType?>(null)
    val activeGameType: StateFlow<GameType?> = _activeGameType.asStateFlow()

    private val _activeGameMode = MutableStateFlow(GameMatchMode.RANDOM_OPPONENT)
    val activeGameMode: StateFlow<GameMatchMode> = _activeGameMode.asStateFlow()

    private val _activeGameOpponent = MutableStateFlow("")
    val activeGameOpponent: StateFlow<String> = _activeGameOpponent.asStateFlow()

    // --- Team State ---
    private val _myTeam = MutableStateFlow(
        Team(
            id = "",
            name = "",
            motto = "",
            level = 0,
            totalScore = 0,
            memberCount = 0,
            maxMembers = 0,
            rank = 0,
            members = emptyList(),
            challenges = emptyList()
        )
    )
    val myTeam: StateFlow<Team> = _myTeam.asStateFlow()

    // --- Referrals & Friends State ---
    private val _referrals = MutableStateFlow<List<ReferredUser>>(emptyList())
    val referrals: StateFlow<List<ReferredUser>> = _referrals.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    // --- Wallet State ---
    private val _walletBalance = MutableStateFlow(0)
    val walletBalance: StateFlow<Int> = _walletBalance.asStateFlow()

    private val _dailyBonusClaimed = MutableStateFlow(false)
    val dailyBonusClaimed: StateFlow<Boolean> = _dailyBonusClaimed.asStateFlow()

    private val _walletFilter = MutableStateFlow("الكل") // الكل، كسب، إنفاق
    val walletFilter: StateFlow<String> = _walletFilter.asStateFlow()

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    private val _storeItems = MutableStateFlow(
        listOf(
            StoreItem("item_1", "إطار البروفايل الذهبي", "إطار ملكي مميز يظهر حول صورتك في التعليقات والدردشة", 500, "إطارات", isOwned = false),
            StoreItem("item_2", "وسام عبقري الألغاز", "شارة تظهر بجانب اسمك تثبت تفوقك في تحديات الذكاء", 750, "أوسمة", isOwned = false),
            StoreItem("item_3", "ثيم الدردشة الليلي الفاخر", "ألوان وخلفيات مخصصة لمحادثاتك الخاصة ومحادثات الفريق", 400, "ثيمات", isOwned = false),
            StoreItem("item_4", "درع دعم الفريق المشترك", "يرفع مضاعف نقاط الفريق بنسبة 10% لمدة 24 ساعة", 900, "تعزيزات", isOwned = false)
        )
    )
    val storeItems: StateFlow<List<StoreItem>> = _storeItems.asStateFlow()

    // Profile State
    private val _userProfile = MutableStateFlow(UserProfile())
    private val lastVoiceSeatRequestTime = mutableMapOf<String, Long>()
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Notification toast / snackbar message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // --- Tab Actions ---
    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }
    
    // --- Feed Actions ---
    fun toggleLike(postId: String) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val newLiked = !post.isLiked
                    val newCount = if (newLiked) post.likesCount + 1 else post.likesCount - 1
                    post.copy(isLiked = newLiked, likesCount = newCount)
                } else post
            }
        }
    }

    fun openComments(postId: String) {
        _activeCommentPostId.value = postId
    }

    fun closeComments() {
        _activeCommentPostId.value = null
    }

    fun addComment(postId: String, commentText: String) {
        if (commentText.isBlank()) return
        val newComment = PostComment(
            id = "c_${System.currentTimeMillis()}",
            authorName = "أنت (أنا)",
            text = commentText.trim(),
            timeAgo = "الآن"
        )
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    post.copy(
                        commentsCount = post.commentsCount + 1,
                        commentsList = listOf(newComment) + post.commentsList
                    )
                } else post
            }
        }
        _userMessage.value = "تمت إضافة تعليقك بنجاح!"
    }

    fun sharePost(post: Post) {
        _posts.update { list ->
            list.map { if (it.id == post.id) it.copy(sharesCount = it.sharesCount + 1) else it }
        }
        _userMessage.value = "تم نسخ رابط المنشور ومشاركته مع الأصدقاء!"
    }

    fun publishPost(content: String, tag: String, mediaType: PostMediaType) {
        if (content.isBlank()) return
        val profile = _userProfile.value
        val newPost = Post(
            id = "p_${System.currentTimeMillis()}",
            authorName = profile.name,
            authorHandle = "ID: ${profile.id}",
            authorRole = "مشرف فريق فرسان الضاد",
            timeAgo = "الآن",
            content = content.trim(),
            mediaType = mediaType,
            mediaCaption = if (mediaType != PostMediaType.NONE) "محتوى مرئي مرفق" else null,
            tag = if (tag.isNotBlank()) if (tag.startsWith("#")) tag else "#$tag" else null,
            likesCount = 1,
            isLiked = true,
            commentsCount = 0,
            sharesCount = 0,
            commentsList = emptyList(),
            isAuthor = true,
            isFollowing = false
        )
        _posts.update { listOf(newPost) + it }
        _userMessage.value = "تم نشر منشورك بنجاح في خلاصة المجتمع!"
        addNotification(NotificationType.SYSTEM, "تم نشر منشورك بنجاح")
    }

    fun deletePost(postId: String) {
        _posts.update { list -> list.filter { it.id != postId } }
        _userMessage.value = "تم حذف المنشور بنجاح"
    }

    fun editPost(postId: String, newContent: String) {
        if (newContent.isBlank()) return
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) post.copy(content = newContent.trim()) else post
            }
        }
        _userMessage.value = "تم تعديل المنشور بنجاح"
    }

    fun reportPost(postId: String, reason: String = "محتوى غير لائق") {
        _userMessage.value = "تم استلام البلاغ وسيتم مراجعته من قِبل المشرفين، شكراً لمساعدتك في حماية المجتمع"
    }

    fun toggleFollowUser(postId: String) {
        var isNowFollowing = false
        _posts.update { list ->
            val target = list.find { it.id == postId } ?: return@update list
            val author = target.authorName
            val newFollow = !target.isFollowing
            isNowFollowing = newFollow
            list.map { post ->
                if (post.authorName == author) {
                    post.copy(isFollowing = newFollow)
                } else post
            }
        }
        _userMessage.value = if (isNowFollowing) "تمت متابعة المستخدم بنجاح" else "تم إلغاء المتابعة"
    }

    // --- Story Actions ---
    fun openStory(story: Story) {
        _activeStory.value = story
        _stories.update { list ->
            list.map { if (it.id == story.id) it.copy(isViewed = true) else it }
        }
    }

    fun closeStory() {
        _activeStory.value = null
    }

    fun addStory(
        text: String = "",
        mediaUri: String? = null,
        mediaType: StoryMediaType = StoryMediaType.TEXT,
        gradientColors: List<Long> = listOf(0xFF673AB7, 0xFF00897B)
    ) {
        val newStory = Story(
            id = "story_${System.currentTimeMillis()}",
            authorName = "قصتي",
            isViewed = false,
            mediaText = text.trim(),
            timeAgo = "الآن",
            isCurrentUser = true,
            gradientColors = gradientColors,
            mediaType = mediaType,
            mediaUri = mediaUri
        )
        _stories.update { list ->
            listOf(newStory) + list.filter { !it.isCurrentUser }
        }
        _userMessage.value = "تم نشر قصتك المؤقتة لجميع المتابعين بنجاح!"
        addNotification(NotificationType.SYSTEM, "تم نشر قصتك بنجاح")
    }

    // --- Chat Actions ---
    fun setChatFilter(filter: String) {
        _chatFilter.value = filter
    }

    fun togglePushNotifications() {
        _pushNotificationsEnabled.value = !_pushNotificationsEnabled.value
        _userMessage.value = if (_pushNotificationsEnabled.value) "تم تفعيل التنبيهات الفورية (Push Notifications)" else "تم كتم التنبيهات الفورية"
    }

    fun openConversation(conversationId: String) {
        _activeChatId.value = conversationId
        _conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it }
        }
    }

    fun closeConversation() {
        _activeChatId.value = null
    }

    fun sendMessage(conversationId: String, text: String, type: ChatMessageType = ChatMessageType.TEXT) {
        if (text.isBlank() && type == ChatMessageType.TEXT) return
        val msg = ChatMessage(
            id = "m_${System.currentTimeMillis()}",
            senderName = "أنت",
            text = text,
            timestamp = "الآن",
            isFromMe = true,
            type = type,
            audioDurationSec = if (type == ChatMessageType.AUDIO) 12 else 0,
            gameTitle = if (type == ChatMessageType.GAME_INVITE) "تحدي الألعاب الكلاسيكية" else null,
            gameReward = if (type == ChatMessageType.GAME_INVITE) 100 else 0
        )
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) {
                    val preview = when (type) {
                        ChatMessageType.AUDIO -> "تسجيل صوتي (0:12)"
                        ChatMessageType.IMAGE -> "صورة مرفقة 📷"
                        ChatMessageType.GAME_INVITE -> "دعوة لتحدي لعبة سريعة 🎮"
                        else -> text
                    }
                    conv.copy(
                        lastMessage = "أنت: $preview",
                        time = "الآن",
                        messages = conv.messages + msg
                    )
                } else conv
            }
        }
    }

    fun startInChatGame(conversationId: String, gameType: GameType = GameType.LUDO) {
        val gameTitle = gameType.titleAr
        val msg = ChatMessage(
            id = "m_${System.currentTimeMillis()}",
            senderName = "أنت",
            text = "هيا نلعب $gameTitle معاً الآن! 🎮",
            timestamp = "الآن",
            isFromMe = true,
            type = ChatMessageType.GAME_INVITE,
            gameTitle = gameTitle,
            gameReward = 100
        )
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) {
                    conv.copy(
                        lastMessage = "أنت: دعوة لتحدي $gameTitle 🎮",
                        time = "الآن",
                        messages = conv.messages + msg
                    )
                } else conv
            }
        }
        _userMessage.value = "تم إرسال دعوة لعبة $gameTitle إلى المحادثة! يمكنك خوض اللعبة بالضغط على 'العب التحدي الآن'."
    }

// --- Chat Rooms Actions ---
    fun setChatSubTab(tab: String) {
        _chatSubTab.value = tab
    }

    fun setRoomCategoryFilter(category: String) {
        _roomCategoryFilter.value = category
    }

    fun openRoom(roomId: String) {
        val room = _chatRooms.value.find { it.id == roomId } ?: return

        if (room.blockedMembers.any { it.id == "me" }) {
            _userMessage.value = "لا يمكنك دخول هذه الغرفة."
            return
        }

        if (room.isLocked && !room.isOwner && !room.isJoined) {
            _userMessage.value = "هذه الغرفة مقفلة حاليًا ولا تقبل أعضاء جدد."
            return
        }

        _activeRoomId.value = roomId
    }

    fun closeRoom() {
        _activeRoomId.value = null
    }

    fun createChatRoom(
        name: String,
        description: String,
        category: String,
        accessType: RoomAccessType,
        password: String?,
        maxMembers: Int,
        iconEmoji: String,
        imageUrl: String? = null
    ) {
        val existingIds = _chatRooms.value.map { it.id }.toSet()
        var generatedId: String
        do {
            generatedId = (10000000..99999999).random().toString()
        } while (existingIds.contains(generatedId))

        val newRoom = ChatRoom(
            id = generatedId,
            name = name.trim(),
            description = description.trim(),
            category = category,
            iconEmoji = iconEmoji,
            imageUrl = imageUrl,
            accessType = accessType,
            password = if (accessType == RoomAccessType.PASSWORD) password else null,
            inviteCode = "INVITE-${(1000..9999).random()}",
            memberCount = 1,
            maxMembers = maxMembers.coerceIn(10, 1000),
            isJoined = true,
            isOwner = true,
            pinnedMessage = "📌 أهلاً بكم في غرفة $name الجديدة! نتمنى لكم قضاء أمتع الأوقات والالتزام بالاحترام المتبادل.",
            members = listOf(
                RoomMember("me", "أنت (المالك)", RoomMemberRole.OWNER, isOnline = true, avatarUrl = _userProfile.value.avatarUrl)
            ),
            messages = listOf(
                ChatMessage(
                    id = "rm_init",
                    senderName = "النظام",
                    text = "تم إنشاء الغرفة بنجاح. يرجى الالتزام بالآداب العامة وسياسة المحتوى.",
                    timestamp = "الآن",
                    isFromMe = false
                )
            )
        )
        _chatRooms.update { listOf(newRoom) + it }
        _activeRoomId.value = newRoom.id
        _userMessage.value = "تم إنشاء غرفة الدردشة '${newRoom.name}' بنجاح وأنت الآن المالك!"
    }
    
fun joinChatRoom(roomId: String, passwordInput: String = ""): Boolean {
        val room = _chatRooms.value.find { it.id == roomId } ?: return false

        if (room.blockedMembers.any { it.id == "me" }) {
            _userMessage.value = "لا يمكنك الانضمام لهذه الغرفة."
            return false
        }

        if (room.isJoined) {
            _activeRoomId.value = roomId
            return true
        }

        if (room.isLocked && !room.isOwner) {
            _userMessage.value = "هذه الغرفة مقفلة حاليًا ولا تقبل أعضاء جدد."
            return false
        }

        if (room.accessType == RoomAccessType.PASSWORD && room.password != passwordInput.trim()) {
            _userMessage.value = "كلمة المرور غير صحيحة للغرفة!"
            return false
        }

        if (room.memberCount >= room.maxMembers) {
            _userMessage.value = "الغرفة ممتلئة بالكامل (الحد الأقصى ${room.maxMembers} عضو)!"
            return false
        }

        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(
                        isJoined = true,
                        memberCount = it.memberCount + 1,
                        members = it.members + RoomMember("me", "أنت", RoomMemberRole.MEMBER, isOnline = true, avatarUrl = _userProfile.value.avatarUrl),
                        messages = it.messages + ChatMessage(
                            id = "rm_join_${System.currentTimeMillis()}",
                            senderName = "النظام",
                            text = "انضم 'أنت' إلى الغرفة 👋",
                            timestamp = "الآن",
                            isFromMe = false
                        )
                    )
                } else it
            }
        }
        _activeRoomId.value = roomId
        _userMessage.value = "تم الانضمام إلى الغرفة بنجاح!"
        return true
    }

    fun leaveChatRoom(roomId: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(
                        isJoined = false,
                        memberCount = (it.memberCount - 1).coerceAtLeast(0),
                        members = it.members.filter { m -> m.id != "me" },
                        voiceSeats = it.voiceSeats.map { seat ->
                            if (seat.occupantId == "me") {
                                seat.copy(occupantId = null, occupantName = null, occupantAvatarUrl = null, isMuted = false)
                            } else seat
                        },
                        voiceSeatRequests = it.voiceSeatRequests.filter { req -> req.requesterId != "me" }
                    )
                } else it
            }
        }
        _activeRoomId.value = null
        _userMessage.value = "تمت مغادرة الغرفة."
    }

    fun toggleRoomLock(roomId: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) it.copy(isLocked = !it.isLocked) else it
            }
        }
    }
fun updateRoomBackground(roomId: String, imageUrl: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) it.copy(backgroundImageUrl = imageUrl.ifBlank { null }) else it
            }
        }
}
fun blockRoomMember(roomId: String, memberId: String) {
        _chatRooms.update { list ->
            list.map { room ->
                if (room.id == roomId) {
                    val memberToBlock = room.members.find { it.id == memberId }
                    if (memberToBlock != null) {
                        room.copy(
                            members = room.members.filter { it.id != memberId },
                            memberCount = (room.memberCount - 1).coerceAtLeast(0),
                            blockedMembers = room.blockedMembers + memberToBlock
                        )
                    } else room
                } else room
            }
        }
    }

    fun unblockRoomMember(roomId: String, memberId: String) {
        _chatRooms.update { list ->
            list.map { room ->
                if (room.id == roomId) {
                    room.copy(blockedMembers = room.blockedMembers.filter { it.id != memberId })
                } else room
            }
        }
    }
    fun sendRoomMessage(roomId: String, text: String, type: ChatMessageType = ChatMessageType.TEXT) {
        if (text.isBlank() && type == ChatMessageType.TEXT) return
        val room = _chatRooms.value.find { it.id == roomId } ?: return
        val currentMember = room.members.find { it.id == "me" }
        if (currentMember?.isMuted == true) {
            _userMessage.value = "تم كتمك في هذه الغرفة من قبل المشرف، لا يمكنك إرسال رسائل حالياً!"
            return
        }

        val msg = ChatMessage(
            id = "rm_${System.currentTimeMillis()}",
            senderName = "أنت",
            text = text,
            timestamp = "الآن",
            isFromMe = true,
            type = type,
            audioDurationSec = if (type == ChatMessageType.AUDIO) 15 else 0,
            gameTitle = if (type == ChatMessageType.GAME_INVITE) "تحدي الألعاب الجماعي للغرفة 🎮" else null,
            gameReward = if (type == ChatMessageType.GAME_INVITE) 100 else 0
        )
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(messages = it.messages + msg)
                } else it
            }
        }
    }

    fun pinRoomMessage(roomId: String, messageText: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(pinnedMessage = "📌 $messageText")
                } else it
            }
        }
        _userMessage.value = "تم تثبيت الإعلان في أعلى الغرفة بنجاح!"
    }

    fun unpinRoomMessage(roomId: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(pinnedMessage = null)
                } else it
            }
        }
        _userMessage.value = "تم إلغاء تثبيت الإعلان."
    }

    fun deleteRoomMessage(roomId: String, messageId: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(messages = it.messages.filter { m -> m.id != messageId })
                } else it
            }
        }
        _userMessage.value = "تم حذف الرسالة من الغرفة."
    }

    fun kickRoomMember(roomId: String, memberId: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    val kickedMember = it.members.find { m -> m.id == memberId }
                    val memberName = kickedMember?.name ?: "العضو"
                    it.copy(
                        memberCount = (it.memberCount - 1).coerceAtLeast(1),
                        members = it.members.filter { m -> m.id != memberId },
                        messages = it.messages + ChatMessage(
                            id = "rm_kick_${System.currentTimeMillis()}",
                            senderName = "إدارة الغرفة",
                            text = "تم طرد $memberName من الغرفة من قبل المشرف.",
                            timestamp = "الآن",
                            isFromMe = false
                        )
                    )
                } else it
            }
        }
        _userMessage.value = "تم طرد العضو من الغرفة بنجاح."
    }

    fun muteRoomMember(roomId: String, memberId: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    val target = it.members.find { m -> m.id == memberId }
                    val willBeMuted = !(target?.isMuted ?: false)
                    it.copy(
                        members = it.members.map { m ->
                            if (m.id == memberId) m.copy(isMuted = willBeMuted) else m
                        }
                    )
                } else it
            }
        }
        _userMessage.value = "تم تحديث حالة كتم العضو."
    }
fun requestVoiceSeat(roomId: String, seatNumber: Int) {
        val room = _chatRooms.value.find { it.id == roomId } ?: return
        val me = room.members.find { it.id == "me" }
        if (me?.isMuted == true) {
            _userMessage.value = "لا يمكنك طلب المايك وأنت مكتوم."
            return
        }
        val requestKey = "${roomId}_me"
        val lastRequest = lastVoiceSeatRequestTime[requestKey]
        val now = System.currentTimeMillis()
        if (lastRequest != null && now - lastRequest < 10 * 60 * 1000) {
            _userMessage.value = "يمكنك طلب المايك مرة كل 10 دقائق فقط."
            return
        }
        val seat = room.voiceSeats.find { it.seatNumber == seatNumber }
        if (seat?.occupantId != null) {
            _userMessage.value = "هذا المقعد مشغول بالفعل."
            return
        }
        if (room.voiceSeatRequests.any { it.requesterId == "me" }) {
            _userMessage.value = "لديك طلب معلّق بالفعل بانتظار الرد."
            return
        }
        lastVoiceSeatRequestTime[requestKey] = now
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(
                        voiceSeatRequests = it.voiceSeatRequests + VoiceSeatRequest(
                            id = "vsr_${System.currentTimeMillis()}",
                            requesterId = "me",
                            requesterName = me?.name ?: "أنت",
                            requesterAvatarUrl = _userProfile.value.avatarUrl,
                            seatNumber = seatNumber
                        )
                    )
                } else it
            }
        }
        _userMessage.value = "تم إرسال طلب المايك، بانتظار موافقة المالك."
}
fun respondToVoiceSeatRequest(roomId: String, requestId: String, accept: Boolean) {
        _chatRooms.update { list ->
            list.map { room ->
                if (room.id == roomId) {
                    val request = room.voiceSeatRequests.find { it.id == requestId }
                    if (request == null) {
                        room
                    } else if (accept) {
                        room.copy(
                            voiceSeats = room.voiceSeats.map { seat ->
                                if (seat.seatNumber == request.seatNumber) {
                                    seat.copy(
                                        occupantId = request.requesterId,
                                        occupantName = request.requesterName,
                                        occupantAvatarUrl = request.requesterAvatarUrl
                                    )
                                } else seat
                            },
                            voiceSeatRequests = room.voiceSeatRequests.filter { it.id != requestId }
                        )
                    } else {
                        room.copy(
                            voiceSeatRequests = room.voiceSeatRequests.filter { it.id != requestId }
                        )
                    }
                } else room
            }
        }
        _userMessage.value = if (accept) "تم قبول الطلب وصعود العضو للمايك." else "تم رفض الطلب."
    }

    fun takeVoiceSeatDirectly(roomId: String, seatNumber: Int) {
        val room = _chatRooms.value.find { it.id == roomId } ?: return
        val me = room.members.find { it.id == "me" }
        val isPrivileged = room.isOwner || me?.role == RoomMemberRole.ADMIN
        if (!isPrivileged) {
            _userMessage.value = "هذي الصلاحية للمالك والمشرف فقط."
            return
        }
        val seat = room.voiceSeats.find { it.seatNumber == seatNumber }
        if (seat?.occupantId != null) {
            _userMessage.value = "هذا المقعد مشغول بالفعل."
            return
        }
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(
                        voiceSeats = it.voiceSeats.map { s ->
                            if (s.seatNumber == seatNumber) {
                                s.copy(
                                    occupantId = "me",
                                    occupantName = me?.name ?: "أنت",
                                    occupantAvatarUrl = _userProfile.value.avatarUrl
                                )
                            } else s
                        }
                    )
                } else it
            }
        }
        _userMessage.value = "تم الصعود على المايك."
    }

    fun takeOwnerVoiceSeatDirectly(roomId: String) {
        val room = _chatRooms.value.find { it.id == roomId } ?: return
        if (!room.isOwner) {
            _userMessage.value = "هذا المقعد لمالك الغرفة فقط."
            return
        }
        if (!room.ownerVoiceSeat.occupantId.isNullOrBlank()) {
            _userMessage.value = "أنت بالفعل على مقعد المالك."
            return
        }
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(
                        ownerVoiceSeat = it.ownerVoiceSeat.copy(
                            occupantId = "me",
                            occupantName = it.members.find { m -> m.id == "me" }?.name ?: "المالك",
                            occupantAvatarUrl = _userProfile.value.avatarUrl,
                            isMuted = false
                        )
                    )
                } else it
            }
        }
        _userMessage.value = "تم الصعود على مايك المالك."
    }
fun leaveVoiceSeat(roomId: String, seatNumber: Int) {
        _chatRooms.update { list ->
            list.map { room ->
                if (room.id == roomId) {
                    room.copy(
                        voiceSeats = room.voiceSeats.map { seat ->
                            if (seat.seatNumber == seatNumber) {
                                seat.copy(occupantId = null, occupantName = null, occupantAvatarUrl = null, isMuted = false)
                            } else seat
                        }
                    )
                } else room
            }
        }
}
fun muteVoiceSeat(roomId: String, seatNumber: Int) {
        _chatRooms.update { list ->
            list.map { room ->
                if (room.id == roomId) {
                    room.copy(
                        voiceSeats = room.voiceSeats.map { seat ->
                            if (seat.seatNumber == seatNumber) {
                                seat.copy(isMuted = !seat.isMuted)
                            } else seat
                        }
                    )
                } else room
            }
        }
}
fun toggleOwnerVoiceMute(roomId: String) {
        _chatRooms.update { list ->
            list.map { room ->
                if (room.id == roomId) {
                    room.copy(ownerVoiceSeat = room.ownerVoiceSeat.copy(isMuted = !room.ownerVoiceSeat.isMuted))
                } else room
            }
        }
    }

    fun changeRoomMemberRole(roomId: String, memberId: String, newRole: RoomMemberRole) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(
                        members = it.members.map { m ->
                            if (m.id == memberId) m.copy(role = newRole) else m
                        }
                    )
                } else it
            }
        }
        _userMessage.value = "تم تعيين الرتبة (${newRole.labelAr}) للعضو بنجاح."
    }

    fun updateRoomSettings(roomId: String, newName: String, newDescription: String, newMax: Int) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(
                        name = newName.trim(),
                        description = newDescription.trim(),
                        maxMembers = newMax.coerceIn(10, 1000)
                    )
                } else it
            }
        }
        _userMessage.value = "تم حفظ تعديلات إعدادات الغرفة."
    }

    fun startInRoomGame(roomId: String, gameType: GameType = GameType.LUDO) {
        val gameTitle = gameType.titleAr
        val msg = ChatMessage(
            id = "rm_${System.currentTimeMillis()}",
            senderName = "أنت",
            text = "هيا بنا لبدء جولة $gameTitle جماعية بين أعضاء الغرفة الآن! الجائزة 100 نقطة من النظام 🏆",
            timestamp = "الآن",
            isFromMe = true,
            type = ChatMessageType.GAME_INVITE,
            gameTitle = gameTitle,
            gameReward = 100
        )
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(messages = it.messages + msg)
                } else it
            }
        }
        _userMessage.value = "تم إطلاق تحدي $gameTitle في الغرفة! يمكنك التوجه لقسم الألعاب."
    }

    // --- Games Actions ---
    fun launchGame(gameType: GameType, mode: GameMatchMode = GameMatchMode.RANDOM_OPPONENT, opponent: String? = null) {
        _activeGameType.value = gameType
        _activeGameMode.value = mode
        _activeGameOpponent.value = opponent ?: if (mode == GameMatchMode.WITH_FRIEND) "صديقك المقرب" else "خصم عشوائي (بوت)"
    }

    fun exitGame() {
        _activeGameType.value = null
    }

    fun rewardGameWin(points: Int = 100, gameName: String = "الألعاب") {
        addSystemPoints(points, "مكافأة الفوز في لعبة $gameName")
        _userMessage.value = "مبروك الفوز! أضيفت +$points نقطة إلى محفظتك من النظام 🌟"
    }

    // --- Team Actions ---
    fun updateMemberRole(memberId: String, newRole: String) {
        _myTeam.update { team ->
            val updatedMembers = team.members.map { member ->
                if (member.id == memberId) member.copy(role = newRole) else member
            }
            team.copy(members = updatedMembers)
        }
        _userMessage.value = "تم تحديث رتبة وصلاحيات العضو بنجاح."
    }

    fun inviteMemberToTeam(memberName: String) {
        if (memberName.isBlank()) return
        val newMember = TeamMember(
            id = "m_${System.currentTimeMillis()}",
            name = memberName.trim(),
            role = "عضو جديد",
            scoreContribution = 0,
            isOnline = true
        )
        _myTeam.update { team ->
            team.copy(
                memberCount = team.memberCount + 1,
                members = team.members + newMember
            )
        }
        _userMessage.value = "تم إرسال دعوة الانضمام وقبول $memberName في الفريق!"
    }

    fun createTeam(teamName: String, motto: String) {
        if (teamName.isBlank()) return
        _myTeam.value = Team(
            id = "team_${System.currentTimeMillis()}",
            name = teamName.trim(),
            motto = if (motto.isBlank()) "فريق مجتمعنا المتحد 🛡️" else motto.trim(),
            level = 1,
            totalScore = 500,
            memberCount = 1,
            maxMembers = 20,
            rank = 12,
            members = listOf(
                TeamMember("m_me", "أنت (أنا)", "قائد الفريق ومؤسسه", 500, true)
            ),
            challenges = listOf(
                TeamChallenge("tc_new", "تحدي البداية السريعة", "إكمال 50 جولة ألعاب", 10, 50, 7, 500)
            )
        )
        _userMessage.value = "تهانينا! تم تأسيس فريق '$teamName' بنجاح وأصبحت قائده."
    }

    // --- Referrals & Friends Actions ---
    /**
     * توليد ID رقمي فريد مكون من 8 أرقام للمستخدمين الجدد
     * لضمان عدم التكرار والاتساق مع نظام معرفات الغرف (Unique 8-digit numeric ID)
     */
    fun generateUniqueNumericUserId(): String {
        val existingReferralIds = _referrals.value.map { it.handle }.toSet()
        val currentProfileId = _userProfile.value.id
        var newId: String
        do {
            newId = (10000000..99999999).random().toString()
        } while (newId == currentProfileId || existingReferralIds.contains(newId))
        return newId
    }

    fun simulateNewReferralJoined(newUserName: String = "مستخدم جديد") {
        val trimmed = newUserName.trim()
        val randomNum = (100..999).random()
        val uniqueNumericId = generateUniqueNumericUserId()
        val newUser = ReferredUser(
            id = "ref_${System.currentTimeMillis()}",
            name = if (trimmed.isNotBlank()) trimmed else "عضو جديد #$randomNum",
            handle = uniqueNumericId,
            joinedDate = "اليوم",
            avatarEmoji = listOf("🌟", "🚀", "👤", "🎯", "👑").random()
        )
        _referrals.update { it + newUser }
        addSystemPoints(50, "مكافأة دعوة صديق جديد (${newUser.name})")
        _userMessage.value = "انضم ${newUser.name} (ID: $uniqueNumericId) عبر رابط دعوتك! أضيفت +50 نقطة لرصيدك."
    }

    fun sendFriendRequest(friendInput: String) {
        val trimmed = friendInput.trim()
        if (trimmed.isBlank()) {
            _userMessage.value = "يرجى كتابة رقم الـ ID أو اسم المستخدم!"
            return
        }
        _userMessage.value = "تم إرسال طلب الصداقة إلى $trimmed بنجاح!"
    }

    fun acceptFriendRequest(requestId: String) {
        val req = _friendRequests.value.find { it.id == requestId }
        _friendRequests.update { list -> list.filter { it.id != requestId } }
        _userMessage.value = if (req != null) "تم قبول طلب صداقة ${req.senderName}!" else "تم قبول طلب الصداقة."
    }

    fun rejectFriendRequest(requestId: String) {
        val req = _friendRequests.value.find { it.id == requestId }
        _friendRequests.update { list -> list.filter { it.id != requestId } }
        _userMessage.value = if (req != null) "تم رفض طلب ${req.senderName}." else "تم رفض طلب الصداقة."
    }

    // --- Wallet Actions ---
    fun setWalletFilter(filter: String) {
        _walletFilter.value = filter
    }

    fun claimDailyBonus() {
        if (_dailyBonusClaimed.value) {
            _userMessage.value = "لقد حصلت بالفعل على مكافأة اليوم! عد غداً للمزيد."
            return
        }
        val bonus = 150
        _dailyBonusClaimed.value = true
        addSystemPoints(bonus, "مكافأة تسجيل الدخول اليومي")
        _userMessage.value = "تم شحن محفظتك بـ +$bonus نقطة مكافأة يومية من النظام!"
    }

    private fun addSystemPoints(points: Int, title: String) {
        _walletBalance.update { it + points }
        val newTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            title = title,
            type = TransactionType.EARN,
            points = points,
            date = "اليوم",
            note = "شحن رسمي من النظام (بدون إيداع مالي)"
        )
        _transactions.update { listOf(newTx) + it }
    }

    fun buyStoreItem(item: StoreItem) {
        if (item.isOwned) {
            _userMessage.value = "هذا العنصر مملوك لك بالفعل!"
            return
        }
        if (_walletBalance.value < item.cost) {
            _userMessage.value = "رصيد نقاطك غير كافٍ! العب واكسب المزيد من النقاط المجانية."
            return
        }
        _walletBalance.update { it - item.cost }
        _storeItems.update { list ->
            list.map { if (it.id == item.id) it.copy(isOwned = true) else it }
        }
        val newTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            title = "شراء ${item.title}",
            type = TransactionType.SPEND,
            points = item.cost,
            date = "اليوم",
            note = "عنصر افتراضي للبروفايل"
        )
        _transactions.update { listOf(newTx) + it }
        _userMessage.value = "تم شراء '${item.title}' بنجاح وتطبيقه على حسابك!"
    }

    // --- Profile Actions ---
    fun updateUserBio(newBio: String) {
        _userProfile.update { it.copy(bio = newBio.trim()) }
        _userMessage.value = "تم تحديث النبذة التعريفية بنجاح!"
    }

    fun updateUserProfile(name: String, bio: String, emoji: String) {
        _userProfile.update {
            it.copy(
                name = name.trim(),
                bio = bio.trim(),
                avatarEmoji = emoji
            )
        }
        _userMessage.value = "تم حفظ معلومات الملف الشخصي بنجاح!"
    }

    fun updateUserAvatarUrl(url: String) {
        _userProfile.update { it.copy(avatarUrl = url) }
        _userMessage.value = "تم تحديث صورة الملف الشخصي بنجاح!"
    }

    fun toggleProfileNotifications() {
        _userProfile.update {
            it.copy(isNotificationsEnabled = !it.isNotificationsEnabled)
        }
        val isEnabled = _userProfile.value.isNotificationsEnabled
        _userMessage.value = if (isEnabled) "تم تفعيل إشعارات الحساب" else "تم إيقاف إشعارات الحساب"
    }

    fun toggleProfilePrivacy() {
        _userProfile.update {
            val nextPrivacy = if (it.privacyLevel == "عام للجميع") "للأصدقاء فقط" else "عام للجميع"
            it.copy(privacyLevel = nextPrivacy)
        }
        _userMessage.value = "تم تحديث خصوصية الحساب إلى (${_userProfile.value.privacyLevel})"
    }

    fun onAuthSuccess(account: AuthUserAccount, generatedId: String) {
        _userProfile.update { current ->
            current.copy(
                id = generatedId,
                handle = generatedId,
                name = account.name,
                email = account.email,
                authProvider = "بريد إلكتروني",
                avatarEmoji = account.avatarEmoji
            )
        }
        _isLoggedIn.value = true
        _userMessage.value = "مرحباً بك ${account.name}! تم تعيين معرّفك الرقمي الفريد: $generatedId"
    }

    fun logoutUser() {
        _isLoggedIn.value = false
        _userMessage.value = "تم تسجيل الخروج بنجاح. مرحباً بك في أي وقت!"
    }
}
