package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.NotificationRepository
import com.example.vietshare.domain.repository.PostRepository
import javax.inject.Inject

class UnlikePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(postId: String, postOwnerId: String): Result<Unit> {
        val currentUserId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not logged in"))

        // Unlike the post
        val unlikeResult = postRepository.unlikePost(postId, currentUserId)

        // If unliking was successful, try to delete the corresponding notification
        if (unlikeResult.isSuccess) {
            notificationRepository.deleteNotification(postOwnerId, currentUserId, "LIKE", postId)
        }

        return unlikeResult
    }
}
