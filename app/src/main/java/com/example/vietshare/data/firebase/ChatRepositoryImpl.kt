package com.example.vietshare.data.firebase

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.vietshare.data.model.Chat
import com.example.vietshare.data.model.ChatType
import com.example.vietshare.data.model.MediaInfo
import com.example.vietshare.data.model.Message
import com.example.vietshare.data.model.MessageType
import com.example.vietshare.domain.repository.ChatRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
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

    override fun getChatRoom(roomId: String): Flow<Chat?> = callbackFlow {
        val listener = firestore.collection("Chats").document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Chat::class.java))
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

    override fun getUnreadChatsCount(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("Chats")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val totalUnreadCount = snapshot?.documents?.sumOf {
                    val chat = it.toObject(Chat::class.java)
                    chat?.unreadCount?.get(userId) ?: 0
                }?.toInt() ?: 0
                trySend(totalUnreadCount)
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

    override suspend fun createGroupChat(groupName: String, memberIds: List<String>, adminId: String): Result<String> = try {
        val roomRef = firestore.collection("Chats").document()
        val allMemberIds = (memberIds + adminId).distinct()
        val unreadCountMap = allMemberIds.associateWith { 0 }

        val newGroup = Chat(
            roomId = roomRef.id,
            type = ChatType.GROUP.name,
            groupName = groupName,
            adminId = adminId,
            participantIds = allMemberIds,
            unreadCount = unreadCountMap,
            lastMessage = "Group created.",
            lastMessageTimestamp = Timestamp.now()
        )
        roomRef.set(newGroup).await()
        Result.success(roomRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addMembersToGroup(roomId: String, memberIds: List<String>): Result<Unit> = try {
        val chatRoomRef = firestore.collection("Chats").document(roomId)
        val updates = mutableMapOf<String, Any>()
        updates["participantIds"] = FieldValue.arrayUnion(*memberIds.toTypedArray())
        memberIds.forEach { memberId ->
            updates["unreadCount.$memberId"] = 0
        }
        chatRoomRef.update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeMemberFromGroup(roomId: String, memberId: String): Result<Unit> = try {
        val chatRoomRef = firestore.collection("Chats").document(roomId)
        val updates = mapOf(
            "participantIds" to FieldValue.arrayRemove(memberId),
            "unreadCount.$memberId" to FieldValue.delete()
        )
        chatRoomRef.update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun sendMessage(message: Message, receiverId: String): Result<Unit> = try {
        val chatRoomRef = firestore.collection("Chats").document(message.roomId)
        val newMessageRef = chatRoomRef.collection("Messages").document()

        firestore.runTransaction { transaction: Transaction ->
            val snapshot = transaction.get(chatRoomRef)
            val chat = snapshot.toObject(Chat::class.java)
            if (chat != null) {
                if(chat.type == ChatType.ONE_TO_ONE.name) {
                    val updates = mapOf(
                        "lastMessage" to if (message.media != null) "[Image]" else message.content,
                        "lastMessageTimestamp" to message.timestamp,
                        "unreadCount.$receiverId" to FieldValue.increment(1)
                    )
                    transaction.set(newMessageRef, message.copy(messageId = newMessageRef.id))
                    transaction.update(chatRoomRef, updates)
                } else { // Group Chat
                    val updates = mutableMapOf<String, Any?>(
                        "lastMessage" to if (message.media != null) "[Image]" else message.content,
                        "lastMessageTimestamp" to message.timestamp
                    )
                    chat.participantIds.forEach {
                        if(it != message.senderId){
                             updates["unreadCount.$it"] = FieldValue.increment(1)
                        }
                    }
                     transaction.set(newMessageRef, message.copy(messageId = newMessageRef.id))
                     transaction.update(chatRoomRef, updates as Map<String, Any>)
                }
            }
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

     override suspend fun sendSystemMessage(roomId: String, content: String): Result<Unit> = try {
        val chatRoomRef = firestore.collection("Chats").document(roomId)
        val newMessageRef = chatRoomRef.collection("Messages").document()

        val message = Message(
            messageId = newMessageRef.id,
            roomId = roomId,
            content = content,
            type = MessageType.SYSTEM.name,
            timestamp = Timestamp.now()
        )

        val batch = firestore.batch()
        batch.set(newMessageRef, message)
        batch.update(chatRoomRef, "lastMessage", content)
        batch.update(chatRoomRef, "lastMessageTimestamp", message.timestamp)
        batch.commit().await()
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

    override suspend fun uploadGroupChatImage(imageUri: Uri, roomId: String): Result<String> = 
        suspendCancellableCoroutine { continuation ->
            mediaManager.upload(imageUri)
                .option("public_id", roomId) 
                .option("folder", "group_avatars")
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

    override suspend fun updateGroupImageUrl(roomId: String, imageUrl: String): Result<Unit> = try {
        firestore.collection("Chats").document(roomId)
            .update("groupImageUrl", imageUrl)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
