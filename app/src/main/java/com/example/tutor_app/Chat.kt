package com.example.tutor_app

data class Chat(
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantTypes: Map<String, String> = emptyMap(), // "student" or "tutor"
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Map<String, Long> = emptyMap() // Changed from Int to Long
) {
    // Helper function to get other participant's name
    fun getOtherParticipantName(currentUserId: String): String {
        return participantNames.filter { it.key != currentUserId }.values.firstOrNull() ?: "Unknown"
    }

    // Helper function to get other participant's ID
    fun getOtherParticipantId(currentUserId: String): String {
        return participantIds.firstOrNull { it != currentUserId } ?: ""
    }

    // Helper function to get other participant's type
    fun getOtherParticipantType(currentUserId: String): String {
        val otherId = getOtherParticipantId(currentUserId)
        return participantTypes[otherId] ?: "student"
    }
}