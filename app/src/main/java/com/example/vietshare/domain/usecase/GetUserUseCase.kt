package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.UserRepository
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(userId: String) = userRepository.getUser(userId)
}
