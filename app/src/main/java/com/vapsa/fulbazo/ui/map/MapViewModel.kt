package com.vapsa.fulbazo.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vapsa.fulbazo.BuildConfig
import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.model.GeoPoint
import com.vapsa.fulbazo.domain.model.MatchStatus
import com.vapsa.fulbazo.domain.model.location
import com.vapsa.fulbazo.domain.repository.AuthRepository
import com.vapsa.fulbazo.domain.repository.CourtRepository
import com.vapsa.fulbazo.domain.repository.CourtSyncStatus
import com.vapsa.fulbazo.domain.repository.LocationRepository
import com.vapsa.fulbazo.domain.repository.MatchRepository
import com.vapsa.fulbazo.domain.repository.PlayerRepository
import com.vapsa.fulbazo.ui.match.MatchItem
import com.vapsa.fulbazo.ui.match.toItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/** Estado del permiso de ubicación, visto por la pantalla. */
enum class LocationPermission {
    /** Todavía no lo sabemos (la pantalla lo informa al abrirse). */
    UNKNOWN,
    GRANTED,
    DENIED,
}

/**
 * Estado de la pantalla principal: losas en el mapa y partidos próximos debajo.
 */
data class MapUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val courts: List<Court> = emptyList(),
    /**
     * Partidos próximos, ya filtrados por [selectedCourtId] si hay uno elegido.
     * Con tu ubicación van del más cercano al más lejano; sin ella, por fecha.
     */
    val matches: List<MatchItem> = emptyList(),
    /** Cantidad de partidos próximos por losa (para el filtro y los marcadores). */
    val matchCountByCourt: Map<String, Int> = emptyMap(),
    /** Losa elegida en el filtro de "Partidos cercanos", o `null` para ver todas. */
    val selectedCourtId: String? = null,
    /** Losa abierta en la hoja de detalle, o `null` si está cerrada. */
    val openedCourtId: String? = null,
    val locationPermission: LocationPermission = LocationPermission.UNKNOWN,
    /** Tu ubicación, o `null` si no hay permiso o no se pudo obtener. */
    val userLocation: GeoPoint? = null,
    /** `false` si falta la clave de Google Maps: se muestra un aviso en lugar del mapa. */
    val mapsAvailable: Boolean = true,
    /** Estado de la descarga de losas (con caché local). */
    val courtSync: CourtSyncStatus = CourtSyncStatus.UP_TO_DATE,
) {
    /** Sin conexión, pero con losas guardadas: se muestran con un aviso. */
    val showingSavedCourts: Boolean get() = courtSync == CourtSyncStatus.OFFLINE && courts.isNotEmpty()

    /** Primera vez sin conexión: no hay losas guardadas que mostrar. */
    val courtsUnavailable: Boolean get() = courtSync == CourtSyncStatus.OFFLINE && courts.isEmpty()

    /** Descargando las losas por primera vez. */
    val courtsLoading: Boolean get() = courtSync == CourtSyncStatus.SYNCING && courts.isEmpty()

    /** Ids de las losas que tienen al menos un partido próximo. */
    val courtIdsWithMatches: Set<String>
        get() = matchCountByCourt.filterValues { it > 0 }.keys

    /** Losas que aparecen en el filtro: primero las que tienen partidos. */
    val filterCourts: List<Court>
        get() = courts.sortedByDescending { matchCountByCourt[it.id] ?: 0 }

    val sortedByDistance: Boolean get() = userLocation != null
}

