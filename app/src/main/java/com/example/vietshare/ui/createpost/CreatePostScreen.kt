package com.example.vietshare.ui.createpost

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel = hiltViewModel(),
    onPostCreated: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    val createPostState by viewModel.createPostState.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> viewModel.onImageSelected(uri) }
    )

    LaunchedEffect(Unit) {
        viewModel.createPostState.collectLatest { state ->
            if (state is CreatePostState.Success) {
                onPostCreated()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Create Post") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("What's on your mind?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Image Preview
            selectedImageUri?.let {
                Box(modifier = Modifier.padding(bottom = 16.dp)) {
                    AsyncImage(
                        model = it,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Action Buttons
            Row {
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val currentState = createPostState
            if (currentState is CreatePostState.Error) {
                Text(currentState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            if (currentState is CreatePostState.Loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                     CircularProgressIndicator()
                }
            } else {
                Button(
                    onClick = { viewModel.createPost(content) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = content.isNotBlank() || selectedImageUri != null
                ) {
                    Text("Post")
                }
            }
        }
    }
}
