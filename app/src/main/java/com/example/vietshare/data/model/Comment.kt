package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val senderId: String = "", // FIX: Ensure the field is named senderId
    val content: String = "",
    val timestamp: Timestamp? = null
)
