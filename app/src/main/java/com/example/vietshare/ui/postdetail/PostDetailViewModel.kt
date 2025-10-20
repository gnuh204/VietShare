package com.example.vietshare.ui.postdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.Comment
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.model.PostWithUser
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.PostRepository
import com.example.vietshare.domain.repository.UserRepository
import com.example.vietshare.domain.usecase.DeletePostUseCase
import com.example.vietshare.domain.usecase.LikePostUseCase
import com.example.vietshare.domain.usecase.UnlikePostUseCase
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommentWithUser(
    val comment: Comment,
    val user: User
)

sealed class PostDetailState {
    object Loading : PostDetailState()
    data class Success(
        val post: PostWithUser,
        val comments: List<CommentWithUser>,
        val postDeleted: Boolean = false
    ) : PostDetailState()
    data class Error(val message: String) : PostDetailState()
}

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val likePostUseCase: LikePostUseCase,
    private val unlikePostUseCase: UnlikePostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: String = savedStateHandle.get<String>("postId")!!
    val currentUserId = authRepository.getCurrentUserId()

    private val _postDetailState = MutableStateFlow<PostDetailState>(PostDetailState.Loading)
    val postDetailState: StateFlow<PostDetailState> = _postDetailState

    var commentContent by mutableStateOf("")
        private set

    init {
        loadContent()
    }

    fun onCommentContentChange(content: String) {
        commentContent = content
    }

    private fun loadContent() {
        viewModelScope.launch {
            val postFlow = postRepository.getPost(postId).flatMapLatest { post ->
                if (post == null) return@flatMapLatest flowOf(null)
                userRepository.getUser(post.userId).map { user ->
                    user?.let { PostWithUser(post, it) }
                }
            }

            val commentsFlow = postRepository.getComments(postId).flatMapLatest { comments: List<Comment> ->
                if (comments.isEmpty()) {
                    flowOf(emptyList<CommentWithUser>())
                } else {
                    val userIds = comments.map { it.senderId }.distinct()
                    userRepository.getUsers(userIds).map { users ->
                        val userMap = users.associateBy { it.userId }
                        comments.mapNotNull { comment ->
                            userMap[comment.senderId]?.let { user ->
                                CommentWithUser(comment, user)
                            }
                        }
                    }
                }
            }

            postFlow.combine(commentsFlow) { postWithUser, commentsWithUsers ->
                if (postWithUser == null) {
                    PostDetailState.Error("Post not found")
                } else {
                    PostDetailState.Success(postWithUser, commentsWithUsers)
                }
            }
            .onStart { _postDetailState.value = PostDetailState.Loading }
            .catch { e -> _postDetailState.value = PostDetailState.Error(e.message ?: "An error occurred") }
            .collect { state ->
                _postDetailState.value = state
            }
        }
    }

    fun addComment() {
        if (commentContent.isBlank() || currentUserId == null) return
        val newComment = Comment(
            postId = postId,
            senderId = currentUserId,
            content = commentContent,
            timestamp = Timestamp.now() // Add timestamp
        )
        viewModelScope.launch {
            postRepository.addComment(newComment)
            commentContent = ""
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            val currentState = (_postDetailState.value as? PostDetailState.Success) ?: return@launch
            val post = currentState.post.post
            val isLiked = post.likes.contains(currentUserId)

            if (isLiked) {
                unlikePostUseCase(post.postId, post.userId)
            } else {
                likePostUseCase(post.postId, post.userId)
            }
        }
    }

    fun deletePost() {
        viewModelScope.launch {
            val result = deletePostUseCase(postId)
            if (result.isSuccess) {
                val currentState = _postDetailState.value
                if (currentState is PostDetailState.Success) {
                    _postDetailState.value = currentState.copy(postDeleted = true)
                }
            }
        }
    }
}
