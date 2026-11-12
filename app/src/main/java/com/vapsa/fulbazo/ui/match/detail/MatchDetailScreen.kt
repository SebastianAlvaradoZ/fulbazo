package com.vapsa.fulbazo.ui.match.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.PlayerSummary
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorState
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.components.UserAvatar
import com.vapsa.fulbazo.ui.match.MatchCard

@Composable
fun MatchDetailRoute(
    matchId: String,
    onNavigateBack: () -> Unit,
    onOpenPlayer: (playerId: String) -> Unit,
    onRatePlayers: () -> Unit,
    viewModel: MatchDetailViewModel = viewModel(
        key = "match-$matchId",
        factory = MatchDetailViewModel.factory(matchId),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        val text = when (error) {
            MatchDetailError.MATCH_FULL -> context.getString(R.string.match_detail_full_note)
            MatchDetailError.FAILED -> context.getString(R.string.generic_error)
        }
        snackbarHostState.showSnackbar(text)
        viewModel.onErrorShown()
    }
    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        val text = when (message) {
            MatchDetailMessage.REQUEST_SENT -> context.getString(R.string.match_detail_request_sent)
            MatchDetailMessage.REQUEST_CANCELLED -> context.getString(R.string.match_detail_request_cancelled)
            MatchDetailMessage.LEFT_MATCH -> context.getString(R.string.match_detail_left)
        }
        snackbarHostState.showSnackbar(text)
        viewModel.onMessageShown()
    }

    MatchDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = MatchDetailActions(
            onBack = onNavigateBack,
            onRequestToJoin = viewModel::requestToJoin,
            onCancelRequest = viewModel::cancelRequest,
            onLeave = viewModel::leaveMatch,
            onRespond = viewModel::respondToRequest,
            onOpenPlayer = onOpenPlayer,
            onRatePlayers = onRatePlayers,
        ),
    )
}

data class MatchDetailActions(
    val onBack: () -> Unit = {},
    val onRequestToJoin: () -> Unit = {},
    val onCancelRequest: () -> Unit = {},
    val onLeave: () -> Unit = {},
    val onRespond: (requestId: String, accept: Boolean) -> Unit = { _, _ -> },
    val onOpenPlayer: (playerId: String) -> Unit = {},
    val onRatePlayers: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    uiState: MatchDetailUiState,
    actions: MatchDetailActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.match_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        val match = uiState.match
        when {
            uiState.isLoading -> LoadingState(contentModifier)
            uiState.loadFailed -> ErrorState(stringResource(R.string.match_detail_load_error), contentModifier)
            uiState.notFound || match == null -> EmptyState(
                stringResource(R.string.match_detail_not_found),
                contentModifier,
                iconRes = R.drawable.ic_matches,
            )
            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { MatchCard(match) }
                if (uiState.isFinished) {
                    item { HelperText(stringResource(R.string.match_detail_finished)) }
                    // Quien jugó el partido puede evaluar a sus compañeros.
                    if (uiState.role == ViewerRole.HOST || uiState.role == ViewerRole.PLAYER) {
                        item {
                            Button(onClick = actions.onRatePlayers, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.rate_open))
                            }
                        }
                    }
                } else {
                    item { ViewerActions(uiState, actions) }
                }

                if (uiState.role == ViewerRole.HOST && !uiState.isFinished) {
                    item {
                        SectionTitle(stringResource(R.string.match_detail_requests, uiState.pendingRequests.size))
                    }
                    if (uiState.isFull && uiState.pendingRequests.isNotEmpty()) {
                        item { HelperText(stringResource(R.string.match_detail_full_note)) }
                    }
                    if (uiState.pendingRequests.isEmpty()) {
                        item { HelperText(stringResource(R.string.match_detail_no_requests)) }
                    }
                    items(uiState.pendingRequests, key = { it.requestId }) { request ->
                        RequestCard(
                            request = request,
                            busy = request.requestId in uiState.busyRequestIds,
                            canAccept = !uiState.isFull,
                            onRespond = actions.onRespond,
                            onOpenPlayer = actions.onOpenPlayer,
                        )
                    }
                    item {
                        // Lugar reservado para la IA que arma equipos balanceados.
                        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.match_detail_teams_soon))
                        }
                    }
                }

                item { HorizontalDivider() }
                item {
                    SectionTitle(stringResource(R.string.match_detail_players, uiState.playersCount, match.slots))
                }
                items(uiState.players, key = { it.player.id }) { item ->
                    PlayerRow(item.player, onClick = { actions.onOpenPlayer(item.player.id) }) {
                        if (item.isHost) AssistChip(onClick = {}, label = { Text(stringResource(R.string.match_detail_host_tag)) })
                    }
                }
            }
        }
    }
}

/** Botón principal según la relación del usuario con el partido. */
@Composable
private fun ViewerActions(uiState: MatchDetailUiState, actions: MatchDetailActions) {
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    val busy = uiState.isBusy

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (uiState.role) {
            ViewerRole.HOST -> Unit
            ViewerRole.PLAYER -> {
                HelperText(stringResource(R.string.match_detail_you_joined))
                OutlinedButton(onClick = { confirmLeave = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.match_detail_leave))
                }
            }
            ViewerRole.REQUEST_PENDING -> {
                HelperText(stringResource(R.string.match_detail_request_pending))
                OutlinedButton(onClick = actions.onCancelRequest, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.match_detail_cancel_request))
                }
            }
            ViewerRole.REQUEST_REJECTED -> HelperText(stringResource(R.string.match_detail_request_rejected))
            ViewerRole.NONE -> Button(
                onClick = actions.onRequestToJoin,
                enabled = !busy && !uiState.isFull,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                when {
                    busy -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    uiState.isFull -> Text(stringResource(R.string.match_detail_full))
                    else -> Text(stringResource(R.string.match_detail_request_join))
                }
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.match_detail_leave_title)) },
            text = { Text(stringResource(R.string.match_detail_leave_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    actions.onLeave()
                }) { Text(stringResource(R.string.match_detail_leave)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun RequestCard(
    request: PendingRequestItem,
    busy: Boolean,
    canAccept: Boolean,
    onRespond: (String, Boolean) -> Unit,
    onOpenPlayer: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlayerRow(
                request.player,
                subtitle = stringResource(R.string.match_detail_wants_to_join),
                onClick = { onOpenPlayer(request.player.id) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = { onRespond(request.requestId, false) }, enabled = !busy) {
                    Text(stringResource(R.string.action_decline))
                }
                Button(onClick = { onRespond(request.requestId, true) }, enabled = !busy && canAccept) {
                    Text(stringResource(R.string.action_accept))
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(
    player: PlayerSummary,
    subtitle: String = "${player.fullName} · ${player.district}",
    onClick: () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(photoUri = player.photoUri, contentDescription = null, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(player.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun HelperText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
