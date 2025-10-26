package com.example.vietshare.domain.usecase

import com.example.vietshare.data.model.Notification
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.NotificationRepository
import com.example.vietshare.domain.repository.PostRepository
import com.google.firebase.Timestamp
import javax.inject.Inject

class LikePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(postId: String, postOwnerId: String) {
        val currentUserId = authRepository.getCurrentUserId() ?: return
        
        // User cannot like their own post
        if (currentUserId == postOwnerId) return

        val likeResult = postRepository.likePost(postId, currentUserId)

        if (likeResult.isSuccess) {
            val notification = Notification(
                recipientId = postOwnerId,
                senderId = currentUserId,
                type = "LIKE",
                targetId = postId,
                isRead = false, // Set isRead to false
                timestamp = Timestamp.now()
            )
            notificationRepository.sendNotification(notification)
        }
    }
}
