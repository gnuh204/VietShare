package com.example.vietshare.ui.findfriends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.UserRepository
import com.example.vietshare.domain.usecase.FollowUserUseCase
import com.example.vietshare.domain.usecase.UnfollowUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data class to combine a User with their follow state
data class UserWithFollowState(
    val user: User,
    val isFollowing: Boolean
)

// UI State for the entire screen
data class FindFriendsUiState(
    val searchResults: List<UserWithFollowState> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class FindFriendsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FindFriendsUiState())
    val uiState: StateFlow<FindFriendsUiState> = _uiState

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = FindFriendsUiState()
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce to avoid rapid API calls

            val currentUserId = authRepository.getCurrentUserId() ?: return@launch

            val currentUserFlow = userRepository.getUser(currentUserId)
            
            // This flow is now fully reactive
            val searchResultFlow = flow {
                userRepository.searchUsers(query, currentUserId)
                    .onSuccess { emit(it) }
                    .onFailure { throw it }
            }

            searchResultFlow.combine(currentUserFlow) { searchResults, currentUser ->
                if (currentUser == null) return@combine emptyList()
                searchResults.map {
                    UserWithFollowState(
                        user = it,
                        isFollowing = currentUser.following.contains(it.userId)
                    )
                }
            }
            .onStart { _uiState.value = _uiState.value.copy(isLoading = true) }
            .catch { e -> _uiState.value = FindFriendsUiState(message = e.message) }
            .collect { results ->
                _uiState.value = FindFriendsUiState(searchResults = results)
            }
        }
    }

    fun toggleFollow(targetUser: UserWithFollowState) {
        viewModelScope.launch {
            if (targetUser.isFollowing) {
                unfollowUserUseCase(targetUser.user.userId)
            } else {
                followUserUseCase(targetUser.user.userId)
            }
            // The UI will update automatically because we are observing the currentUserFlow
        }
    }
}
