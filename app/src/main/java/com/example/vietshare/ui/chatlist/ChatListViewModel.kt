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

// Data class to combine a Chat room with the other user's info
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

    init {
        loadChatList()
    }

    private fun loadChatList() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch

            chatRepository.getChatRooms(currentUserId)
                .flatMapLatest { chats ->
                    if (chats.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val otherUserIds = chats.mapNotNull { chat ->
                            chat.participantIds.find { it != currentUserId }
                        }.distinct()

                        userRepository.getUsers(otherUserIds).map { users ->
                            val userMap = users.associateBy { it.userId }
                            chats.mapNotNull { chat ->
                                val otherUserId = chat.participantIds.find { it != currentUserId }
                                userMap[otherUserId]?.let { otherUser ->
                                    ChatWithUserInfo(chat, otherUser)
                                }
                            }
                        }
                    }
                }
                .onStart { _chatListState.value = ChatListState.Loading }
                .catch { e -> _chatListState.value = ChatListState.Error(e.message ?: "An error occurred") }
                .collect { result ->
                    _chatListState.value = ChatListState.Success(result)
                }
        }
    }
}
