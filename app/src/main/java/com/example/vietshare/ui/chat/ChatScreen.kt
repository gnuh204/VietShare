package com.example.vietshare.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vietshare.data.model.Message
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MessageViewModel = hiltViewModel()) {
    val messageState by viewModel.messageState.collectAsState()
    val otherUser by viewModel.otherUserState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(otherUser?.displayName?.takeIf { it.isNotEmpty() } ?: otherUser?.username ?: "Chat")
            })
        },
        bottomBar = {
            MessageInput(
                value = viewModel.messageContent,
                onValueChange = viewModel::onMessageContentChange,
                onSendClick = viewModel::sendMessage
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = messageState) {
                is MessageState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is MessageState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(state.messages, key = { it.messageId }) { message ->
                            val isCurrentUser = message.senderId == viewModel.currentUserId
                            MessageBubble(message = message, isCurrentUser = isCurrentUser)
                        }
                    }
                    // Auto-scroll to the bottom when a new message arrives
                    LaunchedEffect(state.messages.size) {
                        if (state.messages.isNotEmpty()) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(state.messages.size - 1)
                            }
                        }
                    }
                }
                is MessageState.Error -> {
                    Text(state.message, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isCurrentUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f) // Prevent message from taking full screen width
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isCurrentUser) 16.dp else 0.dp,
                    bottomEnd = if (isCurrentUser) 0.dp else 16.dp
                ))
                .background(if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = message.content)
        }
    }
}

@Composable
fun MessageInput(
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
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onSendClick, enabled = value.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
