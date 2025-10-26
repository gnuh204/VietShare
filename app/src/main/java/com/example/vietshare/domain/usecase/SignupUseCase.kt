package com.example.vietshare.domain.usecase

import com.example.vietshare.data.model.User
import com.example.vietshare.domain.repository.AuthRepository
import javax.inject.Inject

class SignupUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, username: String): Result<Unit> {
        val user = User(
            email = email,
            username = username,
            displayName = username,
            displayNameLower = username.lowercase()
        )
        return authRepository.signup(email, password, user)
    }
}
