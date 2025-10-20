package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val displayName: String = "",
    val bio: String = "",
    val profileImageUrl: String = "", // Avatar URL

    val dateOfBirth: String = "",
    val hometown: String = "",
    val hobbies: List<String> = emptyList(),

    // Follow system
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),

    val lastActive: Timestamp? = null,
    val settings: Map<String, Any> = emptyMap()
)
