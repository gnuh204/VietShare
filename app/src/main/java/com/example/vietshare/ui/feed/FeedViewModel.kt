package com.example.vietshare.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.domain.model.PostWithUser
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.NotificationRepository
import com.example.vietshare.domain.repository.UserRepository
import com.example.vietshare.domain.usecase.DeletePostUseCase
import com.example.vietshare.domain.usecase.GetFeedPostsUseCase
import com.example.vietshare.domain.usecase.LikePostUseCase
import com.example.vietshare.domain.usecase.UnlikePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getFeedPostsUseCase: GetFeedPostsUseCase,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val likePostUseCase: LikePostUseCase,
    private val unlikePostUseCase: UnlikePostUseCase,
    private val deletePostUseCase: DeletePostUseCase // Add this
) : ViewModel() {

    val currentUserId: String? = authRepository.getCurrentUserId()

    private val _feedState = MutableStateFlow<FeedState>(FeedState.Loading)
    val feedState: StateFlow<FeedState> = _feedState

    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications

    init {
        loadFeed()
        listenForUnreadNotifications()
    }

    private fun listenForUnreadNotifications() {
        currentUserId ?: return
        viewModelScope.launch {
            notificationRepository.hasUnreadNotifications(currentUserId).collect {
                _hasUnreadNotifications.value = it
            }
        }
    }

    private fun loadFeed() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                _feedState.value = FeedState.Error("User not logged in")
                return@launch
            }

            userRepository.getUser(currentUserId)
                .flatMapLatest { currentUser ->
                    if (currentUser == null) {
                        return@flatMapLatest flowOf(FeedState.Error("Could not load user profile"))
                    }

                    val userIdsToFetch = (currentUser.following + currentUserId).distinct()

                    if (userIdsToFetch.size == 1 && userIdsToFetch.contains(currentUserId)) {
                         return@flatMapLatest flowOf(FeedState.Empty("Follow other users to see their posts here."))
                    }

                    getFeedPostsUseCase(userIdsToFetch).flatMapLatest { posts ->
                        if (posts.isEmpty()) {
                            flowOf(FeedState.Empty("No posts from the users you follow yet."))
                        } else {
                            val userIds = posts.map { it.userId }.distinct()
                            userRepository.getUsers(userIds).map { users ->
                                val userMap = users.associateBy { it.userId }
                                val combinedPosts = posts.mapNotNull { post ->
                                    userMap[post.userId]?.let { user -> PostWithUser(post, user) }
                                }
                                FeedState.Success(combinedPosts)
                            }
                        }
                    }
                }
                .onStart { _feedState.value = FeedState.Loading }
                .catch { e -> _feedState.value = FeedState.Error(e.message ?: "An error occurred") }
                .collect { state ->
                    _feedState.value = state
                }
        }
    }

    fun toggleLike(postId: String, postOwnerId: String, isLiked: Boolean) {
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

sealed class FeedState {
    object Loading : FeedState()
    data class Success(val posts: List<PostWithUser>) : FeedState()
    data class Empty(val message: String) : FeedState()
    data class Error(val message: String) : FeedState()
}
