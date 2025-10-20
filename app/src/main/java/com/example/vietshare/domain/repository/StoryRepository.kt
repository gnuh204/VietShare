package com.example.vietshare.domain.repository

import com.example.vietshare.data.model.Story
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun getStories(userIds: List<String>): Flow<List<Story>>
    suspend fun createStory(story: Story): Result<Unit>
    suspend fun viewStory(storyId: String, userId: String): Result<Unit>
}
