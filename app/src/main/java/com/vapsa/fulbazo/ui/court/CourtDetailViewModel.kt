package com.vapsa.fulbazo.ui.court

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vapsa.fulbazo.di.AppContainer
import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.ui.match.MatchItem
import com.vapsa.fulbazo.ui.match.toItem
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
class CourtDetailViewModel(
    private val courtId: String,
    authRepository: AuthRepository,
    courtRepository: CourtRepository,
    matchRepository: MatchRepository,
    playerRepository: PlayerRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

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

    companion object {
        fun factory(courtId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CourtDetailViewModel(
                    courtId,
                    AppContainer.authRepository,
                    AppContainer.courtRepository,
                    AppContainer.matchRepository,
                    AppContainer.playerRepository,
                )
            }
        }
    }
}
