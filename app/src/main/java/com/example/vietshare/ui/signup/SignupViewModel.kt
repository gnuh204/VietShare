package com.example.vietshare.ui.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietshare.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.* 
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable data class SendGridEmail(val email: String)
@Serializable data class SendGridPersonalization(val to: List<SendGridEmail>)
@Serializable data class SendGridContent(val type: String, val value: String)
@Serializable data class SendGridRequest(
    val personalizations: List<SendGridPersonalization>,
    val from: SendGridEmail,
    val subject: String,
    val content: List<SendGridContent>
)

sealed class SignupState {
    object Idle : SignupState()
    object Loading : SignupState()
    data class OtpSent(val otp: String) : SignupState()
    data class Error(val message: String) : SignupState()
}

@HiltViewModel
class SignupViewModel @Inject constructor() : ViewModel() {

    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState

    fun sendOtp(email: String) {
        viewModelScope.launch {
            _signupState.value = SignupState.Loading
            try {
                val client = HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json()
                    }
                    // Add this to log response details
                    expectSuccess = false 
                }

                val otp = (100000..999999).random().toString()

                val requestBody = SendGridRequest(
                    personalizations = listOf(SendGridPersonalization(to = listOf(SendGridEmail(email)))),
                    from = SendGridEmail(BuildConfig.SENDER_EMAIL),
                    subject = "Your Verification Code for VietShare",
                    content = listOf(SendGridContent("text/html", "<p>Your verification code is: <b>$otp</b></p>"))
                )

                val response = withContext(Dispatchers.IO) {
                    client.post("https://api.sendgrid.com/v3/mail/send") {
                        header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SENDGRID_API_KEY}")
                        contentType(ContentType.Application.Json)
                        setBody(requestBody)
                    }
                }

                // Log the response status and body
                Log.d("SendGridResponse", "Status: ${response.status}")
                Log.d("SendGridResponse", "Body: ${response.bodyAsText()}")

                if (response.status.isSuccess()) {
                    _signupState.value = SignupState.OtpSent(otp)
                } else {
                    _signupState.value = SignupState.Error("Failed with status: ${response.status}")
                }
                
                client.close()

            } catch (e: Exception) {
                _signupState.value = SignupState.Error(e.message ?: "Failed to send OTP.")
            }
        }
    }

    fun resetState() {
        _signupState.value = SignupState.Idle
    }
}
