package com.example

import com.example.model.RoomAccessType
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
        assertEquals(initialCount + 1, updatedRooms.size)
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
        val passRoom = vm.chatRooms.value.first { it.accessType == RoomAccessType.PASSWORD }
        val wrongJoin = vm.joinChatRoom(passRoom.id, "wrong_pass")
        assertFalse(wrongJoin)

        val correctJoin = vm.joinChatRoom(passRoom.id, passRoom.password ?: "1234")
        assertTrue(correctJoin)
        val roomAfter = vm.chatRooms.value.find { it.id == passRoom.id }!!
        assertTrue(roomAfter.isJoined)
    }

    @Test
    fun testPinAndUnpinRoomMessage() {
        val vm = SocialAppViewModel()
        val targetRoom = vm.chatRooms.value.first { it.isOwner }
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
        val targetRoom = vm.chatRooms.value.first { it.members.any { m -> m.id == "rm_1" } }
        var room1 = vm.chatRooms.value.find { it.id == targetRoom.id }!!
        val targetMember = room1.members.find { it.id == "rm_1" }!!
        assertFalse(targetMember.isMuted)

        // Mute
        vm.muteRoomMember(targetRoom.id, "rm_1")
        room1 = vm.chatRooms.value.find { it.id == targetRoom.id }!!
        assertTrue(room1.members.find { it.id == "rm_1" }!!.isMuted)

        // Kick
        val countBefore = room1.members.size
        vm.kickRoomMember(targetRoom.id, "rm_1")
        room1 = vm.chatRooms.value.find { it.id == targetRoom.id }!!
        assertEquals(countBefore - 1, room1.members.size)
        assertNull(room1.members.find { it.id == "rm_1" })
    }

    @Test
    fun testUserProfileAndBioUpdate() {
        val vm = SocialAppViewModel()
        val initialProfile = vm.userProfile.value
        assertEquals("أحمد المنصور", initialProfile.name)

        vm.updateUserBio("مطور ومتحمس لتحديات مجتمعنا الذكية 🚀")
        assertEquals("مطور ومتحمس لتحديات مجتمعنا الذكية 🚀", vm.userProfile.value.bio)

        vm.toggleProfilePrivacy()
        assertEquals("للأصدقاء فقط", vm.userProfile.value.privacyLevel)

        // Verify wallet balance is preserved
        val currentBalance = vm.walletBalance.value
        assertTrue(currentBalance > 0)
        assertEquals(5, vm.transactions.value.size)
    }

    @Test
    fun testPostOwnershipAndFollowToggle() {
        val vm = SocialAppViewModel()
        val posts = vm.posts.value
        val authorPost = posts.find { it.isAuthor }
        assertNotNull(authorPost)
        assertEquals("أحمد المنصور", authorPost!!.authorName)

        val otherPost = posts.find { !it.isAuthor }
        assertNotNull(otherPost)
        val initialFollow = otherPost!!.isFollowing

        // Toggle follow
        vm.toggleFollowUser(otherPost.id)
        val afterFollow = vm.posts.value.find { it.id == otherPost.id }!!
        assertEquals(!initialFollow, afterFollow.isFollowing)
    }

    @Test
    fun testPostEditAndDelete() {
        val vm = SocialAppViewModel()
        val authorPost = vm.posts.value.find { it.isAuthor }!!
        val updatedText = "محتوى معدل جديد للمنشور الخاص بي ✍️"

        // Edit
        vm.editPost(authorPost.id, updatedText)
        val edited = vm.posts.value.find { it.id == authorPost.id }!!
        assertEquals(updatedText, edited.content)

        // Delete
        val countBefore = vm.posts.value.size
        vm.deletePost(authorPost.id)
        val countAfter = vm.posts.value.size
        assertEquals(countBefore - 1, countAfter)
        assertNull(vm.posts.value.find { it.id == authorPost.id })
    }

    @Test
    fun testPostReportMessage() {
        val vm = SocialAppViewModel()
        val otherPost = vm.posts.value.find { !it.isAuthor }!!
        vm.reportPost(otherPost.id, "محتوى غير لائق")
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
