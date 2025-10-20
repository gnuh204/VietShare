package com.example.vietshare.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import com.example.vietshare.data.model.Post
import com.example.vietshare.data.model.User
import com.example.vietshare.domain.model.PostWithUser
import com.example.vietshare.ui.feed.PostItem
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = profileState) {
            is ProfileState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProfileState.Success -> {
                ProfileContent(
                    user = state.user,
                    posts = state.posts,
                    isFollowing = state.isFollowing,
                    isCurrentUserProfile = state.isCurrentUserProfile,
                    currentUserId = viewModel.currentUserId,
                    onFollowClick = { viewModel.toggleFollow() },
                    onMessageClick = {
                        coroutineScope.launch {
                            viewModel.startChat()
                                .onSuccess { roomId -> onNavigateToChat(roomId) }
                        }
                    },
                    onLikeClick = { postId, isLiked, ownerId -> viewModel.toggleLike(postId, isLiked, ownerId) },
                    onCommentClick = { postId -> onNavigateToPostDetail(postId) },
                    onEditProfileClick = onNavigateToEditProfile,
                    onSettingsClick = onNavigateToSettings,
                    onDeletePostClick = { postId -> viewModel.deletePost(postId) } // Add this
                )
            }
            is ProfileState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message)
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: User,
    posts: List<Post>,
    isFollowing: Boolean,
    isCurrentUserProfile: Boolean,
    currentUserId: String?,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onLikeClick: (String, Boolean, String) -> Unit,
    onCommentClick: (String) -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDeletePostClick: (String) -> Unit // Add this
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Profile Header
        item {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = user.profileImageUrl.ifEmpty { R.drawable.ic_launcher_background },
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))
                Text(user.displayName.ifEmpty { user.username }, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(user.bio.ifEmpty { "No bio yet." }, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("${user.followersCount} Followers")
                    Text("${user.followingCount} Following")
                }
                Spacer(Modifier.height(16.dp))

                // Action Buttons
                if (isCurrentUserProfile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEditProfileClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Edit Profile")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onFollowClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isFollowing) "Following" else "Follow")
                        }
                        if (isFollowing) {
                            OutlinedButton(
                                onClick = onMessageClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Message")
                            }
                        }
                    }
                }
            }
        }
        item { Divider(Modifier.padding(horizontal = 16.dp)) }

        // Posts List
        if (posts.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text("This user has no posts yet.")
                }
            }
        } else {
            items(posts) { post ->
                val postWithUser = PostWithUser(post, user)
                PostItem(
                    item = postWithUser,
                    currentUserId = currentUserId,
                    onUsernameClick = {},
                    onCommentClick = { onCommentClick(post.postId) },
                    onLikeClick = { 
                        onLikeClick(post.postId, post.likes.contains(currentUserId), post.userId)
                    },
                    onDeleteClick = { onDeletePostClick(post.postId) } // Connect the delete action
                )
            }
        }
    }
}
