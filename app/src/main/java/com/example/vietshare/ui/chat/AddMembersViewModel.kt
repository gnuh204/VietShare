package com.example.vietshare.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.ChatRepository
import com.example.vietshare.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddMembersUiState(
    val isLoading: Boolean = false,
    val potentialMembers: List<User> = emptyList(),
    val selectedMembers: Set<String> = emptySet(),
    val membersAdded: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddMembersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomId: String = savedStateHandle.get<String>("roomId")!!

    private val _uiState = MutableStateFlow(AddMembersUiState())
    val uiState: StateFlow<AddMembersUiState> = _uiState

    init {
        loadPotentialMembers()
    }

    private fun loadPotentialMembers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch

            val friendsFlow = userRepository.getUser(currentUserId).map { it?.following ?: emptyList() }
            val groupMembersFlow = chatRepository.getChatRoom(roomId).map { it?.participantIds ?: emptyList() }

            // Combine the two lists of IDs to get a flow of potential member IDs
            val potentialMemberIdsFlow = friendsFlow.combine(groupMembersFlow) { friendIds, memberIds ->
                friendIds.filter { !memberIds.contains(it) }
            }

            // Use flatMapLatest to switch to the user-fetching flow
            potentialMemberIdsFlow.flatMapLatest { potentialIds ->
                if (potentialIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    userRepository.getUsers(potentialIds)
                }
            }
            .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            .collect { potentialMembers ->
                _uiState.value = _uiState.value.copy(isLoading = false, potentialMembers = potentialMembers)
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

    fun addSelectedMembers() {
        if (_uiState.value.selectedMembers.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val memberIdsToAdd = _uiState.value.selectedMembers.toList()
                chatRepository.addMembersToGroup(roomId, memberIdsToAdd).getOrThrow()

                // Send system message
                val addedUsers = _uiState.value.potentialMembers.filter { memberIdsToAdd.contains(it.userId) }
                val addedUsersString = addedUsers.joinToString(", ") { it.displayName }
                val systemMessage = "$addedUsersString was added to the group."
                chatRepository.sendSystemMessage(roomId, systemMessage)

                _uiState.value = _uiState.value.copy(isLoading = false, membersAdded = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
