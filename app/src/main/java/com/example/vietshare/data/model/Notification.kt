package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class Notification(
    val notificationId: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val type: String = "",
    val targetId: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val timestamp: Timestamp? = null
)
