package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.NotificationRepository
import com.example.vietshare.domain.repository.PostRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class DeletePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(postId: String): Result<Unit> = try {
        coroutineScope {
            // 1. Get the post to find out which media files need to be deleted
            val post = postRepository.getPost(postId).firstOrNull()

            // 2. Delete images from Cloudinary concurrently
            post?.media?.forEach { mediaInfo ->
                async { postRepository.deletePostImage(mediaInfo.publicId) }
            }

            // 3. Delete all comments for the post
            val deleteCommentsJob = async { postRepository.deleteCommentsByPostId(postId) }

            // 4. Delete all notifications for the post (likes, comments, etc.)
            val deleteNotificationsJob = async { notificationRepository.deleteNotificationsForPost(postId) }

            // Wait for dependent deletions to complete
            deleteCommentsJob.await()
            deleteNotificationsJob.await()

            // 5. Finally, delete the post document itself
            postRepository.deletePost(postId).getOrThrow()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
