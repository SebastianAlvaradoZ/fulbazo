package com.vapsa.fulbazo.ui.match.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.domain.model.JoinRequestStatus
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.JoinRequestRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.ui.match.MatchItem
import com.vapsa.fulbazo.ui.match.toItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class MyRequestItem(
    val requestId: String,
    val status: JoinRequestStatus,
    /** `null` si el partido ya no existe. */
    val match: MatchItem?,
)

data class MyMatchesUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** Partidos futuros donde soy anfitrión o estoy inscrito, del más cercano al más lejano. */
    val upcoming: List<MatchItem> = emptyList(),
    /** Mis solicitudes: primero las pendientes. */
    val requests: List<MyRequestItem> = emptyList(),
    /** Partidos ya jugados, del más reciente al más antiguo. */
    val history: List<MatchItem> = emptyList(),
    val busyRequestIds: Set<String> = emptySet(),
    val actionFailed: Boolean = false,
) {
    val pendingRequestsCount: Int get() = requests.count { it.status == JoinRequestStatus.PENDING }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyMatchesViewModel(
    authRepository: AuthRepository,
    matchRepository: MatchRepository,
    courtRepository: CourtRepository,
    playerRepository: PlayerRepository,
    private val joinRequestRepository: JoinRequestRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

    /** Constructor que usa Hilt; las pruebas usan el principal para fijar la hora y los tiempos. */
    @Inject constructor(
        authRepository: AuthRepository,
        matchRepository: MatchRepository,
        courtRepository: CourtRepository,
        playerRepository: PlayerRepository,
        joinRequestRepository: JoinRequestRepository,
    ) : this(
        authRepository = authRepository,
        matchRepository = matchRepository,
        courtRepository = courtRepository,
        playerRepository = playerRepository,
        joinRequestRepository = joinRequestRepository,
        now = { LocalDateTime.now() },
    )

    private val userId: String? = authRepository.currentUser.value?.id

    /** Cambia para volver a suscribirse a los datos después de un error. */
    private val reloadTrigger = MutableStateFlow(0)
    private val busyRequestIds = MutableStateFlow<Set<String>>(emptySet())
    private val actionFailed = MutableStateFlow(false)

    private val content = reloadTrigger.flatMapLatest {
        combine(
            matchRepository.observeMatches(),
            courtRepository.observeCourts(),
            playerRepository.observePlayers(),
            userId?.let { joinRequestRepository.observeByUser(it) } ?: flowOf(emptyList()),
        ) { matches, courts, players, requests ->
            val courtsById = courts.associateBy { it.id }
            val matchesById = matches.associateBy { it.id }
            val mine = matches.filter { userId != null && userId in it.playerIds }
            val (past, future) = mine.partition { it.status == MatchStatus.FINISHED || !it.dateTime.isAfter(now()) }

            MyMatchesUiState(
                isLoading = false,
                upcoming = future
                    .filter { it.status != MatchStatus.CANCELLED }
                    .sortedBy { it.dateTime }
                    .map { it.toItem(courtsById, players, userId) },
                requests = requests
                    .sortedBy { it.status != JoinRequestStatus.PENDING }
                    .map { request ->
                        MyRequestItem(
                            requestId = request.id,
                            status = request.status,
                            match = matchesById[request.matchId]?.toItem(courtsById, players, userId),
                        )
                    },
                history = past
                    .sortedByDescending { it.dateTime }
                    .map { it.toItem(courtsById, players, userId) },
            )
        }.catch { emit(MyMatchesUiState(isLoading = false, loadFailed = true)) }
    }

    val uiState: StateFlow<MyMatchesUiState> = combine(content, busyRequestIds, actionFailed) { state, busy, failed ->
        state.copy(busyRequestIds = busy, actionFailed = failed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyMatchesUiState(),
    )

    fun retry() {
        reloadTrigger.update { it + 1 }
    }

    fun cancelRequest(requestId: String) {
        if (requestId in busyRequestIds.value) return
        busyRequestIds.update { it + requestId }
        viewModelScope.launch {
            if (joinRequestRepository.cancelRequest(requestId).isFailure) actionFailed.value = true
            busyRequestIds.update { it - requestId }
        }
    }

    fun onErrorShown() {
        actionFailed.value = false
    }
}
