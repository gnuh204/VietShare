package com.example.vietshare.domain.repository

import android.net.Uri
import com.example.vietshare.data.model.Chat
import com.example.vietshare.data.model.MediaInfo
import com.example.vietshare.data.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatRooms(userId: String): Flow<List<Chat>>
    fun getMessages(roomId: String): Flow<List<Message>>

    suspend fun createChatRoom(userId1: String, userId2: String): Result<String>
    suspend fun sendMessage(message: Message, receiverId: String): Result<Unit>
    suspend fun markMessagesAsRead(roomId: String, userId: String): Result<Unit>
    suspend fun uploadChatImage(imageUri: Uri, roomId: String): Result<MediaInfo> // Add this
}
