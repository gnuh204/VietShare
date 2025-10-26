package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class Message(
    val messageId: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val content: String? = null, // Can be null if it's an image message
    val media: MediaInfo? = null, // Add this for image messages
    val timestamp: Timestamp? = null
)
