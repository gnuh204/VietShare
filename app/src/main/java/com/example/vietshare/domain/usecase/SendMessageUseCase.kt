package com.example.vietshare.domain.usecase

import android.net.Uri
import com.example.vietshare.data.model.MediaInfo
import com.example.vietshare.data.model.Message
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.ChatRepository
import com.google.firebase.Timestamp
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(roomId: String, content: String?, imageUri: Uri?): Result<Unit> {
        val currentUserId = authRepository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        if (content.isNullOrBlank() && imageUri == null) {
            return Result.failure(Exception("Cannot send an empty message"))
        }
        
        return try {
            var mediaInfo: MediaInfo? = null
            imageUri?.let {
                mediaInfo = chatRepository.uploadChatImage(it, roomId).getOrThrow()
            }

            val message = Message(
                roomId = roomId,
                senderId = currentUserId,
                content = content,
                media = mediaInfo,
                timestamp = Timestamp.now()
            )
            
            // The receiverId is only needed for 1-on-1 chats to increment the counter.
            // For group chats, the logic is handled differently in the repository.
            val receiverId = roomId.split("_").find { it != currentUserId } ?: ""

            chatRepository.sendMessage(message, receiverId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
