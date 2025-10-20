package com.example.vietshare.data.firebase

import com.example.vietshare.data.model.Chat
import com.example.vietshare.data.model.Message
import com.example.vietshare.domain.repository.ChatRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
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

    override suspend fun createChatRoom(userId1: String, userId2: String): Result<String> {
        return try {
            val participants = listOf(userId1, userId2).sorted()
            val roomId = participants.joinToString(separator = "_")

            val roomRef = firestore.collection("Chats").document(roomId)
            val existingRoom = roomRef.get().await()

            if (existingRoom.exists()) {
                Result.success(roomId)
            } else {
                val newChatRoom = Chat(
                    roomId = roomId,
                    participantIds = participants
                )
                roomRef.set(newChatRoom).await()
                Result.success(roomId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(message: Message): Result<Unit> = try {
        val chatRoomRef = firestore.collection("Chats").document(message.roomId)
        val newMessageRef = chatRoomRef.collection("Messages").document()

        firestore.runBatch {
            // 1. Add the new message
            it.set(newMessageRef, message.copy(messageId = newMessageRef.id))

            // 2. Update the parent chat document
            val updates = mapOf(
                "lastMessage" to message.content,
                "lastMessageTimestamp" to message.timestamp
            )
            it.update(chatRoomRef, updates)
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
