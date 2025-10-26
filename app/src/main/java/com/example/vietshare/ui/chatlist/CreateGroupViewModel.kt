package com.example.vietshare.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.ChatRepository
import com.example.vietshare.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateGroupUiState(
    val isLoading: Boolean = false,
    val friends: List<User> = emptyList(),
    val selectedMembers: Set<String> = emptySet(),
    val groupCreated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            val currentUser = userRepository.getUser(currentUserId).first()
            if (currentUser != null && currentUser.following.isNotEmpty()) {
                val friends = userRepository.getUsers(currentUser.following).first()
                _uiState.value = _uiState.value.copy(isLoading = false, friends = friends)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onMemberSelected(userId: String) {
        val currentSelected = _uiState.value.selectedMembers.toMutableSet()
        if (currentSelected.contains(userId)) {
            currentSelected.remove(userId)
        } else {
            currentSelected.add(userId)
        }
        _uiState.value = _uiState.value.copy(selectedMembers = currentSelected)
    }

    fun createGroup(groupName: String) {
        // TODO: Implement group creation logic
    }
}
