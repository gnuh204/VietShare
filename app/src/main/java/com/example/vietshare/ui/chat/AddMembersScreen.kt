package com.example.vietshare.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vietshare.ui.chatlist.FriendSelectItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMembersScreen(
    viewModel: AddMembersViewModel = hiltViewModel(),
    onMembersAdded: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.membersAdded) {
        if (uiState.membersAdded) {
            onMembersAdded()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Members") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Show full screen loading only on initial load
            if (uiState.isLoading && uiState.potentialMembers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.potentialMembers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("All your friends are already in this group, or you have no friends to add.")
                }
            } else {
                Text("Select friends to add:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.potentialMembers, key = { it.userId }) { user ->
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
                onClick = { viewModel.addSelectedMembers() }, // Call the ViewModel function
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedMembers.isNotEmpty() && !uiState.isLoading
            ) {
                 if(uiState.isLoading){
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Add Members")
                }
            }
        }
    }
}
