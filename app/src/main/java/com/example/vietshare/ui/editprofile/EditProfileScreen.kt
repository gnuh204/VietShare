package com.example.vietshare.ui.editprofile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.vietshare.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> viewModel.onImageSelected(uri) }
    )

    // Navigate back on successful save
    LaunchedEffect(uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                actions = {
                    IconButton(onClick = viewModel::saveProfile, enabled = !uiState.isLoading) {
                        Icon(Icons.Default.Check, contentDescription = "Save Profile")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(uiState.error!!, modifier = Modifier.align(Alignment.Center))
                }
                uiState.user != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Image
                        AsyncImage(
                            model = viewModel.selectedImageUri ?: uiState.user?.profileImageUrl?.takeIf { it.isNotEmpty() } ?: R.drawable.ic_launcher_background,
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Display Name
                        OutlinedTextField(
                            value = viewModel.displayName,
                            onValueChange = { viewModel.displayName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bio
                        OutlinedTextField(
                            value = viewModel.bio,
                            onValueChange = { viewModel.bio = it },
                            label = { Text("Bio") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                }
            }
        }
    }
}
