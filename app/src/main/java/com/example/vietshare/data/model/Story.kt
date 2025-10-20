package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val mediaUrl: String = "",
    val caption: String? = null,
    val timestamp: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val views: List<String> = emptyList()
)
