package com.example.vietshare.ui.postdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.vietshare.R
import com.example.vietshare.ui.feed.PostItem
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    viewModel: PostDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val postDetailState by viewModel.postDetailState.collectAsState()

    val currentState = postDetailState
    if (currentState is PostDetailState.Success && currentState.postDeleted) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Post") }) },
        bottomBar = {
            if (currentState is PostDetailState.Success) {
                 CommentInput(
                    value = viewModel.commentContent,
                    onValueChange = viewModel::onCommentContentChange,
                    onSendClick = viewModel::addComment
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = postDetailState) {
                is PostDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PostDetailState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            PostItem(
                                item = state.post,
                                currentUserId = viewModel.currentUserId,
                                onUsernameClick = {},
                                onCommentClick = {},
                                onLikeClick = { viewModel.toggleLike() },
                                onDeleteClick = { viewModel.deletePost() }
                            )
                            Divider()
                            Text(
                                text = "Comments",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        if (state.comments.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                                    Text("No comments yet.")
                                }
                            }
                        } else {
                            items(state.comments, key = { it.comment.commentId }) {
                                CommentItem(item = it)
                                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
                is PostDetailState.Error -> {
                    Text(state.message, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun CommentItem(item: CommentWithUser) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = item.user.profileImageUrl.ifEmpty { R.drawable.ic_launcher_background },
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.user.displayName.ifEmpty { item.user.username },
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                item.comment.timestamp?.toDate()?.let {
                    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    Text(
                        text = sdf.format(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Text(text = item.comment.content)
        }
    }
}

@Composable
fun CommentInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Add a comment...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onSendClick, enabled = value.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "Send Comment")
            }
        }
    }
}
