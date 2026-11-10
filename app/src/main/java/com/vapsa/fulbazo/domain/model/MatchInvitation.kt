package com.vapsa.fulbazo.domain.model

import java.time.LocalDateTime

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
}

/** Invitación de un jugador a otro para unirse a un partido. */
data class MatchInvitation(
    val id: String,
    val matchId: String,
    val fromUserId: String,
    val toUserId: String,
    val createdAt: LocalDateTime,
    val status: InvitationStatus = InvitationStatus.PENDING,
)
