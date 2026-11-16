package com.vapsa.fulbazo.ui.match.teams

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAiRepository
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.data.repository.FakeStatsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeamBuilderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val aiRepository = FakeAiRepository(latencyMillis = 0)

    /** Partido de prueba con carlos, maria y pedro. */
    private fun viewModel() = TeamBuilderViewModel(
        matchId = FakeMatchRepository.SEED_MATCH_ID,
        matchRepository = FakeMatchRepository(latencyMillis = 0),
        playerRepository = FakePlayerRepository(authRepository),
        statsRepository = FakeStatsRepository(latencyMillis = 0),
        aiRepository = aiRepository,
    )

    @Test
    fun buildTeams_showsAiTeamsWithEveryPlayer() = runTest {
        val viewModel = viewModel()

        viewModel.buildTeams()
        advanceUntilIdle()

        val state = viewModel.uiState.value as TeamsUiState.Ready
        assertTrue(state.generatedByAi)
        assertEquals(
            setOf("carlitos10", "mari.gol", "pedrito"),
            (state.teamA + state.teamB).map { it.nickname }.toSet(),
        )
        // maria también ataja: aparece marcada como arquera.
        assertTrue((state.teamA + state.teamB).first { it.id == "maria" }.isGoalkeeper)
    }

    @Test
    fun aiFailure_showsError_andLocalSplitIsMarkedWithoutAi() = runTest {
        aiRepository.fail = true
        val viewModel = viewModel()

        viewModel.buildTeams()
        advanceUntilIdle()
        assertEquals(TeamsUiState.Error, viewModel.uiState.value)

        viewModel.useLocalSplit()

        val state = viewModel.uiState.value as TeamsUiState.Ready
        assertFalse(state.generatedByAi)
        assertEquals(3, state.teamA.size + state.teamB.size)
    }

    @Test
    fun dismiss_returnsToIdle() = runTest {
        val viewModel = viewModel()
        viewModel.buildTeams()
        advanceUntilIdle()

        viewModel.dismiss()

        assertEquals(TeamsUiState.Idle, viewModel.uiState.value)
    }
}
