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
    suspend operator fun invoke(postId: String, postOwnerId: String): Result<Unit> {
        val currentUserId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not logged in"))

        // Like the post first
        val likeResult = postRepository.likePost(postId, currentUserId)

        // If liking was successful and it's not your own post, send a notification
        if (likeResult.isSuccess && currentUserId != postOwnerId) {
            val notification = Notification(
                recipientId = postOwnerId,
                senderId = currentUserId,
                type = "LIKE",
                targetId = postId,
                timestamp = Timestamp.now()
            )
            notificationRepository.sendNotification(notification)
        }

        return likeResult
    }
}
