package com.example.vietshare.domain.usecase

import com.example.vietshare.data.model.Comment
import com.example.vietshare.data.model.Notification
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.NotificationRepository
import com.example.vietshare.domain.repository.PostRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AddCommentUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(postId: String, content: String, parentId: String? = null): Result<Unit> {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId == null) {
            return Result.failure(Exception("User not logged in"))
        }

        return try {
            val newComment = Comment(
                postId = postId,
                senderId = currentUserId,
                parentId = parentId, // Set the parent ID
                content = content,
                timestamp = Timestamp.now()
            )
            
            postRepository.addComment(newComment).getOrThrow()

            val post = postRepository.getPost(postId).first()
            val postOwnerId = post?.userId

            if (postOwnerId != null && postOwnerId != currentUserId) {
                val notification = Notification(
                    recipientId = postOwnerId,
                    senderId = currentUserId,
                    type = "COMMENT",
                    targetId = postId,
                    isRead = false,
                    timestamp = Timestamp.now()
                )
                notificationRepository.sendNotification(notification)
            }
            
            // TODO: Notify parent comment owner in the future

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
