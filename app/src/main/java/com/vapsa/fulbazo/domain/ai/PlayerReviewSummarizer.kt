package com.vapsa.fulbazo.domain.ai

import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.PlayerComment
import com.vapsa.fulbazo.domain.model.ReviewSummary
import com.vapsa.fulbazo.domain.repository.AiRepository
import com.vapsa.fulbazo.domain.repository.CommentRepository
import javax.inject.Inject

/** Resultado de pedir el resumen de un jugador. */
sealed interface SummaryOutcome {
    data class Ready(val summary: ReviewSummary) : SummaryOutcome

    /** Todavía no hay comentarios suficientes para resumir. */
    data class NotEnough(val commentCount: Int) : SummaryOutcome

    data class Failed(val error: Throwable) : SummaryOutcome
}

/**
 * Pide a la IA el resumen de los comentarios de un jugador. Usa el resumen
 * guardado mientras no lleguen comentarios nuevos, para no gastar cuota.
 */
class PlayerReviewSummarizer @Inject constructor(
    private val commentRepository: CommentRepository,
    private val aiRepository: AiRepository,
) {

    /** [comments] van del más nuevo al más antiguo. */
    suspend fun summarize(
        playerId: String,
        nickname: String,
        comments: List<PlayerComment>,
        community: CommunityStats,
    ): SummaryOutcome {
        if (comments.size < MIN_COMMENTS) return SummaryOutcome.NotEnough(comments.size)
        val latestId = comments.first().id

        val cached = commentRepository.getSummary(playerId)
        if (cached != null && cached.lastCommentId == latestId && cached.commentCount == comments.size) {
            return SummaryOutcome.Ready(cached)
        }

        val recent = comments.take(AiPrompts.MAX_COMMENTS)
        return aiRepository.summarizeReviews(nickname, recent, community).fold(
            onSuccess = { text ->
                val summary = ReviewSummary(playerId, text.trim(), latestId, comments.size)
                // Si no se puede guardar, igual se muestra; la próxima vez se genera otra vez.
                commentRepository.saveSummary(summary)
                SummaryOutcome.Ready(summary)
            },
            onFailure = { SummaryOutcome.Failed(it) },
        )
    }

    companion object {
        /** Con menos comentarios el resumen no dice nada útil. */
        const val MIN_COMMENTS = 3
    }
}
