package com.vapsa.fulbazo.ui.court

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.ui.match.MatchItem
import com.vapsa.fulbazo.ui.match.toItem
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

data class CourtDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val loadFailed: Boolean = false,
    val court: Court? = null,
    /** Partidos futuros (abiertos o llenos) en esta losa, del más cercano al más lejano. */
    val upcomingMatches: List<MatchItem> = emptyList(),
)

/**
 * Detalle de una losa con sus próximos partidos. Lo usa la hoja que se abre
 * desde el mapa (hoy la vista previa; luego el marcador del mapa real).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CourtDetailViewModel.Factory::class)
class CourtDetailViewModel(
    private val courtId: String,
    authRepository: AuthRepository,
    courtRepository: CourtRepository,
    matchRepository: MatchRepository,
    playerRepository: PlayerRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

    /** Constructor que usa Hilt; las pruebas usan el principal para fijar la hora y los tiempos. */
    @AssistedInject constructor(
        @Assisted courtId: String,
        authRepository: AuthRepository,
        courtRepository: CourtRepository,
        matchRepository: MatchRepository,
        playerRepository: PlayerRepository,
    ) : this(
        courtId = courtId,
        authRepository = authRepository,
        courtRepository = courtRepository,
        matchRepository = matchRepository,
        playerRepository = playerRepository,
        now = { LocalDateTime.now() },
    )

    /** Cambia para volver a suscribirse después de un error. */
    private val reloadTrigger = MutableStateFlow(0)

    val uiState: StateFlow<CourtDetailUiState> = reloadTrigger.flatMapLatest {
        combine(
            courtRepository.observeCourts(),
            matchRepository.observeMatches(),
            playerRepository.observePlayers(),
            authRepository.currentUser,
        ) { courts, matches, players, user ->
            val court = courts.firstOrNull { it.id == courtId }
                ?: return@combine CourtDetailUiState(isLoading = false, notFound = true)
            CourtDetailUiState(
                isLoading = false,
                court = court,
                upcomingMatches = matches
                    .filter {
                        it.courtId == courtId &&
                            (it.status == MatchStatus.OPEN || it.status == MatchStatus.FULL) &&
                            it.dateTime.isAfter(now())
                    }
                    .sortedBy { it.dateTime }
                    .map { it.toItem(courts.associateBy { c -> c.id }, players, user?.id) },
            )
        }.catch { emit(CourtDetailUiState(isLoading = false, loadFailed = true)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CourtDetailUiState(),
    )

    fun retry() {
        reloadTrigger.update { it + 1 }
    }

    /** Hilt crea el ViewModel con el [courtId] que recibe la pantalla. */
    @AssistedFactory
    interface Factory {
        fun create(courtId: String): CourtDetailViewModel
    }
}
