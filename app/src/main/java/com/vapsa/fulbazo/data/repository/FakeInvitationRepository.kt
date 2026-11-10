package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.InvitationStatus
import com.vapsa.fulbazo.domain.model.MatchInvitation
import com.vapsa.fulbazo.domain.repository.InvitationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.util.UUID

/**
 * Invitaciones en memoria. Mientras no haya backend, solo existen en este
 * teléfono; con Firebase llegarán a otros dispositivos con notificaciones push.
 */
class FakeInvitationRepository(
    private val latencyMillis: Long = 400,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : InvitationRepository {

    private val invitations = MutableStateFlow(emptyList<MatchInvitation>())

    override fun observeReceived(userId: String): Flow<List<MatchInvitation>> =
        invitations.map { list ->
            list.filter { it.toUserId == userId }.sortedByDescending { it.createdAt }
        }

    override suspend fun sendInvitations(
        matchId: String,
        fromUserId: String,
        toUserIds: Collection<String>,
    ): Result<Unit> {
        delay(latencyMillis)
        invitations.update { current ->
            val alreadyInvited = current.filter { it.matchId == matchId }.map { it.toUserId }.toSet()
            val created = toUserIds
                .filter { it != fromUserId && it !in alreadyInvited }
                .distinct()
                .map { MatchInvitation(UUID.randomUUID().toString(), matchId, fromUserId, it, now()) }
            current + created
        }
        return Result.success(Unit)
    }

    override suspend fun respond(invitationId: String, accept: Boolean): Result<Unit> {
        delay(latencyMillis)
        if (invitations.value.none { it.id == invitationId }) {
            return Result.failure(NoSuchElementException("La invitación no existe"))
        }
        val status = if (accept) InvitationStatus.ACCEPTED else InvitationStatus.DECLINED
        invitations.update { list -> list.map { if (it.id == invitationId) it.copy(status = status) else it } }
        return Result.success(Unit)
    }
}
