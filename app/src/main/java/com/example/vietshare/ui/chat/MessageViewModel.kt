package com.example.vietshare.ui.chat

import android.net.Uri
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
    private val userRepository: UserRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomId: String = savedStateHandle.get<String>("roomId")!!
    val currentUserId: String? = authRepository.getCurrentUserId()

    private val _messageState = MutableStateFlow<MessageState>(MessageState.Loading)
    val messageState: StateFlow<MessageState> = _messageState

    private val _otherUserState = MutableStateFlow<User?>(null)
    val otherUserState: StateFlow<User?> = _otherUserState

    var messageContent by mutableStateOf("")
        private set
    
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    init {
        loadMessages()
        loadOtherUserInfo()
        markAsRead()
    }

    private fun markAsRead() {
        if (currentUserId != null) {
            viewModelScope.launch {
                chatRepository.markMessagesAsRead(roomId, currentUserId)
            }
        }
    }

    fun onMessageContentChange(newContent: String) {
        messageContent = newContent
    }

    fun onImageSelected(uri: Uri?) {
        selectedImageUri = uri
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
        if (messageContent.isBlank() && selectedImageUri == null) return

        val contentToSend = messageContent.takeIf { it.isNotBlank() }
        val imageToSend = selectedImageUri

        // Clear inputs immediately
        messageContent = ""
        selectedImageUri = null

        viewModelScope.launch {
            sendMessageUseCase(roomId, contentToSend, imageToSend).onFailure {
                // Restore inputs if sending failed
                messageContent = contentToSend ?: ""
                selectedImageUri = imageToSend
            }
        }
    }
}

sealed class MessageState {
    object Loading : MessageState()
    data class Success(val messages: List<Message>) : MessageState()
    data class Error(val message: String) : MessageState()
}
