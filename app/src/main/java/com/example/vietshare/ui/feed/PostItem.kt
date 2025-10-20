package com.example.vietshare.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vietshare.R
import com.example.vietshare.domain.model.PostWithUser
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PostItem(
    item: PostWithUser,
    currentUserId: String?,
    onUsernameClick: () -> Unit,
    onCommentClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isLiked = item.post.likes.contains(currentUserId)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                // User Info with Avatar and Timestamp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onUsernameClick)
                ) {
                    AsyncImage(
                        model = item.user.profileImageUrl.ifEmpty { R.drawable.ic_launcher_background },
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.user.displayName.takeIf { it.isNotEmpty() } ?: item.user.username,
                            style = MaterialTheme.typography.titleMedium
                        )
                        item.post.timestamp?.toDate()?.let {
                            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                            Text(
                                text = sdf.format(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Post Content
                if (item.post.content.isNotEmpty()) {
                    Text(
                        text = item.post.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val imageUrl = if (item.post.media.isNotEmpty()) {
                    item.post.media.first().url
                } else if (item.post.mediaUrls.isNotEmpty()) {
                    item.post.mediaUrls.first()
                } else {
                    null
                }

                // Post Image
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Post image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action Buttons (Like & Comment)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${item.post.likes.size}")

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(onClick = onCommentClick) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${item.post.commentCount}")
                }
            }

            // Delete Menu
            if (currentUserId == item.post.userId) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
