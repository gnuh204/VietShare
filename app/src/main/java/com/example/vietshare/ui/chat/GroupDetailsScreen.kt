package com.example.vietshare.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.vietshare.R
import com.example.vietshare.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    viewModel: GroupDetailsViewModel = hiltViewModel(),
    onNavigateToProfile: (String) -> Unit,
    onNavigateToAddMembers: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<User?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { viewModel.changeGroupAvatar(it) } }
    )
    
    LaunchedEffect(uiState.groupLeft){
        if(uiState.groupLeft){
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Group Details") }, navigationIcon = { /* TODO: Back button */ }) }
    ) { padding ->
        val currentChat = uiState.chat
        if (uiState.isLoading) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (currentChat != null) {
            val isAdmin = currentChat.adminId == viewModel.currentUserId
            Column(modifier = Modifier.fillMaxSize().padding(padding)){
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = currentChat.groupImageUrl ?: R.drawable.ic_launcher_background,
                                    contentDescription = "Group Avatar",
                                    modifier = Modifier.size(100.dp).clip(CircleShape).clickable(enabled = isAdmin) { imagePickerLauncher.launch("image/*") },
                                    contentScale = ContentScale.Crop
                                )
                                if (uiState.isUploading) { CircularProgressIndicator() }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = currentChat.groupName ?: "Group Chat", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween){
                            Text(text = "${uiState.members.size} Members", style = MaterialTheme.typography.titleMedium)
                            if(isAdmin){
                                TextButton(onClick = { onNavigateToAddMembers(currentChat.roomId) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Members", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Members")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                    }

                    items(uiState.members, key = { it.userId }) { user ->
                        MemberItem(
                            user = user, 
                            isAdmin = user.userId == currentChat.adminId,
                            showRemoveIcon = isAdmin && user.userId != viewModel.currentUserId,
                            onUserClick = { onNavigateToProfile(user.userId) },
                            onRemoveClick = { memberToRemove = user }
                        )
                        Divider()
                    }
                }
                // Leave Group Button
                if(!isAdmin){
                     Button(
                        onClick = { showLeaveConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Leave Group")
                    }
                }
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 Text(uiState.error!!)
            }
        }
    }

    // Confirmation Dialogs
    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text("Leave Group") },
            text = { Text("Are you sure you want to leave this group?") },
            confirmButton = { Button(onClick = { viewModel.leaveGroup(); showLeaveConfirmDialog = false }) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { showLeaveConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    memberToRemove?.let { user ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove Member") },
            text = { Text("Are you sure you want to remove ${user.displayName} from the group?") },
            confirmButton = { Button(onClick = { viewModel.removeMember(user.userId); memberToRemove = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun MemberItem(user: User, isAdmin: Boolean, showRemoveIcon: Boolean, onUserClick: () -> Unit, onRemoveClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onUserClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.profileImageUrl.ifEmpty { R.drawable.ic_launcher_background },
            contentDescription = "Member Avatar",
            modifier = Modifier.size(48.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName.ifEmpty { user.username }, fontWeight = FontWeight.Bold)
        }
        if (isAdmin) {
            Text("Admin", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
        if(showRemoveIcon){
            IconButton(onClick = onRemoveClick) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove member", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
