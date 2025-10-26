package com.example.vietshare.ui.signup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun VerifyOtpScreen(
    viewModel: VerifyOtpViewModel = hiltViewModel(),
    onVerificationSuccess: () -> Unit
) {
    val verifyOtpState by viewModel.verifyOtpState.collectAsState()
    var otp by remember { mutableStateOf("") }

    // Handle navigation on success
    LaunchedEffect(verifyOtpState) {
        if (verifyOtpState is VerifyOtpState.Success) {
            onVerificationSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Verify your Email", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("An OTP has been sent to your email address. Please enter it below to complete your registration.", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) otp = it },
            label = { Text("6-Digit OTP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = verifyOtpState) {
            is VerifyOtpState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }

        Button(
            onClick = { viewModel.verifyOtpAndSignup(otp) },
            enabled = otp.length == 6 && verifyOtpState !is VerifyOtpState.Loading
        ) {
            if (verifyOtpState is VerifyOtpState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Verify & Signup")
            }
        }
        
        TextButton(onClick = { viewModel.resendOtp() }) {
            Text("Resend OTP")
        }
    }
}
