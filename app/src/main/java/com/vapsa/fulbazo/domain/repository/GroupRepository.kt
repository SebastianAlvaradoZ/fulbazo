package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.FriendGroup
import kotlinx.coroutines.flow.Flow

interface GroupRepository {

    fun observeGroups(ownerId: String): Flow<List<FriendGroup>>

    suspend fun createGroup(ownerId: String, name: String, memberIds: List<String>): Result<FriendGroup>

    suspend fun updateGroup(groupId: String, name: String, memberIds: List<String>): Result<Unit>

    suspend fun deleteGroup(groupId: String): Result<Unit>
}
