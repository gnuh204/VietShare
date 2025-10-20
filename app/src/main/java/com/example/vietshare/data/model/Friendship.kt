package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class Friendship(
    val id: String = "",
    val userAId: String = "",
    val userBId: String = "",
    val participantIds: List<String> = emptyList(), // [userAId, userBId] sorted
    val status: String = "", // PENDING, ACCEPTED
    val createdAt: Timestamp? = null
)
