package com.example.vietshare.ui.createpost

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.MediaInfo
import com.example.vietshare.data.model.Post
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.PostRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

sealed class CreatePostState {
    object Idle : CreatePostState()
    object Loading : CreatePostState()
    object Success : CreatePostState()
    data class Error(val message: String) : CreatePostState()
}

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _createPostState = MutableStateFlow<CreatePostState>(CreatePostState.Idle)
    val createPostState: StateFlow<CreatePostState> = _createPostState

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    fun onImageSelected(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun createPost(content: String) {
        viewModelScope.launch {
            _createPostState.value = CreatePostState.Loading

            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                _createPostState.value = CreatePostState.Error("User not logged in")
                return@launch
            }

            try {
                val postId = UUID.randomUUID().toString()
                val mediaInfoList = mutableListOf<MediaInfo>()

                // 1. Upload image if selected and get MediaInfo
                _selectedImageUri.value?.let {
                    val result = postRepository.uploadPostImage(it, postId)
                    val mediaInfo = result.getOrThrow()
                    mediaInfoList.add(mediaInfo)
                }

                // 2. Create Post object with the new media structure
                val newPost = Post(
                    postId = postId,
                    userId = currentUserId,
                    content = content,
                    media = mediaInfoList, // Use the new field
                    timestamp = Timestamp.now()
                )

                // 3. Create the post in the repository
                postRepository.createPost(newPost).getOrThrow()

                _createPostState.value = CreatePostState.Success

            } catch (e: Exception) {
                _createPostState.value = CreatePostState.Error(e.message ?: "Failed to create post")
            }
        }
    }
}
