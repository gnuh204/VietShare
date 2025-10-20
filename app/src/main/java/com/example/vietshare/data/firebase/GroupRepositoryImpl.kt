package com.example.vietshare.data.firebase

import com.example.vietshare.data.model.Group
import com.example.vietshare.data.model.GroupMember
import com.example.vietshare.domain.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : GroupRepository {

    override fun getGroup(groupId: String): Flow<Group?> = callbackFlow {
        val listener = firestore.collection("Groups").document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val group = snapshot?.toObject(Group::class.java)
                trySend(group)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createGroup(group: Group): Result<Unit> = try {
        firestore.collection("Groups").add(group).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getGroupMembers(groupId: String): Flow<List<GroupMember>> = callbackFlow {
        val listener = firestore.collection("Groups").document(groupId).collection("GroupMembers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val members = snapshot?.toObjects(GroupMember::class.java) ?: emptyList()
                trySend(members)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun joinGroup(groupId: String, userId: String, role: String): Result<Unit> = try {
        val member = GroupMember(userId = userId, role = role)
        firestore.collection("Groups").document(groupId).collection("GroupMembers")
            .document(userId).set(member).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
