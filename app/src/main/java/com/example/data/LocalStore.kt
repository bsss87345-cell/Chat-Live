package com.example.data

import android.content.Context
import com.example.model.ChatRoom
import com.example.model.Follow
import com.example.model.Post
import com.example.model.RoomAccessType
import com.example.model.PostComment
import com.example.model.PostMediaType
import com.example.model.TransactionType
import com.example.model.UserProfile
import com.example.model.WalletTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * لقطة المحفظة (الرصيد + العمليات) — نوع مساعد لإرجاع القيمتين معاً.
 */
data class WalletSnapshot(
    val balance: Int,
    val transactions: List<WalletTransaction>
)

/**
 * التخزين المحلي للتطبيق.
 *
 * ‼️ قاعدة مهمة: هذي الواجهة **محايدة عن طريقة التخزين**.
 * لا تستخدم JSONObject ولا الملفات خارج هذا الملف إطلاقاً.
 * يوم نبدل لـRoom أو Firestore نغيّر داخل هذا الملف فقط، وما نلمس أي سطر بالـViewModel.
 *
 * ‼️ تحذير صيانة: إذا أضفت حقلاً جديداً لـPost أو UserProfile أو WalletTransaction
 * **لازم تضيفه يدوياً** بدالتي التحويل (toJson / fromJson) بالأسفل،
 * وإلا ينحفظ ناقصاً بصمت بلا أي خطأ بناء.
 *
 * السلوك: كل الكتابات **مجمّعة وغير متزامنة** (debounce 300ms على خيط IO)،
 * فلو ضغط المستخدم 20 إعجاب بثانية ينكتب الملف مرة وحدة مو 20 مرة.
 */
object LocalStore {

    private const val FILE_NAME = "local_store.json"
    private const val SCHEMA_VERSION = 1
    private const val WRITE_DEBOUNCE_MS = 300L
    private const val POST_FILE_PREFIX = "post_"

    private const val KEY_VERSION = "schemaVersion"
    private const val KEY_POSTS = "myPosts"
    private const val KEY_DELETED_POSTS = "deletedPosts"
    private const val KEY_SHARED_POSTS = "sharedPostIds"
    private const val KEY_PROFILE = "profile"
    private const val KEY_WALLET_BALANCE = "walletBalance"
    private const val KEY_WALLET_TX = "walletTransactions"
    private const val KEY_ROOMS = "chatRooms"
    private const val KEY_FOLLOWS = "follows"
    private const val KEY_FOLLOW_SEED = "followSeedFor"

    @Volatile
    private var appContext: Context? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var writeJob: Job? = null
    private var doc: JSONObject? = null

    /** تُستدعى مرة وحدة عند إقلاع التطبيق. بدونها كل الدوال تعمل بلا تأثير (آمنة بالاختبارات). */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun isReady(): Boolean = appContext != null

