package com.vapsa.fulbazo.ui.match.teams

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vapsa.fulbazo.R

const val TEAMS_RESULT_TAG = "teams_result"

/** Botón "Armar equipos con IA" y los equipos propuestos (solo para el anfitrión). */
@Composable
fun TeamBuilderSection(
    matchId: String,
    playerCount: Int,
    modifier: Modifier = Modifier,
    viewModel: TeamBuilderViewModel = hiltViewModel<TeamBuilderViewModel, TeamBuilderViewModel.Factory>(
        key = "teams-$matchId",
    ) { factory -> factory.create(matchId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TeamBuilderContent(
        state = uiState,
        canBuild = playerCount >= TeamBuilderViewModel.MIN_PLAYERS,
        onBuild = viewModel::buildTeams,
        onUseLocal = viewModel::useLocalSplit,
        onDismiss = viewModel::dismiss,
        modifier = modifier,
    )
}

@Composable
fun TeamBuilderContent(
    state: TeamsUiState,
    canBuild: Boolean,
    onBuild: () -> Unit,
    onUseLocal: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(targetState = state, modifier = modifier.fillMaxWidth(), label = "teams") { current ->
        when (current) {
            TeamsUiState.Idle -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(onClick = onBuild, enabled = canBuild, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_auto_awesome), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.teams_build))
                }
                if (!canBuild) {
                    Text(
                        stringResource(R.string.teams_need_players, TeamBuilderViewModel.MIN_PLAYERS),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TeamsUiState.Loading -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.teams_loading))
            }
            is TeamsUiState.Error -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(if (current.offline) R.string.ai_offline else R.string.teams_error),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onBuild) { Text(stringResource(R.string.action_retry)) }
                        TextButton(onClick = onUseLocal) { Text(stringResource(R.string.teams_use_local)) }
                    }
                }
            }
            is TeamsUiState.Ready -> TeamsResult(current, onRegenerate = onBuild, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun TeamsResult(state: TeamsUiState.Ready, onRegenerate: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(TEAMS_RESULT_TAG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_auto_awesome), null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(if (state.generatedByAi) R.string.teams_ai_label else R.string.teams_local_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TeamColumn(stringResource(R.string.teams_a), state.teamA, Modifier.weight(1f))
                TeamColumn(stringResource(R.string.teams_b), state.teamB, Modifier.weight(1f))
            }
            if (state.explanation.isNotBlank()) {
                Text(state.explanation, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRegenerate) { Text(stringResource(R.string.teams_regenerate)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        }
    }
}

@Composable
private fun TeamColumn(title: String, members: List<TeamMember>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$title (${members.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        members.forEach { member ->
            Text(
                if (member.isGoalkeeper) stringResource(R.string.teams_goalkeeper_member, member.nickname) else member.nickname,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
