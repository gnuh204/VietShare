package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.ChatRepository
import javax.inject.Inject

class FindOrCreateChatRoomUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(targetUserId: String): Result<String> {
        val currentUserId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not logged in"))
        if (currentUserId == targetUserId) {
            return Result.failure(Exception("Cannot create a chat room with yourself"))
        }
        return chatRepository.createChatRoom(currentUserId, targetUserId)
    }
}
