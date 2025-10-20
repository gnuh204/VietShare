package com.example.vietshare.domain.usecase

import com.example.vietshare.data.model.Message
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.ChatRepository
import com.google.firebase.Timestamp
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(roomId: String, content: String): Result<Unit> {
        val currentUserId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not logged in"))

        if (content.isBlank()) {
            return Result.failure(Exception("Message cannot be empty"))
        }

        val message = Message(
            roomId = roomId,
            senderId = currentUserId,
            content = content,
            timestamp = Timestamp.now()
        )

        return chatRepository.sendMessage(message)
    }
}
