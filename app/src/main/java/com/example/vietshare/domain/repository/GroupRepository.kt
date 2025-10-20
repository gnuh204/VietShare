package com.example.vietshare.domain.repository

import com.example.vietshare.data.model.Group
import com.example.vietshare.data.model.GroupMember
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getGroup(groupId: String): Flow<Group?>
    suspend fun createGroup(group: Group): Result<Unit>
    fun getGroupMembers(groupId: String): Flow<List<GroupMember>>
    suspend fun joinGroup(groupId: String, userId: String, role: String): Result<Unit>
}
