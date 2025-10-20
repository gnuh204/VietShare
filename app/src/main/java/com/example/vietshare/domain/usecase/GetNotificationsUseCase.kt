package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(userId: String) = notificationRepository.getNotifications(userId)
}
