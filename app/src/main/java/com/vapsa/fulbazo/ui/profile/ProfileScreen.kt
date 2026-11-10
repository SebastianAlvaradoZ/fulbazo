package com.vapsa.fulbazo.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.CommunityStats
import com.vapsa.fulbazo.domain.model.SelfAssessment
import com.vapsa.fulbazo.domain.model.User
import com.vapsa.fulbazo.ui.components.UserAvatar
import com.vapsa.fulbazo.ui.stats.labelRes
import com.vapsa.fulbazo.ui.theme.FulbazoTheme
import java.time.LocalDate

@Composable
fun ProfileRoute(
    onEditStats: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(uiState = uiState, onEditStats = onEditStats, modifier = modifier)
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEditStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        UserAvatar(photoUri = user.photoUri, contentDescription = null)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // El apodo es el nombre principal del jugador.
            Text(
                text = user.nickname,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${user.fullName} · ${user.district}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SelfStatsCard(self)
        CommunityStatsCard(community)

        Button(onClick = onEditStats, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (self == null) R.string.profile_complete_stats else R.string.profile_edit_stats,
                ),
            )
        }
    }
}

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
        Text(
            text = stringResource(
                R.string.profile_position_line,
                stringResource(self.mainPosition.labelRes),
                stringResource(self.dominantFoot.labelRes).lowercase(),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
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
    Card(modifier = Modifier.fillMaxWidth()) {
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
        Text(
            text = stringResource(R.string.stats_overall_value, value),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
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
