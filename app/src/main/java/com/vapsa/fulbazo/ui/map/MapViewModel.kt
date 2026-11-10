package com.vapsa.fulbazo.ui.map

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime

/**
 * Estado de la pantalla principal: losas en el mapa y partidos próximos debajo.
 */
data class MapUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val courts: List<Court> = emptyList(),
    val matches: List<MatchItem> = emptyList(),
) {
    /** Ids de las losas que tienen al menos un partido abierto. */
    val courtIdsWithMatches: Set<String>
        get() = matches.map { it.courtId }.toSet()
}

class MapViewModel(
    authRepository: AuthRepository,
    courtRepository: CourtRepository,
    matchRepository: MatchRepository,
    playerRepository: PlayerRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

    val uiState: StateFlow<MapUiState> = combine(
        courtRepository.observeCourts(),
        matchRepository.observeMatches(),
        playerRepository.observePlayers(),
        authRepository.currentUser,
    ) { courts, matches, players, user ->
        val courtsById = courts.associateBy { it.id }
        val upcoming = matches
            .filter { it.status == MatchStatus.OPEN && it.dateTime.isAfter(now()) }
            .sortedBy { it.dateTime }
            .map { it.toItem(courtsById, players, user?.id) }
        MapUiState(isLoading = false, courts = courts, matches = upcoming)
    }
        .catch { emit(MapUiState(isLoading = false, loadFailed = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState(),
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MapViewModel(
                    AppContainer.authRepository,
                    AppContainer.courtRepository,
                    AppContainer.matchRepository,
                    AppContainer.playerRepository,
                )
            }
        }
    }
}