    private fun storeFile(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, FILE_NAME)
    }

    @Synchronized
    private fun document(): JSONObject {
        doc?.let { return it }
        val loaded = try {
            val f = storeFile()
            if (f != null && f.exists() && f.length() > 0L) {
                JSONObject(f.readText())
            } else {
                JSONObject().put(KEY_VERSION, SCHEMA_VERSION)
            }
        } catch (e: Exception) {
            // ملف تالف أو مقطوع: نبدأ من جديد بدل ما ينهار التطبيق
            JSONObject().put(KEY_VERSION, SCHEMA_VERSION)
        }
        doc = loaded
        return loaded
    }

    private fun scheduleWrite() {
        if (!isReady()) return
        writeJob?.cancel()
        writeJob = scope.launch {
            delay(WRITE_DEBOUNCE_MS)
            writeNow()
        }
    }

    /** كتابة فورية بلا انتظار. نادها عند إيقاف التطبيق حتى لا تضيع آخر 300ms. */
    fun flush() {
        if (!isReady()) return
        writeJob?.cancel()
        scope.launch { writeNow() }
    }

    @Synchronized
    private fun writeNow() {
        val f = storeFile() ?: return
        val current = doc ?: return
        try {
            val tmp = File(f.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(current.toString())
            if (f.exists()) f.delete()
            tmp.renameTo(f)
        } catch (e: Exception) {
            // فشل الكتابة لا يجوز يسقط التطبيق
        }
    }

    // ---------------------------------------------------------------- المنشورات

    fun loadMyPosts(): List<Post> {
        if (!isReady()) return emptyList()
        return try {
            val arr = document().optJSONArray(KEY_POSTS) ?: return emptyList()
            val out = ArrayList<Post>(arr.length())
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { out.add(postFromJson(it)) }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

        @Synchronized
    fun saveMyPosts(posts: List<Post>) {
        if (!isReady()) return
        try {
            val arr = JSONArray()
            posts.forEach { arr.put(postToJson(it)) }
            document().put(KEY_POSTS, arr)
            scheduleWrite()
        } catch (e: Exception) {
        }
    }

    // ------------------------------------------ المحذوفات والمشاركات

    /** المنشورات المحذوفة (سلة النشاط) — آخر 50 محذوفاً. */
    fun loadDeletedPosts(): List<Post> {
        if (!isReady()) return emptyList()
        return try {
            val arr = document().optJSONArray(KEY_DELETED_POSTS) ?: return emptyList()
            val out = ArrayList<Post>(arr.length())
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { out.add(postFromJson(it)) }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveDeletedPosts(posts: List<Post>) {
        if (!isReady()) return
        try {
            val arr = JSONArray()
            posts.take(50).forEach { arr.put(postToJson(it)) }
            document().put(KEY_DELETED_POSTS, arr)
            scheduleWrite()
        } catch (e: Exception) {
        }
    }

    /** معرّفات المنشورات اللي شاركها المستخدم — آخر 50 معرفاً. */
    fun loadSharedPostIds(): List<String> {
        if (!isReady()) return emptyList()
        return try {
            val arr = document().optJSONArray(KEY_SHARED_POSTS) ?: return emptyList()
            val out = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) {
                val id = arr.optString(i)
                if (id.isNotBlank()) out.add(id)
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveSharedPostIds(ids: List<String>) {
        if (!isReady()) return
        try {
            val arr = JSONArray()
            ids.take(50).forEach { arr.put(it) }
            document().put(KEY_SHARED_POSTS, arr)
            scheduleWrite()
        } catch (e: Exception) {
        }
    }

    // ------------------------------------------------------------ الملف الشخصي

    fun loadProfile(): UserProfile? {
        if (!isReady()) return null
        return try {
            val obj = document().optJSONObject(KEY_PROFILE) ?: return null
            profileFromJson(obj)
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun saveProfile(profile: UserProfile) {
        if (!isReady()) return
        try {
            document().put(KEY_PROFILE, profileToJson(profile))
            scheduleWrite()
        } catch (e: Exception) {
        }
    }

    // ---------------------------------------------------------------- المحفظة

    fun loadWallet(): WalletSnapshot? {
        if (!isReady()) return null
        return try {
            val d = document()
            if (!d.has(KEY_WALLET_BALANCE)) return null
            val arr = d.optJSONArray(KEY_WALLET_TX) ?: JSONArray()
            val tx = ArrayList<WalletTransaction>(arr.length())
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { tx.add(transactionFromJson(it)) }
            }
            WalletSnapshot(d.optInt(KEY_WALLET_BALANCE, 0), tx)
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun saveWallet(balance: Int, transactions: List<WalletTransaction>) {
        if (!isReady()) return
        try {
            val arr = JSONArray()
            transactions.forEach { arr.put(transactionToJson(it)) }
            document().put(KEY_WALLET_BALANCE, balance)
            document().put(KEY_WALLET_TX, arr)
            scheduleWrite()
        } catch (e: Exception) {
        }
    }

        // ------------------------------------------------------------ غرف الدردشة

    fun loadRooms(): List<ChatRoom> {
        if (!isReady()) return emptyList()
        return try {
            val arr = document().optJSONArray(KEY_ROOMS) ?: return emptyList()
            val out = ArrayList<ChatRoom>(arr.length())
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { out.add(roomFromJson(it)) }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveRooms(rooms: List<ChatRoom>) {
        if (!isReady()) return
        try {
            val arr = JSONArray()
            rooms.forEach { arr.put(roomToJson(it)) }
            document().put(KEY_ROOMS, arr)
            scheduleWrite()
        } catch (e: Exception) {
        }
    }

    // -------------------------------------------------- تنظيف الملفات اليتيمة

    /**
     * يحذف ملفات الوسائط بـfilesDir اللي ما عاد لها منشور.
     * يشتغل على خيط خلفي ولا يعطل الإقلاع.
     */
        // ------------------------------------------------- المتابعة

    /** كل المتابعات (من يتابع من) — تبقى بعد إغلاق التطبيق. */
    fun loadFollows(): List<Follow> {
        if (!isReady()) return emptyList()
        return try {
            val arr = document().optJSONArray(KEY_FOLLOWS) ?: return emptyList()
            val out = ArrayList<Follow>(arr.length())
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { o ->
                    val follower = o.optString("followerId")
                    val following = o.optString("followingId")
                    if (follower.isNotBlank() && following.isNotBlank()) {
                        out.add(
                            Follow(
                                id = o.optString("id"),
                                followerId = follower,
                                followingId = following
                            )
                        )
                    }
                }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** معرّف الحساب الذي زُرعت له بذرة المتابعة (تُزرع مرة واحدة فقط). */
    fun loadFollowSeedFor(): String {
        if (!isReady()) return ""
        return try {
            document().optString(KEY_FOLLOW_SEED, "")
        } catch (e: Exception) {
            ""
        }
    }

    @Synchronized
    fun saveFollowSeedFor(userId: String) {
        if (!isReady()) return
        try {
            document().put(KEY_FOLLOW_SEED, userId)
            scheduleWrite()
        } catch (e: Exception) {
        }
    }

    @Synchronized
    fun saveFollows(follows: List<Follow>) {
        if (!isReady()) return
        try {
            val arr = JSONArray()
            follows.forEach { f ->
                arr.put(JSONObject().apply {
                    put("id", f.id)
                    put("followerId", f.followerId)
                    put("followingId", f.followingId)
                })
            }
            document().put(KEY_FOLLOWS, arr)
            scheduleWrite()
        } catch (e: Exception) {
        }
    }

    fun cleanupOrphanMedia(keepPaths: Set<String>) {
        val ctx = appContext ?: return
        scope.launch {
            try {
                val files = ctx.filesDir.listFiles() ?: return@launch
                files.forEach { f ->
                    if (f.isFile &&
                        f.name.startsWith(POST_FILE_PREFIX) &&
                        !keepPaths.contains(f.absolutePath)
                    ) {
                        f.delete()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    // ------------------------------------------------------------- التحويلات
    // ‼️ أي حقل جديد بالموديل لازم يُضاف هنا يدوياً

        /**
     * نحفظ هوية الغرفة وإعداداتها فقط.
     * الأعضاء والرسائل والمقاعد الصوتية وحالة العجلة حالة جلسة مؤقتة ولا تُحفظ.
     */
    private fun roomToJson(r: ChatRoom): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("description", r.description)
        put("category", r.category)
        put("iconEmoji", r.iconEmoji)
        put("accessType", r.accessType.name)
        put("password", r.password ?: JSONObject.NULL)
        put("memberCount", r.memberCount)
        put("maxMembers", r.maxMembers)
        put("isJoined", r.isJoined)
        put("isOwner", r.isOwner)
        put("isLocked", r.isLocked)
        put("lockCode", r.lockCode ?: JSONObject.NULL)
        put("pinnedMessage", r.pinnedMessage ?: JSONObject.NULL)
        put("imageUrl", r.imageUrl ?: JSONObject.NULL)
        put("backgroundImageUrl", r.backgroundImageUrl ?: JSONObject.NULL)
        put("ownerId", r.ownerId)
    }

    private fun roomFromJson(o: JSONObject): ChatRoom = ChatRoom(
        id = o.optString("id", ""),
        name = o.optString("name", ""),
        description = o.optString("description", ""),
        category = o.optString("category", "عام"),
        iconEmoji = o.optString("iconEmoji", "💬"),
        accessType = try {
            RoomAccessType.valueOf(o.optString("accessType", RoomAccessType.PUBLIC.name))
        } catch (e: Exception) {
            RoomAccessType.PUBLIC
        },
        password = if (o.isNull("password")) null else o.optString("password", ""),
        memberCount = o.optInt("memberCount", 1),
        maxMembers = o.optInt("maxMembers", 100),
        isJoined = o.optBoolean("isJoined", false),
        isOwner = o.optBoolean("isOwner", false),
        isLocked = o.optBoolean("isLocked", false),
        lockCode = if (o.isNull("lockCode")) null else o.optString("lockCode", ""),
        pinnedMessage = if (o.isNull("pinnedMessage")) null else o.optString("pinnedMessage", ""),
        imageUrl = if (o.isNull("imageUrl")) null else o.optString("imageUrl", ""),
        backgroundImageUrl = if (o.isNull("backgroundImageUrl")) null else o.optString("backgroundImageUrl", ""),
        ownerId = o.optString("ownerId", "")
    )

    private fun postToJson(p: Post): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("authorId", p.authorId)
        put("authorName", p.authorName)
        put("authorHandle", p.authorHandle)
        put("timeAgo", p.timeAgo)
        put("content", p.content)
        put("mediaType", p.mediaType.name)
        put("mediaCaption", p.mediaCaption ?: JSONObject.NULL)
        put("tag", p.tag ?: JSONObject.NULL)
        put("likesCount", p.likesCount)
        put("isLiked", p.isLiked)
        put("commentsCount", p.commentsCount)
        put("sharesCount", p.sharesCount)
        put("isAuthor", p.isAuthor)
        put("isFollowing", p.isFollowing)
        put("authorAvatarUrl", p.authorAvatarUrl)
        put("mediaUri", p.mediaUri)
        val comments = JSONArray()
        p.commentsList.forEach { c ->
            comments.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("authorName", c.authorName)
                    put("text", c.text)
                    put("timeAgo", c.timeAgo)
                }
            )
        }
        put("commentsList", comments)
    }

    private fun postFromJson(o: JSONObject): Post {
        val comments = ArrayList<PostComment>()
        o.optJSONArray("commentsList")?.let { arr ->
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                comments.add(
                    PostComment(
                        id = c.optString("id", ""),
                        authorName = c.optString("authorName", ""),
                        text = c.optString("text", ""),
                        timeAgo = c.optString("timeAgo", "")
                    )
                )
            }
        }
        return Post(
            id = o.optString("id", ""),
            authorId = o.optString("authorId", ""),
            authorName = o.optString("authorName", ""),
            authorHandle = o.optString("authorHandle", ""),
            timeAgo = o.optString("timeAgo", ""),
            content = o.optString("content", ""),
            mediaType = mediaTypeOf(o.optString("mediaType", PostMediaType.NONE.name)),
            mediaCaption = if (o.isNull("mediaCaption")) null else o.optString("mediaCaption", ""),
            tag = if (o.isNull("tag")) null else o.optString("tag", ""),
            likesCount = o.optInt("likesCount", 0),
            isLiked = o.optBoolean("isLiked", false),
            commentsCount = o.optInt("commentsCount", 0),
            sharesCount = o.optInt("sharesCount", 0),
            commentsList = comments,
            isAuthor = o.optBoolean("isAuthor", true),
            isFollowing = o.optBoolean("isFollowing", false),
            authorAvatarUrl = o.optString("authorAvatarUrl", ""),
            mediaUri = o.optString("mediaUri", "")
        )
    }

    private fun mediaTypeOf(name: String): PostMediaType = try {
        PostMediaType.valueOf(name)
    } catch (e: Exception) {
        PostMediaType.NONE
    }

    private fun profileToJson(p: UserProfile): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("handle", p.handle)
        put("bio", p.bio)
        put("avatarEmoji", p.avatarEmoji)
        put("avatarUrl", p.avatarUrl)
        put("email", p.email)
        put("authProvider", p.authProvider)
        put("followersCount", p.followersCount)
        put("followingCount", p.followingCount)
        put("teamsJoinedCount", p.teamsJoinedCount)
        put("joinDate", p.joinDate)
        put("isNotificationsEnabled", p.isNotificationsEnabled)
        put("privacyLevel", p.privacyLevel)
    }

    private fun profileFromJson(o: JSONObject): UserProfile = UserProfile(
        id = o.optString("id", ""),
        name = o.optString("name", ""),
        handle = o.optString("handle", ""),
        bio = o.optString("bio", ""),
        avatarEmoji = o.optString("avatarEmoji", "👤"),
        avatarUrl = o.optString("avatarUrl", ""),
        email = o.optString("email", ""),
        authProvider = o.optString("authProvider", ""),
        followersCount = o.optInt("followersCount", 0),
        followingCount = o.optInt("followingCount", 0),
        teamsJoinedCount = o.optInt("teamsJoinedCount", 0),
        joinDate = o.optString("joinDate", ""),
        isNotificationsEnabled = o.optBoolean("isNotificationsEnabled", true),
        privacyLevel = o.optString("privacyLevel", "عام للجميع")
    )

    private fun transactionToJson(t: WalletTransaction): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("type", t.type.name)
        put("points", t.points)
        put("date", t.date)
        put("note", t.note)
    }

    private fun transactionFromJson(o: JSONObject): WalletTransaction = WalletTransaction(
        id = o.optString("id", ""),
        title = o.optString("title", ""),
        type = try {
            TransactionType.valueOf(o.optString("type", TransactionType.EARN.name))
        } catch (e: Exception) {
            TransactionType.EARN
        },
        points = o.optInt("points", 0),
        date = o.optString("date", ""),
        note = o.optString("note", "")
    )
}
