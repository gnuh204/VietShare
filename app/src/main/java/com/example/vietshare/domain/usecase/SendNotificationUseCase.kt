package com.example.vietshare.domain.usecase

import com.example.vietshare.data.model.Notification
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.NotificationRepository
import com.google.firebase.Timestamp
import javax.inject.Inject

class SendNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(recipientId: String, type: String, targetId: String): Result<Unit> {
        val senderId = authRepository.getCurrentUserId()
        if (senderId == null || senderId == recipientId) {
            return Result.failure(Exception("Invalid sender or recipient."))
        }

        val notification = Notification(
            senderId = senderId,
            recipientId = recipientId,
            type = type,
            targetId = targetId, // Changed from resourceId
            timestamp = Timestamp.now()
        )

        return notificationRepository.sendNotification(notification)
    }
}
