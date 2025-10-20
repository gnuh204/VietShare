package com.example.vietshare.domain.model

import com.example.vietshare.data.model.Notification
import com.example.vietshare.data.model.User

data class NotificationWithSender(
    val notification: Notification,
    val sender: User
)
