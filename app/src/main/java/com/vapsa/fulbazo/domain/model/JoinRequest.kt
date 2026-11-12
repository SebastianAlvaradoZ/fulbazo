package com.vapsa.fulbazo.domain.model

import java.time.LocalDateTime

enum class JoinRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
}

/**
 * Solicitud de un jugador para unirse a un partido. Solo el anfitrión la
 * acepta o la rechaza.
 */
data class JoinRequest(
    val id: String,
    val matchId: String,
    val userId: String,
    val createdAt: LocalDateTime,
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
)
