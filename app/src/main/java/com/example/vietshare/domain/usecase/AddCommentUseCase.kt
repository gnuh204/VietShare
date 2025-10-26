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
    suspend operator fun invoke(postId: String, content: String): Result<Unit> {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId == null) {
            return Result.failure(Exception("User not logged in"))
        }

        return try {
            // 1. Create the comment object
            val newComment = Comment(
                postId = postId,
                senderId = currentUserId,
                content = content,
                timestamp = Timestamp.now()
            )
            
            // 2. Add the comment to the repository
            postRepository.addComment(newComment).getOrThrow()

            // 3. Get post owner to send notification
            val post = postRepository.getPost(postId).first()
            val postOwnerId = post?.userId

            // 4. Send notification if the commenter is not the post owner
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
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
