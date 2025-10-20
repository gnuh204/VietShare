package com.example.vietshare.domain.usecase

import com.example.vietshare.data.model.Story
import com.example.vietshare.domain.repository.StoryRepository
import javax.inject.Inject

class CreateStoryUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    suspend operator fun invoke(story: Story) = storyRepository.createStory(story)
}
