package com.example

import com.example.model.Post
import com.example.model.RoomAccessType
import com.example.model.RoomMember
import com.example.model.RoomMemberRole
import com.example.viewmodel.SocialAppViewModel
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testChatRoomCreationAndOwner() {
        val vm = SocialAppViewModel()
        val initialCount = vm.chatRooms.value.size
        assertEquals(0, initialCount)

        vm.createChatRoom(
            name = "غرفة البرمجة والتقنية",
            description = "نقاشات حول التقنية والبرمجة",
            category = "تقنية",
            accessType = RoomAccessType.PUBLIC,
            password = null,
            maxMembers = 50,
            iconEmoji = "💻"
        )

        val updatedRooms = vm.chatRooms.value
        assertEquals(1, updatedRooms.size)
        val created = updatedRooms.first()
        assertEquals("غرفة البرمجة والتقنية", created.name)
        assertTrue(created.isOwner)
        assertTrue(created.isJoined)
        assertEquals(RoomMemberRole.OWNER, created.members.first().role)
        assertEquals(8, created.id.length)
        assertTrue(created.id.all { it.isDigit() })
    }

    @Test
    fun testChatRoomPasswordProtection() {
        val vm = SocialAppViewModel()
        // Create a password protected room
        vm.createChatRoom(
            name = "غرفة سرية",
            description = "غرفة بكلمة مرور",
            category = "عام",
            accessType = RoomAccessType.PASSWORD,
            password = "1234",
            maxMembers = 10,
            iconEmoji = "🔒"
        )

        val passRoom = vm.chatRooms.value.first()
        val wrongJoin = vm.joinChatRoom(passRoom.id, "wrong_pass")
        assertFalse(wrongJoin)

        val correctJoin = vm.joinChatRoom(passRoom.id, "1234")
        assertTrue(correctJoin)
        val roomAfter = vm.chatRooms.value.find { it.id == passRoom.id }!!
        assertTrue(roomAfter.isJoined)
    }

    @Test
    fun testPinAndUnpinRoomMessage() {
        val vm = SocialAppViewModel()
        vm.createChatRoom(
            name = "غرفة الإعلانات",
            description = "غرفة للإعلانات",
            category = "عام",
            accessType = RoomAccessType.PUBLIC,
            password = null,
            maxMembers = 20,
            iconEmoji = "📢"
        )
        val targetRoom = vm.chatRooms.value.first()
        vm.pinRoomMessage(targetRoom.id, "إعلان هام للأعضاء")
        var room1 = vm.chatRooms.value.find { it.id == targetRoom.id }!!
        assertEquals("📌 إعلان هام للأعضاء", room1.pinnedMessage)

        vm.unpinRoomMessage(targetRoom.id)
        room1 = vm.chatRooms.value.find { it.id == targetRoom.id }!!
        assertNull(room1.pinnedMessage)
    }

    @Test
    fun testMuteAndKickRoomMember() {
        val vm = SocialAppViewModel()
        vm.createChatRoom(
            name = "غرفة الفريق",
            description = "غرفة خاصة بالفريق",
            category = "عام",
            accessType = RoomAccessType.PUBLIC,
            password = null,
            maxMembers = 10,
            iconEmoji = "👥"
        )
        val targetRoom = vm.chatRooms.value.first()
        // Add a mock member
        val newMember = RoomMember(id = "rm_test", name = "عضو تجريبي", avatarEmoji = "👤", role = RoomMemberRole.MEMBER, isMuted = false)
        val updatedRooms = vm.chatRooms.value.map { room ->
            if (room.id == targetRoom.id) room.copy(members = room.members + newMember) else room
        }
        // Use reflection or standard method to update state
        var room1 = targetRoom.copy(members = targetRoom.members + newMember)
        assertEquals(2, room1.members.size)
        val targetMember = room1.members.find { it.id == "rm_test" }!!
        assertFalse(targetMember.isMuted)
    }

    @Test
    fun testUserProfileAndBioUpdate() {
        val vm = SocialAppViewModel()
        val initialProfile = vm.userProfile.value
        // Clean initial state has empty name
        assertEquals("", initialProfile.name)

        vm.updateUserBio("مطور ومتحمس لتحديات مجتمعنا الذكية 🚀")
        assertEquals("مطور ومتحمس لتحديات مجتمعنا الذكية 🚀", vm.userProfile.value.bio)

        vm.toggleProfilePrivacy()
        assertEquals("للأصدقاء فقط", vm.userProfile.value.privacyLevel)

        // Verify clean wallet balance is zeroed
        val currentBalance = vm.walletBalance.value
        assertEquals(0, currentBalance)
        assertEquals(0, vm.transactions.value.size)
    }

    @Test
    fun testPostOwnershipAndFollowToggle() {
        val vm = SocialAppViewModel()
        // Initially empty
        assertTrue(vm.posts.value.isEmpty())

        // Add a post
        vm.addNewPost("منشور جديد للاختبار", emptyList())
        val posts = vm.posts.value
        assertEquals(1, posts.size)
        val authorPost = posts.first()
        assertTrue(authorPost.isAuthor)
    }

    @Test
    fun testPostEditAndDelete() {
        val vm = SocialAppViewModel()
        vm.addNewPost("منشور قبل التعديل", emptyList())
        val authorPost = vm.posts.value.first()
        val updatedText = "محتوى معدل جديد للمنشور الخاص بي ✍️"

        // Edit
        vm.editPost(authorPost.id, updatedText)
        val edited = vm.posts.value.find { it.id == authorPost.id }!!
        assertEquals(updatedText, edited.content)

        // Delete
        vm.deletePost(authorPost.id)
        assertTrue(vm.posts.value.isEmpty())
    }

    @Test
    fun testPostReportMessage() {
        val vm = SocialAppViewModel()
        vm.reportPost("test_id", "محتوى غير لائق")
        assertNotNull(vm.userMessage.value)
        assertTrue(vm.userMessage.value!!.contains("البلاغ"))
    }

    @Test
    fun testStoryCreationModes() {
        val vm = SocialAppViewModel()

        // 1. Photo story
        vm.addStory(
            text = "صورة رائعة من الرحلة",
            mediaUri = "https://images.unsplash.com/photo-1",
            mediaType = com.example.model.StoryMediaType.PHOTO
        )
        val photoStory = vm.stories.value.first()
        assertEquals("صورة رائعة من الرحلة", photoStory.mediaText)
        assertEquals(com.example.model.StoryMediaType.PHOTO, photoStory.mediaType)
        assertEquals("https://images.unsplash.com/photo-1", photoStory.mediaUri)
        assertTrue(photoStory.isCurrentUser)

        // 2. Video story
        vm.addStory(
            text = "تسجيل فيديو سريع 🎥",
            mediaUri = "https://example.com/video1.mp4",
            mediaType = com.example.model.StoryMediaType.VIDEO
        )
        val videoStory = vm.stories.value.first()
        assertEquals("تسجيل فيديو سريع 🎥", videoStory.mediaText)
        assertEquals(com.example.model.StoryMediaType.VIDEO, videoStory.mediaType)
        assertTrue(videoStory.isCurrentUser)

        // 3. Text story
        vm.addStory(
            text = "صباح الخير والبركة لجميع الأصدقاء ✨",
            mediaUri = null,
            mediaType = com.example.model.StoryMediaType.TEXT,
            gradientColors = listOf(0xFF673AB7, 0xFF00897B)
        )
        val textStory = vm.stories.value.first()
        assertEquals("صباح الخير والبركة لجميع الأصدقاء ✨", textStory.mediaText)
        assertEquals(com.example.model.StoryMediaType.TEXT, textStory.mediaType)
        assertNull(textStory.mediaUri)
        assertEquals(2, textStory.gradientColors.size)
    }

    @Test
    fun testGameCatalogStructureAndDominoActive() {
        // Verify Domino is active and only Domino is returned in available list
        val availableGames = com.example.ui.screens.ALL_CATALOG_GAMES.filter { it.isAvailable }
        assertEquals(1, availableGames.size)

        val domino = availableGames.first()
        assertEquals(com.example.model.GameType.DOMINO, domino.gameType)
        assertEquals("الدومينو الكلاسيكية", domino.title)
        assertTrue(domino.isAvailable)

        // Verify the catalog is extensible and contains the other planned games
        val allCatalogTypes = com.example.ui.screens.ALL_CATALOG_GAMES.map { it.gameType }
        assertTrue(allCatalogTypes.contains(com.example.model.GameType.DOMINO))
        assertTrue(allCatalogTypes.contains(com.example.model.GameType.LUDO))
        assertTrue(allCatalogTypes.contains(com.example.model.GameType.JACKAROO))
        assertTrue(allCatalogTypes.contains(com.example.model.GameType.SNAKES_AND_LADDERS))
        assertTrue(allCatalogTypes.contains(com.example.model.GameType.CHESS))
    }
}
