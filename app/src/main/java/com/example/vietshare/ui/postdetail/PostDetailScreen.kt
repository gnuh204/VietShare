package com.example.vietshare.ui.postdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val postDetailState by viewModel.postDetailState.collectAsState()
    var showReactionPickerFor by remember { mutableStateOf<String?>(null) }
    var showCommentMenuFor by remember { mutableStateOf<CommentWithUser?>(null) }
    val replyingToComment by viewModel.replyingToComment.collectAsState()

    val currentState = postDetailState
    if (currentState is PostDetailState.Success && currentState.postDeleted) {
        LaunchedEffect(Unit) { onNavigateBack() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Post") }) },
        bottomBar = {
            if (currentState is PostDetailState.Success) {
                 CommentInput(
                    value = viewModel.commentContent,
                    onValueChange = viewModel::onCommentContentChange,
                    onSendClick = viewModel::addComment,
                    replyingTo = replyingToComment,
                    onCancelReply = viewModel::onCancelReply
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                                onUsernameClick = { onNavigateToProfile(state.post.user.userId) },
                                onCommentClick = {},
                                onLikeClick = { viewModel.toggleLike() },
                                onDeleteClick = { viewModel.deletePost() }
                            )
                            Divider()
                            Text(text = "Comments", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                        }

                        if (state.comments.isEmpty()) {
                            item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { Text("No comments yet.") } }
                        } else {
                            items(state.comments, key = { it.commentWithUser.comment.commentId }) { commentNode ->
                                CommentNodeItem(
                                    node = commentNode,
                                    currentUserId = viewModel.currentUserId,
                                    onUserClick = onNavigateToProfile,
                                    onLongPress = { commentWithUser -> showCommentMenuFor = commentWithUser },
                                    onReplyClick = viewModel::onStartReply,
                                    onDeleteClick = { viewModel.deleteComment(it) }
                                )
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

    if (showReactionPickerFor != null) {
        ReactionPicker(
            onDismiss = { showReactionPickerFor = null },
            onReactionSelected = {
                viewModel.toggleCommentReaction(showReactionPickerFor!!, it)
                showReactionPickerFor = null
            }
        )
    }

    if (showCommentMenuFor != null) {
        CommentMenu(
            commentWithUser = showCommentMenuFor!!,
            currentUserId = viewModel.currentUserId,
            onDismiss = { showCommentMenuFor = null },
            onDeleteClick = { 
                viewModel.deleteComment(showCommentMenuFor!!.comment.commentId)
                showCommentMenuFor = null
             },
            onReactClick = { 
                showReactionPickerFor = showCommentMenuFor!!.comment.commentId
                showCommentMenuFor = null
            }
        )
    }
}

@Composable
fun CommentNodeItem(
    node: CommentNode,
    currentUserId: String?,
    onUserClick: (String) -> Unit,
    onLongPress: (CommentWithUser) -> Unit,
    onReplyClick: (CommentWithUser) -> Unit,
    onDeleteClick: (String) -> Unit,
    depth: Int = 0
) {
    var showAllReplies by remember { mutableStateOf(false) }
    val replies = node.replies

    Column(modifier = Modifier.padding(start = (24 * depth).dp)) {
        CommentItem(
            item = node.commentWithUser,
            currentUserId = currentUserId,
            onUserClick = { onUserClick(node.commentWithUser.user.userId) },
            onLongPress = { onLongPress(node.commentWithUser) },
            onReplyClick = { onReplyClick(node.commentWithUser) },
            onDeleteClick = { onDeleteClick(node.commentWithUser.comment.commentId) }
        )

        val repliesToShow = if (showAllReplies) replies else replies.take(1)

        repliesToShow.forEach { replyNode ->
            Row(Modifier.padding(start = 16.dp)){
                 Divider(modifier = Modifier.width(2.dp).height(24.dp).padding(vertical = 4.dp), color = Color.Gray.copy(alpha = 0.3f))
                 CommentNodeItem(
                     node = replyNode, 
                     currentUserId = currentUserId,
                     onUserClick = onUserClick, 
                     onLongPress = onLongPress, 
                     onReplyClick = onReplyClick,
                     onDeleteClick = onDeleteClick,
                     depth = depth + 1
                )
            }
        }

        if (replies.size > 1 && !showAllReplies) {
            TextButton(
                onClick = { showAllReplies = true },
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text("View ${replies.size - 1} more replies...")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentItem(
    item: CommentWithUser,
    currentUserId: String?,
    onUserClick: () -> Unit,
    onLongPress: () -> Unit,
    onReplyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onUserClick, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = item.user.profileImageUrl.ifEmpty { R.drawable.ic_launcher_background },
            contentDescription = "User Avatar",
            modifier = Modifier.size(32.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            // Content in a bubble
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(12.dp)){
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.user.displayName.ifEmpty { item.user.username }, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        item.comment.timestamp?.toDate()?.let {
                            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            Text(text = sdf.format(it), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Text(text = item.comment.content)
                }
            }
            
            // Actions below the bubble
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReplyClick) { Text("Reply", fontSize = 12.sp) }
                 if (item.comment.reactions.isNotEmpty()) {
                    ReactionsSummary(reactions = item.comment.reactions)
                }
            }
        }
    }
}


@Composable
fun CommentMenu(
    commentWithUser: CommentWithUser,
    currentUserId: String?,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit,
    onReactClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comment options") },
        text = {
            Column {
                Text("React to comment", modifier = Modifier.clickable(onClick = onReactClick).fillMaxWidth().padding(vertical = 12.dp))
                if (commentWithUser.user.userId == currentUserId) {
                    Text("Delete comment", modifier = Modifier.clickable(onClick = onDeleteClick).fillMaxWidth().padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ReactionsSummary(reactions: Map<String, List<String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        reactions.filter { it.value.isNotEmpty() }.forEach { (reaction, users) ->
             Surface(
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(reaction, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(users.size.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReactionPicker(onDismiss: () -> Unit, onReactionSelected: (String) -> Unit) {
    val reactions = listOf("❤️", "👍", "😂", "😡", "😢")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("React") },
        text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                reactions.forEach { reaction ->
                    Text(
                        text = reaction,
                        fontSize = 24.sp,
                        modifier = Modifier.clickable { onReactionSelected(reaction) }.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun CommentInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    replyingTo: CommentWithUser?,
    onCancelReply: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Column {
            if (replyingTo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Replying to ${replyingTo.user.displayName}", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(16.dp)) { Icon(Icons.Default.Close, contentDescription = "Cancel Reply") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text("Add a comment...") },
                    modifier = Modifier.weight(1f),
                    visualTransformation = MentionVisualTransformation(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onSendClick, enabled = value.isNotBlank()) {
                    Icon(Icons.Default.Send, contentDescription = "Send Comment")
                }
            }
        }
    }
}

private class MentionVisualTransformation(private val color: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = buildAnnotatedString {
            append(text)
            val regex = Regex("@[^\\s]+")
            regex.findAll(text.text).forEach { matchResult ->
                addStyle(
                    style = SpanStyle(color = color, fontWeight = FontWeight.Bold),
                    start = matchResult.range.first,
                    end = matchResult.range.last + 1
                )
            }
        }
        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}