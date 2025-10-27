package com.example.vietshare.data.model

import com.google.firebase.Timestamp

enum class MessageType {
    USER,
    SYSTEM
}

data class Message(
    val messageId: String = "",
    val roomId: String = "",
    val senderId: String? = null, // Can be null for system messages
    val content: String? = null,
    val media: MediaInfo? = null,
    val type: String = MessageType.USER.name, // Add this
    val timestamp: Timestamp? = null
)
