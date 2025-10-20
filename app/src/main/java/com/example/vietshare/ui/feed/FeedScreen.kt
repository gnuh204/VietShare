package com.example.vietshare.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel(),
    onNavigateToCreatePost: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToFindFriends: () -> Unit,
    onNavigateToChatList: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val feedState by viewModel.feedState.collectAsState()
    val hasUnreadNotifications by viewModel.hasUnreadNotifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VietShare") },
                actions = {
                    IconButton(onClick = onNavigateToFindFriends) {
                        Icon(Icons.Default.Search, contentDescription = "Find Friends")
                    }
                    IconButton(onClick = onNavigateToChatList) {
                        Icon(Icons.Default.Email, contentDescription = "Messages")
                    }
                    BadgedBox(badge = {
                        if (hasUnreadNotifications) {
                            Badge()
                        }
                    }) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = { viewModel.currentUserId?.let { onNavigateToProfile(it) } }) {
                        Icon(Icons.Default.Person, contentDescription = "My Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreatePost) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = feedState) {
                is FeedState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is FeedState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.posts, key = { it.post.postId }) { item ->
                            PostItem(
                                item = item,
                                currentUserId = viewModel.currentUserId,
                                onUsernameClick = { onNavigateToProfile(item.user.userId) },
                                onCommentClick = { onNavigateToPostDetail(item.post.postId) },
                                onLikeClick = { 
                                    viewModel.toggleLike(
                                        postId = item.post.postId,
                                        postOwnerId = item.post.userId,
                                        isLiked = item.post.likes.contains(viewModel.currentUserId)
                                    )
                                },
                                onDeleteClick = { viewModel.deletePost(item.post.postId) } // Connect the delete action
                            )
                        }
                    }
                }
                is FeedState.Empty -> {
                     Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                        Text(text = state.message, textAlign = TextAlign.Center)
                    }
                }
                is FeedState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message)
                    }
                }
            }
        }
    }
}
