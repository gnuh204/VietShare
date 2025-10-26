package com.example.vietshare.domain.repository

import com.example.vietshare.data.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(userId: String): Flow<List<Notification>>
    fun getUnreadNotificationCount(userId: String): Flow<Int> // Changed from hasUnreadNotifications
    suspend fun sendNotification(notification: Notification): Result<Unit>
    suspend fun markAllAsRead(userId: String): Result<Unit>
    suspend fun deleteNotification(recipientId: String, senderId: String, type: String, targetId: String): Result<Unit>
    suspend fun deleteNotificationsForPost(postId: String): Result<Unit>
}
