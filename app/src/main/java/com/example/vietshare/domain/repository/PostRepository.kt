package com.example.vietshare.domain.repository

import android.net.Uri
import com.example.vietshare.data.model.Comment
import com.example.vietshare.data.model.MediaInfo
import com.example.vietshare.data.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPosts(userId: String): Flow<List<Post>>
    fun getFeedPosts(userIds: List<String>): Flow<List<Post>>
    fun getPost(postId: String): Flow<Post?>
    fun getComments(postId: String): Flow<List<Comment>>

    suspend fun createPost(post: Post): Result<Unit>
    suspend fun uploadPostImage(imageUri: Uri, postId: String): Result<MediaInfo>
    suspend fun likePost(postId: String, userId: String): Result<Unit>
    suspend fun unlikePost(postId: String, userId: String): Result<Unit>
    suspend fun addComment(comment: Comment): Result<Unit>
    suspend fun toggleCommentReaction(postId: String, commentId: String, reaction: String, userId: String): Result<Unit> // New function
    
    // Functions for deletion
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun deletePostImage(publicId: String): Result<Unit> // New
    suspend fun deleteCommentsByPostId(postId: String): Result<Unit> // New
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>
}
