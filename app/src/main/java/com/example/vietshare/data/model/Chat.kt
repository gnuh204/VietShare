package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class Chat(
    val roomId: String = "",
    val type: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null
)
