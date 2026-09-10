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

    private val _currentTab = MutableStateFlow(AppTab.FEED)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // --- Stories State ---
    private val _stories = MutableStateFlow(
        listOf(
            Story("story_0", "قصتي", isViewed = false, mediaText = "أهلاً بكم في مجتمعنا! 🌟", isCurrentUser = true),
            Story("story_1", "سارة أحمد", isViewed = false, mediaText = "تحدينا في لعبة الكلمات كان حماسياً جداً اليوم! 🏆", timeAgo = "منذ 20 د"),
            Story("story_2", "طارق العمري", isViewed = false, mediaText = "فريق فرسان الضاد في المركز الأول هذا الأسبوع 💪", timeAgo = "منذ 45 د"),
            Story("story_3", "نور الهدى", isViewed = true, mediaText = "صباح الخير والمحبة للجميع ☕✨", timeAgo = "منذ ساعتين"),
            Story("story_4", "فريق الأبطال", isViewed = true, mediaText = "باب الانضمام مفتوح للتحدي الشهري 🎮", timeAgo = "منذ 3 ساعات")
        )
    )
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _activeStory = MutableStateFlow<Story?>(null)
    val activeStory: StateFlow<Story?> = _activeStory.asStateFlow()

    // --- Feed Posts State ---
    private val _posts = MutableStateFlow(
        listOf(
            Post(
                id = "p1",
                authorName = "أحمد خالد",
                authorHandle = "@ahmed_k",
                authorRole = "قائد فريق فرسان الضاد",
                timeAgo = "منذ ساعتين",
                content = "أنهينا اليوم التحدي الجماعي للكلمات والألغاز وحققنا المركز الأول على مستوى المجتمع! شكر كبير لكل أعضاء الفريق على الروح الحماسية 🚀🔥",
                mediaType = PostMediaType.SHORT_VIDEO,
                mediaCaption = "مقتطف من الجولة النهائية لتحدي الكلمات (0:45)",
                tag = "#تحديات_الألعاب",
                likesCount = 84,
                isLiked = false,
                commentsCount = 19,
                sharesCount = 6,
                commentsList = listOf(
                    PostComment("c1", "سارة أحمد", "ألف مبروك يا شباب، أداء أسطوري اليوم! 👏", "منذ ساعة"),
                    PostComment("c2", "عمر الفاروق", "الجولة القادمة ستكون أقوى بإذن الله 🎯", "منذ 30 دقيقة")
                )
            ),
            Post(
                id = "p2",
                authorName = "مها السعيد",
                authorHandle = "@maha_s",
                authorRole = "عضوة مميزة",
                timeAgo = "منذ 4 ساعات",
                content = "حصلت للتو على وسام 'بطل الألغاز' بعد الإجابة على 20 لغز متتالي في قسم الألعاب بدون أي خطأ! النقاط في المحفظة ارتفعت إلى 3200 نقطة ✨🎮 جربوا التحدي الآن.",
                mediaType = PostMediaType.IMAGE,
                mediaCaption = "وسام بطل الألغاز الذهبي 🏅",
                tag = "#ألعاب_الذكاء",
                likesCount = 126,
                isLiked = true,
                commentsCount = 28,
                sharesCount = 11,
                commentsList = listOf(
                    PostComment("c3", "يوسف الدوسري", "ما شاء الله! أي لغز كان الأصعب؟", "منذ 3 ساعات")
                )
            ),
            Post(
                id = "p3",
                authorName = "إدارة مجتمعنا",
                authorHandle = "@mujtama_official",
                authorRole = "حساب رسمي",
                timeAgo = "منذ 6 ساعات",
                content = "تذكير لجميع الأعضاء: تم شحن مكافأة الدخول اليومية في المحفظة (+150 نقطة). النقاط مخصصة لترقية ألقاب الحساب ودعم الفرق وشارات البروفايل، ولا تخضع لأي مراهنات مالية حفاظاً على بيئة آمنة وودية 🛡️❤️",
                mediaType = PostMediaType.NONE,
                tag = "#إعلان_المجتمع",
                likesCount = 310,
                isLiked = true,
                commentsCount = 45,
                sharesCount = 34,
                commentsList = listOf(
                    PostComment("c4", "فاطمة النجار", "شكراً للإدارة على النظام النزيه والبيئة الرائعة", "منذ 5 ساعات")
                )
            )
        )
    )
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _activeCommentPostId = MutableStateFlow<String?>(null)
    val activeCommentPostId: StateFlow<String?> = _activeCommentPostId.asStateFlow()

    // --- Chat State ---
    private val _conversations = MutableStateFlow(
        listOf(
            ChatConversation(
                id = "chat_group_1",
                name = "فريق فرسان الضاد 🛡️",
                isGroup = true,
                lastMessage = "أحمد: هيا لنبدأ تحدي الكلمات الجديد معاً!",
                time = "10:30 ص",
                unreadCount = 2,
                isOnline = true,
                memberCount = 18,
                messages = listOf(
                    ChatMessage("m1", "أحمد خالد", "السلام عليكم جميعاً، جاهزون لجولة اليوم؟", "10:15 ص", false),
                    ChatMessage("m2", "سارة أحمد", "جاهزة ومتحمسة جداً!", "10:18 ص", false),
                    ChatMessage("m3", "أنا", "أهلاً بالجميع، جاهز للانطلاق 🎯", "10:25 ص", true),
                    ChatMessage(
                        id = "m4",
                        senderName = "أحمد خالد",
                        text = "بدأ أحمد لعبة 'تحدي الكلمات والألغاز' المباشرة!",
                        timestamp = "10:30 ص",
                        isFromMe = false,
                        type = ChatMessageType.GAME_INVITE,
                        gameTitle = "تحدي الكلمات والألغاز",
                        gameReward = 150
                    )
                )
            ),
            ChatConversation(
                id = "chat_user_1",
                name = "سارة أحمد",
                isGroup = false,
                lastMessage = "تسجيل صوتي (0:18)",
                time = "أمس",
                unreadCount = 0,
                isOnline = true,
                memberCount = 2,
                messages = listOf(
                    ChatMessage("m20", "سارة أحمد", "مرحباً! هل رأيت تحدي الفرق الجديد في قسم الألعاب؟", "أمس 4:00 م", false),
                    ChatMessage("m21", "أنا", "نعم، سنفوز به بالتأكيد ونحصل على نقاط المحفظة 🚀", "أمس 4:05 م", true),
                    ChatMessage("m22", "سارة أحمد", "", "أمس 4:10 م", false, type = ChatMessageType.AUDIO, audioDurationSec = 18)
                )
            ),
            ChatConversation(
                id = "chat_user_2",
                name = "يوسف الدوسري",
                isGroup = false,
                lastMessage = "سأرسل لك دعوة الانضمام الآن",
                time = "أمس",
                unreadCount = 0,
                isOnline = false,
                memberCount = 2,
                messages = listOf(
                    ChatMessage("m30", "يوسف الدوسري", "سأرسل لك دعوة الانضمام الآن", "أمس 2:15 م", false)
                )
            ),
            ChatConversation(
                id = "chat_group_2",
                name = "رابطة محبي الألغاز 🧩",
                isGroup = true,
                lastMessage = "نور: اللغز رقم 15 كان رائعاً جداً",
                time = "منذ يومين",
                unreadCount = 0,
                isOnline = true,
                memberCount = 42,
                messages = listOf(
                    ChatMessage("m40", "نور الهدى", "اللغز رقم 15 كان رائعاً جداً", "منذ يومين", false)
                )
            )
        )
    )
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _chatFilter = MutableStateFlow("الكل") // الكل، متصل الآن
    val chatFilter: StateFlow<String> = _chatFilter.asStateFlow()

    private val _pushNotificationsEnabled = MutableStateFlow(true)
    val pushNotificationsEnabled: StateFlow<Boolean> = _pushNotificationsEnabled.asStateFlow()

    // --- Chat Rooms State ---
    private val _chatSubTab = MutableStateFlow("المحادثات الخاصة") // "المحادثات الخاصة" أو "غرف الدردشة"
    val chatSubTab: StateFlow<String> = _chatSubTab.asStateFlow()

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _roomCategoryFilter = MutableStateFlow("الكل")
    val roomCategoryFilter: StateFlow<String> = _roomCategoryFilter.asStateFlow()

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(
        listOf(
            ChatRoom(
                id = "10842915",
                name = "ديوانية عشاق الألعاب 🎮",
                description = "نقاشات حول أحدث ألعاب الذكاء، الألغاز، والتحديات الثنائية في مجتمعنا.",
                category = "ألعاب",
                iconEmoji = "🎮",
                imageUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=800&auto=format&fit=crop",
                accessType = RoomAccessType.PUBLIC,
                inviteCode = "GAME-2026",
                memberCount = 85,
                maxMembers = 150,
                isJoined = true,
                isOwner = true,
                pinnedMessage = "📌 مرحباً بكم في ديوانية الألعاب! التحدي الأسبوعي يبدأ كل خميس الساعة 8 مساءً مع نقاط مضاعفة.",
                members = listOf(
                    RoomMember("me", "أنت (المالك)", RoomMemberRole.OWNER, isOnline = true),
                    RoomMember("rm_1", "فيصل الغامدي", RoomMemberRole.ADMIN, isOnline = true),
                    RoomMember("rm_2", "نور الهدى", RoomMemberRole.MEMBER, isOnline = true),
                    RoomMember("rm_3", "سارة أحمد", RoomMemberRole.MEMBER, isOnline = true),
                    RoomMember("rm_4", "عمر فاروق", RoomMemberRole.MEMBER, isOnline = false)
                ),
                messages = listOf(
                    ChatMessage("rm_m1", "فيصل الغامدي", "السلام عليكم جميعاً! مبروك للفائزين بتحدي الأمس 👏", "10:30 ص", false),
                    ChatMessage("rm_m2", "نور الهدى", "الله يبارك فيك فيصل، الأسئلة كانت ممتعة جداً", "10:32 ص", false),
                    ChatMessage("rm_m3", "أنت", "أهلاً بالجميع، جاهزون لتحدي اليوم من داخل الغرفة؟ 🚀", "10:35 ص", true),
                    ChatMessage(
                        id = "rm_m4",
                        senderName = "فيصل الغامدي",
                        text = "",
                        timestamp = "10:36 ص",
                        isFromMe = false,
                        type = ChatMessageType.GAME_INVITE,
                        gameTitle = "تحدي الألغاز الجماعي 🧩",
                        gameReward = 200
                    )
                )
            ),
            ChatRoom(
                id = "24918274",
                name = "مجلس الرياضة وكرة القدم ⚽",
                description = "تحليلات المباريات، بطولات الأندية العربية، والنقاشات الرياضية الراقية بدون تعصب.",
                category = "رياضة",
                iconEmoji = "⚽",
                imageUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=800&auto=format&fit=crop",
                accessType = RoomAccessType.PUBLIC,
                inviteCode = "SPORT-2026",
                memberCount = 142,
                maxMembers = 200,
                isJoined = false,
                isOwner = false,
                pinnedMessage = "📌 الالتزام بالروح الرياضية هو شعار الغرفة الأول دائماً.",
                members = listOf(
                    RoomMember("rm_s1", "سلطان العتيبي", RoomMemberRole.OWNER, isOnline = true),
                    RoomMember("rm_s2", "ماجد عبد الله", RoomMemberRole.ADMIN, isOnline = true),
                    RoomMember("rm_s3", "خالد الشمري", RoomMemberRole.MEMBER, isOnline = false)
                ),
                messages = listOf(
                    ChatMessage("rm_sm1", "سلطان العتيبي", "أهلاً بعشاق الساحرة المستديرة في مجلسنا الرياضي!", "أمس", false)
                )
            ),
            ChatRoom(
                id = "38102948",
                name = "نخبة فرق التحدي الذهبية 🛡️",
                description = "غرفة مغلقة بكلمة مرور لقادة ومشرفي الفرق لتنسيق الاستراتيجيات وجوائز المحفظة.",
                category = "فرق",
                iconEmoji = "🛡️",
                imageUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?q=80&w=800&auto=format&fit=crop",
                accessType = RoomAccessType.PASSWORD,
                password = "1234",
                inviteCode = "TEAM-VIP",
                memberCount = 28,
                maxMembers = 50,
                isJoined = false,
                isOwner = false,
                pinnedMessage = "📌 كلمة المرور مطلوبة للدخول. نقاط التحدي القادم 5,000 نقطة من النظام.",
                members = listOf(
                    RoomMember("rm_t1", "كابتن زياد", RoomMemberRole.OWNER, isOnline = true),
                    RoomMember("rm_t2", "هدى المنصور", RoomMemberRole.ADMIN, isOnline = true)
                ),
                messages = listOf(
                    ChatMessage("rm_tm1", "كابتن زياد", "تم تجديد خطة التحديات الأسبوعية للفريق.", "منذ يومين", false)
                )
            ),
            ChatRoom(
                id = "49201943",
                name = "صالون الأدب والثقافة العامة 📚",
                description = "مساحة راقية للحوار الثقافي، الشعر العربي، وتحديات اللغة والألغاز الفكرية.",
                category = "عام",
                iconEmoji = "📚",
                imageUrl = "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?q=80&w=800&auto=format&fit=crop",
                accessType = RoomAccessType.INVITE_ONLY,
                inviteCode = "ADAB2026",
                memberCount = 45,
                maxMembers = 100,
                isJoined = false,
                isOwner = false,
                pinnedMessage = "📌 الانضمام لهذه الغرفة عبر كود الدعوة فقط لضمان جودة المحتوى الأدبي.",
                members = listOf(
                    RoomMember("rm_a1", "د. عبد الرحمن", RoomMemberRole.OWNER, isOnline = true)
                ),
                messages = listOf(
                    ChatMessage("rm_am1", "د. عبد الرحمن", "مرحباً بروّاد الكلمة الطيبة والفكر المستنير.", "منذ 3 أيام", false)
                )
            )
        )
    )
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    init {
        startLiveRoomUpdates()
    }

    private fun startLiveRoomUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(12000)
                _chatRooms.update { currentRooms ->
                    currentRooms.map { room ->
                        // Slight natural member count fluctuation (+/- 1 or 2) within bounds
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

    // --- Games State ---
    private val _activeGameType = MutableStateFlow<GameType?>(null)
    val activeGameType: StateFlow<GameType?> = _activeGameType.asStateFlow()

    private val _activeGameMode = MutableStateFlow(GameMatchMode.RANDOM_OPPONENT)
    val activeGameMode: StateFlow<GameMatchMode> = _activeGameMode.asStateFlow()

    private val _activeGameOpponent = MutableStateFlow("خصم عشوائي")
    val activeGameOpponent: StateFlow<String> = _activeGameOpponent.asStateFlow()

    // --- Team State ---
    private val _myTeam = MutableStateFlow(
        Team(
            id = "team_1",
            name = "فرسان الضاد",
            motto = "بالعلم والذكاء نتصدر القمم 🏹",
            level = 5,
            totalScore = 24650,
            memberCount = 18,
            maxMembers = 25,
            rank = 1,
            members = listOf(
                TeamMember("m_1", "أحمد خالد", "قائد", 4200, true),
                TeamMember("m_2", "سارة أحمد", "مشرف", 3850, true),
                TeamMember("m_3", "أنت (أنا)", "مشرف", 3450, true),
                TeamMember("m_4", "يوسف الدوسري", "عضو", 2100, false),
                TeamMember("m_5", "منى الحربي", "عضو", 1900, true),
                TeamMember("m_6", "فهد السالم", "عضو", 1650, false)
            ),
            challenges = listOf(
                TeamChallenge("tc1", "تحدي المليون نقطة الأسبوعي", "جمع نقاط الألعاب عبر كل أعضاء الفريق", 780000, 1000000, 3, 2500),
                TeamChallenge("tc2", "حل 500 لغز عربي", "الإجابة الصحيحة على ألغاز الكلمات", 340, 500, 5, 1200)
            )
        )
    )
    val myTeam: StateFlow<Team> = _myTeam.asStateFlow()

    // --- Referrals & Friends State ---
    private val _referrals = MutableStateFlow<List<ReferredUser>>(emptyList())
    val referrals: StateFlow<List<ReferredUser>> = _referrals.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    // --- Wallet State ---
    private val _walletBalance = MutableStateFlow(3450)
    val walletBalance: StateFlow<Int> = _walletBalance.asStateFlow()

    private val _dailyBonusClaimed = MutableStateFlow(false)
    val dailyBonusClaimed: StateFlow<Boolean> = _dailyBonusClaimed.asStateFlow()

    private val _walletFilter = MutableStateFlow("الكل") // الكل، كسب، إنفاق
    val walletFilter: StateFlow<String> = _walletFilter.asStateFlow()

    private val _transactions = MutableStateFlow(
        listOf(
            WalletTransaction("t1", "هدية ترحيبية من النظام", TransactionType.EARN, 1000, "01 سبتمبر", "مكافأة الانضمام للتطبيق"),
            WalletTransaction("t2", "مكافأة الفوز في مسابقة الألغاز", TransactionType.EARN, 350, "04 سبتمبر", "تحدي الكلمات الثنائي"),
            WalletTransaction("t3", "شراء إطار الحساب الذهبي", TransactionType.SPEND, 500, "06 سبتمبر", "متجر المقتنيات الافتراضية"),
            WalletTransaction("t4", "مكافأة تحدي الفريق الأسبوعي", TransactionType.EARN, 800, "07 سبتمبر", "فريق فرسان الضاد - المركز الأول"),
            WalletTransaction("t5", "مكافأة تسجيل الدخول اليومي", TransactionType.EARN, 150, "اليوم", "شحن نظامي مجاني")
        )
    )
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    private val _storeItems = MutableStateFlow(
        listOf(
            StoreItem("item_1", "إطار البروفايل الذهبي", "إطار ملكي مميز يظهر حول صورتك في التعليقات والدردشة", 500, "إطارات", isOwned = true),
            StoreItem("item_2", "وسام عبقري الألغاز", "شارة تظهر بجانب اسمك تثبت تفوقك في تحديات الذكاء", 750, "أوسمة", isOwned = false),
            StoreItem("item_3", "ثيم الدردشة الليلي الفاخر", "ألوان وخلفيات مخصصة لمحادثاتك الخاصة ومحادثات الفريق", 400, "ثيمات", isOwned = false),
            StoreItem("item_4", "درع دعم الفريق المشترك", "يرفع مضاعف نقاط الفريق بنسبة 10% لمدة 24 ساعة", 900, "تعزيزات", isOwned = false)
        )
    )
    val storeItems: StateFlow<List<StoreItem>> = _storeItems.asStateFlow()

    // Profile State
    private val _userProfile = MutableStateFlow(UserProfile())
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
            commentsList = emptyList()
        )
        _posts.update { listOf(newPost) + it }
        _userMessage.value = "تم نشر منشورك بنجاح في خلاصة المجتمع!"
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

    fun addStory(text: String) {
        if (text.isBlank()) return
        val newStory = Story(
            id = "story_${System.currentTimeMillis()}",
            authorName = "قصتي",
            isViewed = false,
            mediaText = text.trim(),
            timeAgo = "الآن",
            isCurrentUser = true
        )
        _stories.update { list ->
            listOf(newStory) + list.filter { !it.isCurrentUser }
        }
        _userMessage.value = "تم نشر قصتك المؤقتة لجميع المتابعين!"
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
                RoomMember("me", "أنت (المالك)", RoomMemberRole.OWNER, isOnline = true)
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

    fun updateRoomImage(roomId: String, imageUrl: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) it.copy(imageUrl = imageUrl) else it
            }
        }
    }

    fun joinChatRoom(roomId: String, passwordInput: String = ""): Boolean {
        val room = _chatRooms.value.find { it.id == roomId } ?: return false
        if (room.isJoined) {
            _activeRoomId.value = roomId
            return true
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
                        members = it.members + RoomMember("me", "أنت", RoomMemberRole.MEMBER, isOnline = true),
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
                        members = it.members.filter { m -> m.id != "me" }
                    )
                } else it
            }
        }
        _activeRoomId.value = null
        _userMessage.value = "تمت مغادرة الغرفة."
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

    fun logoutUser() {
        _userMessage.value = "تم تسجيل الخروج بنجاح. مرحباً بك في أي وقت!"
    }
}
