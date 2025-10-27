package com.example.vietshare.domain.repository

import android.net.Uri
import com.example.vietshare.data.model.Chat
import com.example.vietshare.data.model.MediaInfo
import com.example.vietshare.data.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatRooms(userId: String): Flow<List<Chat>>
    fun getChatRoom(roomId: String): Flow<Chat?>
    fun getMessages(roomId: String): Flow<List<Message>>
    fun getUnreadChatsCount(userId: String): Flow<Int> // New function

    suspend fun createChatRoom(userId1: String, userId2: String): Result<String>
    suspend fun createGroupChat(groupName: String, memberIds: List<String>, adminId: String): Result<String>
    suspend fun addMembersToGroup(roomId: String, memberIds: List<String>): Result<Unit>
    suspend fun removeMemberFromGroup(roomId: String, memberId: String): Result<Unit>
    suspend fun sendMessage(message: Message, receiverId: String): Result<Unit>
    suspend fun sendSystemMessage(roomId: String, content: String): Result<Unit>
    suspend fun markMessagesAsRead(roomId: String, userId: String): Result<Unit>
    suspend fun uploadChatImage(imageUri: Uri, roomId: String): Result<MediaInfo>
    suspend fun uploadGroupChatImage(imageUri: Uri, roomId: String): Result<String>
    suspend fun updateGroupImageUrl(roomId: String, imageUrl: String): Result<Unit>
    suspend fun deleteMessage(roomId: String, messageId: String): Result<Unit>
    suspend fun deleteChat(roomId: String): Result<Unit>
}
