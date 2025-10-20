package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.PostRepository
import javax.inject.Inject

class GetPostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(userId: String) = postRepository.getPosts(userId)
}
