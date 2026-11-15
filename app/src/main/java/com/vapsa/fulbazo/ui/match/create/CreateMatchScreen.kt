package com.vapsa.fulbazo.ui.match.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.NewMatch
import com.vapsa.fulbazo.ui.components.DateField
import com.vapsa.fulbazo.ui.components.DropdownField
import com.vapsa.fulbazo.ui.components.LoadingState
import com.vapsa.fulbazo.ui.components.TimeField
import com.vapsa.fulbazo.ui.match.labelRes
import java.time.LocalDate
import java.time.LocalTime

/** Hasta cuántos días adelante se puede programar un partido. */
private const val MAX_DAYS_AHEAD = 60L

@Composable
fun CreateMatchRoute(
    onNavigateBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateMatchViewModel = viewModel(factory = CreateMatchViewModel.factory()),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.createdMatchId) {
        if (uiState.createdMatchId != null) {
            viewModel.onResultHandled()
            onCreated()
        }
    }
    LaunchedEffect(uiState.saveFailed) {
        if (uiState.saveFailed) {
            snackbarHostState.showSnackbar(context.getString(R.string.create_match_failed))
            viewModel.onResultHandled()
        }
    }

    CreateMatchScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = CreateMatchActions(
            onBack = onNavigateBack,
            onCourtSelected = viewModel::onCourtSelected,
            onDateChange = viewModel::onDateChange,
            onTimeChange = viewModel::onTimeChange,
            onSlotsChange = viewModel::onSlotsChange,
            onLevelChange = viewModel::onLevelChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onToggleFriend = viewModel::onToggleFriend,
            onToggleGroup = viewModel::onToggleGroup,
            onCreate = viewModel::createMatch,
        ),
    )
}

data class CreateMatchActions(
    val onBack: () -> Unit = {},
    val onCourtSelected: (courtId: String) -> Unit = {},
    val onDateChange: (LocalDate) -> Unit = {},
    val onTimeChange: (LocalTime) -> Unit = {},
    val onSlotsChange: (Int) -> Unit = {},
    val onLevelChange: (MatchLevel) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onToggleFriend: (String) -> Unit = {},
    val onToggleGroup: (String) -> Unit = {},
    val onCreate: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMatchScreen(
    uiState: CreateMatchUiState,
    actions: CreateMatchActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_match_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            return@Scaffold
        }
        val enabled = !uiState.isSaving
        val errors = uiState.errors
        val today = LocalDate.now()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.create_match_host_note),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DropdownField(
                selected = uiState.selectedCourt,
                options = uiState.courts,
                optionLabel = { "${it.name} · ${it.district}" },
                onSelected = { actions.onCourtSelected(it.id) },
                label = stringResource(R.string.create_match_court),
                error = if (CreateMatchError.COURT_REQUIRED in errors) {
                    stringResource(R.string.create_match_error_court)
                } else {
                    null
                },
                enabled = enabled,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(
                    value = uiState.date,
                    onValueChange = actions.onDateChange,
                    label = stringResource(R.string.create_match_date),
                    error = when {
                        CreateMatchError.DATE_REQUIRED in errors -> stringResource(R.string.create_match_error_date)
                        CreateMatchError.DATE_IN_PAST in errors -> stringResource(R.string.create_match_error_past)
                        else -> null
                    },
                    enabled = enabled,
                    minDate = today,
                    maxDate = today.plusDays(MAX_DAYS_AHEAD),
                    initialDisplayedDate = today,
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    value = uiState.time,
                    onValueChange = actions.onTimeChange,
                    label = stringResource(R.string.create_match_time),
                    error = if (CreateMatchError.TIME_REQUIRED in errors) {
                        stringResource(R.string.create_match_error_time)
                    } else {
                        null
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionTitle(stringResource(R.string.create_match_slots))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = { actions.onSlotsChange(uiState.slots - 1) },
                    enabled = enabled && uiState.canDecreaseSlots,
                ) {
                    Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.create_match_slots_less))
                }
                Text(
                    text = stringResource(R.string.create_match_slots_value, uiState.slots),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                FilledTonalIconButton(
                    onClick = { actions.onSlotsChange(uiState.slots + 1) },
                    enabled = enabled && uiState.canIncreaseSlots,
                ) {
                    Icon(painterResource(R.drawable.ic_add), stringResource(R.string.create_match_slots_more))
                }
            }

            SectionTitle(stringResource(R.string.create_match_level))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MatchLevel.entries.forEach { level ->
                    FilterChip(
                        selected = uiState.level == level,
                        onClick = { actions.onLevelChange(level) },
                        label = { Text(stringResource(level.labelRes)) },
                        enabled = enabled,
                    )
                }
            }

            OutlinedTextField(
                value = uiState.description,
                onValueChange = actions.onDescriptionChange,
                label = { Text(stringResource(R.string.create_match_description)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                minLines = 2,
                maxLines = 4,
                isError = CreateMatchError.DESCRIPTION_TOO_LONG in errors,
                supportingText = {
                    Text(
                        stringResource(
                            R.string.create_match_description_counter,
                            uiState.description.trim().length,
                            NewMatch.MAX_DESCRIPTION_LENGTH,
                        ),
                    )
                },
            )

            HorizontalDivider()
            InviteSection(uiState = uiState, actions = actions, enabled = enabled)

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = actions.onCreate,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.create_match_button))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InviteSection(uiState: CreateMatchUiState, actions: CreateMatchActions, enabled: Boolean) {
    SectionTitle(stringResource(R.string.create_match_invite))
    if (uiState.friends.isEmpty()) {
        Text(
            stringResource(R.string.create_match_no_friends),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    if (uiState.groups.isNotEmpty()) {
        Text(stringResource(R.string.create_match_invite_groups), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            uiState.groups.forEach { group ->
                FilterChip(
                    selected = group.id in uiState.invitedGroupIds,
                    onClick = { actions.onToggleGroup(group.id) },
                    label = { Text(stringResource(R.string.create_match_group_chip, group.name, group.memberIds.size)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_group), null, Modifier.size(18.dp)) },
                    enabled = enabled,
                )
            }
        }
    }
    Text(stringResource(R.string.create_match_invite_friends), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        uiState.friends.forEach { friend ->
            FilterChip(
                // Un amigo cuenta como elegido si viene en un grupo seleccionado.
                selected = friend.id in uiState.inviteeIds,
                onClick = { actions.onToggleFriend(friend.id) },
                label = { Text(friend.nickname) },
                enabled = enabled,
            )
        }
    }
    val count = uiState.inviteeIds.size
    if (count > 0) {
        Text(
            stringResource(R.string.create_match_invitees, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}
