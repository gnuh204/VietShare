package com.example.vietshare.ui.findfriends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.vietshare.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindFriendsScreen(viewModel: FindFriendsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Find Users") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.onSearchQueryChanged(it)
                },
                label = { Text("Search by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.message != null) {
                    Text(uiState.message!!, modifier = Modifier.align(Alignment.Center))
                } else if (searchQuery.isBlank()) {
                    Text("Start typing to find users.", modifier = Modifier.align(Alignment.Center))
                } else if (uiState.searchResults.isEmpty()) {
                    Text("No users found.", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.searchResults, key = { it.user.userId }) { userWithState ->
                            UserItem(
                                userWithState = userWithState,
                                onFollowClick = { viewModel.toggleFollow(userWithState) }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserItem(userWithState: UserWithFollowState, onFollowClick: () -> Unit) {
    val user = userWithState.user
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AsyncImage(
            model = user.profileImageUrl.ifEmpty { R.drawable.ic_launcher_background },
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // User Info
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName.ifEmpty { user.username }, style = MaterialTheme.typography.titleMedium)
            user.hometown.takeIf { it.isNotEmpty() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))

        // Follow/Following Button
        if (userWithState.isFollowing) {
            OutlinedButton(onClick = onFollowClick) {
                Text("Following")
            }
        } else {
            Button(onClick = onFollowClick) {
                Text("Follow")
            }
        }
    }
}
