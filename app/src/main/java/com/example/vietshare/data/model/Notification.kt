package com.example.vietshare.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

// Using @PropertyName to map Firestore field "read" to data class property "isRead"
data class Notification(
    val notificationId: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val type: String = "",
    val targetId: String = "",
    val message: String = "",

    @get:PropertyName("read")
    @set:PropertyName("read")
    var isRead: Boolean = false, // The name in code is isRead

    val timestamp: Timestamp? = null
)
