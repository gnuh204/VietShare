package com.example.vietshare.domain.usecase

import com.example.vietshare.data.model.Notification
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.NotificationRepository
import com.example.vietshare.domain.repository.UserRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class FollowUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(targetUserId: String) {
        val currentUserId = authRepository.getCurrentUserId() ?: return

        // 1. Check if already following
        val currentUser = userRepository.getUser(currentUserId).first()
        if (currentUser?.following?.contains(targetUserId) == true) {
            // Already following, do nothing
            return
        }

        // 2. If not following, proceed with the follow action
        val followResult = userRepository.followUser(currentUserId, targetUserId)

        // 3. Send notification only if the follow action was successful
        if (followResult.isSuccess) {
            val notification = Notification(
                recipientId = targetUserId,
                senderId = currentUserId,
                type = "FOLLOW",
                targetId = currentUserId, // For a follow, the target is the user themselves
                isRead = false,
                timestamp = Timestamp.now()
            )
            notificationRepository.sendNotification(notification)
        }
    }
}
