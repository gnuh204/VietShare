package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val senderId: String = "",
    val parentId: String? = null, // Add this for nested comments
    val content: String = "",
    val timestamp: Timestamp? = null,
    val reactions: Map<String, List<String>> = emptyMap()
)
