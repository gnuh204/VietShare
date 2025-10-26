package com.example.vietshare.data.firebase

import android.util.Log
import com.example.vietshare.data.model.Notification
import com.example.vietshare.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    override fun getNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = firestore.collection("Notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Notification::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getUnreadNotificationCount(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("Notifications")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("read", false) // THE FIX: Use "read" to match Firestore
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Error getting notification count", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendNotification(notification: Notification): Result<Unit> = try {
        val newDocRef = firestore.collection("Notifications").document()
        newDocRef.set(notification.copy(notificationId = newDocRef.id)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> = try {
        val unreadNotifications = firestore.collection("Notifications")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("read", false) // THE FIX: Use "read" to match Firestore
            .get()
            .await()

        if (!unreadNotifications.isEmpty) {
            val batch: WriteBatch = firestore.batch()
            for (document in unreadNotifications.documents) {
                batch.update(document.reference, "read", true) // THE FIX: Use "read" to match Firestore
            }
            batch.commit().await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteNotification(recipientId: String, senderId: String, type: String, targetId: String): Result<Unit> = try {
        val query = firestore.collection("Notifications")
            .whereEqualTo("recipientId", recipientId)
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("type", type)
            .whereEqualTo("targetId", targetId)
            .limit(1)
            .get()
            .await()
        
        if (!query.isEmpty) {
            query.documents.first().reference.delete().await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteNotificationsForPost(postId: String): Result<Unit> = try {
        val notificationsToDelete = firestore.collection("Notifications")
            .whereEqualTo("targetId", postId)
            .get()
            .await()

        if (!notificationsToDelete.isEmpty) {
            val batch: WriteBatch = firestore.batch()
            for (document in notificationsToDelete.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
