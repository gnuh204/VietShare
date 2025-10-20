package com.example.vietshare.ui.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.vietshare.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = hiltViewModel(),
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPost: (String) -> Unit
) {
    val notificationState by viewModel.notificationState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notifications") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = notificationState) {
                is NotificationState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is NotificationState.Success -> {
                    if (state.notifications.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("You have no notifications yet.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.notifications, key = { it.notification.notificationId }) { itemState ->
                                NotificationItem(
                                    item = itemState,
                                    onItemClick = {
                                        when (itemState.notification.type) {
                                            "FOLLOW" -> onNavigateToProfile(itemState.sender.userId)
                                            "LIKE" -> onNavigateToPost(itemState.notification.targetId)
                                        }
                                    },
                                    onFollowBackClick = { viewModel.followBack(itemState.sender.userId) }
                                )
                                Divider()
                            }
                        }
                    }
                }
                is NotificationState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    item: NotificationItemState,
    onItemClick: () -> Unit,
    onFollowBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.sender.profileImageUrl.ifEmpty { R.drawable.ic_launcher_background },
            contentDescription = "Sender Avatar",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .clickable { onItemClick() }, // Make avatar clickable too
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val message = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(item.sender.displayName.ifEmpty { item.sender.username })
                }
                when (item.notification.type) {
                    "LIKE" -> append(" liked your post.")
                    "FOLLOW" -> append(" started following you.")
                    else -> append(" sent you a notification.")
                }
            }
            Text(text = message)
        }

        // Follow back button
        if (item.notification.type == "FOLLOW" && !item.isFollowingBack) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onFollowBackClick) {
                Text("Follow")
            }
        }
    }
}
