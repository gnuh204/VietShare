package com.example.vietshare.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.Chat
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.ChatRepository
import com.example.vietshare.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatWithUserInfo(
    val chat: Chat,
    val otherUser: User
)

sealed class ChatListState {
    object Loading : ChatListState()
    data class Success(val chats: List<ChatWithUserInfo>) : ChatListState()
    data class Error(val message: String) : ChatListState()
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _chatListState = MutableStateFlow<ChatListState>(ChatListState.Loading)
    val chatListState: StateFlow<ChatListState> = _chatListState

    val currentUserId = authRepository.getCurrentUserId()

    init {
        loadChatRooms()
    }

    private fun loadChatRooms() {
        if (currentUserId == null) {
            _chatListState.value = ChatListState.Error("User not logged in.")
            return
        }

        viewModelScope.launch {
            chatRepository.getChatRooms(currentUserId)
                .flatMapLatest { chats ->
                    if (chats.isEmpty()) {
                        return@flatMapLatest flowOf(emptyList<ChatWithUserInfo>())
                    }

                    val otherUserIds = chats.mapNotNull { chat ->
                        chat.participantIds.find { it != currentUserId }
                    }.distinct()

                    userRepository.getUsers(otherUserIds).map { users ->
                        val userMap = users.associateBy { it.userId }
                        chats.mapNotNull { chat ->
                            val otherUserId = chat.participantIds.find { it != currentUserId }
                            userMap[otherUserId]?.let {
                                ChatWithUserInfo(chat, it)
                            }
                        }
                    }
                }
                .onStart { _chatListState.value = ChatListState.Loading }
                .catch { e -> _chatListState.value = ChatListState.Error(e.message ?: "An error occurred") }
                .collect { chatList ->
                    _chatListState.value = ChatListState.Success(chatList)
                }
        }
    }

    fun deleteChat(roomId: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(roomId)
        }
    }
}
