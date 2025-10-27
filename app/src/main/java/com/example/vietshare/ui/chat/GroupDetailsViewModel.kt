package com.example.vietshare.ui.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
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

data class GroupDetailsUiState(
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val chat: Chat? = null,
    val members: List<User> = emptyList(),
    val groupLeft: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomId: String = savedStateHandle.get<String>("roomId")!!
    val currentUserId = authRepository.getCurrentUserId()

    private val _uiState = MutableStateFlow(GroupDetailsUiState(isLoading = true))
    val uiState: StateFlow<GroupDetailsUiState> = _uiState

    init {
        loadGroupDetails()
    }

    private fun loadGroupDetails() {
        viewModelScope.launch {
            chatRepository.getChatRoom(roomId).flatMapLatest { chat ->
                if (chat == null) {
                    flowOf(GroupDetailsUiState(groupLeft = true))
                } else {
                    userRepository.getUsers(chat.participantIds).map { members ->
                        GroupDetailsUiState(chat = chat, members = members)
                    }
                }
            }.onStart { _uiState.value = _uiState.value.copy(isLoading = true) }
            .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            .collect { state ->
                _uiState.value = state
            }
        }
    }

    fun changeGroupAvatar(uri: Uri) {
        if (_uiState.value.chat?.adminId != currentUserId) return

        _uiState.value = _uiState.value.copy(isUploading = true)
        viewModelScope.launch {
            try {
                val imageUrl = chatRepository.uploadGroupChatImage(uri, roomId).getOrThrow()
                chatRepository.updateGroupImageUrl(roomId, imageUrl).getOrThrow()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isUploading = false)
            }
        }
    }

    fun removeMember(memberId: String) {
        if (_uiState.value.chat?.adminId != currentUserId) return
        if (memberId == currentUserId) return
        
        viewModelScope.launch {
            val result = chatRepository.removeMemberFromGroup(roomId, memberId)
            if(result.isSuccess){
                val removedUser = _uiState.value.members.find { it.userId == memberId }
                val message = "${removedUser?.displayName ?: "A member"} was removed from the group."
                chatRepository.sendSystemMessage(roomId, message)
            }
        }
    }

    fun leaveGroup() {
        if (currentUserId == null) return
        if (_uiState.value.chat?.adminId == currentUserId) return
        
        viewModelScope.launch {
            val result = chatRepository.removeMemberFromGroup(roomId, currentUserId)
            if(result.isSuccess){
                 val currentUser = _uiState.value.members.find { it.userId == currentUserId }
                val message = "${currentUser?.displayName ?: "A member"} left the group."
                chatRepository.sendSystemMessage(roomId, message)
            }
        }
    }
}
