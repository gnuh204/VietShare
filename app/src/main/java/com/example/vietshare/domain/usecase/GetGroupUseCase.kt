package com.example.vietshare.domain.usecase

import com.example.vietshare.domain.repository.GroupRepository
import javax.inject.Inject

class GetGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    operator fun invoke(groupId: String) = groupRepository.getGroup(groupId)
}
