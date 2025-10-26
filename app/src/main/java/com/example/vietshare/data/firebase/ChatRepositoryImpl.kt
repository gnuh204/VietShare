package com.example.vietshare.data.firebase

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.vietshare.data.model.Chat
import com.example.vietshare.data.model.MediaInfo
import com.example.vietshare.data.model.Message
import com.example.vietshare.domain.repository.ChatRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mediaManager: MediaManager
) : ChatRepository {

    override fun getChatRooms(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection("Chats")
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Chat::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getMessages(roomId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("Chats").document(roomId)
            .collection("Messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Message::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createChatRoom(userId1: String, userId2: String): Result<String> = try {
        val participants = listOf(userId1, userId2).sorted()
        val roomId = participants.joinToString(separator = "_")

        val roomRef = firestore.collection("Chats").document(roomId)
        val existingRoom = roomRef.get().await()

        if (existingRoom.exists()) {
            Result.success(roomId)
        } else {
            val newChatRoom = Chat(
                roomId = roomId,
                participantIds = participants,
                unreadCount = mapOf(userId1 to 0, userId2 to 0)
            )
            roomRef.set(newChatRoom).await()
            Result.success(roomId)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun sendMessage(message: Message, receiverId: String): Result<Unit> = try {
        val chatRoomRef = firestore.collection("Chats").document(message.roomId)
        val newMessageRef = chatRoomRef.collection("Messages").document()

        firestore.runTransaction { transaction: Transaction ->
            val snapshot = transaction.get(chatRoomRef)
            val unreadCount = snapshot.get("unreadCount") as? MutableMap<String, Long> ?: mutableMapOf()

            val currentUnread = unreadCount[receiverId] ?: 0
            unreadCount[receiverId] = currentUnread + 1

            transaction.set(newMessageRef, message.copy(messageId = newMessageRef.id))
            transaction.update(
                chatRoomRef,
                "lastMessage", if (message.media != null) "[Image]" else message.content,
                "lastMessageTimestamp", message.timestamp,
                "unreadCount", unreadCount
            )
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun markMessagesAsRead(roomId: String, userId: String): Result<Unit> = try {
        val updates = mapOf("unreadCount.$userId" to 0)
        firestore.collection("Chats").document(roomId).update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun uploadChatImage(imageUri: Uri, roomId: String): Result<MediaInfo> = 
        suspendCancellableCoroutine { continuation ->
            val publicId = UUID.randomUUID().toString()
            mediaManager.upload(imageUri)
                .option("public_id", publicId)
                .option("folder", "chat_images/$roomId")
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
}
