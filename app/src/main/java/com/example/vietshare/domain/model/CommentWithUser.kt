package com.example.vietshare.domain.model

import com.example.vietshare.data.model.Comment
import com.example.vietshare.data.model.User

data class CommentWithUser(
    val comment: Comment,
    val user: User
)
