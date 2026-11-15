package com.vapsa.fulbazo.ui.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.PlayerRelation
import com.vapsa.fulbazo.domain.model.PlayerSummary
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.components.UserAvatar

@Composable
fun FriendsRoute(
    onOpenGroup: (groupId: String?) -> Unit,
    onOpenPlayer: (playerId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = viewModel(factory = FriendsViewModel.Factory),
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
        FriendsScreen(
            uiState = uiState,
            actions = FriendsActions(
                onQueryChange = viewModel::onQueryChange,
                onSendRequest = viewModel::sendRequest,
                onRespond = viewModel::respondToRequest,
                onRemoveFriend = viewModel::removeFriend,
                onOpenGroup = onOpenGroup,
                onOpenPlayer = onOpenPlayer,
            ),
        )
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

data class FriendsActions(
    val onQueryChange: (String) -> Unit = {},
    val onSendRequest: (String) -> Unit = {},
    val onRespond: (requestId: String, accept: Boolean) -> Unit = { _, _ -> },
    val onRemoveFriend: (String) -> Unit = {},
    val onOpenGroup: (groupId: String?) -> Unit = {},
    val onOpenPlayer: (playerId: String) -> Unit = {},
)

@Composable
fun FriendsScreen(
    uiState: FriendsUiState,
    actions: FriendsActions,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize()) {
        SearchField(
            query = uiState.query,
            onQueryChange = actions.onQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        when {
            uiState.isLoading -> LoadingState(Modifier.fillMaxWidth())
            uiState.isSearchActive -> SearchResults(uiState, actions)
            else -> {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.friends_tab_friends, uiState.friends.size)) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.friends_tab_requests, uiState.incomingRequests.size)) },
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(stringResource(R.string.friends_tab_groups, uiState.groups.size)) },
                    )
                }
                when (selectedTab) {
                    0 -> FriendsList(uiState, actions)
                    1 -> RequestsList(uiState, actions)
                    else -> GroupsList(uiState, actions)
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.friends_search_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.friends_clear_search))
                }
            }
        },
    )
}

@Composable
private fun SearchResults(uiState: FriendsUiState, actions: FriendsActions) {
    when {
        uiState.isSearching -> LoadingState(Modifier.fillMaxWidth())
        uiState.searchResults.isEmpty() -> EmptyState(stringResource(R.string.friends_search_empty))
        else -> PlayerLazyList(uiState.searchResults.map { it.player }, actions.onOpenPlayer) { player ->
            val relation = uiState.searchResults.first { it.player.id == player.id }.relation
            val busy = player.id in uiState.busyIds
            when (relation) {
                PlayerRelation.NONE -> Button(onClick = { actions.onSendRequest(player.id) }, enabled = !busy) {
                    Icon(painterResource(R.drawable.ic_person_add), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.friends_add))
                }
                PlayerRelation.REQUEST_SENT -> OutlinedButton(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.friends_request_sent))
                }
                // Enviarle una solicitud a quien ya me envió una la acepta.
                PlayerRelation.REQUEST_RECEIVED -> Button(onClick = { actions.onSendRequest(player.id) }, enabled = !busy) {
                    Text(stringResource(R.string.action_accept))
                }
                PlayerRelation.FRIEND -> Text(
                    stringResource(R.string.friends_already_friends),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun FriendsList(uiState: FriendsUiState, actions: FriendsActions) {
    if (uiState.friends.isEmpty()) {
        EmptyState(stringResource(R.string.friends_empty), iconRes = R.drawable.ic_group)
        return
    }
    var toRemove by remember { mutableStateOf<PlayerSummary?>(null) }
    PlayerLazyList(uiState.friends, actions.onOpenPlayer) { friend ->
        IconButton(onClick = { toRemove = friend }, enabled = friend.id !in uiState.busyIds) {
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.friends_remove))
        }
    }
    toRemove?.let { friend ->
        AlertDialog(
            onDismissRequest = { toRemove = null },
            title = { Text(stringResource(R.string.friends_remove_title, friend.nickname)) },
            text = { Text(stringResource(R.string.friends_remove_text)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.onRemoveFriend(friend.id)
                    toRemove = null
                }) { Text(stringResource(R.string.friends_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { toRemove = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun RequestsList(uiState: FriendsUiState, actions: FriendsActions) {
    if (uiState.incomingRequests.isEmpty()) {
        EmptyState(stringResource(R.string.friends_requests_empty))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(uiState.incomingRequests, key = { it.requestId }) { request ->
            val busy = request.requestId in uiState.busyIds
            Card(
                Modifier
                    .fillMaxWidth()
                    .animateItem(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerRow(
                        request.player,
                        subtitle = stringResource(R.string.friends_wants_to_be_friend),
                        modifier = Modifier.clickable { actions.onOpenPlayer(request.player.id) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        OutlinedButton(onClick = { actions.onRespond(request.requestId, false) }, enabled = !busy) {
                            Text(stringResource(R.string.action_decline))
                        }
                        Button(onClick = { actions.onRespond(request.requestId, true) }, enabled = !busy) {
                            Text(stringResource(R.string.action_accept))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupsList(uiState: FriendsUiState, actions: FriendsActions) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(onClick = { actions.onOpenGroup(null) }, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.friends_new_group))
            }
        }
        if (uiState.groups.isEmpty()) {
            item { EmptyState(stringResource(R.string.friends_groups_empty), iconRes = R.drawable.ic_group) }
        }
        items(uiState.groups, key = { it.id }) { group ->
            Card(
                Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .clickable { actions.onOpenGroup(group.id) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.friends_group_members,
                            group.memberNicknames.size,
                            group.memberNicknames.joinToString(", "),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerLazyList(
    players: List<PlayerSummary>,
    onOpenPlayer: (String) -> Unit,
    action: @Composable (PlayerSummary) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(players, key = { it.id }) { player ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerRow(
                    player,
                    subtitle = "${player.fullName} · ${player.district}",
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenPlayer(player.id) },
                )
                action(player)
            }
        }
    }
}

@Composable
private fun PlayerRow(player: PlayerSummary, subtitle: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(photoUri = player.photoUri, contentDescription = null, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(player.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
