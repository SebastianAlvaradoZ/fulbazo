package com.vapsa.fulbazo.ui.match.mine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.JoinRequestStatus
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorState
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.match.MatchCard

@Composable
fun MyMatchesRoute(
    onOpenMatch: (matchId: String) -> Unit,
    onCreateMatch: () -> Unit,
    onExploreMap: () -> Unit,
    onRateMatch: (matchId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyMatchesViewModel = viewModel(factory = MyMatchesViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.actionFailed) {
        if (uiState.actionFailed) {
            snackbarHostState.showSnackbar(context.getString(R.string.generic_error))
            viewModel.onErrorShown()
        }
    }

    Box(modifier.fillMaxSize()) {
        MyMatchesScreen(
            uiState = uiState,
            actions = MyMatchesActions(
                onOpenMatch = onOpenMatch,
                onCreateMatch = onCreateMatch,
                onExploreMap = onExploreMap,
                onRateMatch = onRateMatch,
                onCancelRequest = viewModel::cancelRequest,
                onRetry = viewModel::retry,
            ),
        )
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

data class MyMatchesActions(
    val onOpenMatch: (String) -> Unit = {},
    val onCreateMatch: () -> Unit = {},
    val onExploreMap: () -> Unit = {},
    val onRateMatch: (String) -> Unit = {},
    val onCancelRequest: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
)

@Composable
fun MyMatchesScreen(
    uiState: MyMatchesUiState,
    actions: MyMatchesActions,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.my_matches_tab_upcoming, uiState.upcoming.size)) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.my_matches_tab_requests, uiState.pendingRequestsCount)) },
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(stringResource(R.string.my_matches_tab_history, uiState.history.size)) },
            )
        }
        when {
            uiState.isLoading -> LoadingState(Modifier.fillMaxWidth())
            uiState.loadFailed -> ErrorState(
                stringResource(R.string.my_matches_load_error),
                onRetry = actions.onRetry,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedTab) {
                    0 -> upcomingTab(uiState, actions)
                    1 -> requestsTab(uiState, actions)
                    else -> historyTab(uiState, actions)
                }
            }
        }
    }
}

private fun LazyListScope.upcomingTab(uiState: MyMatchesUiState, actions: MyMatchesActions) {
    if (uiState.upcoming.isEmpty()) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                EmptyState(stringResource(R.string.my_matches_upcoming_empty), iconRes = R.drawable.ic_matches)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = actions.onExploreMap) {
                        Text(stringResource(R.string.my_matches_explore_map))
                    }
                    Button(onClick = actions.onCreateMatch) {
                        Text(stringResource(R.string.map_create_match))
                    }
                }
            }
        }
        return
    }
    // La tarjeta ya indica "Eres el anfitrión" o "Ya estás inscrito".
    items(uiState.upcoming, key = { it.matchId }) { match ->
        MatchCard(match, onClick = { actions.onOpenMatch(match.matchId) }, modifier = Modifier.animateItem())
    }
}

private fun LazyListScope.requestsTab(uiState: MyMatchesUiState, actions: MyMatchesActions) {
    if (uiState.requests.isEmpty()) {
        item { EmptyState(stringResource(R.string.my_matches_requests_empty)) }
        return
    }
    items(uiState.requests, key = { it.requestId }) { request ->
        val match = request.match ?: return@items
        MatchCard(match, onClick = { actions.onOpenMatch(match.matchId) }, modifier = Modifier.animateItem()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(onClick = {}, label = { Text(stringResource(request.status.labelRes)) })
                if (request.status == JoinRequestStatus.PENDING) {
                    OutlinedButton(
                        onClick = { actions.onCancelRequest(request.requestId) },
                        enabled = request.requestId !in uiState.busyRequestIds,
                    ) {
                        Text(stringResource(R.string.match_detail_cancel_request))
                    }
                }
            }
        }
    }
}

private fun LazyListScope.historyTab(uiState: MyMatchesUiState, actions: MyMatchesActions) {
    if (uiState.history.isEmpty()) {
        item { EmptyState(stringResource(R.string.my_matches_history_empty)) }
        return
    }
    items(uiState.history, key = { it.matchId }) { match ->
        MatchCard(match, onClick = { actions.onOpenMatch(match.matchId) }) {
            OutlinedButton(
                onClick = { actions.onRateMatch(match.matchId) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.rate_open))
            }
        }
    }
}

private val JoinRequestStatus.labelRes: Int
    get() = when (this) {
        JoinRequestStatus.PENDING -> R.string.my_matches_status_pending
        JoinRequestStatus.ACCEPTED -> R.string.my_matches_status_accepted
        JoinRequestStatus.REJECTED -> R.string.my_matches_status_rejected
    }
