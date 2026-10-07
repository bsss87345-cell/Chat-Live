package com.example.data

import com.example.model.AppNotification
import com.example.model.Follow
import com.example.model.ChatMessage
import com.example.model.ChatMessageType
import com.example.model.ChatRoom
import com.example.model.RoomAccessType
import com.example.model.NotificationType
import com.example.model.TransactionType
import com.example.model.UserProfile
import com.example.model.WalletTransaction
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import java.util.Date

/**
 * الجسر السحابي — كتابة/قراءة **بيانات المستخدم** في Firestore.
 *
 *  • كل الدوال تُحجب عمداً ⇒ تُستدعى من `Dispatchers.IO` فقط.
 *  • لا ترمي استثناءات أبداً: أسوأ حالة = لا مزامنة، والتطبيق يستمر محلياً.
 *  • المعرّف دائماً من جلسة Firebase Auth ⇒ لا معرّفات من العميل.
 *  • شكل المستندات مطابق لقواعد firestore.rules المنشورة.
 */
object CloudStore {

    private const val USERS = "users"
    private const val FOLLOWS = "follows"
    private const val NOTIFICATIONS = "notifications"
    private const val WALLETS = "wallets"
    private const val MAX_TRANSACTIONS = 100
    private const val MAX_BALANCE = 1_000_000
    private const val ROOMS = "rooms"
    private const val MESSAGES = "messages"
    private const val MAX_ROOMS = 50

    /** معرّف الحساب الحقيقي من Firebase Auth، أو null لو لا جلسة. */
    val uid: String? get() = try {
        FirebaseAuth.getInstance().currentUser?.uid
    } catch (e: Exception) {
        null
    }

    fun isReady(): Boolean = uid != null

    // ───────────────── البروفايل (users/{uid}) ─────────────────

    fun saveProfile(profile: UserProfile): Boolean {
        val me = uid ?: return false
        if (profile.id != me) return false
        val data = mapOf(
            "id" to me,
            "name" to profile.name.take(60),
            "handle" to profile.handle.take(60),
            "bio" to profile.bio.take(300),
            "avatarEmoji" to profile.avatarEmoji.take(8),
            "avatarUrl" to profile.avatarUrl.take(500),
            "email" to profile.email.take(100),
            "authProvider" to profile.authProvider.take(40),
            "followersCount" to profile.followersCount,
            "followingCount" to profile.followingCount,
            "teamsJoinedCount" to profile.teamsJoinedCount,
            "joinDate" to profile.joinDate.take(40),
            "isNotificationsEnabled" to profile.isNotificationsEnabled,
            "privacyLevel" to profile.privacyLevel.take(20)
        )
        return awaitWrite { db().collection(USERS).document(me).set(data) }
    }

    fun loadProfile(): UserProfile? {
        val me = uid ?: return null
        val d = awaitRead { db().collection(USERS).document(me).get() } ?: return null
        if (!d.exists()) return null
        return UserProfile(
            id = d.getString("id") ?: me,
            name = d.getString("name").orEmpty(),
            handle = d.getString("handle").orEmpty(),
            bio = d.getString("bio").orEmpty(),
            avatarEmoji = d.getString("avatarEmoji") ?: "👤",
            avatarUrl = d.getString("avatarUrl").orEmpty(),
            email = d.getString("email").orEmpty(),
            authProvider = d.getString("authProvider").orEmpty(),
            followersCount = (d.getLong("followersCount") ?: 0L).toInt(),
            followingCount = (d.getLong("followingCount") ?: 0L).toInt(),
            teamsJoinedCount = (d.getLong("teamsJoinedCount") ?: 0L).toInt(),
            joinDate = d.getString("joinDate").orEmpty(),
            isNotificationsEnabled = d.getBoolean("isNotificationsEnabled") ?: true,
            privacyLevel = d.getString("privacyLevel") ?: "عام للجميع"
        )
    }
    // ───────────────── المتابعات (follows/{follower}_{following}) ─────────────────

    private fun followIdOf(followerId: String, followingId: String) = "${followerId}_${followingId}"

