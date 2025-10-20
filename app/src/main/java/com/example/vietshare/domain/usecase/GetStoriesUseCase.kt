package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.StoryRepository
import javax.inject.Inject

class GetStoriesUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    operator fun invoke(userIds: List<String>) = storyRepository.getStories(userIds)
}
