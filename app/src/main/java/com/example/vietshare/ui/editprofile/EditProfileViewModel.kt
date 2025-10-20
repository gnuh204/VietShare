package com.example.vietshare.ui.editprofile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import com.example.vietshare.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI State for the Edit Profile screen
data class EditProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isSaveSuccess: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState(isLoading = true))
    val uiState: StateFlow<EditProfileUiState> = _uiState

    // States for the input fields
    var displayName by mutableStateOf("")
    var bio by mutableStateOf("")
    var selectedImageUri by mutableStateOf<Uri?>(null)

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            val user = userRepository.getUser(currentUserId).first()
            if (user != null) {
                displayName = user.displayName
                bio = user.bio
                _uiState.value = EditProfileUiState(user = user)
            } else {
                _uiState.value = EditProfileUiState(error = "User not found")
            }
        }
    }

    fun onImageSelected(uri: Uri?) {
        selectedImageUri = uri
    }

    fun saveProfile() {
        val currentUser = _uiState.value.user ?: return
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                var newImageUrl = currentUser.profileImageUrl
                // 1. Upload new image if selected
                selectedImageUri?.let {
                    val uploadResult = userRepository.uploadProfileImage(currentUser.userId, it)
                    newImageUrl = uploadResult.getOrThrow()
                }

                // 2. Update user document in Firestore
                val updatedUser = currentUser.copy(
                    displayName = displayName,
                    bio = bio,
                    profileImageUrl = newImageUrl
                )
                userRepository.updateUser(updatedUser).getOrThrow()
                
                _uiState.value = _uiState.value.copy(isLoading = false, isSaveSuccess = true)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