    fun addFollow(follow: Follow): Boolean {
        val me = uid ?: return false
        val target = follow.followingId
        if (follow.followerId != me || target.isBlank() || target == me) return false
        val data = mapOf(
            "followerId" to me,
            "followingId" to target,
            "timestamp" to System.currentTimeMillis()
        )
        // مستند جديد = إنشاء (مسموح للمالك). موجود مسبقاً ⇒ القاعدة ترفض التعديل ⇒ نعتبرها نجاحاً.
        return awaitWrite { db().collection(FOLLOWS).document(followIdOf(me, target)).set(data) }
    }

    fun removeFollow(followerId: String, followingId: String): Boolean {
        val me = uid ?: return false
        if (followerId != me || followingId.isBlank()) return false
        return awaitWrite { db().collection(FOLLOWS).document(followIdOf(me, followingId)).delete() }
    }

    /** كل علاقات المتابعة لمستخدم: يتابعهم + يتابعونه. */
    fun loadFollowsFor(userId: String): List<Follow> {
        if (uid == null || userId.isBlank()) return emptyList()
        val out = ArrayList<Follow>(32)
        try {
            Tasks.await(db().collection(FOLLOWS).whereEqualTo("followerId", userId).get())
                .documents.forEach { d ->
                    out.add(
                        Follow(
                            id = d.id,
                            followerId = userId,
                            followingId = d.getString("followingId").orEmpty(),
                            timestamp = d.getLong("timestamp") ?: 0L
                        )
                    )
                }
            Tasks.await(db().collection(FOLLOWS).whereEqualTo("followingId", userId).get())
                .documents.forEach { d ->
                    out.add(
                        Follow(
                            id = d.id,
                            followerId = d.getString("followerId").orEmpty(),
                            followingId = userId,
                            timestamp = d.getLong("timestamp") ?: 0L
                        )
                    )
                }
        } catch (e: Exception) {
            // بلا شبكة: نرجع ما وصل (غالباً لا شيء) — والبيانات المحلية تبقى مصدراً مؤقتاً
        }
        return out
    }
        // ───────────────── الإشعارات (notifications/{id}) ─────────────────

    fun saveNotification(notification: AppNotification): Boolean {
        val me = uid ?: return false
        val data = mapOf(
            "userId" to me,
            "fromId" to me,
            "type" to notification.type.name,
            "text" to notification.text.take(300),
            "timeMillis" to notification.timeMillis,
            "isRead" to notification.isRead
        )
        return awaitWrite { db().collection(NOTIFICATIONS).document(notification.id).set(data) }
    }

