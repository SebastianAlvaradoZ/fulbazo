package com.vapsa.fulbazo.ui.invitations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.InvitationStatus
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorState
import com.vapsa.fulbazo.ui.components.FulbazoCard
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.components.PhaseCrossfade
import com.vapsa.fulbazo.ui.components.ScreenPhase
import com.vapsa.fulbazo.ui.match.MatchCard

@Composable
fun InvitationsRoute(
    onNavigateBack: () -> Unit,
    viewModel: InvitationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.actionError) {
        val error = uiState.actionError ?: return@LaunchedEffect
        val message = when (error) {
            InvitationActionError.MATCH_FULL -> context.getString(R.string.invitations_match_full)
            InvitationActionError.FAILED -> context.getString(R.string.generic_error)
        }
        snackbarHostState.showSnackbar(message)
        viewModel.onErrorShown()
    }

    InvitationsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onNavigateBack,
        onRespond = viewModel::respond,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitationsScreen(
    uiState: InvitationsUiState,
    onBack: () -> Unit,
    onRespond: (invitationId: String, accept: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.invitations_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        val phase = when {
            uiState.isLoading -> ScreenPhase.LOADING
            uiState.loadFailed -> ScreenPhase.ERROR
            uiState.isEmpty -> ScreenPhase.EMPTY
            else -> ScreenPhase.CONTENT
        }
        PhaseCrossfade(phase) { current ->
            when (current) {
                ScreenPhase.LOADING -> LoadingState(contentModifier)
                ScreenPhase.ERROR -> ErrorState(stringResource(R.string.invitations_load_error), contentModifier)
                ScreenPhase.EMPTY -> EmptyState(
                    stringResource(R.string.invitations_empty),
                    contentModifier,
                    iconRes = R.drawable.ic_notifications,
                )
                ScreenPhase.CONTENT -> LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.pending.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.invitations_pending)) }
                        items(uiState.pending, key = { it.invitationId }) { item ->
                            InvitationCard(item, item.invitationId in uiState.busyIds, onRespond, Modifier.animateItem())
                        }
                    }
                    if (uiState.answered.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.invitations_answered)) }
                        items(uiState.answered, key = { it.invitationId }) { item ->
                            InvitationCard(item, busy = false, onRespond = onRespond, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(
    item: InvitationItem,
    busy: Boolean,
    onRespond: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Un solo Column: en un LazyColumn, varios elementos sueltos se superpondrían.
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InvitationCardContent(item, busy, onRespond)
    }
}

@Composable
private fun InvitationCardContent(
    item: InvitationItem,
    busy: Boolean,
    onRespond: (String, Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.invitations_from, item.fromNickname),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
    )
    val match = item.match
    if (match == null) {
        FulbazoCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.invitations_match_gone), Modifier.padding(16.dp))
        }
        return
    }
    MatchCard(match) {
        when (item.status) {
            InvitationStatus.PENDING -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = { onRespond(item.invitationId, false) }, enabled = !busy) {
                    Text(stringResource(R.string.action_decline))
                }
                Button(onClick = { onRespond(item.invitationId, true) }, enabled = !busy) {
                    Text(stringResource(R.string.action_accept))
                }
            }
            InvitationStatus.ACCEPTED -> StatusText(stringResource(R.string.invitations_accepted))
            InvitationStatus.DECLINED -> StatusText(stringResource(R.string.invitations_declined))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun StatusText(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}
