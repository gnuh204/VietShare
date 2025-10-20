package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.PostRepository
import javax.inject.Inject

class GetFeedPostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(userIds: List<String>) = postRepository.getFeedPosts(userIds)
}
