package com.example.vietshare.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.Message
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.ChatRepository
import com.example.vietshare.domain.repository.UserRepository
import com.example.vietshare.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository, // Add UserRepository
    private val sendMessageUseCase: SendMessageUseCase,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomId: String = savedStateHandle.get<String>("roomId")!!

    private val _messageState = MutableStateFlow<MessageState>(MessageState.Loading)
    val messageState: StateFlow<MessageState> = _messageState

    private val _otherUserState = MutableStateFlow<User?>(null)
    val otherUserState: StateFlow<User?> = _otherUserState

    val currentUserId: String? = authRepository.getCurrentUserId()

    var messageContent by mutableStateOf("")
        private set

    init {
        loadMessages()
        loadOtherUserInfo()
    }

    fun onMessageContentChange(newContent: String) {
        messageContent = newContent
    }

    private fun loadOtherUserInfo() {
        val otherUserId = roomId.split("_").find { it != currentUserId }
        if (otherUserId != null) {
            viewModelScope.launch {
                userRepository.getUser(otherUserId).collect {
                    _otherUserState.value = it
                }
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getMessages(roomId)
                .onStart { _messageState.value = MessageState.Loading }
                .catch { e -> _messageState.value = MessageState.Error(e.message ?: "An error occurred") }
                .collect { messages ->
                    _messageState.value = MessageState.Success(messages)
                }
        }
    }

    fun sendMessage() {
        if (messageContent.isBlank()) return

        val contentToSend = messageContent
        messageContent = "" // Clear the input field immediately

        viewModelScope.launch {
            sendMessageUseCase(roomId, contentToSend).onFailure {
                // If sending fails, restore the content to the input field
                messageContent = contentToSend
            }
        }
    }
}

sealed class MessageState {
    object Loading : MessageState()
    data class Success(val messages: List<Message>) : MessageState()
    data class Error(val message: String) : MessageState()
}
