package com.vapsa.fulbazo.ui.match.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.domain.model.JoinRequestStatus
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.JoinRequestRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.ui.match.toItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Detalle de un partido: datos, jugadores y solicitudes para unirse.
 */
@HiltViewModel(assistedFactory = MatchDetailViewModel.Factory::class)
class MatchDetailViewModel(
    private val matchId: String,
    authRepository: AuthRepository,
    private val matchRepository: MatchRepository,
    courtRepository: CourtRepository,
    playerRepository: PlayerRepository,
    private val joinRequestRepository: JoinRequestRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

    /** Constructor que usa Hilt; las pruebas usan el principal para fijar la hora y los tiempos. */
    @AssistedInject constructor(
        @Assisted matchId: String,
        authRepository: AuthRepository,
        matchRepository: MatchRepository,
        courtRepository: CourtRepository,
        playerRepository: PlayerRepository,
        joinRequestRepository: JoinRequestRepository,
    ) : this(
        matchId = matchId,
        authRepository = authRepository,
        matchRepository = matchRepository,
        courtRepository = courtRepository,
        playerRepository = playerRepository,
        joinRequestRepository = joinRequestRepository,
        now = { LocalDateTime.now() },
    )

    private val userId: String? = authRepository.currentUser.value?.id

    private val isBusy = MutableStateFlow(false)
    private val busyRequestIds = MutableStateFlow<Set<String>>(emptySet())
    private val error = MutableStateFlow<MatchDetailError?>(null)
    private val message = MutableStateFlow<MatchDetailMessage?>(null)

    private val content = combine(
        matchRepository.observeMatches(),
        courtRepository.observeCourts(),
        playerRepository.observePlayers(),
        joinRequestRepository.observeForMatch(matchId),
    ) { matches, courts, players, requests ->
        val match = matches.firstOrNull { it.id == matchId }
            ?: return@combine MatchDetailUiState(isLoading = false, notFound = true)

        val myRequest = requests.lastOrNull { it.userId == userId }
        val role = when {
            match.hostId == userId -> ViewerRole.HOST
            userId != null && userId in match.playerIds -> ViewerRole.PLAYER
            myRequest?.status == JoinRequestStatus.PENDING -> ViewerRole.REQUEST_PENDING
            myRequest?.status == JoinRequestStatus.REJECTED -> ViewerRole.REQUEST_REJECTED
            else -> ViewerRole.NONE
        }
        MatchDetailUiState(
            isLoading = false,
            match = match.toItem(courts.associateBy { it.id }, players, userId),
            // El anfitrión siempre aparece primero.
            players = match.playerIds
                .sortedByDescending { it == match.hostId }
                .mapNotNull { id -> players[id]?.let { PlayerItem(it, isHost = id == match.hostId) } },
            role = role,
            isFinished = match.status == MatchStatus.FINISHED || !match.dateTime.isAfter(now()),
            myPendingRequestId = myRequest?.takeIf { it.status == JoinRequestStatus.PENDING }?.id,
            pendingRequests = if (role == ViewerRole.HOST) {
                requests
                    .filter { it.status == JoinRequestStatus.PENDING }
                    .mapNotNull { request ->
                        players[request.userId]?.let { PendingRequestItem(request.id, it) }
                    }
            } else {
                emptyList()
            },
        )
    }

    val uiState: StateFlow<MatchDetailUiState> = combine(
        content,
        isBusy,
        busyRequestIds,
        error,
        message,
    ) { state, busy, busyIds, currentError, currentMessage ->
        state.copy(isBusy = busy, busyRequestIds = busyIds, error = currentError, message = currentMessage)
    }
        .catch { emit(MatchDetailUiState(isLoading = false, loadFailed = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MatchDetailUiState(),
        )

    fun requestToJoin() = runOwnAction(MatchDetailMessage.REQUEST_SENT) {
        joinRequestRepository.sendRequest(matchId, requireNotNull(userId))
    }

    fun cancelRequest() {
        val requestId = uiState.value.myPendingRequestId ?: return
        runOwnAction(MatchDetailMessage.REQUEST_CANCELLED) { joinRequestRepository.cancelRequest(requestId) }
    }

    fun leaveMatch() = runOwnAction(MatchDetailMessage.LEFT_MATCH) {
        matchRepository.leaveMatch(matchId, requireNotNull(userId))
    }

    /** Solo el anfitrión: al aceptar, el jugador entra si todavía hay cupo. */
    fun respondToRequest(requestId: String, accept: Boolean) {
        val state = uiState.value
        if (state.role != ViewerRole.HOST || requestId in busyRequestIds.value) return
        val playerId = state.pendingRequests.firstOrNull { it.requestId == requestId }?.player?.id ?: return

        busyRequestIds.update { it + requestId }
        viewModelScope.launch {
            if (accept) {
                val joined = matchRepository.joinMatch(matchId, playerId)
                if (joined.isFailure) {
                    error.value = MatchDetailError.MATCH_FULL
                    busyRequestIds.update { it - requestId }
                    return@launch
                }
            }
            if (joinRequestRepository.respond(requestId, accept).isFailure) {
                error.value = MatchDetailError.FAILED
            }
            busyRequestIds.update { it - requestId }
        }
    }

    fun onErrorShown() {
        error.value = null
    }

    fun onMessageShown() {
        message.value = null
    }

    private fun runOwnAction(successMessage: MatchDetailMessage, action: suspend () -> Result<Unit>) {
        if (userId == null || isBusy.value) return
        isBusy.value = true
        viewModelScope.launch {
            val result = action()
            isBusy.value = false
            if (result.isSuccess) message.value = successMessage else error.value = MatchDetailError.FAILED
        }
    }

    /** Hilt crea el ViewModel con el [matchId] que recibe la pantalla. */
    @AssistedFactory
    interface Factory {
        fun create(matchId: String): MatchDetailViewModel
    }
}
