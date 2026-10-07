package com.example.data

import com.example.model.AppNotification
import com.example.model.Follow
import com.example.model.NotificationType
import com.example.model.TransactionType
import com.example.model.UserProfile
import com.example.model.WalletTransaction
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

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
