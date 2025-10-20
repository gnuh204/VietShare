package com.example.vietshare.domain.repository

import android.net.Uri
import com.example.vietshare.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(userId: String): Flow<User?>
    fun getUsers(userIds: List<String>): Flow<List<User>>
    suspend fun searchUsers(query: String, currentUserId: String): Result<List<User>>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String>

    // New Follow/Unfollow system
    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit>
    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit>
}
