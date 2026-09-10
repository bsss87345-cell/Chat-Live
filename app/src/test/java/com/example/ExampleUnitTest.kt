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
}
