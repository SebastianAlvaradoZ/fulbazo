package com.vapsa.fulbazo.ui.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.model.GeoPoint
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorState
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.court.CourtDetailSheet
import com.vapsa.fulbazo.ui.match.MatchCard
import com.vapsa.fulbazo.ui.theme.FulbazoTheme
import kotlinx.coroutines.delay

/** Parte de la pantalla que ocupa el mapa; el resto es la lista de partidos. */
private const val MAP_HEIGHT_FRACTION = 0.42f

/** Zoom inicial: se ven todas las losas de Arequipa. */
private const val CITY_ZOOM = 12.5f

/** Zoom al centrarte en tu ubicación: se ven tus calles y las losas de alrededor. */
private const val NEARBY_ZOOM = 13f

/** Si tu ubicación está a menos de esto de Arequipa, el mapa se centra en ti. */
private const val NEARBY_CITY_KM = 40.0

/** Tiempo sin que el mapa termine de cargar antes de avisar que no hay conexión. */
private const val MAP_LOAD_TIMEOUT_MILLIS = 12_000L

/** Etiqueta de la lista de partidos cercanos, para las pruebas automáticas. */
const val MAP_MATCHES_LIST_TAG = "map_matches_list"
const val OFFLINE_COURTS_TAG = "offline_courts"

