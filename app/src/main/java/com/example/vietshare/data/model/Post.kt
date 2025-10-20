package com.example.vietshare.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties // Important: Ignores fields in Firestore that are not in the class
data class Post(
    val postId: String = "",
    val userId: String = "",
    val content: String = "",

    // For new posts that store more info
    val media: List<MediaInfo> = emptyList(),

    // For backward compatibility with old posts
    @JvmField
    val mediaUrls: List<String> = emptyList(),

    val timestamp: Timestamp? = null,
    val likes: List<String> = emptyList(),
    val commentCount: Int = 0
)
