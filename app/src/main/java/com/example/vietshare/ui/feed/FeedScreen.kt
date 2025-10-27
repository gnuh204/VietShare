package com.example.vietshare.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel(),
    onNavigateToCreatePost: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToChatList: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToFindFriends: () -> Unit
) {
    val feedState by viewModel.feedState.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val unreadChatCount by viewModel.unreadChatCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VietShare") },
                actions = {
                    IconButton(onClick = onNavigateToFindFriends) {
                        Icon(Icons.Default.Search, contentDescription = "Find Friends")
                    }
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(badge = {
                            if (unreadNotificationCount > 0) {
                                Badge { Text(unreadNotificationCount.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = onNavigateToChatList) {
                         BadgedBox(badge = {
                            if (unreadChatCount > 0) {
                                Badge { Text(unreadChatCount.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.ChatBubble, contentDescription = "Chat")
                        }
                    }
                    IconButton(onClick = { viewModel.currentUserId?.let { onNavigateToProfile(it) } }) {
                        AsyncImage(
                            model = currentUser?.profileImageUrl?.ifEmpty { R.drawable.ic_launcher_background } ?: R.drawable.ic_launcher_background,
                            contentDescription = "My Profile",
                            modifier = Modifier.size(32.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
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
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.posts, key = { it.post.postId }) { postWithUser ->
                            PostItem(
                                item = postWithUser,
                                currentUserId = viewModel.currentUserId,
                                onLikeClick = { viewModel.toggleLike(postWithUser.post.postId, postWithUser.post.userId, postWithUser.post.likes.contains(viewModel.currentUserId)) },
                                onUsernameClick = { onNavigateToProfile(postWithUser.user.userId) },
                                onCommentClick = { onNavigateToPostDetail(postWithUser.post.postId) },
                                onDeleteClick = { viewModel.deletePost(postWithUser.post.postId) }
                            )
                        }
                    }
                }
                is FeedState.Empty -> {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message)
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
