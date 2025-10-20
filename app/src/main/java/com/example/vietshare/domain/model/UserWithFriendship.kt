package com.example.vietshare.domain.model

import com.example.vietshare.data.model.Friendship
import com.example.vietshare.data.model.User

data class UserWithFriendship(
    val user: User,
    val friendship: Friendship? // Null if no relationship exists
)
