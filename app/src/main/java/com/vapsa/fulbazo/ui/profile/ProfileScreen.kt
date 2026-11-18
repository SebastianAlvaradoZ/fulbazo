package com.vapsa.fulbazo.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.SelfAssessment
import com.vapsa.fulbazo.domain.model.User
import com.vapsa.fulbazo.ui.components.FulbazoCard
import com.vapsa.fulbazo.ui.components.IconTag
import com.vapsa.fulbazo.ui.components.ProfileHeader
import com.vapsa.fulbazo.ui.components.ScoreRing
import com.vapsa.fulbazo.ui.stats.labelRes
import com.vapsa.fulbazo.ui.theme.FulbazoTheme
import com.vapsa.fulbazo.ui.theme.Spacing
import java.time.LocalDate

@Composable
fun ProfileRoute(
    onEditStats: () -> Unit,
    onEditProfile: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()

    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }

    ProfileScreen(
        uiState = uiState,
        onEditStats = onEditStats,
        onEditProfile = onEditProfile,
        onLogout = viewModel::logout,
        modifier = modifier,
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEditStats: () -> Unit,
    modifier: Modifier = Modifier,
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    val user = uiState.user
    if (uiState.isLoading || user == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val self = uiState.stats?.selfAssessment
    val community = uiState.stats?.community ?: CommunityStats()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Portada verde con el avatar; el apodo es el nombre principal del jugador.
        ProfileHeader(
            nickname = user.nickname,
            subtitle = "${user.fullName} · ${user.district}",
            photoUri = user.photoUri,
            name = user.fullName,
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
        SelfStatsCard(self)
        CommunityStatsCard(community)

        Button(onClick = onEditStats, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (self == null) R.string.profile_complete_stats else R.string.profile_edit_stats,
                ),
            )
        }
        OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.profile_edit))
        }
        TextButton(
            onClick = { confirmLogout = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(R.string.profile_logout))
        }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.profile_logout_title)) },
            text = { Text(stringResource(R.string.profile_logout_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text(stringResource(R.string.profile_logout)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelfStatsCard(self: SelfAssessment?) {
    StatsCard(title = stringResource(R.string.profile_how_i_see_me)) {
        if (self == null) {
            Text(
                stringResource(R.string.profile_no_self_stats),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@StatsCard
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            IconTag(R.drawable.ic_sports_soccer, stringResource(self.mainPosition.labelRes))
            IconTag(
                R.drawable.ic_directions_run,
                stringResource(R.string.player_foot, stringResource(self.dominantFoot.labelRes).lowercase()),
            )
        }
        OverallRow(stringResource(R.string.stats_overall_field), self.fieldOverall)
        self.goalkeeperOverall?.let { OverallRow(stringResource(R.string.stats_overall_goalkeeper), it) }
    }
}

@Composable
private fun CommunityStatsCard(community: CommunityStats) {
    StatsCard(title = stringResource(R.string.profile_how_they_see_me)) {
        val field = community.asFieldPlayer
        val goalkeeper = community.asGoalkeeper
        if (field == null && goalkeeper == null) {
            Text(
                stringResource(R.string.profile_no_community_stats),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@StatsCard
        }
        field?.let {
            OverallRow(
                label = stringResource(R.string.stats_overall_field),
                value = it.overall,
                detail = stringResource(R.string.profile_votes, it.votes),
            )
        }
        goalkeeper?.let {
            OverallRow(
                label = stringResource(R.string.stats_overall_goalkeeper),
                value = it.overall,
                detail = stringResource(R.string.profile_votes, it.votes),
            )
        }
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable () -> Unit) {
    FulbazoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
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
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ScoreRing(value)
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    FulbazoTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                user = User(
                    id = "1",
                    firstName = "Lucía",
                    lastName = "Quispe",
                    nickname = "lu.quispe",
                    phone = "912345678",
                    email = "lucia@fulbazo.com",
                    birthDate = LocalDate.of(2004, 5, 20),
                    district = "Cayma",
                ),
            ),
            onEditStats = {},
        )
    }
}
