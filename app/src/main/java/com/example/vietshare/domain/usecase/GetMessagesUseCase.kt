package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.ChatRepository
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(roomId: String) = chatRepository.getMessages(roomId)
}
