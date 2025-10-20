package com.example.vietshare.domain.model

import com.example.vietshare.data.model.Post
import com.example.vietshare.data.model.User

data class PostWithUser(
    val post: Post,
    val user: User
)
