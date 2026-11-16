package com.vapsa.fulbazo.data.repository

import com.vapsa.fulbazo.domain.model.PlayerComment
import com.vapsa.fulbazo.domain.model.ReviewSummary
import com.vapsa.fulbazo.domain.repository.CommentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

/**
 * Comentarios en memoria para las pruebas. carlitos10 ya tiene varios
 * comentarios (alcanza para el resumen de IA) y rodri tiene uno solo.
 */
class FakeCommentRepository(
    private val latencyMillis: Long = 300,
    now: LocalDateTime = LocalDateTime.now(),
) : CommentRepository {

    private val comments = MutableStateFlow(
        listOf(
            PlayerComment("c1", "match-past-paucarpata", "demo", "carlos", "Muy rápido y siempre busca el arco.", now.minusDays(10)),
            PlayerComment("c2", "match-past-selva", "maria", "carlos", "Buen compañero, aunque a veces no pasa la pelota.", now.minusDays(2)),
            PlayerComment("c3", "match-past-selva", "valeria", "carlos", "Llegó puntual y metió dos goles.", now.minusDays(2).plusMinutes(5)),
            PlayerComment("c4", "match-past-paucarpata", "carlos", "rodrigo", "Juega limpio.", now.minusDays(10)),
        ),
    )
    private val summaries = mutableMapOf<String, ReviewSummary>()

    /** Reportes recibidos, para revisarlos en las pruebas. */
    val reports = MutableStateFlow(emptySet<Pair<String, String>>())

    override fun observeComments(playerId: String): Flow<List<PlayerComment>> =
        comments.map { list -> list.filter { it.playerId == playerId }.sortedByDescending { it.createdAt } }

    override fun observeCommentsBy(authorId: String, matchId: String): Flow<List<PlayerComment>> =
        comments.map { list -> list.filter { it.authorId == authorId && it.matchId == matchId } }

    override suspend fun saveComment(matchId: String, authorId: String, playerId: String, text: String): Result<Unit> {
        delay(latencyMillis)
        val id = "${matchId}_${authorId}_$playerId"
        val clean = text.trim().take(PlayerComment.MAX_LENGTH)
        comments.update { list ->
            val others = list.filterNot { it.matchId == matchId && it.authorId == authorId && it.playerId == playerId }
            if (clean.isEmpty()) others else others + PlayerComment(id, matchId, authorId, playerId, clean, LocalDateTime.now())
        }
        return Result.success(Unit)
    }

    override suspend fun reportComment(commentId: String, reporterId: String): Result<Unit> {
        delay(latencyMillis)
        reports.update { it + (commentId to reporterId) }
        return Result.success(Unit)
    }

    override suspend fun getSummary(playerId: String): ReviewSummary? = summaries[playerId]

    override suspend fun saveSummary(summary: ReviewSummary): Result<Unit> {
        summaries[summary.playerId] = summary
        return Result.success(Unit)
    }
}