    fun loadNotifications(): List<AppNotification> {
        val me = uid ?: return emptyList()
        return try {
            Tasks.await(db().collection(NOTIFICATIONS).whereEqualTo("userId", me).get())
                .documents.mapNotNull { it.toNotification() }
                .sortedByDescending { it.timeMillis }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** القاعدة تسمح بتغيير `isRead` فقط. */
    fun markNotificationRead(notificationId: String): Boolean {
        if (uid == null || notificationId.isBlank()) return false
        return awaitWrite { db().collection(NOTIFICATIONS).document(notificationId).update("isRead", true) }
    }

    private fun DocumentSnapshot.toNotification(): AppNotification? {
        val docId = id
        if (docId.isBlank()) return null
        val type = runCatching { NotificationType.valueOf(getString("type") ?: "") }
            .getOrDefault(NotificationType.SYSTEM)
        return AppNotification(
            id = docId,
            type = type,
            text = getString("text").orEmpty(),
            timeMillis = getLong("timeMillis") ?: 0L,
            isRead = getBoolean("isRead") ?: false
        )
    }
    // ───────────────── المحفظة (wallets/{uid}) ─────────────────

    fun saveWallet(balance: Int, transactions: List<WalletTransaction>): Boolean {
        val me = uid ?: return false
        val tx = transactions.take(MAX_TRANSACTIONS).map { t ->
            mapOf(
                "id" to t.id.take(60),
                "title" to t.title.take(80),
                "type" to t.type.name,
                "points" to t.points,
                "date" to t.date.take(30),
                "note" to t.note.take(120)
            )
        }
        return awaitWrite {
            db().collection(WALLETS).document(me)
                .set(mapOf("balance" to balance.coerceIn(0, MAX_BALANCE), "transactions" to tx))
        }
    }

    fun loadWallet(): WalletSnapshot? {
        val me = uid ?: return null
        val d = awaitRead { db().collection(WALLETS).document(me).get() } ?: return null
        if (!d.exists()) return null
        val balance = (d.getLong("balance") ?: 0L).toInt()
        val raw = d.get("transactions")
        val list = ArrayList<WalletTransaction>(8)
        if (raw is List<*>) {
            raw.forEach { item ->
                val map = item as? Map<*, *> ?: return@forEach
                val type = runCatching { TransactionType.valueOf(map["type"] as? String ?: "") }
                    .getOrDefault(TransactionType.EARN)
                list.add(
                    WalletTransaction(
                        id = (map["id"] as? String).orEmpty(),
                        title = (map["title"] as? String).orEmpty(),
                        type = type,
                        points = (map["points"] as? Number)?.toInt() ?: 0,
                        date = (map["date"] as? String).orEmpty(),
                        note = (map["note"] as? String).orEmpty()
                    )
                )
            }
        }
        return WalletSnapshot(balance = balance, transactions = list)
    }

        // ───────────────── الغرف (rooms/{roomId}) ─────────────────

    /** يحفظ غرفة **أملكها** سحابياً (إنشاء أول مرة أو تحديث لاحق). غير المالك يُرفض بهدوء. */
    fun saveRoom(room: ChatRoom): Boolean {
        val me = uid ?: return false
        val data = mapOf(
            "id" to room.id.take(40),
            "ownerId" to me,
            "name" to room.name.trim().take(60),
            "description" to room.description.trim().take(300),
            "category" to room.category.take(30),
            "iconEmoji" to room.iconEmoji.take(8),
            "imageUrl" to (room.imageUrl ?: "").take(500),
            "backgroundImageUrl" to (room.backgroundImageUrl ?: "").take(500),
            "accessType" to room.accessType.name,
            "password" to (room.password ?: ""),
            "memberCount" to room.memberCount,
            "maxMembers" to room.maxMembers,
            "isLocked" to room.isLocked,
            "lockCode" to (room.lockCode ?: ""),
            "pinnedMessage" to (room.pinnedMessage ?: "").take(300),
            "updatedAt" to System.currentTimeMillis()
        )
        return awaitWrite { db().collection(ROOMS).document(room.id).set(data) }
    }

    /** يعدّل عدّاد الأعضاء فقط (+1/-1) — تسمح به القاعدة لأي عضو مسجَّل. */
    fun stepRoomMemberCount(roomId: String, step: Int): Boolean {
        if (uid == null || (step != 1 && step != -1)) return false
        return awaitWrite {
            db().collection(ROOMS).document(roomId).update("memberCount", FieldValue.increment(step.toLong()))
        }
    }

    /** كل الغرف السحابية (للتصفح). */
    fun loadRooms(): List<ChatRoom> = try {
        if (uid == null) emptyList()
        else Tasks.await(db().collection(ROOMS).limit(MAX_ROOMS.toLong()).get())
            .documents.mapNotNull { d -> if (d.exists()) d.toRoom() else null }
    } catch (e: Exception) {
        emptyList()
    }

    private fun DocumentSnapshot.toRoom(): ChatRoom {
        val owner = getString("ownerId").orEmpty()
        val access = try {
            RoomAccessType.valueOf(getString("accessType") ?: "PUBLIC")
        } catch (e: Exception) {
            RoomAccessType.PUBLIC
        }
        return ChatRoom(
            id = getString("id") ?: id,
            name = getString("name").orEmpty(),
            description = getString("description").orEmpty(),
            category = getString("category") ?: "عام",
            iconEmoji = getString("iconEmoji") ?: "💬",
            accessType = access,
            password = getString("password")?.takeIf { it.isNotBlank() },
            memberCount = (getLong("memberCount") ?: 1L).toInt(),
            maxMembers = (getLong("maxMembers") ?: 100L).toInt(),
            isJoined = owner == uid,
            isOwner = owner == uid,
            isLocked = getBoolean("isLocked") ?: false,
            lockCode = getString("lockCode")?.takeIf { it.isNotBlank() },
            pinnedMessage = getString("pinnedMessage")?.takeIf { it.isNotBlank() },
            imageUrl = getString("imageUrl")?.takeIf { it.isNotBlank() },
            backgroundImageUrl = getString("backgroundImageUrl")?.takeIf { it.isNotBlank() },
            ownerId = owner
        )
    }

        // ───────────────── رسائل الغرف (rooms/{roomId}/messages/{msgId}) ─────────────────

    /** يرفع رسالة غرفة — القاعدة تشترط senderId = معرّف المُرسِل وطول النص ≤ 2000. */
        fun saveRoomMessage(
        roomId: String,
        message: ChatMessage,
        senderName: String = message.senderName
    ): Boolean {
        val me = uid ?: return false
        val data = mapOf(
            "senderId" to me,
            "senderName" to senderName.take(60),
            "text" to message.text.take(2000),
            "type" to message.type.name,
            "timeText" to message.timestamp.take(20),
            "createdAt" to System.currentTimeMillis(),
            "sentAt" to FieldValue.serverTimestamp()
        )
        return awaitWrite {
            db().collection(ROOMS).document(roomId).collection(MESSAGES).document(message.id).set(data)
        }
    }

    /** يحذف رسالة (يسمح به لمن أرسلها فقط). */
    fun deleteRoomMessage(roomId: String, messageId: String): Boolean = awaitWrite {
        db().collection(ROOMS).document(roomId).collection(MESSAGES).document(messageId).delete()
    }

    /** رسائل الغرفة بالترتيب الزمني (الأقدم أولاً). */
    fun loadRoomMessages(roomId: String, limit: Int = 100): List<ChatMessage> = try {
        if (uid == null) emptyList()
        else Tasks.await(
            db().collection(ROOMS).document(roomId).collection(MESSAGES)
                .orderBy("createdAt").limit(limit.toLong()).get()
        ).documents.map { d ->
            ChatMessage(
                id = d.id,
                senderName = d.getString("senderName").orEmpty(),
                text = d.getString("text").orEmpty(),
                timestamp = d.getString("timeText") ?: "الآن",
                isFromMe = d.getString("senderId") == uid,
                type = try {
                    ChatMessageType.valueOf(d.getString("type") ?: "TEXT")
                } catch (e: Exception) {
                    ChatMessageType.TEXT
                }
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
    
    // ───────────────── التنفيذ ─────────────────

        /**
     * 🔴 استماع لحظي لرسائل الغرفة (آخر [limit] رسالة، الأقدم أولاً).
     * بخلاف باقي الدوال: غير حاجبة، تُستدعى من أي خيط، والنتيجة تصل على الخيط الرئيسي.
     * لازم تستدعي remove() على المسجّل عند الخروج من الغرفة.
     */
    fun listenRoomMessages(
        roomId: String,
        limit: Int = 50,
        sinceMillis: Long = 0L,
        onChange: (List<ChatMessage>) -> Unit
    ): ListenerRegistration? {
        if (uid == null || roomId.isBlank()) return null
        return try {
            db().collection(ROOMS).document(roomId).collection(MESSAGES)
                .whereGreaterThan("sentAt", Timestamp(Date(sinceMillis)))
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .addSnapshotListener { snap, error ->
                    if (error != null || snap == null) return@addSnapshotListener
                    onChange(snap.documents.map { it.toChatMessage() }.reversed())
                }
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toChatMessage(): ChatMessage = ChatMessage(
        id = id,
        senderName = getString("senderName").orEmpty(),
        text = getString("text").orEmpty(),
        timestamp = getString("timeText") ?: "الآن",
        isFromMe = getString("senderId") == uid,
        type = try {
            ChatMessageType.valueOf(getString("type") ?: "TEXT")
        } catch (e: Exception) {
            ChatMessageType.TEXT
        }
    )

    private fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun awaitWrite(block: () -> Task<*>): Boolean = try {
        Tasks.await(block())
        true
    } catch (e: Exception) {
        false
    }

    private fun awaitRead(block: () -> Task<DocumentSnapshot>): DocumentSnapshot? = try {
        Tasks.await(block())
    } catch (e: Exception) {
        null
    }
}
