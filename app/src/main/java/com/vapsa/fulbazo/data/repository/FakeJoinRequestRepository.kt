package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.JoinRequest
import com.vapsa.fulbazo.domain.model.JoinRequestStatus
import com.vapsa.fulbazo.domain.repository.JoinRequestRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.util.UUID

/** Solicitudes para unirse a partidos, en memoria. Se reemplazará por Firestore. */
class FakeJoinRequestRepository(
    private val latencyMillis: Long = 400,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : JoinRequestRepository {

    /** El usuario de prueba "demo" ya pidió unirse al partido de vale9 en Cayma. */
    private val requests = MutableStateFlow(
        listOf(JoinRequest("req-demo-cayma", "match-cayma", "demo", now().minusHours(3))),
    )

    override fun observeForMatch(matchId: String): Flow<List<JoinRequest>> =
        requests.map { list -> list.filter { it.matchId == matchId }.sortedBy { it.createdAt } }

    override fun observeByUser(userId: String): Flow<List<JoinRequest>> =
        requests.map { list -> list.filter { it.userId == userId }.sortedByDescending { it.createdAt } }

    override suspend fun sendRequest(matchId: String, userId: String): Result<Unit> {
        delay(latencyMillis)
        requests.update { current ->
            val alreadyPending = current.any {
                it.matchId == matchId && it.userId == userId && it.status == JoinRequestStatus.PENDING
            }
            if (alreadyPending) current
            else current + JoinRequest(UUID.randomUUID().toString(), matchId, userId, now())
        }
        return Result.success(Unit)
    }

    override suspend fun cancelRequest(requestId: String): Result<Unit> {
        delay(latencyMillis)
        if (requests.value.none { it.id == requestId && it.status == JoinRequestStatus.PENDING }) {
            return Result.failure(IllegalStateException("La solicitud ya no está pendiente"))
        }
        requests.update { list -> list.filterNot { it.id == requestId } }
        return Result.success(Unit)
    }

    override suspend fun respond(requestId: String, accept: Boolean): Result<Unit> {
        delay(latencyMillis)
        if (requests.value.none { it.id == requestId && it.status == JoinRequestStatus.PENDING }) {
            return Result.failure(IllegalStateException("La solicitud ya no está pendiente"))
        }
        val status = if (accept) JoinRequestStatus.ACCEPTED else JoinRequestStatus.REJECTED
        requests.update { list -> list.map { if (it.id == requestId) it.copy(status = status) else it } }
        return Result.success(Unit)
    }
}
