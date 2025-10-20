package com.example.vietshare.ui.signup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SignupScreen(
    viewModel: SignupViewModel = hiltViewModel(),
    onSignupSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val signupState by viewModel.signupState.collectAsState()

    // Listen for success state to navigate
    LaunchedEffect(signupState) {
        if (signupState is SignupState.Success) {
            onSignupSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign Up", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Show password mismatch error or API error
        passwordError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        val currentState = signupState
        if (currentState is SignupState.Error) {
            Text(currentState.message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Show button or loading indicator
        if (currentState is SignupState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (password == confirmPassword) {
                        passwordError = null
                        viewModel.signup(email, password, username)
                    } else {
                        passwordError = "Passwords do not match."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateBack) {
            Text("Already have an account? Login")
        }
    }
}
