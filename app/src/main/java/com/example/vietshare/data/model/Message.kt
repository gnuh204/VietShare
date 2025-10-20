package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class Message(
    val messageId: String = "",
    val roomId: String = "", // Add this field
    val senderId: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Map<String, Boolean> = emptyMap(),
    val messageType: String = "TEXT"
)
