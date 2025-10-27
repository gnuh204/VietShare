package com.example.vietshare.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.vietshare.R
import com.example.vietshare.data.model.ChatType
import com.example.vietshare.data.model.Message
import com.example.vietshare.data.model.MessageType
import com.example.vietshare.data.model.User
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MessageViewModel = hiltViewModel(), onNavigateToGroupDetails: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = viewModel::onImageSelected
    )
    
    val currentUiState = uiState

    Scaffold(
        topBar = {
            if(currentUiState is UiState.Success){
                val chat = currentUiState.chat
                val otherUser = if(chat?.type == ChatType.ONE_TO_ONE.name) {
                    currentUiState.members.values.find { it.userId != viewModel.currentUserId }
                } else null
                
                TopAppBar(
                    title = { Text(chat?.groupName ?: otherUser?.displayName ?: "Chat") },
                    modifier = if (chat?.type == ChatType.GROUP.name) {
                        Modifier.clickable { onNavigateToGroupDetails(chat.roomId) }
                    } else {
                        Modifier
                    }
                )
            }
            
        },
        bottomBar = {
            MessageInput(
                value = viewModel.messageContent,
                onValueChange = viewModel::onMessageContentChange,
                onSendClick = viewModel::sendMessage,
                onAttachmentClick = { imagePickerLauncher.launch("image/*") },
                selectedImageUri = viewModel.selectedImageUri,
                onClearImage = { viewModel.onImageSelected(null) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(state.messages, key = { it.messageId }) { message ->
                            if (message.type == MessageType.SYSTEM.name) {
                                SystemMessage(message = message)
                            } else {
                                val isCurrentUser = message.senderId == viewModel.currentUserId
                                val sender = state.members[message.senderId]
                                MessageBubble(
                                    message = message, 
                                    isCurrentUser = isCurrentUser, 
                                    sender = sender,
                                    isGroupChat = state.chat?.type == ChatType.GROUP.name
                                )
                            }
                        }
                    }
                    LaunchedEffect(state.messages.size) {
                        if (state.messages.isNotEmpty()) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(state.messages.size - 1)
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Text(state.message, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun SystemMessage(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = message.content ?: "",
            style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = FontStyle.Italic,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun MessageBubble(message: Message, isCurrentUser: Boolean, sender: User?, isGroupChat: Boolean) {
    val horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    val horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isGroupChat && !isCurrentUser) {
            AsyncImage(
                model = if (sender?.profileImageUrl.isNullOrEmpty()) {
                    R.drawable.ic_launcher_background
                } else {
                    sender.profileImageUrl
                },
                contentDescription = "Sender Avatar",
                modifier = Modifier.size(32.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = horizontalAlignment) {
            if (isGroupChat && !isCurrentUser) {
                Text(
                    text = sender?.displayName ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isCurrentUser) 16.dp else 0.dp,
                        bottomEnd = if (isCurrentUser) 0.dp else 16.dp
                    ))
                    .background(if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                Column {
                    message.media?.url?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = "Sent image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (!message.content.isNullOrBlank()) {
                        Text(text = message.content)
                    }

                    message.timestamp?.let {
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
        
        if (!isGroupChat || (isGroupChat && isCurrentUser)) {
            Spacer(modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    selectedImageUri: Uri?,
    onClearImage: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Column {
            if (selectedImageUri != null) {
                Box(modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .height(100.dp)) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxHeight().clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onClearImage,
                        modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear image", tint = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onAttachmentClick) {
                    Icon(Icons.Default.Add, contentDescription = "Attach file")
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onSendClick, enabled = value.isNotBlank() || selectedImageUri != null) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}
