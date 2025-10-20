package com.example.vietshare.domain.usecase

import com.example.vietshare.data.model.Group
import com.example.vietshare.domain.repository.GroupRepository
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(group: Group) = groupRepository.createGroup(group)
}
