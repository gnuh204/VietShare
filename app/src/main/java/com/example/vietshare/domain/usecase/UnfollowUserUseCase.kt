package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.NotificationRepository
import com.example.vietshare.domain.repository.UserRepository
import javax.inject.Inject

class UnfollowUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(targetUserId: String): Result<Unit> {
        val currentUserId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not logged in"))

        val unfollowResult = userRepository.unfollowUser(currentUserId, targetUserId)

        // If unfollowing was successful, delete the corresponding FOLLOW notification
        if (unfollowResult.isSuccess) {
            notificationRepository.deleteNotification(
                recipientId = targetUserId,
                senderId = currentUserId,
                type = "FOLLOW",
                targetId = currentUserId
            )
        }

        return unfollowResult
    }
}
