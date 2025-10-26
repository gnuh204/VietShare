package com.example.vietshare.ui.signup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VerifyOtpState {
    object Idle : VerifyOtpState()
    object Loading : VerifyOtpState()
    object Success : VerifyOtpState()
    data class Error(val message: String) : VerifyOtpState()
}

@HiltViewModel
class VerifyOtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // The user's details passed via navigation arguments
    private val email: String = savedStateHandle.get<String>("email")!!
    private val password: String = savedStateHandle.get<String>("password")!!
    private val displayName: String = savedStateHandle.get<String>("displayName")!!
    private val correctOtp: String = savedStateHandle.get<String>("otp")!!

    private val _verifyOtpState = MutableStateFlow<VerifyOtpState>(VerifyOtpState.Idle)
    val verifyOtpState: StateFlow<VerifyOtpState> = _verifyOtpState

    fun verifyOtpAndSignup(otpFromUser: String) {
        viewModelScope.launch {
            _verifyOtpState.value = VerifyOtpState.Loading

            // 1. Verify the OTP
            if (otpFromUser != correctOtp) {
                _verifyOtpState.value = VerifyOtpState.Error("Invalid OTP. Please try again.")
                return@launch
            }

            // 2. If OTP is correct, proceed with signup
            try {
                val user = com.example.vietshare.data.model.User(
                    email = email,
                    username = displayName,
                    displayName = displayName,
                    displayNameLower = displayName.lowercase()
                )
                authRepository.signup(email, password, user).getOrThrow()
                _verifyOtpState.value = VerifyOtpState.Success
            } catch (e: Exception) {
                _verifyOtpState.value = VerifyOtpState.Error(e.message ?: "Signup failed after verification.")
            }
        }
    }

    fun resendOtp() {
        // In this demo version, resending OTP is not implemented as it requires a new call.
        // In a real app, this would call the sendOtp function again.
    }
}
