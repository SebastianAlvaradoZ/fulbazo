package com.vapsa.fulbazo.ui.match.rate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.GoalkeeperSkill
import com.vapsa.fulbazo.domain.model.MatchRole
import com.vapsa.fulbazo.domain.model.PlayerComment
import com.vapsa.fulbazo.ui.components.EmptyState
import com.vapsa.fulbazo.ui.components.ErrorState
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.components.SkillSlider
import com.vapsa.fulbazo.ui.components.UserAvatar
import com.vapsa.fulbazo.ui.match.MatchCard
import com.vapsa.fulbazo.ui.stats.labelRes

@Composable
fun RatePlayersRoute(
    matchId: String,
    onNavigateBack: () -> Unit,
    viewModel: RatePlayersViewModel = hiltViewModel<RatePlayersViewModel, RatePlayersViewModel.Factory>(
        key = "rate-$matchId",
    ) { factory -> factory.create(matchId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.savedNickname, uiState.saveFailed) {
        val text = when {
            uiState.savedNickname != null -> context.getString(R.string.rate_saved, uiState.savedNickname)
            uiState.saveFailed -> context.getString(R.string.rate_save_error)
            else -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(text)
        viewModel.onMessageShown()
    }

    RatePlayersScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = RatePlayersActions(
            onBack = onNavigateBack,
            onOpenRating = viewModel::openRating,
            onDismissRating = viewModel::dismissRating,
            onRoleChange = viewModel::onRoleChange,
            onFieldSkillChange = viewModel::onFieldSkillChange,
            onGoalkeeperSkillChange = viewModel::onGoalkeeperSkillChange,
            onCommentChange = viewModel::onCommentChange,
            onSave = viewModel::saveRating,
        ),
    )
}

data class RatePlayersActions(
    val onBack: () -> Unit = {},
    val onOpenRating: (playerId: String) -> Unit = {},
    val onDismissRating: () -> Unit = {},
    val onRoleChange: (MatchRole) -> Unit = {},
    val onFieldSkillChange: (FieldSkill, Int) -> Unit = { _, _ -> },
    val onGoalkeeperSkillChange: (GoalkeeperSkill, Int) -> Unit = { _, _ -> },
    val onCommentChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatePlayersScreen(
    uiState: RatePlayersUiState,
    actions: RatePlayersActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rate_title)) },
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
            uiState.loadFailed -> ErrorState(stringResource(R.string.rate_load_error), contentModifier)
            uiState.notFound -> EmptyState(stringResource(R.string.rate_not_found), contentModifier)
            uiState.notAllowed || match == null -> EmptyState(stringResource(R.string.rate_not_allowed), contentModifier)
            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { MatchCard(match) }
                item {
                    Text(
                        stringResource(R.string.rate_progress, uiState.ratedCount, uiState.teammates.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (uiState.teammates.isEmpty()) {
                    item { EmptyState(stringResource(R.string.rate_no_teammates)) }
                }
                items(uiState.teammates, key = { it.player.id }) { teammate ->
                    TeammateRow(teammate, onRate = { actions.onOpenRating(teammate.player.id) })
                }
            }
        }
    }

    uiState.draft?.let { draft ->
        RatingSheet(draft = draft, isSaving = uiState.isSaving, actions = actions)
    }
}

@Composable
private fun TeammateRow(teammate: TeammateItem, onRate: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(photoUri = teammate.player.photoUri, contentDescription = null, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(teammate.player.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = when (teammate.ratedAs) {
                    null -> stringResource(R.string.rate_pending)
                    MatchRole.FIELD -> stringResource(R.string.rate_done_field)
                    MatchRole.GOALKEEPER -> stringResource(R.string.rate_done_goalkeeper)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (teammate.ratedAs == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
        if (teammate.ratedAs == null) {
            FilledTonalButton(onClick = onRate) { Text(stringResource(R.string.rate_button)) }
        } else {
            OutlinedButton(onClick = onRate) { Text(stringResource(R.string.rate_edit)) }
        }
    }
}

/** Hoja inferior para calificar a un compañero según el rol que tuvo en el partido. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RatingSheet(draft: RatingDraft, isSaving: Boolean, actions: RatePlayersActions) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = actions.onDismissRating, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.rate_sheet_title, draft.player.nickname),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.rate_sheet_role), style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.role == MatchRole.FIELD,
                    onClick = { actions.onRoleChange(MatchRole.FIELD) },
                    label = { Text(stringResource(R.string.rate_role_field)) },
                )
                FilterChip(
                    selected = draft.role == MatchRole.GOALKEEPER,
                    onClick = { actions.onRoleChange(MatchRole.GOALKEEPER) },
                    label = { Text(stringResource(R.string.rate_role_goalkeeper)) },
                )
            }
            when (draft.role) {
                MatchRole.FIELD -> FieldSkill.entries.forEach { skill ->
                    SkillSlider(
                        label = stringResource(skill.labelRes),
                        value = draft.fieldSkills.getValue(skill),
                        onValueChange = { actions.onFieldSkillChange(skill, it) },
                        enabled = !isSaving,
                    )
                }
                MatchRole.GOALKEEPER -> GoalkeeperSkill.entries.forEach { skill ->
                    SkillSlider(
                        label = stringResource(skill.labelRes),
                        value = draft.goalkeeperSkills.getValue(skill),
                        onValueChange = { actions.onGoalkeeperSkillChange(skill, it) },
                        enabled = !isSaving,
                    )
                }
            }
            OutlinedTextField(
                value = draft.comment,
                onValueChange = actions.onCommentChange,
                label = { Text(stringResource(R.string.rate_comment_label)) },
                placeholder = { Text(stringResource(R.string.rate_comment_hint)) },
                supportingText = {
                    Text("${draft.comment.length} / ${PlayerComment.MAX_LENGTH}")
                },
                enabled = !isSaving,
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = actions.onSave,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.rate_save))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
