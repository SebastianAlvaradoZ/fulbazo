package com.vapsa.fulbazo.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorState
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.match.MatchCard
import com.vapsa.fulbazo.ui.theme.FulbazoTheme

/** Parte de la pantalla que ocupa el mapa; el resto es la lista de partidos. */
private const val MAP_HEIGHT_FRACTION = 0.42f

@Composable
fun MapRoute(
    onCreateMatch: () -> Unit,
    onOpenMatch: (matchId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(uiState = uiState, onCreateMatch = onCreateMatch, onOpenMatch = onOpenMatch, modifier = modifier)
}

@Composable
fun MapScreen(
    uiState: MapUiState,
    onCreateMatch: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMatch: (matchId: String) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CourtsMapPreview(
                courts = uiState.courts,
                highlighted = uiState.courtIdsWithMatches,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(MAP_HEIGHT_FRACTION),
            )
            NearbyMatchesList(uiState = uiState, onOpenMatch = onOpenMatch, modifier = Modifier.weight(1f))
        }
        ExtendedFloatingActionButton(
            onClick = onCreateMatch,
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
    onOpenMatch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
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
        when {
            uiState.isLoading -> item { LoadingState(Modifier.fillMaxWidth()) }
            uiState.loadFailed -> item { ErrorState(stringResource(R.string.map_load_error)) }
            uiState.matches.isEmpty() -> item {
                EmptyState(stringResource(R.string.map_empty), iconRes = R.drawable.ic_matches)
            }
            else -> items(uiState.matches, key = { it.matchId }) { item ->
                MatchCard(item, onClick = { onOpenMatch(item.matchId) }, modifier = Modifier.animateItem())
            }
        }
    }
}

/**
 * Vista previa de las losas sobre una cuadrícula, ubicadas según sus
 * coordenadas. Se reemplazará por el mapa real (Google Maps u OpenStreetMap).
 */
@Composable
private fun CourtsMapPreview(
    courts: List<Court>,
    highlighted: Set<String>,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val courtColor = MaterialTheme.colorScheme.outline
    val activeColor = MaterialTheme.colorScheme.primary

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        Box {
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
                if (courts.isEmpty()) return@Canvas

                // Escala las coordenadas al área disponible, dejando un margen.
                val margin = 32.dp.toPx()
                val minLat = courts.minOf { it.latitude }
                val maxLat = courts.maxOf { it.latitude }
                val minLng = courts.minOf { it.longitude }
                val maxLng = courts.maxOf { it.longitude }
                val latSpan = (maxLat - minLat).takeIf { it > 0 } ?: 1.0
                val lngSpan = (maxLng - minLng).takeIf { it > 0 } ?: 1.0
                courts.forEach { court ->
                    val cx = margin + ((court.longitude - minLng) / lngSpan * (size.width - 2 * margin)).toFloat()
                    val cy = margin + ((maxLat - court.latitude) / latSpan * (size.height - 2 * margin)).toFloat()
                    if (court.id in highlighted) {
                        drawCircle(activeColor, radius = 9.dp.toPx(), center = Offset(cx, cy))
                    } else {
                        drawCircle(courtColor, radius = 6.dp.toPx(), center = Offset(cx, cy), style = Stroke(2.dp.toPx()))
                    }
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
            uiState = MapUiState(isLoading = false, courts = previewCourts),
            onCreateMatch = {},
        )
    }
}
