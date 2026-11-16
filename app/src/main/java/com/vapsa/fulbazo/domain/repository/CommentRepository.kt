package com.vapsa.fulbazo.domain.repository

import com.vapsa.fulbazo.domain.model.PlayerComment
import com.vapsa.fulbazo.domain.model.ReviewSummary
import kotlinx.coroutines.flow.Flow

/** Comentarios entre compañeros de partido y el resumen de IA guardado. */
interface CommentRepository {

    /** Comentarios que recibió el jugador, del más nuevo al más antiguo. */
    fun observeComments(playerId: String): Flow<List<PlayerComment>>

    /** Comentarios que [authorId] dejó en el partido [matchId]. */
    fun observeCommentsBy(authorId: String, matchId: String): Flow<List<PlayerComment>>

    /**
     * Guarda el comentario de [authorId] sobre [playerId] en ese partido y
     * reemplaza el anterior. Con texto vacío, borra el comentario.
     */
    suspend fun saveComment(matchId: String, authorId: String, playerId: String, text: String): Result<Unit>

    /** Marca un comentario como inapropiado para revisarlo. */
    suspend fun reportComment(commentId: String, reporterId: String): Result<Unit>

    /** Último resumen de IA guardado para el jugador, o `null`. */
    suspend fun getSummary(playerId: String): ReviewSummary?

    suspend fun saveSummary(summary: ReviewSummary): Result<Unit>
}
