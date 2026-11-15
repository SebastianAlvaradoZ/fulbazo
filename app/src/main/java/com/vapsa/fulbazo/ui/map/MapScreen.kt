package com.vapsa.fulbazo.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorState
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.court.CourtDetailSheet
import com.vapsa.fulbazo.ui.match.MatchCard
import com.vapsa.fulbazo.ui.theme.FulbazoTheme

/** Parte de la pantalla que ocupa el mapa; el resto es la lista de partidos. */
private const val MAP_HEIGHT_FRACTION = 0.42f

/** Etiqueta de la lista de partidos cercanos, para las pruebas automáticas. */
const val MAP_MATCHES_LIST_TAG = "map_matches_list"

@Composable
fun MapRoute(
    /** Con `null` abre Crear partido sin losa; con un id, con esa losa elegida. */
    onCreateMatch: (courtId: String?) -> Unit,
    onOpenMatch: (matchId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(
        uiState = uiState,
        actions = MapActions(
            onCreateMatch = { onCreateMatch(null) },
            onOpenMatch = onOpenMatch,
            onOpenCourt = viewModel::openCourt,
            onFilterCourt = viewModel::onFilterCourt,
        ),
        modifier = modifier,
    )

    // La misma hoja la abrirá el marcador del mapa real.
    uiState.openedCourtId?.let { courtId ->
        CourtDetailSheet(
            courtId = courtId,
            onDismiss = viewModel::closeCourt,
            onOpenMatch = { matchId ->
                viewModel.closeCourt()
                onOpenMatch(matchId)
            },
            onCreateMatchHere = { id ->
                viewModel.closeCourt()
                onCreateMatch(id)
            },
        )
    }
}

data class MapActions(
    val onCreateMatch: () -> Unit = {},
    val onOpenMatch: (matchId: String) -> Unit = {},
    val onOpenCourt: (courtId: String) -> Unit = {},
    val onFilterCourt: (courtId: String?) -> Unit = {},
)

@Composable
fun MapScreen(
    uiState: MapUiState,
    actions: MapActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CourtsMapPreview(
                courts = uiState.courts,
                matchCountByCourt = uiState.matchCountByCourt,
                onCourtClick = actions.onOpenCourt,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(MAP_HEIGHT_FRACTION),
            )
            NearbyMatchesList(uiState = uiState, actions = actions, modifier = Modifier.weight(1f))
        }
        ExtendedFloatingActionButton(
            onClick = actions.onCreateMatch,
            icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
            text = { Text(stringResource(R.string.map_create_match)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}

@Composable
private fun NearbyMatchesList(
    uiState: MapUiState,
    actions: MapActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MAP_MATCHES_LIST_TAG),
        // Espacio abajo para que el botón flotante no tape la última tarjeta.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.map_nearby_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.map_nearby_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (uiState.courts.isNotEmpty()) {
            item { CourtFilter(uiState, actions) }
        }
        when {
            uiState.isLoading -> item { LoadingState(Modifier.fillMaxWidth()) }
            uiState.loadFailed -> item { ErrorState(stringResource(R.string.map_load_error)) }
            uiState.matches.isEmpty() -> item {
                EmptyState(
                    stringResource(
                        if (uiState.selectedCourtId == null) R.string.map_empty else R.string.map_filter_empty,
                    ),
                    iconRes = R.drawable.ic_matches,
                )
            }
            else -> items(uiState.matches, key = { it.matchId }) { item ->
                MatchCard(item, onClick = { actions.onOpenMatch(item.matchId) }, modifier = Modifier.animateItem())
            }
        }
    }
}

/** Chips para filtrar los partidos por losa, con la cantidad de partidos de cada una. */
@Composable
private fun CourtFilter(uiState: MapUiState, actions: MapActions) {
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = uiState.selectedCourtId == null,
                    onClick = { actions.onFilterCourt(null) },
                    label = { Text(stringResource(R.string.map_filter_all)) },
                )
            }
            items(uiState.filterCourts, key = { it.id }) { court ->
                FilterChip(
                    selected = uiState.selectedCourtId == court.id,
                    onClick = { actions.onFilterCourt(court.id) },
                    label = {
                        Text(stringResource(R.string.map_filter_court, court.name, uiState.matchCountByCourt[court.id] ?: 0))
                    },
                )
            }
        }
        // Con una losa elegida, se puede abrir su detalle.
        AnimatedVisibility(visible = uiState.selectedCourtId != null) {
            Row {
                TextButton(onClick = { uiState.selectedCourtId?.let(actions.onOpenCourt) }) {
                    Text(stringResource(R.string.map_view_court))
                }
            }
        }
    }
}

/**
 * Vista previa de las losas sobre una cuadrícula, ubicadas según sus
 * coordenadas. Cada losa se puede tocar para ver su detalle. Se reemplazará
 * por el mapa real (Google Maps u OpenStreetMap), cuyos marcadores abrirán la
 * misma hoja.
 */
@Composable
private fun CourtsMapPreview(
    courts: List<Court>,
    matchCountByCourt: Map<String, Int>,
    onCourtClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        BoxWithConstraints {
            Canvas(Modifier.fillMaxSize()) {
                val step = 32.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height))
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y))
                    y += step
                }
            }

            if (courts.isNotEmpty()) {
                // Escala las coordenadas al área disponible, dejando un margen.
                val margin = 32.dp
                val minLat = courts.minOf { it.latitude }
                val maxLat = courts.maxOf { it.latitude }
                val minLng = courts.minOf { it.longitude }
                val maxLng = courts.maxOf { it.longitude }
                val latSpan = (maxLat - minLat).takeIf { it > 0 } ?: 1.0
                val lngSpan = (maxLng - minLng).takeIf { it > 0 } ?: 1.0
                courts.forEach { court ->
                    val cx = margin + (maxWidth - margin * 2) * ((court.longitude - minLng) / lngSpan).toFloat()
                    val cy = margin + (maxHeight - margin * 2) * ((maxLat - court.latitude) / latSpan).toFloat()
                    CourtMarker(
                        court = court,
                        matchCount = matchCountByCourt[court.id] ?: 0,
                        onClick = { onCourtClick(court.id) },
                        // El área táctil (48 dp) se centra en el punto de la losa.
                        modifier = Modifier.offset(x = cx - 24.dp, y = cy - 24.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.map_preview_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            )
        }
    }
}

/** Punto de una losa con área táctil accesible de 48 dp. */
@Composable
private fun CourtMarker(
    court: Court,
    matchCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasMatches = matchCount > 0
    val dotSize by animateDpAsState(targetValue = if (hasMatches) 18.dp else 12.dp, label = "markerSize")
    val description = stringResource(R.string.map_court_marker, court.name, matchCount)
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(dotSize),
            shape = CircleShape,
            color = if (hasMatches) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            border = if (hasMatches) null else BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        ) {}
    }
}

/** Losas de ejemplo solo para la vista previa de Android Studio. */
private val previewCourts = listOf(
    Court("p1", "Losa Umacollo", "Yanahuara", -16.3935, -71.5480),
    Court("p2", "Losa Cayma", "Cayma", -16.3720, -71.5450),
    Court("p3", "Losa Paucarpata", "Paucarpata", -16.4280, -71.5050),
)

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun MapScreenPreview() {
    FulbazoTheme {
        MapScreen(
            uiState = MapUiState(
                isLoading = false,
                courts = previewCourts,
                matchCountByCourt = mapOf("p1" to 2),
            ),
            actions = MapActions(),
        )
    }
}
