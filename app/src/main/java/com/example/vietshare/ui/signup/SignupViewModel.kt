package com.example.vietshare.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.domain.usecase.SignupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signupUseCase: SignupUseCase
) : ViewModel() {

    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState

    fun signup(email: String, password: String, username: String) {
        viewModelScope.launch {
            _signupState.value = SignupState.Loading
            signupUseCase(email, password, username)
                .onSuccess { _signupState.value = SignupState.Success }
                .onFailure { _signupState.value = SignupState.Error(it.message ?: "Unknown error") }
        }
    }
}

sealed class SignupState {
    object Idle : SignupState()
    object Loading : SignupState()
    object Success : SignupState()
    data class Error(val message: String) : SignupState()
}
