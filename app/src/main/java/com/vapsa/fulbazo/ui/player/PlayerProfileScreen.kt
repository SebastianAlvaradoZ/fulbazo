package com.vapsa.fulbazo.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.CommunityRatings
import com.vapsa.fulbazo.domain.model.PlayerRelation
import com.vapsa.fulbazo.domain.model.SkillScale
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorState
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.components.UserAvatar
import com.vapsa.fulbazo.ui.match.formatMatchDate
import com.vapsa.fulbazo.ui.stats.labelRes

@Composable
fun PlayerProfileRoute(
    playerId: String,
    onNavigateBack: () -> Unit,
    onOpenOwnProfile: () -> Unit,
    viewModel: PlayerProfileViewModel = viewModel(
        key = "player-$playerId",
        factory = PlayerProfileViewModel.factory(playerId),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Mi propio perfil se ve en la pestaña Perfil.
    LaunchedEffect(uiState.isMe) { if (uiState.isMe) onOpenOwnProfile() }
    LaunchedEffect(uiState.message) {
        val text = when (uiState.message ?: return@LaunchedEffect) {
            PlayerProfileMessage.REQUEST_SENT -> context.getString(R.string.player_msg_request_sent)
            PlayerProfileMessage.REQUEST_ACCEPTED -> context.getString(R.string.player_msg_request_accepted)
            PlayerProfileMessage.INVITED -> context.getString(R.string.player_msg_invited)
        }
        snackbarHostState.showSnackbar(text)
        viewModel.onMessageShown()
    }
    LaunchedEffect(uiState.actionFailed) {
        if (uiState.actionFailed) {
            snackbarHostState.showSnackbar(context.getString(R.string.generic_error))
            viewModel.onErrorShown()
        }
    }

    PlayerProfileScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = PlayerProfileActions(
            onBack = onNavigateBack,
            onRetry = viewModel::retry,
            onAddFriend = viewModel::sendFriendRequest,
            onAcceptRequest = viewModel::acceptFriendRequest,
            onInvite = viewModel::inviteToMatch,
        ),
    )
}

data class PlayerProfileActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onAddFriend: () -> Unit = {},
    val onAcceptRequest: () -> Unit = {},
    val onInvite: (matchId: String) -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    uiState: PlayerProfileUiState,
    actions: PlayerProfileActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.player_title)) },
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
        val player = uiState.player
        when {
            uiState.isLoading || uiState.isMe -> LoadingState(contentModifier)
            uiState.loadFailed -> ErrorState(
                stringResource(R.string.player_load_error),
                contentModifier,
                onRetry = actions.onRetry,
            )
            uiState.notFound || player == null -> EmptyState(stringResource(R.string.player_not_found), contentModifier)
            else -> Column(
                modifier = contentModifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                UserAvatar(photoUri = player.photoUri, contentDescription = null)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(player.nickname, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${player.fullName} · ${player.district}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PositionChips(uiState)
                Text(
                    stringResource(R.string.player_matches_together, uiState.matchesTogether),
                    style = MaterialTheme.typography.bodyLarge,
                )
                ActionButtons(uiState, actions)
                SelfCard(uiState)
                CommunityCard(uiState)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PositionChips(uiState: PlayerProfileUiState) {
    val self = uiState.self ?: return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        AssistChip(onClick = {}, label = { Text(stringResource(self.mainPosition.labelRes)) })
        self.secondaryPosition?.let { AssistChip(onClick = {}, label = { Text(stringResource(it.labelRes)) }) }
        AssistChip(
            onClick = {},
            label = {
                Text(stringResource(R.string.player_foot, stringResource(self.dominantFoot.labelRes).lowercase()))
            },
        )
    }
}

@Composable
private fun ActionButtons(uiState: PlayerProfileUiState, actions: PlayerProfileActions) {
    var showInviteDialog by rememberSaveable { mutableStateOf(false) }
    val busy = uiState.isBusy

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (uiState.relation) {
            PlayerRelation.NONE -> Button(onClick = actions.onAddFriend, enabled = !busy) {
                Icon(painterResource(R.drawable.ic_person_add), null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.player_add_friend))
            }
            PlayerRelation.REQUEST_SENT -> OutlinedButton(onClick = {}, enabled = false) {
                Text(stringResource(R.string.player_request_sent))
            }
            PlayerRelation.REQUEST_RECEIVED -> Button(onClick = actions.onAcceptRequest, enabled = !busy) {
                Text(stringResource(R.string.player_accept_request))
            }
            PlayerRelation.FRIEND -> AssistChip(
                onClick = {},
                label = { Text(stringResource(R.string.player_friends)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_group), null, Modifier.size(18.dp)) },
            )
        }
        OutlinedButton(onClick = { showInviteDialog = true }, enabled = !busy) {
            Text(stringResource(R.string.player_invite))
        }
    }

    if (showInviteDialog) {
        InviteDialog(
            uiState = uiState,
            onDismiss = { showInviteDialog = false },
            onInvite = { matchId ->
                showInviteDialog = false
                actions.onInvite(matchId)
            },
        )
    }
}

