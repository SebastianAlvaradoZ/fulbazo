package com.vapsa.fulbazo.ui.match.mine

import com.vapsa.fulbazo.MainDispatcherRule
import com.vapsa.fulbazo.data.repository.FakeAuthRepository
import com.vapsa.fulbazo.data.repository.FakeCourtRepository
import com.vapsa.fulbazo.data.repository.FakeJoinRequestRepository
import com.vapsa.fulbazo.data.repository.FakeMatchRepository
import com.vapsa.fulbazo.data.repository.FakePlayerRepository
import com.vapsa.fulbazo.domain.model.JoinRequestStatus
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
class MyMatchesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(latencyMillis = 0)
    private val matchRepository = FakeMatchRepository(latencyMillis = 0)
    private val joinRequestRepository = FakeJoinRequestRepository(latencyMillis = 0)

    private suspend fun TestScope.viewModel(): MyMatchesViewModel {
        authRepository.login("demo@fulbazo.com", "demo123")
        val viewModel = MyMatchesViewModel(
            authRepository,
            matchRepository,
            FakeCourtRepository(),
            FakePlayerRepository(authRepository),
            joinRequestRepository,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun startsWithHistoryAndPendingRequest_butNoUpcoming() = runTest {
        val state = viewModel().uiState.value

        assertTrue(state.upcoming.isEmpty())
        assertEquals(
            listOf("match-past-selva", "match-past-sachaca", "match-past-paucarpata"),
            state.history.map { it.matchId },
        )
        assertEquals(1, state.pendingRequestsCount)
        assertEquals("Losa Deportiva Cayma", state.requests.single().match?.courtName)
    }

    @Test
    fun upcoming_includesHostedAndJoinedMatches_sortedByDate() = runTest {
        matchRepository.createMatch(
            "demo",
            NewMatch("losa-sachaca", FakeMatchRepository.inDays(5), slots = 10, level = MatchLevel.ANY),
        )
        matchRepository.joinMatch(FakeMatchRepository.SEED_MATCH_ID, "demo")
        val state = viewModel().uiState.value

        assertEquals(2, state.upcoming.size)
        assertEquals(FakeMatchRepository.SEED_MATCH_ID, state.upcoming.first().matchId)
        assertTrue(state.upcoming.first().isPlayer && !state.upcoming.first().isHost)
        assertTrue(state.upcoming.last().isHost)
    }

    @Test
    fun cancelingPendingRequest_removesIt() = runTest {
        val viewModel = viewModel()

        viewModel.cancelRequest("req-demo-cayma")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.requests.isEmpty())
    }

    @Test
    fun requests_showPendingFirst() = runTest {
        joinRequestRepository.sendRequest("match-melgar", "demo")
        val melgarRequest = "match-melgar"
        val viewModel = viewModel()
        val id = viewModel.uiState.value.requests.first { it.match?.matchId == melgarRequest }.requestId
        joinRequestRepository.respond(id, accept = false)
        advanceUntilIdle()

        val statuses = viewModel.uiState.value.requests.map { it.status }
        assertEquals(listOf(JoinRequestStatus.PENDING, JoinRequestStatus.REJECTED), statuses)
    }
}
