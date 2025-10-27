package com.example.vietshare.data.firebase

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.vietshare.data.model.Comment
import com.example.vietshare.data.model.MediaInfo
import com.example.vietshare.data.model.Post
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.PostRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mediaManager: MediaManager,
    private val authRepository: AuthRepository
) : PostRepository {

    override fun getPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("Posts").whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Post::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getFeedPosts(userIds: List<String>): Flow<List<Post>> = callbackFlow {
        if (userIds.isNotEmpty()) {
            val listener = firestore.collection("Posts").whereIn("userId", userIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObjects(Post::class.java) ?: emptyList())
                }
            awaitClose { listener.remove() }
        } else {
            trySend(emptyList())
            close()
        }
    }

    override fun getPost(postId: String): Flow<Post?> = callbackFlow {
        val listener = firestore.collection("Posts").document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Post::class.java))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createPost(post: Post): Result<Unit> = try {
        firestore.collection("Posts").document(post.postId).set(post).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun uploadPostImage(imageUri: Uri, postId: String): Result<MediaInfo> = 
        suspendCancellableCoroutine { continuation ->
            val publicId = UUID.randomUUID().toString()
            mediaManager.upload(imageUri)
                .option("public_id", publicId)
                .option("folder", "post_images/$postId")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: MutableMap<Any?, Any?>) {
                        val url = resultData["secure_url"]?.toString()
                        val returnedPublicId = resultData["public_id"]?.toString()

                        if (url != null && returnedPublicId != null) {
                            val mediaInfo = MediaInfo(url = url, publicId = returnedPublicId)
                            continuation.resume(Result.success(mediaInfo))
                        } else {
                            continuation.resume(Result.failure(Exception("Upload succeeded but URL or Public ID is null")))
                        }
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception(error.description)))
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        }

    override suspend fun likePost(postId: String, userId: String): Result<Unit> = try {
        firestore.collection("Posts").document(postId)
            .update("likes", FieldValue.arrayUnion(userId)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> = try {
        firestore.collection("Posts").document(postId)
            .update("likes", FieldValue.arrayRemove(userId)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection("Comments").whereEqualTo("postId", postId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Error getting comments", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Comment::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(comment: Comment): Result<Unit> = try {
        firestore.runTransaction {
            val newCommentRef = firestore.collection("Comments").document()
            val commentWithId = comment.copy(commentId = newCommentRef.id)
            it.set(newCommentRef, commentWithId)

            val postRef = firestore.collection("Posts").document(comment.postId)
            it.update(postRef, "commentCount", FieldValue.increment(1))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun toggleCommentReaction(postId: String, commentId: String, reaction: String, userId: String): Result<Unit> = try {
        val commentRef = firestore.collection("Comments").document(commentId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val comment = snapshot.toObject(Comment::class.java) ?: throw Exception("Comment not found")
            
            val currentReactions = comment.reactions.toMutableMap()
            val userList = currentReactions[reaction]?.toMutableList() ?: mutableListOf()
            
            if (userList.contains(userId)) {
                userList.remove(userId)
            } else {
                userList.add(userId)
            }

            if (userList.isEmpty()) {
                currentReactions.remove(reaction)
            } else {
                currentReactions[reaction] = userList
            }

            transaction.update(commentRef, "reactions", currentReactions)
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deletePost(postId: String): Result<Unit> = try {
        firestore.collection("Posts").document(postId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun deletePostImage(publicId: String): Result<Unit> = try {
        withContext(Dispatchers.IO) {
            mediaManager.getCloudinary().uploader().destroy(publicId, emptyMap<String, Any>())
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Log.w("PostRepositoryImpl", "Failed to delete image from Cloudinary: ${e.message}")
        Result.success(Unit)
    }

    override suspend fun deleteCommentsByPostId(postId: String): Result<Unit> = try {
        val commentsToDelete = firestore.collection("Comments")
            .whereEqualTo("postId", postId)
            .get()
            .await()

        if (!commentsToDelete.isEmpty) {
            val batch: WriteBatch = firestore.batch()
            for (document in commentsToDelete.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = try {
        val commentRef = firestore.collection("Comments").document(commentId)

        // Recursively delete replies first
        val repliesSnapshot = firestore.collection("Comments")
            .whereEqualTo("parentId", commentId)
            .get()
            .await()

        firestore.runBatch { batch ->
            for (document in repliesSnapshot.documents) {
                batch.delete(document.reference)
            }
            batch.delete(commentRef) // Delete the main comment
            
            // Decrement post's comment count
            val postRef = firestore.collection("Posts").document(postId)
            batch.update(postRef, "commentCount", FieldValue.increment(-(1 + repliesSnapshot.size().toLong())))

        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
