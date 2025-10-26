package com.example.vietshare.data.model

import com.google.firebase.Timestamp

enum class ChatType {
    ONE_TO_ONE,
    GROUP
}

data class Chat(
    val roomId: String = "",
    val type: String = ChatType.ONE_TO_ONE.name,

    // For Group chats
    val groupName: String? = null,
    val groupImageUrl: String? = null,
    val adminId: String? = null,

    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCount: Map<String, Int> = emptyMap()
)
