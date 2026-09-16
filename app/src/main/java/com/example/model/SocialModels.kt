package com.example.model

enum class AppTab(val titleAr: String) {
    FEED("الرئيسية"),
    CHAT("الدردشة"),
    GAMES("ألعاب"),
    TEAM("الفريق"),
    PROFILE("الملف الشخصي")
}

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val handle: String = "",
    val bio: String = "",
    val avatarEmoji: String = "👤",
    val email: String = "",
    val authProvider: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val teamsJoinedCount: Int = 0,
    val joinDate: String = "",
    val isNotificationsEnabled: Boolean = true,
    val privacyLevel: String = "عام للجميع"
)

enum class StoryMediaType {
    PHOTO, VIDEO, TEXT
}

data class Story(
    val id: String,
    val authorName: String,
    val isViewed: Boolean = false,
    val mediaText: String = "",
    val timeAgo: String = "منذ ساعة",
    val isCurrentUser: Boolean = false,
    val gradientColors: List<Long> = listOf(0xFF673AB7, 0xFF00897B),
    val mediaType: StoryMediaType = StoryMediaType.TEXT,
    val mediaUri: String? = null
)

enum class PostMediaType {
    NONE, IMAGE, SHORT_VIDEO
}

data class PostComment(
    val id: String,
    val authorName: String,
    val text: String,
    val timeAgo: String
)

data class Post(
    val id: String,
    val authorName: String,
    val authorHandle: String,
    val authorRole: String = "عضو نشط",
    val timeAgo: String,
    val content: String,
    val mediaType: PostMediaType = PostMediaType.NONE,
    val mediaCaption: String? = null,
    val tag: String? = null,
    val likesCount: Int,
    val isLiked: Boolean = false,
    val commentsCount: Int,
    val sharesCount: Int,
    val commentsList: List<PostComment> = emptyList(),
    val isAuthor: Boolean = false,
    val isFollowing: Boolean = false
)

enum class ChatMessageType {
    TEXT, AUDIO, IMAGE, GAME_INVITE
}

data class ChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val audioDurationSec: Int = 0,
    val isAudioPlaying: Boolean = false,
    val gameTitle: String? = null,
    val gameReward: Int = 0
)

data class ChatConversation(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val memberCount: Int = 1,
    val messages: List<ChatMessage> = emptyList()
)

enum class RoomAccessType(val labelAr: String) {
    PUBLIC("عامة"),
    PASSWORD("بكلمة سر"),
    INVITE_ONLY("دعوة فقط")
}

enum class RoomMemberRole(val labelAr: String) {
    OWNER("المالك"),
    ADMIN("مشرف"),
    MEMBER("عضو")
}

data class RoomMember(
    val id: String,
    val name: String,
    val role: RoomMemberRole,
    val isOnline: Boolean = false,
    val isMuted: Boolean = false
)

data class ChatRoom(
    val id: String,
    val name: String,
    val description: String,
    val category: String, // ألعاب، رياضة، عام، فرق، تقنية، ثقافة
    val iconEmoji: String = "💬",
    val accessType: RoomAccessType = RoomAccessType.PUBLIC,
    val password: String? = null,
    val inviteCode: String = "",
    val memberCount: Int = 1,
    val maxMembers: Int = 100,
    val isJoined: Boolean = false,
    val isOwner: Boolean = false,
    val isLocked: Boolean = false,
    val pinnedMessage: String? = null,
    val members: List<RoomMember> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val imageUrl: String? = null,
    val backgroundImageUrl: String? = null,
    val blockedMemberIds: List<String> = emptyList()
)

data class TeamMember(
    val id: String,
    val name: String,
    val role: String, // قائد، مشرف، عضو
    val scoreContribution: Int,
    val isOnline: Boolean
)

data class TeamChallenge(
    val id: String,
    val title: String,
    val description: String,
    val current: Int,
    val target: Int,
    val daysLeft: Int,
    val rewardPoints: Int
)

data class Team(
    val id: String,
    val name: String,
    val motto: String,
    val level: Int,
    val totalScore: Int,
    val memberCount: Int,
    val maxMembers: Int,
    val rank: Int,
    val members: List<TeamMember>,
    val challenges: List<TeamChallenge>
)

enum class TransactionType {
    EARN, SPEND
}

data class WalletTransaction(
    val id: String,
    val title: String,
    val type: TransactionType,
    val points: Int,
    val date: String,
    val note: String
)

data class StoreItem(
    val id: String,
    val title: String,
    val description: String,
    val cost: Int,
    val category: String,
    val isOwned: Boolean = false
)

data class ReferredUser(
    val id: String,
    val name: String,
    val handle: String,
    val joinedDate: String,
    val avatarEmoji: String = "👤"
)

data class FriendRequest(
    val id: String,
    val senderName: String,
    val senderHandle: String,
    val senderAvatar: String = "👤",
    val mutualFriendsCount: Int = 0,
    val timeAgo: String = "الآن"
)
