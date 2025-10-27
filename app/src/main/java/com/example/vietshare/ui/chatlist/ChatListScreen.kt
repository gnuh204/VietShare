package com.example.vietshare.ui.chatlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.vietshare.R
import com.example.vietshare.util.TimestampFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit
) {
    val chatListState by viewModel.chatListState.collectAsState()
    var showDeleteConfirmationFor by remember { mutableStateOf<ChatWithUserInfo?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Messages") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateGroup) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = chatListState) {
                is ChatListState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ChatListState.Success -> {
                    if (state.chats.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("You have no messages yet.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.chats, key = { it.chat.roomId }) { item ->
                                ChatItem(
                                    item = item,
                                    currentUserId = viewModel.currentUserId,
                                    onClick = { onNavigateToChat(item.chat.roomId) },
                                    onLongPress = { showDeleteConfirmationFor = item }
                                )
                                Divider()
                            }
                        }
                    }
                }
                is ChatListState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message)
                    }
                }
            }
        }
    }

    if (showDeleteConfirmationFor != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationFor = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation? This will delete the conversation for everyone.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.deleteChat(showDeleteConfirmationFor!!.chat.roomId)
                        showDeleteConfirmationFor = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmationFor = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItem(
    item: ChatWithUserInfo, 
    currentUserId: String?, 
    onClick: () -> Unit, 
    onLongPress: () -> Unit
) {
    val unreadCount = item.chat.unreadCount[currentUserId] ?: 0
    val hasUnread = unreadCount > 0

    // Determine the display name and image for one-to-one vs group chats
    val displayName = if (item.chat.type == "GROUP") {
        item.chat.groupName ?: "Group Chat"
    } else {
        item.otherUser.displayName.ifEmpty { item.otherUser.username }
    }
    val imageUrl = if (item.chat.type == "GROUP") {
        item.chat.groupImageUrl
    } else {
        item.otherUser.profileImageUrl
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl?.ifEmpty { R.drawable.ic_launcher_background } ?: R.drawable.ic_launcher_background,
            contentDescription = "Chat Avatar",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.chat.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasUnread) MaterialTheme.colorScheme.onSurface else Color.Gray,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = TimestampFormatter.formatTimestamp(item.chat.lastMessageTimestamp),
                style = MaterialTheme.typography.bodySmall,
                color = if (hasUnread) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (hasUnread) {
                Badge {
                    Text(unreadCount.toString())
                }
            }
        }
    }
}
