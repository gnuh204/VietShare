package com.example.vietshare.data.firebase

import com.example.vietshare.data.model.Story
import com.example.vietshare.domain.repository.StoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : StoryRepository {

    override fun getStories(userIds: List<String>): Flow<List<Story>> = callbackFlow {
        if (userIds.isNotEmpty()) {
            val listener = firestore.collection("Stories").whereIn("userId", userIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val stories = snapshot?.toObjects(Story::class.java) ?: emptyList()
                    trySend(stories)
                }
            awaitClose { listener.remove() }
        } else {
            trySend(emptyList())
            close()
        }
    }

    override suspend fun createStory(story: Story): Result<Unit> = try {
        firestore.collection("Stories").add(story).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun viewStory(storyId: String, userId: String): Result<Unit> {
        // Implementation for viewing a story
        return Result.success(Unit)
    }
}
