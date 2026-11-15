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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

/**
 * Estado de la pantalla principal: losas en el mapa y partidos próximos debajo.
 */
data class MapUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val courts: List<Court> = emptyList(),
    /** Partidos próximos, ya filtrados por [selectedCourtId] si hay uno elegido. */
    val matches: List<MatchItem> = emptyList(),
    /** Cantidad de partidos próximos por losa (para el filtro y los puntos del mapa). */
    val matchCountByCourt: Map<String, Int> = emptyMap(),
    /** Losa elegida en el filtro de "Partidos cercanos", o `null` para ver todas. */
    val selectedCourtId: String? = null,
    /** Losa abierta en la hoja de detalle, o `null` si está cerrada. */
    val openedCourtId: String? = null,
) {
    /** Ids de las losas que tienen al menos un partido próximo. */
    val courtIdsWithMatches: Set<String>
        get() = matchCountByCourt.filterValues { it > 0 }.keys

    /** Losas que aparecen en el filtro: primero las que tienen partidos. */
    val filterCourts: List<Court>
        get() = courts.sortedByDescending { matchCountByCourt[it.id] ?: 0 }
}

class MapViewModel(
    authRepository: AuthRepository,
    courtRepository: CourtRepository,
    matchRepository: MatchRepository,
    playerRepository: PlayerRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

    private val selectedCourtId = MutableStateFlow<String?>(null)
    private val openedCourtId = MutableStateFlow<String?>(null)

    private val content = combine(
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
        MapUiState(
            isLoading = false,
            courts = courts,
            matches = upcoming,
            matchCountByCourt = upcoming.groupingBy { it.courtId }.eachCount(),
        )
    }
        .catch { emit(MapUiState(isLoading = false, loadFailed = true)) }

    val uiState: StateFlow<MapUiState> = combine(content, selectedCourtId, openedCourtId) { state, selected, opened ->
        state.copy(
            matches = if (selected == null) state.matches else state.matches.filter { it.courtId == selected },
            selectedCourtId = selected,
            openedCourtId = opened,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState(),
        )

    /** Filtra "Partidos cercanos" por losa; tocar la misma losa otra vez quita el filtro. */
    fun onFilterCourt(courtId: String?) {
        selectedCourtId.update { current -> if (courtId == current) null else courtId }
    }

    /** Abre la hoja con el detalle de la losa (desde el mapa o el filtro). */
    fun openCourt(courtId: String) {
        openedCourtId.value = courtId
    }

    fun closeCourt() {
        openedCourtId.value = null
    }

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
