package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.UserRepository
import javax.inject.Inject

class FollowUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val sendNotificationUseCase: SendNotificationUseCase
) {
    suspend operator fun invoke(targetUserId: String): Result<Unit> {
        val currentUserId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not logged in"))

        if (currentUserId == targetUserId) {
            return Result.failure(Exception("Cannot follow yourself"))
        }

        val followResult = userRepository.followUser(currentUserId, targetUserId)

        if (followResult.isSuccess) {
            sendNotificationUseCase(
                recipientId = targetUserId,
                type = "FOLLOW",
                targetId = currentUserId // The person who followed
            )
        }

        return followResult
    }
}
