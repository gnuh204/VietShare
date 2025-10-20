package com.example.vietshare.data.firebase

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mediaManager: MediaManager
) : UserRepository {

    override fun getUser(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("Users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { listener.remove() }
    }

    override fun getUsers(userIds: List<String>): Flow<List<User>> = callbackFlow {
        if (userIds.isNotEmpty()) {
            val listener = firestore.collection("Users").whereIn("userId", userIds)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObjects(User::class.java) ?: emptyList())
                }
            awaitClose { listener.remove() }
        } else {
            trySend(emptyList())
            close()
        }
    }

    override suspend fun searchUsers(query: String, currentUserId: String): Result<List<User>> = try {
        val usersQuery = firestore.collection("Users")
            .orderBy("displayName")
            .startAt(query)
            .endAt(query + '\uf8ff')
            .limit(20)
            .get()
            .await()
        val users = usersQuery.toObjects(User::class.java).filter { it.userId != currentUserId }
        Result.success(users)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateUser(user: User): Result<Unit> = try {
        firestore.collection("Users").document(user.userId).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> = 
        suspendCancellableCoroutine { continuation ->
            mediaManager.upload(imageUri)
                .option("public_id", userId)
                .option("folder", "profile_images")
                .option("overwrite", true)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: MutableMap<Any?, Any?>) {
                         val url = resultData["secure_url"]?.toString()
                        if (url != null) {
                            continuation.resume(Result.success(url))
                        } else {
                            continuation.resume(Result.failure(Exception("Upload succeeded but URL is null")))
                        }
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception(error.description)))
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        }

    override suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> = try {
        firestore.runTransaction {
            val currentUserRef = firestore.collection("Users").document(currentUserId)
            val targetUserRef = firestore.collection("Users").document(targetUserId)

            // For current user: add target to following list and increment count
            it.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserId))
            it.update(currentUserRef, "followingCount", FieldValue.increment(1))

            // For target user: add current user to followers list and increment count
            it.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserId))
            it.update(targetUserRef, "followersCount", FieldValue.increment(1))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> = try {
        firestore.runTransaction {
            val currentUserRef = firestore.collection("Users").document(currentUserId)
            val targetUserRef = firestore.collection("Users").document(targetUserId)

            // For current user: remove target from following list and decrement count
            it.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserId))
            it.update(currentUserRef, "followingCount", FieldValue.increment(-1))

            // For target user: remove current user from followers list and decrement count
            it.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserId))
            it.update(targetUserRef, "followersCount", FieldValue.increment(-1))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
