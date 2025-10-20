package com.example.vietshare.data.model

import com.google.firebase.Timestamp

data class GroupMember(
    val userId: String = "",
    val role: String = "MEMBER",
    val joinDate: Timestamp? = null
)
