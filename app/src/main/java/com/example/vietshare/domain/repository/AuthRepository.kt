package com.example.vietshare.domain.repository

import com.example.vietshare.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUserId(): String?
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signup(email: String, password: String, username: String): Result<Unit>
    suspend fun logout()
    fun getUserDetails(userId: String): Flow<User?>
}
