package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.AuthRepository
import javax.inject.Inject

class SignupUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, username: String) = authRepository.signup(email, password, username)
}
