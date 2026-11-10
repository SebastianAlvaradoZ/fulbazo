package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.MatchInvitation
import kotlinx.coroutines.flow.Flow

interface InvitationRepository {

    /** Invitaciones recibidas por el usuario, de la más nueva a la más antigua. */
    fun observeReceived(userId: String): Flow<List<MatchInvitation>>

    /** Envía una invitación a cada jugador; ignora a quienes ya fueron invitados. */
    suspend fun sendInvitations(matchId: String, fromUserId: String, toUserIds: Collection<String>): Result<Unit>

    suspend fun respond(invitationId: String, accept: Boolean): Result<Unit>
}
