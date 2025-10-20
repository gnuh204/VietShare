package com.example.vietshare.data.model

data class Group(
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val memberCount: Int = 0,
    val isPublic: Boolean = true,
    val coverImageUrl: String = ""
)
