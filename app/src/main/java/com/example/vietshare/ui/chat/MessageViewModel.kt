package com.example.vietshare.ui.chat

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.Chat
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


sealed class UiState {
    object Loading : UiState()
    data class Success(val messages: List<Message>, val members: Map<String, User>, val chat: Chat?) : UiState()
    data class Error(val message: String) : UiState()
}

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

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    var messageContent by mutableStateOf("")
        private set
    
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    init {
        loadChatInfo()
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

    private fun loadChatInfo() {
        viewModelScope.launch {
            chatRepository.getChatRoom(roomId).flatMapLatest { chat ->
                if(chat == null) return@flatMapLatest flowOf(UiState.Error("Chat room not found"))

                val memberIds = chat.participantIds
                val messagesFlow = chatRepository.getMessages(roomId)
                val membersFlow = userRepository.getUsers(memberIds)

                messagesFlow.combine(membersFlow) { messages, members ->
                    val memberMap = members.associateBy { it.userId }
                    UiState.Success(messages, memberMap, chat)
                }
            }
            .onStart { _uiState.value = UiState.Loading }
            .catch { e -> _uiState.value = UiState.Error(e.message ?: "An error occurred") }
            .collect{ state ->
                 _uiState.value = state
            }
        }
    }

    fun sendMessage() {
        if (messageContent.isBlank() && selectedImageUri == null) return

        val contentToSend = messageContent.takeIf { it.isNotBlank() }
        val imageToSend = selectedImageUri

        messageContent = ""
        selectedImageUri = null

        viewModelScope.launch {
            sendMessageUseCase(roomId, contentToSend, imageToSend).onFailure {
                messageContent = contentToSend ?: ""
                selectedImageUri = imageToSend
            }
        }
    }
}
