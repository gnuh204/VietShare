package com.example.vietshare.ui.chatlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.vietshare.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: CreateGroupViewModel = hiltViewModel(),
    onGroupCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var groupName by remember { mutableStateOf("") }

    LaunchedEffect(uiState.groupCreated) {
        if (uiState.groupCreated) {
            onGroupCreated()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Create New Group") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Select Members", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.friends.isEmpty()){
                Text("You are not following anyone yet.")
            }else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.friends, key = { it.userId }) { user ->
                        val isSelected = uiState.selectedMembers.contains(user.userId)
                        FriendSelectItem(user = user, isSelected = isSelected, onSelect = {
                            viewModel.onMemberSelected(user.userId)
                        })
                        Divider()
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* TODO: viewModel.createGroup(groupName) */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = groupName.isNotBlank() && uiState.selectedMembers.isNotEmpty()
            ) {
                Text("Create Group")
            }
        }
    }
}

@Composable
fun FriendSelectItem(user: User, isSelected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.profileImageUrl.ifEmpty { R.drawable.ic_launcher_background },
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(user.displayName.ifEmpty { user.username }, modifier = Modifier.weight(1f))
        Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
    }
}