@HiltViewModel
class MapViewModel(
    authRepository: AuthRepository,
    private val courtRepository: CourtRepository,
    matchRepository: MatchRepository,
    playerRepository: PlayerRepository,
    private val locationRepository: LocationRepository,
    private val mapsConfigured: Boolean = true,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) : ViewModel() {

    /** Constructor que usa Hilt; las pruebas usan el principal para fijar la hora y los tiempos. */
    @Inject constructor(
        authRepository: AuthRepository,
        courtRepository: CourtRepository,
        matchRepository: MatchRepository,
        playerRepository: PlayerRepository,
        locationRepository: LocationRepository,
    ) : this(
        authRepository = authRepository,
        courtRepository = courtRepository,
        matchRepository = matchRepository,
        playerRepository = playerRepository,
        locationRepository = locationRepository,
        mapsConfigured = BuildConfig.HAS_MAPS_API_KEY,
        now = { LocalDateTime.now() },
    )

    private val selectedCourtId = MutableStateFlow<String?>(null)
    private val openedCourtId = MutableStateFlow<String?>(null)
    private val permission = MutableStateFlow(LocationPermission.UNKNOWN)
    private val userLocation = MutableStateFlow<GeoPoint?>(null)
    private var locationJob: Job? = null

    private val content = combine(
        courtRepository.observeCourts(),
        matchRepository.observeMatches(),
        playerRepository.observePlayers(),
        authRepository.currentUser,
        userLocation,
    ) { courts, matches, players, user, location ->
        val courtsById = courts.associateBy { it.id }
        val upcoming = matches
            .filter { it.status == MatchStatus.OPEN && it.dateTime.isAfter(now()) }
            .map { match ->
                match.toItem(courtsById, players, user?.id).copy(
                    distanceKm = location?.let { courtsById[match.courtId]?.location?.distanceKmTo(it) },
                )
            }
            .sortedNearestFirst()
        MapUiState(
            isLoading = false,
            courts = courts,
            matches = upcoming,
            matchCountByCourt = upcoming.groupingBy { it.courtId }.eachCount(),
            userLocation = location,
        )
    }
        .catch { emit(MapUiState(isLoading = false, loadFailed = true)) }

    val uiState: StateFlow<MapUiState> = combine(
        content,
        selectedCourtId,
        openedCourtId,
        permission,
        courtRepository.syncStatus,
    ) { state, selected, opened, permission, courtSync ->
        state.copy(
            courtSync = courtSync,
            matches = if (selected == null) state.matches else state.matches.filter { it.courtId == selected },
            selectedCourtId = selected,
            openedCourtId = opened,
            locationPermission = permission,
            mapsAvailable = mapsConfigured,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState(mapsAvailable = mapsConfigured),
        )

    /**
     * La pantalla informa si hay permiso de ubicación (al abrirse, al volver de
     * Ajustes o al responder el diálogo). Con permiso, busca tu ubicación.
     */
    fun onLocationPermissionChanged(granted: Boolean) {
        permission.value = if (granted) LocationPermission.GRANTED else LocationPermission.DENIED
        if (!granted) {
            locationJob?.cancel()
            userLocation.value = null
        } else if (userLocation.value == null && locationJob?.isActive != true) {
            locationJob = viewModelScope.launch {
                userLocation.value = locationRepository.currentLocation()
            }
        }
    }

    /** Filtra "Partidos cercanos" por losa; tocar la misma losa otra vez quita el filtro. */
    /** "Reintentar" del aviso de losas sin conexión. */
    fun retryCourts() {
        viewModelScope.launch { courtRepository.refresh() }
    }

    fun onFilterCourt(courtId: String?) {
        selectedCourtId.update { current -> if (courtId == current) null else courtId }
    }

    /** Abre la hoja con el detalle de la losa (desde un marcador o el filtro). */
    fun openCourt(courtId: String) {
        openedCourtId.value = courtId
    }

    fun closeCourt() {
        openedCourtId.value = null
    }
}

/**
 * Ordena del más cercano al más lejano; a igual distancia (misma losa) o sin
 * ubicación, por fecha.
 */
internal fun List<MatchItem>.sortedNearestFirst(): List<MatchItem> =
    sortedWith(compareBy<MatchItem, Double?>(nullsLast()) { it.distanceKm }.thenBy { it.dateTime })
