package com.example.vietshare.domain.usecase

import com.example.vietshare.data.model.Post
import com.example.vietshare.domain.repository.PostRepository
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(post: Post) = postRepository.createPost(post)
}
