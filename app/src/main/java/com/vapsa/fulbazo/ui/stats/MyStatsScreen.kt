package com.vapsa.fulbazo.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vapsa.fulbazo.R
import com.vapsa.fulbazo.domain.model.FieldSkill
import com.vapsa.fulbazo.domain.model.Foot
import com.vapsa.fulbazo.domain.model.GoalkeeperSkill
import com.vapsa.fulbazo.domain.model.Position
import com.vapsa.fulbazo.ui.components.SkillSlider
import com.vapsa.fulbazo.ui.theme.FulbazoTheme

@Composable
fun MyStatsRoute(
    onNavigateBack: () -> Unit,
    viewModel: MyStatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.isSaved, uiState.saveFailed) {
        val message = when {
            uiState.isSaved -> context.getString(R.string.stats_saved)
            uiState.saveFailed -> context.getString(R.string.stats_save_error)
            else -> return@LaunchedEffect
        }
        // Se avisa al ViewModel después de mostrarlo: si se hiciera antes, el
        // cambio de estado reiniciaría este efecto y cancelaría el mensaje.
        snackbarHostState.showSnackbar(message)
        viewModel.onSaveMessageShown()
    }

    MyStatsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = MyStatsActions(
            onBack = onNavigateBack,
            onRetry = viewModel::load,
            onFootChange = viewModel::onDominantFootChange,
            onMainPositionChange = viewModel::onMainPositionChange,
            onSecondaryPositionChange = viewModel::onSecondaryPositionChange,
            onFieldSkillChange = viewModel::onFieldSkillChange,
            onGoalkeeperSkillChange = viewModel::onGoalkeeperSkillChange,
            onSave = viewModel::save,
        ),
    )
}

/** Eventos que la vista envía al ViewModel. */
data class MyStatsActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onFootChange: (Foot) -> Unit = {},
    val onMainPositionChange: (Position) -> Unit = {},
    val onSecondaryPositionChange: (Position?) -> Unit = {},
    val onFieldSkillChange: (FieldSkill, Int) -> Unit = { _, _ -> },
    val onGoalkeeperSkillChange: (GoalkeeperSkill, Int) -> Unit = { _, _ -> },
    val onSave: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStatsScreen(
    uiState: MyStatsUiState,
    actions: MyStatsActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.auth_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.loadFailed -> LoadError(onRetry = actions.onRetry, modifier = contentModifier)
            else -> StatsForm(uiState = uiState, actions = actions, modifier = contentModifier)
        }
    }
}

@Composable
private fun LoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_error),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.stats_load_error), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.stats_retry)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsForm(
    uiState: MyStatsUiState,
    actions: MyStatsActions,
    modifier: Modifier = Modifier,
) {
    val enabled = !uiState.isSaving
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.stats_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OverallCard(uiState)

        // Pierna buena
        SectionTitle(stringResource(R.string.stats_dominant_foot))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Foot.entries.forEach { foot ->
                FilterChip(
                    selected = uiState.dominantFoot == foot,
                    onClick = { actions.onFootChange(foot) },
                    label = { Text(stringResource(foot.labelRes)) },
                    enabled = enabled,
                )
            }
        }
        when {
            uiState.footMissing -> ErrorText(stringResource(R.string.stats_select_foot))
            uiState.dominantFoot != null -> HelperText(
                uiState.dominantFoot.weakFoot
                    ?.let { stringResource(R.string.stats_weak_foot, stringResource(it.labelRes)) }
                    ?: stringResource(R.string.stats_weak_foot_none),
            )
        }

        // Posiciones
        SectionTitle(stringResource(R.string.stats_main_position))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Position.entries.forEach { position ->
                FilterChip(
                    selected = uiState.mainPosition == position,
                    onClick = { actions.onMainPositionChange(position) },
                    label = { Text(stringResource(position.labelRes)) },
                    enabled = enabled,
                )
            }
        }
        if (uiState.positionMissing) ErrorText(stringResource(R.string.stats_select_position))

        SectionTitle(stringResource(R.string.stats_secondary_position))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.secondaryPosition == null,
                onClick = { actions.onSecondaryPositionChange(null) },
                label = { Text(stringResource(R.string.stats_secondary_none)) },
                enabled = enabled,
            )
            uiState.secondaryOptions.forEach { position ->
                FilterChip(
                    selected = uiState.secondaryPosition == position,
                    onClick = { actions.onSecondaryPositionChange(position) },
                    label = { Text(stringResource(position.labelRes)) },
                    enabled = enabled,
                )
            }
        }
        if (uiState.mainPosition == Position.GOALKEEPER) {
            HelperText(stringResource(R.string.stats_goalkeeper_note))
        }

        HorizontalDivider()

        SectionTitle(stringResource(R.string.stats_field_skills))
        FieldSkill.entries.forEach { skill ->
            SkillSlider(
                label = stringResource(skill.labelRes),
                value = uiState.fieldSkills.getValue(skill),
                onValueChange = { actions.onFieldSkillChange(skill, it) },
                enabled = enabled,
            )
        }

        // Las habilidades de arquero aparecen y desaparecen con animación.
        AnimatedVisibility(visible = uiState.showGoalkeeperSkills) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider()
                SectionTitle(stringResource(R.string.stats_goalkeeper_skills))
                GoalkeeperSkill.entries.forEach { skill ->
                    SkillSlider(
                        label = stringResource(skill.labelRes),
                        value = uiState.goalkeeperSkills.getValue(skill),
                        onValueChange = { actions.onGoalkeeperSkillChange(skill, it) },
                        enabled = enabled,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = actions.onSave,
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
                Text(stringResource(R.string.stats_save))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Nivel general calculado en vivo mientras se mueven los sliders. */
@Composable
private fun OverallCard(uiState: MyStatsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_overall),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                OverallValue(stringResource(R.string.stats_overall_field), uiState.fieldOverall)
                if (uiState.showGoalkeeperSkills) {
                    OverallValue(
                        stringResource(R.string.stats_overall_goalkeeper),
                        uiState.goalkeeperOverall,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallValue(label: String, value: Double) {
    // El número cambia suavemente al mover los deslizadores.
    val animatedValue by animateFloatAsState(targetValue = value.toFloat(), label = "overall")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.stats_overall_value, animatedValue),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun HelperText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun MyStatsScreenPreview() {
    FulbazoTheme {
        MyStatsScreen(
            uiState = MyStatsUiState(
                isLoading = false,
                dominantFoot = Foot.RIGHT,
                mainPosition = Position.GOALKEEPER,
                secondaryPosition = Position.DEFENDER,
            ),
            actions = MyStatsActions(),
        )
    }
}
