package com.example.vietshare.domain.repository

import com.example.vietshare.data.model.Chat
import com.example.vietshare.data.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatRooms(userId: String): Flow<List<Chat>>
    fun getMessages(roomId: String): Flow<List<Message>>
    suspend fun createChatRoom(userId1: String, userId2: String): Result<String> // Returns roomId
    suspend fun sendMessage(message: Message): Result<Unit>
}
