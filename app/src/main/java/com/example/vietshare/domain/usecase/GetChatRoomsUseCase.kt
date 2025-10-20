package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.ChatRepository
import javax.inject.Inject

class GetChatRoomsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(userId: String) = chatRepository.getChatRooms(userId)
}
