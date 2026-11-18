package com.vapsa.fulbazo.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.CommunityRatings
import com.vapsa.fulbazo.domain.model.PlayerRelation
import com.vapsa.fulbazo.domain.model.SkillScale
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorState
import com.vapsa.fulbazo.ui.components.FulbazoCard
import com.vapsa.fulbazo.ui.components.IconTag
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.components.PhaseCrossfade
import com.vapsa.fulbazo.ui.components.ProfileHeader
import com.vapsa.fulbazo.ui.components.ScoreRing
import com.vapsa.fulbazo.ui.components.ScreenPhase
import com.vapsa.fulbazo.ui.match.formatMatchDate
import com.vapsa.fulbazo.ui.reviews.PlayerReviewsSection
import com.vapsa.fulbazo.ui.stats.labelRes
import com.vapsa.fulbazo.ui.theme.OnBrand
import com.vapsa.fulbazo.ui.theme.Spacing
import com.vapsa.fulbazo.ui.theme.brandGradient

@Composable
fun PlayerProfileRoute(
    playerId: String,
    onNavigateBack: () -> Unit,
    onOpenOwnProfile: () -> Unit,
    viewModel: PlayerProfileViewModel = hiltViewModel<PlayerProfileViewModel, PlayerProfileViewModel.Factory>(
        key = "player-$playerId",
    ) { factory -> factory.create(playerId) },
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
        reviewsSection = { PlayerReviewsSection(playerId) },
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
    /** Comentarios y resumen de IA del jugador. */
    reviewsSection: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Verde, para que siga sin cortes en la portada del perfil.
            TopAppBar(
                title = { Text(stringResource(R.string.player_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = OnBrand,
                    navigationIconContentColor = OnBrand,
                ),
                modifier = Modifier.background(brandGradient()),
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        val player = uiState.player
        val phase = when {
            uiState.isLoading || uiState.isMe -> ScreenPhase.LOADING
            uiState.loadFailed -> ScreenPhase.ERROR
            uiState.notFound || player == null -> ScreenPhase.EMPTY
            else -> ScreenPhase.CONTENT
        }
        PhaseCrossfade(phase) { current ->
            when (current) {
                ScreenPhase.LOADING -> LoadingState(contentModifier)
                ScreenPhase.ERROR -> ErrorState(
                    stringResource(R.string.player_load_error),
                    contentModifier,
                    onRetry = actions.onRetry,
                )
                ScreenPhase.EMPTY -> EmptyState(stringResource(R.string.player_not_found), contentModifier)
                ScreenPhase.CONTENT -> if (player != null) Column(
                    modifier = contentModifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProfileHeader(
                        nickname = player.nickname,
                        subtitle = "${player.fullName} · ${player.district}",
                        photoUri = player.photoUri,
                        name = player.fullName,
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                    PositionChips(uiState)
                    Text(
                        stringResource(R.string.player_matches_together, uiState.matchesTogether),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    ActionButtons(uiState, actions)
                    SelfCard(uiState)
                    CommunityCard(uiState)
                    reviewsSection()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PositionChips(uiState: PlayerProfileUiState) {
    val self = uiState.self ?: return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        IconTag(R.drawable.ic_sports_soccer, stringResource(self.mainPosition.labelRes))
        self.secondaryPosition?.let { IconTag(R.drawable.ic_sports_soccer, stringResource(it.labelRes)) }
        IconTag(
            R.drawable.ic_directions_run,
            stringResource(R.string.player_foot, stringResource(self.dominantFoot.labelRes).lowercase()),
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
    // Dos habilidades por fila: la tarjeta queda más compacta y se compara mejor.
    ratings.averages.entries.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            pair.forEach { (skill, value) ->
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label(skill), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Text(
                            stringResource(R.string.skill_value_decimal, value),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    AnimatedSkillBar(fraction = (value / SkillScale.MAX).toFloat())
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable () -> Unit) {
    FulbazoCard(Modifier.fillMaxWidth()) {
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
        ScoreRing(value)
    }
}

@Composable
private fun HelperText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Barra de una habilidad que se llena desde cero al mostrarse. */
@Composable
private fun AnimatedSkillBar(fraction: Float) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) fraction else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "skillBar",
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
}
