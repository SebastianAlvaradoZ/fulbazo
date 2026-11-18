package com.vapsa.fulbazo.ui.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.domain.ai.PlayerReviewSummarizer
import com.vapsa.fulbazo.domain.ai.SummaryOutcome
import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.PlayerComment
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CommentRepository
import com.vapsa.fulbazo.domain.repository.NoConnectionException
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.domain.repository.StatsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Estado del resumen de IA. */
sealed interface AiSummaryState {
    data object Loading : AiSummaryState

    /** Hay menos de [PlayerReviewSummarizer.MIN_COMMENTS] comentarios. */
    data object NotEnough : AiSummaryState

    data class Ready(val text: String) : AiSummaryState

    /** No se pudo generar; [offline] si fue por falta de internet. */
    data class Error(val offline: Boolean = false) : AiSummaryState
}

data class CommentItem(
    val id: String,
    val authorNickname: String,
    val authorPhotoUri: String?,
    val text: String,
    val createdAt: LocalDateTime,
    /** Lo escribí yo: no lo puedo reportar. */
    val isMine: Boolean,
)

enum class ReviewsMessage { REPORTED, REPORT_FAILED }

data class PlayerReviewsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val comments: List<CommentItem> = emptyList(),
    val summary: AiSummaryState = AiSummaryState.Loading,
    /** Comentarios que ya reporté en esta pantalla. */
    val reportedIds: Set<String> = emptySet(),
    val message: ReviewsMessage? = null,
)

/**
 * Comentarios que recibió un jugador y el resumen que hace Gemini con ellos.
 * Se usa en su perfil y en las solicitudes que revisa el anfitrión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = PlayerReviewsViewModel.Factory::class)
class PlayerReviewsViewModel @AssistedInject constructor(
    @Assisted private val playerId: String,
    authRepository: AuthRepository,
    playerRepository: PlayerRepository,
    statsRepository: StatsRepository,
    private val commentRepository: CommentRepository,
    private val summarizer: PlayerReviewSummarizer,
) : ViewModel() {

    private val myId: String? = authRepository.currentUser.value?.id

    private val summaryRetry = MutableStateFlow(0)
    private val reportedIds = MutableStateFlow(emptySet<String>())
    private val message = MutableStateFlow<ReviewsMessage?>(null)

    private val data = combine(
        commentRepository.observeComments(playerId),
        playerRepository.observePlayers(),
        statsRepository.observeProfile(playerId),
    ) { comments, players, profile -> Triple(comments, players, profile) }

    private val comments = data.map { (comments, players, _) ->
        comments.map { comment ->
            val author = players[comment.authorId]
            CommentItem(
                id = comment.id,
                authorNickname = author?.nickname ?: comment.authorId,
                authorPhotoUri = author?.photoUri,
                text = comment.text,
                createdAt = comment.createdAt,
                isMine = comment.authorId == myId,
            )
        }
    }

    /**
     * Se pide un resumen nuevo solo cuando cambian los comentarios (o al reintentar):
     * moverse por la pantalla no gasta cuota de Gemini.
     */
    private val summary = combine(data, summaryRetry) { (comments, players, profile), retry ->
        SummaryRequest(comments, players[playerId]?.nickname ?: playerId, profile.community, retry)
    }
        .distinctUntilChangedBy { Triple(it.comments.firstOrNull()?.id, it.comments.size, it.retry) }
        .transformLatest { request ->
            emit(AiSummaryState.Loading)
            val outcome = summarizer.summarize(playerId, request.nickname, request.comments, request.community)
            emit(
                when (outcome) {
                    is SummaryOutcome.Ready -> AiSummaryState.Ready(outcome.summary.text)
                    is SummaryOutcome.NotEnough -> AiSummaryState.NotEnough
                    is SummaryOutcome.Failed -> AiSummaryState.Error(offline = outcome.error is NoConnectionException)
                },
            )
        }

    val uiState: StateFlow<PlayerReviewsUiState> = combine(
        comments,
        summary,
        reportedIds,
        message,
    ) { items, summaryState, reported, currentMessage ->
        PlayerReviewsUiState(
            isLoading = false,
            comments = items,
            summary = summaryState,
            reportedIds = reported,
            message = currentMessage,
        )
    }
        .catch { emit(PlayerReviewsUiState(isLoading = false, loadFailed = true, summary = AiSummaryState.Error())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerReviewsUiState(),
        )

    fun retrySummary() {
        summaryRetry.update { it + 1 }
    }

    /** Reporta un comentario inapropiado (no se puede reportar uno propio). */
    fun report(commentId: String) {
        val reporter = myId ?: return
        val comment = uiState.value.comments.firstOrNull { it.id == commentId } ?: return
        if (comment.isMine || commentId in reportedIds.value) return
        viewModelScope.launch {
            val result = commentRepository.reportComment(commentId, reporter)
            if (result.isSuccess) {
                reportedIds.update { it + commentId }
                message.value = ReviewsMessage.REPORTED
            } else {
                message.value = ReviewsMessage.REPORT_FAILED
            }
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    private data class SummaryRequest(
        val comments: List<PlayerComment>,
        val nickname: String,
        val community: CommunityStats,
        val retry: Int,
    )

    /** Hilt crea el ViewModel con el [playerId] del jugador. */
    @AssistedFactory
    interface Factory {
        fun create(playerId: String): PlayerReviewsViewModel
    }
}
