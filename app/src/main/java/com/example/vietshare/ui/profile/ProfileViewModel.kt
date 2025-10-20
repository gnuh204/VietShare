package com.example.vietshare.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.Post
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.PostRepository
import com.example.vietshare.domain.repository.UserRepository
import com.example.vietshare.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val unlikePostUseCase: UnlikePostUseCase,
    private val findOrCreateChatRoomUseCase: FindOrCreateChatRoomUseCase,
    private val deletePostUseCase: DeletePostUseCase, // Add this
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val targetUserId: String = savedStateHandle.get<String>("userId")!!
    val currentUserId = authRepository.getCurrentUserId()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val userFlow = userRepository.getUser(targetUserId)
            val postsFlow = postRepository.getPosts(targetUserId)

            userFlow.combine(postsFlow) { user, posts ->
                if (user == null) {
                    ProfileState.Error("User not found")
                } else {
                    val isFollowing = user.followers.contains(currentUserId)
                    ProfileState.Success(user, posts, isFollowing, currentUserId == targetUserId)
                }
            }.collect { state ->
                _profileState.value = state
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val currentState = (_profileState.value as? ProfileState.Success) ?: return@launch
            if (currentState.isFollowing) {
                unfollowUserUseCase(targetUserId)
            } else {
                followUserUseCase(targetUserId)
            }
        }
    }

    suspend fun startChat(): Result<String> {
        return findOrCreateChatRoomUseCase(targetUserId)
    }

    fun toggleLike(postId: String, isLiked: Boolean, postOwnerId: String) {
        viewModelScope.launch {
            if (isLiked) {
                unlikePostUseCase(postId, postOwnerId)
            } else {
                likePostUseCase(postId, postOwnerId)
            }
        }
    }

    fun deletePost(postId: String) { // Add this function
        viewModelScope.launch {
            deletePostUseCase(postId)
        }
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(
        val user: User,
        val posts: List<Post>,
        val isFollowing: Boolean,
        val isCurrentUserProfile: Boolean
    ) : ProfileState()

    data class Error(val message: String) : ProfileState()
}