/** Etiqueta del mapa de Google, para las pruebas automáticas. */
const val GOOGLE_MAP_TAG = "google_map"

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun MapRoute(
    /** Con `null` abre Crear partido sin losa; con un id, con esa losa elegida. */
    onCreateMatch: (courtId: String?) -> Unit,
    onOpenMatch: (matchId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    // El diálogo del sistema se muestra una sola vez por sí solo; luego, desde el aviso.
    var askedForLocation by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> viewModel.onLocationPermissionChanged(result.values.any { it }) }

    // Al abrir la pantalla y al volver de Ajustes se revisa el permiso otra vez.
    LifecycleResumeEffect(Unit) {
        val granted = context.hasLocationPermission()
        if (!granted && !askedForLocation) {
            askedForLocation = true
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        } else {
            viewModel.onLocationPermissionChanged(granted)
        }
        onPauseOrDispose { }
    }

    // Si ya lo negó con "No volver a preguntar", el diálogo no aparece: hay que ir a Ajustes.
    val opensSettings = uiState.locationPermission == LocationPermission.DENIED &&
        activity != null && !activity.canAskLocationAgain()

    MapScreen(
        uiState = uiState,
        actions = MapActions(
            onCreateMatch = { onCreateMatch(null) },
            onOpenMatch = onOpenMatch,
            onOpenCourt = viewModel::openCourt,
            onFilterCourt = viewModel::onFilterCourt,
            onRetryCourts = viewModel::retryCourts,
            onRequestLocation = {
                if (opensSettings) context.openAppSettings() else permissionLauncher.launch(LOCATION_PERMISSIONS)
            },
        ),
        locationRequestOpensSettings = opensSettings,
        modifier = modifier,
    )

    // La misma hoja se abre desde un marcador o desde el filtro.
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

private fun Context.hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

private fun Activity.canAskLocationAgain(): Boolean =
    LOCATION_PERMISSIONS.any { shouldShowRequestPermissionRationale(it) }

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

data class MapActions(
    val onCreateMatch: () -> Unit = {},
    val onOpenMatch: (matchId: String) -> Unit = {},
    val onOpenCourt: (courtId: String) -> Unit = {},
    val onFilterCourt: (courtId: String?) -> Unit = {},
    /** Vuelve a pedir el permiso de ubicación (o abre Ajustes si ya no se puede). */
    val onRequestLocation: () -> Unit = {},
    /** Vuelve a descargar las losas después de un error de conexión. */
    val onRetryCourts: () -> Unit = {},
)

@Composable
fun MapScreen(
    uiState: MapUiState,
    actions: MapActions,
    modifier: Modifier = Modifier,
    /** Si el botón del aviso de ubicación abre Ajustes en vez del diálogo. */
    locationRequestOpensSettings: Boolean = false,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val mapModifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(MAP_HEIGHT_FRACTION)
            if (uiState.mapsAvailable) {
                CourtsGoogleMap(
                    courts = uiState.courts,
                    matchCountByCourt = uiState.matchCountByCourt,
                    showMyLocation = uiState.locationPermission == LocationPermission.GRANTED,
                    userLocation = uiState.userLocation,
                    onCourtClick = actions.onOpenCourt,
                    modifier = mapModifier,
                )
            } else {
                // Sin clave de Maps no hay mapa, pero las losas se siguen pudiendo tocar.
                CourtsGridFallback(
                    courts = uiState.courts,
                    matchCountByCourt = uiState.matchCountByCourt,
                    onCourtClick = actions.onOpenCourt,
                    modifier = mapModifier,
                )
            }
            AnimatedVisibility(visible = uiState.locationPermission == LocationPermission.DENIED) {
                LocationDeniedBanner(
                    opensSettings = locationRequestOpensSettings,
                    onRequestLocation = actions.onRequestLocation,
                )
            }
            NearbyMatchesList(uiState = uiState, actions = actions, modifier = Modifier.weight(1f))
        }
        ExtendedFloatingActionButton(
            onClick = actions.onCreateMatch,
            icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
            text = { Text(stringResource(R.string.map_create_match)) },
            // Acento naranja de la marca para la acción principal.
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}

/** Explica para qué sirve la ubicación y permite activarla. */
@Composable
private fun LocationDeniedBanner(opensSettings: Boolean, onRequestLocation: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_place),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.map_location_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            TextButton(onClick = onRequestLocation) {
                Text(
                    stringResource(
                        if (opensSettings) R.string.map_location_settings else R.string.map_location_allow,
                    ),
                )
            }
        }
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
                    stringResource(
                        if (uiState.sortedByDistance) R.string.map_nearby_subtitle_distance else R.string.map_nearby_subtitle,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (uiState.showingSavedCourts) {
            item { OfflineCourtsBanner(onRetry = actions.onRetryCourts) }
        }
        if (uiState.courts.isNotEmpty()) {
            item { CourtFilter(uiState, actions) }
        }
        when {
            uiState.isLoading || uiState.courtsLoading -> item { LoadingState(Modifier.fillMaxWidth()) }
            uiState.courtsUnavailable -> item {
                ErrorState(stringResource(R.string.map_courts_offline_error), onRetry = actions.onRetryCourts)
            }
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

/** Aviso de que las losas son las guardadas en el teléfono porque no hay conexión. */
@Composable
private fun OfflineCourtsBanner(onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(OFFLINE_COURTS_TAG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.map_courts_offline),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
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

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

/**
 * Mapa de Google centrado en Arequipa, con un marcador por losa: verde si
 * tiene partidos próximos, celeste y más tenue si no. Tocar un marcador abre
 * el detalle de la losa.
 */
@Composable
private fun CourtsGoogleMap(
    courts: List<Court>,
    matchCountByCourt: Map<String, Int>,
    showMyLocation: Boolean,
    userLocation: GeoPoint?,
    onCourtClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(GeoPoint.AREQUIPA.toLatLng(), CITY_ZOOM)
    }
    var mapLoaded by remember { mutableStateOf(false) }
    var loadTimedOut by remember { mutableStateOf(false) }

    LaunchedEffect(mapLoaded) {
        if (!mapLoaded) {
            delay(MAP_LOAD_TIMEOUT_MILLIS)
            loadTimedOut = true
        }
    }
    // Te centra solo si estás en Arequipa; si no, el mapa sigue mostrando la ciudad.
    LaunchedEffect(userLocation, mapLoaded) {
        if (mapLoaded && userLocation != null && userLocation.distanceKmTo(GeoPoint.AREQUIPA) < NEARBY_CITY_KM) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation.toLatLng(), NEARBY_ZOOM))
        }
    }

    val description = stringResource(R.string.map_content_description)
    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .testTag(GOOGLE_MAP_TAG)
                .semantics { contentDescription = description },
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = showMyLocation),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = showMyLocation,
                mapToolbarEnabled = false,
                zoomControlsEnabled = false,
            ),
            onMapLoaded = { mapLoaded = true },
        ) {
            courts.forEach { court ->
                key(court.id) {
                    val matchCount = matchCountByCourt[court.id] ?: 0
                    val markerState = remember(court.latitude, court.longitude) {
                        MarkerState(LatLng(court.latitude, court.longitude))
                    }
                    Marker(
                        state = markerState,
                        title = court.name,
                        snippet = stringResource(R.string.map_marker_snippet, matchCount),
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (matchCount > 0) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_AZURE,
                        ),
                        alpha = if (matchCount > 0) 1f else 0.7f,
                        zIndex = if (matchCount > 0) 1f else 0f,
                        onClick = {
                            onCourtClick(court.id)
                            true // Sin la ventanita del marcador: se abre la hoja.
                        },
                    )
                }
            }
        }
        if (!mapLoaded) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            ) {
                Text(
                    text = stringResource(if (loadTimedOut) R.string.map_tiles_failed else R.string.map_loading),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/**
 * Vista simple de las losas sobre una cuadrícula, ubicadas según sus
 * coordenadas. Se usa cuando falta la clave de Google Maps, para que la app
 * nunca se quede sin mapa.
 */
@Composable
private fun CourtsGridFallback(
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
                text = stringResource(R.string.map_unavailable_label),
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

// La vista previa usa la cuadrícula: el mapa de Google no se dibuja en Android Studio.
@Preview(showBackground = true, heightDp = 700)
@Composable
private fun MapScreenPreview() {
    FulbazoTheme {
        MapScreen(
            uiState = MapUiState(
                isLoading = false,
                courts = previewCourts,
                matchCountByCourt = mapOf("p1" to 2),
                locationPermission = LocationPermission.DENIED,
                mapsAvailable = false,
            ),
            actions = MapActions(),
        )
    }
}
