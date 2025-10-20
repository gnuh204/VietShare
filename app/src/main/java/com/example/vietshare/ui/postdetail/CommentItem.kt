package com.example.vietshare.ui.postdetail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vietshare.domain.model.CommentWithUser

@Composable
fun CommentItem(item: CommentWithUser) {
    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        Text(
            text = item.user.displayName.ifEmpty { item.user.username },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.comment.content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
