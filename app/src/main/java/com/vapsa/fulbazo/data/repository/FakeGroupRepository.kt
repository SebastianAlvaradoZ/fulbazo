package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.FriendGroup
import com.vapsa.fulbazo.domain.repository.GroupRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/** Grupos de amigos en memoria. */
class FakeGroupRepository(
    private val latencyMillis: Long = 400,
) : GroupRepository {

    private val groups = MutableStateFlow(
        listOf(FriendGroup("group-pichanga", "demo", "Pichanga de los jueves", listOf("carlos", "maria"))),
    )

    override fun observeGroups(ownerId: String): Flow<List<FriendGroup>> =
        groups.map { list -> list.filter { it.ownerId == ownerId }.sortedBy { it.name.lowercase() } }

    override suspend fun createGroup(
        ownerId: String,
        name: String,
        memberIds: List<String>,
    ): Result<FriendGroup> {
        delay(latencyMillis)
        val group = FriendGroup(UUID.randomUUID().toString(), ownerId, name.trim(), memberIds.distinct())
        groups.update { it + group }
        return Result.success(group)
    }

    override suspend fun updateGroup(groupId: String, name: String, memberIds: List<String>): Result<Unit> {
        delay(latencyMillis)
        if (groups.value.none { it.id == groupId }) {
            return Result.failure(NoSuchElementException("El grupo no existe"))
        }
        groups.update { list ->
            list.map {
                if (it.id == groupId) it.copy(name = name.trim(), memberIds = memberIds.distinct()) else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        delay(latencyMillis)
        groups.update { list -> list.filterNot { it.id == groupId } }
        return Result.success(Unit)
    }
}
