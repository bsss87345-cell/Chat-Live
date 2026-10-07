package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LocalStore
import com.example.BuildConfig
import com.example.data.AuthService
import com.example.data.CloudStore
import com.example.data.PasswordHasher
import com.example.data.SecureSessionStore
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SocialAppViewModel : ViewModel() {

    // --- Authentication State (الشاشة التي تظهر عند فتح التطبيق لأول مرة) ---
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _currentTab = MutableStateFlow(AppTab.FEED)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // --- Stories State ---
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _activeStory = MutableStateFlow<Story?>(null)
    val activeStory: StateFlow<Story?> = _activeStory.asStateFlow()

    // --- Feed Posts State ---
    private fun generateMockPostsFor(authorId: String, authorName: String, authorHandle: String, count: Int): List<Post> {
        val sampleContents = listOf(
            "يوم جميل اليوم، الحمدلله على كل شي!",
            "أخيراً خلصت مشروع كنت شغال عليه من فترة",
            "أفضل قهوة جربتها هالأسبوع ☕",
            "مين عنده توصية مكان حلو للعشاء؟",
            "بداية جديدة دايماً فيها حماس",
            "شكراً لكل من دعمني وساندني، الله يوفقكم",
            "لحظة هادئة وسط زحمة اليوم",
            "تعلمت شي جديد اليوم، الحمدلله"
        )
        return (1..count).map { i ->
            Post(
                id = "mock_post_${authorId}_$i",
                authorId = authorId,
                authorName = authorName,
                authorHandle = authorHandle,
                timeAgo = "${i}س",
                content = sampleContents[i % sampleContents.size],
                likesCount = (5..150).random(),
                isLiked = false,
                commentsCount = (0..20).random(),
                sharesCount = (0..10).random(),
                commentsList = emptyList(),
                isAuthor = false,
                isFollowing = false
            )
        }
    }

    private val _posts = MutableStateFlow<List<Post>>(
        generateMockPostsFor("10000001", "سارة أحمد", "@sara_a", 14) +
        generateMockPostsFor("10000002", "محمد العلي", "@m_ali", 27) +
        generateMockPostsFor("10000003", "نورة سالم", "@noura_s", 6) +
        generateMockPostsFor("10000004", "خالد فهد", "@khalid_f", 41)
    )
        val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    /** المنشورات اللي حذفها المستخدم — تظهر بصفحة «المحتوى المحذوف» بالنشاط. */
    private val _deletedPosts = MutableStateFlow<List<Post>>(emptyList())
    val deletedPosts: StateFlow<List<Post>> = _deletedPosts.asStateFlow()

    /** معرّفات المنشورات اللي شاركها المستخدم (الأحدث أولاً، بلا تكرار). */
    private val _sharedPostIds = MutableStateFlow<List<String>>(emptyList())
    val sharedPostIds: StateFlow<List<String>> = _sharedPostIds.asStateFlow()

        private val _activeCommentPostId = MutableStateFlow<String?>(null)
    val activeCommentPostId: StateFlow<String?> = _activeCommentPostId.asStateFlow()

    // --- Deep Link: منشور مطلوب فتحه من رابط خارجي (chatlive://post/<id>) ---
    private val _pendingPostId = MutableStateFlow<String?>(null)
    val pendingPostId: StateFlow<String?> = _pendingPostId.asStateFlow()

    /**
     * يُستدعى عند فتح التطبيق من رابط منشور.
     * ينقل المستخدم لتبويب الرئيسية ويعلّم المنشور المطلوب حتى تمرر له الخلاصة.
     */
    fun openPostFromLink(postId: String) {
        if (postId.isBlank()) return
        if (_posts.value.none { it.id == postId }) {
            _userMessage.value = "المنشور غير متاح على هذا الجهاز"
            return
        }
        _showAccountSettings.value = false
        _currentTab.value = AppTab.FEED
        _pendingPostId.value = postId
    }

    /** تُستدعى بعد ما تخلص الخلاصة من التمرير للمنشور. */
    fun clearPendingPost() {
        _pendingPostId.value = null
    }

    // --- Chat State ---
    private val _conversations = MutableStateFlow<List<ChatConversation>>(
        listOf(
            ChatConversation(
                id = "test_conv_1",
                name = "محادثة تجريبية",
                isGroup = false,
                lastMessage = "هذه رسالة تجريبية لاختبار الضغط المطوّل",
                time = "الآن",
                unreadCount = 1,
                isOnline = true
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
        // ☁️ مزامنة سحابية (القرار 2ب): يُخزَّن الإشعار لتراه من أي جهاز — بلا حجب الواجهة
        viewModelScope.launch(Dispatchers.IO) { CloudStore.saveNotification(item) }
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        // ☁️ نفس الحالة سحابياً
        val ids = _notifications.value.map { it.id }
        viewModelScope.launch(Dispatchers.IO) { ids.forEach { CloudStore.markNotificationRead(it) } }
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
                restoreLocalData()
        startLiveRoomUpdates()
               } 

    /**
     * يستبدل الهوية المحلية بهوية الخادم (uid حقيقي + رمز جلسة) — **بعد تفعيل AuthService فقط**.
     * قبل التفعيل: ترجع فوراً بلا أي اتصال شبكة ⇒ السلوك المحلي كما هو تماماً.
     * مستخدمة من [onAuthSuccess] — لا كود ميت.
     */
    private fun syncIdentityWithServer(account: AuthUserAccount) {
        if (!AuthService.isConfigured) return
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) {
                AuthService.registerOrLogin(account.email, account.password)
            }) {
                is AuthService.Result.Ok -> {
                    // uid الخادم هو مفتاح المستند في قاعدة البيانات (لا معرّفات من العميل)
                    _userProfile.update { it.copy(id = result.uid, authProvider = "Firebase") }
                    SecureSessionStore.save(
                        userId = result.uid,
                        token = result.idToken,
                        provider = "firebase"
                    )
                }
                is AuthService.Result.Error -> _userMessage.value = result.message
                AuthService.Result.NotConfigured -> Unit
            }
        }
    }

    /**
     * يستعيد الجلسة المحفوظة (مشفّرة بـAndroid Keystore) بعد إغلاق التطبيق.
     * بلا Keystore متاح (بيئة اختبار مثلاً) ترجع false بهدوء ⇒ السلوك القديم بلا انهيار.
     */
    private fun restoreSession() {
        if (_userProfile.value.id.isBlank()) return
        val saved = SecureSessionStore.load() ?: return
        if (saved.userId != _userProfile.value.id) return
        _isLoggedIn.value = true
    }
    /**
     * يستعيد منشورات المستخدم المحفوظة محلياً ويضعها فوق المنشورات التجريبية،
     * ثم يحذف ملفات الوسائط اليتيمة اللي ما عاد لها منشور.
     */
                private fun restoreLocalData() {
        val savedPosts = LocalStore.loadMyPosts()
        if (savedPosts.isNotEmpty()) {
            _posts.update { savedPosts + it }
        }

        // استعادة سلة المحذوفات وقائمة مشاركاتي المحفوظة
        _deletedPosts.value = LocalStore.loadDeletedPosts()
        _sharedPostIds.value = LocalStore.loadSharedPostIds()

        // استعادة الغرف: الأعضاء والرسائل حالة جلسة، فنعيد إضافة "أنا" فقط
        val savedRooms = LocalStore.loadRooms()
        if (savedRooms.isNotEmpty()) {
            _chatRooms.value = savedRooms.map { room ->
                when {
                    room.isOwner -> room.copy(
                        members = listOf(
                            RoomMember("me", "أنت (المالك)", RoomMemberRole.OWNER, isOnline = true)
                        )
                    )
                    room.isJoined -> room.copy(
                        members = listOf(
                            RoomMember("me", "أنت", RoomMemberRole.MEMBER, isOnline = true)
                        )
                    )
                    else -> room
                }
            }
        }

        // حفظ تلقائي عند أي تغيير حقيقي بالغرف.
        // البصمة تتجاهل memberCount لأنه يتذبذب عشوائياً كل 12 ثانية
        // ولا نريد كتابة على القرص بلا سبب.
        viewModelScope.launch {
            _chatRooms
                .map { rooms -> rooms.joinToString("||") { roomSignature(it) } }
                .distinctUntilChanged()
                .collect { LocalStore.saveRooms(_chatRooms.value) }
        }

                          LocalStore.cleanupOrphanMedia(
            (_posts.value + _deletedPosts.value)
                .map { it.mediaUri }.filter { it.isNotBlank() }.toSet()
        )
    }

    /** بصمة الحقول المحفوظة فقط، بدون memberCount المتغيّر تلقائياً. */
    private fun roomSignature(r: ChatRoom): String = listOf(
        r.id, r.name, r.description, r.category, r.iconEmoji,
        r.accessType.name, r.password ?: "", r.maxMembers.toString(),
        r.isJoined.toString(), r.isOwner.toString(), r.isLocked.toString(),
        r.lockCode ?: "", r.pinnedMessage ?: "",
        r.imageUrl ?: "", r.backgroundImageUrl ?: ""
    ).joinToString("|")

    /**
     * يستعيد الملف الشخصي والمحفظة، ثم يراقبهما ويحفظهما تلقائياً عند أي تغيير.
     * المراقبة أفضل من إضافة حفظ بكل دالة تعديل: أي دالة جديدة تُضاف مستقبلاً
     * تنحفظ تلقائياً بلا ما نتذكر شي.
     */
    private fun restoreProfileAndWallet() {
                LocalStore.loadProfile()?.let { saved ->
            _userProfile.value = saved
            // ترحيل: الحسابات القديمة كان اليوزر فيها نفس الرقم — نولّد لها يوزراً حقيقياً
            val h = saved.handle.removePrefix("@")
            if (h.isBlank() || h.all { c -> c.isDigit() }) {
                _userProfile.update { it.copy(handle = generateUniqueHandle(it.name, it.email)) }
            }
        }
        LocalStore.loadWallet()?.let { snapshot ->
            _walletBalance.value = snapshot.balance
            _transactions.value = snapshot.transactions
        }

        viewModelScope.launch {
            _userProfile.collect { LocalStore.saveProfile(it) }
        }
        viewModelScope.launch {
            combine(_walletBalance, _transactions) { balance, transactions ->
                balance to transactions
            }.collect { (balance, transactions) ->
                LocalStore.saveWallet(balance, transactions)
            }
        }
    }

        /**
     * يحفظ منشورات المستخدم + سلة المحذوفات + مشاركاتي.
     * الكتابة مجمّعة وغير متزامنة داخل LocalStore فما تعطل الواجهة.
     */
        private fun persistMyPosts() {
        LocalStore.saveMyPosts(_posts.value.filter { it.isAuthor })
        LocalStore.saveDeletedPosts(_deletedPosts.value)
        LocalStore.saveSharedPostIds(_sharedPostIds.value)
    }

    // ─────────────── الحماية من الإساءة: معرّفات آمنة + حدود الإدخال ───────────────

    /** مولّد عشوائي آمن تشفيرياً (kotlin.random غير آمنة لهذه الاستخدامات). */
    private val secureRandom = java.security.SecureRandom()

    /**
     * معرّف محلي فريد: طابع زمني + 48 بت عشوائية آمنة.
     * (24 بت ما كانت تكفي: القياس الفعلي أعطى 5 تكرارات في 200 ألف معرّف بنفس الملي ثانية)
     */
    private fun newLocalId(prefix: String): String =
        prefix + System.currentTimeMillis() + "_" +
            java.lang.Long.toHexString(secureRandom.nextLong() and 0xFFFFFFFFFFFFL)

    /** حدود الإدخال والمعدل — تحمي الواجهة وميزانية قاعدة البيانات لاحقاً. */
    private object Limits {
        const val MAX_POST_CHARS = 2000
        const val MAX_COMMENT_CHARS = 500
        const val MIN_INTERVAL_MS = 1200L
        const val MIN_POST_INTERVAL_MS = 3000L
    }

    private val lastActionAt = mutableMapOf<String, Long>()

    /** يمنع تكرار العملية بسرعة (سبام). يرجع false لو لازم ينتظر. */
    private fun allowAction(key: String, minIntervalMs: Long = Limits.MIN_INTERVAL_MS): Boolean {
        val now = System.currentTimeMillis()
        val last = lastActionAt[key] ?: 0L
        if (now - last < minIntervalMs) return false
        lastActionAt[key] = now
        return true
    }

    /** يرقّي كلمة مرور الغرفة القديمة (نص صريح) إلى مشفّرة. يرجع القيمة كما هي لو مشفّرة أصلاً. */
    private fun upgradePasswordIfLegacy(stored: String?): String? {
        if (stored == null) return null
        if (PasswordHasher.isHashed(stored)) return stored
        return PasswordHasher.hash(stored)
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

        // --- Followers/Following State ---
    // قوائمي الحقيقية تُشتق من رسم المتابعة (_follows) وتُحدَّث لحظياً — التعريف أسفل _follows.

    // --- Wallet State ---
    private val _walletBalance = MutableStateFlow(0)
    val walletBalance: StateFlow<Int> = _walletBalance.asStateFlow()

    private val _dailyBonusClaimed = MutableStateFlow(false)
    val dailyBonusClaimed: StateFlow<Boolean> = _dailyBonusClaimed.asStateFlow()

    private val _walletFilter = MutableStateFlow("الكل") // الكل، كسب، إنفاق
    val walletFilter: StateFlow<String> = _walletFilter.asStateFlow()

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    private val _storeItems = MutableStateFlow<List<StoreItem>>(
        emptyList()
    )
    val storeItems: StateFlow<List<StoreItem>> = _storeItems.asStateFlow()

    // Profile State
    private val _userProfile = MutableStateFlow(UserProfile())
    private val lastVoiceSeatRequestTime = mutableMapOf<String, Long>()
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // كتلة تهيئة ثانية: لازم تكون هنا تحديداً وليس مع init الأولى بالسطر 143،
    // لأن _walletBalance و_transactions و_userProfile معرّفة بعدها وما تكون جاهزة هناك.
        init {
        restoreProfileAndWallet()
        // استعادة الجلسة **هنا تحديداً**: تعتمد على _userProfile المُعرَّف فوق بالسطر 453،
        // ولو نُفِّذت في كتلة التهيئة الأولى لانهار التطبيق (وكل الاختبارات) بـNullPointerException.
        restoreSession()
        }

    // Follow System (نظام مشابه لإنستغرام/تيك توك) — بالذاكرة مؤقتاً، جاهز للربط بـ Firestore لاحقاً
    // بيانات تجريبية مؤقتة لاختبار التنقل بين البروفايلات (mock_1 يتابع mock_2 و mock_3، mock_2 يتابع mock_3 و mock_4، mock_3 يتابع mock_4)
    private val _follows = MutableStateFlow<List<com.example.model.Follow>>(
        listOf(
            com.example.model.Follow(id = "f1", followerId = "10000001", followingId = "10000002"),
            com.example.model.Follow(id = "f2", followerId = "10000001", followingId = "10000003"),
            com.example.model.Follow(id = "f3", followerId = "10000002", followingId = "10000003"),
            com.example.model.Follow(id = "f4", followerId = "10000002", followingId = "10000004"),
            com.example.model.Follow(id = "f5", followerId = "10000003", followingId = "10000004")
        )
    )
        val follows: StateFlow<List<com.example.model.Follow>> = _follows.asStateFlow()

    // --- قوائمي الحقيقية (مشتقة من رسم المتابعة، لا بيانات ثابتة) ---
    private val _myFollowersList = MutableStateFlow<List<FollowUser>>(emptyList())
    val followersList: StateFlow<List<FollowUser>> = _myFollowersList.asStateFlow()

    private val _myFollowingList = MutableStateFlow<List<FollowUser>>(emptyList())
    val followingList: StateFlow<List<FollowUser>> = _myFollowingList.asStateFlow()

    // بذرة حسابي: أتابع «سارة أحمد» و«محمد العلي» يتابعني — منها تبدأ سلسلة التنقل A→B→C→D
    private val followSeedTargetId = "10000001"
    private val followSeedFollowerId = "10000002"

    /**
     * يزرع بداية السلسلة لحسابي مرة واحدة فقط: أنا → سارة أحمد → محمد العلي → نورة سالم → خالد فهد.
     * العلامة محفوظة بالجهاز (followSeedFor) ⇒ لو ألغيت متابعة سارة لاحقاً تبقى ملغاة ولا ترجع تلقائياً.
     */
    private fun ensureMyFollowSeed(myId: String) {
        if (myId.isBlank()) return
        if (LocalStore.loadFollowSeedFor() == myId) return
        val missed = ArrayList<com.example.model.Follow>(2)
        if (_follows.value.none { it.followerId == myId && it.followingId == followSeedTargetId }) {
            missed.add(
                com.example.model.Follow(
                    id = "seed_out_$myId", followerId = myId, followingId = followSeedTargetId
                )
            )
        }
        if (_follows.value.none { it.followerId == followSeedFollowerId && it.followingId == myId }) {
            missed.add(
                com.example.model.Follow(
                    id = "seed_in_$myId", followerId = followSeedFollowerId, followingId = myId
                )
            )
        }
        if (missed.isNotEmpty()) _follows.update { it + missed }
        LocalStore.saveFollowSeedFor(myId)
    }

    /**
     * يربط قوائمي وأعدادي برسم المتابعة لحظياً، ويحفظ أي تغيير محلياً
     * (فتبقى المتابعات بعد إغلاق التطبيق). لا يحتاج أي تعديل من الواجهة.
     */
        init {
        viewModelScope.launch {
            // ⚠️ إصلاح انهيار الإقلاع: هذا الـlaunch يُنفَّذ **داخل الباني فوراً**،
            // وcombine يُصدر قيمته الأولى فوراً ⇒ كان يصل إلى getFollowingOf قبل
            // تهيئة mockUsersDirectory (المعرَّف لاحقاً بالسطر 563) ⇒ NPE عند كل إعادة فتح.
            // yield = تأجيل خطوة واحدة ⇒ تنتهي تهيئة كل الحقول أولاً.
            kotlinx.coroutines.yield()

            LocalStore.loadFollows().takeIf { it.isNotEmpty() }?.let { _follows.value = it }
            launch { _follows.collect { list -> LocalStore.saveFollows(list) } }

            launch {
                combine(_follows, _userProfile) { follows, me -> follows to me.id }
                    .collect { (_, myId) ->
                        if (myId.isBlank()) return@collect
                        ensureMyFollowSeed(myId)
                        _myFollowingList.value = getFollowingOf(myId)
                        _myFollowersList.value = getFollowersOf(myId)
                        // نقرأ القيمة بعد الزرع (لا اللقطة القديمة) ⇒ الرقم يطابق القائمة من أول لحظة
                        val followingNow = _follows.value.count { it.followerId == myId }
                        val followersNow = _follows.value.count { it.followingId == myId }
                        _userProfile.update { p ->
                            if (p.followingCount == followingNow && p.followersCount == followersNow) p
                            else p.copy(followingCount = followingNow, followersCount = followersNow)
                        }
                    }
            }
        }
    }

    // عدد المتابعين/المتابَعين الحقيقي لأي مستخدم (من الرسم مباشرة — بلا حلقات ولا تكرار)
    private fun countFollowersOf(userId: String): Int =
        if (userId.isBlank()) 0 else _follows.value.count { it.followingId == userId }

    private fun countFollowingOf(userId: String): Int =
        if (userId.isBlank()) 0 else _follows.value.count { it.followerId == userId }

    // TODO: عند ربط Firestore، تُستبدل بقراءة استعلام من مجموعة "follows" حيث followingId == userId
    fun loadFollowers(userId: String): List<com.example.model.Follow> {
        return _follows.value.filter { it.followingId == userId }
    }

    // TODO: عند ربط Firestore، تُستبدل بقراءة استعلام من مجموعة "follows" حيث followerId == userId
    fun loadFollowing(userId: String): List<com.example.model.Follow> {
        return _follows.value.filter { it.followerId == userId }
    }

    // دليل مستخدمين وهميين للاختبار المحلي فقط (مؤقت، يُستبدل بـ Firestore لاحقاً)
        private val mockUsersDirectory: List<UserProfile> = listOf(
        UserProfile(id = "10000001", name = "سارة أحمد", handle = "@sara_a", bio = "أحب التصوير والسفر", avatarEmoji = "👩"),
        UserProfile(id = "10000002", name = "محمد العلي", handle = "@m_ali", bio = "مطور تطبيقات", avatarEmoji = "👨"),
        UserProfile(id = "10000003", name = "نورة سالم", handle = "@noura_s", bio = "طالبة جامعية", avatarEmoji = "👩‍🎓", privacyLevel = "خاص"),
        UserProfile(id = "10000004", name = "خالد فهد", handle = "@khalid_f", bio = "شغوف بالرياضة", avatarEmoji = "🏃")
    )

    // يرجع بروفايل أي مستخدم بالـID (أنا أو من الدليل الوهمي)
        fun getUserProfileById(userId: String): UserProfile? {
        val base = if (userId == _userProfile.value.id) _userProfile.value
            else mockUsersDirectory.find { it.id == userId } ?: return null
        // الأرقام تُحسب من رسم المتابعة الفعلي ⇒ تطابق القائمة التي تُفتح عند الضغط على البطاقة
        return base.copy(
            followersCount = countFollowersOf(userId),
            followingCount = countFollowingOf(userId)
        )
        }

    // يرجع منشورات أي مستخدم بالـID (تُستخدم لعرض منشورات أي بروفايل مفتوح + حساب العدد)
    fun getPostsByUserId(userId: String): List<Post> {
        return _posts.value.filter { it.authorId == userId }
    }

    // عدد منشورات وهمي ثابت لكل مستخدم بالدليل الوهمي (اختبار محلي فقط، يُستبدل بعدّ حقيقي عند ربط Firestore)
    private val mockPostsCountByUserId: Map<String, Int> = mapOf(
        "10000001" to 14,
        "10000002" to 27,
        "10000003" to 6,
        "10000004" to 41
    )

    // يرجع عدد منشورات أي مستخدم (أنا: من _posts الحقيقية، غيري: من الدليل الوهمي)
    fun getPostsCountByUserId(userId: String): Int {
        if (userId == _userProfile.value.id) {
            return _posts.value.count { it.authorId == userId }
        }
        return mockPostsCountByUserId[userId] ?: 0
    }

    // يرجع قائمة متابعي مستخدم معيّن (بأي id) كـ FollowUser جاهزة للعرض
        fun getFollowersOf(userId: String): List<FollowUser> {
        return loadFollowers(userId).mapNotNull { follow ->
            getUserProfileById(follow.followerId)?.let { profile ->
                profile.toFollowUser()
            }
        }
    }

    /** يحوّل البروفايل لعنصر قائمة متابعة بكل حقوله (لا بيانات ناقصة). */
    private fun UserProfile.toFollowUser(): FollowUser = FollowUser(
        id = id,
        name = name,
        handle = handle,
        avatarUrl = avatarUrl,
        bio = bio,
        avatarEmoji = avatarEmoji,
        followersCount = countFollowersOf(id),
        followingCount = countFollowingOf(id)
    )

    // يرجع قائمة من يتابعهم مستخدم معيّن (بأي id) كـ FollowUser جاهزة للعرض
        fun getFollowingOf(userId: String): List<FollowUser> {
        return loadFollowing(userId).mapNotNull { follow ->
            getUserProfileById(follow.followingId)?.let { profile ->
                profile.toFollowUser()
            }
        }
        }
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
        persistMyPosts()
    }

    fun openComments(postId: String) {
        _activeCommentPostId.value = postId
    }

    fun closeComments() {
        _activeCommentPostId.value = null
    }

        fun addComment(postId: String, commentText: String) {
        if (commentText.isBlank()) return
        if (commentText.length > Limits.MAX_COMMENT_CHARS) {
            _userMessage.value = "التعليق طويل جداً — الحد الأقصى ${Limits.MAX_COMMENT_CHARS} حرف"
            return
        }
        if (!allowAction("comment")) {
            _userMessage.value = "تمهل قليلاً قبل إضافة تعليق آخر ⏳"
            return
        }
        val newComment = PostComment(
            id = newLocalId("c_"),
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
        persistMyPosts()
        _userMessage.value = "تمت إضافة تعليقك بنجاح!"
    }

                fun sharePost(post: Post) {
            _posts.update { list ->
            list.map { if (it.id == post.id) it.copy(sharesCount = it.sharesCount + 1) else it }
        }
        // نسجّل مشاركتي مرة واحدة فقط (بلا تكرار) والأحدث أولاً، بحد أقصى 50
        _sharedPostIds.update { ids ->
            (listOf(post.id) + ids.filter { it != post.id }).take(50)
        }
        persistMyPosts()
        _userMessage.value = "تم نسخ رابط المنشور ومشاركته مع الأصدقاء!"
                }

        fun publishPost(content: String, tag: String, mediaType: PostMediaType, mediaUri: String = "") {
        if (content.isBlank() && mediaType == PostMediaType.NONE) return
        if (content.length > Limits.MAX_POST_CHARS) {
            _userMessage.value = "النص طويل جداً — الحد الأقصى ${Limits.MAX_POST_CHARS} حرف"
            return
        }
        if (!allowAction("publish", Limits.MIN_POST_INTERVAL_MS)) {
            _userMessage.value = "تمهل قليلاً قبل نشر منشور آخر ⏳"
            return
        }
        val profile = _userProfile.value
        val newPost = Post(
            id = newLocalId("p_"),
            authorId = profile.id,
            authorName = profile.name,
            authorHandle = profile.handle.ifBlank { "ID: ${profile.id}" },
            authorAvatarUrl = profile.avatarUrl,
            timeAgo = "الآن",
            content = content.trim(),
            mediaType = mediaType,
            mediaUri = mediaUri,
            mediaCaption = null,
            tag = if (tag.isNotBlank()) if (tag.startsWith("#")) tag else "#$tag" else null,
            likesCount = 0,
            isLiked = false,
            commentsCount = 0,
            sharesCount = 0,
            commentsList = emptyList(),
            isAuthor = true,
            isFollowing = false
        )
                _posts.update { listOf(newPost) + it }
        persistMyPosts()
        _userMessage.value = "تم نشر منشورك بنجاح في خلاصة المجتمع!"
        addNotification(NotificationType.SYSTEM, "تم نشر منشورك بنجاح")
    }

                fun deletePost(postId: String) {
        val target = _posts.value.find { it.id == postId }
        // ما نحذف ملف الوسائط: المنشور ينتقل لسلة «المحتوى المحذوف» ويبقى ملفه للمعاينة.
        // الملفات اليتيمة (خارج آخر 50 محذوفاً) تُنظَّف تلقائياً عند الإقلاع.
        if (target != null) {
            _deletedPosts.update { list ->
                (listOf(target) + list.filter { it.id != postId }).take(50)
            }
        }
                _posts.update { list -> list.filter { it.id != postId } }
        persistMyPosts()
        _userMessage.value = "تم حذف المنشور — تجده في «المحتوى المحذوف» 📋"
                }

    fun editPost(postId: String, newContent: String) {
        if (newContent.isBlank()) return
                _posts.update { list ->
            list.map { post ->
                if (post.id == postId) post.copy(content = newContent.trim()) else post
            }
        }
        persistMyPosts()
        _userMessage.value = "تم تعديل المنشور بنجاح"
    }

    fun reportPost(postId: String, reason: String = "محتوى غير لائق") {
        _userMessage.value = "تم استلام البلاغ وسيتم مراجعته من قِبل المشرفين، شكراً لمساعدتك في حماية المجتمع"
    }

    fun togglePinConversation(conversationId: String) {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) {
                    conv.copy(isPinned = !conv.isPinned)
                } else conv
            }
        }
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
            mediaUri = mediaUri,
            authorAvatarUrl = _userProfile.value.avatarUrl
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

    fun sendMessage(conversationId: String, text: String, type: ChatMessageType = ChatMessageType.TEXT, imageUri: String = "") {
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
            gameReward = if (type == ChatMessageType.GAME_INVITE) 100 else 0,
            imageUri = imageUri
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

        fun startInChatGame(conversationId: String, gameType: GameType = GameType.DOMINO) {
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
                if (!allowAction("create_room", 2000L)) {
            _userMessage.value = "تمهل قليلاً قبل إنشاء غرفة جديدة ⏳"
            return
        }
        val existingIds = _chatRooms.value.map { it.id }.toSet()
        // SecureRandom بدل kotlin.random (غير آمنة تشفيرياً) — ونفس نطاق الثمانية أرقام
        var generatedId: String
        do {
            generatedId = (55_000_000 + secureRandom.nextInt(45_000_000)).toString()
        } while (existingIds.contains(generatedId))

        val newRoom = ChatRoom(
            id = generatedId,
            name = name.trim(),
            description = description.trim(),
            category = category,
            iconEmoji = iconEmoji,
            imageUrl = imageUrl,
            accessType = accessType,
                                                // تُخزَّن مشفّرة (PBKDF2 + salt) لا كنص صريح
                        password = if (accessType == RoomAccessType.PASSWORD && !password.isNullOrBlank())
                            PasswordHasher.hash(password.trim()) else null,
            memberCount = 1,
            maxMembers = maxMembers.coerceIn(10, 1000),
            isJoined = true,
            isOwner = true,
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
            if (room.lockCode.isNullOrBlank()) {
                _userMessage.value = "هذه الغرفة مقفلة حاليا ولا تقبل أعضاء جدد."
                return false
            }
            if (room.lockCode != passwordInput.trim()) {
                return false
            }
        }

                if (room.accessType == RoomAccessType.PASSWORD &&
            !PasswordHasher.verifyOrLegacy(passwordInput.trim(), room.password)
        ) {
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
                        // ترقية تلقائية: كلمة مرور قديمة نصية ⇒ مشفّرة (بلا تدخل المستخدم)
                        password = upgradePasswordIfLegacy(it.password),
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
    fun lockRoomWithCode(roomId: String, code: String) {
        val clean = code.trim()
        if (clean.length != 4 || !clean.all { it.isDigit() }) return
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) it.copy(isLocked = true, lockCode = clean) else it
            }
        }
    }

    fun unlockRoom(roomId: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) it.copy(isLocked = false, lockCode = null) else it
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

    fun updateRoomImage(roomId: String, imageUrl: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) it.copy(imageUrl = imageUrl.ifBlank { null }) else it
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

    fun deleteConversation(conversationId: String) {
        _conversations.update { list ->
            list.filter { it.id != conversationId }
        }
        _userMessage.value = "تم حذف المحادثة."
    }

    fun blockConversationUser(conversationId: String) {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) conv.copy(isBlocked = true) else conv
            }
        }
        _userMessage.value = "تم الحظر."
    }

    fun unblockConversationUser(conversationId: String) {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) conv.copy(isBlocked = false) else conv
            }
        }
        _userMessage.value = "تم إلغاء الحظر."
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
        val alreadySeated = room.ownerVoiceSeat.occupantId == "me" || room.voiceSeats.any { it.occupantId == "me" }
        if (alreadySeated) {
            _userMessage.value = "أنت بالفعل على مايك، انزل منه أولاً."
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

    fun requestJoinWheel(roomId: String) {
        val room = _chatRooms.value.find { it.id == roomId } ?: return
        val me = room.members.find { it.id == "me" }
        if (_walletBalance.value < 100) {
            _userMessage.value = "رصيد نقاطك غير كافٍ للانضمام إلى عجلة الحظ!"
            return
        }
        if (room.wheelParticipants.any { it.id == "me" }) {
            _userMessage.value = "أنت مشارك بالفعل بعجلة الحظ."
            return
        }
        if (room.wheelJoinRequests.any { it.requesterId == "me" }) {
            _userMessage.value = "لديك طلب معلّق بالفعل بانتظار موافقة المالك."
            return
        }
        if (room.wheelParticipants.size >= 4) {
            _userMessage.value = "اكتمل عدد المشاركين بعجلة الحظ."
            return
        }
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(
                        wheelJoinRequests = it.wheelJoinRequests + WheelJoinRequest(
                            id = "wjr_${System.currentTimeMillis()}",
                            requesterId = "me",
                            requesterName = me?.name ?: "أنت",
                            requesterAvatarUrl = _userProfile.value.avatarUrl
                        )
                    )
                } else it
            }
        }
        _userMessage.value = "تم إرسال طلب الانضمام، بانتظار موافقة المالك."
    }

    fun respondToWheelRequest(roomId: String, requestId: String, accept: Boolean) {
        val room = _chatRooms.value.find { it.id == roomId } ?: return
        val request = room.wheelJoinRequests.find { it.id == requestId } ?: return
        if (accept && room.wheelParticipants.size >= 4) {
            _userMessage.value = "اكتمل عدد المشاركين بعجلة الحظ."
            _chatRooms.update { list ->
                list.map {
                    if (it.id == roomId) it.copy(wheelJoinRequests = it.wheelJoinRequests.filter { r -> r.id != requestId })
                    else it
                }
            }
            return
        }
        if (accept) {
            if (request.requesterId == "me" && _walletBalance.value < 100) {
                _userMessage.value = "رصيد النقاط غير كافٍ لإتمام الانضمام."
                _chatRooms.update { list ->
                    list.map {
                        if (it.id == roomId) it.copy(wheelJoinRequests = it.wheelJoinRequests.filter { r -> r.id != requestId })
                        else it
                    }
                }
                return
            }
            if (request.requesterId == "me") {
                _walletBalance.update { it - 100 }
                val newTx = WalletTransaction(
                    id = "tx_${System.currentTimeMillis()}",
                    title = "الانضمام لعجلة الحظ",
                    type = TransactionType.SPEND,
                    points = 100,
                    date = "اليوم",
                    note = "دخول عجلة الحظ داخل الغرفة"
                )
                _transactions.update { listOf(newTx) + it }
            }
        }
        _chatRooms.update { list ->
            list.map { r ->
                if (r.id == roomId) {
                    if (accept) {
                        r.copy(
                            wheelParticipants = r.wheelParticipants + WheelParticipant(
                                id = request.requesterId,
                                name = request.requesterName,
                                avatarUrl = request.requesterAvatarUrl
                            ),
                            wheelJoinRequests = r.wheelJoinRequests.filter { it.id != requestId },
                            wheelPrizePool = r.wheelPrizePool + 100
                        )
                    } else {
                        r.copy(wheelJoinRequests = r.wheelJoinRequests.filter { it.id != requestId })
                    }
                } else r
            }
        }
        _userMessage.value = if (accept) "تم قبول الانضمام لعجلة الحظ." else "تم رفض طلب الانضمام."
    }
    fun startWheelSpin(roomId: String) {
        val room = _chatRooms.value.find { it.id == roomId } ?: return
        if (room.wheelParticipants.size < 4) {
            _userMessage.value = "يلزم 4 مشاركين على الأقل لتدوير العجلة."
            return
        }
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) it.copy(
                    isWheelSpinning = true,
                    wheelEliminatedIds = emptyList(),
                    wheelWinnerId = null,
                    wheelWinnerName = "",
                    wheelWinnerAvatarUrl = "",
                    wheelTargetRotation = 0f
                ) else it
            }
        }
        viewModelScope.launch {
            var cumulativeRotation = 0f
            while (true) {
                val currentRoom = _chatRooms.value.find { it.id == roomId } ?: break
                if (!currentRoom.isWheelSpinning) break
                val remaining = currentRoom.wheelParticipants.filterNot { it.id in currentRoom.wheelEliminatedIds }
                if (remaining.size <= 1) {
                    val winner = remaining.firstOrNull()
                    if (winner != null) {
                        val totalPrize = currentRoom.wheelPrizePool
                        val winnerShare = (totalPrize * 75) / 100
                        if (winner.id == "me") {
                            _walletBalance.update { it + winnerShare }
                            val newTx = WalletTransaction(
                                id = "tx_${System.currentTimeMillis()}",
                                title = "الفوز بعجلة الحظ 🎉",
                                type = TransactionType.EARN,
                                points = winnerShare,
                                date = "اليوم",
                                note = "جائزة عجلة الحظ داخل الغرفة"
                            )
                            _transactions.update { listOf(newTx) + it }
                        }
                        _chatRooms.update { list ->
                            list.map {
                                if (it.id == roomId) it.copy(
                                    isWheelSpinning = false,
                                    wheelWinnerId = winner.id,
                                    wheelWinnerName = winner.name,
                                    wheelWinnerAvatarUrl = winner.avatarUrl
                                ) else it
                            }
                        }
                        _userMessage.value = "${winner.name} فاز بعجلة الحظ! 🎉"
                    } else {
                        _chatRooms.update { list ->
                            list.map { if (it.id == roomId) it.copy(isWheelSpinning = false) else it }
                        }
                    }
                    break
                } else {
                    val sectorAngle = 90f
val eliminated = remaining.random()
val eliminatedIndex = currentRoom.wheelParticipants.indexOfFirst { it.id == eliminated.id }
val stopAngle = (360f - (eliminatedIndex * sectorAngle)) % 360f
                    val fullSpins = (4..6).random()
                    val currentAngle = cumulativeRotation % 360f
                    val deltaToStop = (stopAngle - currentAngle + 360f) % 360f
                    cumulativeRotation += (fullSpins * 360f) + deltaToStop
                    _chatRooms.update { list ->
                        list.map {
                            if (it.id == roomId) it.copy(wheelTargetRotation = cumulativeRotation)
                            else it
                        }
                    }
                    delay(4500)
                    _chatRooms.update { list ->
                        list.map {
                            if (it.id == roomId) it.copy(
                                wheelEliminatedIds = it.wheelEliminatedIds + eliminated.id
                            ) else it
                        }
                    }
                }
            }
        }
    }
    fun resetWheel(roomId: String) {
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) it.copy(
                    wheelParticipants = emptyList(),
                    wheelJoinRequests = emptyList(),
                    isWheelSpinning = false
                )
                else it
            }
        }
    }

    
    fun takeVoiceSeatDirectly(roomId: String, seatNumber: Int) {
        val room = _chatRooms.value.find { it.id == roomId } ?: return
        val me = room.members.find { it.id == "me" }
        val isPrivileged = room.isOwner || me?.role == RoomMemberRole.ADMIN
        if (!isPrivileged) {
            _userMessage.value = "هذي الصلاحية للمالك والمشرف فقط."
            return
        }
        val alreadySeated = room.ownerVoiceSeat.occupantId == "me" || room.voiceSeats.any { it.occupantId == "me" }
        if (alreadySeated) {
            _userMessage.value = "انزل من مقعدك الحالي أولاً قبل الصعود على مقعد آخر."
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
        val alreadySeated = room.voiceSeats.any { it.occupantId == "me" }
        if (alreadySeated) {
            _userMessage.value = "انزل من مقعدك الحالي أولاً قبل الصعود على مقعد آخر."
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

    fun inviteMemberToOwnerSeat(roomId: String, memberId: String) {
        val room = _chatRooms.value.find { it.id == roomId } ?: return
        val me = room.members.find { it.id == "me" }
        val isPrivileged = room.isOwner || me?.role == RoomMemberRole.ADMIN
        if (!isPrivileged) {
            _userMessage.value = "الدعوة للمالك والمشرف فقط."
            return
        }
        if (memberId == "me") return
        val member = room.members.find { it.id == memberId }
        if (member == null) {
            _userMessage.value = "العضو غير موجود بالغرفة."
            return
        }
        val occupant = room.ownerVoiceSeat.occupantId
        if (!occupant.isNullOrBlank() && occupant != "me") {
            _userMessage.value = "المقعد مشغول بعضو، أنزله أولاً."
            return
        }
        if (room.voiceSeats.any { it.occupantId == memberId }) {
            _userMessage.value = "هذا العضو جالس بمقعد آخر."
            return
        }
        _chatRooms.update { list ->
            list.map {
                if (it.id == roomId) {
                    it.copy(
                        ownerVoiceSeat = it.ownerVoiceSeat.copy(
                            occupantId = member.id,
                            occupantName = member.name,
                            occupantAvatarUrl = member.avatarUrl,
                            isMuted = false
                        ),
                        voiceSeatRequests = it.voiceSeatRequests.filter { r -> r.requesterId != memberId }
                    )
                } else it
            }
        }
        _userMessage.value = "تمت دعوة ${member.name} للصعود على مايكك."
    }

    fun removeMemberFromOwnerSeat(roomId: String) {
        val room = _chatRooms.value.find { it.id == roomId } ?: return
        if (!room.isOwner) {
            _userMessage.value = "الإنزال لمالك الغرفة فقط."
            return
        }
        val occupant = room.ownerVoiceSeat.occupantId
        if (occupant.isNullOrBlank() || occupant == "me") {
            _userMessage.value = "لا يوجد عضو على مقعدك."
            return
        }
        leaveOwnerVoiceSeat(roomId)
        _userMessage.value = "تم إنزال العضو من مقعدك."
    }

    fun leaveOwnerVoiceSeat(roomId: String) {
        _chatRooms.update { list ->
            list.map { room ->
                if (room.id == roomId) {
                    room.copy(
                        ownerVoiceSeat = room.ownerVoiceSeat.copy(
                            occupantId = null,
                            occupantName = null,
                            occupantAvatarUrl = null,
                            isMuted = false
                        )
                    )
                } else room
            }
        }
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

        fun startInRoomGame(roomId: String, gameType: GameType = GameType.DOMINO) {
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
    
        /** يرجّع true فقط لو وُجد المستخدم فعلاً وأُرسل الطلب. */
    fun sendFriendRequest(friendInput: String): Boolean {
        val id = friendInput.trim()
        if (id.isBlank()) {
            _userMessage.value = "يرجى كتابة معرّف المستخدم الرقمي!"
            return false
        }
        if (id == _userProfile.value.id) {
            _userMessage.value = "لا يمكنك إرسال طلب صداقة لنفسك"
            return false
        }
        val target = getUserProfileById(id)
        if (target == null) {
            _userMessage.value = "لا يوجد مستخدم بهذا المعرّف"
            return false
        }
        _userMessage.value = "تم إرسال طلب الصداقة إلى ${target.name} بنجاح!"
        return true
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

    fun spendOnGift(giftName: String, giftPrice: Int, quantity: Int): Boolean {
        val totalCost = giftPrice * quantity
        if (_walletBalance.value < totalCost) {
            _userMessage.value = "رصيد نقاطك غير كافٍ لإرسال هذه الهدية!"
            return false
        }
        _walletBalance.update { it - totalCost }
        val newGiftTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            title = "إرسال هدية: $giftName",
            type = TransactionType.SPEND,
            points = totalCost,
            date = "اليوم",
            note = "هدية داخل الغرفة (الكمية: $quantity)"
        )
        _transactions.update { listOf(newGiftTx) + it }
            return true
    }

        // --- Profile Actions ---
    fun updateUserBio(newBio: String) {
        _userProfile.update { it.copy(bio = newBio.trim()) }
        syncUserProfile()
        _userMessage.value = "تم تحديث النبذة التعريفية بنجاح!"
    }

        fun updateUserHandle(newHandle: String) {
        val cleaned = newHandle.trim()
            .removePrefix("@")
            .lowercase()
            .filter { c -> c in 'a'..'z' || c in '0'..'9' || c == '_' }
            .take(20)
        if (cleaned.isBlank()) {
            _userMessage.value = "اسم المستخدم يجب أن يحتوي أحرفاً إنجليزية أو أرقاماً"
            return
        }
        _userProfile.update { it.copy(handle = "@$cleaned") }
        syncUserProfile()
        _userMessage.value = "تم تحديث اسم المستخدم بنجاح!"
    }

    fun updateUserProfile(name: String, bio: String, emoji: String) {
        _userProfile.update {
            it.copy(
                name = name.trim(),
                bio = bio.trim(),
                avatarEmoji = emoji
            )
        }
        syncUserProfile()
        _userMessage.value = "تم حفظ معلومات الملف الشخصي بنجاح!"
    }

    fun updateUserAvatarUrl(url: String) {
        _userProfile.update { it.copy(avatarUrl = url) }
        syncUserProfile()
        _userMessage.value = "تم تحديث صورة الملف الشخصي بنجاح!"
    }

    fun toggleProfileNotifications() {
        _userProfile.update {
            it.copy(isNotificationsEnabled = !it.isNotificationsEnabled)
        }
        syncUserProfile()
        val isEnabled = _userProfile.value.isNotificationsEnabled
        _userMessage.value = if (isEnabled) "تم تفعيل إشعارات الحساب" else "تم إيقاف إشعارات الحساب"
    }

    fun toggleProfilePrivacy() {
        _userProfile.update {
            val nextPrivacy = if (it.privacyLevel == "عام للجميع") "خاص" else "عام للجميع"
            it.copy(privacyLevel = nextPrivacy)
        }
        syncUserProfile()
        _userMessage.value = "تم تحديث خصوصية الحساب إلى (${_userProfile.value.privacyLevel})"
    }

        /**
     * يولّد اسم مستخدم إنجليزي من البريد (أو الاسم إن تعذّر)، بأحرف a-z وأرقام و_ فقط.
     * ملاحظة: التفرّد الحقيقي يحتاج خادماً — حالياً نفحص مقابل المعروف محلياً فقط.
     */
    fun generateUniqueHandle(name: String, email: String): String {
        val source = email.substringBefore("@").ifBlank { name }
        val base = source
            .lowercase()
            .map { if (it in 'a'..'z' || it in '0'..'9') it else '_' }
            .joinToString("")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(15)
            .ifBlank { "user" }

        val taken = _posts.value.map { it.authorHandle }.toSet() + _userProfile.value.handle
        var candidate = "@$base"
        var attempts = 0
        while (taken.contains(candidate) && attempts < 50) {
            candidate = "@$base${(100..9999).random()}"
            attempts++
        }
        return candidate
    }
    // ═══════════════ المصادقة السحابية (Firebase Auth) — تُستدعى من شاشة الدخول ═══════════════
    // onResult(نجح؟, رسالة): الرسالة تظهر للمستخدم، وnull عند النجاح بلا تنبيه.

    /** رسالة خطأ مفهومة من نتيجة خدمة الهوية (أو null عند النجاح). */
    private fun authErrorMessage(result: AuthService.Result): String? = when (result) {
        is AuthService.Result.Ok -> null
        is AuthService.Result.Error -> result.message
        AuthService.Result.NotConfigured -> "الخدمة غير مهيأة — تحقق من إعدادات المشروع."
    }

    /** يكمل الدخول بعد نجاح Firebase: يضبط الهوية ويسجّل الجلسة (نفس مسار الدخول الحالي). */
    private fun completeServerLogin(email: String, password: String, uid: String, fallbackName: String) {
        val name = fallbackName.trim().ifBlank { email.substringBefore('@') }
        onAuthSuccess(
            AuthUserAccount(
                name = name,
                email = email.trim(),
                password = password,
                avatarEmoji = "👤"
            ),
            uid
        )
    }

    /** **تسجيل الدخول** بحساب موجود (لا يُنشئ حساباً جديداً — رسالة واضحة لو غير موجود). */
    fun loginWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { AuthService.signIn(email, password) }
            if (result is AuthService.Result.Ok) {
                completeServerLogin(email, password, result.uid, AuthService.displayNameOfCurrentUser())
                val notVerified = withContext(Dispatchers.IO) { !AuthService.isEmailVerified() }
                onResult(
                    true,
                    if (notVerified) "تنبيه: بريدك لم يؤكَّد بعد — افتح بريدك واضغط رابط التأكيد." else null
                )
            } else {
                onResult(false, authErrorMessage(result))
            }
        }
    }

    /** **إنشاء حساب جديد**: ينشئ الحساب ويرسل رابط تأكيد البريد — والدخول يكتمل بعد التأكيد. */
    fun signUpWithEmail(name: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { AuthService.signUp(email, password) }
            if (result is AuthService.Result.Ok) {
                withContext(Dispatchers.IO) {
                    AuthService.setDisplayName(name)
                    AuthService.sendVerificationEmail()
                }
                onResult(true, null)
            } else {
                onResult(false, authErrorMessage(result))
            }
        }
    }

        /**
     * زر «تحققت من بريدي»: يعيد تحميل حالة الحساب من الخادم ثم يفحص التأكيد.
     * عند التأكيد (أو عند «أكمل لاحقاً») يكتمل الدخول ويدخل المستخدم التطبيق.
     */
    fun confirmSignupEmail(
        email: String,
        password: String,
        name: String,
        skipVerification: Boolean,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val verified = skipVerification || withContext(Dispatchers.IO) { AuthService.refreshEmailVerified() }
            if (verified) {
                val uid = withContext(Dispatchers.IO) { AuthService.currentUid() }
                if (uid.isNullOrBlank()) {
                    onResult(false, "انتهت الجلسة — أنشئ الحساب من جديد.")
                } else {
                    completeServerLogin(email, password, uid, name)
                    onResult(
                        true,
                        if (skipVerification) "تم إنشاء الحساب. يمكنك تأكيد بريدك لاحقاً من رسالة التأكيد." else null
                    )
                }
            } else {
                onResult(false, "لم نتحقق من بريدك بعد — افتح الرسالة واضغط الرابط ثم أعد المحاولة.")
            }
        }
    }

    /** يعيد إرسال رابط تأكيد البريد (للمستخدم الذي لم تصل له الرسالة). */
    fun resendVerificationEmail(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val sent = withContext(Dispatchers.IO) { AuthService.sendVerificationEmail() }
            onResult(sent, if (sent) "أُرسل رابط التأكيد إلى بريدك ✅" else "تعذّر الإرسال — تحقق من الاتصال.")
        }
    }

    /** **نسيت كلمة السر**: يرسل رابط إعادة التعيين إلى البريد (رسالة من Firebase). */
    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { AuthService.sendPasswordReset(email) }
            if (result is AuthService.Result.Ok) {
                onResult(true, "أُرسل رابط إعادة تعيين كلمة السر إلى بريدك ✅")
            } else {
                onResult(false, authErrorMessage(result))
            }
        }
    }
    
    fun onAuthSuccess(account: AuthUserAccount, generatedId: String) {
        val newHandle = generateUniqueHandle(account.name, account.email)
        _userProfile.update { current ->
            current.copy(
                id = generatedId,
                handle = newHandle,
                name = account.name,
                email = account.email,
                authProvider = "بريد إلكتروني",
                avatarEmoji = account.avatarEmoji,
                                // BuildConfig.APPLICATION_ID بدل معرّف ثابت: يشتغل في النسختين
                // (نسخة التعديلات معرّفها ينتهي بـ.edits — بالثابت كان الأفاتار لا يُحمَّل فيها)
                avatarUrl = current.avatarUrl.ifBlank { "android.resource://${BuildConfig.APPLICATION_ID}/drawable/default_avatar" }
            )
        }
                _isLoggedIn.value = true
        // الجلسة تُحفظ مشفّرة ⇒ إغلاق التطبيق لا يعني تسجيل دخول من جديد
        SecureSessionStore.save(userId = generatedId, provider = "بريد إلكتروني")
        // هوية الخادم: لا تُنفَّذ قبل تفعيل AuthService (بلا مفتاح ⇒ صفر اتصال شبكة)
        syncIdentityWithServer(account)
        _userMessage.value = "مرحباً بك ${account.name}! اسم المستخدم الخاص بك: $newHandle"
    
    }

        fun logoutUser() {
                SecureSessionStore.clear()
        // ☁️ إنهاء الجلسة السحابية أيضاً (وإلا بقي الحساب مسجّلاً في Firebase)
        AuthService.signOut()
        _isLoggedIn.value = false
        _userMessage.value = "تم تسجيل الخروج بنجاح. مرحباً بك في أي وقت!"
    }

    // --- Follow System (نظام مشابه لإنستغرام/تيك توك) ---
    // TODO: عند ربط Firestore، تُستبدل بإضافة مستند جديد لمجموعة "follows"
        fun followUser(targetUserId: String, forceDirect: Boolean = false) {
        val myId = _userProfile.value.id
        if (myId == targetUserId || myId.isBlank()) return
        val alreadyFollowing = _follows.value.any { it.followerId == myId && it.followingId == targetUserId }
        if (alreadyFollowing) return

        // حساب خاص: المتابعة تحتاج موافقة ⇒ تُسجَّل كطلب بدل متابعة فورية (سلوك إنستغرام)
        if (!forceDirect && isPrivateAccount(targetUserId)) {
            if (isFollowRequestPending(targetUserId)) {
                _userMessage.value = "طلب المتابعة مُرسل مسبقاً — بانتظار الموافقة ⏳"
                return
            }
            _pendingFollowRequests.update { it + targetUserId }
            _userMessage.value = "تم إرسال طلب المتابعة — بانتظار موافقة صاحب الحساب ⏳"
            return
        }

                val newFollow = com.example.model.Follow(followerId = myId, followingId = targetUserId)
        _follows.update { it + newFollow }
        // ☁️ مزامنة المتابعة سحابياً
        viewModelScope.launch(Dispatchers.IO) { CloudStore.addFollow(newFollow) }
        _userProfile.update { it.copy(followingCount = it.followingCount + 1) }
        _userMessage.value = "تمت المتابعة بنجاح"
        }

    // TODO: عند ربط Firestore، تُستبدل بحذف المستند المطابق من مجموعة "follows"
    fun unfollowUser(targetUserId: String) {
        val myId = _userProfile.value.id
        val wasFollowing = _follows.value.any { it.followerId == myId && it.followingId == targetUserId }
        if (!wasFollowing) return

                _follows.update { list -> list.filterNot { it.followerId == myId && it.followingId == targetUserId } }
        _pendingFollowRequests.update { list -> list.filterNot { it == targetUserId } }
        _userProfile.update { it.copy(followingCount = (it.followingCount - 1).coerceAtLeast(0)) }
        _userMessage.value = "تم إلغاء المتابعة"
    }

        /** طلبات المتابعة المُرسلة لحسابات خاصة — بانتظار الموافقة. */
    private val _pendingFollowRequests = MutableStateFlow<List<String>>(emptyList())
    val pendingFollowRequests: StateFlow<List<String>> = _pendingFollowRequests.asStateFlow()

    /** هل الحساب خاص؟ (الحسابات الخاصة تحتاج موافقة قبل ظهور المحتوى) */
    fun isPrivateAccount(userId: String): Boolean =
        getUserProfileById(userId)?.privacyLevel == "خاص"

    /** هل أرسلت طلب متابعة لهذا الحساب وما زال معلّقاً؟ */
    fun isFollowRequestPending(userId: String): Boolean =
        _pendingFollowRequests.value.contains(userId)

    /** يوافق صاحب الحساب الخاص على الطلب — يجعل المتابعة فعلية. */
    fun approveFollowRequest(requesterId: String) {
        _pendingFollowRequests.update { list -> list.filterNot { it == requesterId } }
        followUser(requesterId, forceDirect = true)
    }

    // يفحص هل "أنا" أتابع هذا المستخدم فعلياً حالياً
    fun isFollowing(targetUserId: String): Boolean {
        val myId = _userProfile.value.id
        return _follows.value.any { it.followerId == myId && it.followingId == targetUserId }
    }

    // يبدّل حالة المتابعة (متابعة/إلغاء متابعة) لمستخدم معيّن — تُستخدم من زر المتابعة بالمنشورات
            fun toggleFollow(targetUserId: String) {
        if (!allowAction("toggle_follow", 800L)) {
            _userMessage.value = "تمهل قليلاً ⏳"
            return
        }
        if (isFollowing(targetUserId)) {
            unfollowUser(targetUserId)
        } else if (isFollowRequestPending(targetUserId)) {
            _pendingFollowRequests.update { list -> list.filterNot { it == targetUserId } }
            _userMessage.value = "تم إلغاء طلب المتابعة"
        } else {
            followUser(targetUserId)
        }
        }

    // TODO: عند ربط Firestore، تُستبدل بكتابة (set/update) بيانات _userProfile.value الحالية
    // بمستند المستخدم بمجموعة "users". تُستدعى بنهاية أي دالة تعدّل بيانات البروفايل.
    private fun syncUserProfile() {
        // placeholder — لا يوجد Firestore مربوط حالياً
    }
}
