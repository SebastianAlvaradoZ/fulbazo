package com.vapsa.fulbazo.ui.court

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorState
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.components.PhaseCrossfade
import com.vapsa.fulbazo.ui.components.ScreenPhase
import com.vapsa.fulbazo.ui.match.MatchCard

/**
 * Hoja inferior con el detalle de una losa. Se abre al tocar una losa en el
 * mapa (hoy la vista previa; luego el marcador del mapa real) o desde el filtro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourtDetailSheet(
    courtId: String,
    onDismiss: () -> Unit,
    onOpenMatch: (matchId: String) -> Unit,
    onCreateMatchHere: (courtId: String) -> Unit,
    viewModel: CourtDetailViewModel = hiltViewModel<CourtDetailViewModel, CourtDetailViewModel.Factory>(
        key = "court-$courtId",
    ) { factory -> factory.create(courtId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        CourtDetailContent(
            uiState = uiState,
            onRetry = viewModel::retry,
            onOpenMatch = onOpenMatch,
            onCreateMatchHere = { onCreateMatchHere(courtId) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourtDetailContent(
    uiState: CourtDetailUiState,
    onRetry: () -> Unit,
    onOpenMatch: (String) -> Unit,
    onCreateMatchHere: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val court = uiState.court
    val phase = when {
        uiState.isLoading -> ScreenPhase.LOADING
        uiState.loadFailed -> ScreenPhase.ERROR
        uiState.notFound || court == null -> ScreenPhase.EMPTY
        else -> ScreenPhase.CONTENT
    }
    PhaseCrossfade(phase, modifier) { current ->
        when (current) {
            ScreenPhase.LOADING -> LoadingState(Modifier.fillMaxWidth())
            ScreenPhase.ERROR -> ErrorState(stringResource(R.string.court_load_error), onRetry = onRetry)
            ScreenPhase.EMPTY -> EmptyState(stringResource(R.string.court_not_found))
            ScreenPhase.CONTENT -> if (court != null) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_place),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(court.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        listOf(court.address, court.district).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    stringResource(
                                        if (court.hasLighting) R.string.court_lighting_yes else R.string.court_lighting_no,
                                    ),
                                )
                            },
                        )
                    }

                    Text(stringResource(R.string.court_upcoming), style = MaterialTheme.typography.titleMedium)
                    if (uiState.upcomingMatches.isEmpty()) {
                        Text(
                            stringResource(R.string.court_no_matches),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    uiState.upcomingMatches.forEach { match ->
                        MatchCard(match, onClick = { onOpenMatch(match.matchId) })
                    }

                    Button(
                        onClick = onCreateMatchHere,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.court_create_here))
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
