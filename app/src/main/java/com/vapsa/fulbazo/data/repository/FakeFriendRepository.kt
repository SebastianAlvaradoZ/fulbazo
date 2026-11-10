package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.FriendRequest
import com.vapsa.fulbazo.domain.model.FriendRequestStatus
import com.vapsa.fulbazo.domain.repository.FriendRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Amistades y solicitudes en memoria. El usuario de prueba "demo" ya tiene
 * dos amigos y una solicitud recibida.
 */
class FakeFriendRepository(
    private val latencyMillis: Long = 400,
) : FriendRepository {

    /** Cada amistad se guarda como un par sin orden. */
    private val friendships = MutableStateFlow(
        setOf(setOf("demo", "carlos"), setOf("demo", "maria"), setOf("carlos", "maria")),
    )

    private val requests = MutableStateFlow(
        listOf(FriendRequest(id = "req-jose", fromUserId = "jose", toUserId = "demo")),
    )

    override fun observeFriendIds(userId: String): Flow<Set<String>> =
        friendships.map { pairs ->
            pairs.filter { userId in it }.map { pair -> pair.first { it != userId } }.toSet()
        }

    override fun observePendingRequests(userId: String): Flow<List<FriendRequest>> =
        requests.map { list ->
            list.filter {
                it.status == FriendRequestStatus.PENDING &&
                    (it.fromUserId == userId || it.toUserId == userId)
            }
        }

    override suspend fun sendRequest(fromUserId: String, toUserId: String): Result<Unit> {
        delay(latencyMillis)
        if (fromUserId == toUserId) return Result.failure(IllegalArgumentException("No puedes agregarte"))
        if (setOf(fromUserId, toUserId) in friendships.value) return Result.success(Unit)

        // Si el otro ya me había enviado una solicitud, la acepto directamente.
        val reverse = requests.value.firstOrNull {
            it.status == FriendRequestStatus.PENDING &&
                it.fromUserId == toUserId && it.toUserId == fromUserId
        }
        if (reverse != null) return respondToRequest(reverse.id, accept = true)

        val alreadySent = requests.value.any {
            it.status == FriendRequestStatus.PENDING &&
                it.fromUserId == fromUserId && it.toUserId == toUserId
        }
        if (!alreadySent) {
            requests.update {
                it + FriendRequest(UUID.randomUUID().toString(), fromUserId, toUserId)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun respondToRequest(requestId: String, accept: Boolean): Result<Unit> {
        delay(latencyMillis)
        val request = requests.value.firstOrNull { it.id == requestId }
            ?: return Result.failure(NoSuchElementException("La solicitud no existe"))
        val status = if (accept) FriendRequestStatus.ACCEPTED else FriendRequestStatus.REJECTED
        requests.update { list -> list.map { if (it.id == requestId) it.copy(status = status) else it } }
        if (accept) friendships.update { it + setOf(setOf(request.fromUserId, request.toUserId)) }
        return Result.success(Unit)
    }

    override suspend fun removeFriend(userId: String, friendId: String): Result<Unit> {
        delay(latencyMillis)
        friendships.update { it - setOf(setOf(userId, friendId)) }
        return Result.success(Unit)
    }
}
