package com.example.vietshare.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SignupState {
    object Idle : SignupState()
    object Loading : SignupState()
    object Success : SignupState()
    data class Error(val message: String) : SignupState()
}

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState

    fun signup(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _signupState.value = SignupState.Loading
            if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
                _signupState.value = SignupState.Error("All fields are required.")
                return@launch
            }
            try {
                val newUser = User(
                    email = email,
                    username = displayName, // Use displayName for username initially
                    displayName = displayName,
                    displayNameLower = displayName.lowercase() // Add this
                )
                authRepository.signup(email, password, newUser).getOrThrow()
                _signupState.value = SignupState.Success
            } catch (e: Exception) {
                _signupState.value = SignupState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }
}