@Composable
private fun InviteDialog(
    uiState: PlayerProfileUiState,
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(uiState.invitableMatches.firstOrNull()?.matchId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_invite_title, uiState.player?.nickname.orEmpty())) },
        text = {
            if (uiState.invitableMatches.isEmpty()) {
                Text(stringResource(R.string.player_invite_empty))
            } else {
                Column {
                    Text(stringResource(R.string.player_invite_text))
                    uiState.invitableMatches.forEach { match ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected == match.matchId,
                                    role = Role.RadioButton,
                                    onClick = { selected = match.matchId },
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected == match.matchId, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(match.courtName, style = MaterialTheme.typography.bodyLarge)
                                Text(match.dateTime.formatMatchDate(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let(onInvite) },
                enabled = selected != null && uiState.invitableMatches.isNotEmpty(),
            ) { Text(stringResource(R.string.player_invite_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun SelfCard(uiState: PlayerProfileUiState) {
    StatsCard(stringResource(R.string.player_self_title)) {
        val self = uiState.self
        if (self == null) {
            HelperText(stringResource(R.string.player_no_self))
            return@StatsCard
        }
        OverallRow(stringResource(R.string.stats_overall_field), self.fieldOverall)
        self.goalkeeperOverall?.let { OverallRow(stringResource(R.string.stats_overall_goalkeeper), it) }
    }
}

@Composable
private fun CommunityCard(uiState: PlayerProfileUiState) {
    StatsCard(stringResource(R.string.player_community_title)) {
        val field = uiState.community.asFieldPlayer
        val goalkeeper = uiState.community.asGoalkeeper
        if (field == null && goalkeeper == null) {
            HelperText(stringResource(R.string.player_no_community))
            return@StatsCard
        }
        field?.let { ratings ->
            RatingsSection(stringResource(R.string.stats_overall_field), ratings) { stringResource(it.labelRes) }
        }
        if (field != null && goalkeeper != null) HorizontalDivider()
        goalkeeper?.let { ratings ->
            RatingsSection(stringResource(R.string.stats_overall_goalkeeper), ratings) { stringResource(it.labelRes) }
        }
    }
}

/** Promedio general y una barra por habilidad, con la cantidad de votos. */
@Composable
private fun <S : Enum<S>> RatingsSection(
    title: String,
    ratings: CommunityRatings<S>,
    label: @Composable (S) -> String,
) {
    OverallRow(title, ratings.overall, stringResource(R.string.profile_votes, ratings.votes))
    ratings.averages.forEach { (skill, value) ->
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label(skill), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.skill_value_decimal, value), style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(
                progress = { (value / SkillScale.MAX).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun OverallRow(label: String, value: Double, detail: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            stringResource(R.string.stats_overall_value, value),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun HelperText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
