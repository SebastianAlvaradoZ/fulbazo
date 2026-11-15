package com.vapsa.fulbazo.ui.court

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.NewMatch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourtDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val matchRepository = FakeMatchRepository(latencyMillis = 0)

    private fun TestScope.viewModel(courtId: String): CourtDetailViewModel {
        val viewModel = CourtDetailViewModel(
            courtId,
            authRepository,
            FakeCourtRepository(),
            matchRepository,
            FakePlayerRepository(authRepository),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun unknownCourt_isNotFound() = runTest {
        assertTrue(viewModel("no-existe").uiState.value.notFound)
    }

    @Test
    fun court_showsDataAndOnlyItsUpcomingMatches() = runTest {
        val state = viewModel("losa-umacollo").uiState.value

        assertEquals("Losa Deportiva Umacollo", state.court?.name)
        assertTrue(state.court!!.hasLighting)
        assertEquals(listOf(FakeMatchRepository.SEED_MATCH_ID), state.upcomingMatches.map { it.matchId })
    }

    @Test
    fun pastMatchesAreExcluded_andCourtWithoutMatchesIsEmpty() = runTest {
        // En Sachaca solo hay un partido ya jugado.
        val state = viewModel("losa-sachaca").uiState.value

        assertTrue(state.upcomingMatches.isEmpty())
    }

    @Test
    fun newMatchAtCourt_appearsSortedByDate() = runTest {
        val viewModel = viewModel("losa-umacollo")
        matchRepository.createMatch(
            "carlos",
            NewMatch("losa-umacollo", FakeMatchRepository.inDays(5), slots = 10, level = MatchLevel.ANY),
        )
        advanceUntilIdle()

        val matches = viewModel.uiState.value.upcomingMatches
        assertEquals(2, matches.size)
        assertEquals(FakeMatchRepository.SEED_MATCH_ID, matches.first().matchId)
    }
}
