package com.vapsa.fulbazo.domain.model

import java.time.LocalDateTime

/**
 * Comentario que un jugador ([authorId]) deja sobre un compañero ([playerId])
 * después de jugar juntos. Hay uno solo por partido y compañero; al volver a
 * calificar, se reemplaza.
 */
data class PlayerComment(
    val id: String,
    val matchId: String,
    val authorId: String,
    val playerId: String,
    val text: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        const val MAX_LENGTH = 200
    }
}

/**
 * Resumen de los comentarios de un jugador generado por IA. Se guarda junto con
 * el último comentario que leyó, para generarlo de nuevo solo si llegan comentarios nuevos.
 */
data class ReviewSummary(
    val playerId: String,
    val text: String,
    val lastCommentId: String,
    val commentCount: Int,
)
